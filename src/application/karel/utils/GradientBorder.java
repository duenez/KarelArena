/*
 * SimpleBorder.java
 *
 * Created on 24 de febrero de 2004, 11:33 AM
 */

package application.karel.utils;

import java.awt.*;

/**
 *
 * @author Edgar A. Duenez-Guzman
 * @version 1.0
 */
public class GradientBorder implements BorderDrawer
{
    private static final int[][] sum = { {0,0}, {1,0}, {1,1}, {0,1} };
    public static final double EPSILON = 0.0001;
    private int width;
    private Color[] colors;
    private static final Color[] dflt = { Color.black, Color.white };
    /** Creates a new instance of SimpleBorder */
    public GradientBorder() { this( 0 ); }
    public GradientBorder( int width ) { this( width, dflt ); }
    public GradientBorder( int width, Color[] colors ) { this.width = width; this.colors = createColors( colors ); }

    public void draw( Graphics g ) { throw new UnsupportedOperationException(); }
    public void draw( Graphics g, int x, int y ) { throw new UnsupportedOperationException(); }
    public void draw( Graphics g, Point p ) { throw new UnsupportedOperationException(); }
    public void draw( Graphics g, Rectangle rect ) { draw( g, rect.x, rect.y, rect.width, rect.height ); }
    public void draw( Graphics g, Point p, Dimension d ) { draw( g, p.x, p.y, d.width, d.height ); }
    public void draw( Graphics g, int x, int y, int width, int height )
    {
	for( int i = 1; i < this.width+1; i++ )
        {
            g.setColor( colors[i-1] );
            g.drawRect( x-i, y-i, width+2*i, height+2*i );
        }
    }
    public void draw( int place, Graphics g, Point p, Dimension d ) { draw( place, g, p.x, p.y, d.width, d.height ); }
    public void draw( int place, Graphics g, Rectangle rect ) { draw( place, g, rect.x, rect.y, rect.width, rect.height ); }
    public void draw( int place, Graphics g, int x, int y, int w, int h )
    {
        if( place == 15 )
            draw( g, x, y, w, h );
        int pow = 1;
        for( int k = 0; k < 4; k++ )
        {
            if( (place & pow) == pow )
                for( int i = 1; i < width+1; i++ )
                {
                    g.setColor( colors[i-1] );
                    g.drawLine( x - i + sum[k][0]*(w+2*i), y - i + sum[k][1]*(h+2*i), x - i + sum[(k+1)%4][0]*(w+2*i), y - i + sum[(k+1)%4][1]*(h+2*i) );
                }
            pow *= 2;
        }
    }
    public Color[] createColors( Color[] c )
    {
        if( c.length >= width || width <= 1 ) return c;
        Color[] tmp = new Color[ width ];
        double ratio = (c.length-1) / (double)(tmp.length-1);
        double sum = 0;
        for( int i = 0; i < tmp.length; i++ )
        {
            if( Math.abs(sum-(int)sum) < EPSILON )
                tmp[i] = c[(int)sum];
            else
            {
                int red =   (int)(  (1-sum+(int)sum)*c[(int)sum].getRed() +   (sum-(int)sum)*( c[(int)sum+1].getRed() )    );
                int green = (int)(  (1-sum+(int)sum)*c[(int)sum].getGreen() + (sum-(int)sum)*( c[(int)sum+1].getGreen() )  );
                int blue =  (int)(  (1-sum+(int)sum)*c[(int)sum].getBlue() +  (sum-(int)sum)*( c[(int)sum+1].getBlue() )   );
                tmp[i] = new Color( red, green, blue );
            }
            sum += ratio;
        }
        return tmp;
    }
    public int getBorderWidth() { return width; }
}