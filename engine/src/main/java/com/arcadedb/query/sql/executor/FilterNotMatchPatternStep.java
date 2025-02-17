package com.arcadedb.query.sql.executor;

import com.arcadedb.exception.TimeoutException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class FilterNotMatchPatternStep extends AbstractExecutionStep {

  private List<AbstractExecutionStep> subSteps;

  private ResultSet prevResult = null;

  private long cost;

  public FilterNotMatchPatternStep(List<AbstractExecutionStep> steps, CommandContext ctx, boolean enableProfiling) {
    super(ctx, enableProfiling);
    this.subSteps = steps;
  }

  @Override
  public ResultSet syncPull(CommandContext ctx, int nRecords) throws TimeoutException {
    if (!prev.isPresent()) {
      throw new IllegalStateException("filter step requires a previous step");
    }
    ExecutionStepInternal prevStep = prev.get();

    return new ResultSet() {
      public boolean finished = false;

      private Result nextItem = null;
      private int fetched = 0;

      private void fetchNextItem() {
        nextItem = null;
        if (finished) {
          return;
        }
        if (prevResult == null) {
          prevResult = prevStep.syncPull(ctx, nRecords);
          if (!prevResult.hasNext()) {
            finished = true;
            return;
          }
        }
        while (!finished) {
          while (!prevResult.hasNext()) {
            prevResult = prevStep.syncPull(ctx, nRecords);
            if (!prevResult.hasNext()) {
              finished = true;
              return;
            }
          }
          nextItem = prevResult.next();
          long begin = profilingEnabled ? System.nanoTime() : 0;
          try {
            if (!matchesPattern(nextItem, ctx)) {
              break;
            }

            nextItem = null;
          } finally {
            if (profilingEnabled) {
              cost += (System.nanoTime() - begin);
            }
          }
        }
      }

      @Override
      public boolean hasNext() {

        if (fetched >= nRecords || finished) {
          return false;
        }
        if (nextItem == null) {
          fetchNextItem();
        }

        if (nextItem != null) {
          return true;
        }

        return false;
      }

      @Override
      public Result next() {
        if (fetched >= nRecords || finished) {
          throw new IllegalStateException();
        }
        if (nextItem == null) {
          fetchNextItem();
        }
        if (nextItem == null) {
          throw new IllegalStateException();
        }
        Result result = nextItem;
        nextItem = null;
        fetched++;
        return result;
      }

      @Override
      public void close() {
        FilterNotMatchPatternStep.this.close();
      }

      @Override
      public Optional<ExecutionPlan> getExecutionPlan() {
        return Optional.empty();
      }

      @Override
      public Map<String, Long> getQueryStats() {
        return null;
      }
    };
  }

  private boolean matchesPattern(Result nextItem, CommandContext ctx) {
    SelectExecutionPlan plan = createExecutionPlan(nextItem, ctx);
    try (ResultSet rs = plan.fetchNext(1)) {
      return rs.hasNext();
    }
  }

  private SelectExecutionPlan createExecutionPlan(Result nextItem, CommandContext ctx) {
    SelectExecutionPlan plan = new SelectExecutionPlan(ctx);
    plan.chain(new AbstractExecutionStep(ctx, profilingEnabled) {
      private boolean executed = false;

      @Override
      public ResultSet syncPull(CommandContext ctx, int nRecords) throws TimeoutException {
        InternalResultSet result = new InternalResultSet();
        if (!executed) {
          result.add(copy(nextItem));
          executed = true;
        }
        return result;
      }

      private Result copy(Result nextItem) {
        ResultInternal result = new ResultInternal();
        for (String prop : nextItem.getPropertyNames()) {
          result.setProperty(prop, nextItem.getProperty(prop));
        }
        for (String md : nextItem.getMetadataKeys()) {
          result.setMetadata(md, nextItem.getMetadata(md));
        }
        return result;
      }
    });
    subSteps.stream().forEach(step -> plan.chain(step));
    return plan;
  }

  @Override
  public List<ExecutionStep> getSubSteps() {
    return (List) subSteps;
  }

  @Override
  public String prettyPrint(int depth, int indent) {
    String spaces = ExecutionStepInternal.getIndent(depth, indent);
    StringBuilder result = new StringBuilder();
    result.append(spaces);
    result.append("+ NOT (\n");
    this.subSteps.forEach(x -> result.append(x.prettyPrint(depth + 1, indent)).append("\n"));
    result.append(spaces);
    result.append("  )");
    return result.toString();
  }

  @Override
  public void close() {
    super.close();
  }
}
