package dev.jcps.jpuzzle;
/**
 * The <code>iSolver</code> interface represents a solver for a puzzle.
 */
public interface ISolver {

    /**
     * The <code>Pair</code> interface defines edge pairs.
     */
    interface Pair {

        /**
         * Represents the left edge in a pair.
         */
        int LEFT = 0;

        /**
         * Represents the right edge in a pair.
         */
        int RGHT = 1;

        /**
         * Represents the total number of pairs.
         */
        int NPAIR = 2;
    }
}