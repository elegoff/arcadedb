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

package com.arcadedb.graph;

import com.arcadedb.database.RID;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

public class VertexIterator implements Iterator<Vertex>, Iterable<Vertex> {
  private       EdgeSegment   currentContainer;
  private final AtomicInteger currentPosition = new AtomicInteger(MutableEdgeSegment.CONTENT_START_POSITION);

  public VertexIterator(final EdgeSegment current) {
    if (current == null)
      throw new IllegalArgumentException("Edge chunk is null");
    this.currentContainer = current;
  }

  @Override
  public boolean hasNext() {
    if (currentContainer == null)
      return false;

    if (currentPosition.get() < currentContainer.getUsed())
      return true;

    currentContainer = currentContainer.getNext();
    if (currentContainer != null) {
      currentPosition.set(MutableEdgeSegment.CONTENT_START_POSITION);
      return currentPosition.get() < currentContainer.getUsed();
    }
    return false;
  }

  @Override
  public Vertex next() {
    if (!hasNext())
      throw new NoSuchElementException();

    currentContainer.getRID(currentPosition); // SKIP EDGE
    final RID rid = currentContainer.getRID(currentPosition);

    return rid.asVertex();
  }

  @Override
  public Iterator<Vertex> iterator() {
    return this;
  }
}
