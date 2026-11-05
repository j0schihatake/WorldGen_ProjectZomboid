package personal.worldgen;

/** A candidate building site in world tiles. No game objects. */
public record Site(int x, int y, int width, int height) {
    public static final int MAX_SIZE = 4096;

    public Site {
        if (width <= 0 || height <= 0 || width > MAX_SIZE || height > MAX_SIZE)
            throw new IllegalArgumentException("Invalid site size");
    }

    public long area() {
        return (long) width * height;
    }
}
