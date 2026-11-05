package personal.worldgen;

import java.util.HashSet;
import java.util.Set;

/** Own planning guard only. No game classes, generator callbacks or world mutations. */
public final class ProtectedCells {
    public record Cell(int x, int y) {}
    private final int cellSize;
    private final Set<Cell> protectedCells;
    private static final int MAX_LOOKUPS = 4096;

    public ProtectedCells(int cellSize, Set<Cell> cells) {
        if (cellSize <= 0 || cells == null) throw new IllegalArgumentException("Missing cell profile");
        this.cellSize = cellSize;
        this.protectedCells = Set.copyOf(new HashSet<>(cells));
    }

    /** Inclusive footprint with padding. Unknown/oversized geometry fails closed. */
    public boolean allows(int x, int y, int width, int height, int padding) {
        if (width <= 0 || height <= 0 || padding < 0) return false;
        long minX = (long)x - padding, minY = (long)y - padding;
        long maxX = (long)x + width - 1 + padding, maxY = (long)y + height - 1 + padding;
        if (minX < Integer.MIN_VALUE || minY < Integer.MIN_VALUE
                || maxX > Integer.MAX_VALUE || maxY > Integer.MAX_VALUE) return false;
        long left = Math.floorDiv(minX, cellSize), top = Math.floorDiv(minY, cellSize);
        long right = Math.floorDiv(maxX, cellSize), bottom = Math.floorDiv(maxY, cellSize);
        long columns = right - left + 1, rows = bottom - top + 1;
        if (columns > MAX_LOOKUPS || rows > MAX_LOOKUPS || columns * rows > MAX_LOOKUPS) return false;
        for (long cx = left; cx <= right; cx++)
            for (long cy = top; cy <= bottom; cy++)
                if (protectedCells.contains(new Cell((int)cx, (int)cy))) return false;
        return true;
    }
}
