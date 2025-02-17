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

package com.arcadedb.network.binary;

import com.arcadedb.database.RID;

import java.io.IOException;
import java.io.OutputStream;

public interface ChannelDataOutput {

  ChannelDataOutput writeByte(final byte iContent) throws IOException;

  ChannelDataOutput writeBoolean(final boolean iContent) throws IOException;

  ChannelDataOutput writeInt(final int iContent) throws IOException;

  ChannelDataOutput writeLong(final long iContent) throws IOException;

  ChannelDataOutput writeShort(final short iContent) throws IOException;

  ChannelDataOutput writeString(final String iContent) throws IOException;

  ChannelDataOutput writeVarLengthBytes(final byte[] iContent) throws IOException;

  ChannelDataOutput writeVarLengthBytes(final byte[] iContent, final int iLength) throws IOException;

  void writeRID(final RID iRID) throws IOException;

  void writeVersion(final int version) throws IOException;

  OutputStream getDataOutput();

}
