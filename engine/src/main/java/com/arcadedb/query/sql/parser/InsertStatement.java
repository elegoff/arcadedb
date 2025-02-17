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

/* Generated By:JJTree: Do not edit this line. OInsertStatement.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_USERTYPE_VISIBILITY_PUBLIC=true */
package com.arcadedb.query.sql.parser;

import com.arcadedb.database.Database;
import com.arcadedb.query.sql.executor.*;

import java.util.HashMap;
import java.util.Map;

public class InsertStatement extends Statement {

  Identifier      targetType;
  Identifier      targetBucketName;
  Bucket          targetBucket;
  IndexIdentifier targetIndex;
  InsertBody      insertBody;
  Projection      returnStatement;
  SelectStatement selectStatement;
  boolean         selectInParentheses = false;
  boolean         selectWithFrom      = false;
  boolean         unsafe              = false;

  public InsertStatement(int id) {
    super(id);
  }

  public InsertStatement(SqlParser p, int id) {
    super(p, id);
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append("INSERT INTO ");
    if (targetType != null) {
      targetType.toString(params, builder);
      if (targetBucketName != null) {
        builder.append(" BUCKET ");
        targetBucketName.toString(params, builder);
      }
    }
    if (targetBucket != null) {
      targetBucket.toString(params, builder);
    }
    if (targetIndex != null) {
      targetIndex.toString(params, builder);
    }
    if (insertBody != null) {
      builder.append(" ");
      insertBody.toString(params, builder);
    }
    if (returnStatement != null) {
      builder.append(" RETURN ");
      returnStatement.toString(params, builder);
    }
    if (selectStatement != null) {
      builder.append(" ");
      if (selectWithFrom) {
        builder.append("FROM ");
      }
      if (selectInParentheses) {
        builder.append("(");
      }
      selectStatement.toString(params, builder);
      if (selectInParentheses) {
        builder.append(")");
      }

    }
    if (unsafe) {
      builder.append(" UNSAFE");
    }
  }

  @Override
  public InsertStatement copy() {
    InsertStatement result = new InsertStatement(-1);
    result.targetType = targetType == null ? null : targetType.copy();
    result.targetBucketName = targetBucketName == null ? null : targetBucketName.copy();
    result.targetBucket = targetBucket == null ? null : targetBucket.copy();
    result.targetIndex = targetIndex == null ? null : targetIndex.copy();
    result.insertBody = insertBody == null ? null : insertBody.copy();
    result.returnStatement = returnStatement == null ? null : returnStatement.copy();
    result.selectStatement = selectStatement == null ? null : selectStatement.copy();
    result.selectInParentheses = selectInParentheses;
    result.selectWithFrom = selectWithFrom;
    result.unsafe = unsafe;
    return result;
  }

  @Override
  public ResultSet execute(Database db, Object[] args, CommandContext parentCtx, boolean usePlanCache) {
    BasicCommandContext ctx = new BasicCommandContext();
    if (parentCtx != null) {
      ctx.setParentWithoutOverridingChild(parentCtx);
    }
    ctx.setDatabase(db);
    Map<Object, Object> params = new HashMap<>();
    if (args != null) {
      for (int i = 0; i < args.length; i++) {
        params.put(i, args[i]);
      }
    }
    ctx.setInputParameters(params);
    InsertExecutionPlan executionPlan = createExecutionPlan(ctx, false);
    executionPlan.executeInternal();
    return new LocalResultSet(executionPlan);
  }

  @Override
  public ResultSet execute(Database db, Map params, CommandContext parentCtx, boolean usePlanCache) {
    BasicCommandContext ctx = new BasicCommandContext();
    if (parentCtx != null) {
      ctx.setParentWithoutOverridingChild(parentCtx);
    }
    ctx.setDatabase(db);
    ctx.setInputParameters(params);
    InsertExecutionPlan executionPlan = createExecutionPlan(ctx, false);
    executionPlan.executeInternal();
    return new LocalResultSet(executionPlan);
  }

  public InsertExecutionPlan createExecutionPlan(CommandContext ctx, boolean enableProfiling) {
    OInsertExecutionPlanner planner = new OInsertExecutionPlanner(this);
    return planner.createExecutionPlan(ctx, enableProfiling);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    InsertStatement that = (InsertStatement) o;

    if (selectInParentheses != that.selectInParentheses)
      return false;
    if (selectWithFrom != that.selectWithFrom)
      return false;
    if (unsafe != that.unsafe)
      return false;
    if (targetType != null ? !targetType.equals(that.targetType) : that.targetType != null)
      return false;
    if (targetBucketName != null ? !targetBucketName.equals(that.targetBucketName) : that.targetBucketName != null)
      return false;
    if (targetBucket != null ? !targetBucket.equals(that.targetBucket) : that.targetBucket != null)
      return false;
    if (targetIndex != null ? !targetIndex.equals(that.targetIndex) : that.targetIndex != null)
      return false;
    if (insertBody != null ? !insertBody.equals(that.insertBody) : that.insertBody != null)
      return false;
    if (returnStatement != null ? !returnStatement.equals(that.returnStatement) : that.returnStatement != null)
      return false;
    return selectStatement != null ? selectStatement.equals(that.selectStatement) : that.selectStatement == null;
  }

  @Override
  public int hashCode() {
    int result = targetType != null ? targetType.hashCode() : 0;
    result = 31 * result + (targetBucketName != null ? targetBucketName.hashCode() : 0);
    result = 31 * result + (targetBucket != null ? targetBucket.hashCode() : 0);
    result = 31 * result + (targetIndex != null ? targetIndex.hashCode() : 0);
    result = 31 * result + (insertBody != null ? insertBody.hashCode() : 0);
    result = 31 * result + (returnStatement != null ? returnStatement.hashCode() : 0);
    result = 31 * result + (selectStatement != null ? selectStatement.hashCode() : 0);
    result = 31 * result + (selectInParentheses ? 1 : 0);
    result = 31 * result + (selectWithFrom ? 1 : 0);
    result = 31 * result + (unsafe ? 1 : 0);
    return result;
  }

  public Identifier getTargetType() {
    return targetType;
  }

  public Identifier getTargetBucketName() {
    return targetBucketName;
  }

  public Bucket getTargetBucket() {
    return targetBucket;
  }

  public IndexIdentifier getTargetIndex() {
    return targetIndex;
  }

  public InsertBody getInsertBody() {
    return insertBody;
  }

  public Projection getReturnStatement() {
    return returnStatement;
  }

  public SelectStatement getSelectStatement() {
    return selectStatement;
  }

  public boolean isSelectInParentheses() {
    return selectInParentheses;
  }

  public boolean isSelectWithFrom() {
    return selectWithFrom;
  }

  public boolean isUnsafe() {
    return unsafe;
  }
}
/* JavaCC - OriginalChecksum=ccfabcf022d213caed873e6256cb26ad (do not edit this line) */
