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

    public static void write(Path file, List<SitePlan> plans) throws IOException {
        if (file == null) throw new IllegalArgumentException("Missing file");
        Files.writeString(file, encode(plans), StandardCharsets.UTF_8);
    }
}
