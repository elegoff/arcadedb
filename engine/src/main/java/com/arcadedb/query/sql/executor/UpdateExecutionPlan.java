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

package com.arcadedb.query.sql.executor;

/**
 * Created by luigidellaquila on 08/08/16.
 */

import com.arcadedb.exception.CommandExecutionException;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Luigi Dell'Aquila (luigi.dellaquila-(at)-gmail.com)
 */
public class UpdateExecutionPlan extends SelectExecutionPlan {

  List<Result> result = new ArrayList<>();
  int          next   = 0;

  public UpdateExecutionPlan(CommandContext ctx) {
    super(ctx);
  }

  @Override
  public ResultSet fetchNext(int n) {
    if (next >= result.size()) {
      return new InternalResultSet();//empty
    }

    IteratorResultSet nextBlock = new IteratorResultSet(result.subList(next, Math.min(next + n, result.size())).iterator());
    next += n;
    return nextBlock;
  }

  @Override
  public void reset(CommandContext ctx) {
    result.clear();
    next = 0;
    super.reset(ctx);
    executeInternal();
  }

  public void executeInternal() throws CommandExecutionException {
    while (true) {
      ResultSet nextBlock = super.fetchNext(100);
      if (!nextBlock.hasNext()) {
        return;
      }
      while (nextBlock.hasNext()) {
        result.add(nextBlock.next());
      }
    }
  }

  @Override
  public Result toResult() {
    ResultInternal res = (ResultInternal) super.toResult();
    res.setProperty("type", "UpdateExecutionPlan");
    return res;
  }

  @Override
  public boolean canBeCached() {
    return false;
  }
}
