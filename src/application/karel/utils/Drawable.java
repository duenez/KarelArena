/*
 * Drawable.java
 *
 * Created on November 8, 2005, 8:41 PM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package application.karel.utils;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;

/**
 *
 * @author Edgar A. Duenez-Guzman
 */
public interface Drawable
{
    public void draw( Graphics g ) throws UnsupportedOperationException;
    public void draw( Graphics g, int x, int y ) throws UnsupportedOperationException;
    public void draw( Graphics g, int x, int y, int width, int height ) throws UnsupportedOperationException;
    public void draw( Graphics g, Point p ) throws UnsupportedOperationException;
    public void draw( Graphics g, Point p, Dimension d ) throws UnsupportedOperationException;
    public void draw( Graphics g, Rectangle rect ) throws UnsupportedOperationException;
}
