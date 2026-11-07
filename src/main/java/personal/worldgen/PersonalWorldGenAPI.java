package personal.worldgen;

/** Read-only game-facing entry point. Plans data; never writes to the world yet. */
public final class PersonalWorldGenAPI {
    public static final String VERSION = "0.1.0";
    private static final PersonalWorldGenAPI INSTANCE = new PersonalWorldGenAPI();

    private PersonalWorldGenAPI() {}

    public static PersonalWorldGenAPI getInstance() { return INSTANCE; }

    public String status() {
        return "core=" + VERSION + " worldWrites=0 mode=planning";
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
