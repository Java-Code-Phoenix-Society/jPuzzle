/*
 * PuzzleUI.java
 *
 * Licensed Materials - See the file license.txt.
 * Copyright (c) Joseph Bowbeer 1998, 1999. All rights reserved.
 */

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import java.awt.*;

/**
 * Provides a default preferred size and maximum size for Puzzle.
 *
 * @author <a href="mailto:jozart@csi.com">Joseph Bowbeer</a>
 * @version 1.2
 */
public class PuzzleUI extends ComponentUI {

    /**
     * The shared instance.
     */
    private static final PuzzleUI puzzleUI = new PuzzleUI();

    /**
     * @param c input JComponent
     * @return PuzzleUI instance.
     */
    public static ComponentUI createUI(JComponent c) {
        return puzzleUI;
    }

    /**
     * If the puzzle has an icon, returns the icon's size;
     * otherwise returns a small fixed size.
     *
     * @param c input JComponent
     * @return the preferred size.
     */
    public Dimension getPreferredSize(JComponent c) {
        Insets insets = c.getInsets();
        int dx = insets.left + insets.right;
        int dy = insets.top + insets.bottom;
        Icon icon = ((Puzzle) c).getIcon();
        if (icon != null) {
            return new Dimension(icon.getIconWidth() + dx,
                    icon.getIconHeight() + dy);
        } else {
            return new Dimension(180 + dx, 180 + dy);
        }
    }

    /**
     * If the puzzle has an icon, returns the icon's size;
     * otherwise returns {@code null} (no constraint).
     *
     * @return the maximum size.
     */
    public Dimension getMaximumSize(JComponent c) {
        Icon icon = ((Puzzle) c).getIcon();
        if (icon != null) {
            return getPreferredSize(c);
        } else {
            return null;
        }
    }

}
