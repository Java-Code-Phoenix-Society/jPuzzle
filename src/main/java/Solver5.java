/*
 * Solver5.java
 *
 * Licensed Materials - See the file license.txt.
 * Copyright (c) Joseph Bowbeer 1998, 1999. All rights reserved.
 */

import java.awt.*;

/**
 * Solves 5-puzzles ({@code 3} x {@code 2}).
 * <p>
 * This solver is rule-based, operating on a set of rules taken
 * from Donald Michie; "Preserving the Vital Link of Comprehension";
 * <i>Practical Computing</i>; 9/79; pg 64. An error in rule 3 has
 * been corrected here. Also, in order to support any position of
 * the goal hole, the original rule 1 has been replaced here by
 * rules 0 and 1.
 *
 * @author <a href="mailto:jozart@csi.com">Joseph Bowbeer</a>
 * @version 1.2
 */
public class Solver5 extends AbstractSolver
        implements iSolver.Pair {
    /* Michie's rules. */
//                                          MA      MAI      MB      MBI      MC      MCI
//  private static final int ruleSet2 = (1<<MA)|(1<<MAI)|(1<<MB)|(1<<MBI)                 ;
    private static final int ruleSet3 = (1 << MC) | (1 << MCI);
    private static final int ruleSet4 = (1 << MA) | (1 << MBI);
    private static final int ruleSet5 = (1 << MAI) | (1 << MB) | (1 << MC) | (1 << MCI);
    private static final int ruleSet6 = (1 << MAI) | (1 << MB);
    /**
     * Maps edge pairs to their piece positions.
     */
    private static final Point[][] edges =
            {
                    {new Point(0, 1), new Point(0, 0)},   // LEFT
                    {new Point(2, 0), new Point(2, 1)},   // RGHT
            };
    /**
     * Constructs a new 5-puzzle solver.
     * @param game current game board
     * @param goal the board in the goal state
     * @param mQue move queue
     */
    public Solver5(Board game, Board goal, MoveQ mQue) {
        super(game, goal, mQue);
    }

    /**
     * Advances a few moves toward a solution.
     *
     * @return {@code true} - if moves were made.
     */
    public boolean solveNext() {
        /* RULE 0 */

        if (isSolved()) return false;

        if (solveHole()) return true;

        /*
         * RULE 1 -- move game hole to the bottom center
         *  position preferred by this solver.
         */

        if (game.pieceAt(HOLE) != null) {
            pushMove(game.nMoveHole(HOLE));
            return true;
        }

        /* Align goal hole with game hole. */

        Point g0 = goal.location(null);
        goal.moveHole(HOLE);

        try {
            return solve5();
        } finally {
            /*
             * Restore original goal position,
             *  eg, bottom right.
             */
            goal.moveHole(g0);
        }
    }

    /**
     * Advances the given piece toward the upper left corner (0,0).
     *
     * @param n piece to move.
     * @return {@code true} - if moves were made.
     */
    public boolean solveCorner(Object n) {
        /*
         * Rule: If piece n is at coord c[i]
         *  then choose move i.
         */
        final Point[] c =
                {
                        new Point(0, 1),     // MA
                        new Point(1, 0),     // MAI
                        new Point(2, 1),     // MB
                        new Point(2, 0),     // MBI
                };

        /* Move game hole to base position. */

        if (game.pieceAt(HOLE) != null) {
            pushMove(game.nMoveHole(HOLE));
            return true;
        }

        for (int i = 0; i < c.length; i++) {
            if (n.equals(game.pieceAt(c[i]))) {
                pushMove(i);
                return true;
            }
        }

        throw new RuntimeException("solveCorner");
    }

    /**
     * Returns the distance between where a piece is and
     * where it should be.
     *
     * @param n piece.
     * @return the piece distance.
     */
    private final int pieceDistance(Object n) {
        Point a = game.location(n);
        Point b = goal.location(n);

        return (Math.abs(a.x - b.x) + Math.abs(a.y - b.y));
    }

    /**
     * Returns the sum of the piece distances of an edge pair.
     *
     * @param pair edge pair.
     * @return the edge pair distance.
     */
    private final int edgePairDistance(int pair) {
        Object n0 = goal.pieceAt(edges[pair][0]);
        Object n1 = goal.pieceAt(edges[pair][1]);

        return (pieceDistance(n0) + pieceDistance(n1));
    }

    /**
     * Returns the separation between the pieces of an edge pair,
     * measured by walking around the puzzle and counting the
     * intervening pieces.
     *
     * @param pair edge pair.
     * @return the number of intervening pieces.
     */
    private final int apart(int pair) {
        Object n0 = goal.pieceAt(edges[pair][0]);
        Object n1 = goal.pieceAt(edges[pair][1]);

        Point c = game.location(n0);

        for (int sum = 0; sum < 4; sum++) {
            if (n1.equals(game.pieceAt(iterate(c))))
                return sum;
        }

        throw new RuntimeException("apart");
    }

    /**
     * Returns the next location in a clockwise enumeration of
     * the puzzle coordinates.
     *
     * @param c starting location.
     * @return the next location in a clockwise enumeration.
     */
    private final Point iterate(Point c) {
        if (c.y == 0)
            if (c.x == 2)
                c.y++;
            else
                c.x++;
        else if (c.x == 0)
            c.y = 0;
        else
            c.x = 0;   // skip hole at (1,1)

        return c;
    }

    /**
     * Solves an edge using a move from the give rule set.
     *
     * @return {@code true} - if moves were made.
     */
    private final boolean solveEdge(int edge, int ruleSet) {
        for (int m = 0; m < NMOVE; m++) {
            if ((ruleSet & 1 << m) != 0) {
                pushMove(m);    // try it
                if (edgePairDistance(edge) == 0)
                    return true;
                undoMove(m);    // undo it
            }
        }
        throw new RuntimeException("solveEdge");
    }

    /**
     * Rule-based solution.
     *
     * @return {@code true} - if moves were made.
     */
    private boolean solve5() {
        /* Compute edge pair distances. */

        final int[] dists = new int[]
                {
                        edgePairDistance(LEFT),
                        edgePairDistance(RGHT),
                };

        /*
         * RULE 2 -- if one edge is solved
         *  then twist the other edge into place.
         */

        if (dists[LEFT] == 0)
            return solveEdge(RGHT, (1 << MB) | (1 << MBI));

        if (dists[RGHT] == 0)
            return solveEdge(LEFT, (1 << MA) | (1 << MAI));

        /* Determine edge pair apartness. */

        final int[] aparts = new int[]
                {
                        apart(LEFT),
                        apart(RGHT),
                };

        /*
         * RULE 3A -- if only one edge is together
         *  and it can be twisted into place
         *  then twist it into place.
         */

        if ((aparts[LEFT] == 0) && (aparts[RGHT] != 0) &&
                (game.pieceAt(CENTER).equals(goal.pieceAt(edges[LEFT][1])))) {
            pushMove(MAI);
            return true;
        }

        if ((aparts[RGHT] == 0) && (aparts[LEFT] != 0) &&
                (game.pieceAt(CENTER).equals(goal.pieceAt(edges[RGHT][0])))) {
            pushMove(MB);
            return true;
        }

        /* Identify the preferred edge pair (pep). */

        int pep = LEFT;

        if ((aparts[RGHT] < aparts[LEFT]) ||
                ((aparts[RGHT] == aparts[LEFT]) &&
                        (dists[RGHT] < dists[LEFT]))) {
            pep = RGHT;
        }

        /*
         * RULE 3B -- if the preferred edge is together
         *  but not in place, move it closer into place.
         */

        if (aparts[pep] == 0) {
            for (int m = 0; m < NMOVE; m++) {
                if ((ruleSet3 & 1 << m) != 0) {
                    pushMove(m);    // try it
                    if (edgePairDistance(pep) < dists[pep])
                        return true;
                    undoMove(m);    // undo it
                }
            }
            throw new RuntimeException("solve5 - rule 3b");
        }

        /*
         * RULE 4 -- if the preferred edge pair is one apart
         *  and the intervening piece is at coord (0,1)
         *  then twist the edge pair together toward its place.
         */

        if ((aparts[pep] == 1) &&
                (game.pieceAt(CORNER).equals(goal.pieceAt(edges[pep][0])))) {
            /*
             * If edge[0] is at coord (0,0)
             *  then intervening piece is at coord (1,0).
             */
            for (int m = 0; m < NMOVE; m++) {
                if ((ruleSet4 & 1 << m) != 0) {
                    pushMove(m);    // try it
                    if (edgePairDistance(pep) < dists[pep])
                        return true;
                    undoMove(m);    // undo it
                }
            }
            throw new RuntimeException("solve5 - rule 4");
        }

        /*
         * RULE 5 -- if preferred edge pair is one apart
         *  and the intervening piece is not at coord (1,0)
         *  then move the intervening piece to coord (1,0).
         */

        if (aparts[pep] == 1) {
            /* Find position of edge[0] on the board. */

            Object n = goal.pieceAt(edges[pep][0]);
            Point c = game.location(n);

            /* Rotate to find intervening piece. */

            n = game.pieceAt(iterate(c));

            /* Move intervening piece to coord (1,0). */

            for (int m = 0; m < NMOVE; m++) {
                if ((ruleSet5 & 1 << m) != 0) {
                    pushMove(m);    // try it
                    if (n.equals(game.pieceAt(CENTER)))
                        return true;
                    undoMove(m);    // undo it
                }
            }
            throw new RuntimeException("solve5 - rule 5");
        }

        /*
         * RULE 6 -- both edges must be three apart,
         *  twist them so that they aren't.
         */
        {
            for (int m = 0; m < NMOVE; m++) {
                if ((ruleSet6 & 1 << m) != 0) {
                    pushMove(m);    // try it
                    if (apart(LEFT) < 3 || apart(RGHT) < 3)
                        return true;
                    undoMove(m);    // undo it
                }
            }
            throw new RuntimeException("solve5 - rule 6");
        }
    }
}

