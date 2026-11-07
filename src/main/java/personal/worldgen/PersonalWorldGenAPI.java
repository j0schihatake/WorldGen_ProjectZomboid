package personal.worldgen;

/** Read-only game-facing entry point. Plans data; never writes to the world yet. */
@me.zed_0xff.zombie_buddy.Exposer.LuaClass
public final class PersonalWorldGenAPI {
    public static final String VERSION = "0.8.0";
    private static final PersonalWorldGenAPI INSTANCE = new PersonalWorldGenAPI();

    private PersonalWorldGenAPI() {}

    public static PersonalWorldGenAPI getInstance() { return INSTANCE; }

    public String status() {
        try (var input = PersonalWorldGenAPI.class.getResourceAsStream("/personal/worldgen/profile.tsv")) {
            var profile = MapProfileIO.read(input);
            return "core=" + VERSION + " profileCells=" + profile.cells().size()
                    + " profileVersion=" + profile.gameVersion() + " worldWrites=0 mode=planning";
        } catch (Exception error) {
            return "core=" + VERSION + " profile=missing worldWrites=0 mode=planning";
        }
    }

    /** Plans a camp near the player using only the position; no world writes. */
    /** First controlled write: one world item on the player's square. */
    public String placeMarker(Object player) {
        if (player == null) return "no-player";
        try {
            Object square = player.getClass().getMethod("getCurrentSquare").invoke(player);
            if (square == null) return "no-square";
            Object item = square.getClass().getMethod("AddWorldInventoryItem",
                            String.class, float.class, float.class, float.class)
                    .invoke(square, "Base.Hammer", 0.5f, 0.5f, 0f);
            return item != null ? "marker=placed worldWrites=1" : "marker=failed worldWrites=0";
        } catch (Throwable error) {
            return "marker-error=" + error.getClass().getSimpleName() + " worldWrites=0";
        }
    }

    /** Places a bounded camp plan into the test world as item markers. */
    public String placeCamp(Object player) {
        if (player == null) return "no-player";
        try {
            Object square = player.getClass().getMethod("getCurrentSquare").invoke(player);
            if (square == null) return "no-square";
            var plan = CampPlanner.plan(new Site(0, 0, 32, 32), VERSION, 42, 8);
            Object cell = player.getClass().getMethod("getCell").invoke(player);
            int placed = 0;
            for (var step : plan.steps()) {
                String item = switch (step.kind()) {
                    case "tent" -> "Base.CampingTentKit2";
                    default -> "Base.Hammer";
                };
                try {
                    Object target = cell.getClass().getMethod("getGridSquare", int.class, int.class, int.class)
                            .invoke(cell,
                                    ((Number) player.getClass().getMethod("getXi").invoke(player)).intValue() + step.dx(),
                                    ((Number) player.getClass().getMethod("getYi").invoke(player)).intValue() + step.dy(),
                                    ((Number) player.getClass().getMethod("getZi").invoke(player)).intValue());
                    if (target == null) continue;
                    target.getClass().getMethod("AddWorldInventoryItem", String.class, float.class, float.class, float.class)
                            .invoke(target, item, 0.5f, 0.5f, 0f);
                    placed++;
                } catch (Throwable ignored) { }
            }
            return "campOps=" + plan.steps().size() + " placed=" + placed + " worldWrites=" + placed;
        } catch (Throwable error) {
            return "camp-error=" + error.getClass().getSimpleName() + " worldWrites=0";
        }
    }

    /** Places a bounded road marker set into the test world. */
    public String placeRoad(Object player) {
        if (player == null) return "no-player";
        try {
            var points = RoadPlanner.connect(new Site(0, 0, 32, 32), new Site(64, 0, 32, 32));
            Object cell = player.getClass().getMethod("getCell").invoke(player);
            int bx = ((Number) player.getClass().getMethod("getXi").invoke(player)).intValue();
            int by = ((Number) player.getClass().getMethod("getYi").invoke(player)).intValue();
            int bz = ((Number) player.getClass().getMethod("getZi").invoke(player)).intValue();
            int placed = 0;
            for (int i = 0; i < points.size(); i += 4) {
                var point = points.get(i);
                try {
                    Object target = cell.getClass().getMethod("getGridSquare", int.class, int.class, int.class)
                            .invoke(cell, bx + point.x(), by + point.y(), bz);
                    if (target == null) continue;
                    target.getClass().getMethod("AddWorldInventoryItem", String.class, float.class, float.class, float.class)
                            .invoke(target, "Base.Hammer", 0.5f, 0.5f, 0f);
                    placed++;
                } catch (Throwable ignored) { }
            }
            return "roadPoints=" + points.size() + " placed=" + placed + " worldWrites=" + placed;
        } catch (Throwable error) {
            return "road-error=" + error.getClass().getSimpleName() + " worldWrites=0";
        }
    }

    /** Places a bounded building marker set into the test world. */
    public String placeBuilding(Object player) {
        if (player == null) return "no-player";
        try {
            var plan = BuildingPlanner.plan(new Site(0, 0, 48, 40), VERSION, 42, 3);
            Object cell = player.getClass().getMethod("getCell").invoke(player);
            int bx = ((Number) player.getClass().getMethod("getXi").invoke(player)).intValue();
            int by = ((Number) player.getClass().getMethod("getYi").invoke(player)).intValue();
            int bz = ((Number) player.getClass().getMethod("getZi").invoke(player)).intValue();
            int placed = 0;
            for (var step : plan.steps()) {
                if (placed >= 24) break;
                try {
                    Object target = cell.getClass().getMethod("getGridSquare", int.class, int.class, int.class)
                            .invoke(cell, bx + step.dx(), by + step.dy(), bz);
                    if (target == null) continue;
                    target.getClass().getMethod("AddWorldInventoryItem", String.class, float.class, float.class, float.class)
                            .invoke(target, "Base.Hammer", 0.5f, 0.5f, 0f);
                    placed++;
                } catch (Throwable ignored) { }
            }
            return "buildingOps=" + plan.steps().size() + " placed=" + placed + " worldWrites=" + placed;
        } catch (Throwable error) {
            return "building-error=" + error.getClass().getSimpleName() + " worldWrites=0";
        }
    }

    /** One bounded test-site pipeline: camp, road and building markers. */
    public String generateTestSite(Object player) {
        String camp = placeCamp(player);
        String road = placeRoad(player);
        String building = placeBuilding(player);
        int campCount = countPlaced(camp);
        int roadCount = countPlaced(road);
        int buildingCount = countPlaced(building);
        return "camp=" + campCount + " road=" + roadCount + " building=" + buildingCount
                + " worldWrites=" + (campCount + roadCount + buildingCount);
    }

    private static int countPlaced(String value) {
        int at = value == null ? -1 : value.indexOf("placed=");
        if (at < 0) return 0;
        int end = value.indexOf(' ', at);
        try {
            return Integer.parseInt(value.substring(at + 7, end < 0 ? value.length() : end));
        } catch (NumberFormatException error) {
            return 0;
        }
    }

    /** Places a short line of real wall objects in the test world. */
    public String placeRealWall(Object player) {
        if (player == null) return "no-player";
        try {
            Object cell = player.getClass().getMethod("getCell").invoke(player);
            Class<?> squareClass = Class.forName("zombie.iso.IsoGridSquare");
            Class<?> objectClass = Class.forName("zombie.iso.IsoObject");
            int bx = ((Number) player.getClass().getMethod("getXi").invoke(player)).intValue();
            int by = ((Number) player.getClass().getMethod("getYi").invoke(player)).intValue();
            int bz = ((Number) player.getClass().getMethod("getZi").invoke(player)).intValue();
            int placed = 0;
            for (int i = 0; i < 4; i++) {
                Object target = cell.getClass().getMethod("getGridSquare", int.class, int.class, int.class)
                        .invoke(cell, bx + 3 + i, by + 3, bz);
                if (target == null) continue;
                Object object = objectClass.getConstructor(squareClass, String.class)
                        .newInstance(target, "walls_exterior_house_01_0");
                object.getClass().getMethod("addToWorld").invoke(object);
                placed++;
            }
            return "realWalls=" + placed + " worldWrites=" + placed;
        } catch (Throwable error) {
            return "wall-error=" + error.getClass().getSimpleName() + " worldWrites=0";
        }
    }

    private MapProfile profile() throws java.io.IOException {
        try (var input = PersonalWorldGenAPI.class.getResourceAsStream("/personal/worldgen/profile.tsv")) {
            return MapProfileIO.read(input);
        }
    }

    public String planNearPlayer(Object player, long seed) {
        if (player == null) return "no-player";
        try {
            double px = ((Number) player.getClass().getMethod("getX").invoke(player)).doubleValue();
            double py = ((Number) player.getClass().getMethod("getY").invoke(player)).doubleValue();
            int x = (int) px + 10;
            int y = (int) py + 10;
            var guard = new ProtectedCells(256, profile());
            if (!guard.allows(x, y, 32, 32, 0)) {
                return "x=" + x + " y=" + y + " protected=true worldWrites=0";
            }
            var plan = CampPlanner.plan(new Site(x, y, 32, 32), VERSION, seed, 8);
            return "x=" + x + " y=" + y + " campOps=" + plan.operations() + " protected=false worldWrites=0";
        } catch (ReflectiveOperationException | java.io.IOException error) {
            return "player-probe-failed=" + error.getClass().getSimpleName();
        }
    }

    public String planDemo(long seed) {
        var settlement = SettlementPlanner.plan(
                new Site(0, 0, 32, 32),
                new Site(64, 0, 48, 40),
                new Site(128, 0, 16, 16),
                VERSION, seed);
        return "campOps=" + settlement.camp().operations()
                + " buildingOps=" + settlement.building().steps().size()
                + " roadCells=" + settlement.road().size()
                + " worldWrites=0";
    }
}
