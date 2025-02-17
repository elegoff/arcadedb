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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Luigi Dell'Aquila (luigi.dellaquila-(at)-gmail.com)
 */
public class ScriptExecutionPlan implements InternalExecutionPlan {

  private       String                location;
  private final CommandContext        ctx;
  private       boolean               executed    = false;
  protected     List<ScriptLineStep>  steps       = new ArrayList<>();
  private       ExecutionStepInternal lastStep    = null;
  private       ResultSet             finalResult = null;
  private       String                statement;

  public ScriptExecutionPlan(CommandContext ctx) {
    this.ctx = ctx;
  }

  @Override
  public void reset(CommandContext ctx) {
    // TODO
    throw new UnsupportedOperationException();
  }

  @Override
  public void close() {
    lastStep.close();
  }

  @Override
  public ResultSet fetchNext(int n) {
    dexecute(n);
    return new ResultSet() {
      private int totalFetched = 0;

      @Override
      public boolean hasNext() {
        return finalResult.hasNext() && totalFetched < n;
      }

      @Override
      public Result next() {
        if (!hasNext()) {
          throw new IllegalStateException();
        }
        return finalResult.next();
      }

      @Override
      public void close() {
        finalResult.close();
      }

      @Override
      public Optional<ExecutionPlan> getExecutionPlan() {
        return finalResult == null ? Optional.empty() : finalResult.getExecutionPlan();
      }

      @Override
      public Map<String, Long> getQueryStats() {
        return null;
      }
    };
  }

  private void dexecute(int n) {
    if (!executed) {
      executeUntilReturn();
      executed = true;
      finalResult = new InternalResultSet();
      ResultSet partial = lastStep.syncPull(ctx, n);
      while (partial.hasNext()) {
        while (partial.hasNext()) {
          ((InternalResultSet) finalResult).add(partial.next());
        }
        partial = lastStep.syncPull(ctx, n);
      }
      if (lastStep instanceof ScriptLineStep) {
        ((InternalResultSet) finalResult).setPlan(((ScriptLineStep) lastStep).plan);
      }
    }
  }

  @Override
  public String prettyPrint(int depth, int indent) {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < steps.size(); i++) {
      ExecutionStepInternal step = steps.get(i);
      result.append(step.prettyPrint(depth, indent));
      if (i < steps.size() - 1) {
        result.append("\n");
      }
    }
    return result.toString();
  }

  public void chain(InternalExecutionPlan nextPlan, boolean profilingEnabled) {
    ScriptLineStep lastStep = steps.size() == 0 ? null : steps.get(steps.size() - 1);
    ScriptLineStep nextStep = new ScriptLineStep(nextPlan, ctx, profilingEnabled);
    if (lastStep != null) {
      lastStep.setNext(nextStep);
      nextStep.setPrevious(lastStep);
    }
    steps.add(nextStep);
    this.lastStep = nextStep;
  }

  @Override
  public List<ExecutionStep> getSteps() {
    // TODO do a copy of the steps
    return (List) steps;
  }

  public void setSteps(List<ExecutionStepInternal> steps) {
    this.steps = (List) steps;
  }

  @Override
  public Result toResult() {
    ResultInternal result = new ResultInternal();
    result.setProperty("type", "ScriptExecutionPlan");
    result.setProperty("javaType", getClass().getName());
    result.setProperty("cost", getCost());
    result.setProperty("prettyPrint", prettyPrint(0, 2));
    result.setProperty("steps", steps == null ? null : steps.stream().map(x -> x.toResult()).collect(Collectors.toList()));
    return result;
  }

  @Override
  public long getCost() {
    return 0l;
  }

  @Override
  public boolean canBeCached() {
    return false;
  }

  public boolean containsReturn() {
    for (ExecutionStepInternal step : steps) {
      if (step instanceof ReturnStep) {
        return true;
      }
      if (step instanceof ScriptLineStep) {
        return ((ScriptLineStep) step).containsReturn();
      }
    }

    return false;
  }

  /**
   * executes all the script and returns last statement execution step, so that it can be executed
   * from outside
   *
   * @return
   */
  public ExecutionStepInternal executeUntilReturn() {
    if (steps.size() > 0) {
      lastStep = steps.get(steps.size() - 1);
    }
    for (int i = 0; i < steps.size() - 1; i++) {
      ScriptLineStep step = steps.get(i);
      if (step.containsReturn()) {
        ExecutionStepInternal returnStep = step.executeUntilReturn(ctx);
        if (returnStep != null) {
          lastStep = returnStep;
          return lastStep;
        }
      }
      ResultSet lastResult = step.syncPull(ctx, 100);

      while (lastResult.hasNext()) {
        while (lastResult.hasNext()) {
          lastResult.next();
        }
        lastResult = step.syncPull(ctx, 100);
      }
    }
    this.lastStep = steps.get(steps.size() - 1);
    return lastStep;
  }

  /**
   * executes the whole script and returns last statement ONLY if it's a RETURN, otherwise it
   * returns null;
   *
   * @return
   */
  public ExecutionStepInternal executeFull() {
    for (int i = 0; i < steps.size(); i++) {
      ScriptLineStep step = steps.get(i);
      if (step.containsReturn()) {
        ExecutionStepInternal returnStep = step.executeUntilReturn(ctx);
        if (returnStep != null) {
          return returnStep;
        }
      }
      ResultSet lastResult = step.syncPull(ctx, 100);

      while (lastResult.hasNext()) {
        while (lastResult.hasNext()) {
          lastResult.next();
        }
        lastResult = step.syncPull(ctx, 100);
      }
    }

    return null;
  }

  @Override
  public String getStatement() {
    return statement;
  }

  @Override
  public void setStatement(String statement) {
    this.statement = statement;
  }
}
