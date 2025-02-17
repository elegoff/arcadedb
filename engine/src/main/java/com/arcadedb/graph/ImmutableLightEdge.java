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

import com.arcadedb.database.Binary;
import com.arcadedb.database.Database;
import com.arcadedb.database.ImmutableDocument;
import com.arcadedb.database.RID;
import com.arcadedb.schema.DocumentType;
import com.arcadedb.serializer.BinaryTypes;
import org.json.JSONObject;

import java.util.Collections;
import java.util.Map;

public class ImmutableLightEdge extends ImmutableDocument implements LightEdge {
  private RID out;
  private RID in;

  public ImmutableLightEdge(final Database graph, final DocumentType type, final RID edgeRID, final RID out, final RID in) {
    super(graph, type, edgeRID, null);
    this.out = out;
    this.in = in;
  }

  public ImmutableLightEdge(final Database graph, final DocumentType type, final Binary buffer) {
    super(graph, type, null, buffer);
    if (buffer != null) {
      buffer.position(1); // SKIP RECORD TYPE
      out = (RID) database.getSerializer().deserializeValue(graph, buffer, BinaryTypes.TYPE_COMPRESSED_RID, null);
      in = (RID) database.getSerializer().deserializeValue(graph, buffer, BinaryTypes.TYPE_COMPRESSED_RID, null);
      propertiesStartingPosition = buffer.position();
    }
  }

  @Override
  public Object get(final String propertyName) {
    return null;
  }

  public MutableEdge modify() {
    throw new IllegalStateException("Lightweight edges cannot be modified");
  }

  @Override
  public RID getOut() {
    checkForLazyLoading();
    return out;
  }

  @Override
  public Vertex getOutVertex() {
    checkForLazyLoading();
    return out.asVertex();
  }

  @Override
  public RID getIn() {
    checkForLazyLoading();
    return in;
  }

  @Override
  public Vertex getInVertex() {
    checkForLazyLoading();
    return in.asVertex();
  }

  @Override
  public Vertex getVertex(final Vertex.DIRECTION iDirection) {
    checkForLazyLoading();
    if (iDirection == Vertex.DIRECTION.OUT)
      return (Vertex) out.getRecord();
    else
      return (Vertex) in.getRecord();
  }

  @Override
  public byte getRecordType() {
    return Edge.RECORD_TYPE;
  }

  @Override
  protected boolean checkForLazyLoading() {
    return false;
  }

  @Override
  public Map<String, Object> toMap() {
    return Collections.emptyMap();
  }

  @Override
  public JSONObject toJSON() {
    return new JSONObject();
  }

  @Override
  public String toString() {
    final StringBuilder buffer = new StringBuilder();
    buffer.append(out.toString());
    buffer.append("<->");
    buffer.append(in.toString());
    return buffer.toString();
  }
}
