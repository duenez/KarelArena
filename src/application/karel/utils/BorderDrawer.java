/*
 * BorderDrawer.java
 *
 * Created on 24 de febrero de 2004, 11:25 AM
 */

package application.karel.utils;

import java.awt.*;

/** An interface representing the border drawer. A border drawer is made to draw the limitting area of rectangle in an image.
 * @author Edgar Alfredo Duenez-Guzman
 * @version 1.0
 */
public interface BorderDrawer extends Drawable
{
    /** The NORTH place to draw a piece of the border.
     */    
    public static final int NORTH = 1;
    /** The EAST place to draw a piece of the border.
     */    
    public static final int EAST = 2;
    /** The SOUTH place to draw a piece of the border.
     */    
    public static final int SOUTH = 4;
    /** The WEST place to draw a piece of the border.
     */    
    public static final int WEST = 8;
    /** Draw the whole border, in the four edges of the rectangle.
     */
    public static final int ALL = NORTH | EAST | SOUTH | WEST;

    /** Draws partially the border of an area.
     * @param place The pieces to be drawn can be NORTH, EAST, SOUTH, WEST, and can be or-ed together to create a border of two or more pieces. For example, if the place is set to NORTH | SOUTH, then the border is drawn only in the north ans south places. The border is drawn outside the resctangle specified by x, y and w, h.
     * @param g The Graphics object to draw the border into.
     * @param x The x coordinate of the enclosed area.
     * @param y The y coordinate of the enclosed area.
     * @param w The width of the enclosed area.
     * @param h The height of the enclosed area.
     */    
    public void draw( int place, Graphics g, int x, int y, int w, int h );
    /** Draws the border of an area. The border is drawn outside the rectangle specified by x, y and w, h.
     * @param place The pieces to be drawn can be NORTH, EAST, SOUTH, WEST, and can be or-ed together to create a border of two or more pieces. For example, if the place is set to NORTH | SOUTH, then the border is drawn only in the north ans south places. The border is drawn outside the resctangle specified by x, y and w, h.
     * @param g The Graphics object to draw the border into.
     * @param p The upper left point of the rectangular enclosed area.
     * @param d The dimensions of the rectangular enclosed area.
     */    
    public void draw( int place, Graphics g, Point p, Dimension d );
    public void draw( int place, Graphics g, Rectangle d );
    /** Draws the border of an area. The border is drawn outside the rectangle specified by x, y and w, h.
     * @return Returns the width, in pixels, this border draws outside of an area.
     */
    public int getBorderWidth();
}