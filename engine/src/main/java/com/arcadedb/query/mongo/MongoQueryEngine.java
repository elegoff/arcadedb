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

package com.arcadedb.query.mongo;

import com.arcadedb.database.Database;
import com.arcadedb.database.DatabaseInternal;
import com.arcadedb.exception.QueryParsingException;
import com.arcadedb.log.LogManager;
import com.arcadedb.query.QueryEngine;
import com.arcadedb.query.sql.executor.ResultSet;

import java.util.Map;
import java.util.logging.Level;

public class MongoQueryEngine implements QueryEngine {
  private final Object mongoDBWrapper;

  public static class MongoQueryEngineFactory implements QueryEngineFactory {
    private static Boolean available = null;
    private static Class   arcadeDatabaseClass;

    @Override
    public boolean isAvailable() {
      if (available == null) {
        try {
          arcadeDatabaseClass = Class.forName("com.arcadedb.mongo.MongoDBDatabaseWrapper");
          available = true;
        } catch (ClassNotFoundException e) {
          available = false;
        }
      }
      return available;
    }

    @Override
    public String getLanguage() {
      return "mongo";
    }

    @Override
    public QueryEngine create(final DatabaseInternal database) {
      try {
        return new MongoQueryEngine(arcadeDatabaseClass.getMethod("open", Database.class).invoke(null, database));
      } catch (Exception e) {
        LogManager.instance().log(this, Level.SEVERE, "Error on initializing Mongo query engine", e);
        throw new QueryParsingException("Error on initializing Mongo query engine", e);
      }
    }
  }

  protected MongoQueryEngine(final Object mongoDBWrapper) {
    this.mongoDBWrapper = mongoDBWrapper;
  }

  @Override
  public ResultSet query(final String query, final Map<String, Object> parameters) {
    try {
      return (ResultSet) MongoQueryEngineFactory.arcadeDatabaseClass.getMethod("query", String.class).invoke(mongoDBWrapper, query);
    } catch (Exception e) {
      LogManager.instance().log(this, Level.SEVERE, "Error on initializing Mongo query engine", e);
      throw new QueryParsingException("Error on initializing Mongo query engine", e);
    }
  }

  @Override
  public ResultSet query(final String query, final Object... parameters) {
    return query(query, (Map) null);
  }

  @Override
  public ResultSet command(final String query, final Map<String, Object> parameters) {
    return null;
  }

  @Override
  public ResultSet command(String query, Object... parameters) {
    return null;
  }
}
