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
package com.arcadedb.query.sql.function.conversion;

import com.arcadedb.database.Identifiable;
import com.arcadedb.log.LogManager;
import com.arcadedb.query.sql.executor.CommandContext;
import com.arcadedb.query.sql.method.misc.OAbstractSQLMethod;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;

/**
 * Transforms a value to date. If the conversion is not possible, null is returned.
 *
 * @author Johann Sorel (Geomatys)
 * @author Luca Garulli (l.garulli--(at)--gmail.com)
 */
public class SQLMethodAsDate extends OAbstractSQLMethod {

  public static final String NAME = "asdate";

  public SQLMethodAsDate() {
    super(NAME, 0, 0);
  }

  @Override
  public String getSyntax() {
    return "asDate()";
  }

  @Override
  public Object execute(Object iThis, Identifiable iCurrentRecord, CommandContext iContext, Object ioResult, Object[] iParams) {
    if (iThis != null) {
      if (iThis instanceof Date) {
        return iThis;
      } else if (iThis instanceof Number) {
        return new Date(((Number) iThis).longValue());
      } else {
        try {
          return new SimpleDateFormat(iContext.getDatabase().getSchema().getDateFormat()).parse(iThis.toString());

        } catch (ParseException e) {
          LogManager.instance().log(this, Level.SEVERE, "Error during %s execution", e, NAME);
        }
      }
    }
    return null;
  }
}
