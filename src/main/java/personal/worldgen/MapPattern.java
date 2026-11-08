package personal.worldgen;

import java.util.ArrayList;
import java.util.List;

/** Bounded, reusable building/camp pattern with deterministic transforms. */
public final class MapPattern {
    public record Cell(int x, int y, String kind) {}
    public record Pattern(int width, int height, List<Cell> cells) {}

    private MapPattern() {}

    public static Pattern rotate(Pattern pattern) {
        var cells = new ArrayList<Cell>();
        for (var cell : pattern.cells()) cells.add(new Cell(pattern.height() - 1 - cell.y(), cell.x(), cell.kind()));
        return new Pattern(pattern.height(), pattern.width(), List.copyOf(cells));
    }

    public static Pattern transpose(Pattern pattern) {
        var cells = new ArrayList<Cell>();
        for (var cell : pattern.cells()) cells.add(new Cell(cell.y(), cell.x(), cell.kind()));
        return new Pattern(pattern.height(), pattern.width(), List.copyOf(cells));
    }

    public static Pattern rotate180(Pattern pattern) {
        return rotate(rotate(pattern));
    }

    public static boolean valid(Pattern pattern) {
        if (pattern == null || pattern.width() <= 0 || pattern.height() <= 0) return false;
        for (var cell : pattern.cells())
            if (cell.x() < 0 || cell.y() < 0 || cell.x() >= pattern.width() || cell.y() >= pattern.height()) return false;
        return true;
    }

    public static Pattern parse(String text) {
        if (text == null || text.isBlank() || !text.contains("|")) return null;
        String[] head = text.substring(0, text.indexOf('|')).split("x");
        if (head.length != 2) return null;
        try {
            int w = Integer.parseInt(head[0]), h = Integer.parseInt(head[1]);
            var cells = new ArrayList<Cell>();
            for (String row : text.substring(text.indexOf('|') + 1).split(";")) {
                if (row.isBlank()) continue;
                String[] p = row.split(",");
                if (p.length != 3) return null;
                cells.add(new Cell(Integer.parseInt(p[0]), Integer.parseInt(p[1]), p[2]));
            }
            var pattern = new Pattern(w, h, List.copyOf(cells));
            return valid(pattern) ? pattern : null;
        } catch (NumberFormatException ignored) { return null; }
    }

    public static int hash(Pattern pattern) {
        return pattern == null ? 0 : java.util.Objects.hash(pattern.width(), pattern.height(), pattern.cells());
    }

    public static Pattern trim(Pattern pattern) {
        if (!valid(pattern) || pattern.cells().isEmpty()) return pattern;
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = 0, maxY = 0;
        for (var c : pattern.cells()) { minX = Math.min(minX, c.x()); minY = Math.min(minY, c.y()); maxX = Math.max(maxX, c.x()); maxY = Math.max(maxY, c.y()); }
        var cells = new ArrayList<Cell>();
        for (var c : pattern.cells()) cells.add(new Cell(c.x() - minX, c.y() - minY, c.kind()));
        return new Pattern(maxX - minX + 1, maxY - minY + 1, List.copyOf(cells));
    }

    public static String countKinds(Pattern pattern) {
        var counts = new java.util.TreeMap<String, Integer>();
        if (pattern != null) for (var c : pattern.cells()) counts.merge(c.kind(), 1, Integer::sum);
        return counts.toString();
    }

    public static int[] bounds(Pattern pattern) {
        if (!valid(pattern) || pattern.cells().isEmpty()) return new int[]{0, 0, 0, 0};
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = 0, maxY = 0;
        for (var c : pattern.cells()) { minX = Math.min(minX, c.x()); minY = Math.min(minY, c.y()); maxX = Math.max(maxX, c.x()); maxY = Math.max(maxY, c.y()); }
        return new int[]{minX, minY, maxX, maxY};
    }

    public static String serialize(Pattern pattern) {
        if (!valid(pattern)) return "";
        var out = new StringBuilder().append(pattern.width()).append('x').append(pattern.height()).append('|');
        for (var cell : pattern.cells())
            out.append(cell.x()).append(',').append(cell.y()).append(',').append(cell.kind()).append(';');
        return out.toString();
    }

    public static Pattern mirror(Pattern pattern) {
        var cells = new ArrayList<Cell>();
        for (var cell : pattern.cells()) cells.add(new Cell(pattern.width() - 1 - cell.x(), cell.y(), cell.kind()));
        return new Pattern(pattern.width(), pattern.height(), List.copyOf(cells));
    }

    public static String demo() {
        var base = new Pattern(3, 2, List.of(new Cell(0, 0, "wall"), new Cell(1, 0, "door"), new Cell(2, 1, "crate")));
        var rotated = rotate(base);
        var mirrored = mirror(base);
        return "base=" + base.cells().size() + " rotated=" + rotated.width() + "x" + rotated.height()
                + " mirrored=" + mirrored.cells().size();
    }
}
