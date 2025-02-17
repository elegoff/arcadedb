package com.arcadedb.query.sql.executor;

import com.arcadedb.TestHelper;
import com.arcadedb.exception.CommandExecutionException;
import com.arcadedb.schema.Schema;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Luigi Dell'Aquila (l.dellaquila-(at)-orientdb.com)
 */
public class OCreateVertexStatementExecutionTest extends TestHelper {
  public OCreateVertexStatementExecutionTest(){
    autoStartTx = true;
  }

  @Test
  public void testInsertSet() {
    String className = "testInsertSet";
    Schema schema = database.getSchema();
    schema.createVertexType(className);

    ResultSet result = database.command("sql", "create vertex " + className + " set name = 'name1'");
    printExecutionPlan(result);
    for (int i = 0; i < 1; i++) {
      Assertions.assertTrue(result.hasNext());
      Result item = result.next();
      Assertions.assertNotNull(item);
      Assertions.assertEquals("name1", item.getProperty("name"));
    }
    Assertions.assertFalse(result.hasNext());

    result = database.query("sql", "select from " + className);
    for (int i = 0; i < 1; i++) {
      Assertions.assertTrue(result.hasNext());
      Result item = result.next();
      Assertions.assertNotNull(item);
      Assertions.assertEquals("name1", item.getProperty("name"));
    }
    Assertions.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testInsertSetNoVertex() {
    String className = "testInsertSetNoVertex";
    Schema schema = database.getSchema();
    schema.createDocumentType(className);

    try {
      ResultSet result = database.command("sql", "create vertex " + className + " set name = 'name1'");
      Assertions.fail();
    } catch (CommandExecutionException e1) {
    } catch (Exception e2) {
      Assertions.fail();
    }
  }

  @Test
  public void testInsertValue() {
    String className = "testInsertValue";
    Schema schema = database.getSchema();
    schema.createVertexType(className);

    ResultSet result = database.command("sql", "create vertex " + className + "  (name, surname) values ('name1', 'surname1')");
    printExecutionPlan(result);
    for (int i = 0; i < 1; i++) {
      Assertions.assertTrue(result.hasNext());
      Result item = result.next();
      Assertions.assertNotNull(item);
      Assertions.assertEquals("name1", item.getProperty("name"));
      Assertions.assertEquals("surname1", item.getProperty("surname"));
    }
    Assertions.assertFalse(result.hasNext());

    result = database.query("sql", "select from " + className);
    for (int i = 0; i < 1; i++) {
      Assertions.assertTrue(result.hasNext());
      Result item = result.next();
      Assertions.assertNotNull(item);
      Assertions.assertEquals("name1", item.getProperty("name"));
    }
    Assertions.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testInsertValue2() {
    String className = "testInsertValue2";
    Schema schema = database.getSchema();
    schema.createVertexType(className);

    ResultSet result = database.command("sql", "create vertex " + className + "  (name, surname) values ('name1', 'surname1'), ('name2', 'surname2')");
    printExecutionPlan(result);

    for (int i = 0; i < 2; i++) {
      Assertions.assertTrue(result.hasNext());
      Result item = result.next();
      Assertions.assertNotNull(item);
      Assertions.assertEquals("name" + (i + 1), item.getProperty("name"));
      Assertions.assertEquals("surname" + (i + 1), item.getProperty("surname"));
    }
    Assertions.assertFalse(result.hasNext());

    Set<String> names = new HashSet<>();
    names.add("name1");
    names.add("name2");
    result = database.query("sql", "select from " + className);
    for (int i = 0; i < 2; i++) {
      Assertions.assertTrue(result.hasNext());
      Result item = result.next();
      Assertions.assertNotNull(item);
      Assertions.assertNotNull(item.getProperty("name"));
      names.remove(item.getProperty("name"));
      Assertions.assertNotNull(item.getProperty("surname"));
    }
    Assertions.assertFalse(result.hasNext());
    Assertions.assertTrue(names.isEmpty());
    result.close();
  }

  private void printExecutionPlan(ResultSet result) {
  }

  @Test
  public void testContent() {
    String className = "testContent";
    Schema schema = database.getSchema();
    schema.createVertexType(className);

    ResultSet result = database.command("sql", "create vertex " + className + " content {'name':'name1', 'surname':'surname1'}");
    printExecutionPlan(result);
    for (int i = 0; i < 1; i++) {
      Assertions.assertTrue(result.hasNext());
      Result item = result.next();
      Assertions.assertNotNull(item);
      Assertions.assertEquals("name1", item.getProperty("name"));
    }
    Assertions.assertFalse(result.hasNext());

    result = database.query("sql", "select from " + className);
    for (int i = 0; i < 1; i++) {
      Assertions.assertTrue(result.hasNext());
      Result item = result.next();
      Assertions.assertNotNull(item);
      Assertions.assertEquals("name1", item.getProperty("name"));
      Assertions.assertEquals("surname1", item.getProperty("surname"));
    }
    Assertions.assertFalse(result.hasNext());
    result.close();
  }
}
