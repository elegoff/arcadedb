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

public interface TestCallback {
  enum TYPE {SERVER_STARTING, SERVER_UP, SERVER_SHUTTING_DOWN, SERVER_DOWN, REPLICA_MSG_RECEIVED, REPLICA_ONLINE, REPLICA_OFFLINE, REPLICA_HOT_RESYNC, REPLICA_FULL_RESYNC, NETWORK_CONNECTION}

  void onEvent(TYPE type, Object object, ArcadeDBServer server) throws Exception;
}
