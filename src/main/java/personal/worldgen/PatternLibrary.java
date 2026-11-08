package personal.worldgen;

import java.util.ArrayList;
import java.util.List;

/** Bounded library of reusable patterns with deterministic picking. */
public final class PatternLibrary {
    private final int max;
    private final List<MapPattern.Pattern> patterns = new ArrayList<>();

    public PatternLibrary(int max) { this.max = Math.max(1, Math.min(256, max)); }

    public boolean add(MapPattern.Pattern pattern) {
        if (pattern == null || patterns.size() >= max) return false;
        patterns.add(pattern);
        return true;
    }

    public MapPattern.Pattern pick(int seed) {
        if (patterns.isEmpty()) return null;
        return patterns.get(Math.floorMod(seed, patterns.size()));
    }

    public String roadPattern(int length) {
        int n = Math.max(2, Math.min(64, length));
        var cells = new ArrayList<MapPattern.Cell>();
        for (int i = 0; i < n; i++) cells.add(new MapPattern.Cell(i, 0, "road"));
        return "road=" + new MapPattern.Pattern(n, 1, List.copyOf(cells)).cells().size();
    }

    public String buildingPattern(int width, int height) {
        int w = Math.max(2, Math.min(32, width)), h = Math.max(2, Math.min(32, height));
        var cells = new ArrayList<MapPattern.Cell>();
        for (int x = 0; x < w; x++) { cells.add(new MapPattern.Cell(x, 0, "wall")); cells.add(new MapPattern.Cell(x, h - 1, "wall")); }
        for (int y = 1; y < h - 1; y++) { cells.add(new MapPattern.Cell(0, y, "wall")); cells.add(new MapPattern.Cell(w - 1, y, "wall")); }
        return "building=" + cells.size();
    }

    public boolean has(MapPattern.Pattern pattern) { return patterns.contains(pattern); }
    public boolean remove(MapPattern.Pattern pattern) { return patterns.remove(pattern); }
    public void clear() { patterns.clear(); }
    public java.util.List<MapPattern.Pattern> all() { return List.copyOf(patterns); }

    public int size() { return patterns.size(); }

    public static String demo() {
        var library = new PatternLibrary(8);
        var base = new MapPattern.Pattern(2, 2, List.of(new MapPattern.Cell(0, 0, "wall"), new MapPattern.Cell(1, 1, "door")));
        library.add(base);
        library.add(MapPattern.rotate(base));
        return "size=" + library.size() + " pick=" + library.pick(3).cells().size()
                + " " + library.roadPattern(5) + " " + library.buildingPattern(4, 4);
    }
}
