package personal.worldgen.db;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/** Per-save SQLite store for generated regions, plans and write records. */
public final class WorldDatabase {
    private static volatile WorldDatabase instance;
    private static volatile String activeSave = "";
    private volatile Path path;
    private Connection connection;
    private final java.util.Map<String, String> settingsCache = new java.util.concurrent.ConcurrentHashMap<>();

    public WorldDatabase(Path path) { this.path = path; }

    public static WorldDatabase getInstance() {
        var save = currentSave();
        var local = instance;
        if (local == null || !save.equals(activeSave)) synchronized (WorldDatabase.class) {
            if (instance == null || !save.equals(activeSave)) {
                if (instance != null) instance.close();
                instance = new WorldDatabase(pathFor(save));
                activeSave = save;
            }
            local = instance;
        }
        return local;
    }

    private static String currentSave() {
        try {
            var core = Class.forName("zombie.core.Core").getMethod("getInstance").invoke(null);
            var folder = String.valueOf(core.getClass().getMethod("getSaveFolder").invoke(core));
            return folder == null || folder.isBlank() || "null".equals(folder) ? "" : folder;
        } catch (Throwable ignored) { return ""; }
    }

    private static Path pathFor(String save) {
        if (save != null && !save.isBlank()) return Path.of(save, "PersonalWorldGen.db");
        try {
            var core = Class.forName("zombie.core.Core");
            return Path.of(String.valueOf(core.getMethod("getLuaCacheDir").invoke(null)), "PersonalWorldGen.db");
        } catch (Throwable ignored) {
            return Path.of(System.getProperty("user.home"), "Zomboid", "Lua", "PersonalWorldGen.db");
        }
    }

    private synchronized Connection connection() throws Exception {
        if (connection != null && !connection.isClosed()) return connection;
        Files.createDirectories(path.getParent());
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:" + path.toAbsolutePath());
        runResource("schema.sql");
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM settings")) {
            if (!rows.next() || rows.getLong(1) == 0) runResource("seed.sql");
        }
        return connection;
    }

    private void runResource(String name) throws Exception {
        try (InputStream input = WorldDatabase.class.getResourceAsStream("/personal/worldgen/db/" + name)) {
            if (input == null) return;
            String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            try (Statement statement = connection.createStatement()) {
                for (String part : sql.split(";")) if (!part.isBlank()) statement.execute(part);
            }
        }
    }

    public synchronized String getSetting(String key, String fallback) {
        if (key == null || key.isBlank()) return fallback;
        try (PreparedStatement s = connection().prepareStatement("SELECT value FROM settings WHERE key=?")) {
            s.setString(1, key);
            try (ResultSet rows = s.executeQuery()) { return rows.next() ? rows.getString(1) : fallback; }
        } catch (Exception ignored) { return fallback; }
    }

    public synchronized void setSetting(String key, String value) {
        if (key == null || key.isBlank() || value == null) return;
        try (PreparedStatement s = connection().prepareStatement(
                "INSERT OR REPLACE INTO settings(key,value) VALUES(?,?)")) {
            s.setString(1, key); s.setString(2, value); s.executeUpdate();
            settingsCache.put(key, value);
        } catch (Exception ignored) { }
    }

    public synchronized String schemaVersion() {
        try (Statement s = connection().createStatement();
             ResultSet rows = s.executeQuery("SELECT value FROM meta WHERE key='schema_version'")) {
            return rows.next() ? rows.getString(1) : "0";
        } catch (Exception ignored) { return "0"; }
    }

    public synchronized void put(String table, String id, String data) {
        if (id == null || data == null) return;
        try (PreparedStatement s = connection().prepareStatement(
                "INSERT OR REPLACE INTO " + table + "(id,data) VALUES(?,?)")) {
            s.setString(1, id); s.setString(2, data); s.executeUpdate();
        } catch (Exception ignored) { }
    }

    public synchronized String get(String table, String id, String fallback) {
        if (id == null) return fallback;
        try (PreparedStatement s = connection().prepareStatement("SELECT data FROM " + table + " WHERE id=?")) {
            s.setString(1, id);
            try (ResultSet rows = s.executeQuery()) { return rows.next() ? rows.getString(1) : fallback; }
        } catch (Exception ignored) { return fallback; }
    }

    public synchronized void recordWrite(String id, String kind, int x, int y, int z) {
        try (PreparedStatement s = connection().prepareStatement(
                "INSERT OR IGNORE INTO writes(id,kind,x,y,z) VALUES(?,?,?,?,?)")) {
            s.setString(1, id); s.setString(2, kind); s.setInt(3, x); s.setInt(4, y); s.setInt(5, z);
            s.executeUpdate();
        } catch (Exception ignored) { }
    }

    public synchronized java.util.List<String> planIds() {
        var result = new java.util.ArrayList<String>();
        try (Statement s = connection().createStatement();
             ResultSet rows = s.executeQuery("SELECT id FROM plans ORDER BY id")) {
            while (rows.next()) result.add(rows.getString(1));
        } catch (Exception ignored) { }
        return result;
    }

    public synchronized java.util.List<String> regionIds() {
        var result = new java.util.ArrayList<String>();
        try (Statement s = connection().createStatement();
             ResultSet rows = s.executeQuery("SELECT id FROM regions ORDER BY id")) {
            while (rows.next()) result.add(rows.getString(1));
        } catch (Exception ignored) { }
        return result;
    }

    public synchronized int regionCount() {
        try (Statement s = connection().createStatement(); ResultSet rows = s.executeQuery("SELECT COUNT(*) FROM regions")) {
            return rows.next() ? rows.getInt(1) : 0;
        } catch (Exception ignored) { return 0; }
    }

    public synchronized int planCount() {
        try (Statement s = connection().createStatement(); ResultSet rows = s.executeQuery("SELECT COUNT(*) FROM plans")) {
            return rows.next() ? rows.getInt(1) : 0;
        } catch (Exception ignored) { return 0; }
    }

    public synchronized int writeCount() {
        try (Statement s = connection().createStatement(); ResultSet rows = s.executeQuery("SELECT COUNT(*) FROM writes")) {
            return rows.next() ? rows.getInt(1) : 0;
        } catch (Exception ignored) { return 0; }
    }

    public synchronized void close() { try { if (connection != null) connection.close(); } catch (Exception ignored) { } connection = null; }
    public synchronized boolean backupNow() {
        return backup(path.resolveSibling(path.getFileName() + ".bak"));
    }

    public synchronized void useSave(String folder) {
        if (folder == null || folder.isBlank()) return;
        var target = Path.of(folder).resolve("PersonalWorldGen.db");
        if (!target.isAbsolute() || target.equals(path)) return;
        close();
        path = target;
        activeSave = folder;
        settingsCache.clear();
        System.out.println("[PersonalWorldGen][db] switch " + target);
    }

    public synchronized boolean backup(Path target) {
        if (target == null) return false;
        try {
            Files.createDirectories(target.getParent());
            Files.copy(path, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (Exception ignored) { return false; }
    }

    public Path path() { return path; }
}
