package personal.worldgen;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/** Strict, dependency-free text profile; a bad line fails the whole profile. */
public final class MapProfileIO {
    private MapProfileIO() {}

    public static MapProfile read(Path path) throws IOException {
        if (path == null) throw new IllegalArgumentException("Missing profile path");
        String version = null;
        var cells = new HashSet<ProtectedCells.Cell>();
        for (String raw : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] parts = line.split("\t", -1);
            if (parts.length == 2 && parts[0].equals("version")) {
                if (version != null) throw new IOException("Duplicate version line");
                version = parts[1];
            } else if (parts.length == 3 && parts[0].equals("cell")) {
                try {
                    cells.add(new ProtectedCells.Cell(Integer.parseInt(parts[1]), Integer.parseInt(parts[2])));
                } catch (NumberFormatException error) {
                    throw new IOException("Invalid cell coordinates: " + line, error);
                }
            } else {
                throw new IOException("Invalid profile line: " + line);
            }
        }
        if (version == null) throw new IOException("Missing version line");
        return new MapProfile(version, cells);
    }

    public static void write(Path path, MapProfile profile) throws IOException {
        if (path == null || profile == null) throw new IllegalArgumentException("Missing profile");
        List<ProtectedCells.Cell> cells = new ArrayList<>(profile.cells());
        cells.sort((left, right) -> left.x() != right.x()
                ? Integer.compare(left.x(), right.x()) : Integer.compare(left.y(), right.y()));
        var lines = new ArrayList<String>();
        lines.add("# PersonalWorldGen map profile");
        lines.add("version\t" + profile.gameVersion());
        for (var cell : cells) lines.add("cell\t" + cell.x() + "\t" + cell.y());
        Files.write(path, lines, StandardCharsets.UTF_8);
    }
}
