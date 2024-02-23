/*
 * PuzzleButton.java
 *
 * Licensed Materials - See the file license.txt.
 * Copyright (c) Joseph Bowbeer 1998, 1999. All rights reserved.
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

/**
 * Button component for Puzzle pieces. Puzzle buttons are designed
 * to be created and manipulated by a Puzzle.
 * <p>
 * Puzzle buttons maintain their own <i>position</i> property, and
 * inherit their other properties from the Puzzle container. The
 * position determines the text label string and the section of the
 * Puzzle image that is drawn, but it doesn't affect the button's
 * location on the screen. In other words: the pictures change,
 * but the buttons stay put.
 * <p>
 * The properties inherited from the Puzzle container are:
 * <ul>
 * <li>background color (puzzle's foreground color)
 * <li>image (obtained from puzzle's icon)
 * <li>bevel height
 * <li>text font (puzzle's font)
 * <li>text color
 * <li>text outline color
 * <li>text painted
 * <li>text anti-aliased
 * <li>text outlined
 * <li>text opaque
 * </ul>
 *
 * @author <a href="mailto:jozart@csi.com">Joseph Bowbeer</a>
 * @version 1.2
 */
public class PuzzleButton extends JComponent {
    /**
     * Alpha composite for drawing translucent text.
     */
    private static final AlphaComposite translucent =
            AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);

    /**
     * Column position.
     *
     * @serial
     */
    private int col;

    /**
     * Row position.
     *
     * @serial
     */
    private int row;

    /**
     * This button's action listeners.
     *
     * @serial
     */
    private ActionListener actionListener;

    /* Transient button state. */
    private transient boolean pressed;
    private transient boolean down;

    /**
     * Constructs a new button.
     */
    protected PuzzleButton() {
        setOpaque(true);
        addMouseListener(new ButtonListener());
    }

    /**
     * Returns the position of this button.
     *
     * @return the position: {@code x} is column, {@code y} is row.
     * @see #setPosition(Point)
     */
    public Point getPosition() {
        return new Point(col, row);
    }

    /**
     * Assigns a new position.
     *
     * @param pos position: {@code x} is column, {@code y} is row.
     */
    public void setPosition(Point pos) {
        col = pos.x;
        row = pos.y;
        repaint();
    }

    /**
     * Paints the button. Paints the background, image, text label,
     * and bevel border, in that order.
     */
    protected void paintComponent(Graphics g) {
        Dimension size = getSize();

        Puzzle puzzle = (Puzzle) getParent();
        Color background = puzzle.getForeground();

        /* Paint background. */

        g.setColor(background);
        g.fillRect(0, 0, size.width, size.height);

        int bevelHeight = puzzle.getBevelHeight();
        int offs = down ? bevelHeight : 0;
        int cols = puzzle.getColumns();
        int rows = puzzle.getRows();

        /* Paint image. */

        Image image = puzzle.getImage();
        if (image != null) {
            int imgw = image.getWidth(null);
            int imgh = image.getHeight(null);
            int sx1 = col * imgw / cols;
            int sy1 = row * imgh / rows;
            int sx2 = sx1 + imgw / cols;
            int sy2 = sy1 + imgh / rows;

            /*
             * NOTE: Set puzzle as image observer so all pieces
             * are redrawn in sync when new data is available.
             */

            g.drawImage(image, offs, offs, offs + size.width, offs + size.height,
                    sx1, sy1, sx2, sy2, puzzle);
        }

        /* Paint text. */

        if (puzzle.isTextPainted()) {
            Graphics2D g2 = (Graphics2D) g;
            FontRenderContext frc = g2.getFontRenderContext();
            Font f = puzzle.getFont();
            String text = String.valueOf(row * cols + col + 1);
            TextLayout tl = new TextLayout(text, f, frc);
            Rectangle2D r2 = tl.getBounds();
            double x = offs + size.width / 2 - r2.getWidth() / 2;
            double y = offs + size.height / 2 + r2.getHeight() / 2;
            Shape shape = tl.getOutline(AffineTransform.getTranslateInstance(x, y));

            Object oldHint =
                    g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    puzzle.isTextAntiAliased() ?
                            RenderingHints.VALUE_ANTIALIAS_ON :
                            RenderingHints.VALUE_ANTIALIAS_OFF);

            Composite oldComposite = g2.getComposite();
            g2.setComposite(puzzle.isTextOpaque() ?
                    AlphaComposite.SrcOver : translucent);

            if (puzzle.isTextOutlined()) {
                g2.setColor(puzzle.getTextOutlineColor());
                g2.draw(shape);
            }

            g2.setColor(puzzle.getTextColor());
            g2.fill(shape);

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldHint);
            g2.setComposite(oldComposite);
        }

        /* Paint border. */

        for (int i = 0; i < bevelHeight; i++) {
            g.setColor(background);
            g.draw3DRect(i, i, size.width - 1 - (i + i), size.height - 1 - (i + i), !down);
        }
    }

    /**
     * Adds an action listener to the button.
     *
     * @param listener ActionListener to add
     */
    public void addActionListener(ActionListener listener) {
        actionListener = AWTEventMulticaster.add(actionListener, listener);
    }

    /**
     * Removes an action listener from the button.
     *
     * @param listener ActionListener to remove
     */
    public void removeActionListener(ActionListener listener) {
        actionListener = AWTEventMulticaster.remove(actionListener, listener);
    }

    /**
     * Notifies the action listeners.
     */
    protected void fireActionPerformed() {
        if (actionListener != null) {
            actionListener.actionPerformed(new ActionEvent(
                    this, ActionEvent.ACTION_PERFORMED, null));
        }
    }

    /**
     * Mouse listener implementation.
     * <p>
     * NOTE: {@code mouseExited} is not called while a button
     * is pressed. To behave more like a {@code JButton} we'd need
     * to implement {@code MouseMotionListener} and override
     * {@code mouseDragged}. Of course, we'd behave more like
     * a real button if we actually were a real button...
     *
     * @see javax.swing.plaf.basic.BasicButtonListener
     */
    private class ButtonListener implements MouseListener, java.io.Serializable {
        public void mouseClicked(MouseEvent e) {
        }

        public void mousePressed(MouseEvent e) {
            down = true;
            pressed = true;
            repaint();
        }

        public void mouseReleased(MouseEvent e) {
            pressed = false;
            if (down) {
                fireActionPerformed();
                down = false;
                repaint();
            }
        }

        public void mouseEntered(MouseEvent e) {
            if (pressed) {
                down = true;
                repaint();
            }
        }

        public void mouseExited(MouseEvent e) {
            if (down) {
                down = false;
                repaint();
            }
        }
    }
}
