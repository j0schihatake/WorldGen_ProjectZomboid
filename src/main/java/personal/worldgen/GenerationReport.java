package personal.worldgen;

/** Snapshot of a worldgen queue result. */
public record GenerationReport(int total, long completed, long failed, int pending) {
    public GenerationReport {
        if (total < 0 || completed < 0 || failed < 0 || pending < 0)
            throw new IllegalArgumentException("Negative report value");
        if (completed + failed + pending > total)
            throw new IllegalArgumentException("Report exceeds total");
    }

    public boolean success() { return failed == 0 && pending == 0 && completed == total; }

    public String summary() {
        return "worldgen total=" + total + " completed=" + completed
                + " failed=" + failed + " pending=" + pending + " success=" + success();
    }
}
