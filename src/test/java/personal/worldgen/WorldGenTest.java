package personal.worldgen;

import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

public final class WorldGenTest {
    private static SitePlanner planner(ProtectedCells guard, long seed) {
        return new SitePlanner(guard, 64, 64, 4, 64, 0, 0, 1024, 1024, seed);
    }

    public static void main(String[] args) throws Exception {
        ProtectedCellsTest.main(args);

        try { new Site(0, 0, 0, 1); throw new AssertionError("Zero width accepted"); }
        catch (IllegalArgumentException expected) { }
        try { new Site(0, 0, 1, Site.MAX_SIZE + 1); throw new AssertionError("Oversized site accepted"); }
        catch (IllegalArgumentException expected) { }
        assert new Site(0, 0, 64, 64).area() == 4096;
        assert new Site(-100, -200, 1, 1).x() == -100;

        var free = new ProtectedCells(256, Set.of());
        var first = planner(free, 42).find(100);
        assert first.isPresent() : "Free profile must yield a site";
        var again = planner(free, 42).find(100);
        assert first.equals(again) : "Same seed must give the same site";
        assert first.get().width() == 64 && first.get().height() == 64;

        var guard = new ProtectedCells(256, Set.of(new ProtectedCells.Cell(0, 0)));
        var blocked = planner(guard, 0).find(100);
        assert blocked.isPresent() : "Planner must move past a protected cell";
        assert blocked.get().x() != 0 || blocked.get().y() != 0 : "Protected anchor must be skipped";
        assert guard.allows(blocked.get().x(), blocked.get().y(), 64, 64, 4) : "Returned site must be legal";

        var sequence = planner(free, 7);
        var one = sequence.find(100);
        assert one.isPresent();
        sequence.reserve(one.get());
        var two = sequence.find(100);
        assert two.isPresent() && !two.get().equals(one.get()) : "Reserved site must not repeat";
        assert sequence.reservedCount() == 1;
        assert sequence.find(0).isEmpty() : "Zero attempts is empty";

        var allBlocked = new ProtectedCells(16, Set.of(
                new ProtectedCells.Cell(0, 0), new ProtectedCells.Cell(1, 0),
                new ProtectedCells.Cell(2, 0), new ProtectedCells.Cell(3, 0)));
        var tiny = new SitePlanner(allBlocked, 16, 16, 0, 16, 0, 0, 48, 0, 1);
        assert tiny.find(10).isEmpty() : "No legal site must be reported";

        try { new SitePlanner(null, 1, 1, 0, 1, 0, 0, 0, 0, 0); throw new AssertionError("Null guard accepted"); }
        catch (IllegalArgumentException expected) { }
        try { new SitePlanner(free, 1, 1, 0, 0, 0, 0, 0, 0, 0); throw new AssertionError("Zero step accepted"); }
        catch (IllegalArgumentException expected) { }
        try { new SitePlanner(free, 1, 1, 0, 1, 10, 0, 0, 0, 0); throw new AssertionError("Inverted bounds accepted"); }
        catch (IllegalArgumentException expected) { }
        try { new SitePlanner(free, 1, 1, 0, 1, Integer.MIN_VALUE, 0, Integer.MAX_VALUE, 0, 0); throw new AssertionError("Too many candidates accepted"); }
        catch (IllegalArgumentException expected) { }

        var queue = new GenerationQueue();
        var log = new StringBuilder();
        for (int i = 0; i < 3; i++) { int n = i; queue.add(() -> log.append(n)); }
        assert queue.run(2) == 2 && queue.size() == 1;
        assert log.toString().equals("01") : "Operations keep insertion order";
        queue.pause();
        assert queue.run(5) == 0 && queue.size() == 1 : "Paused queue must not run";
        queue.resume();
        assert queue.run(5) == 1 && queue.size() == 0;
        assert log.toString().equals("012");
        try { queue.run(-1); throw new AssertionError("Negative budget accepted"); }
        catch (IllegalArgumentException expected) { }
        try { queue.add(null); throw new AssertionError("Null operation accepted"); }
        catch (IllegalArgumentException expected) { }

        var profile = new MapProfile("42.21.0", Set.of(
                new ProtectedCells.Cell(1, 2), new ProtectedCells.Cell(-3, 4)));
        assert profile.cellCount() == 2;
        var profileFile = Files.createTempFile("wgen-profile", ".tsv");
        MapProfileIO.write(profileFile, profile);
        var loaded = MapProfileIO.read(profileFile);
        assert loaded.equals(profile) : "Profile must survive a round-trip";
        var imported = new ProtectedCells(256, loaded);
        assert !imported.allows(256, 512, 1, 1, 0) : "Imported cell must protect its whole cell";
        assert imported.allows(0, 0, 1, 1, 0) : "Unlisted cell stays free";
        Files.writeString(profileFile, "version\t42.21.0\ncell\tx\ty\n");
        try { MapProfileIO.read(profileFile); throw new AssertionError("Bad cell accepted"); }
        catch (java.io.IOException expected) { }
        try { new MapProfile("", Set.of()); throw new AssertionError("Empty version accepted"); }
        catch (IllegalArgumentException expected) { }
        try { new MapProfile("42.21.0", null); throw new AssertionError("Null cells accepted"); }
        catch (IllegalArgumentException expected) { }

        var stepA = new PlanStep("wall", 0, 0);
        var stepB = new PlanStep("floor", 1, 1);
        var plan = new SitePlan(new Site(0, 0, 64, 64), "42.21.0", java.util.List.of(stepA, stepB));
        assert plan.operations() == 2 : "Plan must count its steps";
        var mutable = new java.util.ArrayList<>(java.util.List.of(stepA));
        var copied = new SitePlan(new Site(0, 0, 64, 64), "42.21.0", mutable);
        mutable.clear();
        assert copied.operations() == 1 : "Plan must copy its steps";
        try { new PlanStep("", 0, 0); throw new AssertionError("Empty step kind accepted"); }
        catch (IllegalArgumentException expected) { }
        try { new PlanStep("wall", Site.MAX_SIZE + 1, 0); throw new AssertionError("Oversized step accepted"); }
        catch (IllegalArgumentException expected) { }
        try { new SitePlan(null, "v", java.util.List.of(stepA)); throw new AssertionError("Null site accepted"); }
        catch (IllegalArgumentException expected) { }
        try { new SitePlan(new Site(0, 0, 1, 1), "v", java.util.List.of()); throw new AssertionError("Empty plan accepted"); }
        catch (IllegalArgumentException expected) { }
        try { new SitePlan(new Site(0, 0, 1, 1), "", java.util.List.of(stepA)); throw new AssertionError("Empty version accepted"); }
        catch (IllegalArgumentException expected) { }

        var planned = new GenerationQueue();
        var order = new StringBuilder();
        planned.addPlan(plan, step -> order.append(step.kind()));
        assert planned.pendingOps() == 2;
        assert planned.run(1, System.nanoTime() + 1_000_000_000L) == 1;
        assert order.toString().equals("wall") : "Plan steps run in order";
        assert planned.run(10, System.nanoTime() - 1_000_000L) == 0 : "Past deadline stops work";
        assert planned.run(10, System.nanoTime() + 1_000_000_000L) == 1;
        assert order.toString().equals("wallfloor");
        planned.addPlan(plan, step -> order.append("x"));
        planned.clear();
        assert planned.pendingOps() == 0 : "Clear must drop pending work";
        try { planned.addPlan(null, step -> { }); throw new AssertionError("Null plan accepted"); }
        catch (IllegalArgumentException expected) { }
        try { planned.addPlan(plan, null); throw new AssertionError("Null executor accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("World-generation tests passed: site bounds, deterministic search, protected cells, reservations, budget queue, pause, map profiles, time budget and plan commit.");
    }
}
