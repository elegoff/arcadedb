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

package com.arcadedb.database.async;

import com.arcadedb.database.Database;
import com.arcadedb.database.DatabaseInternal;
import com.arcadedb.exception.ConcurrentModificationException;

public class DatabaseAsyncTransaction extends DatabaseAsyncAbstractTask {
  public final Database.TransactionScope tx;
  public final int                       retries;
  private      OkCallback                onOkCallback;
  private      ErrorCallback             onErrorCallback;

  public DatabaseAsyncTransaction(final Database.TransactionScope tx, final int retries, final OkCallback okCallback, final ErrorCallback errorCallback) {
    this.tx = tx;
    this.retries = retries;
    this.onOkCallback = okCallback;
    this.onErrorCallback = errorCallback;
  }

  @Override
  public boolean requiresActiveTx() {
    return false;
  }

  @Override
  public void execute(final DatabaseAsyncExecutorImpl.AsyncThread async, final DatabaseInternal database) {
    ConcurrentModificationException lastException = null;

    if (database.isTransactionActive())
      database.commit();

    for (int retry = 0; retry < retries + 1; ++retry) {
      try {
        database.begin();
        tx.execute(database);
        database.commit();

        lastException = null;

        if (onOkCallback != null)
          onOkCallback.call();

        // OK
        break;

      } catch (ConcurrentModificationException e) {
        // RETRY
        lastException = e;

        continue;
      } catch (Exception e) {
        if (database.getTransaction().isActive())
          database.rollback();

        async.onError(e);

        if (onErrorCallback != null)
          onErrorCallback.call(e);

        throw e;
      }
    }

    if (lastException != null)
      async.onError(lastException);
  }

  @Override
  public String toString() {
    return "Transaction(" + tx + ")";
  }
}
