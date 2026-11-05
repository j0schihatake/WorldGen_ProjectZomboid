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
        var sessionProfile = new MapProfile("42.21.0", Set.of());
        var sessionPlanner = new SitePlanner(new ProtectedCells(256, sessionProfile), 32, 32, 0, 64, 0, 0, 512, 512, 3);
        var session = new WorldGenSession(sessionProfile, sessionPlanner, "0.1.0");
        var plannedSite = session.plan(100);
        assert plannedSite != null : "Session must plan a legal site";
        assert session.status().plannedSites() == 1 && session.status().lastSite().equals(plannedSite);
        var secondSite = session.plan(100);
        assert secondSite != null && !secondSite.equals(plannedSite) : "Session must reserve planned sites";
        var sessionPlan = new SitePlan(new Site(0, 0, 32, 32), "0.1.0", java.util.List.of(new PlanStep("camp", 10, 10)));
        var sessionLog = new StringBuilder();
        session.enqueue(sessionPlan, step -> sessionLog.append(step.kind()));
        assert session.tick(10, System.nanoTime() + 1_000_000_000L) == 1;
        assert sessionLog.toString().equals("camp");
        var invalidPlan = new SitePlan(new Site(0, 0, 32, 32), "0.1.0", java.util.List.of(new PlanStep("bad", 500, 500)));
        try { session.enqueue(invalidPlan, step -> { }); throw new AssertionError("Invalid plan enqueued"); }
        catch (IllegalArgumentException expected) { }
        var stats = session.stats();
        assert stats.completed() > 0 && stats.pending() == 0 : "Session stats";
        var settlementSession = new WorldGenSession(sessionProfile,
                new SitePlanner(new ProtectedCells(256, sessionProfile), 32, 32, 0, 64, 0, 0, 512, 512, 3),
                "0.1.0");
        var queued = settlementSession.planSettlement(new Site(0, 0, 64, 64),
                new Site(80, 0, 64, 64), new Site(180, 100, 32, 32), 7, step -> { });
        assert settlementSession.stats().pending() > 0;
        assert queued.camp().operations() > 0;
        var districtGuard = new ProtectedCells(256, java.util.Set.of());
        var district = SettlementPlanner.planDistrict(
                java.util.List.of(new Site(0, 0, 64, 64), new Site(300, 0, 64, 64)),
                java.util.List.of(new Site(80, 0, 64, 64), new Site(380, 0, 64, 64)),
                new Site(500, 100, 32, 32), "0.1.0", 11, districtGuard, 0);
        assert district.settlements().size() == 2;
        try { SettlementPlanner.planDistrict(
                java.util.List.of(new Site(0, 0, 64, 64)), java.util.List.of(new Site(10, 0, 64, 64)),
                new Site(200, 100, 32, 32), "0.1.0", 7, districtGuard, 0); throw new AssertionError("Overlap accepted"); }
        catch (IllegalArgumentException expected) { }
        session.pause();
        assert session.status().paused() && session.tick(10, System.nanoTime() + 1_000_000_000L) == 0;
        session.resume();
        try { new WorldGenSession(null, sessionPlanner, "v"); throw new AssertionError("Null profile accepted"); }
        catch (IllegalArgumentException expected) { }
        try { new WorldGenSession(sessionProfile, sessionPlanner, ""); throw new AssertionError("Empty version accepted"); }
        catch (IllegalArgumentException expected) { }
        var campSite = new Site(0, 0, 64, 64);
        var camp = CampPlanner.plan(campSite, "0.1.0", 42, 8);
        assert camp.operations() == 8 : "Camp must contain the requested props";
        assert camp.equals(CampPlanner.plan(campSite, "0.1.0", 42, 8)) : "Same seed gives the same camp";
        var cells = new java.util.HashSet<String>();
        for (var step : camp.steps()) {
            assert step.dx() >= 4 && step.dx() < 60 && step.dy() >= 4 && step.dy() < 60 : "Step inside the site";
            assert cells.add(step.dx() + ":" + step.dy()) : "No two props on one cell";
        }
        try { CampPlanner.plan(campSite, "0.1.0", 42, 0); throw new AssertionError("Zero props accepted"); }
        catch (IllegalArgumentException expected) { }
        try { CampPlanner.plan(campSite, "0.1.0", 42, CampPlanner.MAX_PROPS + 1); throw new AssertionError("Too many props accepted"); }
        catch (IllegalArgumentException expected) { }
        try { CampPlanner.plan(new Site(0, 0, 8, 8), "0.1.0", 42, 1); throw new AssertionError("Tiny site accepted"); }
        catch (IllegalArgumentException expected) { }
        try { CampPlanner.plan(null, "0.1.0", 42, 1); throw new AssertionError("Null site accepted"); }
        catch (IllegalArgumentException expected) { }
        var road = RoadPlanner.connect(new Site(0, 0, 32, 32), new Site(96, 96, 32, 32));
        assert road.get(0).equals(new RoadPlanner.Point(16, 16)) : "Road starts at the site centre";
        assert road.get(road.size() - 1).equals(new RoadPlanner.Point(112, 112)) : "Road ends at the target centre";
        assert road.size() == 193 : "L-shaped road has one point per tile";
        var roadCells = new java.util.HashSet<RoadPlanner.Point>(road);
        assert roadCells.size() == road.size() : "No duplicate road points";
        var single = RoadPlanner.connect(new Site(0, 0, 32, 32), new Site(0, 0, 32, 32));
        assert single.size() == 1 : "Same site needs one point";
        try { RoadPlanner.connect(null, new Site(0, 0, 1, 1)); throw new AssertionError("Null site accepted"); }
        catch (IllegalArgumentException expected) { }
        try { RoadPlanner.connect(new Site(0, 0, 1, 1), new Site(100000, 0, 1, 1)); throw new AssertionError("Long road accepted"); }
        catch (IllegalArgumentException expected) { }
        var houseSite = new Site(0, 0, 128, 128);
        var house = BuildingPlanner.plan(houseSite, "0.1.0", 11, 3);
        assert house.operations() > 0 : "Building must contain steps";
        assert house.equals(BuildingPlanner.plan(houseSite, "0.1.0", 11, 3)) : "Same seed gives the same house";
        int doors = 0;
        for (var step : house.steps()) {
            assert step.dx() >= 8 && step.dx() < 120 && step.dy() >= 8 && step.dy() < 120 : "Step inside the building area";
            if (step.kind().equals("door")) doors++;
        }
        assert doors == 3 : "One entrance plus one door per extra room";
        try { BuildingPlanner.plan(houseSite, "0.1.0", 1, 0); throw new AssertionError("Zero rooms accepted"); }
        catch (IllegalArgumentException expected) { }
        try { BuildingPlanner.plan(houseSite, "0.1.0", 1, BuildingPlanner.MAX_ROOMS + 1); throw new AssertionError("Too many rooms accepted"); }
        catch (IllegalArgumentException expected) { }
        try { BuildingPlanner.plan(new Site(0, 0, 32, 32), "0.1.0", 1, 1); throw new AssertionError("Tiny site accepted"); }
        catch (IllegalArgumentException expected) { }
        var building = BuildingPlanner.planBuilding(houseSite, "0.1.0", 11, 3);
        assert building.rooms().size() == 3 : "Building must expose three rooms";
        long roomArea = 0;
        for (var room : building.rooms()) {
            assert room.width() > 0 && room.height() > 0;
            assert room.x() > 8 && room.y() > 8 && room.x() + room.width() <= 120 && room.y() + room.height() <= 120;
            roomArea += (long) room.width() * room.height();
        }
        assert roomArea > 0 : "Rooms must not be empty";
        assert building.asSitePlan("0.1.0").operations() == building.steps().size();
        try { new BuildingPlan(houseSite, java.util.List.of(), building.steps()); throw new AssertionError("Empty building accepted"); }
        catch (IllegalArgumentException expected) { }
        var validPlan = new SitePlan(new Site(0, 0, 32, 32), "0.1.0", java.util.List.of(
                new PlanStep("wall", 0, 0), new PlanStep("floor", 10, 10)));
        assert PlanValidator.validate(validPlan, new ProtectedCells(256, java.util.Set.of()), 0).isEmpty();
        var outside = new SitePlan(new Site(0, 0, 32, 32), "0.1.0", java.util.List.of(
                new PlanStep("wall", 32, 0)));
        assert PlanValidator.validate(outside, new ProtectedCells(256, java.util.Set.of()), 0).get(0).reason().equals("out_of_bounds");
        var protectedPlan = new SitePlan(new Site(0, 0, 32, 32), "0.1.0", java.util.List.of(
                new PlanStep("wall", 0, 0)));
        var blockedGuard = new ProtectedCells(16, java.util.Set.of(new ProtectedCells.Cell(0, 0)));
        assert PlanValidator.validate(protectedPlan, blockedGuard, 0).get(0).reason().equals("protected");
        try { PlanValidator.validate(null, blockedGuard, 0); throw new AssertionError("Null plan accepted"); }
        catch (IllegalArgumentException expected) { }
        try { PlanValidator.validate(validPlan, blockedGuard, -1); throw new AssertionError("Negative padding accepted"); }
        catch (IllegalArgumentException expected) { }
        var settlement = SettlementPlanner.plan(new Site(0, 0, 64, 64), new Site(80, 0, 64, 64),
                new Site(180, 100, 32, 32), "0.1.0", 7);
        assert settlement.camp().operations() > 0 && settlement.building().steps().size() > 0;
        assert settlement.road().get(settlement.road().size() - 1).equals(new RoadPlanner.Point(196, 116));
        try { SettlementPlanner.plan(new Site(0, 0, 64, 64), new Site(10, 10, 64, 64),
                new Site(100, 100, 32, 32), "0.1.0", 1); throw new AssertionError("Overlap accepted"); }
        catch (IllegalArgumentException expected) { }
        try { SettlementPlanner.plan(null, new Site(0, 0, 64, 64), new Site(1, 1, 1, 1), "0.1.0", 1); throw new AssertionError("Null site accepted"); }
        catch (IllegalArgumentException expected) { }
        var freeGuard = new ProtectedCells(256, java.util.Set.of());
        SettlementPlanner.planValidated(new Site(0, 0, 64, 64), new Site(80, 0, 64, 64),
                new Site(180, 100, 32, 32), "0.1.0", 7, freeGuard, 0);
        var settlementBlockedGuard = new ProtectedCells(16, java.util.Set.of(new ProtectedCells.Cell(1, 1)));
        try { SettlementPlanner.planValidated(new Site(0, 0, 64, 64), new Site(80, 0, 64, 64),
                new Site(180, 100, 32, 32), "0.1.0", 7, settlementBlockedGuard, 0); throw new AssertionError("Protected settlement accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("World-generation tests passed: site bounds, deterministic search, protected cells, reservations, budget queue, pause, map profiles, time budget and plan commit.");
    }
}
