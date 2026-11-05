package personal.worldgen;

import java.util.HashSet;
import java.util.Set;

public final class ProtectedCellsTest {
    public static void main(String[] args) {
        var cells = new HashSet<>(Set.of(new ProtectedCells.Cell(1, 1), new ProtectedCells.Cell(-1, -1)));
        var guard = new ProtectedCells(256, cells);
        cells.clear(); // The profile must not change under an existing world plan.
        assert !guard.allows(300, 300, 1, 1, 0);
        assert guard.allows(255, 255, 1, 1, 0);
        assert !guard.allows(255, 255, 2, 2, 0) : "Free anchor does not make the whole site free";
        assert !guard.allows(255, 255, 1, 1, 1) : "Include the requested buffer";
        assert !guard.allows(-1, -1, 1, 1, 0) : "Negative coordinates use floor division";
        assert guard.allows(0, 0, 1, 1, 0);
        assert !guard.allows(Integer.MAX_VALUE, 0, 2, 1, 0);
        assert !guard.allows(Integer.MIN_VALUE, 0, 1, 1, 1);
        assert !guard.allows(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE, 0) : "Bound work before looking up cells";
        assert !guard.allows(0, 0, 0, 1, 0);
        assert !guard.allows(0, 0, 1, 1, -1);
        System.out.println("Protected-cell tests passed: whole footprint, borders, buffer, negative coordinates, immutable profile, overflow and lookup budget.");
    }
}
