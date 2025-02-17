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

import java.util.Map;
import java.util.Optional;

/**
 * Created by luigidellaquila on 08/05/17.
 */
public class DistributedExecutionStep extends AbstractExecutionStep {

  private final SelectExecutionPlan subExecuitonPlan;
  private final String              nodeName;

  private boolean inited;

  private ResultSet remoteResultSet;

  public DistributedExecutionStep(SelectExecutionPlan subExecutionPlan, String nodeName, CommandContext ctx,
      boolean profilingEnabled) {
    super(ctx, profilingEnabled);
    this.subExecuitonPlan = subExecutionPlan;
    this.nodeName = nodeName;
  }

  @Override
  public ResultSet syncPull(CommandContext ctx, int nRecords) throws TimeoutException {
    init(ctx);
    getPrev().ifPresent(x -> x.syncPull(ctx, nRecords));
    return new ResultSet() {
      @Override
      public boolean hasNext() {
        throw new UnsupportedOperationException("Implement distributed execution step!");
      }

      @Override
      public Result next() {
        throw new UnsupportedOperationException("Implement distributed execution step!");
      }

      @Override
      public void close() {
        DistributedExecutionStep.this.close();
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

  public void init(CommandContext ctx) {
    if (!inited) {
      inited = true;
      this.remoteResultSet = sendSerializedExecutionPlan(nodeName, subExecuitonPlan, ctx);
    }
  }

  private ResultSet sendSerializedExecutionPlan(String nodeName, ExecutionPlan serializedExecutionPlan, CommandContext ctx) {
    Database db = ctx.getDatabase();
    throw new UnsupportedOperationException();
//    return db.queryOnNode(nodeName, serializedExecutionPlan, ctx.getInputParameters());
  }

  @Override
  public void close() {
    super.close();
    if (this.remoteResultSet != null) {
      this.remoteResultSet.close();
    }
  }

  @Override
  public String prettyPrint(int depth, int indent) {
    StringBuilder builder = new StringBuilder();
    String ind = ExecutionStepInternal.getIndent(depth, indent);
    builder.append(ind);
    builder.append("+ EXECUTE ON NODE " + nodeName + "----------- \n");
    builder.append(subExecuitonPlan.prettyPrint(depth + 1, indent));
    builder.append("  ------------------------------------------- \n");
    builder.append("   |\n");
    builder.append("   V\n");
    return builder.toString();
  }
}
