package personal.worldgen;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** TSV bridge between the planner and a future in-game executor. */
public final class PlanFile {
    private PlanFile() {}

    public static String encode(List<SitePlan> plans) {
        if (plans == null) throw new IllegalArgumentException("Missing plans");
        var out = new StringBuilder("#version\tsite\tkind\tx\ty\tz\n");
        for (var plan : plans) {
            for (var step : plan.steps()) {
                out.append(plan.version()).append('\t')
                        .append(plan.site().x() + step.dx()).append('\t')
                        .append(step.kind()).append('\t')
                        .append(plan.site().x() + step.dx()).append('\t')
                        .append(plan.site().y() + step.dy()).append('\t')
                        .append(0).append('\n');
            }
        }
        return out.toString();
    }

    public record Command(String version, String kind, int x, int y, int z) {
        public Command {
            if (version == null || version.isBlank() || kind == null || kind.isBlank())
                throw new IllegalArgumentException("Invalid command");
        }
    }

    public static List<Command> read(Path file) throws IOException {
        if (file == null) throw new IllegalArgumentException("Missing file");
        var result = new java.util.ArrayList<Command>();
        for (var line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#")) continue;
            var p = line.split("\t", -1);
            if (p.length != 6) throw new IllegalArgumentException("Invalid plan line");
            try {
                result.add(new Command(p[0], p[2], Integer.parseInt(p[3]), Integer.parseInt(p[4]), Integer.parseInt(p[5])));
            } catch (RuntimeException invalid) {
                throw new IllegalArgumentException("Invalid plan line", invalid);
            }
        }
        return List.copyOf(result);
    }

    public static void write(Path file, List<SitePlan> plans) throws IOException {
        if (file == null) throw new IllegalArgumentException("Missing file");
        Files.writeString(file, encode(plans), StandardCharsets.UTF_8);
    }
}
