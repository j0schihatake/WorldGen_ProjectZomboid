package personal.worldgen;

import java.util.ArrayList;
import java.util.List;

/** Validates a plan against its site and the protected map profile before commit. */
public final class PlanValidator {
    public record Issue(int index, String reason) {}

    private PlanValidator() {}

    public static List<Issue> validate(SitePlan plan, ProtectedCells guard, int padding) {
        if (plan == null || guard == null) throw new IllegalArgumentException("Missing plan or guard");
        if (padding < 0) throw new IllegalArgumentException("Invalid padding");
        var issues = new ArrayList<Issue>();
        var site = plan.site();
        for (int i = 0; i < plan.steps().size(); i++) {
            var step = plan.steps().get(i);
            long x = (long) site.x() + step.dx();
            long y = (long) site.y() + step.dy();
            if (step.dx() < 0 || step.dy() < 0 || step.dx() >= site.width() || step.dy() >= site.height()
                    || x < Integer.MIN_VALUE || y < Integer.MIN_VALUE
                    || x > Integer.MAX_VALUE || y > Integer.MAX_VALUE) {
                issues.add(new Issue(i, "out_of_bounds"));
                continue;
            }
            if (!guard.allows((int) x, (int) y, 1, 1, padding)) {
                issues.add(new Issue(i, "protected"));
            }
        }
        return List.copyOf(issues);
    }
}
