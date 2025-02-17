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

package com.arcadedb.importer;

import com.arcadedb.database.Database;
import com.arcadedb.index.IndexCursor;

public abstract class AbstractContentImporter implements ContentImporter {
  private static final char[] STRING_CONTENT_SKIP = new char[] { '\'', '\'', '"', '"' };

  protected IndexCursor lookupRecord(final Database database, final String typeName, final String typeIdProperty, final Object id) {
    return database.lookupByKey(typeName, typeIdProperty, id);
  }

  protected String getStringContent(final String value) {
    return getStringContent(value, STRING_CONTENT_SKIP);
  }

  protected String getStringContent(final String value, final char[] chars) {
    if (value.length() > 1) {
      final char begin = value.charAt(0);

      for (int i = 0; i < chars.length - 1; i += 2) {
        if (begin == chars[i]) {
          final char end = value.charAt(value.length() - 1);
          if (end == chars[i + 1])
            return value.substring(1, value.length() - 1);
        }
      }
    }
    return value;
  }

}
