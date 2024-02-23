/*
 * PuzzleBeanInfo.java
 *
 * Licensed Materials - See the file license.txt.
 * Copyright (c) Joseph Bowbeer 1998, 1999. All rights reserved.
 */

import java.awt.Image;
import java.beans.*;

/**
 * A BeanInfo class for the Puzzle JavaBean.
 * 
 * @author <a href="mailto:jozart@csi.com">Joseph Bowbeer</a>
 * @version 1.2
 */
public class PuzzleBeanInfo extends SimpleBeanInfo {

    /** 16x16 color icon. */
    private Image icon;
    /** 32x32 color icon. */
    private Image icon32;
    /** 16x16 mono icon. */
    private Image iconM;
    /** 32x32 mono icon. */
    private Image icon32M;

    /**
     * Constructs a new BeanInfo class for the Puzzle JavaBean.
     */
    public PuzzleBeanInfo () {
        iconM   = icon   = loadImage ("PuzzleC16.gif");
        icon32M = icon32 = loadImage ("PuzzleC32.gif");
    }

    /**
     * Returns BeanInfo for the superclass of this bean.
     */
    public BeanInfo[] getAdditionalBeanInfo() {
        try {
            return new BeanInfo[] {
                Introspector.getBeanInfo(Puzzle.class.getSuperclass())
            };
        } catch (IntrospectionException e) {
            e.printStackTrace ();
            return null;
        }
    }

    /**
     * Returns an image object that can be used to represent
     * the bean in toolboxes, toolbars, etc.
     */
    public Image getIcon (int iconKind) {
        switch (iconKind) {
            case ICON_COLOR_16x16: return icon;
            case ICON_COLOR_32x32: return icon32;
            case ICON_MONO_16x16: return iconM;
            case ICON_MONO_32x32: return icon32M;
        }
        return null;
    }

    /**
     * Returns an array of PropertyDescriptors describing the
     * editable properties supported by this bean.
     */
    public PropertyDescriptor[] getPropertyDescriptors () {
        PropertyDescriptor[] propertyDescriptors = new PropertyDescriptor [14];
        try {
            propertyDescriptors [0] = new PropertyDescriptor ("columns", Puzzle.class);
            propertyDescriptors [0].setBound (true);
            propertyDescriptors [0].setConstrained (false);
            propertyDescriptors [1] = new PropertyDescriptor ("rows", Puzzle.class);
            propertyDescriptors [1].setBound (true);
            propertyDescriptors [1].setConstrained (false);
            propertyDescriptors [2] = new PropertyDescriptor ("bevelHeight", Puzzle.class);
            propertyDescriptors [2].setBound (true);
            propertyDescriptors [2].setConstrained (false);
            propertyDescriptors [3] = new PropertyDescriptor ("gap", Puzzle.class);
            propertyDescriptors [3].setBound (true);
            propertyDescriptors [3].setConstrained (false);
            propertyDescriptors [4] = new PropertyDescriptor ("icon", Puzzle.class);
            propertyDescriptors [4].setBound (true);
            propertyDescriptors [4].setConstrained (false);
            propertyDescriptors [5] = new PropertyDescriptor ("delay", Puzzle.class);
            propertyDescriptors [5].setBound (true);
            propertyDescriptors [5].setConstrained (false);
            propertyDescriptors [6] = new PropertyDescriptor ("solved", Puzzle.class, "isSolved", null);
            propertyDescriptors [6].setBound (true);
            propertyDescriptors [6].setConstrained (false);
            propertyDescriptors [7] = new PropertyDescriptor ("history", Puzzle.class, "isHistory", null);
            propertyDescriptors [7].setBound (true);
            propertyDescriptors [7].setConstrained (false);
            propertyDescriptors [8] = new PropertyDescriptor ("textPainted", Puzzle.class);
            propertyDescriptors [8].setBound (true);
            propertyDescriptors [8].setConstrained (false);
            propertyDescriptors [9] = new PropertyDescriptor ("textAntiAliased", Puzzle.class);
            propertyDescriptors [9].setBound (true);
            propertyDescriptors [9].setConstrained (false);
            propertyDescriptors [10] = new PropertyDescriptor ("textOutlined", Puzzle.class);
            propertyDescriptors [10].setBound (true);
            propertyDescriptors [10].setConstrained (false);
            propertyDescriptors [11] = new PropertyDescriptor ("textOpaque", Puzzle.class);
            propertyDescriptors [11].setBound (true);
            propertyDescriptors [11].setConstrained (false);
            propertyDescriptors [12] = new PropertyDescriptor ("textColor", Puzzle.class);
            propertyDescriptors [12].setBound (true);
            propertyDescriptors [12].setConstrained (false);
            propertyDescriptors [13] = new PropertyDescriptor ("textOutlineColor", Puzzle.class);
            propertyDescriptors [13].setBound (true);
            propertyDescriptors [13].setConstrained (false);
        } catch (IntrospectionException e) {
            e.printStackTrace ();
            return null;
        }
        return propertyDescriptors;
    }

}

