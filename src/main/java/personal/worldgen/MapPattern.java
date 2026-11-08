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
