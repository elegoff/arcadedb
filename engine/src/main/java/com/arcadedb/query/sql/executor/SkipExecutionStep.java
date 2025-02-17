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

import com.arcadedb.query.sql.parser.Skip;

/**
 * Created by luigidellaquila on 08/07/16.
 */
public class SkipExecutionStep extends AbstractExecutionStep {
  private final Skip skip;

  int skipped = 0;

  ResultSet lastFetch;
  private boolean finished;

  public SkipExecutionStep(Skip skip, CommandContext ctx, boolean profilingEnabled) {
    super(ctx, profilingEnabled);
    this.skip = skip;
  }

  @Override public ResultSet syncPull(CommandContext ctx, int nRecords) {
    if (finished == true) {
      return new InternalResultSet();//empty
    }
    int skipValue = skip.getValue(ctx);
    while (skipped < skipValue) {
      //fetch and discard
      ResultSet rs = prev.get().syncPull(ctx, Math.min(100, skipValue - skipped));//fetch blocks of 100, at most
      if (!rs.hasNext()) {
        finished = true;
        return new InternalResultSet();//empty
      }
      while (rs.hasNext()) {
        rs.next();
        skipped++;
      }
    }

    return prev.get().syncPull(ctx, nRecords);

  }

  @Override public void sendTimeout() {

  }

  @Override public void close() {
    prev.ifPresent(x -> x.close());
  }

  @Override public String prettyPrint(int depth, int indent) {
    return ExecutionStepInternal.getIndent(depth, indent) + "+ SKIP (" + skip.toString() + ")";
  }

}
