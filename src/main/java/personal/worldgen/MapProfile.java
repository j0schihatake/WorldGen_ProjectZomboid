package personal.worldgen;

import java.util.Set;

/** Protected map cells together with the game version they were read from. */
public record MapProfile(String gameVersion, Set<ProtectedCells.Cell> cells) {
    public MapProfile {
        if (gameVersion == null || gameVersion.isBlank()) throw new IllegalArgumentException("Missing game version");
        if (cells == null) throw new IllegalArgumentException("Missing cells");
        cells = Set.copyOf(cells);
    }

    public int cellCount() { return cells.size(); }
}
