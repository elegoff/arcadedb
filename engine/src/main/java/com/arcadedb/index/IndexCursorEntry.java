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

package com.arcadedb.index;

import com.arcadedb.database.Identifiable;

import java.util.Arrays;
import java.util.Objects;

public class IndexCursorEntry {
  public final Object[]     keys;
  public final Identifiable record;
  public final int          score;

  public IndexCursorEntry(final Object[] keys, final Identifiable record, final int score) {
    this.keys = keys;
    this.record = record;
    this.score = score;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    final IndexCursorEntry that = (IndexCursorEntry) o;
    return score == that.score && Objects.equals(record, that.record) && Arrays.equals(keys, that.keys);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(record, score);
    result = 31 * result + Arrays.hashCode(keys);
    return result;
  }
}
