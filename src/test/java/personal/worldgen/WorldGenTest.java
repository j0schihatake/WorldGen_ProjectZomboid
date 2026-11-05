package personal.worldgen;

import java.util.HashSet;
import java.util.Set;

public final class WorldGenTest {
    private static SitePlanner planner(ProtectedCells guard, long seed) {
        return new SitePlanner(guard, 64, 64, 4, 64, 0, 0, 1024, 1024, seed);
    }

    public static void main(String[] args) {
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
        System.out.println("World-generation tests passed: site bounds, deterministic search, protected cells, reservations, budget queue and pause.");
    }
}
