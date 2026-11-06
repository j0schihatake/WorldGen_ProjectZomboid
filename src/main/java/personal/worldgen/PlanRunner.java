package personal.worldgen;

import java.util.List;

/** Runs a validated plan through an executor and reports per-step failures. */
public final class PlanRunner {
    public interface Executor { void apply(PlanStep step, int worldX, int worldY); }

    public record StepFailure(int index, String kind, String message) {
        public StepFailure {
            if (index < 0 || kind == null || message == null) throw new IllegalArgumentException("Invalid failure");
        }
    }

    public record Result(GenerationReport report, List<PlanValidator.Issue> issues, List<StepFailure> failures) {
        public Result {
            if (report == null || issues == null || failures == null)
                throw new IllegalArgumentException("Missing result");
            issues = List.copyOf(issues);
            failures = List.copyOf(failures);
        }
    }

    private PlanRunner() {}

    public static Result run(SitePlan plan, ProtectedCells guard, int padding, Executor executor) {
        if (plan == null || guard == null || executor == null)
            throw new IllegalArgumentException("Missing plan, guard or executor");
        var issues = PlanValidator.validate(plan, guard, padding);
        if (!issues.isEmpty()) return new Result(new GenerationReport(plan.operations(), 0, 0, plan.operations()), issues, List.of());
        int completed = 0;
        var failures = new java.util.ArrayList<StepFailure>();
        for (int i = 0; i < plan.steps().size(); i++) {
            var step = plan.steps().get(i);
            try {
                executor.apply(step, plan.site().x() + step.dx(), plan.site().y() + step.dy());
                completed++;
            } catch (RuntimeException failure) {
                failures.add(new StepFailure(i, step.kind(), String.valueOf(failure.getMessage())));
            }
        }
        return new Result(new GenerationReport(plan.operations(), completed, failures.size(), 0), issues, failures);
    }
}
