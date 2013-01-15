/*
 * KarelPanel.java
 *
 * Created on January 6, 2007, 7:30 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package application.karel;

import application.karel.utils.MenuArea;
import application.karel.utils.GradientBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;
import java.io.*;

/**
 *
 * @author Edgar A. Duenez-Guzman
 */
public class KarelPanel extends JPanel implements ComponentListener, MouseListener, MouseMotionListener
{
    private static final long serialVersionUID = 0x006a4e1;

    //public static final String STATES[] = { "Beepers", "Karel", "Paredes" };
    public static final int BEEPER = 0;
    public static final int KAREL = 1;
    public static final int WALL = 2;
    
    private MenuArea info;
    private int state, beeperQtty = 1, nobeeperQtty = 0, infiniteBeepers;
    private KarelEnvironment env;
    private int deltaX, deltaY;
    private Rectangle view, world;  //view represents the viewable rectangle containing the world in the panel
    private boolean editable, resizeWindow, follow_to_karel, one_click;
    private int pressed_button;
    private Point last;

    private Font mainFont = new Font("Monosapaced", Font.BOLD, 11);
    private Font beepMaxFont = new Font("Monosapaced", Font.PLAIN, 10);
    private Font beepMinFont = new Font("Monosapaced", Font.PLAIN, 9);
    
    private Color sqcolor      = new Color(160, 210, 255);
    private Color sqcolor_gray = new Color(220, 220, 220);

    // Draw Karel images
    private Image knorth_img = null, keast_img = null, ksouth_img = null, kwest_img = null;

    // Determine by close if user wants beeper or wall state
    private boolean smart_state_selection = false, draw_tool_tip = false, display_gray_state = true;

    // Draw selected beepers
    DefaultListModel special_beeper = null;
    DefaultListModel selected_special_beeper = null;
    private Color special_sqcolor = new Color(0, 255, 0), selected_special_sqcolor = new Color(255, 128, 128);

    //world represents the viewable rectangle of the logical world
    /** Creates a new instance of KarelPanel */
    public KarelPanel( KarelEnvironment env )
    {
        this( env, false );
    }
    
    public KarelPanel( KarelEnvironment env, boolean edit )
    {
        super();
        this.env = env;
        addComponentListener( this );
        addMouseListener( this );
        addMouseMotionListener( this );
        view = new Rectangle();
        world = new Rectangle();
        setWorld( 0, 0, 20, 20 );
        state = KarelPanel.BEEPER;
        editable = edit;
        follow_to_karel = !edit;
        last = new Point( -200, -200 );

        this.infiniteBeepers = env.getInfiniteBeepersValue();
        this.init_images();
    }

    private void init_images()
    {
        this.knorth_img = Toolkit.getDefaultToolkit().getImage("KNorte.gif");
        if( !(new File("KNorte.gif").exists()) )
            this.knorth_img = Toolkit.getDefaultToolkit().getImage( this.getClass().getClassLoader().getResource( "application/karel/images/karel/KNorte.gif" ) );

        this.keast_img = Toolkit.getDefaultToolkit().getImage("KEste.gif");
        if( !(new File("KEste.gif").exists()) )
            this.keast_img = Toolkit.getDefaultToolkit().getImage( this.getClass().getClassLoader().getResource( "application/karel/images/karel/KEste.gif" ) );

        this.ksouth_img = Toolkit.getDefaultToolkit().getImage("KSur.gif");
        if( !(new File("KSur.gif").exists()) )
            this.ksouth_img = Toolkit.getDefaultToolkit().getImage( this.getClass().getClassLoader().getResource( "application/karel/images/karel/KSur.gif" ) );

        this.kwest_img = Toolkit.getDefaultToolkit().getImage("KOeste.gif");
        if( !(new File("KOeste.gif").exists()) )
            this.kwest_img = Toolkit.getDefaultToolkit().getImage( this.getClass().getClassLoader().getResource( "application/karel/images/karel/KOeste.gif" ) );
    }

    public void paint()
    {
        this.repaint();
    }

    public void setEnvironment( KarelEnvironment e )
    {
        env = e;
        this.paint();
    }
    public KarelEnvironment getEnvironment() { return env; }

    public void setBeeperQtty( int bq ) { beeperQtty = bq; }
    public int getBeeperQtty() { return beeperQtty; }

    public void setNoBeeperQtty( int nbq ) 
    {
        this.nobeeperQtty = nbq;
    }

    public void set_follow_karel(boolean fk)
    {
        this.follow_to_karel = fk;
    }

    public void set_smart_state_selection( boolean __sss)
    {
        this.smart_state_selection = __sss;
    }

    public void set_draw_tool_tip( boolean __dtt)
    {
        this.draw_tool_tip = __dtt;
    }

    public void set_display_gray_state( boolean __dgs )
    {
        this.display_gray_state = __dgs;
    }

    public void set_special_beepers( DefaultListModel __sp)
    {
        this.special_beeper = __sp;
    }

    public void set_selected_special_beeper(DefaultListModel __ssp)
    {
        this.selected_special_beeper = __ssp;
    }

    public void setState( int st ) { state = st; }
    public int getState() { return state; }

    public void setEditable( boolean ed ) { editable = ed; }
    public boolean getEditable() { return editable; }

    public Rectangle getWorld() { return world; }
    public void setView( Rectangle rect ) { setView( rect.x, rect.y, rect.width, rect.height ); }
    public void setView( int x, int y, int w, int h )
    {
        if( w < 0 || h < 0 )
            return;
        
        view.x = x;
        view.y = y;
        view.width = w;
        view.height = h;
        
        if( !resizeWindow )
        {
            int dx = view.width / 30, dy = view.height / 30;
            int nx = Math.min(world.x, this.env.numAvenidas() - dx);
            int ny = Math.min(world.y, this.env.numCalles() - dy);
            setWorld( nx, ny, dx, dy );
        }
        deltaX = view.width / (world.width + 1);
        deltaY = view.height / (world.height + 1);
    }
    public void moveWorld( int x, int y )
    {
        this.setWorld( x, y, world.width, world.height);
        this.paint();
    }
    public void setWorld( Rectangle rect ) { setWorld( rect.x, rect.y, rect.width, rect.height ); }
    public void setWorld( int x, int y, int w, int h )
    {
        world.x = Math.min( env.numAvenidas()-1, x );
        world.y = Math.min( env.numCalles()-1, y );
        world.width = Math.min( env.numAvenidas() - x, w );
        world.height = Math.min( env.numCalles() - y, h );
        this.paint();
    }
    
    public void transformToCanvas( Point2D.Double pt, Point cnv ) { transformToCanvas( pt.getX(), pt.getY(), cnv ); }
    public void transformToCanvas( double x, double y, Point cnv )
    {
        cnv.x = (int)( view.x + deltaX*( x - world.x + 1 ) );
        cnv.y = (int)( view.y + view.height - deltaY*( y - world.y + 1 ) );
    }
    public void transformToWorld( Point cnv, Point2D.Double pt ) { transformToWorld( cnv.x, cnv.y, pt ); }
    public void transformToWorld( int x, int y, Point2D.Double pt )
    {
        double xx, yy;
        xx = ( x - view.x ) / (double)deltaX - 1 + world.x;
        yy = ( view.y + view.height - y ) / (double)deltaY - 1 + world.y;
        pt.setLocation( xx, yy );
    }
    
    private void followKarel()
    {
        if( follow_to_karel )
        {
            int kx = env.getKarel().getX(), ky = env.getKarel().getY();
            if( kx < world.x || kx >= world.x + world.width )
                world.x = Math.min( env.numCalles() - world.width, kx );
            if( ky < world.y || ky >= world.y + world.height )
                world.y = Math.min( env.numAvenidas() - world.height, ky );
        }
    }

    public int do_smart_state_selection( MouseEvent me )
    {
        final Point2D.Double pt = new Point2D.Double();
        final Point rpt = new Point();
        int x, y, lhw = deltaX/4; // linehalfwidth
        
        this.transformToWorld( me.getX(), me.getY(), pt );
        x = (int)Math.round( pt.getX() );
        y = (int)Math.round( pt.getY() );

        this.transformToCanvas(x, y, rpt);

        if( rpt.x - lhw <= me.getX() && me.getX() <= rpt.x + lhw &&
            rpt.y - lhw <= me.getY() && me.getY() <= rpt.y + lhw )
            this.setState(BEEPER);
        else
            this.setState(WALL);
        return this.state;
    }

    public String get_beeper_value( int beepers )
    {
        if( beepers == this.infiniteBeepers )
            return "INF";
        return "" + beepers;
    }

    private int get_drawing_pair_strings_size(Graphics g, String data[], int sep_between_tag)
    {
        // Compute space
        int sep_tags_to_value = g.getFontMetrics().charWidth('8');
        int sep_tag = sep_tags_to_value + sep_between_tag * g.getFontMetrics().charWidth('8');
        int size_tags = 0;
        for( int i = 0, n = data.length; i < n; i+= 2 )
            size_tags += g.getFontMetrics().stringWidth(data[i]) + g.getFontMetrics().stringWidth(data[i + 1]) + sep_tag;
        return size_tags - sep_tag;
    }

    private void draw_pair_strings(Graphics g, String data[], int ini_pos, int y, int sep_between_tag)
    {
        int sep_tags_to_value = g.getFontMetrics().charWidth('8');
        int sep_tag = sep_tags_to_value + sep_between_tag * g.getFontMetrics().charWidth('8');
        
        // Draw stat strings
        g.setFont( this.mainFont );
        for( int i = 0, n = data.length; i < n; i+= 2 )
        {
            g.setColor( Color.black); g.drawString(data[i],     ini_pos, y);
            g.setColor( Color.blue);  g.drawString(data[i + 1], ini_pos + g.getFontMetrics().stringWidth( data[i] ) + sep_tags_to_value, y);
            ini_pos += g.getFontMetrics().stringWidth(data[i]) + g.getFontMetrics().stringWidth(data[i + 1]) + sep_tag;
        }
    }

    public void draw_beepers( Graphics g, int i, int j, int onbeepers, Color sq_color)
    {
        // set font
        if( onbeepers == this.infiniteBeepers || (0 < onbeepers && onbeepers < 1000) )
            g.setFont( this.beepMaxFont );
        else
            g.setFont( this.beepMinFont );


        final Point cnv = new Point();
        int linehalfwidth = deltaX/4;
        int pixperchar = g.getFontMetrics().charWidth('8'), widthtext = 0, wsq = 2*linehalfwidth, hsq = 2*linehalfwidth, sqx = 0, sqy = 0;

        transformToCanvas( i, j, cnv );
        widthtext = pixperchar + 2;
        if( onbeepers == this.env.getInfiniteBeepersValue() )
            widthtext = 3*pixperchar - 2;
        else if( onbeepers > 9999 )
            widthtext = 5*pixperchar + 1;
        else if( onbeepers >  999 )
            widthtext = 4*pixperchar;
        else if( onbeepers >   99 )
            widthtext = 3*pixperchar + 1;
        else if( onbeepers > 9 )
            widthtext = 2*pixperchar + 2;

        sqx = cnv.x - linehalfwidth;
        sqy = cnv.y - linehalfwidth;
        wsq = hsq = 2*linehalfwidth;

        cnv.x -= widthtext/2 - 1;
        cnv.y += 4;

        // Change rectangle corner and width if necessary
        sqx = Math.min(sqx, cnv.x);
        wsq = Math.max(widthtext, wsq);

        g.setColor( sq_color );
        g.fillRect(sqx, sqy, wsq, hsq);

        g.setColor(Color.black);
        g.drawString(this.get_beeper_value(onbeepers), cnv.x, cnv.y);
    }

    @Override public void paintComponent( Graphics g )
    {
        final Point cnv = new Point();

        g.setFont( this.mainFont );

        super.paintComponent( g );

        followKarel();

        Point PKarel = new Point(env.getKarel().getX(), env.getKarel().getY());
        
        //Draw the "calles" and "avenidas"
        int linehalfwidth = deltaX / 4, stringwidth = 0, stringheight = g.getFontMetrics().getHeight();
        for( int i = world.x; i <= world.x + world.width; i++ )
        {
            transformToCanvas( i, 0, cnv );
            g.setColor( Color.white );
            g.fillRect(cnv.x - linehalfwidth, view.y, 2*linehalfwidth, view.y + view.height - 50);
            if( cnv.x > view.x )
            {
                g.setColor( Color.black );
                stringwidth = g.getFontMetrics().stringWidth("" + (i + 1));
                g.drawString( "" + (i+1), cnv.x - stringwidth/2, view.y + view.height + 15 );
            }
        }
        for( int i = world.y; i <= world.y + world.height; i++ )
        {
            transformToCanvas( 0, i, cnv );
            g.setColor( Color.white );
            g.fillRect(view.x, cnv.y - linehalfwidth, view.y + view.width, 2*linehalfwidth);
            if( cnv.y < view.y + view.height )
            {
                g.setColor( Color.black );
                g.drawString( "" + ( i + 1), view.x - 15, cnv.y + 5);
            }
        }
        
        //Draw the "paredes" (walls) on the world
        g.setColor( Color.blue );
        for( int i = world.x; i <= world.x + world.width; i++ )
        {
            for( int j = world.y; j <= world.y + world.height; j++ )
            {
                // j < world.y + world.height
                if( env.isThereWall( i, j, KarelEnvironment.Heading.SOUTH ) )      //Horizontal line
                    drawWall( g, i, j, KarelEnvironment.Heading.SOUTH );
                if( env.isThereWall( i, j, KarelEnvironment.Heading.WEST ) )               //Vertical line
                    drawWall( g, i, j, KarelEnvironment.Heading.WEST );
            }
            if( env.isThereWall( i, world.y + world.height, KarelEnvironment.Heading.NORTH ) )      //Horizontal line
                drawWall( g, i, world.y + world.height, KarelEnvironment.Heading.NORTH );
        }

        for( int j = world.y; j <= world.y + world.height; j++ )
            if( env.isThereWall( world.x + world.width, j, KarelEnvironment.Heading.EAST ) )               //Vertical line
                drawWall( g, world.x + world.width, j, KarelEnvironment.Heading.EAST );
        
        //Draw Karel
        if( PKarel.x >= world.x && PKarel.x < world.x + world.width &&
            PKarel.y >= world.y && PKarel.y < world.y + world.height   )
        {
            transformToCanvas( PKarel.x, PKarel.y, cnv );
            int mar = 7;
            int sqx = cnv.x - deltaX/4 - mar/2, sqy = cnv.y - deltaY/4 - mar/2;
            int wKarel = deltaX/2 + mar, hKarel = deltaY/2 + mar;

            switch( env.getKarel().getHeading() )
            {
                case EAST:
                    g.drawImage(this.keast_img, sqx, sqy, wKarel, hKarel, this);
                    break;
                case NORTH:
                    g.drawImage(this.knorth_img, sqx, sqy, wKarel, hKarel, this);
                    break;
                case WEST:
                    g.drawImage(this.kwest_img, sqx, sqy, wKarel, hKarel, this);
                    break;
                case SOUTH:
                    g.drawImage(this.ksouth_img, sqx, sqy, wKarel, hKarel, this);
                    break;
            }
        }

        //Draw the "beepers" on the world
        for( int i = 0; i <= world.width; i++ )
            for( int j = 0; j <= world.height; j++ )
                if( env.getBeepers( world.x + i, world.y + j ) > 0 )
                    this.draw_beepers(g, world.x + i, world.y + j, this.env.getBeepers( world.x + i, world.y + j ), this.sqcolor);

        // Draw special beepers
        int sbeeper[] = null;
        if( this.special_beeper != null )
            for( int i = 0, n = this.special_beeper.size(); i < n; i++ )
            {
                sbeeper = KarelArena.extract_values_from_coev_beeper( (String)(this.special_beeper.get(i)) );
                this.draw_beepers(g, sbeeper[0] - 1, sbeeper[1] - 1, sbeeper[2], this.special_sqcolor);
            }

        if( this.selected_special_beeper != null )
            for( int i = 0, n = this.selected_special_beeper.size(); i < n; i++ )
            {
                sbeeper = KarelArena.extract_values_from_coev_beeper( (String)(this.selected_special_beeper.get(i)) );
                //if( this.env.getBeepers(sbeeper[0] - 1, sbeeper[1] - 1) == sbeeper[2] )
                    this.draw_beepers(g, sbeeper[0] - 1, sbeeper[1] - 1, sbeeper[2], this.selected_special_sqcolor);
            }

        String tags[] = {
            "Mochila: ",            this.get_beeper_value(this.env.getKarel().getNumBeepers()),
            "Karel pos: ",          "(A: " + ( PKarel.x + 1 ) + ", C: " + ( PKarel.y + 1 ) + ")",
            "Beepers para poner: ", this.get_beeper_value(this.getBeeperQtty()),
            "Inst: ",               "" + this.env.getKarel().getNativeCalls()
        };
        
        //Karel stats ------------------------------------
        int ini_pos = 10, tag_sep = 5;
        if( info != null )
        {
            info.draw(g);
            ini_pos = Math.max(10, (int)(info.getWidth())/2 - this.get_drawing_pair_strings_size(g, tags, tag_sep)/2 );
        }
        this.draw_pair_strings(g, tags, ini_pos, 35, tag_sep);
        drawTooltip( g );
    }
    private void drawWall( Graphics g, int i, int j, KarelEnvironment.Heading dir )
    {
        final Point cnv = new Point();
        int wallhalftwidth = 1;
        switch( dir )
        {
            case SOUTH:
                transformToCanvas( i - 0.5, j - 0.5, cnv );
                //g.drawLine( cnv.x, cnv.y, cnv.x + deltaX, cnv.y );
                g.fillRect(cnv.x, cnv.y  - wallhalftwidth, deltaX, 2*wallhalftwidth);
                break;
            case NORTH:
                transformToCanvas( i - 0.5, j + 0.5, cnv );
                //g.drawLine( cnv.x, cnv.y, cnv.x + deltaX, cnv.y );
                g.fillRect(cnv.x, cnv.y - wallhalftwidth, deltaX, 2*wallhalftwidth);
                break;
            case WEST:
                transformToCanvas( i - 0.5, j + 0.5, cnv );
                //g.drawLine( cnv.x, cnv.y, cnv.x, cnv.y + deltaY );
                g.fillRect(cnv.x  - wallhalftwidth, cnv.y, 2*wallhalftwidth, deltaY);
                break;
            case EAST:
                transformToCanvas( i + 0.5, j + 0.5, cnv );
                //g.drawLine( cnv.x, cnv.y, cnv.x, cnv.y + deltaY );
                g.fillRect(cnv.x  - wallhalftwidth, cnv.y, 2*wallhalftwidth, deltaY);
                break;
        }
    }
    
    private void drawTooltip( Graphics g )
    {
        final Point2D.Double pt = new Point2D.Double();
        int x, y, width = 0, height = 20, height_char = g.getFontMetrics().getHeight(), sep_tags = 2;
        if( editable || this.draw_tool_tip )
        {

            transformToWorld( last.x, last.y, pt );
            if( pt.getX() < 0 || pt.getX() >= world.x + world.width  || pt.getX() >= this.env.numAvenidas() - 1 ||
                pt.getY() < 0 || pt.getY() >= world.y + world.height || pt.getY() >= this.env.numCalles() - 1 )
                return;

            x = last.x + 10;
            y = last.y + 10;
            String tags[] = {
                "A: ", "" + Math.round( pt.getX() + 1 ),
                "C: ", "" + Math.round( pt.getY() + 1 )
            };
            width = this.get_drawing_pair_strings_size(g, tags, sep_tags) + 20;

            if( x + width > this.getWidth() )
                x = this.getWidth() - width - 1;

            g.setColor( Color.yellow );
            g.fillRect( x, y, width, height );
            g.setColor( Color.black );
            g.drawRect( x, y, width, height );

            this.draw_pair_strings(g, tags, x + 5, last.y + height/2 + height_char, sep_tags);

            if( this.display_gray_state )
            {
                if( state == BEEPER )
                {
                    this.draw_beepers(g, (int)Math.round( pt.getX() ), (int)Math.round( pt.getY() ), this.beeperQtty, this.sqcolor_gray);
                }
                else if( state == WALL )
                {
                    g.setColor( Color.gray );
                    switch ( getPosition( pt ) )
                    {
                        case SOUTH: drawWall( g, (int)pt.getX(), (int)pt.getY(), KarelEnvironment.Heading.EAST ); break;
                        case NORTH: drawWall( g, (int)pt.getX(), 1+(int)pt.getY(), KarelEnvironment.Heading.EAST ); break;
                        case EAST:  drawWall( g, 1+(int)pt.getX(), (int)pt.getY(), KarelEnvironment.Heading.NORTH ); break;
                        case WEST:  drawWall( g, (int)pt.getX(), (int)pt.getY(), KarelEnvironment.Heading.NORTH ); break;
                    }
                }
            }
        }
    }

    // Function to handle walls and beepers setting
    public void process_wall_event( MouseEvent me, boolean __clicked )
    {
        final Point2D.Double pt = new Point2D.Double();
        transformToWorld( me.getX(), me.getY(), pt );
        int x = (int)pt.getX(), y = (int)pt.getY();

        if( x < 0 ||  x >= world.x + world.width  || x >= this.env.numAvenidas() - 1 ||
            y < 0 ||  y >= world.y + world.height || y >= this.env.numCalles() - 1 )
            return;
        if( __clicked )
        {
            switch( getPosition( pt ) )
            {
                case EAST:  this.env.toggleWall( x + 1, y, KarelEnvironment.Heading.NORTH ); break;
                case SOUTH: this.env.toggleWall( x, y, KarelEnvironment.Heading.EAST ); break;
                case NORTH: this.env.toggleWall( x, y + 1, KarelEnvironment.Heading.EAST ); break;
                case WEST:  this.env.toggleWall( x, y, KarelEnvironment.Heading.NORTH ); break;
            }
        }
        else // dragged
        {
            boolean set_wall = this.one_click;
            switch( getPosition( pt ) )
            {
                case EAST:  this.env.setWall( x + 1, y, KarelEnvironment.Heading.NORTH, set_wall); break;
                case SOUTH: this.env.setWall( x, y, KarelEnvironment.Heading.EAST, set_wall); break;
                case NORTH: this.env.setWall( x, y + 1, KarelEnvironment.Heading.EAST, set_wall); break;
                case WEST:  this.env.setWall( x, y, KarelEnvironment.Heading.NORTH, set_wall); break;
            }
        }
    }

    public void process_beeper_event( MouseEvent me, boolean __clicked)
    {
        final Point2D.Double pt = new Point2D.Double();
        transformToWorld( me.getX(), me.getY(), pt );
        int x = (int)Math.round( pt.getX() ), y = (int)Math.round( pt.getY() );

        boolean set_beeper = this.one_click;
        if( __clicked )
            set_beeper = me.getButton() == MouseEvent.BUTTON3 || this.env.getBeepers( x, y) == 0 || this.env.getBeepers( x, y) != this.beeperQtty;

        if( 0 <= x && x <= this.world.x + this.world.width && x < this.env.numAvenidas() &&
            0 <= y && y <= this.world.y + this.world.height && y < this.env.numCalles() )
            this.env.setBeepers( x, y, set_beeper ? beeperQtty : nobeeperQtty );
    }

    public void process_karel_event(MouseEvent me, boolean __clicked)
    {
        final Point2D.Double pt = new Point2D.Double();
        transformToWorld( me.getX(), me.getY(), pt );
        
        if( __clicked )
            this.env.getKarel().setLocation( (int)Math.round( pt.getX() ), (int)Math.round( pt.getY() ) );
        else // dragged
        {
            double dx = pt.getX() - env.getKarel().getX();
            double dy = pt.getY() - env.getKarel().getY();
            if( dx > 0 && dy > 0 )
                if( dx > dy )
                    this.env.getKarel().setHeading( KarelEnvironment.Heading.EAST );
                else
                    this.env.getKarel().setHeading( KarelEnvironment.Heading.NORTH );
            else if( dx > 0 && dy <= 0 )
                if( dx > -dy )
                    this.env.getKarel().setHeading( KarelEnvironment.Heading.EAST );
                else
                    this.env.getKarel().setHeading( KarelEnvironment.Heading.SOUTH );
            else if( dx <= 0 && dy > 0 )
                if( -dx > dy )
                    this.env.getKarel().setHeading( KarelEnvironment.Heading.WEST );
                else
                    this.env.getKarel().setHeading( KarelEnvironment.Heading.NORTH );
            else
                if( dx < dy )
                    this.env.getKarel().setHeading( KarelEnvironment.Heading.WEST );
                else
                    this.env.getKarel().setHeading( KarelEnvironment.Heading.SOUTH );
        }
    }

    private KarelEnvironment.Heading getPosition( Point2D.Double pt )
    {
        int x = (int)pt.getX(), y = (int)pt.getY();
        double fx = pt.getX() - x, fy = pt.getY() - y;
        KarelEnvironment.Heading facing = KarelEnvironment.Heading.CENTER;

        if( (fx-0.5)*(fx-0.5) + (fy-0.5)*(fy-0.5) >= 0.09 )
        {
            if( fx > fy )
                if( fx + fy > 1 )  //East region
                    facing = KarelEnvironment.Heading.EAST;
                else                //South region
                    facing = KarelEnvironment.Heading.SOUTH;
            else
                if( fx + fy > 1 ) //North region
                    facing = KarelEnvironment.Heading.NORTH;
                else                //West region
                    facing = KarelEnvironment.Heading.WEST;
        }
        return facing;
    }

    public Point get_world_position( MouseEvent me )
    {
        final Point2D.Double pt = new Point2D.Double();
        transformToWorld( me.getX(), me.getY(), pt );
        int x = (int)Math.round( pt.getX() ), y = (int)Math.round( pt.getY() );
        
        if( 0 <= x && x <= this.world.x + this.world.width && x < this.env.numAvenidas() &&
            0 <= y && y <= this.world.y + this.world.height && y < this.env.numCalles() )
            return new Point(x, y);
        return new Point(-1, -1);
    }
    
    public void mouseClicked( MouseEvent e )
    {
        if( !editable || e.getButton() != MouseEvent.BUTTON1 )
            return;

        switch( state )
        {
            case WALL:
                this.process_wall_event(e, true);
                break;
            case BEEPER:
                this.process_beeper_event(e, true);
                break;
            case KAREL:
                this.process_karel_event(e, true);
                break;
        }
        this.paint();
    }

    public void mouseDragged( MouseEvent e )
    {
        if( !editable || this.pressed_button != MouseEvent.BUTTON1 )
        {
            this.paint();
            return;
        }

        int current_state = this.state;
        boolean draw_smart_selection  = current_state == this.do_smart_state_selection(e);
        this.state = current_state;
        
        if( draw_smart_selection )
        {
            switch( this.state )
            {
                case KarelPanel.WALL:
                    this.process_wall_event(e, false);
                    break;
                case KarelPanel.BEEPER:
                    this.process_beeper_event(e, false);
                    break;
                case KarelPanel.KAREL:
                    this.process_karel_event(e, false);
                    break;
            }
        }
        this.paint();
    }

    public void mouseEntered( MouseEvent e )
    {
        last.setLocation( e.getX(), e.getY() );
    }
    public void mouseExited( MouseEvent e )
    {
        last.setLocation( -200, -200 );
        repaint();
    }
    public void mousePressed( MouseEvent e )
    {
        this.one_click      = e.getClickCount() == 1;
        this.pressed_button = e.getButton();
        last.setLocation( -200, -200 );
    }
    public void mouseReleased( MouseEvent e )
    {
        last.setLocation( e.getX(), e.getY() );
    }
    
    public void mouseMoved( MouseEvent e )
    {
        last.setLocation( e.getX(), e.getY() );

        if( this.smart_state_selection )
            this.do_smart_state_selection(e);

        if( this.editable || this.draw_tool_tip )
        {
            this.requestFocus();
            this.paint();
        }
    }

    public void componentResized( ComponentEvent e )    {
        setView( 25, 50, getWidth()-35, getHeight()-75 );
        info = new MenuArea( new Rectangle( 10, 10, getWidth() - 20, 40 ),
                new GradientBorder( 5, new Color[] { Color.gray, Color.white } ), Color.lightGray );
        repaint();
    }
    
    public void componentHidden( ComponentEvent e ){}
    public void componentMoved( ComponentEvent e ){}
    public void componentShown( ComponentEvent e ){}
}