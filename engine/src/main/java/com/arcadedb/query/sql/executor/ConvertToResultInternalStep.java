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

import com.arcadedb.database.Document;
import com.arcadedb.database.Record;
import com.arcadedb.exception.TimeoutException;

import java.util.Map;
import java.util.Optional;

/**
 * <p>
 * takes a result set made of OUpdatableRecord instances and transforms it in another result set made of normal OResultInternal
 * instances.
 * </p>
 * <p>This is the opposite of ConvertToUpdatableResultStep</p>
 *
 * @author Luigi Dell'Aquila (luigi.dellaquila-(at)-gmail.com)
 */
public class ConvertToResultInternalStep extends AbstractExecutionStep {
  private long cost = 0;

  ResultSet prevResult = null;

  public ConvertToResultInternalStep(CommandContext ctx, boolean profilingEnabled) {
    super(ctx, profilingEnabled);
  }

  @Override
  public ResultSet syncPull(CommandContext ctx, int nRecords) throws TimeoutException {
    if (!prev.isPresent()) {
      throw new IllegalStateException("filter step requires a previous step");
    }
    ExecutionStepInternal prevStep = prev.get();

    return new ResultSet() {
      public boolean finished = false;

      Result nextItem = null;
      int fetched = 0;

      private void fetchNextItem() {
        nextItem = null;
        if (finished) {
          return;
        }
        if (prevResult == null) {
          prevResult = prevStep.syncPull(ctx, nRecords);
          if (!prevResult.hasNext()) {
            finished = true;
            return;
          }
        }
        while (!finished) {
          while (!prevResult.hasNext()) {
            prevResult = prevStep.syncPull(ctx, nRecords);
            if (!prevResult.hasNext()) {
              finished = true;
              return;
            }
          }
          nextItem = prevResult.next();
          long begin = profilingEnabled ? System.nanoTime() : 0;
          try {
            if (nextItem instanceof UpdatableResult) {
              Record element = nextItem.getElement().get();
              if (element != null && element instanceof Document) {
                nextItem = new ResultInternal();
                ((ResultInternal) nextItem).setElement((Document) element);
              }
              break;
            }
          } finally {
            if (profilingEnabled) {
              cost += (System.nanoTime() - begin);
            }
          }
          nextItem = null;
        }
      }

      @Override
      public boolean hasNext() {
        if (fetched >= nRecords || finished) {
          return false;
        }
        if (nextItem == null) {
          fetchNextItem();
        }

        return nextItem != null;

      }

      @Override
      public Result next() {
        if (fetched >= nRecords || finished) {
          throw new IllegalStateException();
        }
        if (nextItem == null) {
          fetchNextItem();
        }
        if (nextItem == null) {
          throw new IllegalStateException();
        }
        Result result = nextItem;
        nextItem = null;
        fetched++;
        return result;
      }

      @Override
      public void close() {
        ConvertToResultInternalStep.this.close();
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
  public String prettyPrint(int depth, int indent) {
    String result = ExecutionStepInternal.getIndent(depth, indent) + "+ CONVERT TO REGULAR RESULT ITEM";
    if (profilingEnabled) {
      result += " (" + getCostFormatted() + ")";
    }
    return result;
  }

  @Override
  public long getCost() {
    return cost;
  }
}
