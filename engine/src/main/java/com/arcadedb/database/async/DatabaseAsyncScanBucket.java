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
import com.arcadedb.database.Document;
import com.arcadedb.database.DocumentCallback;
import com.arcadedb.database.Record;
import com.arcadedb.engine.Bucket;

import java.util.concurrent.CountDownLatch;

public class DatabaseAsyncScanBucket extends DatabaseAsyncAbstractTask {
  public final CountDownLatch   semaphore;
  public final DocumentCallback userCallback;
  public final Bucket           bucket;

  public DatabaseAsyncScanBucket(final CountDownLatch semaphore, final DocumentCallback userCallback, final Bucket bucket) {
    this.semaphore = semaphore;
    this.userCallback = userCallback;
    this.bucket = bucket;
  }

  @Override
  public void execute(final DatabaseAsyncExecutorImpl.AsyncThread async, final DatabaseInternal database) {
    try {
      bucket.scan((rid, view) -> {
        if (async.isShutdown())
          return false;

        final Record record = database.getRecordFactory()
            .newImmutableRecord(database, database.getSchema().getType(database.getSchema().getTypeNameByBucketId(rid.getBucketId())), rid, view, null);

        return userCallback.onRecord((Document) record);
      });
    } finally {
      // UNLOCK THE CALLER THREAD
      semaphore.countDown();
    }
  }

  @Override
  public String toString() {
    return "ScanBucket(" + bucket + ")";
  }
}
