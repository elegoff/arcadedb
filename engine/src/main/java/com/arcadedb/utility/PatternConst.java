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
package com.arcadedb.utility;

import java.util.regex.Pattern;

public final class PatternConst {

  public static final Pattern PATTERN_COMMA_SEPARATED   = Pattern.compile("\\s*,\\s*");
  public static final Pattern PATTERN_SPACES            = Pattern.compile("\\s+");
  public static final Pattern PATTERN_FETCH_PLAN        = Pattern.compile(".*:-?\\d+");
  public static final Pattern PATTERN_SINGLE_SPACE      = Pattern.compile(" ");
  public static final Pattern PATTERN_NUMBERS           = Pattern.compile("[^\\d]");
  public static final Pattern PATTERN_RID               = Pattern.compile("#(-?[0-9]+):(-?[0-9]+)");
  public static final Pattern PATTERN_DIACRITICAL_MARKS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
  public static final Pattern PATTERN_AMP               = Pattern.compile("&");
  public static final Pattern PATTERN_REST_URL          = Pattern.compile("\\{[a-zA-Z0-9%:]*\\}");

  private PatternConst() {
  }
}
