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

import java.util.Locale;

public class QueryHelper {
  protected static final char WILDCARD_ANYCHAR = '?';
  protected static final char WILDCARD_ANY     = '%';

  public static boolean like(String currentValue, String value) {
    if (currentValue == null || currentValue.length() == 0 || value == null || value.length() == 0)
      // EMPTY/NULL PARAMETERS
      return false;

    value = value.toLowerCase(Locale.ENGLISH);
    currentValue = currentValue.toLowerCase(Locale.ENGLISH);

    value = convertForRegExp(value);

    return currentValue.matches(value);
  }

  public static String convertForRegExp(String value) {
    for (int i = 0; i < value.length(); ) {
      char c = value.charAt(i);

      String replaceWith;
      switch (c) {
      case '\\':
        replaceWith = "\\\\";
        break;
      case '[':
        replaceWith = "\\[";
        break;
      case ']':
        replaceWith = "\\]";
        break;
      case '{':
        replaceWith = "\\{";
        break;
      case '}':
        replaceWith = "\\}";
        break;
      case '(':
        replaceWith = "\\(";
        break;
      case ')':
        replaceWith = "\\)";
        break;
      case '|':
        replaceWith = "\\|";
        break;
      case '*':
        replaceWith = "\\*";
        break;
      case '+':
        replaceWith = "\\+";
        break;
      case '$':
        replaceWith = "\\$";
        break;
      case '^':
        replaceWith = "\\^";
        break;
      case '.':
        replaceWith = "\\.";
        break;
      case '?': // WILDCARD_ANYCHAR
        replaceWith = ".";
        break;
      case '%': // WILDCARD_ANY
        replaceWith = ".*";
        break;

      default:
        ++i;
        continue;
      }

      value = value.substring(0, i) + replaceWith + value.substring(i + 1);
      i += replaceWith.length();
    }
    return value;
  }
}
