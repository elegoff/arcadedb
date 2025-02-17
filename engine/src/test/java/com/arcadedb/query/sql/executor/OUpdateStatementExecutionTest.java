package com.arcadedb.query.sql.executor;

import com.arcadedb.TestHelper;
import com.arcadedb.database.MutableDocument;
import com.arcadedb.schema.DocumentType;
import com.arcadedb.schema.Type;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Luigi Dell'Aquila (l.dellaquila-(at)-orientdb.com)
 */
public class OUpdateStatementExecutionTest extends TestHelper {
  private String className;

  public OUpdateStatementExecutionTest() {
    autoStartTx = true;
  }

  @Override
  public void beginTest() {
    DocumentType clazz = database.getSchema().createDocumentType("OUpdateStatementExecutionTest");
    className = clazz.getName();

    for (int i = 0; i < 10; i++) {
      MutableDocument doc = database.newDocument(className);
      doc.set("name", "name" + i);
      doc.set("surname", "surname" + i);
      doc.set("number", 4L);

      List<String> tagsList = new ArrayList<>();
      tagsList.add("foo");
      tagsList.add("bar");
      tagsList.add("baz");
      doc.set("tagsList", tagsList);

      Map<String, String> tagsMap = new HashMap<>();
      tagsMap.put("foo", "foo");
      tagsMap.put("bar", "bar");
      tagsMap.put("baz", "baz");
      doc.set("tagsMap", tagsMap);

      doc.save();
    }
  }

  @Test
  public void testSetString() {
    ResultSet result = database.command("sql", "update " + className + " set surname = 'foo'");
    printExecutionPlan(result);
    Assertions.assertTrue(result.hasNext());
    Result item = result.next();
    Assertions.assertNotNull(item);
    Assertions.assertEquals((Object) 10L, item.getProperty("count"));

    Assertions.assertFalse(result.hasNext());
    result.close();

    result = database.query("sql", "SElect from " + className);
    for (int i = 0; i < 10; i++) {
      Assertions.assertTrue(result.hasNext());
      item = result.next();
      Assertions.assertNotNull(item);
      Assertions.assertEquals("foo", item.getProperty("surname"));
    }
    Assertions.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testCopyField() {
    ResultSet result = database.command("sql", "update " + className + " set surname = name");

    Assertions.assertTrue(result.hasNext());
    Result item = result.next();
    Assertions.assertNotNull(item);
    Assertions.assertEquals((Object) 10L, item.getProperty("count"));
    Assertions.assertFalse(result.hasNext());
    result.close();

    result = database.query("sql", "SElect from " + className);
    for (int i = 0; i < 10; i++) {
      Assertions.assertTrue(result.hasNext());
      item = result.next();
      Assertions.assertNotNull(item);
      Assertions.assertEquals((Object) item.getProperty("name"), item.getProperty("surname"));
    }
    Assertions.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testSetExpression() {
    ResultSet result = database.command("sql", "update " + className + " set surname = 'foo'+name ");
    Assertions.assertTrue(result.hasNext());
    Result item = result.next();
    Assertions.assertNotNull(item);
    Assertions.assertEquals((Object) 10L, item.getProperty("count"));
    Assertions.assertFalse(result.hasNext());
    result.close();

    result = database.query("sql", "SElect from " + className);
    for (int i = 0; i < 10; i++) {
      Assertions.assertTrue(result.hasNext());
      item = result.next();
      Assertions.assertNotNull(item);
      Assertions.assertEquals("foo" + item.getProperty("name"), item.getProperty("surname"));
    }
    Assertions.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testConditionalSet() {
    ResultSet result = database.command("sql", "update " + className + " set surname = 'foo' where name = 'name3'");
    Assertions.assertTrue(result.hasNext());
    Result item = result.next();
    Assertions.assertNotNull(item);
    Assertions.assertEquals((Object) 1L, item.getProperty("count"));
    Assertions.assertFalse(result.hasNext());
    result.close();

    result = database.query("sql", "SElect from " + className);
    boolean found = false;
    for (int i = 0; i < 10; i++) {
      Assertions.assertTrue(result.hasNext());
      item = result.next();
      Assertions.assertNotNull(item);
      if ("name3".equals(item.getProperty("name"))) {
        Assertions.assertEquals("foo", item.getProperty("surname"));
        found = true;
      }
    }
    Assertions.assertTrue(found);
    Assertions.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testSetOnList() {
    ResultSet result = database.command("sql", "update " + className + " set tagsList[0] = 'abc' where name = 'name3'");
    printExecutionPlan(result);
    Assertions.assertTrue(result.hasNext());
    Result item = result.next();
    Assertions.assertNotNull(item);
    Assertions.assertEquals((Object) 1L, item.getProperty("count"));
    Assertions.assertFalse(result.hasNext());
    result.close();

    result = database.query("sql", "SElect from " + className);
    boolean found = false;
    for (int i = 0; i < 10; i++) {
      Assertions.assertTrue(result.hasNext());
      item = result.next();
      Assertions.assertNotNull(item);
      if ("name3".equals(item.getProperty("name"))) {
        List<String> tags = new ArrayList<>();
        tags.add("abc");
        tags.add("bar");
        tags.add("baz");
        Assertions.assertEquals(tags, item.getProperty("tagsList"));
        found = true;
      }
    }
    Assertions.assertTrue(found);
    Assertions.assertFalse(result.hasNext());
    result.close();
  }

  private void printExecutionPlan(ResultSet result) {
    //ExecutionPlanPrintUtils.printExecutionPlan(result);
  }

  @Test
  public void testSetOnList2() {
    ResultSet result = database.command("sql", "update " + className + " set tagsList[6] = 'abc' where name = 'name3'");
    printExecutionPlan(result);
    Assertions.assertTrue(result.hasNext());
    Result item = result.next();
    Assertions.assertNotNull(item);
    Assertions.assertEquals((Object) 1L, item.getProperty("count"));
    Assertions.assertFalse(result.hasNext());
    result.close();

    result = database.query("sql", "SElect from " + className);
    boolean found = false;
    for (int i = 0; i < 10; i++) {
      Assertions.assertTrue(result.hasNext());
      item = result.next();
      Assertions.assertNotNull(item);
      if ("name3".equals(item.getProperty("name"))) {
        List<String> tags = new ArrayList<>();
        tags.add("foo");
        tags.add("bar");
        tags.add("baz");
        tags.add(null);
        tags.add(null);
        tags.add(null);
        tags.add("abc");
        Assertions.assertEquals(tags, item.getProperty("tagsList"));
        found = true;
      }
    }
    Assertions.assertTrue(found);
    Assertions.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testSetOnMap() {
    ResultSet result = database.command("sql", "update " + className + " set tagsMap['foo'] = 'abc' where name = 'name3'");
    Assertions.assertTrue(result.hasNext());
    Result item = result.next();
    Assertions.assertNotNull(item);
    Assertions.assertEquals((Object) 1L, item.getProperty("count"));
    Assertions.assertFalse(result.hasNext());
    result.close();

    result = database.query("sql", "SElect from " + className);
    boolean found = false;
    for (int i = 0; i < 10; i++) {
      Assertions.assertTrue(result.hasNext());
      item = result.next();
      Assertions.assertNotNull(item);
      if ("name3".equals(item.getProperty("name"))) {
        Map<String, String> tags = new HashMap<>();
        tags.put("foo", "abc");
        tags.put("bar", "bar");
        tags.put("baz", "baz");
        Assertions.assertEquals(tags, item.getProperty("tagsMap"));
        found = true;
      } else {
        Map<String, String> tags = new HashMap<>();
        tags.put("foo", "foo");
        tags.put("bar", "bar");
        tags.put("baz", "baz");
        Assertions.assertEquals(tags, item.getProperty("tagsMap"));
      }
    }
    Assertions.assertTrue(found);
    Assertions.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testPlusAssign() {
    ResultSet result = database.command("sql", "update " + className + " set name += 'foo', newField += 'bar', number += 5");
    Assertions.assertTrue(result.hasNext());
    Result item = result.next();
    Assertions.assertNotNull(item);
    Assertions.assertEquals((Object) 10L, item.getProperty("count"));
    Assertions.assertFalse(result.hasNext());
    result.close();

    result = database.query("sql", "SElect from " + className);
    for (int i = 0; i < 10; i++) {
      Assertions.assertTrue(result.hasNext());
      item = result.next();
      Assertions.assertNotNull(item);
      Assertions.assertTrue(item.getProperty("name").toString().endsWith("foo")); // test concatenate string to string
      Assertions.assertEquals(8, item.getProperty("name").toString().length());
      Assertions.assertEquals("bar", item.getProperty("newField")); // test concatenate null to string
      Assertions.assertEquals((Object) 9L, item.getProperty("number")); // test sum numbers
    }
    Assertions.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testMinusAssign() {
    ResultSet result = database.command("sql", "update " + className + " set number -= 5");
    printExecutionPlan(result);
    Assertions.assertTrue(result.hasNext());
    Result item = result.next();
    Assertions.assertNotNull(item);
    Assertions.assertEquals((Object) 10L, item.getProperty("count"));
    Assertions.assertFalse(result.hasNext());
    result.close();

    result = database.query("sql", "SElect from " + className);
    for (int i = 0; i < 10; i++) {
      Assertions.assertTrue(result.hasNext());
      item = result.next();
      Assertions.assertNotNull(item);
      Assertions.assertEquals((Object) (-1L), item.getProperty("number"));
    }
    Assertions.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testStarAssign() {
    ResultSet result = database.command("sql", "update " + className + " set number *= 5");
    Assertions.assertTrue(result.hasNext());
    Result item = result.next();
    Assertions.assertNotNull(item);
    Assertions.assertEquals((Object) 10L, item.getProperty("count"));
    Assertions.assertFalse(result.hasNext());
    result.close();

    result = database.query("sql", "SElect from " + className);
    for (int i = 0; i < 10; i++) {
      Assertions.assertTrue(result.hasNext());
      item = result.next();
      Assertions.assertNotNull(item);
      Assertions.assertEquals((Object) 20L, item.getProperty("number"));
    }
    Assertions.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testSlashAssign() {
    ResultSet result = database.command("sql", "update " + className + " set number /= 2");
    Assertions.assertTrue(result.hasNext());
    Result item = result.next();
    Assertions.assertNotNull(item);
    Assertions.assertEquals((Object) 10L, item.getProperty("count"));
    Assertions.assertFalse(result.hasNext());
    result.close();

    result = database.query("sql", "SElect from " + className);
    for (int i = 0; i < 10; i++) {
      Assertions.assertTrue(result.hasNext());
      item = result.next();
      Assertions.assertNotNull(item);
      Assertions.assertEquals((Object) 2L, item.getProperty("number"));
    }
    Assertions.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testRemove() {
    ResultSet result = database.query("sql", "SElect from " + className);
    for (int i = 0; i < 10; i++) {
      Assertions.assertTrue(result.hasNext());
      Result item = result.next();
      Assertions.assertNotNull(item);
      Assertions.assertNotNull(item.getProperty("surname"));
    }

    result.close();
    result = database.command("sql", "update " + className + " remove surname");
    for (int i = 0; i < 1; i++) {
      Assertions.assertTrue(result.hasNext());
      Result item = result.next();
      Assertions.assertNotNull(item);
      Assertions.assertEquals((Object) 10L, item.getProperty("count"));
    }
    Assertions.assertFalse(result.hasNext());
    result.close();

    result = database.query("sql", "SElect from " + className);
    for (int i = 0; i < 10; i++) {
      Assertions.assertTrue(result.hasNext());
      Result item = result.next();
      Assertions.assertNotNull(item);
      Assertions.assertNull(item.getProperty("surname"));
    }
    Assertions.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testContent() {

    ResultSet result = database.command("sql", "update " + className + " content {'name': 'foo', 'secondName': 'bar'}");
    Assertions.assertTrue(result.hasNext());
    Result item = result.next();
    Assertions.assertNotNull(item);
    Assertions.assertEquals((Object) 10L, item.getProperty("count"));
    Assertions.assertFalse(result.hasNext());
    result.close();

    result = database.query("sql", "SElect from " + className);
    for (int i = 0; i < 10; i++) {
      Assertions.assertTrue(result.hasNext());
      item = result.next();
      Assertions.assertNotNull(item);
      Assertions.assertEquals("foo", item.getProperty("name"));
      Assertions.assertEquals("bar", item.getProperty("secondName"));
      Assertions.assertNull(item.getProperty("surname"));
    }
    Assertions.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testMerge() {

    ResultSet result = database.command("sql", "update " + className + " merge {'name': 'foo', 'secondName': 'bar'}");
    Assertions.assertTrue(result.hasNext());
    Result item = result.next();
    Assertions.assertNotNull(item);
    Assertions.assertEquals((Object) 10L, item.getProperty("count"));
    Assertions.assertFalse(result.hasNext());
    result.close();

    result = database.query("sql", "SElect from " + className);
    for (int i = 0; i < 10; i++) {
      Assertions.assertTrue(result.hasNext());
      item = result.next();
      Assertions.assertNotNull(item);
      Assertions.assertEquals("foo", item.getProperty("name"));
      Assertions.assertEquals("bar", item.getProperty("secondName"));
      Assertions.assertTrue(item.getProperty("surname").toString().startsWith("surname"));
    }
    Assertions.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testUpsert1() {

    ResultSet result = database.command("sql", "update " + className + " set foo = 'bar' upsert where name = 'name1'");
    Assertions.assertTrue(result.hasNext());
    Result item = result.next();
    Assertions.assertNotNull(item);
    Assertions.assertEquals((Object) 1L, item.getProperty("count"));
    Assertions.assertFalse(result.hasNext());
    result.close();

    result = database.query("sql", "SElect from " + className);
    for (int i = 0; i < 10; i++) {
      Assertions.assertTrue(result.hasNext());
      item = result.next();
      Assertions.assertNotNull(item);
      String name = item.getProperty("name");
      Assertions.assertNotNull(name);
      if ("name1".equals(name)) {
        Assertions.assertEquals("bar", item.getProperty("foo"));
      } else {
        Assertions.assertNull(item.getProperty("foo"));
      }
    }
    Assertions.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testUpsertAndReturn() {

    ResultSet result = database.command("sql", "update " + className + " set foo = 'bar' upsert  return after  where name = 'name1' ");

    Assertions.assertTrue(result.hasNext());
    Result item = result.next();
    Assertions.assertNotNull(item);
    Assertions.assertEquals("bar", item.getProperty("foo"));
    Assertions.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testUpsert2() {

    ResultSet result = database.command("sql", "update " + className + " set foo = 'bar' upsert where name = 'name11'");
    Assertions.assertTrue(result.hasNext());
    Result item = result.next();
    Assertions.assertNotNull(item);
    Assertions.assertEquals((Object) 1L, item.getProperty("count"));
    Assertions.assertFalse(result.hasNext());
    result.close();

    result = database.query("sql", "SElect from " + className);
    for (int i = 0; i < 11; i++) {
      Assertions.assertTrue(result.hasNext());
      item = result.next();
      Assertions.assertNotNull(item);
      String name = item.getProperty("name");
      Assertions.assertNotNull(name);
      if ("name11".equals(name)) {
        Assertions.assertEquals("bar", item.getProperty("foo"));
      } else {
        Assertions.assertNull(item.getProperty("foo"));
      }
    }
    Assertions.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testRemove1() {
    String className = "overridden" + this.className;

    DocumentType clazz = database.getSchema().createDocumentType(className);
    clazz.createProperty("theProperty", Type.LIST);

    MutableDocument doc = database.newDocument(className);
    List theList = new ArrayList();
    for (int i = 0; i < 10; i++) {
      theList.add("n" + i);
    }
    doc.set("theProperty", theList);

    doc.save();

    ResultSet result = database.command("sql", "update " + className + " remove theProperty[0]");
    printExecutionPlan(result);
    Assertions.assertTrue(result.hasNext());
    Result item = result.next();
    Assertions.assertNotNull(item);
    Assertions.assertFalse(result.hasNext());
    result.close();

    result = database.query("sql", "SElect from " + className);
    Assertions.assertTrue(result.hasNext());
    item = result.next();
    Assertions.assertNotNull(item);
    List ls = item.getProperty("theProperty");
    Assertions.assertNotNull(ls);
    Assertions.assertEquals(9, ls.size());
    Assertions.assertFalse(ls.contains("n0"));
    Assertions.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testRemove2() {
    String className = "overridden" + this.className;
    DocumentType clazz = database.getSchema().createDocumentType(className);
    clazz.createProperty("theProperty", Type.LIST);

    MutableDocument doc = database.newDocument(className);
    List theList = new ArrayList();
    for (int i = 0; i < 10; i++) {
      theList.add("n" + i);
    }
    doc.set("theProperty", theList);

    doc.save();

    ResultSet result = database.command("sql", "update " + className + " remove theProperty[0, 1, 3]");
    printExecutionPlan(result);
    Assertions.assertTrue(result.hasNext());
    Result item = result.next();
    Assertions.assertNotNull(item);
    Assertions.assertFalse(result.hasNext());
    result.close();

    result = database.query("sql", "SElect from " + className);
    Assertions.assertTrue(result.hasNext());
    item = result.next();
    Assertions.assertNotNull(item);
    List ls = item.getProperty("theProperty");

    Assertions.assertNotNull(ls);
    Assertions.assertEquals(ls.size(), 7);
    Assertions.assertFalse(ls.contains("n0"));
    Assertions.assertFalse(ls.contains("n1"));
    Assertions.assertTrue(ls.contains("n2"));
    Assertions.assertFalse(ls.contains("n3"));
    Assertions.assertTrue(ls.contains("n4"));

    Assertions.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testRemove3() {
    String className = "overriden" + this.className;
    DocumentType clazz = database.getSchema().createDocumentType(className);
    clazz.createProperty("theProperty", Type.EMBEDDED);

    MutableDocument doc = database.newDocument(className);
    MutableDocument emb = database.newDocument(className);
    emb.set("sub", "foo");
    emb.set("aaa", "bar");
    doc.set("theProperty", emb);

    doc.save();

    ResultSet result = database.command("sql", "update " + className + " remove theProperty.sub");
    printExecutionPlan(result);
    Assertions.assertTrue(result.hasNext());
    Result item = result.next();
    Assertions.assertNotNull(item);
    Assertions.assertFalse(result.hasNext());
    result.close();

    result = database.query("sql", "SElect from " + className);
    Assertions.assertTrue(result.hasNext());
    item = result.next();
    Assertions.assertNotNull(item);
    ResultInternal ls = item.getProperty("theProperty");
    Assertions.assertNotNull(ls);
    Assertions.assertFalse(ls.getPropertyNames().contains("sub"));
    Assertions.assertEquals("bar", ls.getProperty("aaa"));
    Assertions.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testRemoveFromMapSquare() {

    database.command("sql", "UPDATE " + className + " REMOVE tagsMap[\"bar\"]").close();

    ResultSet result = database.query("sql", "SELECT tagsMap FROM " + className);
    printExecutionPlan(result);
    for (int i = 0; i < 10; i++) {
      Assertions.assertTrue(result.hasNext());
      Result item = result.next();
      Assertions.assertNotNull(item);
      Assertions.assertEquals(2, ((Map) item.getProperty("tagsMap")).size());
      Assertions.assertFalse(((Map) item.getProperty("tagsMap")).containsKey("bar"));
    }
    Assertions.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testRemoveFromMapEquals() {

    database.command("sql", "UPDATE " + className + " REMOVE tagsMap = \"bar\"").close();

    ResultSet result = database.query("sql", "SELECT tagsMap FROM " + className);
    printExecutionPlan(result);
    for (int i = 0; i < 10; i++) {
      Assertions.assertTrue(result.hasNext());
      Result item = result.next();
      Assertions.assertNotNull(item);
      Assertions.assertEquals(2, ((Map) item.getProperty("tagsMap")).size());
      Assertions.assertFalse(((Map) item.getProperty("tagsMap")).containsKey("bar"));
    }
    Assertions.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testReturnBefore() {
    ResultSet result = database.command("sql", "update " + className + " set name = 'foo' RETURN BEFORE where name = 'name1'");
    printExecutionPlan(result);
    Assertions.assertTrue(result.hasNext());
    Result item = result.next();
    Assertions.assertNotNull(item);
    Assertions.assertEquals("name1", item.getProperty("name"));

    Assertions.assertFalse(result.hasNext());
    result.close();
  }
}
