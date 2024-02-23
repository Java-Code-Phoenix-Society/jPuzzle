/**
 * Rotations: quarter turns.
 */
public interface Rotation {
    /**
     * No turn.
     */
    int ROT0 = 0;
    /**
     * Quarter turn right.
     */
    int ROT90 = 1;
    /**
     * Half turn.
     */
    int ROT180 = 2;
    /**
     * Quarter turn left.
     */
    int ROT270 = 3;

    /**
     * Number of quarter-turns (4).
     */
    int NROT = 4;
}
