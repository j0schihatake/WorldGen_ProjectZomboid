package personal.worldgen;

public final class PlanFileTest {
    public static void run() {
        var plan = new SitePlan(new Site(10, 20, 8, 8), "0.1.0",
                java.util.List.of(new PlanStep("floor", 1, 2), new PlanStep("wall", 3, 4)));
        var text = PlanFile.encode(java.util.List.of(plan));
        var lines = text.split("\n");
        assert lines.length == 3;
        assert lines[1].equals("0.1.0\t11\tfloor\t11\t22\t0") : lines[1];
        assert lines[2].contains("wall\t13\t24\t0") : lines[2];
        try {
            var tmp = java.nio.file.Files.createTempFile("plan", ".tsv");
            PlanFile.write(tmp, java.util.List.of(plan));
            var commands = PlanFile.read(tmp);
            assert commands.size() == 2 && commands.get(0).x() == 11 && commands.get(0).y() == 22;
            java.nio.file.Files.deleteIfExists(tmp);
        } catch (java.io.IOException error) { throw new RuntimeException(error); }
        System.out.println("Plan-file tests passed: header and absolute coordinates.");
    }
}
