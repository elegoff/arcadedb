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

import java.util.ArrayList;
import java.util.List;

/**
 * Created by luigidellaquila on 19/12/16.
 */
public class InfoExecutionStep implements ExecutionStep {

  String              name;
  String              type;
  String              javaType;
  String              targetNode;
  String              description;
  long                cost;
  List<ExecutionStep> subSteps = new ArrayList<>();

  @Override public String getName() {
    return name;
  }

  @Override public String getType() {
    return type;
  }

  @Override public String getTargetNode() {
    return targetNode;
  }

  @Override public String getDescription() {
    return description;
  }

  @Override public List<ExecutionStep> getSubSteps() {
    return subSteps;
  }

  @Override public long getCost() {
    return cost;
  }

  @Override public Result toResult() {
    return null;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setType(String type) {
    this.type = type;
  }

  public void setTargetNode(String targetNode) {
    this.targetNode = targetNode;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setCost(long cost) {
    this.cost = cost;
  }

  public String getJavaType() {
    return javaType;
  }

  public void setJavaType(String javaType) {
    this.javaType = javaType;
  }
}
