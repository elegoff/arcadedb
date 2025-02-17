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

import com.arcadedb.database.Database;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class contains statistics about graph structure and query execution.
 * <p>
 * To obtain a copy of this object, use
 *
 * @author Luigi Dell'Aquila
 */
public class OQueryStats {

  public Map<String, Long> stats = new ConcurrentHashMap<>();

  public static OQueryStats get(Database db) {
    return new OQueryStats();//TODO
  }

  public long getIndexStats(String indexName, int params, boolean range, boolean additionalRange) {
    String key = generateKey("INDEX", indexName, String.valueOf(params), String.valueOf(range), String.valueOf(additionalRange));
    Long val = stats.get(key);
    if (val != null) {
      return val;
    }
    return -1;
  }

  public void pushIndexStats(String indexName, int params, boolean range, boolean additionalRange, Long value) {
    String key = generateKey("INDEX", indexName, String.valueOf(params), String.valueOf(range), String.valueOf(additionalRange));
    pushValue(key, value);
  }

  public long getAverageOutEdgeSpan(String vertexClass, String edgeClass) {
    String key = generateKey(vertexClass, "-", edgeClass, "->");
    Long val = stats.get(key);
    if (val != null) {
      return val;
    }
    return -1;
  }

  public long getAverageInEdgeSpan(String vertexClass, String edgeClass) {
    String key = generateKey(vertexClass, "<-", edgeClass, "-");
    Long val = stats.get(key);
    if (val != null) {
      return val;
    }
    return -1;
  }

  public long getAverageBothEdgeSpan(String vertexClass, String edgeClass) {
    String key = generateKey(vertexClass, "-", edgeClass, "-");
    Long val = stats.get(key);
    if (val != null) {
      return val;
    }
    return -1;
  }

  public void pushAverageOutEdgeSpan(String vertexClass, String edgeClass, Long value) {
    String key = generateKey(vertexClass, "-", edgeClass, "->");
    pushValue(key, value);
  }

  public void pushAverageInEdgeSpan(String vertexClass, String edgeClass, Long value) {
    String key = generateKey(vertexClass, "<-", edgeClass, "-");
    pushValue(key, value);
  }

  public void pushAverageBothEdgeSpan(String vertexClass, String edgeClass, Long value) {
    String key = generateKey(vertexClass, "-", edgeClass, "-");
    pushValue(key, value);
  }

  private void pushValue(String key, Long value) {
    if (value == null) {
      return;
    }
    Long val = stats.get(key);

    if (val == null) {
      val = value;
    } else {
      //refine this ;-)
      val = ((Double) ((val * .9) + (value * .1))).longValue();
      if (value > 0 && val == 0) {
        val = 1l;
      }
    }
    stats.put(key, val);
  }

  protected String generateKey(String... keys) {
    StringBuilder result = new StringBuilder();
    for (String s : keys) {
      result.append(".->");
      result.append(s);
    }
    return result.toString();
  }
}
