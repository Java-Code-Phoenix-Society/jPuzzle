package dev.jcps.jpuzzle;
/*
 * Board.java
 *
 * Licensed Materials - See the file license.txt.
 * Copyright (c) Joseph Bowbeer 1998, 1999. All rights reserved.
 */

import java.awt.*;
import java.util.*;

/**
 * Maps Puzzle pieces to their column and row coordinates.
 * The <i>hole</i> piece is represented by {@code null}.
 *
 * @author <a href="mailto:jozart@csi.com">Joseph Bowbeer</a>
 * @version 1.2
 */
public class Board implements java.io.Serializable {
    /**
     * Source of random integers.
     */
    private static final Random random = new Random();


    /**
     * Maps rotations to column, row transforms.
     */
    private static final Point[][] trans =
            {
                    {new Point(1, 0), new Point(0, 1)},   // ROT0
                    {new Point(0, 1), new Point(-1, 0)},   // ROT90
                    {new Point(-1, 0), new Point(0, -1)},   // ROT180
                    {new Point(0, -1), new Point(1, 0)},   // ROT270
            };

    /**
     * Number of columns in root board.
     *
     * @serial
     */
    private int nCols;

    /**
     * Number of rows in root board.
     *
     * @serial
     */
    private int nRows;

    /**
     * Maps points to pieces. Shared by all views.
     *
     * @serial
     */
    private Object[][] pieces;

    /**
     * Maps pieces to points. Shared by all views.
     *
     * @serial
     */
    private Map<Object, Point> points;

    /**
     * Origin of this view, relative to parent board.
     *
     * @serial
     */
    private Point base;

    /**
     * Size of this view: {@code width} is number of columns,
     * {@code height} is number of rows.
     *
     * @serial
     */
    private Dimension size;

    /**
     * Orientation of this view measured in quarter-turns.
     *
     * @serial
     */
    private int rot;

    /**
     * Constructs a {@code 0} x {@code 0} board.
     */
    protected Board() {
        this(0, 0);
    }

    /**
     * Constructs a {@code nCols} x {@code nRows} board and
     * assigns the pieces to their default positions.
     *
     * @param nCols number of columns.
     * @param nRows number of rows.
     */
    public Board(int nCols, int nRows) {
        this(nCols, nRows, true);
    }

    /**
     * Constructs a {@code nCols} x {@code nRows} board and
     * optionally assigns the pieces to their default positions.
     *
     * @param nCols  number of columns.
     * @param nRows  number of rows.
     * @param assign if {@code true} assign initial piece positions.
     */
    protected Board(int nCols, int nRows, boolean assign) {
        this.nCols = nCols;
        this.nRows = nRows;

        pieces = new Object[nCols][nRows];
        points = new HashMap<>();
        base = new Point();
        size = new Dimension(nCols, nRows);
        rot = Rotation.ROT0;

        if (!assign) return;

        /* Assign pieces. */

        int k = 0;
        for (int j = 0; j < nRows; j++) {
            for (int i = 0; i < nCols; i++) {
                Object n = ++k;
                pieces[i][j] = n;
                points.put(n, new Point(i, j));
            }
        }

        /* Place hole (null) in last slot. */

        if (nCols > 0 && nRows > 0) {
            Object n = pieces[nCols - 1][nRows - 1];
            pieces[nCols - 1][nRows - 1] = null;
            Point p = points.remove(n);
            points.put(null, p);
        }
    }

    public Board(Board original) {
        this.nCols = original.nCols;
        this.nRows = original.nRows;
        this.pieces = original.pieces.clone();
        for (int i = 0; i < original.pieces.length; i++) {
            this.pieces[i] = original.pieces[i].clone();
        }
        this.points = new HashMap<>(original.points);
        this.base = new Point(original.base);
        this.size = new Dimension(original.size);
        this.rot = original.rot;
    }

    /**
     * Rotates a point clockwise about the origin.
     *
     * @param p    point.
     * @param iRot number of quarter-turns.
     * @return the rotated point.
     */
    private static Point rotate(Point p, int iRot) {
        Point p0 = trans[iRot][0];
        Point p1 = trans[iRot][1];
        return new Point(
                p.x * p0.x + p.y * p0.y,
                p.x * p1.x + p.y * p1.y);
    }

    /**
     * Rotates a point counterclockwise about the origin.
     *
     * @param p    point.
     * @param iRot number of quarter-turns.
     * @return the rotated point.
     */
    private static Point invRotate(Point p, int iRot) {
        return rotate(p, (Rotation.NROT - iRot) % Rotation.NROT);
    }

    private static void doNSwaps(Object[] top, int nSwaps, Board b) {
        if ((nSwaps & 1) != 0) {
            final int len = b.size.width * b.size.height;

            Object n0 = null;
            Object n1 = null;

            int k = top.length;
            for (; k < len && n0 == null; k++) {
                Point pk = new Point(b.col(k), b.row(k));
                n0 = b.pieceAt(pk);
            }

            for (; k < len && n1 == null; k++) {
                Point pk = new Point(b.col(k), b.row(k));
                n1 = b.pieceAt(pk);
            }

            if (n0 == null || n1 == null) {
                //throw new RuntimeException("assign")
                return;
            }

            b.swap(n0, n1);
        }
    }

    /**
     * Returns the size of this view.
     *
     * @return size: {@code width} is number
     * of columns, {@code height} is number of rows.
     */
    public Dimension getSize() {
        return new Dimension(size);
    }


    /**
     * Factory method for creating a deep copy of a Board object.
     *
     * @param original The original Board object to be copied.
     * @return A deep copy of the original Board object.
     */
    public Board copy(Board original) {
        return new Board(original);
    }

    /**
     * Checks if the pieces in this board match the corresponding
     * pieces in another board.
     *
     * @param b another board to check against.
     * @return {@code true} - if the two boards match.
     * @throws IllegalArgumentException if the board to match has fewer
     *                                  rows or columns than this board.
     */
    public boolean matches(Board b) {
        if (b == this)
            return true;

        if (b == null ||
                b.size.width < size.width ||
                b.size.height < size.height) {
            throw new IllegalArgumentException("matches");
        }

        Point c = new Point();
        for (c.y = 0; c.y < size.height; c.y++) {
            for (c.x = 0; c.x < size.width; c.x++) {
                Object p = pieceAt(c);
                Object q = b.pieceAt(c);

                if (!Objects.equals(p, q))
                    return false;
            }
        }

        return true;
    }

    /**
     * Creates a shifted, clipped view of this board.
     *
     * @param newBase origin of view.
     * @param newSize size of view.
     * @return the new board.
     * @throws IllegalArgumentException if the parameters do not
     *                                  define a valid view.
     * @see #transform(Point, Dimension, int)
     */
    public Board transform(Point newBase, Dimension newSize) {
        return transform(newBase, newSize, Rotation.ROT0);
    }

    /**
     * Creates a shifted, clipped, rotated view of this board.
     * This board and the view returned share piece mappings: a
     * piece moved in the view will be reflected in this board.
     *
     * @param newBase origin of view.
     * @param newSize size of view.
     * @param newRot  rotation of view.
     * @return the new board.
     * @throws IllegalArgumentException if the parameters do not
     *                                  define a valid view.
     */
    public Board transform(Point newBase, Dimension newSize, int newRot) {
        Board b = new Board();
        b.nCols = nCols;
        b.nRows = nRows;
        b.pieces = pieces;
        b.points = points;

        b.base = invRotate(newBase, rot);
        b.base.translate(base.x, base.y);
        b.size = new Dimension(newSize);
        b.rot = (rot + newRot) % Rotation.NROT;

        /* Check extents. */

        Point p = new Point(b.size.width - 1, b.size.height - 1);
        p = invRotate(p, b.rot);
        p.translate(b.base.x, b.base.y);
        if (b.base.x < 0 || b.base.x >= b.nCols ||
                b.base.y < 0 || b.base.y >= b.nRows ||
                p.x < 0 || p.x >= b.nCols ||
                p.y < 0 || p.y >= b.nRows) {
            throw new IllegalArgumentException("invalid transform");
        }

        return b;
    }

    /**
     * Returns the location of the given piece.
     *
     * @param piece the piece whose location is sought.
     * @return the location: {@code x} is the column
     * coordinate, {@code y} is the row coordinate.
     * @throws IllegalArgumentException if the piece cannot
     *                                  be found or if its location is outside this view.
     */
    public Point location(Object piece) {
        Point p = points.get(piece);

        if (p == null) {
            throw new IllegalArgumentException("location(" + piece + ")");
        }

        Point c = new Point(p);
        c.translate(-base.x, -base.y);
        c = rotate(c, rot);

        if (c.x < 0 || c.x >= size.width ||
                c.y < 0 || c.y >= size.height) {
            throw new IllegalArgumentException("location " + c);
        }

        return c;
    }

    /**
     * Returns the piece at the given location.
     *
     * @param p location.
     * @return the piece at the given location.
     */
    public Object pieceAt(Point p) {
        if (p.x < 0 || p.x >= size.width ||
                p.y < 0 || p.y >= size.height) {
            return null; // out of bounds
        }

        Point c = invRotate(p, rot);
        c.translate(base.x, base.y);

        return pieces[c.x][c.y];
    }

    /**
     * Returns a collection of this board's pieces.
     * The collection is unmodifiable and sorted in
     * row-major order.
     *
     * @return a collection of the pieces.
     */
    public Collection<Object> pieces() {
        return new Pieces();
    }

    /**
     * Converts a piece index into a column coordinate.
     *
     * @param k piece index.
     * @return column coordinate.
     */
    private int col(int k) {
        return k % size.width;
    }

    /**
     * Converts a piece index into a row coordinate.
     *
     * @param k piece index.
     * @return row coordinate.
     */
    private int row(int k) {
        return k / size.width;
    }

    /**
     * Checks if a piece can be moved toward the hole slot.
     * The piece can be moved if it lies in the same column or
     * row as the hole.
     *
     * @param piece the piece to check.
     * @return {@code true} - if the piece can be moved.
     */
    public boolean canMove(Object piece) {
        return (piece != null) && canMove(location(piece));
    }

    /**
     * Checks if the piece at a given location can be moved
     * toward the hole.
     */
    private boolean canMove(Point p) {
        Dimension d = delta(p);
        return (d.width == 0 || d.height == 0);
    }

    /**
     * Moves the piece. The piece must be adjacent to the hole.
     *
     * @param piece the piece to move.
     * @return {@code piece}.
     * @throws IllegalArgumentException if the piece is not
     *                                  adjacent to the hole.
     */
    public Object move(Object piece) {
        return move(location(piece));
    }

    /**
     * Moves the piece at the given location into the hole slot.
     */
    private Object move(Point p) {
        return moveDelta(delta(p));
    }

    /**
     * Returns the <i>delta</i> between the given piece and the hole.
     * The delta is the column and row distance between the hole
     * and the piece, stored in a Dimension object.
     *
     * @param piece piece to measure.
     * @return delta: {@code width} is the column distance;
     * {@code height} is the row distance.
     */
    public Dimension delta(Object piece) {
        return delta(location(piece));
    }

    /**
     * Returns the <i>delta</i> between a given location and the hole.
     */
    private Dimension delta(Point p) {
        Point p0 = location(null);
        int dCol = p.x - p0.x;
        int dRow = p.y - p0.y;
        return new Dimension(dCol, dRow);
    }

    /**
     * Moves the piece at the location specified by the given delta.
     * The piece must be adjacent to the hole.
     *
     * @param d delta.
     * @return the piece moved, or {@code null} if the
     * location is off limits (boundary condition).
     * @throws IllegalArgumentException if the delta does not
     *                                  correspond to a location adjacent to the hole.
     */
    public Object moveDelta(Dimension d) {
        return moveDelta(d.width, d.height);
    }

    /**
     * Moves the piece at the location corresponding to the
     * given column and row distances.
     */
    private Object moveDelta(int dCol, int dRow) {
        Object piece = nMoveDelta(dCol, dRow);
        if (piece != null) {
            swap(piece, null);
        }
        return piece;
    }

    /**
     * Finds the piece adjacent to the hole at the location
     * indicated by the given delta.
     *
     * @param d delta.
     * @return the piece adjacent to the hole, or {@code null} if
     * the location is off limits (boundary condition).
     * @throws IllegalArgumentException if the delta does not
     *                                  correspond to a location adjacent to the hole.
     * @see #nMoveDelta(int, int)
     */
    public Object nMoveDelta(Dimension d) {
        return nMoveDelta(d.width, d.height);
    }

    /**
     * Finds the piece adjacent to the hole at the location
     * indicated by the given column and row distances.
     *
     * @param dCol column distance.
     * @param dRow row distance.
     * @return the piece adjacent to the hole, or {@code null} if
     * the location is off limits (boundary condition).
     * @throws IllegalArgumentException if the distances do not
     *                                  correspond to a location adjacent to the hole.
     */
    public Object nMoveDelta(int dCol, int dRow) {
        if (Math.abs(dCol) + Math.abs(dRow) != 1) {
            throw new IllegalArgumentException("nMoveDelta(" + dCol + "," + dRow + ")");
        }

        Point p = location(null);
        p.translate(dCol, dRow);
        return pieceAt(p);
    }

    /**
     * Moves the hole to the given location. The new location
     * can be anywhere on the board.
     *
     * @param p new hole location.
     */
    public void moveHole(Point p) {
        while (!p.equals(location(null))) {
            move(nMoveHole(p));
        }
    }

    /**
     * Finds the piece that should move in order for the
     * hole to move closer to the location of the given piece.
     *
     * @param piece the piece located where the hole should be.
     * @return the piece to move.
     * @see #nMoveHole(Point)
     */
    public Object nMoveHole(Object piece) {
        return nMoveHole(location(piece));
    }

    /**
     * Finds the piece that should move in order for the
     * hole to move closer to the given location.
     *
     * @param p the desired hole location.
     * @return the piece to move.
     */
    public Object nMoveHole(Point p) {
        return nMoveDelta(deltaMove(p));
    }

    /**
     * Returns the <i>delta</i> corresponding to a simple move of
     * the hole toward the given location.
     * <p>
     * NOTE: The sequence of moves obtained by repeatedly calling
     * {@code deltaMove} for the same point can be backtracked
     * by repeatedly calling {@code deltaMove} for the original
     * hole location.
     * <p>
     *
     * @param p the desired hole location.
     * @return delta: {@code width} is the column distance;
     * {@code height} is the row distance.
     */
    private Dimension deltaMove(Point p) {
        Dimension d = delta(p);

        /*
         * Make one step towards p
         *  -row first, then col, then +row
         */

        if (d.height < 0) {
            d.width = 0;
            d.height = -1;
        } else if (d.width != 0) {
            d.width = (d.width < 0) ? -1 : 1;
            d.height = 0;
        } else if (d.height > 0) {
            d.height = 1;
        }
        return d;
    }

    /**
     * Swaps the location of two pieces.
     *
     * @param a first piece
     * @param b second piece
     * @throws IllegalArgumentException if the two pieces are identical.
     */
    private void swap(Object a, Object b) {
        if (a == b) {
            throw new IllegalArgumentException("swap(" + a + "," + b + ")");
        }

        Point pa = points.get(a);
        Point pb = points.get(b);

        pieces[pa.x][pa.y] = b;
        pieces[pb.x][pb.y] = a;
        points.put(a, pb);
        points.put(b, pa);
    }

    /**
     * Creates a scrambled copy of this board.
     *
     * @return the scrambled copy.
     * @see #scramble(boolean)
     */
    public Board scramble() {
        return scramble(true);
    }

    /**
     * Creates a scrambled copy of this board.
     * Optionally scramble the hole location, too.
     *
     * @param bMoveHole whether to scramble the hole location.
     * @return the scrambled copy.
     */
    public Board scramble(boolean bMoveHole) {
        Board b = this.copy(this);

        final int len = b.size.width * b.size.height - 1; /* not hole */

        /*
         * For each piece, swap with a remaining piece,
         *  including itself.
         */

        int nSwaps = 0;
        for (int n = 0; n < len - 1; n++) {
            Point pn = new Point(b.col(n), b.row(n));
            Object sn = b.pieceAt(pn);

            // assert hole is last and stays there
            if (sn == null) {
                //throw new RuntimeException("scramble")
                return null;
            }

            int k = n + random.nextInt(len - n);

            if (k != n) {
                Point pk = new Point(b.col(k), b.row(k));
                Object sk = b.pieceAt(pk);

                // assert hole is last and stays there
                if (sk == null) {
                    //throw new RuntimeException("scramble")
                    return null;
                }

                b.swap(sn, sk);
                nSwaps++;
            }
        }

        /* If nSwaps is odd then swap once more. */

        if ((nSwaps & 1) != 0) {
            Point p = new Point(b.col(1), b.row(1));
            b.swap(b.pieceAt(new Point(0, 0)), b.pieceAt(p));
        }

        if (bMoveHole && len > 0) {
            Point p = new Point(random.nextInt(b.size.width),
                    random.nextInt(b.size.height));
            b.moveHole(p);
        }

        return b;
    }

    /**
     * Creates a solvable goal assignment for this board given
     * piece assignments for the top row. The remaining
     * pieces from this board will be assigned to locations in
     * the goal such that the resulting board is <i>solvable</i>,
     * that is, it is reachable from the current configuration.
     *
     * @param top piece assignments for the top row of the goal.
     * @return the new board.
     */
    public Board assign(Object[] top) {
        Board b = new Board(size.width, size.height, false);

        /* Copy piece assignments. */

        for (int j = 0; j < size.height; j++) {
            for (int i = 0; i < size.width; i++) {
                Point p = new Point(i, j);
                Object n = pieceAt(p);
                b.pieces[i][j] = n;
                b.points.put(n, p);
            }
        }

        /* Top pieces are fixed: swap them into place. */

        int nSwaps = 0;
        Point p = new Point();
        for (p.x = 0; p.x < top.length; p.x++) {
            if (!p.equals(b.location(top[p.x]))) {
                Object n = b.pieceAt(p);

                // assert hole is not in top row
                if (n == null) {
                    //throw new RuntimeException("assign")
                    return null;
                }

                b.swap(n, top[p.x]);
                nSwaps++;
            }
        }

        /*
         * If nSwaps is odd then swap two unfixed pieces.
         *
         * NOTE: To produce the same goal each time, the
         * pieces could be exchange-sorted before starting,
         * keeping a count of swaps made during the sort.
         */
        doNSwaps(top, nSwaps, b);

        return b;
    }

    /**
     * An unmodifiable collection of pieces in row-major order.
     */
    private class Pieces extends AbstractCollection<Object> {
        private final int len = size.width * size.height;

        public int size() {
            return len;
        }

        public Iterator<Object> iterator() {
            return new Iterator<>() {
                private int k = 0;

                public boolean hasNext() {
                    return k < len;
                }

                public Object next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    Point p = new Point(col(k), row(k));
                    k++;
                    return pieceAt(p);
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }
}
