package personal.worldgen;

import java.util.ArrayList;
import java.util.List;

/** Ordered, bounded registry of generated region ids. */
public final class RegionRegistry {
    private final int max;
    private final List<String> ids = new ArrayList<>();

    public RegionRegistry(int max) { this.max = Math.max(1, Math.min(1024, max)); }

    public boolean remember(String id) {
        if (id == null || id.isBlank()) return false;
        if (ids.contains(id)) return false;
        if (ids.size() >= max) ids.remove(0);
        ids.add(id);
        return true;
    }

    public boolean has(String id) { return id != null && ids.contains(id); }
    public boolean forget(String id) { return ids.remove(id); }
    public int size() { return ids.size(); }
    public boolean isEmpty() { return ids.isEmpty(); }
    public void clear() { ids.clear(); }
    public String first() { return ids.isEmpty() ? "" : ids.get(0); }
    public String last() { return ids.isEmpty() ? "" : ids.get(ids.size() - 1); }
    public String at(int index) { return index < 0 || index >= ids.size() ? "" : ids.get(index); }
    public List<String> list() { return List.copyOf(ids); }

    /** CSV in insertion order; never contains blanks or duplicates. */
    public String csv() { return String.join(",", ids); }

    /** Restores a CSV snapshot; invalid/blank/duplicate ids are ignored. */
    public int restoreCsv(String csv) {
        if (csv == null || csv.isBlank()) return 0;
        int added = 0;
        for (String id : csv.split(",")) if (remember(id.trim())) added++;
        return added;
    }

    public String summary() { return "regions=" + ids.size() + " first=" + first() + " last=" + last(); }

    public static String demo() {
        var r = new RegionRegistry(4);
        r.remember("a"); r.remember("b"); r.remember("a"); r.remember("c");
        return "has=" + r.has("b") + " forget=" + r.forget("b") + " " + r.summary();
    }
}
