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

package org.apache.tinkerpop.gremlin.arcadedb.structure;

import com.arcadedb.database.MutableDocument;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.util.ElementHelper;

import java.util.*;

/**
 * Created by Enrico Risa on 30/07/2018.
 */
public abstract class ArcadeElement<T extends MutableDocument> implements Element {

  protected final T           baseElement;
  protected final ArcadeGraph graph;

  protected ArcadeElement(final ArcadeGraph graph, final T baseElement) {
    this.baseElement = baseElement;
    this.graph = graph;
  }

  @Override
  public String label() {
    return baseElement.getTypeName();
  }

  @Override
  public <V> V value(final String key) throws NoSuchElementException {
    final V value = (V) baseElement.get(key);
    if (value == null)
      throw Property.Exceptions.propertyDoesNotExist(this, key);
    return value;
  }

  @Override
  public <V> Iterator<V> values(final String... propertyKeys) {
    final List<V> props;
    if (propertyKeys == null || propertyKeys.length == 0) {
      final Set<String> propNames = baseElement.getPropertyNames();
      props = new ArrayList<>(propNames.size());
      for (String p : propNames) {
        final V value = (V) baseElement.get(p);
        if (value != null)
          props.add(value);
      }
    } else {
      props = new ArrayList<>(propertyKeys.length);
      for (String p : propertyKeys) {
        final V value = (V) baseElement.get(p);
        if (value != null)
          props.add(value);
      }
    }
    return props.iterator();
  }

  @Override
  public void remove() {
    this.graph.tx().readWrite();
    this.graph.deleteElement(this);
  }

  @Override
  public Object id() {
    return baseElement.getIdentity();
  }

  @Override
  public Graph graph() {
    return this.graph;
  }

  @Override
  public boolean equals(final Object object) {
    return ElementHelper.areEqual(this, object);
  }

  @Override
  public int hashCode() {
    return ElementHelper.hashCode(this);
  }

  public T getBaseElement() {
    return baseElement;
  }
}
