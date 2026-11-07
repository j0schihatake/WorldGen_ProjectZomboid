package personal.worldgen;

/** Read-only game-facing entry point. Plans data; never writes to the world yet. */
public final class PersonalWorldGenAPI {
    public static final String VERSION = "0.2.0";
    private static final PersonalWorldGenAPI INSTANCE = new PersonalWorldGenAPI();

    private PersonalWorldGenAPI() {}

    public static PersonalWorldGenAPI getInstance() { return INSTANCE; }

    public String status() {
        return "core=" + VERSION + " worldWrites=0 mode=planning";
    }

    /** Plans a camp near the player using only the position; no world writes. */
    public String planNearPlayer(Object player, long seed) {
        if (player == null) return "no-player";
        try {
            double px = (double) player.getClass().getMethod("getX").invoke(player);
            double py = (double) player.getClass().getMethod("getY").invoke(player);
            int x = (int) px + 10;
            int y = (int) py + 10;
            var plan = CampPlanner.plan(new Site(x, y, 16, 16), VERSION, seed, 8);
            return "x=" + x + " y=" + y + " campOps=" + plan.operations() + " worldWrites=0";
        } catch (ReflectiveOperationException error) {
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
