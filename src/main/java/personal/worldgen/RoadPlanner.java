package personal.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Deterministic L-shaped road between two site centres; data only. */
public final class RoadPlanner {
    public record Point(int x, int y) {}
    public static final int MAX_POINTS = 4096;

    private RoadPlanner() {}

    public static List<Point> connect(Site from, Site to) {
        if (from == null || to == null) throw new IllegalArgumentException("Missing site");
        return route(from.x() + from.width() / 2, from.y() + from.height() / 2,
                to.x() + to.width() / 2, to.y() + to.height() / 2, true);
    }

    /**
     * Deterministic road that avoids blocked cells. Tries the direct horizontal-first
     * L, then the vertical-first L, then bounded waypoints around the bounding box.
     * Returns an empty list when the endpoints are boxed in or no bounded route exists.
     */
    public static List<Point> connectAvoiding(Site from, Site to, Set<Point> blocked) {
        if (from == null || to == null) throw new IllegalArgumentException("Missing site");
        if (blocked == null) throw new IllegalArgumentException("Missing blocked set");
        if (blocked.isEmpty()) return connect(from, to);
        int startX = from.x() + from.width() / 2;
        int startY = from.y() + from.height() / 2;
        int endX = to.x() + to.width() / 2;
        int endY = to.y() + to.height() / 2;
        var direct = tryRoute(startX, startY, endX, endY, true, blocked);
        if (direct != null) return direct;
        var alternate = tryRoute(startX, startY, endX, endY, false, blocked);
        if (alternate != null) return alternate;
        int minX = Math.min(startX, endX), maxX = Math.max(startX, endX);
        int minY = Math.min(startY, endY), maxY = Math.max(startY, endY);
        for (int d = 1; d <= 64; d++) {
            int[][] candidates = {
                    {minX - d, minY - d}, {maxX + d, minY - d},
                    {minX - d, maxY + d}, {maxX + d, maxY + d}
            };
            for (int[] candidate : candidates) {
                var first = tryRoute(startX, startY, candidate[0], candidate[1], true, blocked);
                if (first == null) continue;
                var second = tryRoute(candidate[0], candidate[1], endX, endY, false, blocked);
                if (second == null) continue;
                var joined = join(first, second);
                if (joined.size() <= MAX_POINTS && clear(joined, blocked)) return joined;
            }
        }
        return List.of();
    }

    private static List<Point> route(int startX, int startY, int endX, int endY,
                                      boolean horizontalFirst) {
        var points = new ArrayList<Point>();
        int x = startX, y = startY;
        points.add(new Point(x, y));
        if (horizontalFirst) {
            while (x != endX) {
                x += Integer.signum(endX - x);
                points.add(new Point(x, y));
                if (points.size() > MAX_POINTS) throw new IllegalArgumentException("Road too long");
            }
            while (y != endY) {
                y += Integer.signum(endY - y);
                points.add(new Point(x, y));
                if (points.size() > MAX_POINTS) throw new IllegalArgumentException("Road too long");
            }
        } else {
            while (y != endY) {
                y += Integer.signum(endY - y);
                points.add(new Point(x, y));
                if (points.size() > MAX_POINTS) throw new IllegalArgumentException("Road too long");
            }
            while (x != endX) {
                x += Integer.signum(endX - x);
                points.add(new Point(x, y));
                if (points.size() > MAX_POINTS) throw new IllegalArgumentException("Road too long");
            }
        }
        return List.copyOf(points);
    }

    private static List<Point> tryRoute(int startX, int startY, int endX, int endY,
                                        boolean horizontalFirst, Set<Point> blocked) {
        try {
            var route = route(startX, startY, endX, endY, horizontalFirst);
            return clear(route, blocked) ? route : null;
        } catch (IllegalArgumentException tooLong) {
            return null;
        }
    }

    private static boolean clear(List<Point> route, Set<Point> blocked) {
        for (var point : route) if (blocked.contains(point)) return false;
        return true;
    }

    private static List<Point> join(List<Point> first, List<Point> second) {
        var result = new ArrayList<>(first);
        for (var point : second) {
            if (result.isEmpty() || !result.get(result.size() - 1).equals(point)) result.add(point);
        }
        return List.copyOf(result);
    }
}
