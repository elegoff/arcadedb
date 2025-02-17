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

import com.arcadedb.database.Database;
import com.arcadedb.database.Document;
import com.arcadedb.database.RID;
import com.arcadedb.server.http.HttpServer;
import io.undertow.server.HttpServerExchange;

import java.util.Deque;

public class GetDocumentHandler extends DatabaseAbstractHandler {
  public GetDocumentHandler(final HttpServer httpServer) {
    super(httpServer);
  }

  @Override
  public void execute(final HttpServerExchange exchange, final Database database) {
    final Deque<String> rid = exchange.getQueryParameters().get("rid");
    if (rid == null || rid.isEmpty()) {
      exchange.setStatusCode(400);
      exchange.getResponseSender().send("{ \"error\" : \"Record id is null\"}");
      return;
    }

    httpServer.getServer().getServerMetrics().meter("http.get-record").mark();

    final String[] ridParts = rid.getFirst().split(":");

    database.begin();
    try {
      final Document record = (Document) database.lookupByRID(new RID(database, Integer.parseInt(ridParts[0]), Long.parseLong(ridParts[1])), true);

      exchange.setStatusCode(200);
      exchange.getResponseSender().send("{ \"result\" : " + httpServer.getJsonSerializer().serializeRecord(record).toString() + "}");

    } finally {
      database.rollbackAllNested();
    }
  }
}
