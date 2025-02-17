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

/* Generated By:JJTree: Do not edit this line. OIndexName.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_USERTYPE_VISIBILITY_PUBLIC=true */
package com.arcadedb.query.sql.parser;

import com.arcadedb.query.sql.executor.Result;
import com.arcadedb.query.sql.executor.ResultInternal;

import java.util.Map;

public class IndexName extends SimpleNode {

  protected String value;

  public IndexName(int id) {
    super(id);
  }

  public IndexName(SqlParser p, int id) {
    super(p, id);
  }

  public String getValue() {
    return value;
  }

  @Override
  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append(getValue());
  }

  public IndexName copy() {
    IndexName result = new IndexName(-1);
    result.value = value;
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    IndexName that = (IndexName) o;

    return value != null ? value.equals(that.value) : that.value == null;
  }

  @Override
  public int hashCode() {
    return value != null ? value.hashCode() : 0;
  }

  public Result serialize() {
    ResultInternal result = new ResultInternal();
    result.setProperty("value", value);
    return result;
  }

  public void deserialize(Result fromResult) {
    value = fromResult.getProperty("value");
  }

  public void setValue(String value) {
    this.value = value;
  }
}
/* JavaCC - OriginalChecksum=06c827926e7e9ee650b76d42e31feb46 (do not edit this line) */
