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

package com.arcadedb.database;

import com.arcadedb.ContextConfiguration;
import com.arcadedb.database.async.DatabaseAsyncExecutor;
import com.arcadedb.database.async.ErrorCallback;
import com.arcadedb.database.async.OkCallback;
import com.arcadedb.engine.PaginatedFile;
import com.arcadedb.graph.Edge;
import com.arcadedb.graph.MutableVertex;
import com.arcadedb.graph.Vertex;
import com.arcadedb.index.IndexCursor;
import com.arcadedb.query.sql.executor.ResultSet;
import com.arcadedb.schema.Schema;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;

public interface Database extends AutoCloseable {
  ContextConfiguration getConfiguration();

  String getName();

  PaginatedFile.MODE getMode();

  @Override
  void close();

  boolean isOpen();

  void drop();

  DatabaseAsyncExecutor async();

  String getDatabasePath();

  Transaction getTransaction();

  boolean isTransactionActive();

  boolean checkTransactionIsActive(boolean createTx);

  /**
   * Executes a lambda in the transaction scope. If there is an active transaction, then the current transaction is parked and a new sub-transaction is begun.
   *
   * @param txBlock Transaction lambda to execute
   */
  void transaction(TransactionScope txBlock);

  /**
   * Executes a lambda in the transaction scope. If there is an active transaction, then the current transaction is parked and a new sub-transaction is begun
   * if joinCurrentTx is true, otherwise the current active transaction is joined.
   *
   * @param txBlock       Transaction lambda to execute
   * @param joinCurrentTx if active joins the current transaction, otherwise always create a new one
   *
   * @return true if a new transaction has been created or false if an existent transaction has been joined
   */
  boolean transaction(TransactionScope txBlock, boolean joinCurrentTx);

  /**
   * Executes a lambda in the transaction scope. If there is an active transaction, then the current transaction is parked and a new sub-transaction is begun
   * if joinCurrentTx is true, otherwise the current active transaction is joined.
   * The difference with the method {@link #transaction(TransactionScope)} is that in case the NeedRetryException exception is thrown, the transaction is
   * re-executed for a number of retries.
   *
   * @param txBlock       Transaction lambda to execute
   * @param joinCurrentTx if active joins the current transaction, otherwise always create a new one
   * @param retries       number of retries in case the NeedRetryException exception is thrown
   *
   * @return true if a new transaction has been created or false if an existent transaction has been joined
   */
  boolean transaction(TransactionScope txBlock, boolean joinCurrentTx, int retries);

  /**
   * Executes a lambda in the transaction scope. If there is an active transaction, then the current transaction is parked and a new sub-transaction is begun
   * if joinCurrentTx is true, otherwise the current active transaction is joined.
   * The difference with the method {@link #transaction(TransactionScope)} is that in case the NeedRetryException exception is thrown, the transaction is
   * re-executed for a number of retries.
   *
   * @param txBlock       Transaction lambda to execute
   * @param joinCurrentTx if active joins the current transaction, otherwise always create a new one
   * @param retries       number of retries in case the NeedRetryException exception is thrown
   * @param ok            callback invoked if the transaction completes the commit
   * @param error         callback invoked if the transaction cannot complete the commit, after the rollback
   *
   * @return true if a new transaction has been created or false if an existent transaction has been joined
   */
  boolean transaction(TransactionScope txBlock, boolean joinCurrentTx, int retries, final OkCallback ok, final ErrorCallback error);

  boolean isAutoTransaction();

  void setAutoTransaction(boolean autoTransaction);

  /**
   * Begins a new transaction. If a transaction is already begun, the current transaction is parked and a new sub-transaction is begun. The new sub-transaction
   * does not access to the content of the previous transaction. Sub transactions are totally isolated.
   */
  void begin();

  /**
   * Commits the current transaction. If it was a sub-transaction, then the previous in the stack becomes active again.
   */
  void commit();

  /**
   * Rolls back the current transaction. If it was a sub-transaction, then the previous in the stack becomes active again.
   */
  void rollback();

  /**
   * Rolls back all the nested transactions if any.
   */
  void rollbackAllNested();

  void scanType(String typeName, boolean polymorphic, DocumentCallback callback);

  void scanBucket(String bucketName, RecordCallback callback);

  Record lookupByRID(RID rid, boolean loadContent);

  IndexCursor lookupByKey(String type, String keyName, Object keyValue);

  IndexCursor lookupByKey(String type, String[] keyNames, Object[] keyValues);

  Iterator<Record> iterateType(String typeName, boolean polymorphic);

  Iterator<Record> iterateBucket(String bucketName);

  void deleteRecord(Record record);

  long countType(String typeName, boolean polymorphic);

  long countBucket(String bucketName);

  MutableDocument newDocument(String typeName);

  MutableVertex newVertex(String typeName);

  Edge newEdgeByKeys(String sourceVertexType, String[] sourceVertexKeyNames, Object[] sourceVertexKeyValues, String destinationVertexType,
      String[] destinationVertexKeyNames, Object[] destinationVertexKeyValues, boolean createVertexIfNotExist, String edgeType, boolean bidirectional,
      Object... properties);

  Edge newEdgeByKeys(Vertex sourceVertex, String destinationVertexType, String[] destinationVertexKeyNames, Object[] destinationVertexKeyValues,
      boolean createVertexIfNotExist, String edgeType, boolean bidirectional, Object... properties);

  Schema getSchema();

  ResultSet command(String language, String query, Map<String, Object> args);

  ResultSet command(String language, String query, Object... args);

  ResultSet query(String language, String query, Object... args);

  ResultSet query(String language, String query, Map<String, Object> args);

  ResultSet execute(String language, String script, Object... args);

  ResultSet execute(String language, String script, Map<Object, Object> args);

  <RET extends Object> RET executeInReadLock(Callable<RET> callable);

  <RET extends Object> RET executeInWriteLock(Callable<RET> callable);

  boolean isReadYourWrites();

  void setReadYourWrites(boolean value);

  void setEdgeListSize(int size);

  interface TransactionScope {
    void execute(Database db);
  }
}
