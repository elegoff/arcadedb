package com.arcadedb.query.sql.parser;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.fail;

public class CreateVertexStatementTest {

  protected SimpleNode checkRightSyntax(String query) {
    return checkSyntax(query, true);
  }

  protected SimpleNode checkWrongSyntax(String query) {
    return checkSyntax(query, false);
  }

  protected SimpleNode checkSyntax(String query, boolean isCorrect) {
    SqlParser osql = getParserFor(query);
    try {
      SimpleNode result = osql.parse();
      if (!isCorrect) {
        fail();
      }
      return result;
    } catch (Exception e) {
      if (isCorrect) {
        e.printStackTrace();
        fail();
      }
    }
    return null;
  }

  @Test
  public void testSimpleCreate() {
    checkRightSyntax("create vertex");
    checkRightSyntax("create vertex V");
    checkRightSyntax("create vertex x bucket t");
    checkWrongSyntax("create vertex V foo");
    checkRightSyntax("create vertex Foo (a) values (1)");
    checkRightSyntax("create vertex Foo (a) values ('1')");
    checkRightSyntax("create vertex Foo (a) values (\"1\")");

    checkRightSyntax("create vertex Foo (a,b) values (1, 2)");
    checkRightSyntax("create vertex Foo (a,b) values ('1', '2')");
    checkRightSyntax("create vertex (a,b) values (\"1\", \"2\")");

    printTree("create vertex (a,b) values (\"1\", \"2\")");
  }

  @Test
  public void testSimpleCreateSet() {
    checkRightSyntax("create vertex Foo set a = 1");
    checkRightSyntax("create vertex Foo set a = '1'");
    checkRightSyntax("create vertex Foo set a = \"1\"");

    checkRightSyntax("create vertex Foo set a = 1, b = 2");
  }

  @Test
  public void testEmptyArrayCreate() {
    checkRightSyntax("create vertex Foo set a = 'foo'");
    checkRightSyntax("create vertex Foo set a = []");
    //    checkRightSyntax("create vertex Foo set a = [ ]");
  }

  @Test
  public void testEmptyMapCreate() {
    checkRightSyntax("create vertex Foo set a = {}");
    checkRightSyntax("create vertex Foo SET a = { }");
  }

  @Test
  public void testInsertIntoBucket() {
    checkRightSyntax(
        "create vertex bucket:default (equaledges, name, list) values ('yes', 'square', ['bottom', 'top','left','right'] )");
  }

  private void printTree(String s) {
    SqlParser osql = getParserFor(s);
    try {
      SimpleNode n = osql.parse();

    } catch (ParseException e) {
      e.printStackTrace();
    }
  }

  protected SqlParser getParserFor(String string) {
    InputStream is = new ByteArrayInputStream(string.getBytes());
    SqlParser osql = new SqlParser(is);
    return osql;
  }
}
