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

import com.arcadedb.TestHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.UUID;

/**
 * @author Luca Garulli (l.garulli@arcadedata.com)
 */
public class OGroupByExecutionTest extends TestHelper {
  public OGroupByExecutionTest() {
    autoStartTx = true;
  }

  @Test
  public void testGroupByCount() {
    database.getSchema().createDocumentType("InputTx");

    for (int i = 0; i < 100; i++) {
      final String hash = UUID.randomUUID().toString();
      database.command("sql", "insert into InputTx set address = '" + hash + "'");

      // CREATE RANDOM NUMBER OF COPIES
      final int random = new Random().nextInt(10);
      for (int j = 0; j < random; j++) {
        database.command("sql", "insert into InputTx set address = '" + hash + "'");
      }
    }

    final ResultSet result = database.query("sql", "select address, count(*) as occurrencies from InputTx where address is not null group by address limit 10");
    while (result.hasNext()) {
      final Result row = result.next();
      Assertions.assertNotNull(row.getProperty("address"));
      Assertions.assertNotNull(row.getProperty("occurrencies"));
    }
    result.close();
  }
}
