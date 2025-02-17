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
package com.arcadedb.query.sql.function.stat;

/**
 * Compute the standard deviation for a given field.
 *
 * @author Fabrizio Fortino
 */
public class SQLFunctionStandardDeviation extends SQLFunctionVariance {

  public static final String NAME = "stddev";

  public SQLFunctionStandardDeviation() {
    super(NAME, 1, 1);
  }

  @Override
  public Object getResult() {
    return this.evaluate(super.getResult());
  }

  @Override
  public String getSyntax() {
    return NAME + "(<field>)";
  }

  private Double evaluate(Object variance) {
    Double result = null;
    if (variance != null) {
      result = Math.sqrt((Double) variance);
    }

    return result;
  }
}
