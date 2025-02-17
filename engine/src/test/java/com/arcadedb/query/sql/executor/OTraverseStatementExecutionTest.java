package com.arcadedb.query.sql.executor;

import com.arcadedb.TestHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collection;

/**
 * @author Luigi Dell'Aquila (l.dellaquila-(at)-orientdb.com)
 */
public class OTraverseStatementExecutionTest extends TestHelper {
  @Test
  public void testPlainTraverse() {
    database.transaction((db) -> {
      String classPrefix = "testPlainTraverse_";
      database.getSchema().createVertexType(classPrefix + "V");
      database.getSchema().createEdgeType(classPrefix + "E");
      database.command("sql", "create vertex " + classPrefix + "V set name = 'a'").close();
      database.command("sql", "create vertex " + classPrefix + "V set name = 'b'").close();
      database.command("sql", "create vertex " + classPrefix + "V set name = 'c'").close();
      database.command("sql", "create vertex " + classPrefix + "V set name = 'd'").close();

      database.command("sql",
          "create edge " + classPrefix + "E from (select from " + classPrefix + "V where name = 'a') to (select from " + classPrefix + "V where name = 'b')")
          .close();
      database.command("sql",
          "create edge " + classPrefix + "E from (select from " + classPrefix + "V where name = 'b') to (select from " + classPrefix + "V where name = 'c')")
          .close();
      database.command("sql",
          "create edge " + classPrefix + "E from (select from " + classPrefix + "V where name = 'c') to (select from " + classPrefix + "V where name = 'd')")
          .close();

      ResultSet result = database.query("sql", "traverse out() from (select from " + classPrefix + "V where name = 'a')");

      for (int i = 0; i < 4; i++) {
        Assertions.assertTrue(result.hasNext());
        Result item = result.next();
        Assertions.assertEquals(i, item.getMetadata("$depth"));
      }
      Assertions.assertFalse(result.hasNext());
      result.close();
    });
  }

  @Test
  public void testWithDepth() {
    database.transaction((db) -> {
      String classPrefix = "testWithDepth_";
      database.getSchema().createVertexType(classPrefix + "V");
      database.getSchema().createEdgeType(classPrefix + "E");
      database.command("sql", "create vertex " + classPrefix + "V set name = 'a'").close();
      database.command("sql", "create vertex " + classPrefix + "V set name = 'b'").close();
      database.command("sql", "create vertex " + classPrefix + "V set name = 'c'").close();
      database.command("sql", "create vertex " + classPrefix + "V set name = 'd'").close();

      database.command("sql",
          "create edge " + classPrefix + "E from (select from " + classPrefix + "V where name = 'a') to (select from " + classPrefix + "V where name = 'b')")
          .close();
      database.command("sql",
          "create edge " + classPrefix + "E from (select from " + classPrefix + "V where name = 'b') to (select from " + classPrefix + "V where name = 'c')")
          .close();
      database.command("sql",
          "create edge " + classPrefix + "E from (select from " + classPrefix + "V where name = 'c') to (select from " + classPrefix + "V where name = 'd')")
          .close();

      ResultSet result = database.query("sql", "traverse out() from (select from " + classPrefix + "V where name = 'a') WHILE $depth < 2");

      for (int i = 0; i < 2; i++) {
        Assertions.assertTrue(result.hasNext());
        Result item = result.next();
        Assertions.assertEquals(i, item.getMetadata("$depth"));
      }
      Assertions.assertFalse(result.hasNext());
      result.close();
    });
  }

  @Test
  public void testMaxDepth() {
    database.transaction((db) -> {
      String classPrefix = "testMaxDepth";
      database.getSchema().createVertexType(classPrefix + "V");
      database.getSchema().createEdgeType(classPrefix + "E");
      database.command("sql", "create vertex " + classPrefix + "V set name = 'a'").close();
      database.command("sql", "create vertex " + classPrefix + "V set name = 'b'").close();
      database.command("sql", "create vertex " + classPrefix + "V set name = 'c'").close();
      database.command("sql", "create vertex " + classPrefix + "V set name = 'd'").close();

      database.command("sql",
          "create edge " + classPrefix + "E from (select from " + classPrefix + "V where name = 'a') to (select from " + classPrefix + "V where name = 'b')")
          .close();
      database.command("sql",
          "create edge " + classPrefix + "E from (select from " + classPrefix + "V where name = 'b') to (select from " + classPrefix + "V where name = 'c')")
          .close();
      database.command("sql",
          "create edge " + classPrefix + "E from (select from " + classPrefix + "V where name = 'c') to (select from " + classPrefix + "V where name = 'd')")
          .close();

      ResultSet result = database.query("sql", "traverse out() from (select from " + classPrefix + "V where name = 'a') MAXDEPTH 1");

      for (int i = 0; i < 2; i++) {
        Assertions.assertTrue(result.hasNext());
        Result item = result.next();
        Assertions.assertEquals(i, item.getMetadata("$depth"));
      }
      Assertions.assertFalse(result.hasNext());
      result.close();

      result = database.query("sql", "traverse out() from (select from " + classPrefix + "V where name = 'a') MAXDEPTH 2");

      for (int i = 0; i < 3; i++) {
        Assertions.assertTrue(result.hasNext());
        Result item = result.next();
        Assertions.assertEquals(i, item.getMetadata("$depth"));
      }
      Assertions.assertFalse(result.hasNext());
      result.close();
    });
  }

  @Test
  public void testBreadthFirst() {
    database.transaction((db) -> {
      String classPrefix = "testBreadthFirst_";
      database.getSchema().createVertexType(classPrefix + "V");
      database.getSchema().createEdgeType(classPrefix + "E");
      database.command("sql", "create vertex " + classPrefix + "V set name = 'a'").close();
      database.command("sql", "create vertex " + classPrefix + "V set name = 'b'").close();
      database.command("sql", "create vertex " + classPrefix + "V set name = 'c'").close();
      database.command("sql", "create vertex " + classPrefix + "V set name = 'd'").close();

      database.command("sql",
          "create edge " + classPrefix + "E from (select from " + classPrefix + "V where name = 'a') to (select from " + classPrefix + "V where name = 'b')")
          .close();
      database.command("sql",
          "create edge " + classPrefix + "E from (select from " + classPrefix + "V where name = 'b') to (select from " + classPrefix + "V where name = 'c')")
          .close();
      database.command("sql",
          "create edge " + classPrefix + "E from (select from " + classPrefix + "V where name = 'c') to (select from " + classPrefix + "V where name = 'd')")
          .close();

      ResultSet result = database.query("sql", "traverse out() from (select from " + classPrefix + "V where name = 'a') STRATEGY BREADTH_FIRST");

      for (int i = 0; i < 4; i++) {
        Assertions.assertTrue(result.hasNext());
        Result item = result.next();
        Assertions.assertEquals(i, item.getMetadata("$depth"));
      }
      Assertions.assertFalse(result.hasNext());
      result.close();
    });
  }

  @Test
  public void testTraverseInBatchTx() {
    database.transaction((db) -> {
      String script = "";
      script += "";

      script += "drop type testTraverseInBatchTx_V if exists unsafe;";
      script += "create vertex type testTraverseInBatchTx_V;";
      script += "create property testTraverseInBatchTx_V.name STRING;";
      script += "drop type testTraverseInBatchTx_E if exists unsafe;";
      script += "create edge type testTraverseInBatchTx_E;";

      script += "begin;";
      script += "insert into testTraverseInBatchTx_V(name) values ('a'), ('b'), ('c');";
      script += "create edge testTraverseInBatchTx_E from (select from testTraverseInBatchTx_V where name = 'a') to (select from testTraverseInBatchTx_V where name = 'b');";
      script += "create edge testTraverseInBatchTx_E from (select from testTraverseInBatchTx_V where name = 'b') to (select from testTraverseInBatchTx_V where name = 'c');";
      script += "let top = (select * from (traverse in('testTraverseInBatchTx_E') from (select from testTraverseInBatchTx_V where name='c')) where in('testTraverseInBatchTx_E').size() == 0);";
      script += "commit;";
      script += "return $top;";

      ResultSet result = database.execute("sql", script);
      Assertions.assertTrue(result.hasNext());
      Result item = result.next();
      Object val = item.getProperty("value");
      Assertions.assertTrue(val instanceof Collection);
      Assertions.assertEquals(1, ((Collection) val).size());
      result.close();
    });
  }
}
