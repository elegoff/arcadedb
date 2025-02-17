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

package com.arcadedb.console;

import com.arcadedb.utility.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

public class ConsoleTest {
  private static final String  DB_PATH = "target/databases/console";
  private static       Console console;

  @BeforeEach
  public void populate() throws IOException {
    console = new Console(false);
    Assertions.assertTrue(console.parse("create database " + DB_PATH, false));
  }

  @AfterEach
  public void drop() {
    console.close();
    FileUtils.deleteRecursively(new File(DB_PATH));
  }

  @Test
  public void testConnect() throws IOException {
    Assertions.assertTrue(console.parse("connect " + DB_PATH + ";info types", false));
  }

  @Test
  public void testCreateClass() throws IOException {
    Assertions.assertTrue(console.parse("connect " + DB_PATH, false));
    Assertions.assertTrue(console.parse("create document type Person", false));

    final StringBuilder buffer = new StringBuilder();
    console.setOutput(new ConsoleOutput() {
      @Override
      public void onOutput(final String output) {
        buffer.append(output);
      }
    });
    Assertions.assertTrue(console.parse("info types", false));
    Assertions.assertTrue(buffer.toString().contains("Person"));
  }

  @Test
  public void testInsertAndSelectRecord() throws IOException {
    Assertions.assertTrue(console.parse("connect " + DB_PATH, false));
    Assertions.assertTrue(console.parse("create document type Person", false));
    Assertions.assertTrue(console.parse("insert into Person set name = 'Jay', lastname='Miner'", false));

    final StringBuilder buffer = new StringBuilder();
    console.setOutput(new ConsoleOutput() {
      @Override
      public void onOutput(final String output) {
        buffer.append(output);
      }
    });
    Assertions.assertTrue(console.parse("select from Person", false));
    Assertions.assertTrue(buffer.toString().contains("Jay"));
  }

  @Test
  public void testInsertAndRollback() throws IOException {
    Assertions.assertTrue(console.parse("connect " + DB_PATH, false));
    Assertions.assertTrue(console.parse("begin", false));
    Assertions.assertTrue(console.parse("create document type Person", false));
    Assertions.assertTrue(console.parse("insert into Person set name = 'Jay', lastname='Miner'", false));
    Assertions.assertTrue(console.parse("rollback", false));

    final StringBuilder buffer = new StringBuilder();
    console.setOutput(new ConsoleOutput() {
      @Override
      public void onOutput(final String output) {
        buffer.append(output);
      }
    });
    Assertions.assertTrue(console.parse("select from Person", false));
    Assertions.assertFalse(buffer.toString().contains("Jay"));
  }

  @Test
  public void testHelp() throws IOException {
    final StringBuilder buffer = new StringBuilder();
    console.setOutput(new ConsoleOutput() {
      @Override
      public void onOutput(final String output) {
        buffer.append(output);
      }
    });
    Assertions.assertTrue(console.parse("?", false));
    Assertions.assertTrue(buffer.toString().contains("quit"));
  }
}
