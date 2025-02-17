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

package com.arcadedb.server;

import com.arcadedb.ContextConfiguration;
import com.arcadedb.utility.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static com.arcadedb.GlobalConfiguration.TX_WAL;

public class ServerConfigurationIT extends BaseGraphServerTest {
  @Test
  public void testServerLoadConfiguration() throws IOException {
    final ContextConfiguration cfg = new ContextConfiguration();

    Assertions.assertTrue(cfg.getValueAsBoolean(TX_WAL));

    cfg.setValue(TX_WAL, false);

    Assertions.assertFalse(cfg.getValueAsBoolean(TX_WAL));

    final File file = new File(getServer(0).getRootPath() + "/" + ArcadeDBServer.CONFIG_SERVER_CONFIGURATION_FILENAME);
    if (file.exists())
      file.delete();

    FileUtils.writeFile(file, cfg.toJSON());
    try {

      final ArcadeDBServer server = new ArcadeDBServer();
      server.start();

      Assertions.assertFalse(server.getConfiguration().getValueAsBoolean(TX_WAL));
    } finally {
      if (file.exists())
        file.delete();
    }
  }
}
