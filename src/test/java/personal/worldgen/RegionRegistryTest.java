package personal.worldgen;

public final class RegionRegistryTest {
    public static void main(String[] args) {
        var r = new RegionRegistry(3);
        assert r.isEmpty() && r.remember("a") && r.remember("b");
        assert !r.remember("a") : "Duplicate must not be remembered";
        assert r.size() == 2 && r.first().equals("a") && r.last().equals("b");
        assert r.remember("c") && r.remember("d") : "Registry must evict when full";
        assert r.size() == 3 && !r.has("a") && r.has("b") && r.last().equals("d");
        assert r.at(0).equals("b") && r.at(2).equals("d") && r.at(9).isEmpty();
        assert r.forget("c") && !r.forget("c") && r.size() == 2;
        assert !r.remember("") && !r.remember(null) : "Blank ids are rejected";
        r.clear();
        assert r.isEmpty() && r.first().isEmpty() && r.last().isEmpty();
        assert new RegionRegistry(0).remember("x") : "Zero cap clamps to one";
        var small = new RegionRegistry(1);
        assert small.remember("x") && small.remember("y") && small.size() == 1 && small.first().equals("y");
        var csv = new RegionRegistry(4);
        assert csv.remember("a") && csv.remember("b");
        assert csv.csv().equals("a,b") : "CSV must keep insertion order";
        assert csv.restoreCsv(" b , c , a ") == 1 : "CSV restore must add only new ids";
        assert csv.csv().equals("a,b,c") : "CSV restore must trim and deduplicate";
        assert csv.restoreCsv("") == 0 && csv.restoreCsv(null) == 0;
        var bounded = new RegionRegistry(2);
        assert bounded.restoreCsv("x,y,z") == 3 && bounded.csv().equals("y,z") : "CSV restore must respect cap";
        System.out.println("Region registry tests passed: order, duplicate, eviction, bounds, CSV restore, forget and clear.");
    }
}
