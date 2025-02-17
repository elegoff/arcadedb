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

import com.arcadedb.database.Database;
import com.arcadedb.exception.CommandSQLParsingException;
import com.arcadedb.log.LogManager;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * This class is an LRU cache for already parsed SQL statement executors. It stores itself in the storage as a resource. It also
 * acts an an entry point for the SQL parser.
 *
 * @author Luigi Dell'Aquila (luigi.dellaquila-(at)-gmail.com)
 */
public class StatementCache {
  private final Database               db;
  private final Map<String, Statement> map;
  private final int                    mapSize;

  /**
   * @param size the size of the cache
   */
  public StatementCache(final Database db, final int size) {
    this.db = db;
    this.mapSize = size;
    this.map = new LinkedHashMap<String, Statement>(size) {
      protected boolean removeEldestEntry(final Map.Entry<String, Statement> eldest) {
        return super.size() > mapSize;
      }
    };
  }

  /**
   * @param statement an SQL statement
   *
   * @return the corresponding executor, taking it from the internal cache, if it exists
   */
  public Statement get(final String statement) {
    Statement result;
    synchronized (map) {
      //LRU
      result = map.remove(statement);
      if (result != null) {
        map.put(statement, result);
      }
    }
    if (result == null) {
      result = parse(statement);
      synchronized (map) {
        map.put(statement, result);
      }
    }
    return result;
  }

  /**
   * parses an SQL statement and returns the corresponding executor
   *
   * @param statement the SQL statement
   *
   * @return the corresponding executor
   *
   * @throws CommandSQLParsingException if the input parameter is not a valid SQL statement
   */
  protected Statement parse(final String statement) throws CommandSQLParsingException {
    try {

      InputStream is;

      if (db == null) {
        is = new ByteArrayInputStream(statement.getBytes());
      } else {
        try {

          is = new ByteArrayInputStream(statement.getBytes("UTF-8"));
//          is = new ByteArrayInputStream(statement.getBytes(db.getStorage().getConfiguration().getCharset()));
        } catch (UnsupportedEncodingException e2) {
          LogManager.instance().log(this, Level.WARNING, "Unsupported charset for database " + db);
          is = new ByteArrayInputStream(statement.getBytes());
        }
      }

      SqlParser osql = null;
      if (db == null) {
        osql = new SqlParser(is);
      } else {
        try {
//          osql = new SqlParser(is, db.getStorage().getConfiguration().getCharset());
          osql = new SqlParser(is, "UTF-8");
        } catch (UnsupportedEncodingException e2) {
          LogManager.instance().log(this, Level.WARNING, "Unsupported charset for database " + db);
          osql = new SqlParser(is);
        }
      }
      Statement result = osql.parse();
      result.originalStatement = statement;

      return result;
    } catch (ParseException e) {
      throwParsingException(e, statement);
    } catch (TokenMgrError e2) {
      throwParsingException(e2, statement);
    }
    return null;
  }

  protected static void throwParsingException(ParseException e, String statement) {
    throw new CommandSQLParsingException(statement, e);
  }

  protected static void throwParsingException(TokenMgrError e, String statement) {
    throw new CommandSQLParsingException(statement, e);
  }

  public boolean contains(final String statement) {
    synchronized (map) {
      return map.containsKey(statement);
    }
  }

  public void clear() {
    synchronized (map) {
      map.clear();
    }
  }
}
