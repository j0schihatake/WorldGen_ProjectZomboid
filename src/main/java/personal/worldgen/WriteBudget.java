package personal.worldgen;

/** Bounded write counter for one explicit WorldGen operation. */
public final class WriteBudget {
    private int limit;
    private int used;

    public WriteBudget(int limit) { setLimit(limit); }

    public void setLimit(int value) { limit = Math.max(1, Math.min(512, value)); }

    public int limit() { return limit; }
    public int used() { return used; }
    public int remaining() { return Math.max(0, limit - used); }
    public void reset() { used = 0; }

    /** Consumes one write slot; false means the operation must stop. */
    public boolean take() {
        if (used >= limit) return false;
        used++;
        return true;
    }
}
