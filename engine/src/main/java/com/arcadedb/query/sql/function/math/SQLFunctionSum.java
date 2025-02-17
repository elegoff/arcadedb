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
package com.arcadedb.query.sql.function.math;

import com.arcadedb.database.Identifiable;
import com.arcadedb.schema.Type;
import com.arcadedb.query.sql.executor.CommandContext;
import com.arcadedb.query.sql.executor.MultiValue;

/**
 * Computes the sum of field. Uses the context to save the last sum number. When different Number class are used, take the class
 * with most precision.
 *
 * @author Luca Garulli (l.garulli--(at)--gmail.com)
 */
public class SQLFunctionSum extends SQLFunctionMathAbstract {
  public static final String NAME = "sum";

  private Number sum;

  public SQLFunctionSum() {
    super(NAME, 1, -1);
  }

  public Object execute( final Object iThis, final Identifiable iCurrentRecord, Object iCurrentResult,
      final Object[] iParams, CommandContext iContext) {
    if (iParams.length == 1) {
      if (iParams[0] instanceof Number)
        sum((Number) iParams[0]);
      else if (MultiValue.isMultiValue(iParams[0]))
        for (Object n : MultiValue.getMultiValueIterable(iParams[0]))
          sum((Number) n);
    } else {
      sum = null;
      for (int i = 0; i < iParams.length; ++i)
        sum((Number) iParams[i]);
    }
    return sum;
  }

  protected void sum(final Number value) {
    if (value != null) {
      if (sum == null)
        // FIRST TIME
        sum = value;
      else
        sum = Type.increment(sum, value);
    }
  }

  @Override
  public boolean aggregateResults() {
    return configuredParameters.length == 1;
  }

  public String getSyntax() {
    return "sum(<field> [,<field>*])";
  }

  @Override
  public Object getResult() {
    return sum == null ? 0 : sum;
  }
}
