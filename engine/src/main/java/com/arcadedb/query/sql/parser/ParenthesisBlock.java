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

/* Generated By:JJTree: Do not edit this line. OParenthesisBlock.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_USERTYPE_VISIBILITY_PUBLIC=true */
package com.arcadedb.query.sql.parser;

import com.arcadedb.database.Identifiable;
import com.arcadedb.query.sql.executor.CommandContext;
import com.arcadedb.query.sql.executor.Result;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class ParenthesisBlock extends BooleanExpression {

  BooleanExpression subElement;

  public ParenthesisBlock(int id) {
    super(id);
  }

  public ParenthesisBlock(SqlParser p, int id) {
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
    return subElement.evaluate(currentRecord, ctx);
  }

  @Override
  public boolean evaluate(Result currentRecord, CommandContext ctx) {
    return subElement.evaluate(currentRecord, ctx);
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append("(");
    subElement.toString(params, builder);
    builder.append(" )");
  }

  @Override
  public boolean supportsBasicCalculation() {
    return subElement.supportsBasicCalculation();
  }

  @Override
  protected int getNumberOfExternalCalculations() {
    return subElement.getNumberOfExternalCalculations();
  }

  @Override
  protected List<Object> getExternalCalculationConditions() {
    return subElement.getExternalCalculationConditions();
  }

  @Override
  public List<AndBlock> flatten() {
    return subElement.flatten();
  }

  @Override
  public boolean needsAliases(Set<String> aliases) {
    return subElement.needsAliases(aliases);
  }

  @Override
  public ParenthesisBlock copy() {
    ParenthesisBlock result = new ParenthesisBlock(-1);
    result.subElement = subElement.copy();
    return result;
  }

  @Override
  public void extractSubQueries(SubQueryCollector collector) {
    this.subElement.extractSubQueries(collector);
  }

  @Override
  public boolean refersToParent() {
    return subElement.refersToParent();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    ParenthesisBlock that = (ParenthesisBlock) o;

    return subElement != null ? subElement.equals(that.subElement) : that.subElement == null;
  }

  @Override
  public int hashCode() {
    return subElement != null ? subElement.hashCode() : 0;
  }

  @Override
  public List<String> getMatchPatternInvolvedAliases() {
    return subElement.getMatchPatternInvolvedAliases();
  }

  @Override
  public void translateLuceneOperator() {
    subElement.translateLuceneOperator();
  }

  @Override
  public boolean isCacheable() {
    return subElement.isCacheable();
  }
}
/* JavaCC - OriginalChecksum=9a16b6cf7d051382acb94c45067631a9 (do not edit this line) */
