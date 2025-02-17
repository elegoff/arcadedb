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

package com.arcadedb;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static com.arcadedb.GlobalConfiguration.TEST;

public class ConfigurationTest {
  @Test
  public void testGlobalExport2Json() {
    Assertions.assertFalse(TEST.getValueAsBoolean());

    final String json = GlobalConfiguration.toJSON();

    TEST.setValue(true);

    Assertions.assertTrue(TEST.getValueAsBoolean());

    GlobalConfiguration.fromJSON(json);

    Assertions.assertFalse(TEST.getValueAsBoolean());
  }

  @Test
  public void testContextExport2Json() {
    final ContextConfiguration cfg = new ContextConfiguration();

    cfg.setValue(TEST, false);

    Assertions.assertFalse(cfg.getValueAsBoolean(TEST));

    final String json = cfg.toJSON();

    cfg.setValue(TEST, true);

    Assertions.assertTrue(cfg.getValueAsBoolean(TEST));

    cfg.fromJSON(json);

    Assertions.assertFalse(cfg.getValueAsBoolean(TEST));
  }
}
