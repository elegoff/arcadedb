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

/* Generated By:JJTree: Do not edit this line. OBetweenCondition.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_USERTYPE_VISIBILITY_PUBLIC=true */
package com.arcadedb.query.sql.parser;

import com.arcadedb.database.Identifiable;
import com.arcadedb.query.sql.executor.CommandContext;
import com.arcadedb.query.sql.executor.Result;

import java.util.*;

public class BetweenCondition extends BooleanExpression {

  protected Expression first;
  protected Expression second;
  protected Expression third;

  public BetweenCondition(int id) {
    super(id);
  }

  public BetweenCondition(SqlParser p, int id) {
    super(p, id);
  }

  /**
   * Accept the visitor.
   **/
  public Object jjtAccept(SqlParserVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }

  @Override
  public boolean evaluate(Identifiable currentRecord, CommandContext ctx) {
    Object firstValue = first.execute(currentRecord, ctx);
    if (firstValue == null) {
      return false;
    }

    Object secondValue = second.execute(currentRecord, ctx);
    if (secondValue == null) {
      return false;
    }

//    secondValue = OType.convert(secondValue, firstValue.getClass());

    Object thirdValue = third.execute(currentRecord, ctx);
    if (thirdValue == null) {
      return false;
    }
//    thirdValue = OType.convert(thirdValue, firstValue.getClass());

    final int leftResult = ((Comparable<Object>) firstValue).compareTo(secondValue);
    final int rightResult = ((Comparable<Object>) firstValue).compareTo(thirdValue);

    return leftResult >= 0 && rightResult <= 0;
  }

  @Override
  public boolean evaluate(Result currentRecord, CommandContext ctx) {
    Object firstValue = first.execute(currentRecord, ctx);
    if (firstValue == null) {
      return false;
    }

    Object secondValue = second.execute(currentRecord, ctx);
    if (secondValue == null) {
      return false;
    }

//    secondValue = OType.convert(secondValue, firstValue.getClass());

    Object thirdValue = third.execute(currentRecord, ctx);
    if (thirdValue == null) {
      return false;
    }
//    thirdValue = OType.convert(thirdValue, firstValue.getClass());

    final int leftResult = ((Comparable<Object>) firstValue).compareTo(secondValue);
    final int rightResult = ((Comparable<Object>) firstValue).compareTo(thirdValue);

    return leftResult >= 0 && rightResult <= 0;
  }

  public Expression getFirst() {
    return first;
  }

  public void setFirst(Expression first) {
    this.first = first;
  }

  public Expression getSecond() {
    return second;
  }

  public void setSecond(Expression second) {
    this.second = second;
  }

  public Expression getThird() {
    return third;
  }

  public void setThird(Expression third) {
    this.third = third;
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {
    first.toString(params, builder);
    builder.append(" BETWEEN ");
    second.toString(params, builder);
    builder.append(" AND ");
    third.toString(params, builder);
  }

  @Override
  public boolean supportsBasicCalculation() {
    return true;
  }

  @Override
  protected int getNumberOfExternalCalculations() {
    return 0;
  }

  @Override
  protected List<Object> getExternalCalculationConditions() {
    return Collections.EMPTY_LIST;
  }

  @Override
  public boolean needsAliases(Set<String> aliases) {
    if (first.needsAliases(aliases)) {
      return true;
    }
    if (second.needsAliases(aliases)) {
      return true;
    }
    return third.needsAliases(aliases);
  }

  @Override
  public BooleanExpression copy() {
    BetweenCondition result = new BetweenCondition(-1);
    result.first = first.copy();
    result.second = second.copy();
    result.third = third.copy();
    return result;
  }

  @Override
  public void extractSubQueries(SubQueryCollector collector) {
    first.extractSubQueries(collector);
    second.extractSubQueries(collector);
    third.extractSubQueries(collector);
  }

  @Override
  public boolean refersToParent() {
    return first.refersToParent() || second.refersToParent() || third.refersToParent();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    BetweenCondition that = (BetweenCondition) o;

    if (first != null ? !first.equals(that.first) : that.first != null)
      return false;
    if (second != null ? !second.equals(that.second) : that.second != null)
      return false;
    return third != null ? third.equals(that.third) : that.third == null;
  }

  @Override
  public int hashCode() {
    int result = first != null ? first.hashCode() : 0;
    result = 31 * result + (second != null ? second.hashCode() : 0);
    result = 31 * result + (third != null ? third.hashCode() : 0);
    return result;
  }

  @Override
  public List<String> getMatchPatternInvolvedAliases() {
    List<String> result = new ArrayList<String>();
    List<String> x = first.getMatchPatternInvolvedAliases();
    if (x != null) {
      result.addAll(x);
    }
    x = second.getMatchPatternInvolvedAliases();
    if (x != null) {
      result.addAll(x);
    }
    x = third.getMatchPatternInvolvedAliases();
    if (x != null) {
      result.addAll(x);
    }

    if (result.size() == 0) {
      return null;
    }
    return result;
  }

  @Override
  public void translateLuceneOperator() {
  }

  @Override
  public boolean isCacheable() {
    if (first != null && !first.isCacheable()) {
      return false;
    }
    if (second != null && !second.isCacheable()) {
      return false;
    }
    return third == null || third.isCacheable();
  }
}
/* JavaCC - OriginalChecksum=f94f4779c4a6c6d09539446045ceca89 (do not edit this line) */
