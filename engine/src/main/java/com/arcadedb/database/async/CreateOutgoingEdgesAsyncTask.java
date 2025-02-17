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

package com.arcadedb.database.async;

import com.arcadedb.database.DatabaseInternal;
import com.arcadedb.graph.Edge;
import com.arcadedb.graph.GraphEngine;
import com.arcadedb.graph.VertexInternal;

import java.util.List;

/**
 * Asynchronous Task that creates the relationship between the sourceVertex and the destinationVertex as outgoing.
 */
public class CreateOutgoingEdgesAsyncTask extends DatabaseAsyncAbstractTask {
  private final VertexInternal                        sourceVertex;
  private final List<GraphEngine.CreateEdgeOperation> connections;
  private final boolean                               edgeBidirectional;
  private final NewEdgesCallback                      callback;

  public CreateOutgoingEdgesAsyncTask(final VertexInternal sourceVertex, final List<GraphEngine.CreateEdgeOperation> connections,
      final boolean edgeBidirectional, final NewEdgesCallback callback) {
    this.sourceVertex = sourceVertex;
    this.connections = connections;
    this.edgeBidirectional = edgeBidirectional;
    this.callback = callback;
  }

  public void execute(final DatabaseAsyncExecutorImpl.AsyncThread async, final DatabaseInternal database) {
    final List<Edge> edges = database.getGraphEngine().newEdges(database, sourceVertex, connections, edgeBidirectional);

    if (callback != null)
      callback.call(edges);
  }

  @Override
  public String toString() {
    return "CreateOutgoingEdgesAsyncTask(" + sourceVertex + "->" + connections.size() + ")";
  }
}
