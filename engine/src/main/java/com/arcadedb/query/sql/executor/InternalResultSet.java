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

package com.arcadedb.query.sql.executor;

import java.util.*;

/**
 * Created by luigidellaquila on 07/07/16.
 */
public class InternalResultSet implements ResultSet {
  private   List<Result>  content = new ArrayList<>();
  private   int           next    = 0;
  protected ExecutionPlan plan;

  @Override
  public boolean hasNext() {
    return content.size() > next;
  }

  @Override
  public Result next() {
    return content.get(next++);
  }

  @Override
  public void close() {
    this.content.clear();
  }

  @Override
  public Optional<ExecutionPlan> getExecutionPlan() {
    return Optional.ofNullable(plan);
  }

  public void setPlan(ExecutionPlan plan) {
    this.plan = plan;
  }

  @Override
  public Map<String, Long> getQueryStats() {
    return new HashMap<>();
  }

  public void add(Result nextResult) {
    content.add(nextResult);
  }

  public void reset() {
    this.next = 0;
  }
}
