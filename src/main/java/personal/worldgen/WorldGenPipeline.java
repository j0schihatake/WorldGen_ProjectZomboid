package personal.worldgen;

import java.util.function.Function;

/** One site pipeline: find a legal site, build its plan, validate and execute it. */
public final class WorldGenPipeline {
    public record Outcome(Site site, SitePlan plan, PlanRunner.Result result) {
        public Outcome {
            if (site == null || plan == null || result == null) throw new IllegalArgumentException("Missing pipeline outcome");
        }
    }

    private WorldGenPipeline() {}

    public static Outcome run(ProtectedCells guard, SitePlanner planner, int attempts, int padding,
                              Function<Site, SitePlan> planFactory, PlanRunner.Executor executor) {
        if (guard == null || planner == null || planFactory == null || executor == null)
            throw new IllegalArgumentException("Missing pipeline dependency");
        if (attempts < 0 || padding < 0) throw new IllegalArgumentException("Invalid pipeline limits");
        var site = planner.find(attempts).orElseThrow(() -> new IllegalStateException("No legal site"));
        var plan = planFactory.apply(site);
        if (plan == null) throw new IllegalStateException("No plan for site");
        return new Outcome(site, plan, PlanRunner.run(plan, guard, padding, executor));
    }
}
