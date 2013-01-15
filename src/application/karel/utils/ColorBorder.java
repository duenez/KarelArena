/*
 * SimpleBorder.java
 *
 * Created on 24 de febrero de 2004, 11:33 AM
 */

package application.karel.utils;

import java.awt.*;

/**
 *
 * @author Edgar Alfredo Duenez-Guzman
 * @version 1.0
 */
public class ColorBorder implements BorderDrawer
{
    protected int width;
    protected Color color;
    /** Creates a new instance of SimpleBorder */
    public ColorBorder() { this( 5 ); }
    public ColorBorder( int width ) { this( 5, new Color( 255, 255, 255 ) ); }
    public ColorBorder( Color color ) { this( 5, color ); }
    public ColorBorder( int width, Color color ) { this.width = width; this.color = color; }

    public void draw( Graphics g ) { throw new UnsupportedOperationException(); }
    public void draw( Graphics g, int x, int y ) { throw new UnsupportedOperationException(); }
    public void draw( Graphics g, Point p ) { throw new UnsupportedOperationException(); }
    public void draw( Graphics g, Rectangle rect ) { draw( g, rect.x, rect.y, rect.width, rect.height ); }
    public void draw( Graphics g, Point p, Dimension d ) { draw( g, p.x, p.y, d.width, d.height ); }
    public void draw( Graphics g, int x, int y, int w, int h )
    {
        g.setColor( color );
	for( int i = 1; i < width+1; i++ )
            g.drawRect( x-i, y-i, w+2*i, h+2*i );
    }
    public void draw( int place, Graphics g, Point p, Dimension d ) { draw( place, g, p.x, p.y, d.width, d.height ); }
    public void draw( int place, Graphics g, Rectangle rect ) { draw( place, g, rect.x, rect.y, rect.width, rect.height ); }
    public void draw( int place, Graphics g, int x, int y, int w, int h )
    {
        if( place == 15 )   draw( g, x, y, w, h );
        g.setColor( color );
        int[][] sum = { {0,0}, {1,0}, {1,1}, {0,1} };
        int pow = 1;
        for( int k = 0; k < 4; k++ )
        {
            if( (place & pow) == pow )
                for( int i = 1; i < width+1; i++ )
                    g.drawLine( x - i + sum[k][0]*(w+2*i), y - i + sum[k][1]*(h+2*i), x - i + sum[(k+1)%4][0]*(w+2*i), y - i + sum[(k+1)%4][1]*(h+2*i) );
            pow *= 2;
        }
    }
    public int getBorderWidth() { return width; }
    public Color getBorderColor() { return color; }
    public void setBorderColor( Color c ) { color = c;; }
}