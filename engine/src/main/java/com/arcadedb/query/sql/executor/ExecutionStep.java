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

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by luigidellaquila on 20/07/16.
 */
public interface ExecutionStep {

  String getName();

  String getType();

  String getTargetNode();

  String getDescription();

  List<ExecutionStep> getSubSteps();

  /**
   * returns the absolute cost (in nanoseconds) of the execution of this step
   *
   * @return the absolute cost (in nanoseconds) of the execution of this step, -1 if not calculated
   */
  default long getCost() {
    return -1l;
  }

  default Result toResult() {
    ResultInternal result = new ResultInternal();
    result.setProperty("name", getName());
    result.setProperty("type", getType());
    result.setProperty("targetNode", getType());
    result.setProperty(InternalExecutionPlan.JAVA_TYPE, getClass().getName());
    result.setProperty("cost", getCost());
    result.setProperty("subSteps",
        getSubSteps() == null ? null : getSubSteps().stream().map(x -> x.toResult()).collect(Collectors.toList()));
    result.setProperty("description", getDescription());
    return result;
  }

}
