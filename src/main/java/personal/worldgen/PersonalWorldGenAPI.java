package personal.worldgen;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Read-only game-facing entry point. Plans data; never writes to the world yet. */
@me.zed_0xff.zombie_buddy.Exposer.LuaClass
public final class PersonalWorldGenAPI {
    public static final String VERSION = "1.15.0";
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

    /** Places real wall objects from the building plan, bounded to 32. */
    public String placeRealBuilding(Object player) {
        if (player == null) return "no-player";
        try {
            var plan = BuildingPlanner.plan(new Site(0, 0, 48, 40), VERSION, 42, 3);
            Object cell = player.getClass().getMethod("getCell").invoke(player);
            Class<?> squareClass = Class.forName("zombie.iso.IsoGridSquare");
            Class<?> objectClass = Class.forName("zombie.iso.IsoObject");
            int bx = ((Number) player.getClass().getMethod("getXi").invoke(player)).intValue();
            int by = ((Number) player.getClass().getMethod("getYi").invoke(player)).intValue();
            int bz = ((Number) player.getClass().getMethod("getZi").invoke(player)).intValue();
            int walls = 0, floors = 0, doors = 0;
            for (var step : plan.steps()) {
                if ("wall".equals(step.kind()) && walls >= 32) continue;
                if ("floor".equals(step.kind()) && floors >= 24) continue;
                if ("door".equals(step.kind()) && doors >= 8) continue;
                String sprite = switch (step.kind()) {
                    case "wall" -> "walls_exterior_house_01_0";
                    case "floor" -> "floors_interior_carpet_01_0";
                    case "door" -> "doors_01_0";
                    default -> "";
                };
                if (sprite.isEmpty()) continue;
                Object target = cell.getClass().getMethod("getGridSquare", int.class, int.class, int.class)
                        .invoke(cell, bx + step.dx(), by + step.dy(), bz);
                if (target == null) continue;
                Object object = objectClass.getConstructor(squareClass, String.class)
                        .newInstance(target, sprite);
                object.getClass().getMethod("addToWorld").invoke(object);
                if ("wall".equals(step.kind())) walls++;
                else if ("floor".equals(step.kind())) floors++;
                else doors++;
            }
            return "realBuildingWalls=" + walls + " floors=" + floors + " doors=" + doors
                    + " worldWrites=" + (walls + floors + doors);
        } catch (Throwable error) { return "building-wall-error=" + error.getClass().getSimpleName() + " worldWrites=0"; }
    }

    /** Places real street-floor objects along the planned road. */
    public String placeRealRoad(Object player) {
        if (player == null) return "no-player";
        try {
            var points = RoadPlanner.connect(new Site(0, 0, 32, 32), new Site(64, 0, 32, 32));
            Object cell = player.getClass().getMethod("getCell").invoke(player);
            Class<?> squareClass = Class.forName("zombie.iso.IsoGridSquare");
            Class<?> objectClass = Class.forName("zombie.iso.IsoObject");
            int bx = ((Number) player.getClass().getMethod("getXi").invoke(player)).intValue();
            int by = ((Number) player.getClass().getMethod("getYi").invoke(player)).intValue();
            int bz = ((Number) player.getClass().getMethod("getZi").invoke(player)).intValue();
            int placed = 0;
            for (int i = 0; i < points.size(); i += 4) {
                var point = points.get(i);
                Object target = cell.getClass().getMethod("getGridSquare", int.class, int.class, int.class)
                        .invoke(cell, bx + point.x(), by + point.y(), bz);
                if (target == null) continue;
                Object object = objectClass.getConstructor(squareClass, String.class)
                        .newInstance(target, "floors_exterior_street_01_0");
                object.getClass().getMethod("addToWorld").invoke(object);
                placed++;
            }
            return "realRoad=" + placed + " worldWrites=" + placed;
        } catch (Throwable error) { return "road-error=" + error.getClass().getSimpleName() + " worldWrites=0"; }
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

    /** Farthest loaded chunk in eight directions, then one chunk beyond it. */
    private int[] edgePoint(Object player) throws ReflectiveOperationException {
        int px = ((Number) player.getClass().getMethod("getXi").invoke(player)).intValue();
        int py = ((Number) player.getClass().getMethod("getYi").invoke(player)).intValue();
        Object cell = player.getClass().getMethod("getCell").invoke(player);
        int pcx = Math.floorDiv(px, 10), pcy = Math.floorDiv(py, 10);
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}};
        int bestX = pcx, bestY = pcy, bestDistance = 0, bestDx = 1, bestDy = 0;
        for (int[] dir : dirs) {
            for (int distance = 1; distance <= 12; distance++) {
                int cx = pcx + dir[0] * distance, cy = pcy + dir[1] * distance;
                Object chunk = cell.getClass().getMethod("getChunkForGridSquare",
                                int.class, int.class, int.class)
                        .invoke(cell, cx * 10 + 5, cy * 10 + 5, 0);
                if (chunk == null) break;
                boolean loaded;
                try { loaded = chunk.getClass().getField("loaded").getBoolean(chunk); }
                catch (Throwable stop) { break; }
                if (!loaded) break;
                if (distance > bestDistance) {
                    bestDistance = distance;
                    bestX = cx;
                    bestY = cy;
                    bestDx = dir[0];
                    bestDy = dir[1];
                }
            }
        }
        int x = bestX * 10 + 5;
        int y = bestY * 10 + 5;
        edgeInfo = "edgeChunk=" + bestX + "," + bestY + " distance=" + bestDistance
                + " x=" + x + " y=" + y;
        return new int[]{x, y};
    }

    public String edgeStatus(Object player) {
        if (player == null) return "no-player";
        try { edgePoint(player); return edgeInfo; }
        catch (Throwable error) { return "edge-error=" + error.getClass().getSimpleName(); }
    }

    /** Plans in a background thread; never touches game objects there. */
    public String startBackgroundPlan(Object player) {
        if (player == null) return "no-player";
        try {
            int[] edge = edgePoint(player);
            int x = edge[0];
            int y = edge[1];
            planState = "planning";
            planResult = null;
            planner.submit(() -> {
                try {
                    planResult = CampPlanner.plan(new Site(x, y, 16, 16), VERSION, 42, 2);
                    planState = "ready";
                } catch (Throwable error) {
                    planState = "error=" + error.getClass().getSimpleName();
                }
            });
            return "started";
        } catch (Throwable error) { return "start-error=" + error.getClass().getSimpleName(); }
    }

    public String backgroundPlanStatus() { return planState; }

    /** Main thread only: commits the finished background plan. */
    public String commitBackgroundPlan(Object player) {
        return commitBackgroundPlan(player, 8);
    }

    /** Main thread only: commits at most {@code maxOps} steps per call. */
    public String commitBackgroundPlan(Object player, int maxOps) {
        SitePlan plan = planResult;
        if (plan == null) return "not-ready";
        if (player == null) return "no-player";
        int budget = Math.max(1, Math.min(8, maxOps));
        try {
            Object cell = player.getClass().getMethod("getCell").invoke(player);
            int placed = 0;
            while (planIndex < plan.steps().size() && placed < budget) {
                var step = plan.steps().get(planIndex);
                try {
                    Object target = cell.getClass().getMethod("getGridSquare", int.class, int.class, int.class)
                            .invoke(cell, plan.site().x() + step.dx(), plan.site().y() + step.dy(), 0);
                    if (target != null) {
                        target.getClass().getMethod("AddWorldInventoryItem", String.class, float.class, float.class, float.class)
                                .invoke(target, "Base.Hammer", 0.5f, 0.5f, 0f);
                        placed++;
                    }
                } catch (Throwable ignored) { }
                planIndex++;
            }
            int remaining = Math.max(0, plan.steps().size() - planIndex);
            if (remaining == 0) {
                planResult = null;
                planIndex = 0;
                planState = "idle";
            }
            return "commit placed=" + placed + " remaining=" + remaining + " worldWrites=" + placed;
        } catch (Throwable error) { return "commit-error=" + error.getClass().getSimpleName(); }
    }

    public String patternDemo() { return MapPattern.demo(); }
    public String regionRegistryDemo() { return RegionRegistry.demo(); }
    public String structurePatternsDemo() { return StructurePatterns.demo(); }
    public String structureLandmarksDemo() { return StructurePatterns.landmarksDemo(); }

    public String structureVariantsDemo() {
        return "smallHouse=" + StructurePatterns.smallHouse().cells().size()
                + " largeWarehouse=" + StructurePatterns.largeWarehouse().cells().size()
                + " doubleGate=" + StructurePatterns.doubleGate().cells().size()
                + " longRoad=" + StructurePatterns.longRoad().cells().size();
    }

    public String patternOpsDemo() {
        var base = new MapPattern.Pattern(2, 2, java.util.List.of(
                new MapPattern.Cell(0, 0, "wall"), new MapPattern.Cell(1, 1, "door")));
        var parsed = MapPattern.parse(MapPattern.serialize(base));
        var bounds = MapPattern.bounds(base);
        return "parsed=" + (parsed != null) + " hash=" + MapPattern.hash(base)
                + " trim=" + MapPattern.trim(base).width() + " kinds=" + MapPattern.countKinds(base)
                + " bounds=" + bounds[0] + "," + bounds[1] + "," + bounds[2] + "," + bounds[3];
    }

    public String patternEngineDemo() {
        var base = new MapPattern.Pattern(2, 2, java.util.List.of(
                new MapPattern.Cell(0, 0, "wall"), new MapPattern.Cell(1, 1, "door")));
        var t = MapPattern.transpose(base);
        return "valid=" + MapPattern.valid(base) + " transpose=" + t.width() + "x" + t.height()
                + " rotate180=" + MapPattern.rotate180(base).cells().size()
                + " serialized=" + MapPattern.serialize(base).length();
    }

    public String patternLibraryDemo() { return PatternLibrary.demo(); }

    /** Generates one named region exactly once in this process. */
    public String generateStart(Object player) {
        if (player == null) return "no-player";
        if (!generated.add("start-site")) return "already-generated start-site";
        String camp = placeCamp(player);
        String road = placeRealRoad(player);
        String building = placeRealBuilding(player);
        String wall = placeRealWall(player);
        return "start camp=" + camp + " road=" + road + " building=" + building + " wall=" + wall;
    }

    public String generateOnce(Object player, String id) {
        if (id == null || id.isBlank()) return "no-id";
        if (!generated.add(id)) return "already-generated " + id;
        String camp = placeCamp(player);
        return "generated " + id + " " + camp;
    }

    public String generatedRegions() { return String.join(",", generated); }

    public void restoreGenerated(String csv) {
        if (csv == null || csv.isBlank()) return;
        for (String id : csv.split(",")) if (!id.isBlank()) generated.add(id.trim());
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

    private final ExecutorService planner = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "PersonalWorldGen-planner");
        thread.setDaemon(true);
        return thread;
    });
    private volatile String planState = "idle";
    private volatile SitePlan planResult;
    private volatile int planIndex = 0;
    private volatile String edgeInfo = "unknown";
    private final java.util.Set<String> generated = java.util.concurrent.ConcurrentHashMap.newKeySet();

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
