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

import com.arcadedb.database.*;
import com.arcadedb.serializer.BinaryTypes;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class MutableEdgeSegment extends BaseRecord implements EdgeSegment, RecordInternal {
  public static final  byte RECORD_TYPE            = 3;
  public static final  int  CONTENT_START_POSITION = Binary.BYTE_SERIALIZED_SIZE + Binary.INT_SERIALIZED_SIZE + BinaryTypes.getTypeSize(BinaryTypes.TYPE_RID);
  private static final RID  NULL_RID               = new RID(null, -1, -1);

  private int bufferSize;

  public MutableEdgeSegment(final Database database, final RID rid) {
    super(database, rid, null);
    this.buffer = null;
  }

  public MutableEdgeSegment(final Database database, final RID rid, final Binary buffer) {
    super(database, rid, buffer);
    this.buffer = buffer;
    this.buffer.setAutoResizable(false);
    this.bufferSize = buffer.size();
  }

  public MutableEdgeSegment(final DatabaseInternal database, final int bufferSize) {
    super(database, null, new Binary(bufferSize));
    this.buffer.setAutoResizable(false);
    this.bufferSize = bufferSize;
    buffer.putByte(0, RECORD_TYPE);
    buffer.putInt(Binary.BYTE_SERIALIZED_SIZE, CONTENT_START_POSITION);
    buffer.position(Binary.BYTE_SERIALIZED_SIZE + Binary.INT_SERIALIZED_SIZE);
    database.getSerializer().serializeValue(database, buffer, BinaryTypes.TYPE_RID, NULL_RID); // NEXT
  }

  @Override
  public byte getRecordType() {
    return RECORD_TYPE;
  }

  @Override
  public boolean add(final RID edgeRID, final RID vertexRID) {
    final Binary ridSerialized = database.getContext().getTemporaryBuffer1();
    database.getSerializer().serializeValue(database, ridSerialized, BinaryTypes.TYPE_COMPRESSED_RID, edgeRID);
    database.getSerializer().serializeValue(database, ridSerialized, BinaryTypes.TYPE_COMPRESSED_RID, vertexRID);

    final int used = getUsed();

    if (used + ridSerialized.size() <= bufferSize) {
      // APPEND AT THE END OF THE CURRENT CHUNK
      buffer.putByteArray(used, ridSerialized.getContent(), ridSerialized.size());

      // UPDATE USED BYTES
      buffer.putInt(Binary.BYTE_SERIALIZED_SIZE, used + ridSerialized.size());
      // TODO save()

      return true;
    }

    // NO ROOM
    return false;
  }

  @Override
  public boolean containsEdge(final RID rid) {
    final int used = getUsed();
    if (used == 0)
      return false;

    final int bucketId = rid.getBucketId();
    final long position = rid.getPosition();

    buffer.position(CONTENT_START_POSITION);

    while (buffer.position() < used) {
      final int currEdgeBucketId = (int) buffer.getNumber();
      final long currEdgePosition = buffer.getNumber();
      if (currEdgeBucketId == bucketId && currEdgePosition == position)
        return true;

      // SKIP VERTEX RID
      buffer.getNumber();
      buffer.getNumber();
    }

    return false;
  }

  @Override
  public boolean containsVertex(final RID rid) {
    final int used = getUsed();
    if (used == 0)
      return false;

    final int bucketId = rid.getBucketId();
    final long position = rid.getPosition();

    buffer.position(CONTENT_START_POSITION);

    while (buffer.position() < used) {
      // SKIP EDGE RID
      buffer.getNumber();
      buffer.getNumber();

      final int currEdgeBucketId = (int) buffer.getNumber();
      final long currEdgePosition = buffer.getNumber();
      if (currEdgeBucketId == bucketId && currEdgePosition == position)
        return true;
    }

    return false;
  }

  @Override
  public JSONObject toJSON() {
    final JSONObject json = new JSONObject();
    final int used = getUsed();
    if (used > 0) {
      final JSONArray array = new JSONArray();

      buffer.position(CONTENT_START_POSITION);
      while (buffer.position() < used) {
        // EDGE RID
        array.put("#" + buffer.getNumber() + ":" + buffer.getNumber());
        // VERTEX RID
        array.put("#" + buffer.getNumber() + ":" + buffer.getNumber());
      }

      if (array.length() > 0)
        json.put("array", array);
    }
    return json;
  }

  @Override
  public boolean removeEntry(final int index) {
    int used = getUsed();
    if (used == 0)
      return false;

    if (index > used)
      return false;

    buffer.position(index);

    // MOVE THE ENTIRE BUFFER FROM THE NEXT ITEM TO THE CURRENT ONE
    buffer.move(buffer.position(), index, used - buffer.position());

    used -= (buffer.position() - index);
    setUsed(used);

    buffer.position(index);
    return true;
  }

  @Override
  public int removeEdge(final RID rid) {
    int used = getUsed();
    if (used == 0)
      return 0;

    final int bucketId = rid.getBucketId();
    final long position = rid.getPosition();

    buffer.position(CONTENT_START_POSITION);

    int found = 0;
    while (buffer.position() < used) {
      final int lastPos = buffer.position();

      final int currEdgeBucketId = (int) buffer.getNumber();
      final long currEdgePosition = buffer.getNumber();

      buffer.getNumber();
      buffer.getNumber();

      if (currEdgeBucketId == bucketId && currEdgePosition == position) {
        // FOUND MOVE THE ENTIRE BUFFER FROM THE NEXT ITEM TO THE CURRENT ONE
        buffer.move(buffer.position(), lastPos, used - buffer.position());

        used -= (buffer.position() - lastPos);
        setUsed(used);

        buffer.position(lastPos);
        ++found;
      }
    }

    return found;
  }

  @Override
  public int removeVertex(final RID rid) {
    int used = getUsed();
    if (used == 0)
      return 0;

    final int bucketId = rid.getBucketId();
    final long position = rid.getPosition();

    buffer.position(CONTENT_START_POSITION);

    int found = 0;
    while (buffer.position() < used) {
      final int lastPos = buffer.position();

      buffer.getNumber();
      buffer.getNumber();

      final int currVertexBucketId = (int) buffer.getNumber();
      final long currVertexPosition = buffer.getNumber();

      if (currVertexBucketId == bucketId && currVertexPosition == position) {
        // FOUND MOVE THE ENTIRE BUFFER FROM THE NEXT ITEM TO THE CURRENT ONE
        buffer.move(buffer.position(), lastPos, used - buffer.position());

        used -= (buffer.position() - lastPos);
        setUsed(used);

        buffer.position(lastPos);
        ++found;
      }
    }

    return found;
  }

  @Override
  public long count(final Set<Integer> fileIds) {
    long total = 0;

    final int used = getUsed();
    if (used > 0) {
      buffer.position(CONTENT_START_POSITION);

      while (buffer.position() < used) {
        final int fileId = (int) buffer.getNumber();
        // SKIP EDGE RID POSITION AND VERTEX RID
        buffer.getNumber();
        buffer.getNumber();
        buffer.getNumber();

        if (fileIds != null) {
          if (fileIds.contains(fileId))
            ++total;
        } else
          ++total;
      }
    }

    return total;
  }

  @Override
  public EdgeSegment getNext() {
    buffer.position(Binary.BYTE_SERIALIZED_SIZE + Binary.INT_SERIALIZED_SIZE);

    final RID nextRID = (RID) database.getSerializer().deserializeValue(database, buffer, BinaryTypes.TYPE_RID, null); // NEXT

    if (nextRID.getBucketId() == -1 && nextRID.getPosition() == -1)
      return null;

    return (EdgeSegment) database.lookupByRID(nextRID, true);
  }

  @Override
  public void setNext(final EdgeSegment next) {
    final RID nextRID = next.getIdentity();
    if (nextRID == null)
      throw new IllegalArgumentException("Next chunk is not persistent");
    buffer.position(Binary.BYTE_SERIALIZED_SIZE + Binary.INT_SERIALIZED_SIZE);
    database.getSerializer().serializeValue(database, buffer, BinaryTypes.TYPE_RID, nextRID); // NEXT
  }

  @Override
  public Binary getContent() {
    buffer.position(bufferSize);
    buffer.flip();
    return buffer;
  }

  @Override
  public int getUsed() {
    return buffer.getInt(Binary.BYTE_SERIALIZED_SIZE);
  }

  private void setUsed(final int size) {
    buffer.putInt(Binary.BYTE_SERIALIZED_SIZE, size);
  }

  @Override
  public RID getRID(final AtomicInteger currentPosition) {
    buffer.position(currentPosition.get());
    final RID next = (RID) database.getSerializer().deserializeValue(database, buffer, BinaryTypes.TYPE_COMPRESSED_RID, null); // NEXT
    currentPosition.set(buffer.position());
    return next;
  }

  @Override
  public int getRecordSize() {
    return buffer.size();
  }

  @Override
  public void setIdentity(final RID rid) {
    this.rid = rid;
  }

  @Override
  public void unsetDirty() {
  }
}
