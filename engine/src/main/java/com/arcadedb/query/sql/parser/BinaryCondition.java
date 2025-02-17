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

/* Generated By:JJTree: Do not edit this line. OBinaryCondition.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_USERTYPE_VISIBILITY_PUBLIC=true */
package com.arcadedb.query.sql.parser;

import com.arcadedb.database.Database;
import com.arcadedb.database.Identifiable;
import com.arcadedb.database.Record;
import com.arcadedb.exception.CommandExecutionException;
import com.arcadedb.query.sql.executor.CommandContext;
import com.arcadedb.query.sql.executor.Result;
import com.arcadedb.query.sql.executor.ResultInternal;
import com.arcadedb.schema.DocumentType;

import java.util.*;

public class BinaryCondition extends BooleanExpression {
  protected Expression            left;
  protected BinaryCompareOperator operator;
  protected Expression            right;

  public BinaryCondition(int id) {
    super(id);
  }

  public BinaryCondition(SqlParser p, int id) {
    super(p, id);
  }

  /**
   * Accept the visitor.
   **/
  public Object jjtAccept(final SqlParserVisitor visitor, final Object data) {
    return visitor.visit(this, data);
  }

  @Override
  public boolean evaluate(final Identifiable currentRecord, final CommandContext ctx) {
    return operator.execute(ctx.getDatabase(), left.execute(currentRecord, ctx), right.execute(currentRecord, ctx));
  }

  @Override
  public boolean evaluate(final Result currentRecord, final CommandContext ctx) {
    Object leftVal = left.execute(currentRecord, ctx);
    Object rightVal = right.execute(currentRecord, ctx);
    return operator.execute(ctx.getDatabase(), leftVal, rightVal);
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {
    left.toString(params, builder);
    builder.append(" ");
    builder.append(operator.toString());
    builder.append(" ");
    right.toString(params, builder);
  }

  protected boolean supportsBasicCalculation() {
    if (!operator.supportsBasicCalculation()) {
      return false;
    }
    return left.supportsBasicCalculation() && right.supportsBasicCalculation();

  }

  @Override
  protected int getNumberOfExternalCalculations() {
    int total = 0;
    if (!operator.supportsBasicCalculation()) {
      total++;
    }
    if (!left.supportsBasicCalculation()) {
      total++;
    }
    if (!right.supportsBasicCalculation()) {
      total++;
    }
    return total;
  }

  @Override
  protected List<Object> getExternalCalculationConditions() {
    List<Object> result = new ArrayList<>();
    if (!operator.supportsBasicCalculation()) {
      result.add(this);
    }
    if (!left.supportsBasicCalculation()) {
      result.add(left);
    }
    if (!right.supportsBasicCalculation()) {
      result.add(right);
    }
    return result;
  }

  public BinaryCondition isIndexedFunctionCondition(DocumentType iSchemaClass, Database database) {
    if (left.isIndexedFunctionCal()) {
      return this;
    }
    return null;
  }

  public long estimateIndexed(FromClause target, CommandContext context) {
    return left.estimateIndexedFunction(target, context, operator, right.execute((Result) null, context));
  }

  public Iterable<Record> executeIndexedFunction(FromClause target, CommandContext context) {
    return left.executeIndexedFunction(target, context, operator, right.execute((Result) null, context));
  }

  /**
   * tests if current expression involves an indexed function AND that function can also be executed without using the index
   *
   * @param target  the query target
   * @param context the execution context
   *
   * @return true if current expression involves an indexed function AND that function can be used on this target, false otherwise
   */
  public boolean canExecuteIndexedFunctionWithoutIndex(FromClause target, CommandContext context) {
    return left.canExecuteIndexedFunctionWithoutIndex(target, context, operator, right.execute((Result) null, context));
  }

  /**
   * tests if current expression involves an indexed function AND that function can be used on this target
   *
   * @param target  the query target
   * @param context the execution context
   *
   * @return true if current expression involves an indexed function AND that function can be used on this target, false otherwise
   */
  public boolean allowsIndexedFunctionExecutionOnTarget(FromClause target, CommandContext context) {
    return left.allowsIndexedFunctionExecutionOnTarget(target, context, operator, right.execute((Result) null, context));
  }

  /**
   * tests if current expression involves an indexed function AND the function has also to be executed after the index search. In
   * some cases, the index search is accurate, so this condition can be excluded from further evaluation. In other cases the result
   * from the index is a superset of the expected result, so the function has to be executed anyway for further filtering
   *
   * @param target  the query target
   * @param context the execution context
   *
   * @return true if current expression involves an indexed function AND the function has also to be executed after the index
   * search.
   */
  public boolean executeIndexedFunctionAfterIndexSearch(FromClause target, CommandContext context) {
    return left.executeIndexedFunctionAfterIndexSearch(target, context, operator, right.execute((Result) null, context));
  }

  public List<BinaryCondition> getIndexedFunctionConditions(DocumentType iSchemaClass, Database database) {
    if (left.isIndexedFunctionCal()) {
      return Collections.singletonList(this);
    }
    return null;
  }

  @Override
  public boolean needsAliases(Set<String> aliases) {
    if (left.needsAliases(aliases)) {
      return true;
    }
    return right.needsAliases(aliases);
  }

  @Override
  public BinaryCondition copy() {
    BinaryCondition result = new BinaryCondition(-1);
    result.left = left.copy();
    result.operator = operator.copy();
    result.right = right.copy();
    return result;
  }

  @Override
  public void extractSubQueries(SubQueryCollector collector) {
    left.extractSubQueries(collector);
    right.extractSubQueries(collector);
  }

  @Override
  public boolean refersToParent() {
    return left.refersToParent() || right.refersToParent();
  }

  @Override
  public Optional<UpdateItem> transformToUpdateItem() {
    if (!checkCanTransformToUpdate()) {
      return Optional.empty();
    }
    if (operator instanceof EqualsCompareOperator) {
      UpdateItem result = new UpdateItem(-1);
      result.operator = UpdateItem.OPERATOR_EQ;
      BaseExpression baseExp = ((BaseExpression) left.mathExpression);
      result.left = baseExp.identifier.suffix.identifier.copy();
      result.leftModifier = baseExp.modifier == null ? null : baseExp.modifier.copy();
      result.right = right.copy();
      return Optional.of(result);
    }
    return super.transformToUpdateItem();
  }

  private boolean checkCanTransformToUpdate() {
    if (left == null || left.mathExpression == null || !(left.mathExpression instanceof BaseExpression)) {
      return false;
    }
    BaseExpression base = (BaseExpression) left.mathExpression;
    return base.identifier != null && base.identifier.suffix != null && base.identifier.suffix.identifier != null;
  }

  public Expression getLeft() {
    return left;
  }

  public BinaryCompareOperator getOperator() {
    return operator;
  }

  public Expression getRight() {
    return right;
  }

  public void setLeft(Expression left) {
    this.left = left;
  }

  public void setOperator(BinaryCompareOperator operator) {
    this.operator = operator;
  }

  public void setRight(Expression right) {
    this.right = right;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    BinaryCondition that = (BinaryCondition) o;

    if (Objects.equals(left, that.left))
      return false;
    if (Objects.equals(operator, that.operator))
      return false;
    return Objects.equals(right, that.right);
  }

  @Override
  public int hashCode() {
    int result = left != null ? left.hashCode() : 0;
    result = 31 * result + (operator != null ? operator.hashCode() : 0);
    result = 31 * result + (right != null ? right.hashCode() : 0);
    return result;
  }

  @Override
  public List<String> getMatchPatternInvolvedAliases() {
    List<String> leftX = left.getMatchPatternInvolvedAliases();
    List<String> rightX = right.getMatchPatternInvolvedAliases();
    if (leftX == null) {
      return rightX;
    }
    if (rightX == null) {
      return leftX;
    }

    List<String> result = new ArrayList<>();
    result.addAll(leftX);
    result.addAll(rightX);
    return result;
  }

  @Override
  public void translateLuceneOperator() {
    if (operator instanceof LuceneOperator) {
      Expression newLeft = new Expression(-1);
      newLeft.mathExpression = new BaseExpression(-1);
      BaseIdentifier identifier = new BaseIdentifier(-1);
      ((BaseExpression) newLeft.mathExpression).identifier = identifier;
      identifier.levelZero = new LevelZeroIdentifier(-1);
      FunctionCall function = new FunctionCall(-1);
      identifier.levelZero.functionCall = function;
      function.name = new Identifier("search_fields");
      function.params = new ArrayList<>();
      function.params.add(fieldNamesToStrings(left));
      function.params.add(right);
      left = newLeft;

      operator = new EqualsCompareOperator(-1);
      right = new Expression(-1);
      right.booleanValue = true;
    }
  }

  private Expression fieldNamesToStrings(Expression left) {
    if (left.isBaseIdentifier()) {
      Identifier identifier = ((BaseExpression) left.mathExpression).identifier.suffix.identifier;
      PCollection newColl = new PCollection(-1);
      newColl.expressions = new ArrayList<>();
      newColl.expressions.add(identifierToStringExpr(identifier));
      Expression result = new Expression(-1);
      BaseExpression newBase = new BaseExpression(-1);
      result.mathExpression = newBase;
      newBase.identifier = new BaseIdentifier(-1);
      newBase.identifier.levelZero = new LevelZeroIdentifier(-1);
      newBase.identifier.levelZero.collection = newColl;
      return result;
    } else if (left.mathExpression instanceof BaseExpression) {
      BaseExpression base = (BaseExpression) left.mathExpression;
      if (base.identifier != null && base.identifier.levelZero != null && base.identifier.levelZero.collection != null) {
        PCollection coll = base.identifier.levelZero.collection;

        PCollection newColl = new PCollection(-1);
        newColl.expressions = new ArrayList<>();

        for (Expression exp : coll.expressions) {
          if (exp.isBaseIdentifier()) {
            Identifier identifier = ((BaseExpression) exp.mathExpression).identifier.suffix.identifier;
            Expression val = identifierToStringExpr(identifier);
            newColl.expressions.add(val);
          } else {
            throw new CommandExecutionException("Cannot execute because of invalid LUCENE expression");
          }
        }
        Expression result = new Expression(-1);
        BaseExpression newBase = new BaseExpression(-1);
        result.mathExpression = newBase;
        newBase.identifier = new BaseIdentifier(-1);
        newBase.identifier.levelZero = new LevelZeroIdentifier(-1);
        newBase.identifier.levelZero.collection = newColl;
        return result;
      }
    }
    throw new CommandExecutionException("Cannot execute because of invalid LUCENE expression");
  }

  private Expression identifierToStringExpr(Identifier identifier) {
    BaseExpression bExp = new BaseExpression(identifier.getStringValue());

    Expression result = new Expression(-1);
    result.mathExpression = bExp;
    return result;
  }

  public Result serialize() {
    ResultInternal result = new ResultInternal();
    result.setProperty("left", left.serialize());
    result.setProperty("operator", operator.getClass().getName());
    result.setProperty("right", right.serialize());
    return result;
  }

  public void deserialize(Result fromResult) {
    left = new Expression(-1);
    left.deserialize(fromResult.getProperty("left"));
    try {
      operator = (BinaryCompareOperator) Class.forName(String.valueOf(fromResult.getProperty("operator"))).getConstructor().newInstance();
    } catch (Exception e) {
      throw new CommandExecutionException(e);
    }
    right = new Expression(-1);
    right.deserialize(fromResult.getProperty("right"));
  }

  @Override
  public boolean isCacheable() {
    return left.isCacheable() && right.isCacheable();
  }

}
/* JavaCC - OriginalChecksum=99ed1dd2812eb730de8e1931b1764da5 (do not edit this line) */
