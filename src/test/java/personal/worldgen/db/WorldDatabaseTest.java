package personal.worldgen.db;

import java.nio.file.Files;

public final class WorldDatabaseTest {
    public static void run() throws Exception {
        var db = new WorldDatabase(Files.createTempDirectory("pwg-db").resolve("world.db"));
        assert "1".equals(db.schemaVersion());
        assert "false".equals(db.getSetting("writesEnabled","on"));
        db.setSetting("writes","off");
        assert "off".equals(db.getSetting("writes","on"));
        db.put("plans", "site#1", "camp=8");
        db.put("plans", "site#1", "camp=9");
        assert "camp=9".equals(db.get("plans", "site#1", ""));
        db.put("regions", "region#1", "roads=77");
        assert "roads=77".equals(db.get("regions", "region#1", ""));
        db.recordWrite("site#1", "camp", 10, 20, 0);
        db.recordWrite("site#1", "camp", 10, 20, 0);
        assert db.writeCount() == 1;
        assert db.planCount() == 1;
        assert db.planIds().contains("site#1");
        assert db.regionCount() == 1;
        assert db.backup(Files.createTempDirectory("pwg-backup").resolve("world.db"));
        System.out.println("World database tests passed: plans upsert, regions and deduplicated writes.");
    }
}
