package personal.worldgen;

import java.util.ArrayList;
import java.util.List;

/** Composes a camp, a building and a road into one deterministic settlement plan. */
public final class SettlementPlanner {
    public record Settlement(SitePlan camp, BuildingPlan building, List<RoadPlanner.Point> road) {
        public Settlement {
            if (camp == null || building == null || road == null || road.isEmpty())
                throw new IllegalArgumentException("Incomplete settlement");
            road = List.copyOf(road);
        }
    }

    private SettlementPlanner() {}

    public static Settlement plan(Site campSite, Site buildingSite, Site roadTarget,
                                  String version, long seed) {
        if (campSite == null || buildingSite == null || roadTarget == null)
            throw new IllegalArgumentException("Missing settlement site");
        if (overlaps(campSite, buildingSite))
            throw new IllegalArgumentException("Camp and building overlap");
        var camp = CampPlanner.plan(campSite, version, seed, 8);
        var building = BuildingPlanner.planBuilding(buildingSite, version, seed + 1, 3);
        var road = RoadPlanner.connect(buildingSite, roadTarget);
        return new Settlement(camp, building, road);
    }

    /** Same plan, but refuses protected cells before enqueueing anything. */
    public static Settlement planValidated(Site campSite, Site buildingSite, Site roadTarget,
                                           String version, long seed,
                                           ProtectedCells guard, int padding) {
        if (guard == null) throw new IllegalArgumentException("Missing guard");
        var settlement = plan(campSite, buildingSite, roadTarget, version, seed);
        var issues = new ArrayList<>(PlanValidator.validate(settlement.camp(), guard, padding));
        issues.addAll(PlanValidator.validate(settlement.building().asSitePlan(version), guard, padding));
        if (!issues.isEmpty()) throw new IllegalArgumentException("Protected settlement: " + issues);
        return settlement;
    }

    public static boolean overlaps(Site a, Site b) {
        return a.x() < b.x() + b.width() && b.x() < a.x() + a.width()
                && a.y() < b.y() + b.height() && b.y() < a.y() + a.height();
    }
}
