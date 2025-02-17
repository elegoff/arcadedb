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

package com.arcadedb.schema;

import com.arcadedb.TestHelper;
import com.arcadedb.database.Document;
import com.arcadedb.database.MutableDocument;
import com.arcadedb.query.sql.executor.ResultSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DictionaryTest extends TestHelper {
  @Test
  public void updateName() {
    database.transaction((database) -> {
      Assertions.assertFalse(database.getSchema().existsType("V"));

      final DocumentType type = database.getSchema().createDocumentType("V", 3);
      type.createProperty("id", Integer.class);
      type.createProperty("name", String.class);

      for (int i = 0; i < 10; ++i) {
        final MutableDocument v = database.newDocument("V");
        v.set("id", i);
        v.set("name", "Jay");
        v.set("surname", "Miner");
        v.save();
      }
    });

    Assertions.assertEquals(4, database.getSchema().getDictionary().getDictionaryMap().size());

    database.transaction((database) -> {
      Assertions.assertTrue(database.getSchema().existsType("V"));

      final MutableDocument v = database.newDocument("V");
      v.set("id", 10);
      v.set("name", "Jay");
      v.set("surname", "Miner");
      v.set("newProperty", "newProperty");
      v.save();
    });

    Assertions.assertEquals(5, database.getSchema().getDictionary().getDictionaryMap().size());

    database.transaction((database) -> {
      Assertions.assertTrue(database.getSchema().existsType("V"));
      database.getSchema().getDictionary().updateName("name", "firstName");
    });

    Assertions.assertEquals(5, database.getSchema().getDictionary().getDictionaryMap().size());

    database.transaction((database) -> {
      final ResultSet iter = database.query("sql", "select from V order by id asc");

      int i = 0;
      while (iter.hasNext()) {
        final Document d = (Document) iter.next().getRecord().get();

        Assertions.assertEquals(i, d.getInteger("id"));
        Assertions.assertEquals("Jay", d.getString("firstName"));
        Assertions.assertEquals("Miner", d.getString("surname"));

        if (i == 10)
          Assertions.assertEquals("newProperty", d.getString("newProperty"));
        else
          Assertions.assertNull(d.getString("newProperty"));

        Assertions.assertNull(d.getString("name"));

        ++i;
      }

      Assertions.assertEquals(11, i);
    });

    try {
      database.transaction((database) -> {
        Assertions.assertTrue(database.getSchema().existsType("V"));
        database.getSchema().getDictionary().updateName("V", "V2");
      });
      Assertions.fail();
    } catch (Exception e) {
    }
  }
}
