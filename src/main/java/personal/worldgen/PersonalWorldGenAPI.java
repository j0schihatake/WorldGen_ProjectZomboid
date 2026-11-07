package personal.worldgen;

/** Read-only game-facing entry point. Plans data; never writes to the world yet. */
public final class PersonalWorldGenAPI {
    public static final String VERSION = "0.3.1";
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
    private MapProfile profile() throws java.io.IOException {
        try (var input = PersonalWorldGenAPI.class.getResourceAsStream("/personal/worldgen/profile.tsv")) {
            return MapProfileIO.read(input);
        }
    }

    public String planNearPlayer(Object player, long seed) {
        if (player == null) return "no-player";
        try {
            double px = (double) player.getClass().getMethod("getX").invoke(player);
            double py = (double) player.getClass().getMethod("getY").invoke(player);
            int x = (int) px + 10;
            int y = (int) py + 10;
            var guard = new ProtectedCells(256, profile());
            if (!guard.allows(x, y, 16, 16, 0)) {
                return "x=" + x + " y=" + y + " protected=true worldWrites=0";
            }
            var plan = CampPlanner.plan(new Site(x, y, 16, 16), VERSION, seed, 8);
            return "x=" + x + " y=" + y + " campOps=" + plan.operations() + " protected=false worldWrites=0";
        } catch (ReflectiveOperationException | java.io.IOException error) {
            return "player-probe-failed=" + error.getClass().getSimpleName();
        }
    }

    public String planDemo(long seed) {
        var settlement = SettlementPlanner.plan(
                new Site(0, 0, 16, 16),
                new Site(32, 0, 12, 12),
                new Site(64, 0, 16, 16),
                VERSION, seed);
        return "campOps=" + settlement.camp().operations()
                + " buildingOps=" + settlement.building().steps().size()
                + " roadCells=" + settlement.road().size()
                + " worldWrites=0";
    }
}
