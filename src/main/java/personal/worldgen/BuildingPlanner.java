package personal.worldgen;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

/** Deterministic house layout with walls, floor, doors and room dividers; data only. */
public final class BuildingPlanner {
    public static final int MAX_ROOMS = 8;
    private static final int MARGIN = 8;
    private static final int MIN_WIDTH = 24;
    private static final int MIN_HEIGHT = 16;

    private BuildingPlanner() {}

    public static SitePlan plan(Site site, String version, long seed, int rooms) {
        return planBuilding(site, version, seed, rooms).asSitePlan(version);
    }

    public static BuildingPlan planBuilding(Site site, String version, long seed, int rooms) {
        if (site == null) throw new IllegalArgumentException("Missing site");
        if (version == null || version.isBlank()) throw new IllegalArgumentException("Missing version");
        if (rooms <= 0 || rooms > MAX_ROOMS) throw new IllegalArgumentException("Invalid room count");
        int width = site.width() - 2 * MARGIN;
        int height = site.height() - 2 * MARGIN;
        if (width < MIN_WIDTH || height < MIN_HEIGHT)
            throw new IllegalArgumentException("Site is too small for a building");
        var random = new Random(seed);
        var steps = new ArrayList<PlanStep>();
        var used = new HashSet<Long>();

        for (int x = MARGIN; x < MARGIN + width; x++) {
            add(steps, used, "wall", x, MARGIN);
            add(steps, used, "wall", x, MARGIN + height - 1);
        }
        for (int y = MARGIN; y < MARGIN + height; y++) {
            add(steps, used, "wall", MARGIN, y);
            add(steps, used, "wall", MARGIN + width - 1, y);
        }

        int side = random.nextInt(4);
        if (side == 0) replaceDoor(steps, used, MARGIN + 2 + random.nextInt(Math.max(1, width - 4)), MARGIN);
        else if (side == 1) replaceDoor(steps, used, MARGIN + 2 + random.nextInt(Math.max(1, width - 4)), MARGIN + height - 1);
        else if (side == 2) replaceDoor(steps, used, MARGIN, MARGIN + 2 + random.nextInt(Math.max(1, height - 4)));
        else replaceDoor(steps, used, MARGIN + width - 1, MARGIN + 2 + random.nextInt(Math.max(1, height - 4)));

        for (int room = 1; room < rooms; room++) {
            int divider = MARGIN + room * width / rooms;
            int doorY = MARGIN + 2 + random.nextInt(Math.max(1, height - 4));
            for (int y = MARGIN + 1; y < MARGIN + height - 1; y++) {
                if (y == doorY) add(steps, used, "door", divider, y);
                else add(steps, used, "wall", divider, y);
            }
        }
        for (int x = MARGIN + 1; x < MARGIN + width - 1; x++)
            for (int y = MARGIN + 1; y < MARGIN + height - 1; y++)
                add(steps, used, "floor", x, y);

        var roomList = new ArrayList<BuildingPlan.Room>();
        int left = MARGIN + 1;
        for (int room = 0; room < rooms; room++) {
            int right = room == rooms - 1 ? MARGIN + width - 1 : MARGIN + (room + 1) * width / rooms;
            if (right > left) roomList.add(new BuildingPlan.Room(left, MARGIN + 1, right - left, height - 2));
            left = right;
        }
        return new BuildingPlan(site, roomList, steps);
    }

    private static void add(List<PlanStep> steps, HashSet<Long> used, String kind, int x, int y) {
        long key = ((long) x << 32) ^ (y & 0xffffffffL);
        if (used.add(key)) steps.add(new PlanStep(kind, x, y));
    }

    private static void replaceDoor(List<PlanStep> steps, HashSet<Long> used, int x, int y) {
        long key = ((long) x << 32) ^ (y & 0xffffffffL);
        used.remove(key);
        steps.removeIf(step -> step.dx() == x && step.dy() == y);
        add(steps, used, "door", x, y);
    }
}
