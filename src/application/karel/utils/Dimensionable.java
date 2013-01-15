/*
 * Dimensionable.java
 *
 * Created on November 8, 2005, 8:40 PM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package application.karel.utils;

import java.awt.geom.Dimension2D;

/**
 *
 * @author Edgar A. Duenez-Guzman
 */
public interface Dimensionable
{
    public double getWidth();
    public double getHeight();
    public Dimension2D getSize();

    public void setWidth( double w );
    public void setHeight( double h );
    public void setSize( double w, double h );
    public void setSize( Dimension2D d );
}
