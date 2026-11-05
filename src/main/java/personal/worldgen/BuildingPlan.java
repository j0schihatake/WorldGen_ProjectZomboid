package personal.worldgen;

import java.util.List;

/** Building layout with room rectangles; data for future BuildingDef/RoomDef creation. */
public record BuildingPlan(Site site, List<Room> rooms, List<PlanStep> steps) {
    public record Room(int x, int y, int width, int height) {
        public Room {
            if (width <= 0 || height <= 0) throw new IllegalArgumentException("Invalid room");
        }
    }

    public BuildingPlan {
        if (site == null) throw new IllegalArgumentException("Missing site");
        if (rooms == null || rooms.isEmpty()) throw new IllegalArgumentException("Empty building");
        if (steps == null || steps.isEmpty()) throw new IllegalArgumentException("Empty steps");
        rooms = List.copyOf(rooms);
        steps = List.copyOf(steps);
        for (var room : rooms) if (room == null) throw new IllegalArgumentException("Null room");
        for (var step : steps) if (step == null) throw new IllegalArgumentException("Null step");
    }

    public SitePlan asSitePlan(String version) {
        return new SitePlan(site, version, steps);
    }
}
