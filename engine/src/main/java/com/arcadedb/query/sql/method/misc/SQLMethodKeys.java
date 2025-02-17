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

import com.arcadedb.database.Document;
import com.arcadedb.database.Identifiable;
import com.arcadedb.query.sql.executor.CommandContext;
import com.arcadedb.query.sql.executor.Result;

import java.util.*;

/**
 * @author Johann Sorel (Geomatys)
 * @author Luca Garulli (l.garulli--(at)--gmail.com)
 */
public class SQLMethodKeys extends OAbstractSQLMethod {

  public static final String NAME = "keys";

  public SQLMethodKeys() {
    super(NAME);
  }

  @Override
  public Object execute( final Object iThis, Identifiable iCurrentRecord, CommandContext iContext,
      Object ioResult, Object[] iParams) {
    if (ioResult instanceof Map) {
      return ((Map<?, ?>) ioResult).keySet();
    }
    if (ioResult instanceof Document) {
      return Arrays.asList(((Document) ioResult).getPropertyNames());
    }
    if (ioResult instanceof Result) {
      Result res = (Result) ioResult;
      return res.getPropertyNames();
    }
    if (ioResult instanceof Collection) {
      List result = new ArrayList();
      for (Object o : (Collection) ioResult) {
        result.addAll((Collection) execute(iThis, iCurrentRecord, iContext, o, iParams));
      }
      return result;
    }
    return null;
  }
}
