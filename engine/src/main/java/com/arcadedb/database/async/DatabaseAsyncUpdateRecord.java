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

import com.arcadedb.database.DatabaseInternal;
import com.arcadedb.database.Record;
import com.arcadedb.log.LogManager;

import java.util.logging.Level;

public class DatabaseAsyncUpdateRecord extends DatabaseAsyncAbstractTask {
  public final Record                record;
  public final UpdatedRecordCallback callback;

  public DatabaseAsyncUpdateRecord(final Record record, final UpdatedRecordCallback callback) {
    this.record = record;
    this.callback = callback;
  }

  @Override
  public void execute(DatabaseAsyncExecutorImpl.AsyncThread async, DatabaseInternal database) {
    try {
      database.updateRecordNoLock(record);

      if (callback != null)
        callback.call(record);

    } catch (Exception e) {
      LogManager.instance().log(this, Level.SEVERE, "Error on executing async update operation (threadId=%d)", e, Thread.currentThread().getId());

      async.onError(e);
    }
  }

  @Override
  public String toString() {
    return "UpdateRecord(" + record + ")";
  }
}
