package com.arcadedb.query.sql.executor;

/**
 * Created by frank on 21/10/2016.
 */
public class ExecutionPlanPrintUtils {

  public static void printExecutionPlan(ResultSet result) {
    printExecutionPlan(null, result);
  }

  public static void printExecutionPlan(String query, ResultSet result) {
    if (query != null) {
      System.out.println(query);
    }
    result.getExecutionPlan().ifPresent(x -> System.out.println(x.prettyPrint(0, 3)));
    System.out.println();
  }
}
