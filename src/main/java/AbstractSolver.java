/*
 * AbstractSolver.java
 *
 * Licensed Materials - See the file license.txt.
 * Copyright (c) Joseph Bowbeer 1998, 1999. All rights reserved.
 */

import java.awt.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Base class for iterative Puzzle solvers. Maintains a game
 * board, goal board, and move queue. Defines a basic set of
 * moves and provides methods for adding and removing these
 * moves from the move queue.
 * <p>
 * Subclasses must implement the {@code solveNext} method,
 * which advances the game board toward the goal and pushes
 * the few moves that were made onto the move queue.
 *
 * @author <a href="mailto:jozart@csi.com">Joseph Bowbeer</a>
 * @version 1.2
 */
public abstract class AbstractSolver
        implements Rotation,
        iAbstractSolver.Direction,
        iAbstractSolver.Move,
        java.io.Serializable, iAbstractSolver {
    /**
     * Top left corner position.
     */
    protected static final Point CORNER = new Point(0, 0);
    /**
     * Top center position for 3, 5 and 8-puzzles.
     */
    protected static final Point CENTER = new Point(1, 0);
    /**
     * Solver's preferred hole position for 3, 5 and 8-puzzles.
     */
    protected static final Point HOLE = new Point(1, 1);

    /**
     * Maps move directions to column, row offsets.
     */
    protected static final Dimension[] deltas =
            {
                    new Dimension(0, -1),       // UP
                    new Dimension(1, 0),       // RT
                    new Dimension(0, 1),       // DN
                    new Dimension(-1, 0),       // LT
            };

    /**
     * Maps composite moves to sequences of move directions.
     */
    private static final int[][] moves =
            {
                    {UP, LT, DN, RT},           // MA   - left  corner twist
                    {LT, UP, RT, DN},           // MAI  -     inverse
                    {RT, UP, LT, DN},           // MB   - right corner twist
                    {UP, RT, DN, LT},           // MBI  -     inverse
                    {RT, UP, LT, LT, DN, RT},   // MC   - full rotation
                    {LT, UP, RT, RT, DN, LT},   // MCI  -     inverse
            };

    /**
     * Game board.
     *
     * @serial
     */
    protected /*final*/ Board game;

    /**
     * Goal board.
     *
     * @serial
     */
    protected /*final*/ Board goal;

    /**
     * Move queue.
     *
     * @serial
     */
    protected /*final*/ MoveQ mque;

    /**
     * Constructs a new AbstractSolver.
     *
     * @param game The game board.
     * @param goal The goal state.
     * @param mque The move queue.
     */
    protected AbstractSolver(Board game, Board goal, MoveQ mque) {
        this.game = game;
        this.goal = goal;
        this.mque = mque;
    }

//  /**
//   * Solves the puzzle.
//   *
//   * @return {@code true} - if moves were made.
//   */
//  public boolean solveAll()
//  {
//      if (isSolved()) return false;
//      while (solveNext())
//      {
//          if (isSolved()) return true;
//      }
//      throw new RuntimeException("solveAll");
//  }

    /**
     * Checks if the piece at the given location is solved.
     *
     * @param c location of piece.
     * @return {@code true} - if the piece is solved.
     */
    public boolean isPieceSolved(Point c) {
        Object n = goal.pieceAt(c);

        if (n == null)  // the hole is not a piece
        {
            throw new RuntimeException("isPieceSolved(" + c + ")");
        }

        return n.equals(game.pieceAt(c));
    }

    /**
     * Checks if the game is solved.
     * <p>
     * NOTE: The goal is not checked for a hole.
     *
     * @return {@code true}
     */
    public boolean isSolved() {
        return game.matches(goal);
    }

    /**
     * Advances a few moves toward a solution.
     * Implemented by subclasses.
     *
     * @return {@code true} - if moves were made.
     */
    public abstract boolean solveNext();

    /**
     * If the game is solved except for a simple move, solves the game
     * and returns {@code true}.
     * <p>
     * NOTE: The technique employed is to find the location of the game
     * hole and the piece in the goal that occupies that location, and
     * then see if moving that piece solves the game. If this technique
     * were allowed to continue more than one level deep, it would find
     * all solutions that didn't involve a twist, that is, in which the
     * path of the hole didn't cross itself.
     *
     * @return {@code true} - if a move was made.
     */
    public boolean solveHole() {
        Point p = game.location(null);
        Object n = goal.pieceAt(p);

        if (n != null && n == game.nMoveHole(n)) {
            pushMove(n);

            if (isSolved())
                return true;

            undoMove(n);
        }

        return false;
    }

    /**
     * Moves randomly. We'll get there eventually!
     *
     * @return {@code true} - if a move was made.
     */
    public boolean randomWalk() {
        /* Create a shuffled list of deltas. */

        List list = Arrays.asList(deltas.clone());
        Collections.shuffle(list);
        Iterator iter = list.iterator();

        /* Try each one in turn. */

        while (iter.hasNext()) {
            Object n = game.moveDelta((Dimension) iter.next());
            if (n != null) {
                mque.push(n);
                return true;
            }
        }

        throw new RuntimeException("randomWalk");
    }

    /**
     * Pushes a composite move.
     *
     * @param imov move to push.
     * @see #pushMove(int, int)
     */
    protected void pushMove(int imov) {
        pushMove(imov, ROT0);
    }

    /**
     * Pushes a rotated composite move.
     *
     * @param imov composite move.
     * @param irot rotation.
     */
    protected void pushMove(int imov, int irot) {
        for (int j = 0; j < moves[imov].length; j++) {
            int dir = (moves[imov][j] + irot) % NDIR;
            pushMove(deltas[dir]);
        }
    }

    /**
     * Pops a composite move.
     *
     * @param imov move to undo
     * @see #undoMove(int, int)
     */
    protected void undoMove(int imov) {
        undoMove(imov, ROT0);
    }

    /**
     * Pops a rotated composite move.
     *
     * @param imov composite move.
     * @param irot rotation.
     */
    protected void undoMove(int imov, int irot) {
        /* Do the move backwards, looking in a mirror. */
        for (int j = moves[imov].length; j-- > 0; ) {
            int dir = (moves[imov][j] + irot + ROT180) % NDIR;
            undoMove(deltas[dir]);
        }
    }

    /**
     * Pushes a move.
     *
     * @param piece piece to move.
     */
    protected final void pushMove(Object piece) {
        pushMove(game.delta(piece));
    }

    /**
     * Pops a move.
     *
     * @param piece piece to move.
     */
    protected final void undoMove(Object piece) {
        undoMove(game.delta(piece));
    }

    /**
     * Pushes a simple move.
     *
     * @param delta direction to move.
     */
    protected final void pushMove(Dimension delta) {
        /* move & filter do-nothing moves */
        mque.push(game.moveDelta(delta), true);
    }

    /**
     * Pops a simple move.
     *
     * @param delta direction to move.
     */
    protected final void undoMove(Dimension delta) {
        /* undo, regenerating elided moves */
        mque.dequeue(game.moveDelta(delta));
    }

}

