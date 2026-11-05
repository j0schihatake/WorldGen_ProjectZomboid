package personal.worldgen;

import java.util.ArrayDeque;
import java.util.Deque;

/** Bounded operation queue; paused work never blocks the frame. */
public final class GenerationQueue {
    private final Deque<Runnable> queue = new ArrayDeque<>();
    private boolean paused;

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
        }
        return executed;
    }

    public void pause() { paused = true; }
    public void resume() { paused = false; }
    public boolean isPaused() { return paused; }
    public int size() { return queue.size(); }
}
