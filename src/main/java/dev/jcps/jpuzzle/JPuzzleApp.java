package dev.jcps.jpuzzle;
/*
 * JPuzzleApp.java
 *
 * Copyright (c) 1999 Joseph Bowbeer. All Rights Reserved.
 */

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.net.URL;
import java.text.ChoiceFormat;
import java.text.ParsePosition;
import java.util.Objects;

/**
 * A demo applet for the Puzzle JavaBean.
 * <p>
 * Java 2 and Swing 1.1 required.
 *
 * @author <a href="mailto:jozart@csi.com">Joseph Bowbeer</a>
 * @version 1.2
 */
public class JPuzzleApp extends JFrame {

    /**
     * Solver speeds and associated delays.
     */
    private static final ChoiceFormat delays = new ChoiceFormat(
            new double[]{1, 100, 500, 3000},
            new String[]{"faster", "fast", "medium", "slow"}
    );
    private static final Font DIALOG_FONT = new Font("Dialog", Font.BOLD, 12);
    private static final String SOLVE = "Solve";
    /**
     * Image choices.
     */
    private final ImageIcon[] gIcons = {
            null,
            new ImageIcon(getClass().getResource("/tiger.gif"), "tiger"),
            new ImageIcon(getClass().getResource("/eye.gif"), "eyeball")
    };
    /**
     * Cheer audio clip.
     */
    private transient Clip cheer;
    /**
     * The puzzle object associated with this GUI.
     */
    private Puzzle puzzle;

    /**
     * Button for scrambling the puzzle.
     */
    private JButton btnScramble;

    /**
     * Button for solving the puzzle.
     */
    private JButton btnSolve;

    /**
     * Button for navigating to the previous state of the puzzle.
     */
    private JButton btnBack;

    /**
     * Button for navigating to the next state of the puzzle.
     */
    private JButton btnNext;

    /**
     * ComboBox for selecting the image to be used for the puzzle.
     */
    private JComboBox<Object> cmbImage;

    /**
     * ComboBox for selecting the number of rows in the puzzle grid.
     */
    private JComboBox<Integer> cmbRows;

    /**
     * ComboBox for selecting the number of columns in the puzzle grid.
     */
    private JComboBox<Integer> cmbColumns;

    /**
     * ComboBox for selecting the gap size between puzzle pieces.
     */
    private JComboBox<Integer> cmbGap;

    /**
     * ComboBox for selecting the bevel size for puzzle piece edges.
     */
    private JComboBox<Integer> cmbBevel;

    /**
     * ComboBox for selecting the speed of puzzle animations.
     */
    private JComboBox<Object> cmbSpeed;

    /**
     * CheckBox for toggling the visibility of labels on puzzle pieces.
     */
    private JCheckBox chkPaintLabels;

    /**
     * CheckBox for toggling anti-aliasing for puzzle graphics.
     */
    private JCheckBox chkAntiAliased;

    /**
     * CheckBox for toggling outlining for puzzle pieces.
     */
    private JCheckBox chkOutlined;

    /**
     * CheckBox for toggling translucency for puzzle graphics.
     */
    private JCheckBox chkTranslucent;

    /**
     * Initializes the Form
     */
    public JPuzzleApp() {
        initComponents();

        // Image
        Object item = "tiger"; // puzzle.getIcon()
        for (ImageIcon icon : gIcons) {
            cmbImage.addItem(icon == null ?
                    "(none)" : icon.getDescription());
        }

        cmbImage.setSelectedItem(item);

        // Rows
        item = puzzle.getRows();
        for (int i = 3; i <= 10; i++) {
            cmbRows.addItem(i);
        }
        cmbRows.setSelectedItem(item);

        // Columns
        item = puzzle.getColumns();
        for (int i = 3; i <= 10; i++) {
            cmbColumns.addItem(i);
        }
        cmbColumns.setSelectedItem(item);

        // Gap
        item = puzzle.getGap();
        for (int i = 0; i <= 2; i++) {
            cmbGap.addItem(i);
        }
        cmbGap.setSelectedItem(item);

        // Bevel height
        item = puzzle.getBevelHeight();
        for (int i = 0; i <= 2; i++) {
            cmbBevel.addItem(i);
        }
        cmbBevel.setSelectedItem(item);

        // Solver speed
        item = delays.format(puzzle.getDelay());
        Object[] speeds = delays.getFormats();
        for (int n = speeds.length; n-- > 0; ) {
            cmbSpeed.addItem(speeds[n]);
        }
        cmbSpeed.setSelectedItem(item);

        // Labels
        boolean paintLabels = puzzle.isTextPainted();
        chkPaintLabels.setSelected(paintLabels);
        chkAntiAliased.setEnabled(paintLabels);
        chkOutlined.setEnabled(paintLabels);
        chkTranslucent.setEnabled(paintLabels);

        chkAntiAliased.setSelected(puzzle.isTextAntiAliased());
        chkOutlined.setSelected(puzzle.isTextOutlined());
        chkTranslucent.setSelected(!puzzle.isTextOpaque());

        // Register for "history" and "solved" property changes

        puzzle.addPropertyChangeListener(this::puzzlePropertyChange);

        // Register handlers for the arrow keys

        Action actionArrow = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                String s = e.getActionCommand();
                if ("LEFT".equals(s)) puzzle.moveHoleRight();
                else if ("UP".equals(s)) puzzle.moveHoleDown();
                else if ("RIGHT".equals(s)) puzzle.moveHoleLeft();
                else if ("DOWN".equals(s)) puzzle.moveHoleUp();
            }
        };

        JRootPane root = getRootPane();

        root.registerKeyboardAction(
                actionArrow, "LEFT",  // left arrow
                KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        root.registerKeyboardAction(
                actionArrow, "UP",    // up arrow
                KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        root.registerKeyboardAction(
                actionArrow, "RIGHT", // right arrow
                KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        root.registerKeyboardAction(
                actionArrow, "DOWN",  // down arrow
                KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        root.setDefaultButton(btnScramble);
    }

    /**
     * Launches this applet in a frame.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        JPuzzleApp applet = new JPuzzleApp();
        applet.setIconImage(new ImageIcon(
                Objects.requireNonNull(applet.getClass().getResource("/icon16.gif"))).getImage());
        applet.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        applet.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                applet.stop();
            }
        });

        applet.stop();
        applet.pack();
        applet.start();
        applet.setVisible(true);
    }

    private Clip newAudioClip(URL resource) {
        Clip clip;
        try {
            clip = AudioSystem.getClip();
            clip.open(AudioSystem.getAudioInputStream(resource));
        } catch (Exception e) {
            return null;
        }
        return clip;
    }

    /**
     * This method is called from within the constructor to
     * initialize the form. <p>
     * WARNING: Do NOT modify this code. The body of this method
     * is always regenerated by the NetBeans FormEditor.
     */
    private void initComponents() {
        getContentPane().setLayout(new java.awt.GridBagLayout());
        java.awt.GridBagConstraints gridBagConstraints1;

        JPanel jPanel1 = new JPanel();
        jPanel1.setLayout(new java.awt.FlowLayout(FlowLayout.CENTER, 5, 8));

        btnScramble = new JButton();
        btnScramble.setText("Scramble");
        btnScramble.setToolTipText("Scramble the puzzle");
        btnScramble.setForeground(java.awt.Color.blue);
        btnScramble.setFont(DIALOG_FONT);
        btnScramble.addActionListener(this::btnScrambleActionPerformed);
        jPanel1.add(btnScramble);

        btnSolve = new JButton();
        btnSolve.setEnabled(false);
        btnSolve.setText(SOLVE);
        btnSolve.setToolTipText("Solve the puzzle");
        btnSolve.setForeground(java.awt.Color.blue);
        btnSolve.setFont(DIALOG_FONT);
        btnSolve.addActionListener(this::btnSolveActionPerformed);
        jPanel1.add(btnSolve);

        btnBack = new JButton();
        btnBack.setEnabled(false);
        btnBack.setText("Back");
        btnBack.setToolTipText("Undo the last move");
        btnBack.setForeground(java.awt.Color.blue);
        btnBack.setFont(DIALOG_FONT);
        btnBack.addActionListener(this::btnBackActionPerformed);
        jPanel1.add(btnBack);

        btnNext = new JButton();
        btnNext.setEnabled(false);
        btnNext.setText("Next");
        btnNext.setToolTipText("Redo the last move");
        btnNext.setForeground(java.awt.Color.blue);
        btnNext.setFont(DIALOG_FONT);
        btnNext.addActionListener(this::btnNextActionPerformed);
        jPanel1.add(btnNext);

        gridBagConstraints1 = new java.awt.GridBagConstraints();
        gridBagConstraints1.gridx = 0;
        gridBagConstraints1.gridy = 0;
        getContentPane().add(jPanel1, gridBagConstraints1);

        puzzle = new Puzzle();
        puzzle.setGap(1);
        puzzle.setTextPainted(false);
        gridBagConstraints1 = new java.awt.GridBagConstraints();
        gridBagConstraints1.gridx = 0;
        gridBagConstraints1.gridy = 1;
        gridBagConstraints1.gridheight = 0;
        gridBagConstraints1.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints1.insets = new java.awt.Insets(0, 8, 8, 8);
        gridBagConstraints1.weightx = 1.0;
        gridBagConstraints1.weighty = 1.0;
        getContentPane().add(puzzle, gridBagConstraints1);

        JPanel jPanel2 = new JPanel();
        jPanel2.setBorder(new javax.swing.border.CompoundBorder(
                new javax.swing.border.TitledBorder(null, "Puzzle Settings",
                        TitledBorder.LEFT, TitledBorder.TOP, DIALOG_FONT),
                new javax.swing.border.EmptyBorder(new java.awt.Insets(10, 20, 10, 20))));
        jPanel2.setLayout(new java.awt.GridBagLayout());
        java.awt.GridBagConstraints gridBagConstraints2;

        JLabel lblImage = new JLabel();
        lblImage.setText("Image");
        gridBagConstraints2 = new java.awt.GridBagConstraints();
        gridBagConstraints2.insets = new java.awt.Insets(0, 0, 3, 4);
        gridBagConstraints2.anchor = java.awt.GridBagConstraints.EAST;
        jPanel2.add(lblImage, gridBagConstraints2);

        cmbImage = new JComboBox<>();
        cmbImage.addActionListener(this::cmbImageActionPerformed);
        gridBagConstraints2 = new java.awt.GridBagConstraints();
        gridBagConstraints2.gridwidth = 0;
        gridBagConstraints2.insets = new java.awt.Insets(0, 0, 3, 0);
        gridBagConstraints2.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(cmbImage, gridBagConstraints2);

        JLabel lblRows = new JLabel();
        lblRows.setText("Rows");
        gridBagConstraints2 = new java.awt.GridBagConstraints();
        gridBagConstraints2.insets = new java.awt.Insets(0, 0, 3, 4);
        gridBagConstraints2.anchor = java.awt.GridBagConstraints.EAST;
        jPanel2.add(lblRows, gridBagConstraints2);

        cmbRows = new JComboBox<>();
        cmbRows.addActionListener(this::cmbRowsActionPerformed);
        gridBagConstraints2 = new java.awt.GridBagConstraints();
        gridBagConstraints2.gridwidth = 0;
        gridBagConstraints2.insets = new java.awt.Insets(0, 0, 3, 0);
        gridBagConstraints2.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(cmbRows, gridBagConstraints2);

        JLabel lblColumns = new JLabel();
        lblColumns.setText("Columns");
        gridBagConstraints2 = new java.awt.GridBagConstraints();
        gridBagConstraints2.insets = new java.awt.Insets(0, 0, 3, 4);
        gridBagConstraints2.anchor = java.awt.GridBagConstraints.EAST;
        jPanel2.add(lblColumns, gridBagConstraints2);

        cmbColumns = new JComboBox<>();
        cmbColumns.addActionListener(this::cmbColumnsActionPerformed);
        gridBagConstraints2 = new java.awt.GridBagConstraints();
        gridBagConstraints2.gridwidth = 0;
        gridBagConstraints2.insets = new java.awt.Insets(0, 0, 3, 0);
        gridBagConstraints2.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(cmbColumns, gridBagConstraints2);

        JLabel lblGap = new JLabel();
        lblGap.setText("Gap");
        gridBagConstraints2 = new java.awt.GridBagConstraints();
        gridBagConstraints2.insets = new java.awt.Insets(0, 0, 3, 4);
        gridBagConstraints2.anchor = java.awt.GridBagConstraints.EAST;
        jPanel2.add(lblGap, gridBagConstraints2);

        cmbGap = new JComboBox<>();
        cmbGap.addActionListener(this::cmbGapActionPerformed);
        gridBagConstraints2 = new java.awt.GridBagConstraints();
        gridBagConstraints2.gridwidth = 0;
        gridBagConstraints2.insets = new java.awt.Insets(0, 0, 3, 0);
        gridBagConstraints2.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(cmbGap, gridBagConstraints2);

        JLabel lblBevel = new JLabel();
        lblBevel.setText("Bevel");
        gridBagConstraints2 = new java.awt.GridBagConstraints();
        gridBagConstraints2.insets = new java.awt.Insets(0, 0, 3, 4);
        gridBagConstraints2.anchor = java.awt.GridBagConstraints.EAST;
        jPanel2.add(lblBevel, gridBagConstraints2);

        cmbBevel = new JComboBox<>();
        cmbBevel.addActionListener(this::cmbBevelActionPerformed);
        gridBagConstraints2 = new java.awt.GridBagConstraints();
        gridBagConstraints2.gridwidth = 0;
        gridBagConstraints2.insets = new java.awt.Insets(0, 0, 3, 0);
        gridBagConstraints2.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(cmbBevel, gridBagConstraints2);

        JLabel lblSpeed = new JLabel();
        lblSpeed.setText("Speed");
        gridBagConstraints2 = new java.awt.GridBagConstraints();
        gridBagConstraints2.insets = new java.awt.Insets(0, 0, 3, 4);
        gridBagConstraints2.anchor = java.awt.GridBagConstraints.EAST;
        jPanel2.add(lblSpeed, gridBagConstraints2);

        cmbSpeed = new JComboBox<>();
        cmbSpeed.addActionListener(this::cmbSpeedActionPerformed);
        gridBagConstraints2 = new java.awt.GridBagConstraints();
        gridBagConstraints2.gridwidth = 0;
        gridBagConstraints2.insets = new java.awt.Insets(0, 0, 3, 0);
        gridBagConstraints2.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(cmbSpeed, gridBagConstraints2);

        chkPaintLabels = new JCheckBox();
        chkPaintLabels.setText("Paint Labels");
        chkPaintLabels.addActionListener(this::chkPaintLabelsActionPerformed);
        gridBagConstraints2 = new java.awt.GridBagConstraints();
        gridBagConstraints2.gridwidth = 0;
        gridBagConstraints2.insets = new java.awt.Insets(20, 20, 0, 0);
        gridBagConstraints2.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(chkPaintLabels, gridBagConstraints2);

        chkAntiAliased = new JCheckBox();
        chkAntiAliased.setText("Anti-aliased");
        chkAntiAliased.addActionListener(this::chkAntiAliasedActionPerformed);
        gridBagConstraints2 = new java.awt.GridBagConstraints();
        gridBagConstraints2.gridwidth = 0;
        gridBagConstraints2.insets = new java.awt.Insets(0, 40, 0, 0);
        gridBagConstraints2.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(chkAntiAliased, gridBagConstraints2);

        chkOutlined = new JCheckBox();
        chkOutlined.setText("Outlined");
        chkOutlined.addActionListener(this::chkOutlinedActionPerformed);
        gridBagConstraints2 = new java.awt.GridBagConstraints();
        gridBagConstraints2.gridwidth = 0;
        gridBagConstraints2.insets = new java.awt.Insets(0, 40, 0, 0);
        gridBagConstraints2.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(chkOutlined, gridBagConstraints2);

        chkTranslucent = new JCheckBox();
        chkTranslucent.setText("Translucent");
        chkTranslucent.addActionListener(this::chkTranslucentActionPerformed);
        gridBagConstraints2 = new java.awt.GridBagConstraints();
        gridBagConstraints2.gridwidth = 0;
        gridBagConstraints2.insets = new java.awt.Insets(0, 40, 0, 0);
        gridBagConstraints2.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(chkTranslucent, gridBagConstraints2);

        JPanel jPanel3 = new JPanel();
        jPanel3.setLayout(new java.awt.FlowLayout());

        gridBagConstraints2 = new java.awt.GridBagConstraints();
        gridBagConstraints2.gridwidth = 0;
        gridBagConstraints2.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints2.weighty = 1.0;
        jPanel2.add(jPanel3, gridBagConstraints2);

        gridBagConstraints1 = new java.awt.GridBagConstraints();
        gridBagConstraints1.gridwidth = 0;
        gridBagConstraints1.gridheight = 0;
        gridBagConstraints1.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints1.insets = new java.awt.Insets(12, 5, 8, 12);
        getContentPane().add(jPanel2, gridBagConstraints1);
        cheer = newAudioClip(getClass().getResource("/yahoo.wav"));
    }

    private void btnScrambleActionPerformed(ActionEvent evt) {
        puzzle.scramble();
    }

    private void btnSolveActionPerformed(ActionEvent evt) {
        if (SOLVE.equals(evt.getActionCommand())) {
            btnSolve.setText("Stop");
            puzzle.startSolving();
        } else {
            puzzle.stopSolving();
            btnSolve.setText(SOLVE);
        }
    }

    private void btnBackActionPerformed(ActionEvent evt) {
        puzzle.moveBackward();
    }

    private void btnNextActionPerformed(ActionEvent evt) {
        puzzle.moveForward();
    }

    private void cmbImageActionPerformed(ActionEvent evt) {
        int index = cmbImage.getSelectedIndex();
        puzzle.setIcon(gIcons[index]);
    }

    private void cmbRowsActionPerformed(ActionEvent evt) {
        int value = (int) cmbRows.getSelectedItem();
        puzzle.setRows(value);
    }

    private void cmbColumnsActionPerformed(ActionEvent evt) {
        int value = (int) cmbColumns.getSelectedItem();
        puzzle.setColumns(value);
    }

    private void cmbGapActionPerformed(ActionEvent evt) {
        int value = (int) cmbGap.getSelectedItem();
        puzzle.setGap(value);
    }

    private void cmbBevelActionPerformed(ActionEvent evt) {
        int value = (int) cmbBevel.getSelectedItem();
        puzzle.setBevelHeight(value);
    }

    private void cmbSpeedActionPerformed(ActionEvent evt) {
        String speed = (String) cmbSpeed.getSelectedItem();
        puzzle.setDelay(delays.parse(speed, new ParsePosition(0)).intValue());
    }

    private void chkPaintLabelsActionPerformed(ActionEvent evt) {
        boolean textPainted = chkPaintLabels.isSelected();
        puzzle.setTextPainted(textPainted);
        chkAntiAliased.setEnabled(textPainted);
        chkOutlined.setEnabled(textPainted);
        chkTranslucent.setEnabled(textPainted);
    }

    private void chkAntiAliasedActionPerformed(ActionEvent evt) {
        puzzle.setTextAntiAliased(chkAntiAliased.isSelected());
    }

    private void chkOutlinedActionPerformed(ActionEvent evt) {
        puzzle.setTextOutlined(chkOutlined.isSelected());
    }

    private void chkTranslucentActionPerformed(ActionEvent evt) {
        puzzle.setTextOpaque(!chkTranslucent.isSelected());
    }

    /**
     * Restarts the solver when the applet is restarted.
     */
    public void start() {
        String s = btnSolve.getActionCommand();
        if ("Stop".equals(s))
            puzzle.startSolving();
    }

    /**
     * Stops the solver when the applet is stopped.
     */
    public void stop() {
        puzzle.stopSolving();
    }

    /**
     * Handles "history" and "solved" property changes.
     */
    private void puzzlePropertyChange(PropertyChangeEvent event) {
        String propertyName = event.getPropertyName();
        if ("history".equals(propertyName)) // Puzzle.HISTORY
        {
            Object newValue = event.getNewValue();
            if (newValue instanceof Boolean bool) {
                boolean history = bool;
                // Enable the Button on condition...
                btnBack.setEnabled(history);
            }
        } else if ("solved".equals(propertyName))  // Puzzle.SOLVED
        {
            Object newValue = event.getNewValue();
            if (newValue instanceof Boolean bool) {
                boolean solved = bool;
                // Disable the Button on condition...
                btnNext.setEnabled(!solved);
                btnSolve.setEnabled(!solved);
                if (solved) {
                    // Celebrate
                    if (cheer != null) {
                        cheer.setFramePosition(0);
                        cheer.start();
                    }
                    btnSolve.setText(SOLVE);
                }
            }
        }
    }

}
