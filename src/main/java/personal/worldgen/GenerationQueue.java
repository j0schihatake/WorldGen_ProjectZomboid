package personal.worldgen;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

/** Bounded operation queue; paused work never blocks the frame. */
public final class GenerationQueue {
    private final Deque<Runnable> queue = new ArrayDeque<>();
    private boolean paused;
    private long completedOps;

    public void add(Runnable operation) {
        if (operation == null) throw new IllegalArgumentException("Missing operation");
        queue.add(operation);
    }

    public int run(int budget) {
        if (budget < 0) throw new IllegalArgumentException("Invalid budget");
        if (paused) return 0;
        int executed = 0;
        while (executed < budget && !queue.isEmpty()) {
            queue.poll().run();
            executed++;
            completedOps++;
        }
        return executed;
    }

    /** Time-budgeted variant: a slow operation cannot overrun the frame deadline. */
    public int run(int budget, long deadlineNanos) {
        if (budget < 0) throw new IllegalArgumentException("Invalid budget");
        if (paused) return 0;
        int executed = 0;
        while (executed < budget && !queue.isEmpty() && System.nanoTime() < deadlineNanos) {
            queue.poll().run();
            executed++;
            completedOps++;
        }
        return executed;
    }

    /** Commit a validated plan as ordinary queue operations. */
    public void addPlan(SitePlan plan, Consumer<PlanStep> executor) {
        if (plan == null || executor == null) throw new IllegalArgumentException("Missing plan or executor");
        for (var step : plan.steps()) add(() -> executor.accept(step));
    }

    public void pause() { paused = true; }
    public void resume() { paused = false; }
    public boolean isPaused() { return paused; }
    public int size() { return queue.size(); }
    public int pendingOps() { return queue.size(); }
    public long completedOps() { return completedOps; }
    public void clear() { queue.clear(); }
}
