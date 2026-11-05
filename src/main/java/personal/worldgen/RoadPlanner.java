package personal.worldgen;

import java.util.ArrayList;
import java.util.List;

/** Deterministic L-shaped road between two site centres; data only. */
public final class RoadPlanner {
    public record Point(int x, int y) {}
    public static final int MAX_POINTS = 4096;

    private RoadPlanner() {}

    public static List<Point> connect(Site from, Site to) {
        if (from == null || to == null) throw new IllegalArgumentException("Missing site");
        int startX = from.x() + from.width() / 2;
        int startY = from.y() + from.height() / 2;
        int endX = to.x() + to.width() / 2;
        int endY = to.y() + to.height() / 2;
        var points = new ArrayList<Point>();
        int x = startX, y = startY;
        points.add(new Point(x, y));
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
        return List.copyOf(points);
    }
}
