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

package com.arcadedb.server.http.handler;

import com.arcadedb.server.ha.HAServer;
import com.arcadedb.server.http.HttpServer;
import com.arcadedb.server.security.ServerSecurity;
import io.undertow.server.HttpServerExchange;

import java.util.logging.Level;

public class ServersHandler extends AbstractHandler {
  public ServersHandler(final HttpServer httpServer) {
    super(httpServer);
  }

  @Override
  public void execute(final HttpServerExchange exchange, ServerSecurity.ServerUser user) {
    exchange.setStatusCode(200);

    final HAServer ha = httpServer.getServer().getHA();
    if (ha == null) {
      exchange.getResponseSender().send("{}");
    } else {
      final String leaderServer = ha.isLeader() ? ha.getServer().getHttpServer().getListeningAddress() : ha.getLeader().getRemoteHTTPAddress();
      final String replicaServers = ha.getReplicaServersHTTPAddressesList();

      httpServer.getServer().log(this, Level.INFO, "Returning configuration leaderServer=%s replicaServers=[%s]", leaderServer, replicaServers);

      exchange.getResponseSender().send("{ \"leaderServer\": \"" + leaderServer + "\", \"replicaServers\" : \"" + replicaServers + "\"}");
    }
  }
}
