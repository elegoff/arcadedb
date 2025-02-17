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

/* Generated By:JJTree: Do not edit this line. OWithinOperator.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_USERTYPE_VISIBILITY_PUBLIC=true */
package com.arcadedb.query.sql.parser;

import com.arcadedb.database.DatabaseInternal;

public class WithinOperator extends SimpleNode implements BinaryCompareOperator {
  public WithinOperator(int id) {
    super(id);
  }

  public WithinOperator(SqlParser p, int id) {
    super(p, id);
  }

  /**
   * Accept the visitor.
   **/
  public Object jjtAccept(SqlParserVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }

  @Override public boolean execute(DatabaseInternal database, Object left, Object right) {
    throw new UnsupportedOperationException(toString() + " operator cannot be evaluated in this context");
  }

  @Override public String toString() {
    return "WITHIN";
  }

  @Override public boolean supportsBasicCalculation() {
    return true;
  }

  @Override public WithinOperator copy() {
    return this;
  }

  @Override public boolean equals(Object obj) {
    return obj != null && obj.getClass().equals(this.getClass());
  }

  @Override public int hashCode() {
    return getClass().hashCode();
  }
}
/* JavaCC - OriginalChecksum=e627b2d87bdac6de681d462e4b764288 (do not edit this line) */
