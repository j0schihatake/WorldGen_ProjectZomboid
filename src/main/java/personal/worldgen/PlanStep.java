package personal.worldgen;

/** One planned operation inside a site; data only, no world access. */
public record PlanStep(String kind, int dx, int dy) {
    public PlanStep {
        if (kind == null || kind.isBlank()) throw new IllegalArgumentException("Missing step kind");
        if (Math.abs((long) dx) > Site.MAX_SIZE || Math.abs((long) dy) > Site.MAX_SIZE)
            throw new IllegalArgumentException("Step outside the allowed range");
    }
}
