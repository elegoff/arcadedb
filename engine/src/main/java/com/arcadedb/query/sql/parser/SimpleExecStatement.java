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

package com.arcadedb.query.sql.parser;

import com.arcadedb.database.Database;
import com.arcadedb.query.sql.executor.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Superclass for SQL statements that are too simple to deserve an execution planner.
 * All the execution is delegated to the statement itself, with the execute(ctx) method.
 *
 * @author Luigi Dell'Aquila (luigi.dellaquila-(at)-gmail.com)
 */
public abstract class SimpleExecStatement extends Statement {

  public SimpleExecStatement(int id) {
    super(id);
  }

  public SimpleExecStatement(SqlParser p, int id) {
    super(p, id);
  }

  public abstract ResultSet executeSimple(CommandContext ctx);

  public ResultSet execute(Database db, Object[] args, CommandContext parentContext, boolean usePlanCache) {
    BasicCommandContext ctx = new BasicCommandContext();
    if (parentContext != null) {
      ctx.setParentWithoutOverridingChild(parentContext);
    }
    ctx.setDatabase(db);
    Map<Object, Object> params = new HashMap<>();
    if (args != null) {
      for (int i = 0; i < args.length; i++) {
        params.put(i, args[i]);
      }
    }
    ctx.setInputParameters(params);
    SingleOpExecutionPlan executionPlan = (SingleOpExecutionPlan) createExecutionPlan(ctx, false);
    return executionPlan.executeInternal(ctx);
  }

  public ResultSet execute(Database db, Map params, CommandContext parentContext, boolean usePlanCache) {
    BasicCommandContext ctx = new BasicCommandContext();
    if (parentContext != null) {
      ctx.setParentWithoutOverridingChild(parentContext);
    }
    ctx.setDatabase(db);
    ctx.setInputParameters(params);
    SingleOpExecutionPlan executionPlan = (SingleOpExecutionPlan) createExecutionPlan(ctx, false);
    return executionPlan.executeInternal(ctx);
  }

  public InternalExecutionPlan createExecutionPlan(CommandContext ctx, boolean enableProfiling) {
    return new SingleOpExecutionPlan(ctx, this);
  }

}
