package personal.worldgen;

/** Read-only game-facing entry point. Plans data; never writes to the world yet. */
@me.zed_0xff.zombie_buddy.Exposer.LuaClass
public final class PersonalWorldGenAPI {
    public static final String VERSION = "0.4.0";
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
