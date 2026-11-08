package personal.worldgen;

import java.util.ArrayList;
import java.util.List;

/** Ten reusable structure patterns for camps, roads and buildings. */
public final class StructurePatterns {
    private StructurePatterns() {}

    private static MapPattern.Pattern box(int w, int h, String kind) {
        var cells = new ArrayList<MapPattern.Cell>();
        for (int x = 0; x < w; x++) { cells.add(new MapPattern.Cell(x, 0, kind)); cells.add(new MapPattern.Cell(x, h - 1, kind)); }
        for (int y = 1; y < h - 1; y++) { cells.add(new MapPattern.Cell(0, y, kind)); cells.add(new MapPattern.Cell(w - 1, y, kind)); }
        return new MapPattern.Pattern(w, h, List.copyOf(cells));
    }

    public static MapPattern.Pattern house() { return box(6, 5, "wall"); }
    public static MapPattern.Pattern shop() { return box(8, 6, "wall"); }
    public static MapPattern.Pattern warehouse() { return box(12, 9, "wall"); }
    public static MapPattern.Pattern camp() { return box(5, 5, "fence"); }
    public static MapPattern.Pattern road(int length) {
        var cells = new ArrayList<MapPattern.Cell>();
        int n = Math.max(2, Math.min(64, length));
        for (int i = 0; i < n; i++) cells.add(new MapPattern.Cell(i, 0, "road"));
        return new MapPattern.Pattern(n, 1, List.copyOf(cells));
    }
    public static MapPattern.Pattern gate() { return new MapPattern.Pattern(3, 1, List.of(new MapPattern.Cell(1, 0, "gate"))); }
    public static MapPattern.Pattern fence(int length) { return road(length); }
    public static MapPattern.Pattern garden() { return box(4, 4, "soil"); }
    public static MapPattern.Pattern checkpoint() { return box(4, 3, "barrier"); }
    public static MapPattern.Pattern parking() { return box(5, 4, "asphalt"); }

    public static String demo() {
        return "house=" + house().cells().size() + " shop=" + shop().cells().size()
                + " warehouse=" + warehouse().cells().size() + " camp=" + camp().cells().size()
                + " road=" + road(5).cells().size() + " gate=" + gate().cells().size()
                + " fence=" + fence(4).cells().size() + " garden=" + garden().cells().size()
                + " checkpoint=" + checkpoint().cells().size() + " parking=" + parking().cells().size();
    }
}
