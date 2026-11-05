package personal.worldgen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Deterministic, bounded camp layout; produces data, not world objects. */
public final class CampPlanner {
    private static final String[] KINDS = {"campfire", "tent", "crate", "bedroll"};
    public static final int MAX_PROPS = 64;
    private static final int MARGIN = 4;
    private static final int SPACING = 4;

    private CampPlanner() {}

    public static SitePlan plan(Site site, String version, long seed, int props) {
        if (site == null) throw new IllegalArgumentException("Missing site");
        if (version == null || version.isBlank()) throw new IllegalArgumentException("Missing version");
        if (props <= 0 || props > MAX_PROPS) throw new IllegalArgumentException("Invalid prop count");
        int usableWidth = site.width() - 2 * MARGIN;
        int usableHeight = site.height() - 2 * MARGIN;
        int columns = Math.max(0, usableWidth / SPACING);
        int rows = Math.max(0, usableHeight / SPACING);
        if ((long) columns * rows < props) throw new IllegalArgumentException("Site is too small for the camp");

        var cells = new ArrayList<int[]>();
        for (int col = 0; col < columns; col++)
            for (int row = 0; row < rows; row++)
                cells.add(new int[]{MARGIN + col * SPACING, MARGIN + row * SPACING});
        Collections.shuffle(cells, new Random(seed));
        var steps = new ArrayList<PlanStep>();
        for (int i = 0; i < props; i++) {
            int[] cell = cells.get(i);
            steps.add(new PlanStep(KINDS[i % KINDS.length], cell[0], cell[1]));
        }
        return new SitePlan(site, version, steps);
    }
}
