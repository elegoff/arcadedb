/*
 * Copyright 2021 Arcade Data Ltd
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.arcadedb.engine;

import com.arcadedb.database.Binary;
import com.arcadedb.database.DatabaseInternal;
import com.arcadedb.exception.SchemaException;
import com.arcadedb.exception.TransactionException;
import com.arcadedb.log.LogManager;
import com.arcadedb.utility.LockManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.ClosedByInterruptException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

public class TransactionManager {
  private static final long MAX_LOG_FILE_SIZE = 64 * 1024 * 1024;

  private final DatabaseInternal database;
  private       WALFile[]        activeWALFilePool;
  private final List<WALFile>    inactiveWALFilePool = new ArrayList<>();
  private final String           logContext;

  private final Timer          task;
  private       CountDownLatch taskExecuting = new CountDownLatch(0);

  private final AtomicLong                   transactionIds     = new AtomicLong();
  private final AtomicLong                   logFileCounter     = new AtomicLong();
  private final LockManager<Integer, Thread> fileIdsLockManager = new LockManager<>();

  private final AtomicLong statsPagesWritten = new AtomicLong();
  private final AtomicLong statsBytesWritten = new AtomicLong();

  public TransactionManager(final DatabaseInternal database) {
    this.database = database;

    this.logContext = LogManager.instance().getContext();

    if (database.getMode() == PaginatedFile.MODE.READ_WRITE) {
      createWALFilePool();

      task = new Timer("ArcadeDB TransactionManager " + database.getName());
      task.schedule(new TimerTask() {
        @Override
        public void run() {
          if (!database.isOpen()) {
            // DB CLOSED, CANCEL THE TASK
            cancel();
            return;
          }

          if (activeWALFilePool != null) {
            taskExecuting = new CountDownLatch(1);
            try {
              if (logContext != null)
                LogManager.instance().setContext(logContext);

              checkWALFiles();
              cleanWALFiles();
            } finally {
              taskExecuting.countDown();
            }
          }
        }
      }, 1000, 1000);
    } else
      task = null;
  }

  public void close() {
    if (task != null)
      task.cancel();

    try {
      taskExecuting.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      // IGNORE IT
    }

    fileIdsLockManager.close();

    if (activeWALFilePool != null) {
      // MOVE ALL WAL FILES AS INACTIVE
      for (int i = 0; i < activeWALFilePool.length; ++i) {
        inactiveWALFilePool.add(activeWALFilePool[i]);
        activeWALFilePool[i] = null;
      }

      for (int retry = 0; retry < 20 && !cleanWALFiles(); ++retry) {
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          break;
        }
      }

      if (!cleanWALFiles())
        LogManager.instance().log(this, Level.WARNING, "Error on removing all transaction files. Remained: %s", null, inactiveWALFilePool);
    }
  }

  public Binary createTransactionBuffer(final long txId, final List<MutablePage> pages) {
    return WALFile.writeTransactionToBuffer(pages, txId);
  }

  public void writeTransactionToWAL(final List<MutablePage> pages, final WALFile.FLUSH_TYPE sync, final long txId, final Binary bufferChanges) {
    while (true) {
      final WALFile file = activeWALFilePool[(int) (Thread.currentThread().getId() % activeWALFilePool.length)];

      if (file != null && file.acquire(() -> {
        file.writeTransactionToFile(database, pages, sync, file, txId, bufferChanges);
        return null;
      }))
        break;

      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
    }
  }

  public void notifyPageFlushed(final MutablePage page) {
    final WALFile walFile = page.getWALFile();

    if (walFile == null)
      return;

    walFile.notifyPageFlushed();
  }

  public void checkIntegrity() {
    LogManager.instance().log(this, Level.WARNING, "Started recovery of database '%s'", null, database);

    try {
      // OPEN EXISTENT WAL FILES
      final File dir = new File(database.getDatabasePath());
      final File[] walFiles = dir.listFiles((dir1, name) -> name.endsWith(".wal"));

      if (walFiles == null || walFiles.length == 0) {
        LogManager.instance().log(this, Level.WARNING, "Recovery not possible because no WAL files were found");
        return;
      }

      activeWALFilePool = new WALFile[walFiles.length];
      for (int i = 0; i < walFiles.length; ++i) {
        try {
          activeWALFilePool[i] = new WALFile(database.getDatabasePath() + "/" + walFiles[i].getName());
        } catch (FileNotFoundException e) {
          LogManager.instance().log(this, Level.SEVERE, "Error on WAL file management for file '%s'", e, database.getDatabasePath() + walFiles[i].getName());
        }
      }

      if (activeWALFilePool.length > 0) {
        final WALFile.WALTransaction[] walPositions = new WALFile.WALTransaction[activeWALFilePool.length];
        for (int i = 0; i < activeWALFilePool.length; ++i) {
          final WALFile file = activeWALFilePool[i];
          walPositions[i] = file.getFirstTransaction();
        }

        long lastTxId = -1;

        while (true) {
          int lowerTx = -1;
          long lowerTxId = -1;

          for (int i = 0; i < walPositions.length; ++i) {
            final WALFile.WALTransaction walTx = walPositions[i];
            if (walTx != null) {
              if (lowerTxId == -1 || walTx.txId < lowerTxId) {
                lowerTxId = walTx.txId;
                lowerTx = i;
              }
            }
          }

          if (lowerTxId == -1)
            // FINISHED
            break;

          lastTxId = lowerTxId;

          applyChanges(walPositions[lowerTx]);

          walPositions[lowerTx] = activeWALFilePool[lowerTx].getTransaction(walPositions[lowerTx].endPositionInLog);
        }

        // CONTINUE FROM LAST TXID
        transactionIds.set(lastTxId + 1);

        // REMOVE ALL WAL FILES
        for (final WALFile file : activeWALFilePool) {
          try {
            file.drop();
            LogManager.instance().log(this, Level.FINE, "Dropped WAL file '%s'", null, file);
          } catch (IOException e) {
            LogManager.instance().log(this, Level.SEVERE, "Error on dropping WAL file '%s'", e, file);
          }
        }
        createWALFilePool();
        database.getPageManager().clear();
      }
    } finally {
      LogManager.instance().log(this, Level.WARNING, "Recovery of database '%s' completed", null, database);
    }
  }

  public Map<String, Object> getStats() {
    final Map<String, Object> map = new HashMap<>();
    map.put("logFiles", logFileCounter.get());

    for (final WALFile file : activeWALFilePool) {
      if (file != null) {
        final Map<String, Object> stats = file.getStats();
        statsPagesWritten.addAndGet((Long) stats.get("pagesWritten"));
        statsBytesWritten.addAndGet((Long) stats.get("bytesWritten"));
      }
    }

    map.put("pagesWritten", statsPagesWritten.get());
    map.put("bytesWritten", statsBytesWritten.get());
    return map;
  }

  public boolean applyChanges(final WALFile.WALTransaction tx) {
    boolean changed = false;
    boolean involveDictionary = false;

    final int dictionaryId = database.getSchema().getDictionary() != null ? database.getSchema().getDictionary().file.getFileId() : -1;

    LogManager.instance().log(this, Level.FINE, "- applying changes from txId=%d", null, tx.txId);

    for (WALFile.WALPage txPage : tx.pages) {
      final PaginatedFile file;

      if (!database.getFileManager().existsFile(txPage.fileId)) {
        LogManager.instance().log(this, Level.WARNING, "Error on restoring transaction. Found deleted file %d", null, txPage.fileId);
        continue;
      }

      try {
        file = database.getFileManager().getFile(txPage.fileId);
      } catch (Exception e) {
        LogManager.instance().log(this, Level.SEVERE, "Error on applying tx changes for page %s", e, txPage);
        throw e;
      }

      final PageId pageId = new PageId(txPage.fileId, txPage.pageNumber);
      try {
        final BasePage page = database.getPageManager().getPage(pageId, file.getPageSize(), false, true);

        LogManager.instance()
            .log(this, Level.FINE, "-- checking page %s versionInLog=%d versionInDB=%d", null, pageId, txPage.currentPageVersion, page.getVersion());

        if (txPage.currentPageVersion < page.getVersion())
          // SKIP IT
          continue;

        if (txPage.currentPageVersion > page.getVersion() + 1) {
          LogManager.instance().log(this, Level.WARNING,
              "Cannot apply changes to the database because modified page version in WAL (" + txPage.currentPageVersion
                  + ") does not match with existent version (" + page.getVersion() + ") fileId=" + txPage.fileId);
          continue;
        }
//          throw new WALException("Cannot apply changes to the database because modified page version in WAL (" + txPage.currentPageVersion
//              + ") does not match with existent version (" + page.getVersion() + ") fileId=" + txPage.fileId);

        LogManager.instance()
            .log(this, Level.FINE, "Updating page %s versionInLog=%d versionInDB=%d (txId=%d)", null, pageId, txPage.currentPageVersion, page.getVersion(),
                tx.txId);

        // IF VERSION IS THE SAME OR MAJOR, OVERWRITE THE PAGE
        final MutablePage modifiedPage = page.modify();
        txPage.currentContent.rewind();
        modifiedPage.writeByteArray(txPage.changesFrom - BasePage.PAGE_HEADER_SIZE, txPage.currentContent.getContent());
        modifiedPage.version = txPage.currentPageVersion;
        modifiedPage.setContentSize(txPage.currentPageSize);
        modifiedPage.flushMetadata();
        file.write(modifiedPage);

        database.getPageManager().removePageFromCache(modifiedPage.pageId);

        final PaginatedComponent component = database.getSchema().getFileById(txPage.fileId);
        if (component != null) {
          final int newPageCount = (int) (file.getSize() / file.getPageSize());
          if (newPageCount > component.pageCount.get())
            component.setPageCount(newPageCount);
        }

        if (file.getFileId() == dictionaryId)
          involveDictionary = true;

        changed = true;
        LogManager.instance().log(this, Level.FINE, "  - updating page %s v%d", null, pageId, modifiedPage.version);

      } catch (IOException e) {
        if (e instanceof ClosedByInterruptException)
          // NORMAL EXCEPTION IN CASE THE CONNECTION/THREAD IS CLOSED (=INTERRUPTED)
          Thread.currentThread().interrupt();
        else
          LogManager.instance().log(this, Level.SEVERE, "Error on applying changes to page %s", e, pageId);

        throw new WALException("Cannot apply changes to page " + pageId, e);
      }
    }

    if (involveDictionary) {
      try {
        database.getSchema().getDictionary().reload();
      } catch (IOException e) {
        throw new SchemaException("Unable to update dictionary after transaction commit");
      }
    }

    return changed;
  }

  public void kill() {
    if (task != null) {
      task.cancel();
      task.purge();
    }

    fileIdsLockManager.close();

    try {
      taskExecuting.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      // IGNORE IT
    }
  }

  public long getNextTransactionId() {
    return transactionIds.getAndIncrement();
  }

  public List<Integer> tryLockFiles(final Collection<Integer> fileIds, final long timeout) {
    // ORDER THE FILES TO AVOID DEADLOCK
    final List<Integer> orderedFilesIds = new ArrayList<>(fileIds);
    Collections.sort(orderedFilesIds);

    final List<Integer> lockedFiles = new ArrayList<>(orderedFilesIds.size());
    for (Integer fileId : orderedFilesIds) {
      if (tryLockFile(fileId, timeout))
        lockedFiles.add(fileId);
      else
        break;
    }

    if (lockedFiles.size() == orderedFilesIds.size()) {
      // OK: ALL LOCKED
      LogManager.instance().log(this, Level.FINE, "Locked files %s (threadId=%d)", null, orderedFilesIds, Thread.currentThread().getId());
      return lockedFiles;
    }

    // ERROR: UNLOCK LOCKED FILES
    unlockFilesInOrder(lockedFiles);

    throw new TransactionException("Timeout on locking resource during commit (fileIds=" + fileIds + ")");
  }

  public void unlockFilesInOrder(final Collection<Integer> lockedFileIds) {
    if (lockedFileIds != null && !lockedFileIds.isEmpty()) {
      for (Integer fileId : lockedFileIds)
        unlockFile(fileId);

      LogManager.instance().log(this, Level.FINE, "Unlocked files %s (threadId=%d)", null, lockedFileIds, Thread.currentThread().getId());
    }
  }

  public boolean tryLockFile(final Integer fileId, final long timeout) {
    return fileIdsLockManager.tryLock(fileId, Thread.currentThread(), timeout);
  }

  public void unlockFile(final Integer fileId) {
    fileIdsLockManager.unlock(fileId, Thread.currentThread());
  }

  private void createWALFilePool() {
    activeWALFilePool = new WALFile[Runtime.getRuntime().availableProcessors()];
    for (int i = 0; i < activeWALFilePool.length; ++i) {
      try {
        activeWALFilePool[i] = database.getWALFileFactory().newInstance(database.getDatabasePath() + "/txlog_" + logFileCounter.getAndIncrement() + ".wal");
      } catch (FileNotFoundException e) {
        LogManager.instance().log(this, Level.SEVERE, "Error on WAL file management for file '%s'", e,
            database.getDatabasePath() + "/txlog_" + logFileCounter.getAndIncrement() + ".wal");
      }
    }
  }

  private void checkWALFiles() {
    if (activeWALFilePool != null)
      for (int i = 0; i < activeWALFilePool.length; ++i) {
        final WALFile file = activeWALFilePool[i];
        try {
          if (file != null && file.getSize() > MAX_LOG_FILE_SIZE) {
            LogManager.instance()
                .log(this, Level.FINE, "WAL file '%s' reached maximum size (%d), set it as inactive, waiting for the drop (page2flush=%d)", null, file,
                    MAX_LOG_FILE_SIZE, file.getPendingPagesToFlush());
            activeWALFilePool[i] = database.getWALFileFactory().newInstance(database.getDatabasePath() + "/txlog_" + logFileCounter.getAndIncrement() + ".wal");
            file.setActive(false);
            inactiveWALFilePool.add(file);
          }
        } catch (IOException e) {
          LogManager.instance().log(this, Level.SEVERE, "Error on WAL file management for file '%s'", e, file);
        }
      }
  }

  private boolean cleanWALFiles() {
    for (Iterator<WALFile> it = inactiveWALFilePool.iterator(); it.hasNext(); ) {
      final WALFile file = it.next();

      LogManager.instance().log(this, Level.FINE, "Inactive file %s contains %d pending pages to flush", null, file, file.getPagesToFlush());

      if (file.getPagesToFlush() == 0) {
        // ALL PAGES FLUSHED, REMOVE THE FILE
        try {
          final Map<String, Object> fileStats = file.getStats();
          statsPagesWritten.addAndGet((Long) fileStats.get("pagesWritten"));
          statsBytesWritten.addAndGet((Long) fileStats.get("bytesWritten"));

          file.drop();

          LogManager.instance().log(this, Level.FINE, "Dropped WAL file '%s'", null, file);
        } catch (IOException e) {
          LogManager.instance().log(this, Level.SEVERE, "Error on dropping WAL file '%s'", e, file);
        }
        it.remove();
      }
    }

    return inactiveWALFilePool.isEmpty();
  }
}
