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

/**
 * Created by luigidellaquila on 08/08/16.
 */

import java.util.Collections;
import java.util.List;

/**
 * @author Luigi Dell'Aquila (luigi.dellaquila-(at)-gmail.com)
 */
public class IfExecutionPlan implements InternalExecutionPlan {

  private String location;

  private final CommandContext ctx;

  protected IfStep step;

  public IfExecutionPlan(CommandContext ctx) {
    this.ctx = ctx;
  }

  @Override public void reset(CommandContext ctx) {
    //TODO
    throw new UnsupportedOperationException();
  }

  @Override public void close() {
    step.close();
  }

  @Override public ResultSet fetchNext(int n) {
    return step.syncPull(ctx, n);
  }

  @Override public String prettyPrint(int depth, int indent) {
    StringBuilder result = new StringBuilder();
    result.append(step.prettyPrint(depth, indent));
    return result.toString();
  }

  public void chain(IfStep step) {
    this.step = step;
  }

  @Override public List<ExecutionStep> getSteps() {
    //TODO do a copy of the steps
    return Collections.singletonList(step);
  }

  public void setSteps(List<ExecutionStepInternal> steps) {
    this.step = (IfStep) steps.get(0);
  }

  @Override public Result toResult() {
    ResultInternal result = new ResultInternal();
    result.setProperty("type", "IfExecutionPlan");
    result.setProperty("javaType", getClass().getName());
    result.setProperty("cost", getCost());
    result.setProperty("prettyPrint", prettyPrint(0, 2));
    result.setProperty("steps", Collections.singletonList(step.toResult()));
    return result;
  }

  @Override public long getCost() {
    return 0l;
  }

  @Override
  public boolean canBeCached() {
    return false;
  }

  public boolean containsReturn() {
    return step.getPositivePlan().containsReturn() || step.getNegativePlan() != null && step.getPositivePlan().containsReturn();
  }

  public ExecutionStepInternal executeUntilReturn() {
    step.init(ctx);
    if (step.condition.evaluate(new ResultInternal(), ctx)) {
      step.initPositivePlan(ctx);
      return step.positivePlan.executeUntilReturn();
    } else {
      step.initNegativePlan(ctx);
      if (step.negativePlan != null) {
        return step.negativePlan.executeUntilReturn();
      }
    }
    return null;
  }
}
