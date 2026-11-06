package personal.worldgen;

import java.util.List;

/** Runs a validated plan through an executor and reports per-step failures. */
public final class PlanRunner {
    public interface Executor { void apply(PlanStep step, int worldX, int worldY); }

    public record Result(GenerationReport report, List<PlanValidator.Issue> issues) {
        public Result {
            if (report == null || issues == null) throw new IllegalArgumentException("Missing result");
            issues = List.copyOf(issues);
        }
    }

    private PlanRunner() {}

    public static Result run(SitePlan plan, ProtectedCells guard, int padding, Executor executor) {
        if (plan == null || guard == null || executor == null)
            throw new IllegalArgumentException("Missing plan, guard or executor");
        var issues = PlanValidator.validate(plan, guard, padding);
        if (!issues.isEmpty()) return new Result(new GenerationReport(plan.operations(), 0, 0, plan.operations()), issues);
        int completed = 0, failed = 0;
        for (var step : plan.steps()) {
            try {
                executor.apply(step, plan.site().x() + step.dx(), plan.site().y() + step.dy());
                completed++;
            } catch (RuntimeException failure) {
                failed++;
            }
        }
        return new Result(new GenerationReport(plan.operations(), completed, failed, 0), issues);
    }
}
