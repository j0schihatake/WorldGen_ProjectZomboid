package personal.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Owns one world's generator state: profile, site search and bounded commit queue. */
public final class WorldGenSession {
    public record Status(String generatorVersion, int protectedCells, int plannedSites,
                         int queuedOperations, boolean paused, Site lastSite) {}

    private final MapProfile profile;
    private final SitePlanner planner;
    private final GenerationQueue queue = new GenerationQueue();
    private final ProtectedCells guard;
    private final String generatorVersion;
    private final List<SitePlan> plans = new ArrayList<>();
    private int plannedSites;
    private Site lastSite;

    public WorldGenSession(MapProfile profile, SitePlanner planner, String generatorVersion) {
        if (profile == null || planner == null) throw new IllegalArgumentException("Missing session state");
        if (generatorVersion == null || generatorVersion.isBlank())
            throw new IllegalArgumentException("Missing generator version");
        this.profile = profile;
        this.planner = planner;
        this.guard = new ProtectedCells(256, profile);
        this.generatorVersion = generatorVersion;
    }

    /** Plan and reserve one site. Returns null when no legal site is left. */
    public synchronized Site plan(int attempts) {
        if (attempts <= 0) return null;
        Site site = planner.find(attempts).orElse(null);
        if (site == null) return null;
        planner.reserve(site);
        plannedSites++;
        lastSite = site;
        return site;
    }

    public record Stats(int pending, long completed, boolean paused) {}

    public Stats stats() {
        return new Stats(queue.pendingOps(), queue.completedOps(), queue.isPaused());
    }

    public java.util.List<PlanValidator.Issue> validate(SitePlan plan) {
        return PlanValidator.validate(plan, guard, 0);
    }

    public SettlementPlanner.Settlement planSettlement(Site campSite, Site buildingSite, Site roadTarget,
                                                       long seed, Consumer<PlanStep> executor) {
        var settlement = SettlementPlanner.planValidated(campSite, buildingSite, roadTarget,
                generatorVersion, seed, guard, 0);
        enqueue(settlement.camp(), executor);
        enqueue(settlement.building().asSitePlan(generatorVersion), executor);
        return settlement;
    }

    public SettlementPlanner.District planDistrict(java.util.List<Site> campSites,
                                                   java.util.List<Site> buildingSites,
                                                   Site roadTarget, long seed,
                                                   Consumer<PlanStep> executor) {
        var district = SettlementPlanner.planDistrict(campSites, buildingSites, roadTarget,
                generatorVersion, seed, guard, 0);
        for (var settlement : district.settlements()) {
            enqueue(settlement.camp(), executor);
            enqueue(settlement.building().asSitePlan(generatorVersion), executor);
        }
        return district;
    }

    public void enqueue(SitePlan plan, Consumer<PlanStep> executor) {
        var issues = validate(plan);
        if (!issues.isEmpty()) throw new IllegalArgumentException("Invalid plan: " + issues);
        plans.add(plan);
        queue.addPlan(plan, executor);
    }

    public void exportPlan(java.nio.file.Path file) throws java.io.IOException {
        PlanFile.write(file, List.copyOf(plans));
    }

    public int recordedPlans() { return plans.size(); }

    public int tick(int operations, long deadlineNanos) {
        return queue.run(operations, deadlineNanos);
    }

    public void pause() { queue.pause(); }
    public void resume() { queue.resume(); }

    public synchronized Status status() {
        return new Status(generatorVersion, profile.cellCount(), plannedSites,
                queue.pendingOps(), queue.isPaused(), lastSite);
    }
}
