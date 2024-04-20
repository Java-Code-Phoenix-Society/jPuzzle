package dev.jcps.jpuzzle;
/*
 * Solver.java
 *
 * Licensed Materials - See the file license.txt.
 * Copyright (c) Joseph Bowbeer 1998, 1999. All rights reserved.
 */

import java.awt.*;
import java.util.Arrays;
import java.util.Objects;

import static dev.jcps.jpuzzle.Rotation.*;

/**
 * Solves 8-puzzles ({@code 3 x 3}) and larger.
 * <p>
 * 5-puzzles are dispatched to the 5-puzzle solver. 8-puzzles
 * are decomposed into a top 3-edge and a lower 5-puzzle. The
 * top 3-edge is solved by sending the upper 5-puzzle to the
 * 5-puzzle solver. The goal positions for the two extra pieces
 * of the upper 5-puzzle, not in the top 3-edge, are assigned
 * to make the upper 5-puzzle solvable. Once the top
 * 3-edge is solved, the bottom 5-puzzle is solved.
 * <p>
 * This technique of decomposing an {@code MxN} puzzle into an M-edge,
 * and a {@code Mx(N-1)} puzzle was developed by P. Krause, J. O'Brien,
 * M. Tuceiryan, and J. Bowbeer at the Univ. of Illinois in 1980.
 * (The original solver was written in Prolog.) More efficient
 * methods for decomposing 11-puzzles and larger were developed
 * by J. Bowbeer beginning in 1986.
 * <p>
 * A few important aspects of the solver implementation are
 * described below.
 * <p>
 * The goal hole is rotated out of the preferred M-edge,
 * if necessary, before solving begins. This way, the goal hole
 * is never in the preferred M-edge being solved, and any
 * transformations to the goal hole during the edge solving
 * process will not alter the edge goal.
 * <p>
 * Likewise, when the preferred M-edge is solved, the remaining
 * sub-puzzle solver can be called directly without any goal hole
 * adjustments. This helps to ensure that the {@code solveHole}
 * method can restore the goal hole to its original location.
 * Note: {@code solveHole} won't work if the goal hole has been
 * twisted.
 * <p>
 * Finally, the goal sent to a sub-puzzle solver must remain
 * constant, given the same pieces, for each invocation. Otherwise,
 * the solver may thrash.
 *
 * @author <a href="mailto:jozart@csi.com">Joseph Bowbeer</a>
 * @version 1.2
 */
public class Solver extends AbstractSolver {
    /**
     * Set of rules for clearing pieces from the bottom row of
     * an 8-puzzle.
     * <p>
     * NOTE: The additional rules are only needed if the top
     * set can contain more than three pieces.
     */
    private static final ClearRule[] clearRules = {
            /* Clear bottom left. */

            new ClearRule(new Point(0, 2), new Point(0, 1), MB, ROT180),
            new ClearRule(new Point(0, 2), new Point(1, 0), MC, ROT270),
            new ClearRule(new Point(0, 2), new Point(2, 1), MC, ROT180),
            //  new ClearRule(new Point(0,2), new Point(0,0), MAI, ROT0   ),
            //  new ClearRule(new Point(0,2), new Point(2,0), MB , ROT0   ),

            /* Clear bottom right. */

            new ClearRule(new Point(2, 2), new Point(2, 1), MAI, ROT180),
            new ClearRule(new Point(2, 2), new Point(1, 0), MCI, ROT90),
            new ClearRule(new Point(2, 2), new Point(0, 1), MCI, ROT180),
            //  new ClearRule(new Point(2,2), new Point(2,0), MB , ROT0   ),
            //  new ClearRule(new Point(2,2), new Point(0,0), MAI, ROT0   ),

            /* Clear bottom center. */

            new ClearRule(new Point(1, 2), new Point(0, 1), MBI, ROT180),
            new ClearRule(new Point(1, 2), new Point(2, 1), MA, ROT180),
            new ClearRule(new Point(1, 2), new Point(0, 0), MAI, ROT0),
            //  new ClearRule(new Point(1,2), new Point(1,0), MA , ROT0   ),
            //  new ClearRule(new Point(1,2), new Point(2,0), MB , ROT0   ),
    };

    /**
     * Constructs a new solver.
     *
     * @param game current game board
     * @param goal the board in the goal state
     * @param mQue the move queue
     */
    public Solver(Board game, Board goal, MoveQ mQue) {
        super(game, goal, mQue);
    }

    /**
     * Tests a piece for membership in an array of pieces.
     *
     * @param set array of pieces.
     * @param n   piece to find.
     * @return {@code true} - if {@code n} is a member of {@code set}.
     */
    private static boolean member(Object[] set, Object n) {
        for (Object o : set) {
            if (Objects.equals(n, o))
                return true;
        }
        return false;
    }

    /**
     * Advances a few moves toward a solution.
     *
     * @return {@code true} - if moves were made.
     */
    public boolean solveNext() {
        Dimension size = game.getSize();

        /* Dispatch to small puzzle solvers. */

        if (size.width == 3 && size.height == 2) {
            return new Solver5(game, goal, mQue).solveNext();
        }

        if (size.width < 3 || size.height < 3) {
            return randomWalk(); // (!)
        }

        /*
         * If puzzle is sideways then turn it upright,
         *  keeping the hole out of the top row.
         *
         * We'll  restore the original orientation as soon as
         *  the height is equal to the width again.
         */

        if (sidewaysFlip(size)) {
            return true;
        }

        /* RULE 0 */

        if (isSolved()) return false;

        if (solveHole()) return true;

        // assert goal hole is not in top edge
        if (goal.location(null).y == 0) {
            //throw new RuntimeException("solve - goal hole")
            return false;
        }

        /* RULE 1 -- move game hole out of top edge. */

        if (game.location(null).y == 0) {
            pushMove(deltas[DN]);
            return true;
        }

        /*
         * RULE 2 -- if top edge is solved
         *  then solve the bottom sub-puzzle.
         */

        Point c = new Point();
        if (rule2(c, size)) {
            return true;
        }

        /*
         * RULE 3 -- solve next three pieces of top edge of goal
         *  point c is first unsolved position on top edge.
         */

        int col = c.x;

        Result result = getRule3Result(size, col, c);

        /*
         * RULE 3A -- if any piece to the left of chunk
         *  is a member of the chunk then move it to the right.
         */

        if (result.col() > 0) {
            Point base = new Point(size.width - 1, 1);
            Dimension sz = new Dimension(size.height - 1, size.width);

            Board gm = game.transform(base, sz, ROT90);
            //  Board gl; -unused-

            if (new Solver(gm, gm, mQue).clearBottom(result.top(), size.width - result.col()))
                return true;
        }

        /*
         * RULE 3B -- if any piece to the right of chunk
         *  is a member of the chunk then move it to the left.
         */
        if (rule3b(result.col(), size, result.top())) return true;

        /*
         * RULE 3C -- if any piece in the bottom rows
         *  is a member of the top 3-edge of the goal ("chunk")
         *  then move it into the upper 5-puzzle (3x2).
         */
        if (rule3c(result.col(), size, result.top())) return true;

        /*
         * RULE 4 -- if the upper 5-puzzle contains
         *  the top 3-edge of the goal, then solve the
         *  top 3-edge by solving the upper 5-puzzle.
         */
        return rule4(result.col(), result.top());
    }

    /**
     * RULE 2: Solve the bottom sub-puzzle if the top edge is solved.
     * <p>
     * This method checks if the top edge of the puzzle is solved by iterating over its width and
     * checking if each piece is solved. If the top edge is entirely solved, it slices off the top edge
     * and sends the bottom puzzle to the solver. The bottom puzzle is transformed by excluding the top
     * edge, and then both the game and goal boards are updated accordingly.
     * </p>
     *
     * @param c    The position of the first unsolved piece on the top edge.
     * @param size The dimensions of the puzzle board.
     * @return {@code true} if the top edge is solved and the bottom sub-puzzle is sent to the solver,
     * {@code false} otherwise.
     */
    private boolean rule2(Point c, Dimension size) {
        for (; c.x < size.width; c.x++) {
            if (!isPieceSolved(c)) break;
        }

        if (c.x == size.width) {
            /*
             * Top edge is solved: slice off the top edge
             *  and send the bottom puzzle to solver.
             */

            Point base = new Point(0, 1);
            Dimension sz = new Dimension(size.width, size.height - 1);

            Board gm = game.transform(base, sz);
            Board gl = goal.transform(base, sz);

            return new Solver(gm, gl, mQue).solveNext();
        }
        return false;
    }

    /**
     * Performs a sideways flip of the puzzle if necessary.
     * <p>
     * This method checks if the puzzle needs to be flipped sideways based on its dimensions and the goal's location.
     * If the width of the puzzle is greater than its height or if it's already flipped and has equal dimensions,
     * a flip operation is performed. The flip operation rotates the puzzle 90 degrees clockwise if it's already flipped,
     * and if not, it rotates it 270 degrees clockwise. The flip is performed on both the game and goal boards.
     * </p>
     *
     * @param size The dimensions of the puzzle board.
     * @return {@code true} if a flip operation was performed and a solver is invoked for the next step, {@code false} otherwise.
     */
    private boolean sidewaysFlip(Dimension size) {
        final boolean flipped = (goal.location(null).x == 0);

        if (size.width > size.height ||
                (flipped && (size.width == size.height))) {
            Dimension sz = new Dimension(size.height, size.width);
            Point base;
            int rot;

            if (flipped) {
                base = new Point(size.width - 1, 0);
                rot = ROT90;
            } else {
                base = new Point(0, size.height - 1);
                rot = ROT270;
            }

            Board gm = game.transform(base, sz, rot);
            Board gl = goal.transform(base, sz, rot);

            return new Solver(gm, gl, mQue).solveNext();
        }
        return false;
    }

    /**
     * Retrieves the result of Rule 3, which involves solving the next three pieces of the top edge of the goal.
     * <p>
     * Rule 3 dictates solving the next three pieces of the top edge of the goal, with the starting point 'c'
     * representing the first unsolved position on the top edge.
     * </p>
     *
     * @param size The dimensions of the puzzle board.
     * @param col  The column index representing the starting position for solving the top edge pieces.
     * @param c    The first unsolved position on the top edge represented as a {@code Point}.
     * @return A {@link Result} object containing the top pieces to solve and the column index.
     */
    private Result getRule3Result(Dimension size, int col, Point c) {
        Object[] top;

        int rem = size.width % 3;

        if (col < rem) {
            /* Handle odd lot on left. */
            top = new Object[rem - col];
        } else {
            /* Back up to beginning of triplet. */
            top = new Object[3];
            col -= (col - rem) % 3;
        }

        /* Collect pieces of top chunk. */

        Point p = new Point(col, c.y);
        for (int i = 0; i < top.length; i++, p.x++) {
            top[i] = goal.pieceAt(p);
        }

        return new Result(top, col);
    }

    /**
     * RULE 3B -- if any piece to the right of chunk
     * is a member of the chunk then move it to the left.
     */
    private boolean rule3b(int col, Dimension size, Object[] top) {
        if (col + 3 < size.width) {
            Point base = new Point(col, size.height - 1);
            Dimension sz = new Dimension(size.height, size.width - col);

            Board gm = game.transform(base, sz, ROT270);
            //  Board gl; -unused-

            return new Solver(gm, gm, mQue).clearBottom(top, 3);
        }
        return false;
    }

    /**
     * RULE 3C -- if any piece in the bottom rows
     * is a member of the top 3-edge of the goal ("chunk")
     * then move it into the upper 5-puzzle (3x2).
     */
    private boolean rule3c(int col, Dimension size, Object[] top) {
        Point base = new Point(col, 0);
        Dimension sz = new Dimension(3, size.height);

        Board gm = game.transform(base, sz);
        //  Board gl; -unused-

        return new Solver(gm, gm, mQue).clearBottom(top, 2);
    }

    /**
     * RULE 4 -- if the upper 5-puzzle contains
     * the top 3-edge of the goal, then solve the
     * top 3-edge by solving the upper 5-puzzle.
     */
    private boolean rule4(int col, Object[] top) {
        Point base = new Point(col, 0);
        Dimension sz = new Dimension(3, 2);

        Board gm = game.transform(base, sz);
        //  Board gl; -unused-

        if (top.length < 3) {
            /* Handle odd lot on left. */
            return new Solver5(gm, gm, mQue).solveCorner(top[0]);
        } else {
            /* Assign solvable goal using pieces from game. */
            Board gl = gm.assign(top);
            return new Solver5(gm, gl, mQue).solveNext();
        }
    }

    /**
     * Clears the hole and any pieces that are members of
     * {@code top} from the bottom row.
     *
     * @param top  set of pieces to clear.
     * @param nrow the row to clear.
     * @return {@code true} - if moves were made.
     * @see #clearHelper(Object[], Point)
     */
    private boolean clearBottom(Object[] top, int nrow) {
        for (Dimension sz = game.getSize(); sz.height > nrow; --sz.height) {
            /* Move game hole out of bottom edge. */

            if (game.location(null).y == sz.height - 1) {
                pushMove(deltas[UP]);
                return true;
            }

            /*
             * Base is corner of an 8-puzzle work area that
             * slides right-to-left along bottom edge.
             */
            Point base = new Point(sz.width - 3, sz.height - 3);

            if (clearBase(top, base)) return true;
        }

        return false;
    }

    /**
     * Attempts to clear the base of the puzzle by sliding a working area from right to left along the bottom edge.
     * <p>
     * The base is the corner of an 8-puzzle work area that slides right-to-left along the bottom edge.
     * This method iterates over each position of the base, sliding it by 3 units to the left at each step,
     * and checks if there are any pieces from the top array present in the work area.
     * If any such pieces are found, it invokes the {@link #clearHelper(Object[], Point)} method
     * to attempt to clear the puzzle.
     * </p>
     *
     * @param top  An array containing the next three pieces of the top edge.
     * @param base The current base position represented as a {@code Point}.
     * @return {@code true} if moves were made; {@code false} otherwise.
     */
    private boolean clearBase(Object[] top, Point base) {
        for (; base.x + 3 > 0; base.x -= 3) {
            /* Slide back if we've gone over a bit. */

            if (base.x < 0) base.x = 0;

            /* Co-ord c walks along bottom of work area. */

            Point c = new Point(base.x + 2, base.y + 2);

            for (; c.x >= base.x; --c.x) {
                if (member(top, game.pieceAt(c))) break;
            }

            /* Is anyone there? */

            if (c.x >= base.x && (clearHelper(top, base))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Clears pieces from the bottom edge of an 8-puzzle. Pieces
     * in the bottom 3-edge that are members of the top set are
     * moved into the upper 5-puzzle.
     * <p>
     * NOTE: In this deployment, the top set never contains more
     * than three pieces. This method will fail if more than five
     * pieces of an 8-puzzle are members of {@code top}, as no
     * more than 5 pieces can be packed into the upper 5-puzzle.
     * <p>
     *
     * @param top  set of pieces to clear.
     * @param base origin of sub-puzzle to clear.
     * @return {@code true} - if moves were made.
     */
    private boolean clearHelper(Object[] top, Point base) {
        /* Move game hole to center. */

        Point h0 = new Point(HOLE);
        h0.translate(base.x, base.y);

        if (game.pieceAt(h0) != null) {
            pushMove(game.nMoveHole(h0));
            return true;
        }

        /* Find a matching rule. */

        for (ClearRule rule : clearRules) {
            Point c = new Point(rule.lower);
            c.translate(base.x, base.y);
            if (member(top, game.pieceAt(c))) {
                c = new Point(rule.upper);
                c.translate(base.x, base.y);
                if (!member(top, game.pieceAt(c))) {
                    pushMove(rule.iMov, rule.iRot);
                    return true;
                }
            }
        }

        //throw new RuntimeException("clearHelper")
        return false;
    }

    private record Result(Object[] top, int col) {
        @Override
        public String toString() {
            return "Result{" +
                    "top=" + Arrays.toString(top) +
                    ", col=" + col +
                    '}';
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Result result = (Result) obj;
            return col == result.col && Arrays.equals(top, result.top);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(col);
            result = 31 * result + Arrays.hashCode(top);
            return result;
        }
    }

    /**
     * A rule for clearing a piece from the bottom row of an 8-puzzle.
     * <p>
     * Rule: If the piece in the bottom row at position {@code lower}
     * is a member of the top set and the piece in the upper 5-puzzle
     * at position {@code upper} is not a member of the top set, then
     * perform the indicated move & rotation.
     */
    private static final class ClearRule {
        private final Point lower;
        private final Point upper;
        private final int iMov;
        private final int iRot;

        public ClearRule(Point lower, Point upper, int iMov, int iRot) {
            this.lower = lower;
            this.upper = upper;
            this.iMov = iMov;
            this.iRot = iRot;
        }
    }
}

