package personal.worldgen;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Deterministic, bounded search for a free site. The same seed and reservations
 * always produce the same sequence, independent of how often the world was explored.
 */
public final class SitePlanner {
    private static final long MAX_CANDIDATES = 1_000_000;

    private final ProtectedCells guard;
    private final int width, height, padding, step, minX, minY, maxX, maxY;
    private final long seed;
    private final Set<Site> reserved = new HashSet<>();

    public SitePlanner(ProtectedCells guard, int width, int height, int padding, int step,
                       int minX, int minY, int maxX, int maxY, long seed) {
        if (guard == null) throw new IllegalArgumentException("Missing guard");
        if (width <= 0 || height <= 0 || width > Site.MAX_SIZE || height > Site.MAX_SIZE)
            throw new IllegalArgumentException("Invalid site size");
        if (padding < 0 || step <= 0) throw new IllegalArgumentException("Invalid spacing");
        if (maxX < minX || maxY < minY) throw new IllegalArgumentException("Invalid bounds");
        long columns = ((long) maxX - minX) / step + 1;
        long rows = ((long) maxY - minY) / step + 1;
        if (columns * rows > MAX_CANDIDATES) throw new IllegalArgumentException("Too many candidates");
        this.guard = guard;
        this.width = width;
        this.height = height;
        this.padding = padding;
        this.step = step;
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
        this.seed = seed;
    }

    public Optional<Site> find(int maxAttempts) {
        if (maxAttempts <= 0) return Optional.empty();
        long columns = ((long) maxX - minX) / step + 1;
        long rows = ((long) maxY - minY) / step + 1;
        long total = columns * rows;
        long start = Math.floorMod(seed, total);
        for (long i = 0; i < total && i < maxAttempts; i++) {
            long index = (start + i) % total;
            int x = minX + (int) (index % columns) * step;
            int y = minY + (int) (index / columns) * step;
            var site = new Site(x, y, width, height);
            if (reserved.contains(site)) continue;
            if (guard.allows(x, y, width, height, padding)) return Optional.of(site);
        }
        return Optional.empty();
    }

    /** Remember a site so the next search does not return it again. */
    public void reserve(Site site) {
        if (site == null) throw new IllegalArgumentException("Missing site");
        reserved.add(site);
    }

    public int reservedCount() { return reserved.size(); }
}
