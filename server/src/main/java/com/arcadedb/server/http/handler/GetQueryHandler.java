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
import com.arcadedb.server.ServerMetrics;
import com.arcadedb.server.http.HttpServer;
import com.arcadedb.query.sql.executor.ResultSet;
import io.undertow.server.HttpServerExchange;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Deque;

public class GetQueryHandler extends DatabaseAbstractHandler {
  public GetQueryHandler(final HttpServer httpServer) {
    super(httpServer);
  }

  @Override
  public void execute(final HttpServerExchange exchange, final Database database) throws UnsupportedEncodingException {
    final Deque<String> text = exchange.getQueryParameters().get("command");
    if (text == null || text.isEmpty()) {
      exchange.setStatusCode(400);
      exchange.getResponseSender().send("{ \"error\" : \"Command text is null\"}");
      return;
    }

    final Deque<String> language = exchange.getQueryParameters().get("language");
    if (language == null || language.isEmpty()) {
      exchange.setStatusCode(400);
      exchange.getResponseSender().send("{ \"error\" : \"Language is null\"}");
      return;
    }

    final StringBuilder result = new StringBuilder();

    final ServerMetrics.MetricTimer timer = httpServer.getServer().getServerMetrics().timer("http.query");

    database.begin();
    try {

      final String command = URLDecoder.decode(text.getFirst(), exchange.getRequestCharset());
      final ResultSet qResult = database.query(language.getFirst(), command);
      while (qResult.hasNext()) {
        if (result.length() > 0)
          result.append(",");
        result.append(httpServer.getJsonSerializer().serializeResult(qResult.next()).toString());
      }

    } finally {
      database.rollbackAllNested();
      timer.stop();
    }

    exchange.setStatusCode(200);
    exchange.getResponseSender().send("{ \"result\" : [" + result.toString() + "] }");
  }
}
