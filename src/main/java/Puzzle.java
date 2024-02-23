/*
 * Puzzle.java
 *
 * Licensed Materials - See the file license.txt.
 * Copyright (c) Joseph Bowbeer 1998, 1999. All rights reserved.
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Puzzle JavaBean. Puzzle provides:
 * <ul>
 * <li> A customizable sliding block puzzle.
 * <li> Support for images, including animated GIFs.
 * <li> An automated rule-based solver.
 * <li> A reversible move queue.
 * </ul>
 * Java 2 and Swing 1.1 required.
 * <p>
 * About the solver: The solver was first coded in Prolog for
 * Donald Michie's AI class at the Univ. of Illinois in 1980.
 * It is based on a rule-based solver for the 5-puzzle
 * designed by Donald Michie, and a decomposition technique
 * designed by Philip Krause, J.&nbsp;O'Brien, M.&nbsp;Tuceiryan and
 * J.&nbsp;Bowbeer. The decomposition technique has since been
 * refined by J.&nbsp;Bowbeer.
 *
 * @author Joseph Bowbeer
 *         <a href="mailto:jozart@csi.com">jozart@csi.com</a>
 * @version 1.2
 */
public class Puzzle extends JComponent {
    /**
     * Default text color.
     */
    private static final Color DEF_TEXT_COLOR = Color.orange;
    /**
     * Default text outline color.
     */
    private static final Color DEF_LINE_COLOR = Color.black;

    /**
     * Game board.
     *
     * @serial
     */
    private Board game;

    /**
     * Goal board.
     *
     * @serial
     */
    private Board goal;

    /**
     * Move queue.
     *
     * @serial
     */
    private MoveQ mque;

    /**
     * Maps board pieces to button components.
     *
     * @serial
     */
    private Map pieces;

    /**
     * Solver instance.
     *
     * @serial
     */
    private Solver solver;

    /**
     * Timer for automatic solver.
     *
     * @serial
     */
    private Timer timer;

    /**
     * @serial
     */
    private int cols = 4;

    /**
     * @serial
     */
    private int rows = 4;

    /**
     * @serial
     */
    private int bevelHeight = 1;

    /**
     * @serial
     */
    private int gap = 0;

    /**
     * @serial
     */
    private Icon icon;

    /**
     * @serial
     */
    private int delay = 500;

    /**
     * @serial
     */
    private boolean solved = true;

    /**
     * @serial
     */
    private boolean history;

    /**
     * @serial
     */
    private boolean textPainted = true;

    /**
     * @serial
     */
    private boolean textAntiAliased = true;

    /**
     * @serial
     */
    private boolean textOutlined = true;

    /**
     * @serial
     */
    private boolean textOpaque = true;

    /**
     * @serial
     */
    private Color textColor = DEF_TEXT_COLOR;

    /**
     * @serial
     */
    private Color textOutlineColor = DEF_LINE_COLOR;

    /**
     * Constructs a new Puzzle JavaBean.
     */
    public Puzzle() {
        setDoubleBuffered(true);
        setOpaque(true);
        setBackground(Color.darkGray);
        setForeground(Color.gray); // piece color
        setFont(new Font("SansSerif", Font.BOLD, 24));
        /*
         * NOTE: There are no L&F variants of PuzzleUI so we
         * don't implement getUI/setUI/updateUI. In addition,
         * we don't need uiClassID/getUIClassID/writeObject
         * because PuzzleUI doesn't installUI/uninstallUI.
         */
        setUI(PuzzleUI.createUI(this));
    }

    /**
     * Launches Puzzle in a simple test frame. For testing only.
     *
     * @param args command line arguments (ignored)
     * @see #main(String[])
     * @see #addNotify()
     * @see #removeNotify()
     * @see #createBoard()
     */
    public static void main(String[] args) {
        JFrame frame = new JFrame("Puzzle");
        Puzzle pz = new Puzzle();
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {
                System.exit(0);
            }
        });
        frame.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                super.keyTyped(e);
                if (e.getKeyChar() == 's') {
                    pz.startSolving();
                }
            }
        });
        frame.getContentPane().add(pz, "Center");
        frame.pack();
        pz.scramble();
        frame.setVisible(true);

    }

    /**
     * Overridden to create the board when the puzzle is realized.
     * <p>
     * This method is called internally and should not be called
     * directly by programs.
     */
    public void addNotify() {
        if (game == null) {
            createBoard();
        }
        super.addNotify();
    }

    /**
     * Sets the text font. Overridden to repaint the pieces when
     * the font changes.
     */
    public void setFont(Font font) {
        Font oldFont = getFont();
        super.setFont(font);
        if (font != oldFont) {
            repaint();
        }
    }

    /**
     * Retrieves the number of columns.
     *
     * @return The number of columns
     * @see #setColumns(int)
     */
    public int getColumns() {
        return cols;
    }

    /**
     * Sets the number of columns in the puzzle.
     * <p>
     * The default value of this property is 4 columns.
     * <p>
     * This is a JavaBeans bound property.
     *
     * @param value The number of columns to set
     * @throws IllegalArgumentException if the specified number of columns is not positive
     * @see #getColumns()
     */
    public void setColumns(int value) {
        if (value <= 0) {
            throw new IllegalArgumentException(value + "");
        }
        int oldValue = cols;
        cols = value;
        if (oldValue != value) {
            updateBoard();
        }
        firePropertyChange("columns", oldValue, value);
    }

    /**
     * Retrieves the number of rows.
     *
     * @return The number of rows
     * @see #setRows(int)
     */
    public int getRows() {
        return rows;
    }


    /**
     * Sets the number of rows.
     * <p>
     * The default value of this property is 4 rows.
     * <p>
     * This is a JavaBeans bound property.
     *
     * @param value The number of rows in the puzzle.
     * @see #getRows()
     */
    public void setRows(int value) {
        if (value <= 0) {
            throw new IllegalArgumentException(value + "");
        }
        int oldValue = rows;
        rows = value;
        if (oldValue != value) {
            updateBoard();
        }
        firePropertyChange("rows", oldValue, value);
    }

    /**
     * Returns the height of the bevel border.
     *
     * @return the bevel height
     * @see #setBevelHeight(int)
     */
    public int getBevelHeight() {
        return bevelHeight;
    }

    /**
     * Sets the height of the bevel border.
     * If the bevel height is 0, no border is painted.
     * <p>
     * The default value of this property is 1 pixel.
     * <p>
     * This is a JavaBeans bound property.
     *
     * @param value The height of the bevel border
     * @throws IllegalArgumentException if the specified bevel height value is negative
     * @see #getBevelHeight()
     */
    public void setBevelHeight(int value) {
        if (value < 0) {
            throw new IllegalArgumentException(value + "");
        }
        int oldValue = bevelHeight;
        bevelHeight = value;
        if (oldValue != value) {
            repaint();
        }
        firePropertyChange("bevelHeight", oldValue, value);
    }

    /**
     * Returns the amount of space between the pieces.
     *
     * @return the gap between pieces
     * @see #setGap(int)
     */
    public int getGap() {
        return gap;
    }

    /**
     * Sets the space between the pieces.
     * <p>
     * The default value of this property is 0 pixels.
     * <p>
     * This is a JavaBeans bound property.
     *
     * @param value The gap between pieces
     * @throws IllegalArgumentException if the specified gap value is negative
     * @see #getGap()
     */
    public void setGap(int value) {
        if (value < 0) {
            throw new IllegalArgumentException(value + "");
        }
        int oldValue = gap;
        gap = value;
        if (oldValue != value && game != null) {
            GridLayout layout = (GridLayout) getLayout();
            layout.setHgap(value);
            layout.setVgap(value);
            layout.layoutContainer(this);
        }
        firePropertyChange("gap", oldValue, value);
    }

    /**
     * Returns the graphic image that the puzzle displays.
     *
     * @return image the puzzle displays
     * @see #setIcon(Icon)
     */
    public Icon getIcon() {
        return icon;
    }

    /**
     * Sets the icon this puzzle will display. If the value is
     * {@code null}, no icon will be displayed.
     * <p>
     * The default value of this property is {@code null}.
     * <p>
     * This is a JavaBeans bound property.
     *
     * @param value The icon this puzzle will display.
     * @see #getIcon()
     */
    public void setIcon(Icon value) {
        Object oldValue = icon;
        synchronized (this) {
            /* Transmit new value to imageUpdate thread. */
            icon = value;
        }
        if (oldValue != value) {
            revalidate();
            repaint();
        }
        firePropertyChange("icon", oldValue, value);
    }

    /**
     * Overridden to stop repainting when the updated image is no
     * longer the current image.
     * <p>
     * This method is called internally and should not be called
     * directly by programs.
     */
    public boolean imageUpdate(Image image, int flags, int x, int y, int w, int h) {
        if (image == getImage()) {
            return super.imageUpdate(image, flags, x, y, w, h);
        }
        return false;
    }

    /**
     * Returns the image obtained from the current icon.
     * <p>
     * NOTE: This method is called in the event thread when
     * the buttons are painted and is also called by
     * {@link #imageUpdate(Image, int, int, int, int, int) imageUpdate},
     * which may run in an image production thread.
     * </p>
     *
     * @return An Image if available or null
     * @see #getIcon()
     * @see #setIcon(Icon)
     */
    protected synchronized Image getImage() {
        /* Receive icon from event thread. */
        if (icon instanceof ImageIcon) {
            return ((ImageIcon) icon).getImage();
        }
        return null;
    }

    /**
     * Returns the timer delay used by the automatic solver.
     *
     * @return the delay in milliseconds
     * @see #setDelay(int)
     */
    public int getDelay() {
        return delay;
    }

    /**
     * Sets the timer delay used by the automatic solver.
     * The timer delay controls how quickly the solver operates.
     * If the delay is 0, the solver runs at maximum speed.
     * <p>
     * The default value of this property is 500 milliseconds.
     * <p>
     * This is a JavaBeans bound property.
     *
     * @param value The timer delay in milliseconds
     * @throws IllegalArgumentException if the specified delay value is negative
     * @see #getDelay()
     */
    public void setDelay(int value) {
        if (value < 0) {
            throw new IllegalArgumentException(value + "");
        }
        int oldValue = delay;
        delay = value;
        if (oldValue != value && timer != null) {
            timer.setDelay(value);
        }
        firePropertyChange("delay", oldValue, value);
    }

    /**
     * Checks whether the puzzle is in a solved state
     *
     * @return True if the puzzle is solved, false otherwise
     */
    public boolean isSolved() {
        return solved;
    }

    /**
     * Sets the solved property.
     *
     * @see #isSolved()
     */
    private void setSolved(boolean value) {
        boolean oldValue = solved;
        solved = value;
        firePropertyChange("solved", oldValue, value);
    }

    /**
     * Checks whether any moves have been saved on the history stack.
     * <p>
     * This is a JavaBeans bound, read-only property.
     * <p>
     * bound: true
     * description: True if moves have been saved on the history stack.
     *
     * @return True if moves have been saved on the history stack.
     */
    public boolean isHistory() {
        return history;
    }

    /**
     * Sets the history property.
     *
     * @see #isHistory()
     */
    private void setHistory(boolean value) {
        boolean oldValue = history;
        history = value;
        firePropertyChange("history", oldValue, value);
    }

    /**
     * Returns whether the text should be painted.
     *
     * @return if painted or not.
     * @see #setTextPainted(boolean)
     */
    public boolean isTextPainted() {
        return textPainted;
    }

    /**
     * Sets whether the text should be painted. The text in this
     * case is a numeric label. If the value is {@code true},
     * the pieces are labeled; otherwise not.
     * <p>
     * The default value of this property is {@code true}.
     * <p>
     * This is a JavaBeans bound property.
     * <p>
     * bound: true
     * attribute: visualUpdate true
     * description: Whether the text should be painted.
     *
     * @param value Whether the text should be painted.
     * @see #isTextPainted()
     */
    public void setTextPainted(boolean value) {
        boolean oldValue = textPainted;
        textPainted = value;
        if (oldValue != value) {
            repaint();
        }
        firePropertyChange("textPainted", oldValue, value);
    }

    /**
     * Returns whether the text should be anti-aliased.
     *
     * @return if it is on or off
     * @see #setTextAntiAliased(boolean)
     */
    public boolean isTextAntiAliased() {
        return textAntiAliased;
    }

    /**
     * Sets whether the text should be anti-aliased.
     * <p>
     * The default value of this property is {@code true}.
     * <p>
     * This is a JavaBeans bound property.
     * <p>
     * bound: true
     * attribute: visualUpdate true
     * description: Whether the text should be anti-aliased.
     *
     * @param value true for on, false for off
     * @see #isTextAntiAliased()
     */
    public void setTextAntiAliased(boolean value) {
        boolean oldValue = textAntiAliased;
        textAntiAliased = value;
        if (oldValue != value) {
            repaint();
        }
        firePropertyChange("textAntiAliased", oldValue, value);
    }

    /**
     * Returns whether the text outline should be painted.
     *
     * @return true for outline, false for not.
     * @see #setTextOutlined(boolean)
     */
    public boolean isTextOutlined() {
        return textOutlined;
    }

    /**
     * Sets whether the text outline should be painted.
     * <p>
     * The default value of this property is {@code true}.
     * <p>
     * This is a JavaBeans bound property.
     * <p>
     * bound: true
     * attribute: visualUpdate true
     * description: Whether the text outline should be painted.
     *
     * @param value set for on or off
     * @see #isTextOutlined()
     */
    public void setTextOutlined(boolean value) {
        boolean oldValue = textOutlined;
        textOutlined = value;
        if (oldValue != value) {
            repaint();
        }
        firePropertyChange("textOutlined", oldValue, value);
    }

    /**
     * Returns whether the text should be opaque.
     *
     * @return true for Opaque, false if not.
     * @see #setTextOpaque(boolean)
     */
    public boolean isTextOpaque() {
        return textOpaque;
    }

    /**
     * Sets whether the text should be opaque. If the value is
     * {@code true}, the text is opaque; otherwise the text
     * is translucent.
     * <p>
     * The default value of this property is {@code true}.
     * <p>
     * This is a JavaBeans bound property.
     * <p>
     * bound: true
     * attribute: visualUpdate true
     * description: Whether the text should be opaque.
     *
     * @param value set Opaqueness to input value
     * @see #isTextOpaque()
     */
    public void setTextOpaque(boolean value) {
        boolean oldValue = textOpaque;
        textOpaque = value;
        if (oldValue != value) {
            repaint();
        }
        firePropertyChange("textOpaque", oldValue, value);
    }

    /**
     * Returns the text color.
     *
     * @return text color
     * @see #setTextColor(Color)
     */
    public Color getTextColor() {
        return textColor;
    }

    /**
     * Sets the text color.
     * <p>
     * The default value of this property is {@code Color.orange}.
     * <p>
     * This is a JavaBeans bound property.
     * <p>
     * bound: true
     * attribute: visualUpdate true
     * description: The text color.
     *
     * @param value Color to set
     * @see #getTextColor()
     */
    public void setTextColor(Color value) {
        if (value == null) {
            value = DEF_TEXT_COLOR; // restore default
        }
        Color oldValue = textColor;
        textColor = value;
        if (!value.equals(oldValue)) {
            repaint();
        }
        firePropertyChange("textColor", oldValue, value);
    }

    /**
     * Returns the text outline color.
     *
     * @return textOutlineColor value
     * @see #setTextOutlineColor(Color)
     */
    public Color getTextOutlineColor() {
        return textOutlineColor;
    }

    /**
     * Sets the text outline color.
     * <p>
     * The default value of this property is {@code Color.black}.
     * <p>
     * This is a JavaBeans bound property.
     * <p>
     * bound: true
     * attribute: visualUpdate true
     * description: The text outline color.
     *
     * @param value input Color
     * @see #getTextOutlineColor()
     */
    public void setTextOutlineColor(Color value) {
        if (value == null) {
            value = DEF_LINE_COLOR; // restore default
        }
        Color oldValue = textOutlineColor;
        textOutlineColor = value;
        if (!value.equals(oldValue)) {
            repaint();
        }
        firePropertyChange("textOutlineColor", oldValue, value);
    }

    /**
     * Creates the board.
     */
    private void createBoard() {
        /* Remove old pieces and hole. */

        if (pieces != null) {
            Iterator ci = pieces.values().iterator();
            pieces = null;
            while (ci.hasNext()) {
                Component c = (Component) ci.next();
                c.setVisible(false);
                remove(c);
            }
        }

        /* Create new pieces. */

        setLayout(new GridLayout(rows, cols, gap, gap));

        goal = new Board(cols, rows);
        game = (Board) goal.clone();
        pieces = new HashMap();

        Dimension dim = game.getSize();
        ActionListener listener = new PieceListener();

        Iterator pi = game.pieces().iterator();
        while (pi.hasNext()) {
            Object piece = pi.next();
            Point pos = goal.location(piece);

            PuzzleButton pb = new PuzzleButton();
            pieces.put(piece, pb);
            pb.addActionListener(listener);
            pb.setPosition(pos);
            pb.setVisible(piece != null);
            add(pb);
        }

        /* Reset moveq and solver. */

        mque = new MoveQ();
        solver = null;

        setHistory(mque.historySize() > 0);
        setSolved(game.matches(goal));
    }

    /**
     * Updates the board.
     */
    private void updateBoard() {
        if (game == null) return;

        createBoard();
        revalidate();
    }

    /**
     * Scrambles the game board and resets the move history.
     */
    public void scramble() {
        /* Save old pieces. */

        Board oldGame = game;
        Map oldPieces = pieces;

        /* Scramble game board. */

        game = goal.scramble();
        pieces = new HashMap();

        /* Reassign pieces. */

        Iterator pi = oldGame.pieces().iterator();
        while (pi.hasNext()) {
            Object oldpiece = pi.next();
            PuzzleButton pb = (PuzzleButton) oldPieces.get(oldpiece);

            Point p = oldGame.location(oldpiece);
            Object piece = game.pieceAt(p);
            pieces.put(piece, pb);

            Point pos = goal.location(piece);
            pb.setPosition(pos);
            pb.setVisible(piece != null);
        }

        /* Reset moveq and solver. */

        mque = new MoveQ();
        solver = null;

        setHistory(mque.historySize() > 0);
        setSolved(game.matches(goal));
    }

    /**
     * Starts the automatic solver.
     */
    public void startSolving() {
        if (timer != null) return; // already started

        /*
         * Solve one move and start a timer to continue
         * solving automatically.
         */

        moveForward();

        if (!isSolved()) {
            ActionListener callback = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    moveForward();
                }
            };

            timer = new Timer(delay, callback);
            timer.start();
        }
    }

    /**
     * Stops the automatic solver.
     */
    public void stopSolving() {
        if (timer == null) return;

        timer.stop();
        timer = null;
    }

    /**
     * Backs up one move. Reinstates the move at the head
     * of the pending list.
     */
    public void moveBackward() {
        if (mque.historySize() == 0) {
            thud();
            return;
        }
        moveBack();
    }

    /**
     * Executes the next pending move; the solver makes
     * the next move if no moves are pending.
     */
    public void moveForward() {
        /*
         * The solver runs a few moves ahead so that the elision
         * filter in the move queue has a chance to eliminate
         * useless moves. If the solver isn't currently active,
         * we must drain the pending moves before we start a new
         * solver, otherwise the new solver will be out of sync.
         */
        int n = (solver == null) ? 0 : 3;

        while (mque.pendingSize() <= n && solveNext()) {
        }
        nextMove();
    }

    /**
     * Requests the solver to push a few moves onto the
     * pending list. Creates a new solver if needed.
     *
     * @return {@code true} if any moves were pushed;
     * {@code false} otherwise.
     */
    private boolean solveNext() {
        if (isSolved()) return false;

        if (solver == null) {
            solver = new Solver((Board) game.clone(), goal, mque);
        }

        return solver.solveNext();
    }

    /**
     * Moves the piece at the specified location toward the hole.
     * The piece must lie in the same column or row as the hole.
     * Intervening pieces, if any, will also move.
     *
     * @param col target column
     * @param row target row
     */
    public void movePieceAt(int col, int row) {
        Object n = game.pieceAt(new Point(col, row));
        moveTowardHole(n);
    }

    /**
     * Moves the piece and any intervening pieces toward the hole.
     *
     * @param n target piece to move
     */
    private void moveTowardHole(Object n) {
        if (!game.canMove(n)) {
            thud();
            return;
        }

        while (movePiece(game.nMoveHole(n)) != n) {
        }
    }

    /**
     * Moves the hole left. The piece to the left of the
     * hole moves right...
     */
    public void moveHoleLeft() {
        moveHole(-1, 0);
    }

    /**
     * Moves the hole right. The piece to the left of the
     * hole moves left...
     */
    public void moveHoleRight() {
        moveHole(1, 0);
    }

    /**
     * Moves the hole up. The piece above the hole moves down...
     */
    public void moveHoleUp() {
        moveHole(0, -1);
    }

    /**
     * Moves the hole down. The piece above the hole moves up...
     */
    public void moveHoleDown() {
        moveHole(0, 1);
    }

    /**
     * Moves the hole in the direction indicated.
     */
    private Object moveHole(int dcol, int drow) {
        Object n = game.nMoveDelta(dcol, drow);

        if (n == null) {
            thud();
            return null;
        }

        return movePiece(n);
    }

    /**
     * The common locus for all user-initiated moves.
     * Reset the pending list and the solver. Move the
     * piece into the hole.
     *
     * @param n piece to move.
     * @return the piece.
     */
    private Object movePiece(Object n) {
        mque.clearPending();
        solver = null;

        mque.push(n);
        nextMove();
        return n;
    }

    /**
     * Undoes the previous move and reinstates it at the
     * head of the pending list. Updates the "history" and
     * "solved" properties.
     */
    private void moveBack() {
        move(mque.pull());

        setSolved(game.matches(goal));

        if (isSolved()) {
            /*
             * The user just backed into a solution.
             * Don't let 'em back up any farther.
             */
            mque.clearHistory();
        }

        setHistory(mque.historySize() > 0);
    }

    /**
     * Executes the next pending move and pushes it onto
     * the history stack. Updates the "history" and "solved"
     * properties. Stops the automatic solver if the puzzle
     * is solved.
     */
    private void nextMove() {
        move(mque.pop());

        setHistory(mque.historySize() > 0);
        setSolved(game.matches(goal));

        if (isSolved()) {
            stopSolving();
            mque.clearPending();
            solver = null;
        }
    }

    /**
     * Move implemention. Updates the game board and the buttons.
     *
     * @param n piece to move.
     */
    private void move(Object n) {
        if (n == null) return;

        game.move(n);

        /* Exchange hole and piece. */

        PuzzleButton pb = (PuzzleButton) pieces.get(null);
        PuzzleButton hole = (PuzzleButton) pieces.put(n, pb);
        pieces.put(null, hole);

        assert hole != null;
        hole.setVisible(false);
        pb.setPosition(hole.getPosition());
        pb.setVisible(true);
    }

    /**
     * Gives audible feedback when the user attempts an illegal move.
     * Override to customize.
     */
    protected void thud() {
        getToolkit().beep();
    }

    /**
     * An action listener for the puzzle pieces.
     */
    private class PieceListener implements ActionListener,
            java.io.Serializable {
        public void actionPerformed(ActionEvent e) {
            Object source = e.getSource();
            if (source instanceof PuzzleButton) {
                PuzzleButton pb = (PuzzleButton) source;
                Point p = pb.getPosition();
                Object n = goal.pieceAt(p);
                moveTowardHole(n);
            }
        }
    }

}

