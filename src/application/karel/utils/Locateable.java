/*
 * Locateable.java
 *
 * Created on November 11, 2005, 12:26 AM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package application.karel.utils;

import java.awt.geom.Point2D;

/**
 *
 * @author Edgar A. Duenez-Guzman
 */
public interface Locateable
{
    public void setLocation( double x, double y );
    public void setLocation( Point2D p );
    public Point2D getLocation();
}
