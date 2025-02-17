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

/* Generated By:JJTree: Do not edit this line. ArraySingleValuesSelector.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_USERTYPE_VISIBILITY_PUBLIC=true */
package com.arcadedb.query.sql.parser;

import com.arcadedb.database.Identifiable;
import com.arcadedb.exception.CommandExecutionException;
import com.arcadedb.query.sql.executor.CommandContext;
import com.arcadedb.query.sql.executor.MultiValue;
import com.arcadedb.query.sql.executor.Result;
import com.arcadedb.query.sql.executor.ResultInternal;

import java.util.*;
import java.util.stream.Collectors;

public class ArraySingleValuesSelector extends SimpleNode {

  protected List<ArraySelector> items = new ArrayList<ArraySelector>();

  public ArraySingleValuesSelector(int id) {
    super(id);
  }

  public ArraySingleValuesSelector(SqlParser p, int id) {
    super(p, id);
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {
    boolean first = true;
    for (ArraySelector item : items) {
      if (!first) {
        builder.append(",");
      }
      item.toString(params, builder);
      first = false;
    }
  }

  public Object execute(Identifiable iCurrentRecord, Object iResult, CommandContext ctx) {
    List<Object> result = new ArrayList<Object>();
    for (ArraySelector item : items) {
      Object index = item.getValue(iCurrentRecord, iResult, ctx);
      if (index == null) {
        return null;
      }

      if (index instanceof Integer) {
        result.add(MultiValue.getValue(iResult, ((Integer) index).intValue()));
      } else {
        if (iResult instanceof Map) {
          result.add(((Map) iResult).get(index));
        } else if (iResult instanceof Result && index instanceof String) {
          result.add(((Result) iResult).getProperty((String) index));
        } else if (MultiValue.isMultiValue(iResult)) {
          Iterator<Object> iter = MultiValue.getMultiValueIterator(iResult);
          while (iter.hasNext()) {
            result.add(calculateValue(iter.next(), index));
          }
        } else {
          result.add(null);
        }
      }
      if (this.items.size() == 1 && result.size() == 1) {
        //      if (this.items.size() == 1) {
        return result.get(0);
      }
    }
    return result;
  }

  public Object execute(Result iCurrentRecord, Object iResult, CommandContext ctx) {
    List<Object> result = new ArrayList<Object>();
    for (ArraySelector item : items) {
      Object index = item.getValue(iCurrentRecord, iResult, ctx);
      if (index == null) {
        return null;
      }

      if (index instanceof Integer) {
        result.add(MultiValue.getValue(iResult, ((Integer) index).intValue()));
      } else {
        if (iResult instanceof Map) {
          result.add(((Map) iResult).get(index));
        } else if (iResult instanceof Result && index instanceof String) {
          result.add(((Result) iResult).getProperty((String) index));
        } else if (MultiValue.isMultiValue(iResult)) {
          Iterator<Object> iter = MultiValue.getMultiValueIterator(iResult);
          while (iter.hasNext()) {
            result.add(calculateValue(iter.next(), index));
          }
        } else {
          result.add(null);
        }
      }
      if (this.items.size() == 1 && result.size() == 1) {
        //      if (this.items.size() == 1) {
        return result.get(0);
      }
    }
    return result;
  }

  private Object calculateValue(Object item, Object index) {
    if (index instanceof Integer) {
      return MultiValue.getValue(item, ((Integer) index).intValue());
    } else if (item instanceof Map) {
      return ((Map) item).get(index);
    } else if (item instanceof Result && index instanceof String) {
      return ((Result) item).getProperty((String) index);
    } else if (MultiValue.isMultiValue(item)) {
      Iterator<Object> iter = MultiValue.getMultiValueIterator(item);
      List<Object> result = new ArrayList<>();
      while (iter.hasNext()) {
        result.add(calculateValue(iter.next(), index));
      }
      return null;
    } else {
      return null;
    }
  }

  public boolean needsAliases(Set<String> aliases) {
    for (ArraySelector item : items) {
      if (item.needsAliases(aliases)) {
        return true;
      }
    }
    return false;
  }

  public ArraySingleValuesSelector copy() {
    ArraySingleValuesSelector result = new ArraySingleValuesSelector(-1);
    result.items = items.stream().map(x -> x.copy()).collect(Collectors.toList());
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    ArraySingleValuesSelector that = (ArraySingleValuesSelector) o;

    if (items != null ? !items.equals(that.items) : that.items != null)
      return false;

    return true;
  }

  @Override
  public int hashCode() {
    return items != null ? items.hashCode() : 0;
  }

  public void extractSubQueries(SubQueryCollector collector) {
    if (items != null) {
      for (ArraySelector item : items) {
        item.extractSubQueries(collector);
      }
    }
  }

  public boolean refersToParent() {
    if (items != null) {
      for (ArraySelector item : items) {
        if (item.refersToParent()) {
          return true;
        }
      }
    }
    return false;
  }

  public void setValue(Result currentRecord, Object target, Object value, CommandContext ctx) {
    if (items != null) {
      for (ArraySelector item : items) {
        item.setValue(currentRecord, target, value, ctx);
      }
    }
  }

  public void applyRemove(Object currentValue, ResultInternal originalRecord, CommandContext ctx) {
    if (currentValue == null) {
      return;
    }
    List values = this.items.stream().map(x -> x.getValue(originalRecord, null, ctx)).collect(Collectors.toList());
    if (currentValue instanceof List) {
      List<Object> list = (List) currentValue;
      Collections.sort(values, this::compareKeysForRemoval);
      for (Object val : values) {
        if (val instanceof Integer) {
          list.remove((int) (Integer) val);
        } else {
          list.remove(val);
        }
      }
    } else if (currentValue instanceof Set) {
      Set set = (Set) currentValue;
      Iterator iterator = set.iterator();
      int count = 0;
      while (iterator.hasNext()) {
        Object item = iterator.next();
        if (values.contains(count) || values.contains(item)) {
          iterator.remove();
        }
      }
    } else if (currentValue instanceof Map) {
      for (Object val : values) {
        ((Map) currentValue).remove(val);
      }
    } else if (currentValue instanceof Result) {
      for (Object val : values) {
        ((ResultInternal) currentValue).removeProperty("" + val);
      }
    } else {
      throw new CommandExecutionException("Trying to remove elements from " + currentValue + " (" + currentValue.getClass().getSimpleName() + ")");
    }
  }

  private int compareKeysForRemoval(Object o1, Object o2) {
    if (o1 instanceof Integer) {
      if (o2 instanceof Integer) {
        return (int) o2 - (int) o1;
      } else {
        return -1;
      }
    } else if (o2 instanceof Integer) {
      return 1;
    } else {
      return 0;
    }
  }

  public Result serialize() {
    ResultInternal result = new ResultInternal();
    if (items != null) {
      result.setProperty("items", items.stream().map(x -> x.serialize()).collect(Collectors.toList()));
    }
    return result;
  }

  public void deserialize(Result fromResult) {

    if (fromResult.getProperty("items") != null) {
      List<Result> ser = fromResult.getProperty("items");
      items = new ArrayList<>();
      for (Result r : ser) {
        ArraySelector exp = new ArraySelector(-1);
        exp.deserialize(r);
        items.add(exp);
      }
    }
  }
}
/* JavaCC - OriginalChecksum=991998c77a4831184b6dca572513fd8d (do not edit this line) */
