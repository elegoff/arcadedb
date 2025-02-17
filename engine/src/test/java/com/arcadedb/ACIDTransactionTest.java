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

package com.arcadedb;

import com.arcadedb.database.Database;
import com.arcadedb.database.DatabaseInternal;
import com.arcadedb.database.Document;
import com.arcadedb.database.MutableDocument;
import com.arcadedb.database.async.ErrorCallback;
import com.arcadedb.engine.WALException;
import com.arcadedb.engine.WALFile;
import com.arcadedb.exception.TransactionException;
import com.arcadedb.log.LogManager;
import com.arcadedb.schema.DocumentType;
import com.arcadedb.schema.Schema;
import com.arcadedb.schema.Type;
import com.arcadedb.query.sql.executor.ResultSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public class ACIDTransactionTest extends TestHelper {
  @Override
  protected void beginTest() {
    database.transaction(new Database.TransactionScope() {
      @Override
      public void execute(Database database) {
        if (!database.getSchema().existsType("V")) {
          final DocumentType v = database.getSchema().createDocumentType("V");

          v.createProperty("id", Integer.class);
          v.createProperty("name", String.class);
          v.createProperty("surname", String.class);
        }
      }
    });
  }

  @Test
  public void testAsyncTX() {
    final Database db = database;

    db.async().setTransactionSync(WALFile.FLUSH_TYPE.YES_NOMETADATA);
    db.async().setTransactionUseWAL(true);
    db.async().setCommitEvery(1);

    final int TOT = 1000;

    final AtomicInteger total = new AtomicInteger(0);

    try {
      for (; total.get() < TOT; total.incrementAndGet()) {
        final MutableDocument v = db.newDocument("V");
        v.set("id", total.get());
        v.set("name", "Crash");
        v.set("surname", "Test");

        db.async().createRecord(v, null);
      }

      db.async().waitCompletion();

      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        // IGNORE IT
      }

    } catch (TransactionException e) {
      Assertions.assertTrue(e.getCause() instanceof IOException);
    }

    ((DatabaseInternal) db).kill();

    verifyWALFilesAreStillPresent();

    verifyDatabaseWasNotClosedProperly();

    database.transaction(new Database.TransactionScope() {
      @Override
      public void execute(Database database) {
        Assertions.assertEquals(TOT, database.countType("V", true));
      }
    });
  }

  @Test
  public void testCrashDuringTx() {
    final Database db = database;
    db.begin();
    try {
      final MutableDocument v = db.newDocument("V");
      v.set("id", 0);
      v.set("name", "Crash");
      v.set("surname", "Test");
      v.save();

    } finally {
      ((DatabaseInternal) db).kill();
    }

    verifyDatabaseWasNotClosedProperly();

    database.transaction(new Database.TransactionScope() {
      @Override
      public void execute(Database database) {
        Assertions.assertEquals(0, database.countType("V", true));
      }
    });
  }

  @Test
  public void testIOExceptionAfterWALIsWritten() {
    final Database db = database;
    db.begin();

    try {
      final MutableDocument v = db.newDocument("V");
      v.set("id", 0);
      v.set("name", "Crash");
      v.set("surname", "Test");
      v.save();

      ((DatabaseInternal) db).registerCallback(DatabaseInternal.CALLBACK_EVENT.TX_AFTER_WAL_WRITE, new Callable<Void>() {
        @Override
        public Void call() throws IOException {
          throw new IOException("Test IO Exception");
        }
      });

      db.commit();

      Assertions.fail("Expected commit to fail");

    } catch (TransactionException e) {
      Assertions.assertTrue(e.getCause() instanceof WALException);
    }
    ((DatabaseInternal) db).kill();

    verifyWALFilesAreStillPresent();

    verifyDatabaseWasNotClosedProperly();

    database.transaction(new Database.TransactionScope() {
      @Override
      public void execute(Database database) {
        Assertions.assertEquals(1, database.countType("V", true));
      }
    });
  }

  @Test
  public void testAsyncIOExceptionAfterWALIsWrittenLastRecords() {
    final Database db = database;

    final AtomicInteger errors = new AtomicInteger(0);

    db.async().setTransactionSync(WALFile.FLUSH_TYPE.YES_NOMETADATA);
    db.async().setTransactionUseWAL(true);
    db.async().setCommitEvery(1);
    db.async().onError(new ErrorCallback() {
      @Override
      public void call(Throwable exception) {
        errors.incrementAndGet();
      }
    });

    final int TOT = 1000;

    final AtomicInteger total = new AtomicInteger(0);
    final AtomicInteger commits = new AtomicInteger(0);

    try {
      ((DatabaseInternal) db).registerCallback(DatabaseInternal.CALLBACK_EVENT.TX_AFTER_WAL_WRITE, new Callable<Void>() {
        @Override
        public Void call() throws IOException {
          if (commits.incrementAndGet() > TOT - 1) {
            LogManager.instance().log(this, Level.INFO, "TEST: Causing IOException at commit %d...", null, commits.get());
            throw new IOException("Test IO Exception");
          }
          return null;
        }
      });

      for (; total.get() < TOT; total.incrementAndGet()) {
        final MutableDocument v = db.newDocument("V");
        v.set("id", 0);
        v.set("name", "Crash");
        v.set("surname", "Test");

        db.async().createRecord(v, null);
      }

      db.async().waitCompletion();

      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        // IGNORE IT
      }

      Assertions.assertEquals(1, errors.get());

    } catch (TransactionException e) {
      Assertions.assertTrue(e.getCause() instanceof IOException);
    }
    ((DatabaseInternal) db).kill();

    verifyWALFilesAreStillPresent();

    verifyDatabaseWasNotClosedProperly();

    database.transaction(new Database.TransactionScope() {
      @Override
      public void execute(Database database) {
        Assertions.assertEquals(TOT, database.countType("V", true));
      }
    });
  }

  @Test
  public void testAsyncIOExceptionAfterWALIsWrittenManyRecords() {
    final Database db = database;

    final int TOT = 100000;

    final AtomicInteger total = new AtomicInteger(0);

    final AtomicInteger errors = new AtomicInteger(0);

    db.async().setTransactionSync(WALFile.FLUSH_TYPE.YES_NOMETADATA);
    db.async().setTransactionUseWAL(true);
    db.async().setCommitEvery(1000000);
    db.async().onError(new ErrorCallback() {
      @Override
      public void call(Throwable exception) {
        errors.incrementAndGet();
      }
    });

    try {
      ((DatabaseInternal) db).registerCallback(DatabaseInternal.CALLBACK_EVENT.TX_AFTER_WAL_WRITE, new Callable<Void>() {
        @Override
        public Void call() throws IOException {
          if (total.incrementAndGet() > TOT - 10)
            throw new IOException("Test IO Exception");
          return null;
        }
      });

      for (; total.get() < TOT; total.incrementAndGet()) {
        final MutableDocument v = db.newDocument("V");
        v.set("id", 0);
        v.set("name", "Crash");
        v.set("surname", "Test");

        db.async().createRecord(v, null);
      }

      db.async().waitCompletion();

      Assertions.assertTrue(errors.get() > 0);

    } catch (TransactionException e) {
      Assertions.assertTrue(e.getCause() instanceof IOException);
    }
    ((DatabaseInternal) db).kill();

    verifyWALFilesAreStillPresent();

    verifyDatabaseWasNotClosedProperly();

    database.transaction(new Database.TransactionScope() {
      @Override
      public void execute(Database database) {
        Assertions.assertEquals(TOT, database.countType("V", true));
      }
    });
  }

  @Test
  public void multiThreadConcurrentTransactions() {
    database.transaction((tx) -> {
      final DocumentType type = database.getSchema().createDocumentType("Stock");
      type.createProperty("symbol", Type.STRING);
      type.createProperty("date", Type.DATETIME);
      type.createProperty("history", Type.LIST);
      type.createTypeIndex(Schema.INDEX_TYPE.LSM_TREE, true, new String[] { "symbol", "date" });

      final DocumentType type2 = database.getSchema().createDocumentType("Aggregate", 1);
      type2.createProperty("volume", Type.LONG);
    });

    final int TOT_STOCKS = 100;
    final int TOT_DAYS = 150;
    final int TOT_MINS = 400;

    final Calendar startingDay = Calendar.getInstance();
    for (int i = 0; i < TOT_DAYS; ++i)
      startingDay.add(Calendar.DAY_OF_YEAR, -1);

    final AtomicInteger errors = new AtomicInteger();

    for (int stockId = 0; stockId < TOT_STOCKS; ++stockId) {
      final int id = stockId;

      database.async().transaction((tx) -> {
        try {
          final Calendar now = Calendar.getInstance();
          now.setTimeInMillis(startingDay.getTimeInMillis());

          for (int i = 0; i < TOT_DAYS; ++i) {
            final MutableDocument stock = database.newDocument("Stock");

            stock.set("symbol", "" + id);
            stock.set("date", now.getTimeInMillis());

            final List<Document> history = new ArrayList<>();
            for (int e = 0; e < TOT_MINS; ++e) {
              final MutableDocument embedded = ((DatabaseInternal) database).newEmbeddedDocument(null, "Aggregate");
              embedded.set("volume", 1_000_000l);
              history.add(embedded);
            }

            stock.set("history", history);
            stock.save();

//            LogManager.instance().log(this, Level.INFO, "- Saved stockId=%d date=%d", null, id, now.getTimeInMillis());

            now.add(Calendar.DAY_OF_YEAR, +1);
          }

          //LogManager.instance().log(this, Level.INFO, "Finished stockId=%d", null, id);
        } catch (Exception e) {
          errors.incrementAndGet();
          LogManager.instance().log(this, Level.SEVERE, "Error on saving stockId=%d", e, id);
        }
      });
    }

    database.async().waitCompletion();

    Assertions.assertEquals(0, errors.get());

    database.transaction((tx) -> {
      Assertions.assertEquals(TOT_STOCKS * TOT_DAYS, database.countType("Stock", true));
      Assertions.assertEquals(0, database.countType("Aggregate", true));

      final Calendar now = Calendar.getInstance();
      now.setTimeInMillis(startingDay.getTimeInMillis());

      for (int i = 0; i < TOT_DAYS; ++i) {
        for (int stockId = 0; stockId < TOT_STOCKS; ++stockId) {
          final ResultSet result = database.query("sql", "select from Stock where symbol = ? and date = ?", "" + stockId, now.getTimeInMillis());
          Assertions.assertNotNull(result);
          Assertions.assertTrue(result.hasNext(), "Cannot find stock=" + stockId + " date=" + now.getTimeInMillis());
        }
        now.add(Calendar.DAY_OF_YEAR, +1);
      }
    });
  }

  private void verifyDatabaseWasNotClosedProperly() {
    final AtomicBoolean dbNotClosedCaught = new AtomicBoolean(false);

    factory.registerCallback(DatabaseInternal.CALLBACK_EVENT.DB_NOT_CLOSED, new Callable<Void>() {
      @Override
      public Void call() {
        dbNotClosedCaught.set(true);
        return null;
      }
    });

    database = factory.open();
    Assertions.assertTrue(dbNotClosedCaught.get());
  }

  private void verifyWALFilesAreStillPresent() {
    File dbDir = new File(getDatabasePath());
    Assertions.assertTrue(dbDir.exists());
    Assertions.assertTrue(dbDir.isDirectory());
    File[] files = dbDir.listFiles(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return name.endsWith("wal");
      }
    });
    Assertions.assertTrue(files.length > 0);
  }
}
