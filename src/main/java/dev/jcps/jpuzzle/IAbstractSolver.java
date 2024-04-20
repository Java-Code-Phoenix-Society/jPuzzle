package dev.jcps.jpuzzle;

/**
 * The <code>iAbstractSolver</code> interface represents an abstract solver for a puzzle.
 */
public interface IAbstractSolver {
    /**
     * Composite moves. The six basic hole-preserving moves on a
     * 5-puzzle ({@code 3 x 2}).
     */
    interface Move {
        /**
         * Represents the left corner twist, clockwise.
         */
        int MA = 0;

        /**
         * Represents the left corner twist, inverse.
         */
        int MAI = 1;

        /**
         * Represents the right corner twist, clockwise.
         */
        int MB = 2;

        /**
         * Represents the right corner twist, inverse.
         */
        int MBI = 3;

        /**
         * Represents the full rotation, clockwise.
         */
        int MC = 4;

        /**
         * Represents the full rotation, inverse.
         */
        int MCI = 5;

        /**
         * Represents the total number of composite moves (6).
         */
        int NMOVE = 6;
    }

    /**
     * The <code>Direction</code> interface defines directions for simple moves on a puzzle.
     */
    interface Direction {

        /**
         * Represents the up direction.
         */
        int UP = Rotation.ROT0;

        /**
         * Represents the right direction.
         */
        int RT = Rotation.ROT90;

        /**
         * Represents the down direction.
         */
        int DN = Rotation.ROT180;

        /**
         * Represents the left direction.
         */
        int LT = Rotation.ROT270;

        /**
         * Represents the total number of directions (4).
         */
        int NDIR = Rotation.NROT;
    }
}
