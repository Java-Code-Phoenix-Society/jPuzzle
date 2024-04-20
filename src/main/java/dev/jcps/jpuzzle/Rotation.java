package dev.jcps.jpuzzle;

/**
 * Rotations: quarter turns.
 */
public class Rotation {
    /**
     * No turn.
     */
    public static final int ROT0 = 0;
    /**
     * Quarter turn right.
     */
    public static final int ROT90 = 1;
    /**
     * Half turn.
     */
    public static final int ROT180 = 2;
    /**
     * Quarter turn left.
     */
    public static final int ROT270 = 3;

    /**
     * Number of quarter-turns (4).
     */
    public static final int NROT = 4;

    private Rotation() {
    }
}
