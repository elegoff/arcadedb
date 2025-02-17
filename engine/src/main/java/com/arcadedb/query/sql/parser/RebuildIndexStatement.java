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

/* Generated By:JJTree: Do not edit this line. ORebuildIndexStatement.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_USERTYPE_VISIBILITY_PUBLIC=true */
package com.arcadedb.query.sql.parser;

import com.arcadedb.database.Database;
import com.arcadedb.database.Document;
import com.arcadedb.exception.CommandExecutionException;
import com.arcadedb.index.Index;
import com.arcadedb.index.TypeIndex;
import com.arcadedb.index.lsm.LSMTreeIndexAbstract;
import com.arcadedb.log.LogManager;
import com.arcadedb.schema.EmbeddedSchema;
import com.arcadedb.query.sql.executor.CommandContext;
import com.arcadedb.query.sql.executor.InternalResultSet;
import com.arcadedb.query.sql.executor.ResultInternal;
import com.arcadedb.query.sql.executor.ResultSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

public class RebuildIndexStatement extends SimpleExecStatement {

  protected boolean   all      = false;
  protected IndexName name;
  private   int       pageSize = LSMTreeIndexAbstract.DEF_PAGE_SIZE;

  public RebuildIndexStatement(int id) {
    super(id);
  }

  public RebuildIndexStatement(SqlParser p, int id) {
    super(p, id);
  }

  @Override
  public ResultSet executeSimple(final CommandContext ctx) {
    final ResultInternal result = new ResultInternal();
    result.setProperty("operation", "rebuild index");

    final AtomicLong total = new AtomicLong();

    final Database database = ctx.getDatabase();
    database.transaction((tx) -> {
      final Index.BuildIndexCallback callback = new Index.BuildIndexCallback() {
        @Override
        public void onDocumentIndexed(final Document document, final long totalIndexed) {
          total.incrementAndGet();

          if (totalIndexed % 100000 == 0) {
            System.out.print(".");
            System.out.flush();
          }
        }
      };

      final List<String> indexList = new ArrayList<>();

      if (all) {
        final Index[] indexes = database.getSchema().getIndexes();

        for (Index idx : indexes) {
          try {
            if (idx instanceof TypeIndex) {
              final EmbeddedSchema.INDEX_TYPE indexType = idx.getType();
              final boolean unique = idx.isUnique();
              final String[] propNames = idx.getPropertyNames();

              final String typeName = idx.getTypeName();

              database.getSchema().dropIndex(idx.getName());
              database.getSchema().createTypeIndex(indexType, unique, typeName, propNames, LSMTreeIndexAbstract.DEF_PAGE_SIZE, callback);
              indexList.add(idx.getName());
            }
          } catch (Exception e) {
            LogManager.instance().log(this, Level.SEVERE, "Error on rebuilding index '%s'", e, idx.getName());
          }
        }

      } else {
        final Index idx = database.getSchema().getIndexByName(name.getValue());
        if (idx == null)
          throw new CommandExecutionException("Index '" + name + "' not found");

        if (!idx.isAutomatic())
          throw new CommandExecutionException("Cannot rebuild index '" + name + "' because it's manual and there aren't indications of what to index");

        final EmbeddedSchema.INDEX_TYPE type = idx.getType();
        final String typeName = idx.getTypeName();
        final boolean unique = idx.isUnique();
        final String[] propertyNames = idx.getPropertyNames();
        final LSMTreeIndexAbstract.NULL_STRATEGY nullStrategy = idx.getNullStrategy();

        database.getSchema().dropIndex(idx.getName());

        if (typeName != null && idx instanceof TypeIndex) {
          database.getSchema().getType(typeName).createTypeIndex(type, unique, propertyNames, LSMTreeIndexAbstract.DEF_PAGE_SIZE, nullStrategy, callback);
        } else {
          database.getSchema()
              .createBucketIndex(type, unique, idx.getTypeName(), database.getSchema().getBucketById(idx.getAssociatedBucketId()).getName(), propertyNames,
                  pageSize, nullStrategy, callback);
        }

        indexList.add(idx.getName());
      }
      result.setProperty("indexes", indexList);
      result.setProperty("totalIndexed", total.get());
    });

    final InternalResultSet rs = new InternalResultSet();
    rs.add(result);
    return rs;
  }

  @Override
  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append("REBUILD INDEX ");
    if (all) {
      builder.append("*");
    } else {
      name.toString(params, builder);
    }
  }

  @Override
  public RebuildIndexStatement copy() {
    RebuildIndexStatement result = new RebuildIndexStatement(-1);
    result.all = all;
    result.name = name == null ? null : name.copy();
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    RebuildIndexStatement that = (RebuildIndexStatement) o;

    if (all != that.all)
      return false;
    return name != null ? name.equals(that.name) : that.name == null;
  }

  @Override
  public int hashCode() {
    int result = (all ? 1 : 0);
    result = 31 * result + (name != null ? name.hashCode() : 0);
    return result;
  }
}
/* JavaCC - OriginalChecksum=baca3c54112f1c08700ebdb691fa85bd (do not edit this line) */
