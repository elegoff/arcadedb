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

import com.arcadedb.exception.CommandExecutionException;
import com.arcadedb.query.sql.parser.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by luigidellaquila on 08/08/16.
 */
public class OUpdateExecutionPlanner {
  private final FromClause  target;
  public        WhereClause whereClause;

  protected boolean upsert = false;

  protected List<UpdateOperations> operations   = new ArrayList<UpdateOperations>();
  protected boolean                returnBefore = false;
  protected boolean                returnAfter  = false;
  protected boolean                returnCount  = false;

  protected boolean updateEdge = false;

  protected Projection returnProjection;

//  public OStorage.LOCKING_STRATEGY lockRecord = null;

  public Limit   limit;
  public Timeout timeout;

  public OUpdateExecutionPlanner(UpdateStatement oUpdateStatement) {
    if (oUpdateStatement instanceof UpdateEdgeStatement) {
      updateEdge = true;
    }
    this.target = oUpdateStatement.getTarget().copy();
    this.whereClause = oUpdateStatement.getWhereClause() == null ? null : oUpdateStatement.getWhereClause().copy();
    this.operations = oUpdateStatement.getOperations() == null ?
        null :
        oUpdateStatement.getOperations().stream().map(x -> x.copy()).collect(Collectors.toList());
    this.upsert = oUpdateStatement.isUpsert();

    this.returnBefore = oUpdateStatement.isReturnBefore();
    this.returnAfter = oUpdateStatement.isReturnAfter();
    this.returnCount = !(returnAfter || returnBefore);
    this.returnProjection = oUpdateStatement.getReturnProjection() == null ? null : oUpdateStatement.getReturnProjection().copy();
//    this.lockRecord = oUpdateStatement.getLockRecord();
    this.limit = oUpdateStatement.getLimit() == null ? null : oUpdateStatement.getLimit().copy();
    this.timeout = oUpdateStatement.getTimeout() == null ? null : oUpdateStatement.getTimeout().copy();
  }

  public UpdateExecutionPlan createExecutionPlan(CommandContext ctx, boolean enableProfiling) {
    UpdateExecutionPlan result = new UpdateExecutionPlan(ctx);

    handleTarget(result, ctx, this.target, this.whereClause, this.timeout, enableProfiling);
    if(updateEdge){
      result.chain(new CheckRecordTypeStep(ctx, "E", enableProfiling));
    }
    handleUpsert(result, ctx, this.target, this.whereClause, this.upsert, enableProfiling);
    handleTimeout(result, ctx, this.timeout, enableProfiling);
    convertToModifiableResult(result, ctx, enableProfiling);
    handleLimit(result, ctx, this.limit, enableProfiling);
    handleReturnBefore(result, ctx, this.returnBefore, enableProfiling);
    handleOperations(result, ctx, this.operations, enableProfiling);
//    handleLock(result, ctx, this.lockRecord);
    handleSave(result, ctx, enableProfiling);
    handleResultForReturnBefore(result, ctx, this.returnBefore, returnProjection, enableProfiling);
    handleResultForReturnAfter(result, ctx, this.returnAfter, returnProjection, enableProfiling);
    handleResultForReturnCount(result, ctx, this.returnCount, enableProfiling);
    return result;
  }

  /**
   * add a step that transforms a normal OResult in a specific object that under setProperty() updates the actual PIdentifiable
   *
   * @param plan the execution plan
   * @param ctx  the executino context
   */
  private void convertToModifiableResult(UpdateExecutionPlan plan, CommandContext ctx, boolean profilingEnabled) {
    plan.chain(new ConvertToUpdatableResultStep(ctx, profilingEnabled));
  }

  private void handleResultForReturnCount(UpdateExecutionPlan result, CommandContext ctx, boolean returnCount, boolean profilingEnabled) {
    if (returnCount) {
      result.chain(new CountStep(ctx, profilingEnabled));
    }
  }

  private void handleResultForReturnAfter(UpdateExecutionPlan result, CommandContext ctx, boolean returnAfter,
      Projection returnProjection, boolean profilingEnabled) {
    if (returnAfter) {
      //re-convert to normal step
      result.chain(new ConvertToResultInternalStep(ctx, profilingEnabled));
      if (returnProjection != null) {
        result.chain(new ProjectionCalculationStep(returnProjection, ctx, profilingEnabled));
      }
    }
  }

  private void handleResultForReturnBefore(UpdateExecutionPlan result, CommandContext ctx, boolean returnBefore,
      Projection returnProjection, boolean profilingEnabled) {
    if (returnBefore) {
      result.chain(new UnwrapPreviousValueStep(ctx, profilingEnabled));
      if (returnProjection != null) {
        result.chain(new ProjectionCalculationStep(returnProjection, ctx, profilingEnabled));
      }
    }
  }

  private void handleSave(UpdateExecutionPlan result, CommandContext ctx, boolean profilingEnabled) {
    result.chain(new SaveElementStep(ctx, profilingEnabled));
  }

  private void handleTimeout(UpdateExecutionPlan result, CommandContext ctx, Timeout timeout, boolean profilingEnabled) {
    if (timeout != null && timeout.getVal().longValue() > 0) {
      result.chain(new TimeoutStep(timeout, ctx, profilingEnabled));
    }
  }

  private void handleReturnBefore(UpdateExecutionPlan result, CommandContext ctx, boolean returnBefore, boolean profilingEnabled) {
    if (returnBefore) {
      result.chain(new CopyRecordContentBeforeUpdateStep(ctx, profilingEnabled));
    }
  }

//  private void handleLock(OUpdateExecutionPlan result, OCommandContext ctx, OStorage.LOCKING_STRATEGY lockRecord) {
//
//  }

  private void handleLimit(UpdateExecutionPlan plan, CommandContext ctx, Limit limit, boolean profilingEnabled) {
    if (limit != null) {
      plan.chain(new LimitExecutionStep(limit, ctx, profilingEnabled));
    }
  }

  private void handleUpsert(UpdateExecutionPlan plan, CommandContext ctx, FromClause target, WhereClause where,
      boolean upsert, boolean profilingEnabled) {
    if (upsert) {
      plan.chain(new UpsertStep(target, where, ctx, profilingEnabled));
    }
  }

  private void handleOperations(UpdateExecutionPlan plan, CommandContext ctx, List<UpdateOperations> ops, boolean profilingEnabled) {
    if (ops != null) {
      for (UpdateOperations op : ops) {
        switch (op.getType()) {
        case UpdateOperations.TYPE_SET:
          plan.chain(new UpdateSetStep(op.getUpdateItems(), ctx, profilingEnabled));
          if(updateEdge){
            plan.chain(new UpdateEdgePointersStep( ctx, profilingEnabled));
          }
          break;
        case UpdateOperations.TYPE_REMOVE:
          plan.chain(new UpdateRemoveStep(op.getUpdateRemoveItems(), ctx, profilingEnabled));
          break;
        case UpdateOperations.TYPE_MERGE:
          plan.chain(new UpdateMergeStep(op.getJson(), ctx, profilingEnabled));
          break;
        case UpdateOperations.TYPE_CONTENT:
          plan.chain(new UpdateContentStep(op.getJson(), ctx, profilingEnabled));
          break;
        case UpdateOperations.TYPE_PUT:
        case UpdateOperations.TYPE_INCREMENT:
        case UpdateOperations.TYPE_ADD:
          throw new CommandExecutionException("Cannot execute with UPDATE PUT/ADD/INCREMENT new executor: " + op);
        }
      }
    }
  }

  private void handleTarget(UpdateExecutionPlan result, CommandContext ctx, FromClause target, WhereClause whereClause,
      Timeout timeout, boolean profilingEnabled) {
    SelectStatement sourceStatement = new SelectStatement(-1);
    sourceStatement.setTarget(target);
    sourceStatement.setWhereClause(whereClause);
    if (timeout != null) {
      sourceStatement.setTimeout(this.timeout.copy());
    }
    OSelectExecutionPlanner planner = new OSelectExecutionPlanner(sourceStatement);
    result.chain(new SubQueryStep(planner.createExecutionPlan(ctx, profilingEnabled), ctx, ctx, profilingEnabled));
  }
}
