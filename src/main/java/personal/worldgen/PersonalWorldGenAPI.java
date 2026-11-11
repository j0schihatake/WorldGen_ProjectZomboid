package personal.worldgen;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Read-only game-facing entry point. Plans data; never writes to the world yet. */
@me.zed_0xff.zombie_buddy.Exposer.LuaClass
public final class PersonalWorldGenAPI {
    public static final String VERSION = "1.18.6";
    private static final PersonalWorldGenAPI INSTANCE = new PersonalWorldGenAPI();

    private volatile boolean writesEnabled = false;
    private final WriteBudget writeBudget = new WriteBudget(256);
    private final personal.worldgen.db.WorldDatabase worldDb = personal.worldgen.db.WorldDatabase.getInstance();
    private volatile boolean batchWrite = false;
    private volatile int protectedPadding = 0;
    private volatile long worldSeed = 42L;

    private PersonalWorldGenAPI() {}

    public static PersonalWorldGenAPI getInstance() { return INSTANCE; }

    public String status() {
        try (var input = PersonalWorldGenAPI.class.getResourceAsStream("/personal/worldgen/profile.tsv")) {
            var profile = MapProfileIO.read(input);
            return "core=" + VERSION + " profileCells=" + profile.cells().size()
                    + " profileVersion=" + profile.gameVersion()
                    + " writes=" + (writesEnabled ? "on" : "off")
                    + " writeBudget=" + writeBudget.limit() + " writeBudgetUsed=" + writeBudget.used() + " worldDbWrites=" + worldDb.writeCount()
                    + " protectedPadding=" + protectedPadding + " seed=" + worldSeed
                    + " worldWrites=0 mode=planning";
        } catch (Exception error) {
            return "core=" + VERSION + " profile=missing writes=off worldWrites=0 mode=planning";
        }
    }

    public String worldDbPutPlan(String id, String data) {
        worldDb.put("plans", id, data);
        return "plan=" + id;
    }

    public String worldDbGetPlan(String id) { return worldDb.get("plans", id, ""); }

    public String worldDbPutRegion(String id, String data) {
        worldDb.put("regions", id, data);
        return "region=" + id;
    }

    public String worldDbGetRegion(String id) { return worldDb.get("regions", id, ""); }

    public String worldDbStatus() {
        return "writes=" + worldDb.writeCount() + " path=" + worldDb.path();
    }

    public String worldDbSetting(String key, String fallback) { return worldDb.getSetting(key, fallback); }
    public void worldDbSetSetting(String key, String value) { worldDb.setSetting(key, value); }

    public String worldgenSettings() {
        return "writes=" + (writesEnabled ? "on" : "off") + " writeBudget=" + writeBudget.limit()
                + " used=" + writeBudget.used() + " remaining=" + writeBudget.remaining()
                + " protectedPadding=" + protectedPadding + " seed=" + worldSeed;
    }

    public void setWritesEnabled(boolean enabled) { writesEnabled = enabled; }

    public void setWriteBudget(int value) { writeBudget.setLimit(value); }
    public void resetWriteBudget() { writeBudget.reset(); }
    public void setProtectedPadding(int value) { protectedPadding = Math.max(0, Math.min(16, value)); }
    public void setWorldSeed(long value) { worldSeed = value; }

    private String writesDisabled() { return "writes-disabled writes=off worldWrites=0"; }

    /** Plans a camp near the player using only the position; no world writes. */
    /** First controlled write: one world item on the player's square. */
    public String placeMarker(Object player) {
        if (!writesEnabled) return writesDisabled();
        if (!batchWrite) writeBudget.reset();
        if (player == null) return "no-player";
        try {
            Object square = player.getClass().getMethod("getCurrentSquare").invoke(player);
            if (square == null) return "no-square";
            if (!writeBudget.take()) return "marker-budget worldWrites=0";
            Object item = square.getClass().getMethod("AddWorldInventoryItem",
                            String.class, float.class, float.class, float.class)
                    .invoke(square, "Base.Hammer", 0.5f, 0.5f, 0f);
            return item != null ? "marker=placed worldWrites=1 used=" + writeBudget.used()
                    : "marker=failed worldWrites=0";
        } catch (Throwable error) {
            return "marker-error=" + error.getClass().getSimpleName() + " worldWrites=0";
        }
    }

    /** Places a bounded camp plan into the test world as item markers. */
    public String placeCamp(Object player) {
        if (!writesEnabled) return writesDisabled();
        if (!batchWrite) writeBudget.reset();
        if (player == null) return "no-player";
        try {
            Object square = player.getClass().getMethod("getCurrentSquare").invoke(player);
            if (square == null) return "no-square";
            var plan = CampPlanner.plan(new Site(0, 0, 32, 32), VERSION, 42, 8);
            Object cell = player.getClass().getMethod("getCell").invoke(player);
            int bx = ((Number) player.getClass().getMethod("getXi").invoke(player)).intValue();
            int by = ((Number) player.getClass().getMethod("getYi").invoke(player)).intValue();
            int bz = ((Number) player.getClass().getMethod("getZi").invoke(player)).intValue();
            int placed = 0;
            for (var step : plan.steps()) {
                if (!writeBudget.take()) break;
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
                    worldDb.recordWrite("camp", step.kind(), bx + step.dx(), by + step.dy(), bz);
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
        if (!writesEnabled) return writesDisabled();
        if (!batchWrite) writeBudget.reset();
        if (player == null) return "no-player";
        try {
            var points = RoadPlanner.connect(new Site(0, 0, 32, 32), new Site(64, 0, 32, 32));
            Object cell = player.getClass().getMethod("getCell").invoke(player);
            int bx = ((Number) player.getClass().getMethod("getXi").invoke(player)).intValue();
            int by = ((Number) player.getClass().getMethod("getYi").invoke(player)).intValue();
            int bz = ((Number) player.getClass().getMethod("getZi").invoke(player)).intValue();
            int placed = 0;
            for (int i = 0; i < points.size(); i += 4) {
                if (!writeBudget.take()) break;
                var point = points.get(i);
                try {
                    Object target = cell.getClass().getMethod("getGridSquare", int.class, int.class, int.class)
                            .invoke(cell, bx + point.x(), by + point.y(), bz);
                    if (target == null) continue;
                    target.getClass().getMethod("AddWorldInventoryItem", String.class, float.class, float.class, float.class)
                            .invoke(target, "Base.Hammer", 0.5f, 0.5f, 0f);
                    worldDb.recordWrite("road", "road", bx + point.x(), by + point.y(), bz);
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
        if (!writesEnabled) return writesDisabled();
        if (!batchWrite) writeBudget.reset();
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
                if (!writeBudget.take()) break;
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
                worldDb.recordWrite("building", step.kind(), bx + step.dx(), by + step.dy(), bz);
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
        if (!writesEnabled) return writesDisabled();
        if (!batchWrite) writeBudget.reset();
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
                if (!writeBudget.take()) break;
                var point = points.get(i);
                Object target = cell.getClass().getMethod("getGridSquare", int.class, int.class, int.class)
                        .invoke(cell, bx + point.x(), by + point.y(), bz);
                if (target == null) continue;
                Object object = objectClass.getConstructor(squareClass, String.class)
                        .newInstance(target, "floors_exterior_street_01_0");
                object.getClass().getMethod("addToWorld").invoke(object);
                worldDb.recordWrite("road", "road", bx + point.x(), by + point.y(), bz);
                placed++;
            }
            return "realRoad=" + placed + " worldWrites=" + placed;
        } catch (Throwable error) { return "road-error=" + error.getClass().getSimpleName() + " worldWrites=0"; }
    }

    /** Places real camp props from the camp plan. */
    public String placeRealCamp(Object player) {
        if (!writesEnabled) return writesDisabled();
        if (!batchWrite) writeBudget.reset();
        if (player == null) return "no-player";
        try {
            var plan = CampPlanner.plan(new Site(0, 0, 32, 32), VERSION, 42, 8);
            Object cell = player.getClass().getMethod("getCell").invoke(player);
            Class<?> squareClass = Class.forName("zombie.iso.IsoGridSquare");
            Class<?> objectClass = Class.forName("zombie.iso.IsoObject");
            int bx = ((Number) player.getClass().getMethod("getXi").invoke(player)).intValue();
            int by = ((Number) player.getClass().getMethod("getYi").invoke(player)).intValue();
            int bz = ((Number) player.getClass().getMethod("getZi").invoke(player)).intValue();
            int placed = 0;
            for (var step : plan.steps()) {
                if (!writeBudget.take()) break;
                String sprite = switch (step.kind()) {
                    case "tent" -> "tent_01_0";
                    case "campfire" -> "camping_01_0";
                    case "bedroll" -> "camping_01_2";
                    default -> "camping_01_1";
                };
                Object target = cell.getClass().getMethod("getGridSquare", int.class, int.class, int.class)
                        .invoke(cell, bx + step.dx(), by + step.dy(), bz);
                if (target == null) continue;
                Object object = objectClass.getConstructor(squareClass, String.class)
                        .newInstance(target, sprite);
                object.getClass().getMethod("addToWorld").invoke(object);
                worldDb.recordWrite("camp", step.kind(), bx + step.dx(), by + step.dy(), bz);
                placed++;
            }
            return "realCamp=" + placed + " worldWrites=" + placed;
        } catch (Throwable error) { return "camp-error=" + error.getClass().getSimpleName() + " worldWrites=0"; }
    }

    /** Places a bounded building marker set into the test world. */
    public String placeBuilding(Object player) {
        if (!writesEnabled) return writesDisabled();
        if (!batchWrite) writeBudget.reset();
        if (player == null) return "no-player";
        try {
            var plan = BuildingPlanner.plan(new Site(0, 0, 48, 40), VERSION, 42, 3);
            Object cell = player.getClass().getMethod("getCell").invoke(player);
            int bx = ((Number) player.getClass().getMethod("getXi").invoke(player)).intValue();
            int by = ((Number) player.getClass().getMethod("getYi").invoke(player)).intValue();
            int bz = ((Number) player.getClass().getMethod("getZi").invoke(player)).intValue();
            int placed = 0;
            for (var step : plan.steps()) {
                if (!writeBudget.take()) break;
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

    public String structureCatalogDemo() { return StructurePatterns.catalogDemo(); }

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
        if (!writesEnabled) return writesDisabled();
        if (player == null) return "no-player";
        try {
            int px = ((Number) player.getClass().getMethod("getXi").invoke(player)).intValue();
            int py = ((Number) player.getClass().getMethod("getYi").invoke(player)).intValue();
            var guard = new ProtectedCells(256, profile());
            if (!guard.allows(px, py, 32, 32, protectedPadding)) return "protected-start worldWrites=0";
        } catch (ReflectiveOperationException | java.io.IOException error) {
            return "start-guard-error=" + error.getClass().getSimpleName() + " worldWrites=0";
        }
        synchronized (generated) {
            if (!generated.remember("start-site")) return "already-generated start-site";
        }
        writeBudget.reset();
        batchWrite = true;
        try {
            String camp = placeRealCamp(player);
            String road = placeRealRoad(player);
            String building = placeRealBuilding(player);
            String wall = placeRealWall(player);
            String result = "start camp=" + camp + " road=" + road + " building=" + building + " wall=" + wall
                    + " budget=" + writeBudget.limit() + " used=" + writeBudget.used()
                    + " remaining=" + writeBudget.remaining();
            worldDb.put("plans", "start-site", result);
            worldDb.put("regions", "registry", generated.csv());
            return result;
        } finally {
            batchWrite = false;
        }
    }

    public String generateOnce(Object player, String id) {
        if (!writesEnabled) return writesDisabled();
        if (id == null || id.isBlank()) return "no-id";
        synchronized (generated) {
            if (!generated.remember(id)) return "already-generated " + id;
        }
        writeBudget.reset();
        batchWrite = true;
        try {
            String camp = placeCamp(player);
            String result = "generated " + id + " " + camp
                    + " budget=" + writeBudget.limit() + " used=" + writeBudget.used();
            worldDb.put("plans", id, result);
            return result;
        } finally {
            batchWrite = false;
        }
    }

    public String worldDbLoadRegions() {
        var csv = worldDb.get("regions", "registry", "");
        if (!csv.isBlank()) restoreGenerated(csv);
        return "regions=" + generated.csv().split(",").length + " loaded=" + (csv.isBlank() ? 0 : 1);
    }

    public String generatedRegions() {
        synchronized (generated) { return generated.csv(); }
    }

    public void restoreGenerated(String csv) {
        synchronized (generated) { generated.restoreCsv(csv); }
    }

    /** One bounded test-site pipeline: camp, road and building markers. */
    public String generateTestSite(Object player) {
        if (!writesEnabled) return writesDisabled();
        writeBudget.reset();
        batchWrite = true;
        try {
            String camp = placeCamp(player);
            String road = placeRoad(player);
            String building = placeBuilding(player);
            int campCount = countPlaced(camp);
            int roadCount = countPlaced(road);
            int buildingCount = countPlaced(building);
            return "camp=" + campCount + " road=" + roadCount + " building=" + buildingCount
                    + " worldWrites=" + (campCount + roadCount + buildingCount)
                    + " budget=" + writeBudget.limit() + " used=" + writeBudget.used();
        } finally {
            batchWrite = false;
        }
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
        if (!writesEnabled) return writesDisabled();
        if (!batchWrite) writeBudget.reset();
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
                if (!writeBudget.take()) break;
                Object target = cell.getClass().getMethod("getGridSquare", int.class, int.class, int.class)
                        .invoke(cell, bx + 3 + i, by + 3, bz);
                if (target == null) continue;
                Object object = objectClass.getConstructor(squareClass, String.class)
                        .newInstance(target, "walls_exterior_house_01_0");
                object.getClass().getMethod("addToWorld").invoke(object);
                worldDb.recordWrite("wall", "wall", bx + 3 + i, by + 3, bz);
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
    private static final int REGION_LIMIT = 256;
    private final RegionRegistry generated = new RegionRegistry(REGION_LIMIT);

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
            var guard = new ProtectedCells(256, profile());
            return planNear(px, py, seed, guard);
        } catch (ReflectiveOperationException | java.io.IOException error) {
            return "player-probe-failed=" + error.getClass().getSimpleName();
        }
    }

    /** Pure settlement probe; the caller supplies the protected-cell guard. */
    public String planNear(double px, double py, long seed, ProtectedCells guard) {
        if (guard == null || !Double.isFinite(px + py)) return "invalid-input";
        int x = (int) px + 10;
        int y = (int) py + 10;
        var campSite = new Site(x, y, 32, 32);
        var buildingSite = new Site(x + 40, y, 48, 40);
        var roadTarget = new Site(x + 120, y, 16, 16);
        if (!guard.allows(campSite.x(), campSite.y(), campSite.width(), campSite.height(), 0)
                || !guard.allows(buildingSite.x(), buildingSite.y(), buildingSite.width(), buildingSite.height(), 0)) {
            return "x=" + x + " y=" + y + " protected=true worldWrites=0";
        }
        SettlementPlanner.Settlement settlement;
        try {
            settlement = SettlementPlanner.planValidated(campSite, buildingSite, roadTarget,
                    VERSION, seed, guard, 0);
        } catch (IllegalArgumentException protectedPlan) {
            return "x=" + x + " y=" + y + " protected=true worldWrites=0";
        }
        return "x=" + x + " y=" + y + " campOps=" + settlement.camp().operations()
                + " buildingOps=" + settlement.building().steps().size()
                + " roadPoints=" + settlement.road().size()
                + " protected=false worldWrites=0";
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
