package personal.worldgen;

import java.util.HashSet;

/** Verifies the 1.17 civic/infrastructure/housing pattern catalog. */
public final class StructurePatternsTest {
    private StructurePatternsTest() {}

    public static void main(String[] args) {
        String[] names = {"townHall", "library", "museum", "cinema", "hotel", "office",
                "factory", "powerPlant", "waterPlant", "sewage", "depot", "trainStation",
                "busDepot", "garage2", "junkyard2", "market", "mall", "motel2", "cabin", "shack"};
        MapPattern.Pattern[] values = {StructurePatterns.townHall(), StructurePatterns.library(),
                StructurePatterns.museum(), StructurePatterns.cinema(), StructurePatterns.hotel(),
                StructurePatterns.office(), StructurePatterns.factory(), StructurePatterns.powerPlant(),
                StructurePatterns.waterPlant(), StructurePatterns.sewage(), StructurePatterns.depot(),
                StructurePatterns.trainStation(), StructurePatterns.busDepot(), StructurePatterns.garage2(),
                StructurePatterns.junkyard2(), StructurePatterns.market(), StructurePatterns.mall(),
                StructurePatterns.motel2(), StructurePatterns.cabin(), StructurePatterns.shack()};
        assert values.length == names.length && values.length == 20;
        int total = 0;
        for (int i = 0; i < values.length; i++) {
            var pattern = values[i];
            assert MapPattern.valid(pattern) : names[i] + " must be a valid pattern";
            assert pattern.width() >= 3 && pattern.height() >= 3 : names[i] + " must have a usable footprint";
            var unique = new HashSet<String>();
            for (var cell : pattern.cells()) {
                assert cell.x() == 0 || cell.y() == 0 || cell.x() == pattern.width() - 1
                        || cell.y() == pattern.height() - 1 : names[i] + " must be a closed outline";
                assert cell.kind() != null && !cell.kind().isBlank() : names[i] + " needs a cell kind";
                unique.add(cell.x() + "," + cell.y());
            }
            assert unique.size() == pattern.cells().size() : names[i] + " must not repeat cells";
            total += pattern.cells().size();
        }
        var demo = PersonalWorldGenAPI.getInstance().structureCatalogDemo();
        assert demo.startsWith("count=20") && demo.contains("townHall=") && demo.contains("shack=")
                : "Catalog demo must expose all 1.17 patterns";
        System.out.println("Structure pattern tests passed: 20 civic/infrastructure/housing outlines, "
                + total + " wall cells, no duplicates, catalog exposed.");
    }
}
