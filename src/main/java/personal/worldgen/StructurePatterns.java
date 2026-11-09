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

    public static MapPattern.Pattern smallHouse() { return box(4, 4, "wall"); }
    public static MapPattern.Pattern largeHouse() { return box(8, 7, "wall"); }
    public static MapPattern.Pattern smallShop() { return box(6, 4, "wall"); }
    public static MapPattern.Pattern largeShop() { return box(10, 8, "wall"); }
    public static MapPattern.Pattern smallWarehouse() { return box(8, 6, "wall"); }
    public static MapPattern.Pattern largeWarehouse() { return box(16, 12, "wall"); }
    public static MapPattern.Pattern smallCamp() { return box(4, 4, "fence"); }
    public static MapPattern.Pattern largeCamp() { return box(8, 8, "fence"); }
    public static MapPattern.Pattern shortRoad() { return road(3); }
    public static MapPattern.Pattern longRoad() { return road(12); }
    public static MapPattern.Pattern wideGate() { return new MapPattern.Pattern(5, 1, List.of(new MapPattern.Cell(2, 0, "gate"))); }
    public static MapPattern.Pattern doubleGate() { return new MapPattern.Pattern(5, 1, List.of(new MapPattern.Cell(1, 0, "gate"), new MapPattern.Cell(3, 0, "gate"))); }
    public static MapPattern.Pattern smallGarden() { return box(3, 3, "soil"); }
    public static MapPattern.Pattern largeGarden() { return box(6, 6, "soil"); }
    public static MapPattern.Pattern smallCheckpoint() { return box(3, 2, "barrier"); }
    public static MapPattern.Pattern largeCheckpoint() { return box(6, 4, "barrier"); }
    public static MapPattern.Pattern smallParking() { return box(4, 3, "asphalt"); }
    public static MapPattern.Pattern largeParking() { return box(8, 6, "asphalt"); }
    public static MapPattern.Pattern tinyFence() { return fence(2); }
    public static MapPattern.Pattern longFence() { return fence(10); }

    public static MapPattern.Pattern tinyHouse() { return box(3, 3, "wall"); }
    public static MapPattern.Pattern hugeHouse() { return box(12, 10, "wall"); }
    public static MapPattern.Pattern tinyShop() { return box(4, 3, "wall"); }
    public static MapPattern.Pattern hugeShop() { return box(14, 10, "wall"); }
    public static MapPattern.Pattern tinyCamp() { return box(3, 3, "fence"); }
    public static MapPattern.Pattern hugeCamp() { return box(12, 12, "fence"); }

    public static MapPattern.Pattern barracks() { return box(10, 6, "wall"); }
    public static MapPattern.Pattern hospital() { return box(12, 10, "wall"); }
    public static MapPattern.Pattern church() { return box(8, 12, "wall"); }
    public static MapPattern.Pattern school() { return box(14, 8, "wall"); }
    public static MapPattern.Pattern gasStation() { return box(6, 5, "wall"); }
    public static MapPattern.Pattern farmField() { return box(10, 10, "soil"); }
    public static MapPattern.Pattern junkyard() { return box(9, 9, "fence"); }
    public static MapPattern.Pattern motel() { return box(14, 5, "wall"); }
    public static MapPattern.Pattern policeStation() { return box(9, 7, "wall"); }
    public static MapPattern.Pattern warehouseRoad() { return road(16); }

    public static MapPattern.Pattern fireStation() { return box(10, 8, "wall"); }
    public static MapPattern.Pattern clinic() { return box(8, 6, "wall"); }
    public static MapPattern.Pattern pharmacy() { return box(6, 5, "wall"); }
    public static MapPattern.Pattern bank() { return box(9, 7, "wall"); }
    public static MapPattern.Pattern diner() { return box(7, 5, "wall"); }
    public static MapPattern.Pattern bar() { return box(6, 6, "wall"); }
    public static MapPattern.Pattern restaurant() { return box(9, 7, "wall"); }
    public static MapPattern.Pattern hardware() { return box(10, 8, "wall"); }
    public static MapPattern.Pattern gunShop() { return box(6, 5, "wall"); }
    public static MapPattern.Pattern gym() { return box(8, 6, "wall"); }
    public static MapPattern.Pattern apartment() { return box(10, 12, "wall"); }
    public static MapPattern.Pattern trailer() { return box(5, 3, "wall"); }
    public static MapPattern.Pattern farmhouse() { return box(7, 6, "wall"); }
    public static MapPattern.Pattern barn() { return box(9, 11, "wall"); }

    public static String landmarksDemo() {
        return "barracks=" + barracks().cells().size() + " hospital=" + hospital().cells().size()
                + " church=" + church().cells().size() + " school=" + school().cells().size()
                + " gas=" + gasStation().cells().size() + " field=" + farmField().cells().size()
                + " junkyard=" + junkyard().cells().size() + " motel=" + motel().cells().size()
                + " police=" + policeStation().cells().size() + " road=" + warehouseRoad().cells().size();
    }

    public static String demo() {
        return "house=" + house().cells().size() + " shop=" + shop().cells().size()
                + " warehouse=" + warehouse().cells().size() + " camp=" + camp().cells().size()
                + " road=" + road(5).cells().size() + " gate=" + gate().cells().size()
                + " fence=" + fence(4).cells().size() + " garden=" + garden().cells().size()
                + " checkpoint=" + checkpoint().cells().size() + " parking=" + parking().cells().size();
    }
}
