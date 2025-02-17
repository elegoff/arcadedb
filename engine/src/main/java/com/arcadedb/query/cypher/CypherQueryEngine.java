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

package com.arcadedb.query.cypher;

import com.arcadedb.database.Database;
import com.arcadedb.database.DatabaseInternal;
import com.arcadedb.exception.QueryParsingException;
import com.arcadedb.log.LogManager;
import com.arcadedb.query.QueryEngine;
import com.arcadedb.query.sql.executor.ResultSet;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class CypherQueryEngine implements QueryEngine {
  private final Object arcadeGraph;

  public static class CypherQueryEngineFactory implements QueryEngineFactory {
    private static Boolean available = null;
    private static Class   arcadeGraphClass;
    private static Class   arcadeCypherClass;

    @Override
    public boolean isAvailable() {
      if (available == null) {
        try {
          arcadeGraphClass = Class.forName("org.apache.tinkerpop.gremlin.arcadedb.structure.ArcadeGraph");
          arcadeCypherClass = Class.forName("org.apache.tinkerpop.gremlin.arcadedb.structure.ArcadeCypher");
          available = true;
        } catch (ClassNotFoundException e) {
          available = false;
        }
      }
      return available;
    }

    @Override
    public String getLanguage() {
      return "cypher";
    }

    @Override
    public QueryEngine create(final DatabaseInternal database) {
      try {
        return new CypherQueryEngine(arcadeGraphClass.getMethod("open", Database.class).invoke(null, database));
      } catch (Exception e) {
        LogManager.instance().log(this, Level.SEVERE, "Error on initializing Cypher query engine", e);
        throw new QueryParsingException("Error on initializing Cypher query engine", e);
      }
    }
  }

  protected CypherQueryEngine(final Object arcadeGraph) {
    this.arcadeGraph = arcadeGraph;
  }

  @Override
  public ResultSet query(final String query, final Map<String, Object> parameters) {
    return command(query, parameters);
  }

  @Override
  public ResultSet query(final String query, final Object... parameters) {
    return command(query, parameters);
  }

  @Override
  public ResultSet command(final String query, final Map<String, Object> parameters) {
    try {
      final Object arcadeGremlin = CypherQueryEngineFactory.arcadeGraphClass.getMethod("cypher", String.class).invoke(arcadeGraph, query);
      CypherQueryEngineFactory.arcadeCypherClass.getMethod("setParameters", Map.class).invoke(arcadeGremlin, parameters);
      return (ResultSet) CypherQueryEngineFactory.arcadeCypherClass.getMethod("execute").invoke(arcadeGremlin);
    } catch (Exception e) {
      LogManager.instance().log(this, Level.SEVERE, "Error on initializing Cypher query engine", e);
      throw new QueryParsingException("Error on initializing Cypher query engine", e);
    }
  }

  @Override
  public ResultSet command(final String query, final Object... parameters) {
    final Map<String, Object> map = new HashMap<>(parameters.length / 2);
    for (int i = 0; i < parameters.length; i += 2)
      map.put((String) parameters[i], parameters[i + 1]);
    return command(query, map);
  }
}
