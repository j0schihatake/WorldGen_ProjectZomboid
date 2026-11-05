package personal.worldgen;

import java.util.function.Consumer;

/** Owns one world's generator state: profile, site search and bounded commit queue. */
public final class WorldGenSession {
    public record Status(String generatorVersion, int protectedCells, int plannedSites,
                         int queuedOperations, boolean paused, Site lastSite) {}

    private final MapProfile profile;
    private final SitePlanner planner;
    private final GenerationQueue queue = new GenerationQueue();
    private final String generatorVersion;
    private int plannedSites;
    private Site lastSite;

    public WorldGenSession(MapProfile profile, SitePlanner planner, String generatorVersion) {
        if (profile == null || planner == null) throw new IllegalArgumentException("Missing session state");
        if (generatorVersion == null || generatorVersion.isBlank())
            throw new IllegalArgumentException("Missing generator version");
        this.profile = profile;
        this.planner = planner;
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

    public void enqueue(SitePlan plan, Consumer<PlanStep> executor) {
        queue.addPlan(plan, executor);
    }

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
