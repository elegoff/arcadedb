package com.arcadedb.query.sql.functions.graph;

import com.arcadedb.TestHelper;
import com.arcadedb.database.Database;
import com.arcadedb.database.DatabaseFactory;
import com.arcadedb.graph.MutableEdge;
import com.arcadedb.graph.MutableVertex;
import com.arcadedb.graph.Vertex;
import com.arcadedb.query.sql.executor.BasicCommandContext;
import com.arcadedb.query.sql.function.graph.SQLFunctionDijkstra;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class SQLFunctionDijkstraTest {

  private DatabaseFactory orientDB;
  private Database        graph;

  private MutableVertex       v1;
  private MutableVertex       v2;
  private MutableVertex       v3;
  private MutableVertex       v4;
  private SQLFunctionDijkstra functionDijkstra;

  public void setUp(Database graph) throws Exception {
    graph.transaction((db -> {
      graph.getSchema().createVertexType("node");
      graph.getSchema().createEdgeType("weight");

      v1 = graph.newVertex("node");
      v2 = graph.newVertex("node");
      v3 = graph.newVertex("node");
      v4 = graph.newVertex("node");

      v1.set("node_id", "A").save();
      v2.set("node_id", "B").save();
      v3.set("node_id", "C").save();
      v4.set("node_id", "D").save();

      MutableEdge e1 = v1.newEdge("weight", v2, true);
      e1.set("weight", 1.0f);
      e1.save();

      MutableEdge e2 = v2.newEdge("weight", v3, true);
      e2.set("weight", 1.0f);
      e2.save();

      MutableEdge e3 = v1.newEdge("weight", v3, true);
      e3.set("weight", 100.0f);
      e3.save();

      MutableEdge e4 = v3.newEdge("weight", v4, true);
      e4.set("weight", 1.0f);
      e4.save();

      functionDijkstra = new SQLFunctionDijkstra();
    }));
  }

  @Test
  public void testExecute() throws Exception {
    TestHelper.executeInNewDatabase("SQLFunctionDijkstraTest", (graph) -> {
      setUp(graph);
      final List<Vertex> result = functionDijkstra.execute(null, null, null, new Object[] { v1, v4, "'weight'" }, new BasicCommandContext());

      Assertions.assertEquals(4, result.size());
      Assertions.assertEquals(v1, result.get(0));
      Assertions.assertEquals(v2, result.get(1));
      Assertions.assertEquals(v3, result.get(2));
      Assertions.assertEquals(v4, result.get(3));
    });
  }
}
