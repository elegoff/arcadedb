package com.arcadedb.query.sql.functions.math;

import com.arcadedb.TestHelper;
import com.arcadedb.query.sql.executor.ResultSet;
import com.arcadedb.query.sql.function.math.SQLFunctionAbsoluteValue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Tests the absolute value function. The key is that the mathematical abs function is correctly
 * applied and that values retain their types.
 *
 * @author Michael MacFadden
 */
public class SQLFunctionAbsoluteValueTest {

  private SQLFunctionAbsoluteValue function;

  @BeforeEach
  public void setup() {
    function = new SQLFunctionAbsoluteValue();
  }

  @Test
  public void testEmpty() {
    Object result = function.getResult();
    Assertions.assertNull(result);
  }

  @Test
  public void testNull() {
    function.execute(null, null, null, new Object[] { null }, null);
    Object result = function.getResult();
    Assertions.assertNull(result);
  }

  @Test
  public void testPositiveInteger() {
    function.execute(null, null, null, new Object[] { 10 }, null);
    Object result = function.getResult();
    Assertions.assertTrue(result instanceof Integer);
    Assertions.assertEquals(result, 10);
  }

  @Test
  public void testNegativeInteger() {
    function.execute(null, null, null, new Object[] { -10 }, null);
    Object result = function.getResult();
    Assertions.assertTrue(result instanceof Integer);
    Assertions.assertEquals(result, 10);
  }

  @Test
  public void testPositiveLong() {
    function.execute(null, null, null, new Object[] { 10L }, null);
    Object result = function.getResult();
    Assertions.assertTrue(result instanceof Long);
    Assertions.assertEquals(result, 10L);
  }

  @Test
  public void testNegativeLong() {
    function.execute(null, null, null, new Object[] { -10L }, null);
    Object result = function.getResult();
    Assertions.assertTrue(result instanceof Long);
    Assertions.assertEquals(result, 10L);
  }

  @Test
  public void testPositiveShort() {
    function.execute(null, null, null, new Object[] { (short) 10 }, null);
    Object result = function.getResult();
    Assertions.assertTrue(result instanceof Short);
    Assertions.assertEquals(result, (short) 10);
  }

  @Test
  public void testNegativeShort() {
    function.execute(null, null, null, new Object[] { (short) -10 }, null);
    Object result = function.getResult();
    Assertions.assertTrue(result instanceof Short);
    Assertions.assertEquals(result, (short) 10);
  }

  @Test
  public void testPositiveDouble() {
    function.execute(null, null, null, new Object[] { 10.5D }, null);
    Object result = function.getResult();
    Assertions.assertTrue(result instanceof Double);
    Assertions.assertEquals(result, 10.5D);
  }

  @Test
  public void testNegativeDouble() {
    function.execute(null, null, null, new Object[] { -10.5D }, null);
    Object result = function.getResult();
    Assertions.assertTrue(result instanceof Double);
    Assertions.assertEquals(result, 10.5D);
  }

  @Test
  public void testPositiveFloat() {
    function.execute(null, null, null, new Object[] { 10.5F }, null);
    Object result = function.getResult();
    Assertions.assertTrue(result instanceof Float);
    Assertions.assertEquals(result, 10.5F);
  }

  @Test
  public void testNegativeFloat() {
    function.execute(null, null, null, new Object[] { -10.5F }, null);
    Object result = function.getResult();
    Assertions.assertTrue(result instanceof Float);
    Assertions.assertEquals(result, 10.5F);
  }

  @Test
  public void testPositiveBigDecimal() {
    function.execute(null, null, null, new Object[] { new BigDecimal(10.5D) }, null);
    Object result = function.getResult();
    Assertions.assertTrue(result instanceof BigDecimal);
    Assertions.assertEquals(result, new BigDecimal(10.5D));
  }

  @Test
  public void testNegativeBigDecimal() {
    function.execute(null, null, null, new Object[] { new BigDecimal(-10.5D) }, null);
    Object result = function.getResult();
    Assertions.assertTrue(result instanceof BigDecimal);
    Assertions.assertEquals(result, new BigDecimal(10.5D));
  }

  @Test
  public void testPositiveBigInteger() {
    function.execute(null, null, null, new Object[] { new BigInteger("10") }, null);
    Object result = function.getResult();
    Assertions.assertTrue(result instanceof BigInteger);
    Assertions.assertEquals(result, new BigInteger("10"));
  }

  @Test
  public void testNegativeBigInteger() {
    function.execute(null, null, null, new Object[] { new BigInteger("-10") }, null);
    Object result = function.getResult();
    Assertions.assertTrue(result instanceof BigInteger);
    Assertions.assertEquals(result, new BigInteger("10"));
  }

  @Test
  public void testNonNumber() {
    try {
      function.execute(null, null, null, new Object[] { "abc" }, null);
      Assertions.fail("Expected  IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // OK
    }
  }

  @Test
  public void testFromQuery() throws Exception {
    TestHelper.executeInNewDatabase("./target/databases/testAbsFunction", (db) -> {
      ResultSet result = db.query("sql", "select abs(-45.4) as abs");
      Assertions.assertEquals(45.4F, ((Number) result.next().getProperty("abs")).floatValue());
    });
  }
}
