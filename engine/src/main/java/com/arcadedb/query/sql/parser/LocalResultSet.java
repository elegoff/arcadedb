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

import com.arcadedb.query.sql.executor.ExecutionPlan;
import com.arcadedb.query.sql.executor.InternalExecutionPlan;
import com.arcadedb.query.sql.executor.Result;
import com.arcadedb.query.sql.executor.ResultSet;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Created by luigidellaquila on 07/07/16.
 */
public class LocalResultSet implements ResultSet {

  private       ResultSet             lastFetch = null;
  private final InternalExecutionPlan executionPlan;
  private       boolean               finished  = false;

  long totalExecutionTime = 0;
  long startTime          = 0;

  public LocalResultSet(InternalExecutionPlan executionPlan) {
    this.executionPlan = executionPlan;
    fetchNext();
  }

  private boolean fetchNext() {
    long begin = System.currentTimeMillis();
    try {
      if (lastFetch == null) {
        startTime = begin;
      }
      lastFetch = executionPlan.fetchNext(100);
      if (!lastFetch.hasNext()) {
        finished = true;
        return false;
      }
      return true;
    } finally {
      totalExecutionTime += (System.currentTimeMillis() - begin);
    }
  }

  @Override
  public boolean hasNext() {
    if (finished) {
      return false;
    }
    if (lastFetch.hasNext()) {
      return true;
    } else {
      return fetchNext();
    }
  }

  @Override
  public Result next() {
    if (finished) {
      throw new IllegalStateException();
    }
    if (!lastFetch.hasNext()) {
      if (!fetchNext()) {
        throw new IllegalStateException();
      }
    }
    return lastFetch.next();
  }

  @Override
  public void close() {
    executionPlan.close();
  }

  @Override
  public Optional<ExecutionPlan> getExecutionPlan() {
    return Optional.of(executionPlan);
  }

  @Override
  public String toString() {
    return "LocalResultSet(hasNext=" + hasNext() + ")";
  }

  /**
   * Prints the resultset content to a string. The resultset is completely browsed.
   */
  public String print() {
    final StringBuilder buffer = new StringBuilder();
    for (int i = 0; hasNext(); ++i) {
      if (i > 0)
        buffer.append("\n");
      buffer.append(i + ": " + next().toJSON());
    }
    return buffer.toString();
  }

  @Override
  public Map<String, Long> getQueryStats() {
    return new HashMap<>();//TODO
  }

}
