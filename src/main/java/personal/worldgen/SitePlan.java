package personal.worldgen;

import java.util.List;

/** Validated plan for one site, committed only after the whole plan is built. */
public record SitePlan(Site site, String version, List<PlanStep> steps) {
    public SitePlan {
        if (site == null) throw new IllegalArgumentException("Missing site");
        if (version == null || version.isBlank()) throw new IllegalArgumentException("Missing plan version");
        if (steps == null || steps.isEmpty()) throw new IllegalArgumentException("Empty plan");
        steps = List.copyOf(steps);
        for (var step : steps) if (step == null) throw new IllegalArgumentException("Null step");
    }

    public int operations() { return steps.size(); }
}
