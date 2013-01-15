/*
 * MenuArea.java
 *
 * Created on November 8, 2005, 8:40 PM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package application.karel.utils;

import java.awt.*;
import java.awt.geom.*;

/**
 *
 * @author Edgar A. Duenez-Guzman
 */
public class MenuArea implements Drawable, Dimensionable, Locateable
{
    private BorderDrawer border;
    private Paint paint;
    private Font font;
    private boolean visible;

    private Rectangle area, available;

    public MenuArea( Rectangle r ) { this( r, new ColorBorder( Color.black ), new Color( 0xFFFFFF ) ); }
    public MenuArea( Rectangle r, BorderDrawer b, Paint p )
    {
        super();
        paint = p;
        border = b;
        int bw = border.getBorderWidth();
        area = r;
        available = new Rectangle( r.x + bw, r.y + bw, r.width - 2*bw, r.height - 2*bw );
        visible = true;
    }

    public void setBorderDrawer( BorderDrawer bd ) { border = bd; }
    public BorderDrawer getBorderDrawer() { return border; }

    public Paint getPaint() { return paint; }
    public void setPaint( Paint p ) { paint = p; }

    public void setLocation( double x, double y )
    {
        area.setLocation( (int)x, (int)y );
        available.setLocation( (int)x - border.getBorderWidth(), (int)y - border.getBorderWidth() );
    }
    public void setLocation( Point2D p ) { setLocation( p.getX(), p.getY() ); }
    public Point getLocation() { return area.getLocation(); }
    public Rectangle getAvailableArea() { return available; }

    public Font getFont() { return font; }
    public void setFont( Font f ) { font = f; }

    public boolean isVisible() { return visible; }
    public void setVisible( boolean v ) { visible = v; }
//---------------------- Dimensionable Interface -----------------------------
    public double getWidth() { return area.width; }
    public double getHeight() { return area.height; }
    public Dimension getSize() { return area.getSize(); }

    public void setWidth( double w )
    {
        area.width = (int)w;
        available.width = (int)w - 2*border.getBorderWidth();
    }
    public void setHeight( double h )
    {
        area.height = (int)h;
        available.height = (int)h - 2*border.getBorderWidth();
    }
    public void setSize( double w, double h ) { setWidth( w ); setHeight( h ); }
    public void setSize( Dimension2D d ) { setSize( d.getWidth(), d.getHeight() ); }
//------------------------ Drawable Interface --------------------------------
    public void draw( Graphics g )
    {
        if( g instanceof Graphics2D )
            draw( (Graphics2D) g );
        else
            throw new UnsupportedOperationException();
    }
    public void draw( Graphics g, int x, int y ) { throw new UnsupportedOperationException(); }
    public void draw( Graphics g, int x, int y, int width, int height ) { throw new UnsupportedOperationException(); }
    public void draw( Graphics g, Point p ) { throw new UnsupportedOperationException(); }
    public void draw( Graphics g, Point p, Dimension d ) { throw new UnsupportedOperationException(); }
    public void draw( Graphics g, Rectangle rect ) { throw new UnsupportedOperationException(); }

    public void draw( Graphics2D g )
    {
        if( !visible )
            return;
        Font f = g.getFont();
        Paint p = g.getPaint();

        g.setPaint( paint );
        g.setFont( font );
        border.draw( g, available.x, available.y, available.width-1, available.height-1 );
        g.fillRect( available.x, available.y, available.width, available.height );

        g.setPaint( p );
        g.setFont( f );
    }
}
