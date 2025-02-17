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

package com.arcadedb.index.lsm;

import com.arcadedb.database.*;
import com.arcadedb.engine.*;
import com.arcadedb.exception.DatabaseIsReadOnlyException;
import com.arcadedb.index.*;
import com.arcadedb.log.LogManager;
import com.arcadedb.schema.EmbeddedSchema;
import com.arcadedb.schema.Schema;
import com.arcadedb.schema.Type;
import com.arcadedb.serializer.BinaryTypes;
import com.arcadedb.utility.RWLockContext;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

/**
 * LSM-Tree index implementation. It relies on a mutable index and its underlying immutable, compacted index.
 */
public class LSMTreeIndex implements RangeIndex, IndexInternal {
  private static final IndexCursor                                             EMPTY_CURSOR       = new EmptyIndexCursor();
  private final        String                                                  name;
  private final        RWLockContext                                           lock               = new RWLockContext();
  private              int                                                     associatedBucketId = -1;
  private              String                                                  typeName;
  protected            String[]                                                propertyNames;
  protected            LSMTreeIndexMutable                                     mutable;
  protected            AtomicReference<LSMTreeIndexAbstract.COMPACTING_STATUS> compactingStatus   = new AtomicReference<>(
      LSMTreeIndexAbstract.COMPACTING_STATUS.NO);

  public static class IndexFactoryHandler implements com.arcadedb.index.IndexFactoryHandler {
    @Override
    public IndexInternal create(final DatabaseInternal database, final String name, final boolean unique, final String filePath, final PaginatedFile.MODE mode,
        final byte[] keyTypes, final int pageSize, final LSMTreeIndexAbstract.NULL_STRATEGY nullStrategy, final BuildIndexCallback callback)
        throws IOException {
      return new LSMTreeIndex(database, name, unique, filePath, mode, keyTypes, pageSize, nullStrategy);
    }
  }

  public static class PaginatedComponentFactoryHandlerUnique implements PaginatedComponentFactory.PaginatedComponentFactoryHandler {
    @Override
    public PaginatedComponent createOnLoad(final DatabaseInternal database, final String name, final String filePath, final int id,
        final PaginatedFile.MODE mode, final int pageSize) throws IOException {
      if (filePath.endsWith(LSMTreeIndexCompacted.UNIQUE_INDEX_EXT))
        return new LSMTreeIndexCompacted(null, database, name, true, filePath, id, mode, pageSize);

      return new LSMTreeIndex(database, name, true, filePath, id, mode, pageSize).mutable;
    }
  }

  public static class PaginatedComponentFactoryHandlerNotUnique implements PaginatedComponentFactory.PaginatedComponentFactoryHandler {
    @Override
    public PaginatedComponent createOnLoad(final DatabaseInternal database, final String name, final String filePath, final int id,
        final PaginatedFile.MODE mode, final int pageSize) throws IOException {
      if (filePath.endsWith(LSMTreeIndexCompacted.UNIQUE_INDEX_EXT))
        return new LSMTreeIndexCompacted(null, database, name, false, filePath, id, mode, pageSize);

      return new LSMTreeIndex(database, name, false, filePath, id, mode, pageSize).mutable;
    }
  }

  /**
   * Called at creation time.
   */
  public LSMTreeIndex(final DatabaseInternal database, final String name, final boolean unique, String filePath, final PaginatedFile.MODE mode,
      final byte[] keyTypes, final int pageSize, final LSMTreeIndexAbstract.NULL_STRATEGY nullStrategy) throws IOException {
    this.name = name;
    this.mutable = new LSMTreeIndexMutable(this, database, name, unique, filePath, mode, keyTypes, pageSize, nullStrategy);
  }

  /**
   * Called at load time (1st page only).
   */
  public LSMTreeIndex(final DatabaseInternal database, final String name, final boolean unique, String filePath, final int id, final PaginatedFile.MODE mode,
      final int pageSize) throws IOException {
    this.name = name;
    this.mutable = new LSMTreeIndexMutable(this, database, name, unique, filePath, id, mode, pageSize);
  }

  public boolean scheduleCompaction() {
    return compactingStatus.compareAndSet(LSMTreeIndexAbstract.COMPACTING_STATUS.NO, LSMTreeIndexAbstract.COMPACTING_STATUS.SCHEDULED);
  }

  public void setMetadata(final String typeName, final String[] propertyNames, final int associatedBucketId) {
    this.typeName = typeName;
    this.propertyNames = propertyNames;
    this.associatedBucketId = associatedBucketId;
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this)
      return true;

    if (!(obj instanceof LSMTreeIndex))
      return false;

    final LSMTreeIndex m2 = (LSMTreeIndex) obj;

    if (!name.equals(m2.name))
      return false;

    if (!typeName.equals(m2.typeName))
      return false;

    if (associatedBucketId != m2.associatedBucketId)
      return false;

    return Arrays.equals(propertyNames, m2.propertyNames);
  }

  @Override
  public EmbeddedSchema.INDEX_TYPE getType() {
    return Schema.INDEX_TYPE.LSM_TREE;
  }

  @Override
  public String getTypeName() {
    return typeName;
  }

  @Override
  public String[] getPropertyNames() {
    return propertyNames;
  }

  @Override
  public boolean compact() throws IOException, InterruptedException {
    if (mutable.getDatabase().getMode() == PaginatedFile.MODE.READ_ONLY)
      throw new DatabaseIsReadOnlyException("Cannot update the index '" + getName() + "'");

    if (!compactingStatus.compareAndSet(LSMTreeIndexAbstract.COMPACTING_STATUS.SCHEDULED, LSMTreeIndexAbstract.COMPACTING_STATUS.IN_PROGRESS))
      // ALREADY COMPACTING
      return false;

//    return false;

    try {
      return LSMTreeIndexCompactor.compact(this);
    } finally {
      compactingStatus.set(LSMTreeIndexAbstract.COMPACTING_STATUS.NO);
    }
  }

  @Override
  public boolean isCompacting() {
    return compactingStatus.get() == LSMTreeIndexAbstract.COMPACTING_STATUS.IN_PROGRESS;
  }

  @Override
  public void close() {
    lock.executeInWriteLock(() -> {
      if (mutable != null)
        mutable.close();
      return null;
    });
  }

  public void drop() {
    lock.executeInWriteLock(() -> {
      final LSMTreeIndexCompacted subIndex = mutable.getSubIndex();
      if (subIndex != null)
        subIndex.drop();

      mutable.drop();

      return null;
    });
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public long countEntries() {
    long total = 0;
    for (IndexCursor it = iterator(true); it.hasNext(); ) {
      it.next();
      ++total;
    }
    return total;
  }

  @Override
  public IndexCursor iterator(final boolean ascendingOrder) {
    return lock.executeInReadLock(() -> new LSMTreeIndexCursor(mutable, ascendingOrder));
  }

  @Override
  public IndexCursor iterator(final boolean ascendingOrder, final Object[] fromKeys, final boolean inclusive) {
    return lock.executeInReadLock(() -> mutable.iterator(ascendingOrder, fromKeys, inclusive));
  }

  @Override
  public IndexCursor range(final Object[] beginKeys, final boolean beginKeysInclusive, final Object[] endKeys, final boolean endKeysInclusive) {
    return lock.executeInReadLock(() -> mutable.range(beginKeys, beginKeysInclusive, endKeys, endKeysInclusive));
  }

  @Override
  public boolean supportsOrderedIterations() {
    return true;
  }

  @Override
  public boolean isAutomatic() {
    return propertyNames != null;
  }

  @Override
  public IndexCursor get(final Object[] keys) {
    return get(keys, -1);
  }

  @Override
  public IndexCursor get(final Object[] keys, final int limit) {
    final Object[] convertedKeys = convertKeys(keys);

    if (mutable.getDatabase().getTransaction().getStatus() == TransactionContext.STATUS.BEGUN) {
      Set<IndexCursorEntry> txChanges = null;

      final Map<TransactionIndexContext.ComparableKey, Set<TransactionIndexContext.IndexKey>> indexChanges = mutable.getDatabase().getTransaction()
          .getIndexChanges().getIndexKeys(getName());
      if (indexChanges != null) {
        final Set<TransactionIndexContext.IndexKey> values = indexChanges.get(new TransactionIndexContext.ComparableKey(convertedKeys));
        if (values != null) {
          for (final TransactionIndexContext.IndexKey value : values) {
            if (value != null) {
              if (!value.addOperation)
                // REMOVED
                return EMPTY_CURSOR;

              if (txChanges == null)
                txChanges = new HashSet<>();

              txChanges.add(new IndexCursorEntry(convertedKeys, value.rid, 1));

              if (limit > -1 && txChanges.size() > limit)
                // LIMIT REACHED
                return new TempIndexCursor(txChanges);
            }
          }
        }
      }

      final IndexCursor result = lock.executeInReadLock(() -> mutable.get(convertedKeys, limit));

      if (txChanges != null) {
        // MERGE SETS
        while (result.hasNext())
          txChanges.add(new IndexCursorEntry(convertedKeys, result.next(), 1));
        return new TempIndexCursor(txChanges);
      }

      return result;
    }

    return lock.executeInReadLock(() -> mutable.get(convertedKeys, limit));
  }

  @Override
  public void put(final Object[] keys, final RID[] rids) {
    final Object[] convertedKeys = convertKeys(keys);

    if (mutable.getDatabase().getTransaction().getStatus() == TransactionContext.STATUS.BEGUN) {
      // KEY ADDED AT COMMIT TIME (IN A LOCK)
      final TransactionContext tx = mutable.getDatabase().getTransaction();
      for (RID rid : rids)
        tx.addIndexOperation(this, true, convertedKeys, rid);
    } else
      lock.executeInReadLock(() -> {
        mutable.put(convertedKeys, rids);
        return null;
      });
  }

  @Override
  public void remove(final Object[] keys) {
    final Object[] convertedKeys = convertKeys(keys);

    if (mutable.getDatabase().getTransaction().getStatus() == TransactionContext.STATUS.BEGUN)
      // KEY REMOVED AT COMMIT TIME (IN A LOCK)
      mutable.getDatabase().getTransaction().addIndexOperation(this, false, convertedKeys, null);
    else
      lock.executeInReadLock(() -> {
        mutable.remove(convertedKeys);
        return null;
      });
  }

  @Override
  public void remove(final Object[] keys, final Identifiable rid) {
    final Object[] convertedKeys = convertKeys(keys);

    if (mutable.getDatabase().getTransaction().getStatus() == TransactionContext.STATUS.BEGUN)
      // KEY REMOVED AT COMMIT TIME (IN A LOCK)
      mutable.getDatabase().getTransaction().addIndexOperation(this, false, convertedKeys, rid.getIdentity());
    else
      lock.executeInReadLock(() -> {
        mutable.remove(convertedKeys, rid);
        return null;
      });
  }

  @Override
  public Map<String, Long> getStats() {
    return mutable.getStats();
  }

  @Override
  public LSMTreeIndexAbstract.NULL_STRATEGY getNullStrategy() {
    return mutable.nullStrategy;
  }

  @Override
  public void setNullStrategy(final LSMTreeIndexAbstract.NULL_STRATEGY nullStrategy) {
    mutable.nullStrategy = nullStrategy;
  }

  @Override
  public int getFileId() {
    return mutable.getFileId();
  }

  @Override
  public boolean isUnique() {
    return mutable.isUnique();
  }

  public LSMTreeIndexMutable getMutableIndex() {
    return mutable;
  }

  @Override
  public PaginatedComponent getPaginatedComponent() {
    return mutable;
  }

  @Override
  public int getAssociatedBucketId() {
    return associatedBucketId;
  }

  @Override
  public String toString() {
    return name;
  }

  protected LSMTreeIndexMutable splitIndex(final int startingFromPage, final LSMTreeIndexCompacted compactedIndex) {
    final DatabaseInternal database = mutable.getDatabase();
    if (database.isTransactionActive())
      throw new IllegalStateException("Cannot replace compacted index because a transaction is active");

    final int fileId = mutable.getFileId();

    database.getTransactionManager().tryLockFile(fileId, 0);
    try {

      final LSMTreeIndexMutable prevMutable = mutable;

      // COPY MUTABLE PAGES TO THE NEW FILE
      final LSMTreeIndexMutable result = lock.executeInWriteLock(() -> {
        final int pageSize = mutable.getPageSize();

        int last_ = mutable.getName().lastIndexOf('_');
        final String newName = mutable.getName().substring(0, last_) + "_" + System.nanoTime();

        final LSMTreeIndexMutable newMutableIndex = new LSMTreeIndexMutable(this, database, newName, mutable.isUnique(),
            database.getDatabasePath() + "/" + newName, mutable.getKeyTypes(), pageSize, compactedIndex);
        database.getSchema().getEmbedded().registerFile(newMutableIndex);

        final MutablePage subIndexMainPage = compactedIndex.setCompactedTotalPages();
        database.getPageManager().updatePage(subIndexMainPage, false, false);

        // KEEP METADATA AND LEAVE IT EMPTY
        final MutablePage rootPage = newMutableIndex.createNewPage();
        database.getPageManager().updatePage(rootPage, true, false);
        newMutableIndex.setPageCount(1);

        for (int i = 0; i < mutable.getTotalPages() - startingFromPage; ++i) {
          final BasePage currentPage = database.getTransaction().getPage(new PageId(mutable.getFileId(), i + startingFromPage), pageSize);

          // COPY THE ENTIRE PAGE TO THE NEW INDEX
          final MutablePage newPage = newMutableIndex.createNewPage();

          final ByteBuffer pageContent = currentPage.getContent();
          pageContent.rewind();
          newPage.getContent().put(pageContent);

          database.getPageManager().updatePage(newPage, true, false);
          newMutableIndex.setPageCount(i + 2);
        }

        newMutableIndex.setCurrentMutablePages(newMutableIndex.getTotalPages() - 1);

        // SWAP OLD WITH NEW INDEX IN EXCLUSIVE LOCK (NO READ/WRITE ARE POSSIBLE IN THE MEANTIME)
        newMutableIndex.removeTempSuffix();

        mutable = newMutableIndex;

        database.getSchema().getEmbedded().saveConfiguration();

        return newMutableIndex;
      });

      if (prevMutable != null) {
        try {
          prevMutable.drop();
        } catch (IOException e) {
          LogManager.instance().log(this, Level.WARNING, "Error on deleting old copy of mutable index file %s", e, prevMutable);
        }
      }

      return result;

    } finally {
      database.getTransactionManager().unlockFile(fileId);
    }
  }

  public long build(final BuildIndexCallback callback) {
    final AtomicLong total = new AtomicLong();

    if (propertyNames == null || propertyNames.length == 0)
      throw new IndexException("Cannot rebuild index '" + name + "' because metadata information are missing");

    final DatabaseInternal db = mutable.getDatabase();

    db.scanBucket(db.getSchema().getBucketById(associatedBucketId).getName(), record -> {
      db.getIndexer().addToIndex(LSMTreeIndex.this, record.getIdentity(), (Document) record);
      total.incrementAndGet();

      if (callback != null)
        callback.onDocumentIndexed((Document) record, total.get());

      return true;
    });

    return total.get();
  }

  private Object[] convertKeys(final Object[] keys) {
    if (keys != null) {
      final byte[] keyTypes = mutable.keyTypes;
      final Object[] convertedKeys = new Object[keys.length];
      for (int i = 0; i < keys.length; ++i) {
        if (keys[i] == null)
          continue;
        convertedKeys[i] = Type.convert(mutable.getDatabase(), keys[i], BinaryTypes.getClassFromType(keyTypes[i]));
      }
      return convertedKeys;
    }
    return null;
  }
}
