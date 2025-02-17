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

package com.arcadedb.database.async;

import com.arcadedb.database.DatabaseInternal;
import com.arcadedb.query.sql.executor.ResultSet;

import java.util.Map;

public class DatabaseAsyncCommand extends DatabaseAsyncAbstractTask {
  public final boolean                idempotent;
  public final String                 language;
  public final String                 command;
  public final Object[]               parameters;
  public final Map<String, Object>    parametersMap;
  public final AsyncResultsetCallback userCallback;

  public DatabaseAsyncCommand(final boolean idempotent, final String language, final String command, final Object[] parameters,
      final AsyncResultsetCallback userCallback) {
    this.idempotent = idempotent;
    this.language = language;
    this.command = command;
    this.parameters = parameters;
    this.parametersMap = null;
    this.userCallback = userCallback;
  }

  public DatabaseAsyncCommand(final boolean idempotent, final String language, final String command,
      final Map<String, Object> parametersMap, final AsyncResultsetCallback userCallback) {
    this.idempotent = idempotent;
    this.language = language;
    this.command = command;
    this.parameters = null;
    this.parametersMap = parametersMap;
    this.userCallback = userCallback;
  }

  @Override
  public void execute(final DatabaseAsyncExecutorImpl.AsyncThread async, final DatabaseInternal database) {
    try {
      final ResultSet resultset = idempotent ?
          database.query(language, command, parameters) :
          database.command(language, command, parameters);

      if (userCallback != null)
        userCallback.onOk(resultset);

    } catch (Exception e) {
      if (userCallback != null)
        userCallback.onError(e);
    }
  }

  @Override
  public String toString() {
    return (idempotent ? "Query" : "Command") + "(" + language + "," + command + ")";
  }
}
