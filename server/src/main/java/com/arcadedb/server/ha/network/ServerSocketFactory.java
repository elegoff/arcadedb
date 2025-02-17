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
package com.arcadedb.server.ha.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

public abstract class ServerSocketFactory {

  private static ServerSocketFactory theFactory;

  public ServerSocketFactory() {
  }

  public static ServerSocketFactory getDefault() {
    synchronized (ServerSocketFactory.class) {
      if (theFactory == null) {
        theFactory = new DefaultServerSocketFactory();
      }
    }

    return theFactory;
  }

  public abstract ServerSocket createServerSocket(int port) throws IOException;

  public abstract ServerSocket createServerSocket(int port, int backlog) throws IOException;

  public abstract ServerSocket createServerSocket(int port, int backlog, InetAddress ifAddress) throws IOException;
}
