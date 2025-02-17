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
package com.arcadedb.query.sql.method.misc;

import com.arcadedb.database.Identifiable;
import com.arcadedb.query.sql.executor.CommandContext;
import com.arcadedb.query.sql.executor.MultiValue;
import com.arcadedb.utility.Callable;

/**
 * Remove all the occurrences of elements from a collection.
 *
 * @author Luca Garulli (l.garulli--(at)--gmail.com)
 * @see SQLMethodRemove
 */
public class SQLMethodRemoveAll extends OAbstractSQLMethod {

  public static final String NAME = "removeall";

  public SQLMethodRemoveAll() {
    super(NAME, 1, -1);
  }

  @Override
  public Object execute( Object iThis, final Identifiable iCurrentRecord, final CommandContext iContext,
      Object ioResult, Object[] iParams) {
    if (iParams != null && iParams.length > 0 && iParams[0] != null) {
      iParams = MultiValue.array(iParams, Object.class, new Callable<Object, Object>() {

        @Override
        public Object call(final Object iArgument) {
          if (iArgument instanceof String && ((String) iArgument).startsWith("$")) {
            return iContext.getVariable((String) iArgument);
          }
          return iArgument;
        }
      });
      for (Object o : iParams) {
        ioResult = MultiValue.remove(ioResult, o, true);
      }
    }

    return ioResult;
  }
}
