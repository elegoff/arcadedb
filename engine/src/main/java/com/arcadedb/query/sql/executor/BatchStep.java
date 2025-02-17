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

package com.arcadedb.query.sql.executor;

import com.arcadedb.database.Database;
import com.arcadedb.exception.TimeoutException;
import com.arcadedb.query.sql.parser.Batch;

import java.util.Map;
import java.util.Optional;

/**
 * Created by luigidellaquila on 14/02/17.
 */
public class BatchStep extends AbstractExecutionStep {
  Integer batchSize;

  int count = 0;

  public BatchStep(Batch batch, CommandContext ctx, boolean profilingEnabled) {
    super(ctx, profilingEnabled);
    batchSize = batch.evaluate(ctx);
  }

  @Override
  public ResultSet syncPull(CommandContext ctx, int nRecords) throws TimeoutException {
    ResultSet prevResult = getPrev().get().syncPull(ctx, nRecords);
    return new ResultSet() {
      @Override
      public boolean hasNext() {
        return prevResult.hasNext();
      }

      @Override
      public Result next() {
        Result res = prevResult.next();
        if (count % batchSize == 0) {
          Database db = ctx.getDatabase();
          if (db.getTransaction().isActive()) {
            db.commit();
            db.begin();
          }
        }
        count++;
        return res;
      }

      @Override
      public void close() {
        getPrev().get().close();
      }

      @Override
      public Optional<ExecutionPlan> getExecutionPlan() {
        return null;
      }

      @Override
      public Map<String, Long> getQueryStats() {
        return null;
      }
    };
  }

  @Override
  public void reset() {
    this.count = 0;
  }

  @Override
  public String prettyPrint(int depth, int indent) {
    String spaces = ExecutionStepInternal.getIndent(depth, indent);
    StringBuilder result = new StringBuilder();
    result.append(spaces);
    result.append("+ BATCH COMMIT EVERY " + batchSize);
    return result.toString();
  }
}
