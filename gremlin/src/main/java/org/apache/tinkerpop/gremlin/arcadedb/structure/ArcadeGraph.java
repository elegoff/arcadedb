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

import com.arcadedb.database.Database;
import com.arcadedb.database.DatabaseFactory;
import com.arcadedb.database.RID;
import com.arcadedb.database.Record;
import com.arcadedb.engine.Bucket;
import com.arcadedb.exception.RecordNotFoundException;
import com.arcadedb.graph.MutableEdge;
import com.arcadedb.graph.MutableVertex;
import com.arcadedb.query.sql.executor.ResultSet;
import com.arcadedb.schema.DocumentType;
import com.arcadedb.schema.EdgeType;
import com.arcadedb.schema.VertexType;
import com.arcadedb.utility.FileUtils;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.tinkerpop.gremlin.arcadedb.structure.io.ArcadeIoRegistry;
import org.apache.tinkerpop.gremlin.process.computer.GraphComputer;
import org.apache.tinkerpop.gremlin.structure.*;
import org.apache.tinkerpop.gremlin.structure.io.Io;
import org.apache.tinkerpop.gremlin.structure.util.ElementHelper;
import org.apache.tinkerpop.gremlin.structure.util.StringFactory;

import java.io.File;
import java.util.*;

/**
 * Created by Enrico Risa on 30/07/2018.
 */

@Graph.OptIn(Graph.OptIn.SUITE_STRUCTURE_STANDARD)
@Graph.OptIn(Graph.OptIn.SUITE_STRUCTURE_INTEGRATE)
@Graph.OptIn(Graph.OptIn.SUITE_PROCESS_STANDARD)
@Graph.OptIn(Graph.OptIn.SUITE_PROCESS_COMPUTER)
@Graph.OptIn("org.apache.tinkerpop.gremlin.arcadedb.suite.ArcadeDebugSuite")
public class ArcadeGraph implements Graph {

  private final   ArcadeVariableFeatures graphVariables = new ArcadeVariableFeatures();
  private final   ArcadeGraphTransaction transaction;
  protected final Database               database;
  protected final BaseConfiguration      configuration  = new BaseConfiguration();

  private final static Iterator<Vertex> EMPTY_VERTICES = Collections.emptyIterator();
  private final static Iterator<Edge>   EMPTY_EDGES    = Collections.emptyIterator();

  protected Features features = new ArcadeGraphFeatures();

  protected ArcadeGraph(final Configuration configuration) {
    this.configuration.copy(configuration);
    final String directory = this.configuration.getString(CONFIG_DIRECTORY);
    final DatabaseFactory factory = new DatabaseFactory(directory);

    if (!factory.exists())
      this.database = factory.create();
    else
      this.database = factory.open();

    this.transaction = new ArcadeGraphTransaction(this);
  }

  protected ArcadeGraph(final Database database) {
    this.database = database;
    this.transaction = new ArcadeGraphTransaction(this);
  }

  public static final String CONFIG_DIRECTORY = "gremlin.arcadedb.directory";

  @Override
  public Features features() {
    return features;
  }

  public static ArcadeGraph open(final Configuration configuration) {
    if (null == configuration)
      throw Graph.Exceptions.argumentCanNotBeNull("configuration");
    if (!configuration.containsKey(CONFIG_DIRECTORY))
      throw new IllegalArgumentException(String.format("Arcade configuration requires that the %s be set", CONFIG_DIRECTORY));
    return new ArcadeGraph(configuration);
  }

  public static ArcadeGraph open(final String directory) {
    final Configuration config = new BaseConfiguration();
    config.setProperty(CONFIG_DIRECTORY, directory);
    return open(config);
  }

  public static ArcadeGraph open(final Database database) {
    return new ArcadeGraph(database);
  }

  public ArcadeCypher cypher(final String query) {
    return new ArcadeCypher(this, query);
  }

  public ArcadeGremlin gremlin(final String query) {
    return new ArcadeGremlin(this, query);
  }

  public ArcadeSQL sql(final String query) {
    return new ArcadeSQL(this, query);
  }

  @Override
  public Vertex addVertex(final Object... keyValues) {
    ElementHelper.legalPropertyKeyValueArray(keyValues);
    if (ElementHelper.getIdValue(keyValues).isPresent())
      throw Vertex.Exceptions.userSuppliedIdsNotSupported();
    this.tx().readWrite();

    String label = ElementHelper.getLabelValue(keyValues).orElse(Vertex.DEFAULT_LABEL);

    if (!this.database.getSchema().existsType(label)) {
      this.database.getSchema().createVertexType(label);
    }
    final MutableVertex modifiableVertex = this.database.newVertex(label);
    final ArcadeVertex vertex = new ArcadeVertex(this, modifiableVertex);
    ElementHelper.attachProperties(vertex, keyValues);
    modifiableVertex.save();
    return vertex;
  }

  @Override
  public <C extends GraphComputer> C compute(Class<C> graphComputerClass) throws IllegalArgumentException {
    throw Graph.Exceptions.graphComputerNotSupported();
  }

  @Override
  public GraphComputer compute() throws IllegalArgumentException {
    throw Graph.Exceptions.graphComputerNotSupported();
  }

  @Override
  public Iterator<Vertex> vertices(final Object... vertexIds) {
    tx().readWrite();

    if (vertexIds.length == 0) {
      final Collection<DocumentType> types = this.database.getSchema().getTypes();
      final Set<Bucket> buckets = new HashSet<>();
      for (DocumentType t : types)
        if (t instanceof VertexType)
          buckets.addAll(t.getBuckets(true));

      if (buckets.isEmpty())
        return EMPTY_VERTICES;

      // BUILD THE QUERY
      final StringBuilder query = new StringBuilder("select from bucket:[");
      int i = 0;
      for (Bucket b : buckets) {
        if (i > 0)
          query.append(", ");
        query.append("`");
        query.append(b.getName());
        query.append("`");
        ++i;
      }
      query.append("]");

      final ResultSet resultset = this.database.query("sql", query.toString());
      return resultset.stream().map(result -> (Vertex) new ArcadeVertex(this, (MutableVertex) (result.toElement()).modify())).iterator();

    }

    ElementHelper.validateMixedElementIds(Vertex.class, vertexIds);

    final List<Vertex> resultset = new ArrayList<>();

    for (Object o : vertexIds) {
      final RID rid;
      if (o instanceof RID)
        rid = (RID) o;
      else if (o instanceof Vertex)
        rid = (RID) ((Vertex) o).id();
      else if (o instanceof String)
        rid = new RID(database, (String) o);
      else
        continue;

      try {
        final Record r = database.lookupByRID(rid, true);
        if (r instanceof com.arcadedb.graph.Vertex)
          resultset.add(new ArcadeVertex(this, ((com.arcadedb.graph.Vertex) r).modify()));
      } catch (RecordNotFoundException e) {
        // NP, IGNORE IT
      }
    }

    return resultset.iterator();
  }

  @Override
  public Iterator<Edge> edges(final Object... edgeIds) {
    tx().readWrite();

    if (edgeIds.length == 0) {

      final Collection<DocumentType> types = this.database.getSchema().getTypes();
      final Set<Bucket> buckets = new HashSet<>();
      for (DocumentType t : types)
        if (t instanceof EdgeType)
          buckets.addAll(t.getBuckets(true));

      if (buckets.isEmpty())
        return EMPTY_EDGES;

      // BUILD THE QUERY
      final StringBuilder query = new StringBuilder("select from bucket:[");
      int i = 0;
      for (Bucket b : buckets) {
        if (i > 0)
          query.append(", ");
        query.append("`");
        query.append(b.getName());
        query.append("`");
        ++i;
      }
      query.append("]");

      final ResultSet resultset = this.database.query("sql", query.toString());
      return resultset.stream().map(result -> (Edge) new ArcadeEdge(this, (MutableEdge) (result.toElement()).modify())).iterator();

    }

    ElementHelper.validateMixedElementIds(Vertex.class, edgeIds);

    final List<Edge> resultset = new ArrayList<>();

    for (Object o : edgeIds) {
      final RID rid;
      if (o instanceof RID)
        rid = (RID) o;
      else if (o instanceof Edge)
        rid = (RID) ((Edge) o).id();
      else if (o instanceof String)
        rid = new RID(database, (String) o);
      else
        continue;

      try {
        final Record r = database.lookupByRID(rid, true);
        if (r instanceof com.arcadedb.graph.Edge)
          resultset.add(new ArcadeEdge(this, ((com.arcadedb.graph.Edge) r).modify()));
      } catch (RecordNotFoundException e) {
        // NP, IGNORE IT
      }
    }

    return resultset.iterator();
  }

  @Override
  public <I extends Io> I io(Io.Builder<I> builder) {
    return (I) Graph.super.io(builder.onMapper(mb -> mb.addRegistry(new ArcadeIoRegistry(this.database))));
  }

  @Override
  public Transaction tx() {
    return transaction;
  }

  @Override
  public void close() {
    if (this.database != null) {
      if (this.database.isTransactionActive())
        this.database.commit();

      this.database.close();
    }
  }

  public void drop() {
    if (this.database != null) {
      if (!this.database.isOpen())
        FileUtils.deleteRecursively(new File(this.database.getDatabasePath()));
      else {
        if (this.database.isTransactionActive())
          this.database.rollback();
        this.database.drop();
      }
    }
  }

  @Override
  public Variables variables() {
    throw Graph.Exceptions.variablesNotSupported();
  }

  @Override
  public Configuration configuration() {
    return configuration;
  }

  protected void deleteElement(final ArcadeElement element) {
    database.deleteRecord(element.getBaseElement().getRecord());
  }

  public Database getDatabase() {
    return database;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    ArcadeGraph that = (ArcadeGraph) o;
    return Objects.equals(database, that.database);
  }

  @Override
  public int hashCode() {
    return Objects.hash(database);
  }

  @Override
  public String toString() {
    return StringFactory.graphString(this, database.getName());
  }

  public static com.arcadedb.graph.Vertex.DIRECTION mapDirection(final Direction direction) {
    switch (direction) {
    case OUT:
      return com.arcadedb.graph.Vertex.DIRECTION.OUT;
    case IN:
      return com.arcadedb.graph.Vertex.DIRECTION.IN;
    case BOTH:
      return com.arcadedb.graph.Vertex.DIRECTION.BOTH;
    }
    throw new IllegalArgumentException(String.format("Cannot get direction for argument %s", direction));
  }
}
