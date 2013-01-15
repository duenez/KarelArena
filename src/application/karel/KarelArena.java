/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * KarelArena.java
 *
 * Created on 16/05/2011, 06:38:10 PM
 */

package application.karel;

import com.Ostermiller.Syntax.HighlightedDocument;
import com.instil.lgi.*;
import application.karel.utils.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Stack;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.undo.*;

/**
 *
 * @author Kuko
 */
public class KarelArena extends JFrame implements ExecutionListener, KarelArenaListener
{
    /** Karel Variables */
    private static final long serialVersionUID = 0x00a66137;
    
    // <editor-fold defaultstate="collapsed" desc="Global variables">
    private JFileChooser fc = new JFileChooser(".");
    private boolean loading_app = true;
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Karel variables">
    private int count_id = 0;
    private int infBeepers = 0;
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="World Tab Variables">
    private KarelEnvironment env;
    private boolean world_change, world_resize = false, world_press_control = false;;
    private String current_world_file = "";
    private java.awt.event.MouseEvent pop_world_click_event = null;
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Code Tab Variables">
    private HighlightedDocument code_document = new HighlightedDocument();
    private UndoManager undoman;
    private boolean code_change;
    private String current_code_file;

    private KarelExecutor ka_exec;
    private PackratParser parser_java, parser_pascal;
    public static PackratParser parser;
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Exec Tab Variables">
    private HighlightedDocument exec_document = new HighlightedDocument();
    private boolean exec_stepping = false;
    
    private SimpleAttributeSet debug = new SimpleAttributeSet(), plain = new SimpleAttributeSet();
    private int lastFrom, lastTo;
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Coev Tab Variables">
    private boolean coev_change, coev_resize = false, coev_press_control = false;
    private String current_coev_file = "";
    private KarelKECEnvironment coev_kec_file = new KarelKECEnvironment();
    javax.swing.JTextField coev_field[] = null;
    javax.swing.JCheckBox  coev_check[] = null;

    DefaultListModel coev_beeper = new DefaultListModel();
    DefaultListModel coev_beeper_selected = new DefaultListModel();
    Point coev_current_position = new Point(0, 0);

    private int coev_press_button = 0;

    //</editor-fold>

    public KarelArena() {
        this.pre_init();
        this.initComponents();
        this.post_init();
        this.prepare_application();
        this.loading_app = false;
    }

    // -----------------------------------------------------------------
    // <editor-fold defaultstate="collapsed" desc="Implements Methods">
    public void execWorldInit()
    {
        KarelEnvironment envClone = (KarelEnvironment) env.clone();
        ((KarelPanel)(this.main_exec_painter_pnlPainter)).setEnvironment( envClone );
        ((KarelPanel)(this.main_exec_painter_pnlPainter)).set_follow_karel(true);
        this.ka_exec.setEnvironment( envClone );
        this.main_exec_display_txtCode.getStyledDocument().setCharacterAttributes(0, this.main_exec_display_txtCode.getStyledDocument().getLength(), plain, true);
    }

    public void doneExecuting()
    {
        this.ka_exec.stop();
        this.main_exec_options_btnRun.setEnabled( true );
    }

    public void executionEvent(int from, int to)
    {
        this.ka_exec.setPaused( this.exec_stepping );
        this.main_exec_display_txtCode.select( from, to );
        StyledDocument doc = this.main_exec_display_txtCode.getStyledDocument();
        doc.setCharacterAttributes( lastFrom, lastTo-lastFrom, plain, true );
        doc.setCharacterAttributes( from, to-from, debug, true );
        lastFrom = from;
        lastTo = to;
    }

    public void executionEvent(Stack<String> exec)
    {
    }

    public void paint_enviroment()
    {
        ((KarelPanel)this.main_exec_painter_pnlPainter).paint();
    }

    
    // </editor-fold>

    // -----------------------------------------------------------------
    // <editor-fold defaultstate="collapsed" desc="Init Methods">
    private void pre_init() {
        // World
        this.env   = new KarelEnvironment(101, 101);
        infBeepers = this.env.getInfiniteBeepersValue();

        this.ka_exec = new KarelExecutor( null );
        this.ka_exec.setExecutionListener( this );
    }

    private void post_init() {
        // World    -----------------------------------------------------------------
        ((KarelPanel)this.main_world_painter_pnlPainter).set_smart_state_selection(true);
        this.reset_world();

        // Code     -----------------------------------------------------------------
        try
        {
            this.parser_java   = PackratParser.createParser( this.getClass().getClassLoader().getResource( "application/karel/KarelSyntax.java.txt" ) );
            this.parser_pascal = PackratParser.createParser( this.getClass().getClassLoader().getResource( "application/karel/KarelSyntax.pascal.txt" ) );
            KarelArena.parser  = this.parser_java;
            KarelArena.parser.setUtility( KarelUtility.utility );
        }catch( Exception exc ){
            JOptionPane.showMessageDialog( this, "Error fatal al inicializar la Arena" );
            System.exit( -1 );
        }
        
        LineNumber lnumber = new LineNumber( this.main_code_editor_txtCode );
        lnumber.setFont( this.main_code_editor_txtCode.getFont() );
        this.main_code_editor_container.setRowHeaderView( lnumber );
        this.code_document.setHighlightStyle( KarelLexerJava.class );
        
        
        // Undo manager
        this.undoman = new UndoManager();
        this.undoman.setLimit(-1);
        this.code_document.addUndoableEditListener(  new UndoableEditListener(){
            public void undoableEditHappened(UndoableEditEvent e)
            {
                undoman.addEdit( e.getEdit() );
                main_code_options_btnUndo.setEnabled(true);
            }
        });
        
        this.reset_code();

        // Init shorcut
        this.init_code_shortcut();
        
        // Execution    -----------------------------------------------------------
        this.ka_exec.setExceptionHandler( new DefaultHandler( this, (JPanel)(this.main_exec_painter_pnlPainter) ) );
        
        KarelArena.setAttributeSet( debug, Color.pink, true );
        KarelArena.setAttributeSet( plain, Color.white, false );

        // Conditions   -----------------------------------------------------------
        ((KarelPanel)this.main_coev_painter_pnlPainter).setState(KarelPanel.BEEPER);
        ((KarelPanel)this.main_coev_painter_pnlPainter).set_display_gray_state(false);
        this.read_coev_world(0);

        // Prepare data
        this.coev_field = new javax.swing.JTextField[]{
            this.main_coev_settings_txtIns,  this.main_coev_settings_txtMove, this.main_coev_settings_txtTurn,
            this.main_coev_settings_txtPick, this.main_coev_settings_txtPut,  this.main_coev_settings_txtBeeperBag, this.main_coev_settings_txtBeepersPos};
        
        this.coev_check = new javax.swing.JCheckBox[]{
            this.main_coev_settings_chkIns, this.main_coev_settings_chkMove, this.main_coev_settings_chkTurn,
            this.main_coev_settings_chkPick, this.main_coev_settings_chkPut, this.main_coev_settings_chkBeeperBag, this.main_coev_settings_chkBeepersPos};

        this.main_coev_settings_lstBeepers.setModel(this.coev_beeper);
        ((KarelPanel)(this.main_coev_painter_pnlPainter)).set_special_beepers(this.coev_beeper);
        ((KarelPanel)(this.main_coev_painter_pnlPainter)).set_selected_special_beeper(this.coev_beeper_selected);
        this.reset_coev_settings();

    }
    // </editor-fold>

    // -----------------------------------------------------------------
    // <editor-fold defaultstate="collapsed" desc="Global methods">
    private void prepare_application() {

        // Main frame
        int mywidth = this.getWidth(), myheight = this.getHeight();
        final boolean centering = false;

        final Point center = GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
        Point corner = new Point(0, 0);

        this.setTitle("Karel 2.0 - Version en Java de KarelOMI");
        this.setSize(mywidth, myheight);
        if (centering) {
            corner = new Point(center.x - this.getWidth() / 2, center.y - this.getHeight() / 2);

        }
        this.setBounds(corner.x, corner.y, this.getWidth(), this.getHeight());

        // Uncomment to maximize
        //this.setExtendedState( JFrame.MAXIMIZED_BOTH );

        // World

        // Code

        // Exec
    }

    public boolean confirm_discard_changes(String __what_discard)
    {
        return JOptionPane.showConfirmDialog(this, __what_discard.toUpperCase() + " actual ha sufrido cambios. ¿Deseas DESCARTAR estos cambios?", "Descartar cambios", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    public boolean confirm_overwrite(String __filename)
    {
        return JOptionPane.showConfirmDialog(this, "El archivo " + __filename.toUpperCase() + " ya existe. ¿Deseas SOBREESCRIBIRLO?", "Sobreescribir archivo", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    private void do_selected_pop_world_menu_option(String data)
    {
        if( this.pop_world_click_event == null )
            return;

        JPanel    src = (JPanel)(this.pop_world_click_event.getSource());
        KarelPanel kp = (KarelPanel)(src);
        String pattern_beepers = "01234556789NI";
        this.main_pop_world.setVisible(false);
        if( data.length() == 2 && data.startsWith("K") ) // Karel option
        {
            kp.process_karel_event(this.pop_world_click_event, true);
            if( data.equals("KN") )
                kp.getEnvironment().getKarel().setHeading(KarelEnvironment.Heading.NORTH);
            else if( data.equals("KS") )
                kp.getEnvironment().getKarel().setHeading(KarelEnvironment.Heading.SOUTH);
            else if( data.equals("KE") )
                kp.getEnvironment().getKarel().setHeading(KarelEnvironment.Heading.EAST);
            else if( data.equals("KW") )
                kp.getEnvironment().getKarel().setHeading(KarelEnvironment.Heading.WEST);

            if( src == this.main_coev_painter_pnlPainter )
            {
                this.main_coev_settings_txtPKA.setText( String.valueOf(kp.getEnvironment().getKarel().getX() + 1) );
                this.main_coev_settings_txtPKC.setText( String.valueOf(kp.getEnvironment().getKarel().getY() + 1) );
                this.main_coev_settings_cmbFace.setSelectedIndex( KarelWorldManager.KarelMDOToken.get_inverse_heading( kp.getEnvironment().getKarel().getHeading() ) - 1 );
            }
        }
        else if( data.length() > 0 && pattern_beepers.contains( String.valueOf(data.charAt(0))) )
        {
            int beepers = 0;
            if( data.equals("N") )
                data = JOptionPane.showInputDialog(this, "Numero de beepers:");
            else if( data.equals("INFINITOS") )
                data = String.valueOf( this.infBeepers );
            if( data == null || data.trim().isEmpty() )
                return;
            try
            {
                beepers = Integer.parseInt(data);
                if( beepers >= this.infBeepers )
                    beepers = this.infBeepers;

                if( beepers < 0 )
                    throw new NumberFormatException("");

                // decide what to do
                if( src == this.main_world_painter_pnlPainter )
                {
                    kp.setBeeperQtty( beepers );
                    kp.process_beeper_event(this.pop_world_click_event, true);
                    kp.setBeeperQtty( Math.max(0, beepers) );
                }
                else if( src == this.main_coev_painter_pnlPainter )
                {
                    kp.setBeeperQtty( beepers );
                    this.process_coev_mouse_event(this.pop_world_click_event, "add");
                }
                this.set_world_change_values(true);
            } catch( NumberFormatException nexc ){
                JOptionPane.showMessageDialog(this, "Los beepers deben ser un entero mayor o igual a 0", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        this.pop_world_click_event = null;
        kp.paint();
    }

    public int check_positive_content(javax.swing.JTextField __txtField, int __min_value, int __max_value, boolean __show_error_message)
    {
        boolean parse_error = true;
        int b = 0;
        if( __txtField.getText().trim().isEmpty() )
            __txtField.setText("");
        try
        {
            String data = __txtField.getText().trim();
            b = Integer.parseInt( data );
            parse_error = false;
            if( b < __min_value )
            {
                __txtField.setText( String.valueOf(__min_value) );
                throw new NumberFormatException( String.valueOf(__min_value) );
            }
            if( b > __max_value )
            {
                __txtField.setText( String.valueOf(__max_value) );
                throw new NumberFormatException( String.valueOf(__max_value) );
            }
        } catch( NumberFormatException nfe ) {
            b = __min_value;
            if( !parse_error )
                b = Integer.parseInt( nfe.getMessage() );
            __txtField.setText( String.valueOf(b) );
            if( __show_error_message )
                JOptionPane.showMessageDialog( this, "El numero debe que ser un entero positivo en el rango[" + __min_value + ", " + __max_value + "]", "Error", JOptionPane.ERROR_MESSAGE);
        }
        return b;
    }

    public void undo_redo_action(final int __action)
    {
        try {
            if( __action == 0 )
                for( int i = 0; i < 5; i++ )
                    this.undoman.undo();
            else if( __action == 1 )
                for( int i = 0; i < 5; i++ )
                    this.undoman.redo();
        } catch( CannotUndoException uex ) {}
          catch( CannotRedoException rex ) {}
        this.main_code_options_btnUndo.setEnabled( this.undoman.canUndo() );
        this.main_code_options_btnRedo.setEnabled( this.undoman.canRedo() );
    }

    private static void setAttributeSet( SimpleAttributeSet att, Color bg, boolean bold )
    {
        //Setup the attribute.
        StyleConstants.setFontFamily( att, "Monospaced" );
        StyleConstants.setFontSize( att, 12 );
        StyleConstants.setBackground( att, bg );
        StyleConstants.setForeground( att, Color.black );
        StyleConstants.setBold( att, bold );
        StyleConstants.setItalic( att, false );
    }

    private void display_status(String fc) {
        javax.swing.JLabel tmp = null;
        if( fc == null )
            fc = "blue";
        int index = this.main_tabKarelArenaTabs.getSelectedIndex();
        switch (index) {
            case 0:
                this.main_status_lblStatus.setText("<html>mundo actual: <font color=\"" + fc+ "\">" + this.current_world_file + "</font></html>");
                break;
            case 1:
                this.main_status_lblStatus.setText("<html>codigo actual: <font color=\"" + fc+ "\">" + this.current_code_file + "</font></html>");
                break;
            case 3:
                this.main_status_lblStatus.setText("<html>Condiciones actuales: <font color=\"" + fc+ "\">" + this.current_coev_file + "</font></html>");
                break;
            default:
                this.main_status_lblStatus.setText("Sin informacion");
        }
    }

    private void update_status(int index, String status, String fc)
    {
        switch (index)
        {
            case 0: 
                this.current_world_file = status;
                break;
            case 1:
                this.current_code_file = status;
                break;
            case 3:
                this.current_coev_file = status;
                break;
        }
        this.display_status(fc);
    }


    // </editor-fold>

    // -----------------------------------------------------------------
    // <editor-fold defaultstate="collapsed" desc="World Tab Methods">
    private void reset_world() {
        // Interface
        this.env.clear();
        this.reset_world_scroll_values();

        // Prepare KarelEnviroment
        // ------------------------------------------------------------
        // Beeper bag
        this.main_world_options_txtBeepersBagKeyReleased(null);

        // Options
        this.world_change = false;
        this.main_world_options_btnSave.setEnabled(true);
        this.update_status(0, "", "blue");
    }

    public void reset_world_scroll_values()
    {
        this.world_resize = true;
        Rectangle world = ((KarelPanel)this.main_world_painter_pnlPainter).getWorld();
        this.main_world_painter_scrY.setMaximum( this.env.numCalles() - world.height );
        this.main_world_painter_scrY.setMinimum( 0);
        this.main_world_painter_scrY.setValue( this.env.numCalles() - world.height );

        this.main_world_painter_scrX.setMaximum( this.env.numAvenidas() - world.width );
        this.main_world_painter_scrX.setMinimum(0);
        this.main_world_painter_scrX.setValue(0);
        this.world_resize = false;
        this.main_world_painter_scrXAdjustmentValueChanged(null);
    }

    private boolean save_world(String filename) {
        try {
            KarelWorldManager.write_world(filename, this.env);
            //JOptionPane.showMessageDialog(this, "El mundo ha sido guardado correctamente", "Confirmacion", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "No se ha podido crear/reemplazar el archivo del mundo: " + new File(filename).getName(), "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    private void set_world_change_values(final boolean b)
    {
        this.main_world_options_btnSave.setEnabled(b);
        this.world_change = b;
        this.display_status( b ? "red" : "blue");
    }

    private void check_beeper_bag_content(java.awt.event.KeyEvent evt)
    {
        if( this.main_world_options_txtBeepersBag.getText().trim().isEmpty() )
            this.main_world_options_txtBeepersBag.setText("0");
        try
        {
            int b = 0;
            String data = this.main_world_options_txtBeepersBag.getText().trim();
            if( data.equals("INFINITO") )
                b = this.infBeepers;
            else
                b = Integer.parseInt( data );
            if( b < 0 )
                throw new NumberFormatException();
            if( b >= this.infBeepers )
            {
                this.main_world_options_txtBeepersBag.setText("INFINITO");
                b = this.infBeepers;
            }
            if( b == this.env.getKarel().getNumBeepers() )
                return;
            this.env.getKarel().setNumBeepers( b );
        } catch( NumberFormatException nfe ) {
            this.main_world_options_txtBeepersBag.setText("0");
            this.env.getKarel().setNumBeepers( 0 );
            //JOptionPane.showMessageDialog( null, "Los beepers en la mochila tienen que ser un entero positivo", "Error", JOptionPane.ERROR_MESSAGE);
        }
        this.set_world_change_values(true);
        ((KarelPanel)this.main_world_painter_pnlPainter).paint();
    }
    // </editor-fold>

    // -----------------------------------------------------------------
    // <editor-fold defaultstate="collapsed" desc="Code Tab Methods">
    private void reset_code()
    {
        // Options
        this.update_status(1, "", "blue");

        this.code_change = false;
        this.main_code_options_btnSave.setEnabled(true);
        
        
        // New code
        this.main_code_options_btnUndo.setEnabled(false);
        this.main_code_options_btnRedo.setEnabled(false);

        if( this.isVisible() )
        {
            this.main_code_editor_txtCode.setText( "class program\n{\n    program()\n    {\n        \n        turnoff();\n    }\n}\n" );
            this.main_code_editor_txtCode.setCaretPosition( 44 );
        }
        this.main_code_editor_txtCode.requestFocus();
    }

    private boolean save_code(String filename)
    {
        try {
            File file = new File(filename);
            BufferedWriter w = new BufferedWriter( new FileWriter( file ) );
            String str = this.main_code_editor_txtCode.getText();
            w.write( str );
            w.close();
            //JOptionPane.showMessageDialog(this, "El codigo ha sido guardado correctamente", "Confirmacion", JOptionPane.INFORMATION_MESSAGE);
        } catch( IOException ex ){ 
            JOptionPane.showMessageDialog( this, "No se pudo crear/reemplazar el archivo: " + new File(filename).getName(), "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    private void set_code_change_values(final boolean b)
    {
        this.main_code_options_btnSave.setEnabled(b);
        this.code_change = b;
        this.display_status( b ? "red" : "blue");
    }

    public void init_code_shortcut()
    {        
        this.main_code_editor_txtCode.getActionMap().put("undo", new AbstractAction("undo") {
            public void actionPerformed(ActionEvent evt) {
                undo_redo_action(0);
            }});
        this.main_code_editor_txtCode.getActionMap().put("redo", new AbstractAction("redo") {
            public void actionPerformed(ActionEvent evt) { 
                undo_redo_action(1);
            }});
            
        this.main_code_editor_txtCode.getActionMap().put("compile", new AbstractAction("compile") {
            public void actionPerformed(ActionEvent evt) { 
                main_code_options_btnCompileActionPerformed(null);
            }});

        this.main_code_editor_txtCode.getActionMap().put("language", new AbstractAction("language") {
            public void actionPerformed(ActionEvent evt) {
                main_code_options_btnChangeLangActionPerformed(null);
            }});

        this.main_code_editor_txtCode.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK), "new");
        this.main_code_editor_txtCode.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK), "open");
        this.main_code_editor_txtCode.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), "save");
        this.main_code_editor_txtCode.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "saveas");

        this.main_code_editor_txtCode.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "undo");
        this.main_code_editor_txtCode.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK), "redo");

        this.main_code_editor_txtCode.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.ALT_DOWN_MASK), "compile");
        this.main_code_editor_txtCode.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.ALT_DOWN_MASK), "language");
    }

    // </editor-fold>

    // -----------------------------------------------------------------
    // <editor-fold defaultstate="collapsed" desc="Exec Tab Methods">
    private void check_sleep_content(java.awt.event.KeyEvent evt)
    {
        this.ka_exec.setSleep( this.check_positive_content(this.main_exec_options_txtSleep, 0, 2000, false ) );
    }
    // </editor-fold>
    
    // -----------------------------------------------------------------
    // <editor-fold defaultstate="collapsed" desc="Coev Tab Methods">
    public void read_coev_world(int __tab)
    {
        if( __tab == 0 ) // read current world
            ((KarelPanel)(this.main_coev_painter_pnlPainter)).setEnvironment( (KarelEnvironment) this.env.clone() );
        else
            ((KarelPanel)(this.main_coev_painter_pnlPainter)).setEnvironment( (KarelEnvironment) ((KarelPanel)(this.main_exec_painter_pnlPainter)).getEnvironment().clone() );
        // Reset scroll
        this.reset_coev_scroll_values();

        // Centering Karel
        ((KarelPanel)(this.main_coev_painter_pnlPainter)).set_follow_karel(false);
        ((KarelPanel)(this.main_coev_painter_pnlPainter)).set_draw_tool_tip(true);
        this.track_karel();

        // Clear beepers on the world
        this.coev_beeper.clear();

        // adding current coev world values
        this.add_current_coev_world_values();
    }

    public void add_current_coev_world_values()
    {
        KarelEnvironment ke = (KarelEnvironment)( ((KarelPanel)this.main_coev_painter_pnlPainter).getEnvironment() );
        Point beeppos[] = ke.get_all_non_zero_beepers();

        // beepers
        for( int i = 0, n = beeppos.length; i < n; i++ )
            this.add_coev_beeper(beeppos[i].x + 1, beeppos[i].y + 1, ke.getBeepers(beeppos[i].x, beeppos[i].y));

        // Karel position
        this.main_coev_settings_txtPKA.setText( String.valueOf(ke.getKarel().getX() + 1) );
        this.main_coev_settings_txtPKC.setText( String.valueOf(ke.getKarel().getY() + 1) );
        this.main_coev_settings_cmbFace.setSelectedIndex( KarelWorldManager.KarelMDOToken.get_inverse_heading( ke.getKarel().getHeading() ) - 1 );
    }

    public void reset_coev_scroll_values()
    {
        Rectangle world = ((KarelPanel)this.main_coev_painter_pnlPainter).getWorld();
        this.coev_resize = true;
        this.main_coev_painter_scrY.setMaximum( this.env.numCalles() -  world.height);
        this.main_coev_painter_scrY.setMinimum( 0);
        this.main_coev_painter_scrY.setValue( this.env.numCalles() -  world.height );

        this.main_coev_painter_scrX.setMaximum( this.env.numAvenidas() - world.width);
        this.main_coev_painter_scrX.setMinimum(0);
        this.main_coev_painter_scrX.setValue(0);
        this.coev_resize = false;
        this.main_coev_painter_scrXAdjustmentValueChanged(null);
    }

    public void track_karel()
    {
        KarelEnvironment ke = ((KarelPanel)(this.main_coev_painter_pnlPainter)).getEnvironment();
        Rectangle world = ((KarelPanel)(this.main_coev_painter_pnlPainter)).getWorld();
        int x = ke.getKarel().getX();
        int y = ke.getKarel().getY();

        if( x + world.width/2 < ke.numAvenidas() )
            x = Math.max(x - world.width/2, 0);
        if( y + world.height/2 < ke.numCalles() )
            y = Math.max(y - world.height/2, 0);

        this.main_coev_painter_scrX.setValue( x );
        this.main_coev_painter_scrY.setValue( this.main_coev_painter_scrY.getMaximum() - y );
    }

    private void set_coev_change_values(final boolean b)
    {
        this.main_coev_options_btnSave.setEnabled(b);
        this.coev_change = b;
    }

    public void reset_coev_settings()
    {
        // First settings
        this.main_coev_settings_chkIns.setSelected(false);        this.main_coev_settings_txtIns.setText("");
        this.main_coev_settings_chkMove.setSelected(false);       this.main_coev_settings_txtMove.setText("");
        this.main_coev_settings_chkTurn.setSelected(false);       this.main_coev_settings_txtTurn.setText("");
        this.main_coev_settings_chkPick.setSelected(false);       this.main_coev_settings_txtPick.setText("");
        this.main_coev_settings_chkPut.setSelected(false);        this.main_coev_settings_txtPut.setText("");
        this.main_coev_settings_chkBeeperBag.setSelected(false);  this.main_coev_settings_txtBeeperBag.setText("");
        this.main_coev_settings_chkBeepersPos.setSelected(false); this.main_coev_settings_txtBeepersPos.setText("");

        // Second
        this.main_coev_settings_chkPKarel.setSelected(false);  this.main_coev_settings_txtPKA.setText(""); this.main_coev_settings_txtPKC.setText("");
        this.main_coev_settings_chkOKarel.setSelected(false);  this.main_coev_settings_cmbFace.setSelectedIndex(0);
        this.main_coev_settings_chkBeepers.setSelected(false);

        this.main_coev_settings_txtAddPKA.setText("");
        this.main_coev_settings_txtAddPKC.setText("");
        this.main_coev_settings_txtAddPKB.setText("");

        // Remove beepers
        this.coev_beeper.clear();
        this.coev_change = false;
        this.main_coev_options_btnSave.setEnabled(true);
    }

    public String prepare_coev_beeper(int a, int c, int beep)
    {

        if( beep < 0 )
            return "A: " + a + " C: " + c + " B: \\d+";
        return "A: " + a + " C: " + c + " B: " + beep;
    }

    public int exists_coev_beeper(int a, int c, int beep)
    {
        String target = this.prepare_coev_beeper(a, c, beep);
        for( int i = this.coev_beeper.size() - 1; i >= 0; i-- )
            if( ((String)this.coev_beeper.get(i)).matches( target )  )
                return i;
        return -1;
    }

    public void add_coev_beeper(int a, int c, int beep)
    {
        if( this.exists_coev_beeper(a, c, -1) < 0 )
            this.coev_beeper.addElement( this.prepare_coev_beeper(a, c, beep) );
    }

    public void remove_coev_beeper(int i)
    {
        if( 0 <= i && i < this.coev_beeper.size() )
            this.coev_beeper.remove(i);
    }

    public void remove_coev_beeper(int a, int c, int beep)
    {
        this.coev_beeper.removeElement( this.prepare_coev_beeper(a, c, beep) );
    }

    public void remove_coev_beepers_selected()
    {
        int index = 0;
        while( (index = this.main_coev_settings_lstBeepers.getMinSelectionIndex()) != -1 )
            this.remove_coev_beeper(index);
    }

    public void select_coev_beepers_equals(String __str_coev_beeper)
    {
        int count = 0;
        for( int i = 0, n = this.coev_beeper.size(); i < n; i++ )
            if( __str_coev_beeper.equals( (String)(this.coev_beeper.get(i))) )
                count++;
        
        int indices[] = new int[count]; count = 0;
        for( int i = 0, n = this.coev_beeper.size(); i < n; i++ )
            if( __str_coev_beeper.equals( (String)(this.coev_beeper.get(i))) )
                indices[count++] = i;
        this.main_coev_settings_lstBeepers.setSelectedIndices(indices);
    }

    public static int[] extract_values_from_coev_beeper(String __coev_beeper_parse)
    {
        int res[] = new int[3];
        String sbeeper[] = __coev_beeper_parse.split("\\p{Alpha}:");
        res[0] = Integer.parseInt(sbeeper[1].trim());
        res[1] = Integer.parseInt(sbeeper[2].trim());
        res[2] = Integer.parseInt(sbeeper[3].trim());
        return res;
    }

    public void load_settings_from_kec( KarelKECEnvironment __kke )
    {
        this.reset_coev_settings();
        
        KarelKECEnvironment.KECTokenType token[] = KarelKECEnvironment.KECTokenType.values();
        for( int i = 0; i < 7; i++ )
            if( __kke.is_set(token[i]) )
            {
                this.coev_field[i].setText( String.valueOf(__kke.get(token[i]).p2) );
                this.coev_check[i].setSelected(true);
            }
        if( __kke.is_set(token[7]) ) // Karel position
        {
            this.main_coev_settings_txtPKA.setText( String.valueOf(__kke.get(token[7]).p2) );
            this.main_coev_settings_txtPKC.setText( String.valueOf(__kke.get(token[7]).p3) );
            this.main_coev_settings_chkPKarel.setSelected(true);
        }
        if( __kke.is_set(token[8]) ) // Karel orientation
        {
            this.main_coev_settings_cmbFace.setSelectedIndex( __kke.get(token[8]).p3 );
            this.main_coev_settings_chkOKarel.setSelected(true);
        }
        if( __kke.is_set(token[9]) ) // Beepers on the world
        {
            this.main_coev_settings_chkBeepers.setSelected(true);
            for( int i = 0, n = __kke.beeper.length; i < n; i++ )
                this.add_coev_beeper(__kke.beeper[i].p1, __kke.beeper[i].p2, __kke.beeper[i].p3);
        }
    }

    public void load_kec_from_settings( KarelKECEnvironment __kke )
        throws NumberFormatException, Exception
    {
        KarelKECEnvironment.KECTokenType token[] = KarelKECEnvironment.KECTokenType.values();
        for( int i = 0; i < 7; i++ )
        {
            if( this.coev_check[i].isSelected() )
            {
                __kke.get(token[i]).p1 = 1;
                __kke.get(token[i]).p2 = Integer.parseInt( this.coev_field[i].getText() );
            }
            else
            {
                __kke.get(token[i]).p1 = 0;
                __kke.get(token[i]).p2 = 0;
            }
            __kke.get(token[i]).p3 = 0;
        }
        if( this.main_coev_settings_chkIns.isSelected() ) // Karel position
        {
            __kke.get(token[7]).p1 = 1;
            __kke.get(token[7]).p2 = Integer.parseInt(this.main_coev_settings_txtPKA.getText());
            __kke.get(token[7]).p3 = Integer.parseInt(this.main_coev_settings_txtPKC.getText());
        }
        if( this.main_coev_settings_chkIns.isSelected() ) // Karel orientation
        {
            __kke.get(token[8]).p1 = 1;
            __kke.get(token[8]).p2 = this.main_coev_settings_cmbFace.getSelectedIndex();
            __kke.get(token[8]).p3 = 0;
        }
        if( this.main_coev_settings_chkBeepers.isSelected() ) // World beepers
        {
            int n = this.coev_beeper.size();

            __kke.get(token[9]).p1 = 1;
            __kke.get(token[9]).p2 = n;
            __kke.get(token[9]).p3 = 0;
            int sbeeper[] = null;
            KarelWorldManager.KarelMDOToken beeper[] = new KarelWorldManager.KarelMDOToken[n];
            for( int i = 0; i < n; i++ )
            {
                sbeeper = KarelArena.extract_values_from_coev_beeper( (String)this.coev_beeper.get(i) );
                if( sbeeper.length != 3 )
                    throw new Exception("El formato de los zumbadores/beepers en el mundo es incorrecto");
                beeper[i] = new KarelWorldManager.KarelMDOToken( sbeeper[0], sbeeper[1], sbeeper[2] );
            }
            __kke.add_beepers(beeper);
        }
    }

    public boolean save_coev_kec(String __filename, KarelKECEnvironment __kke)
    {
        try
        {
            this.load_kec_from_settings(__kke);
            KarelKECManager.write_kec(__filename, __kke);
        } catch( NumberFormatException nfe){
            JOptionPane.showMessageDialog( this, "Algunos datos no son numericos", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        } catch (KarelException ke){
            JOptionPane.showMessageDialog( this, "El formato de las condiciones es incorrecto", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        } catch( IOException ioe){
            JOptionPane.showMessageDialog( this, "No se pudo crear/reemplazar el archivo: " + new File(__filename).getName(), "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        } catch( Exception exc){
            JOptionPane.showMessageDialog( this, exc.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        return true;
    }

    public void selected_coev_beeper_from_mouse_event(MouseEvent evt)
    {
        this.coev_current_position = ((KarelPanel)this.main_coev_painter_pnlPainter).get_world_position(evt);
        if( this.coev_current_position.x != -1 )
        {
            int index = this.exists_coev_beeper(this.coev_current_position.x + 1, this.coev_current_position.y + 1, -1);
            if( 0 <= index )
            {
                this.main_coev_settings_lstBeepers.setSelectedIndex(index);
                this.main_coev_settings_lstBeepers.ensureIndexIsVisible( this.main_coev_settings_lstBeepers.getSelectedIndex() );
            }
        }
    }

    public void process_coev_mouse_event(MouseEvent evt, String __action)
    {
        __action = __action.toLowerCase();
        if( __action.equals("display") )
        {
            this.main_coev_settings_lstBeepers.clearSelection();
            this.selected_coev_beeper_from_mouse_event(evt);
            ((KarelPanel)this.main_coev_painter_pnlPainter).set_display_gray_state( this.main_coev_settings_lstBeepers.isSelectionEmpty() );
        }
        else if( __action.startsWith("add") ) // add position
        {
            this.coev_current_position = ((KarelPanel)this.main_coev_painter_pnlPainter).get_world_position(evt);
            if( this.coev_current_position.x != -1 )
            {
                int beepers = ((KarelPanel)this.main_coev_painter_pnlPainter).getBeeperQtty();
                int a = this.coev_current_position.x + 1, c = this.coev_current_position.y + 1, index = this.exists_coev_beeper(a, c, -1);
                if( index < 0 )
                {
                    this.add_coev_beeper(a, c, beepers);
                    ((KarelPanel)(this.main_coev_painter_pnlPainter)).paint();
                }
                else if( __action.endsWith("click") )
                {
                    String current_select_value = this.main_coev_settings_lstBeepers.getSelectedValue().toString();
                    this.process_coev_mouse_event(evt, "remove");

                    // Uncomment next lines if you want to replace value instance to remove if current beepers are different
                    //int current_coev_beeper[] = KarelArena.extract_values_from_coev_beeper( current_select_value );
                    //if( beepers != current_coev_beeper[2] )
                    //    this.add_coev_beeper(a, c, beepers);
                }
            }
        }
        else if( __action.startsWith("remove") )
        {
            this.selected_coev_beeper_from_mouse_event(evt);
            this.remove_coev_beepers_selected();
            ((KarelPanel)(this.main_coev_painter_pnlPainter)).paint();
        }
    }

    // </editor-fold>

    // -----------------------------------------------------------------
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        main_pop_world = new javax.swing.JPopupMenu();
        main_pop_world_karel = new javax.swing.JMenu();
        main_pop_world_karel_north = new javax.swing.JMenuItem();
        main_pop_world_karel_south = new javax.swing.JMenuItem();
        main_pop_world_karel_east = new javax.swing.JMenuItem();
        main_pop_world_karel_west = new javax.swing.JMenuItem();
        main_pop_world_jSeparator1 = new javax.swing.JPopupMenu.Separator();
        main_pop_world_beepers_mni0 = new javax.swing.JMenuItem();
        main_pop_world_beepers_mni1 = new javax.swing.JMenuItem();
        main_pop_world_beepers_mni2 = new javax.swing.JMenuItem();
        main_pop_world_beepers_mni3 = new javax.swing.JMenuItem();
        main_pop_world_beepers_mni4 = new javax.swing.JMenuItem();
        main_pop_world_beepers_mni5 = new javax.swing.JMenuItem();
        main_pop_world_beepers_mni6 = new javax.swing.JMenuItem();
        main_pop_world_beepers_mni7 = new javax.swing.JMenuItem();
        main_pop_world_beepers_mni8 = new javax.swing.JMenuItem();
        main_pop_world_beepers_mni9 = new javax.swing.JMenuItem();
        main_pop_world_beepers_mniN = new javax.swing.JMenuItem();
        main_pop_world_beepers_mniInf = new javax.swing.JMenuItem();
        main_tabKarelArenaTabs = new javax.swing.JTabbedPane();
        main_world = new javax.swing.JPanel();
        main_world_painter = new javax.swing.JPanel();
        main_world_painter_scrY = new javax.swing.JScrollBar();
        main_world_painter_pnlPainter = new KarelPanel( this.env, true );
        main_world_painter_scrX = new javax.swing.JScrollBar();
        main_world_options_drawing = new javax.swing.JPanel();
        main_world_options_jLabel1 = new javax.swing.JLabel();
        main_world_options_txtBeepersBag = new javax.swing.JTextField();
        main_world_options_btnInfBag = new javax.swing.JButton();
        main_world_options_btnNew = new javax.swing.JButton();
        main_world_options_btnSave = new javax.swing.JButton();
        main_world_options_btnSaveAs = new javax.swing.JButton();
        main_world_options_btnOpen = new javax.swing.JButton();
        main_code = new javax.swing.JPanel();
        main_code_options_coding = new javax.swing.JPanel();
        main_code_options_btnNew = new javax.swing.JButton();
        main_code_options_btnSave = new javax.swing.JButton();
        main_code_options_btnSaveAs = new javax.swing.JButton();
        main_code_options_btnOpen = new javax.swing.JButton();
        main_code_options_btnCompile = new javax.swing.JButton();
        main_code_options_btnCopy = new javax.swing.JButton();
        main_code_options_btnPaste = new javax.swing.JButton();
        main_code_options_btnCut = new javax.swing.JButton();
        main_code_options_btnUndo = new javax.swing.JButton();
        main_code_options_btnRedo = new javax.swing.JButton();
        main_code_options_btnChangeLang = new javax.swing.JButton();
        main_code_editor_container = new javax.swing.JScrollPane();
        main_code_editor_txtCode = new NonWrappingTextPane( this.code_document );
        main_exec = new javax.swing.JPanel();
        main_exec_options_debug = new javax.swing.JPanel();
        main_exec_options_btnRun = new javax.swing.JButton();
        main_exec_options_btnRestart = new javax.swing.JButton();
        main_exec_options_btnNextStep = new javax.swing.JButton();
        main_exec_options_jLabel1 = new javax.swing.JLabel();
        main_exec_options_txtSleep = new javax.swing.JTextField();
        main_exec_splitpane = new javax.swing.JSplitPane();
        main_exec_code_codecontainer = new javax.swing.JScrollPane();
        main_exec_display_txtCode = new NonWrappingTextPane( this.exec_document );
        main_exec_painter_pnlPainter = new KarelPanel( (KarelEnvironment) this.env.clone() );
        main_coev = new javax.swing.JPanel();
        main_coev_options_container = new javax.swing.JPanel();
        main_coev_options_btnNew = new javax.swing.JButton();
        main_coev_options_btnSave = new javax.swing.JButton();
        main_coev_options_btnSaveAs = new javax.swing.JButton();
        main_coev_options_btnOpen = new javax.swing.JButton();
        main_coev_options_btnReadWorld = new javax.swing.JButton();
        main_coev_options_btnReadExec = new javax.swing.JButton();
        main_coev_options_btnTrackKarel = new javax.swing.JButton();
        main_coev_container = new javax.swing.JSplitPane();
        main_coev_painter_container = new javax.swing.JPanel();
        main_coev_painter_scrY = new javax.swing.JScrollBar();
        main_coev_painter_pnlPainter = new KarelPanel( (KarelEnvironment) this.env.clone() );
        main_coev_painter_scrX = new javax.swing.JScrollBar();
        main_coev_settings_container = new javax.swing.JPanel();
        main_coev_settings_chkTurn = new javax.swing.JCheckBox();
        main_coev_settings_txtPick = new javax.swing.JTextField();
        main_coev_settings_chkIns = new javax.swing.JCheckBox();
        main_coev_settings_txtMove = new javax.swing.JTextField();
        main_coev_settings_txtIns = new javax.swing.JTextField();
        main_coev_settings_txtTurn = new javax.swing.JTextField();
        main_coev_settings_chkMove = new javax.swing.JCheckBox();
        main_coev_settings_chkPick = new javax.swing.JCheckBox();
        main_coev_settings_chkPut = new javax.swing.JCheckBox();
        main_coev_settings_txtPut = new javax.swing.JTextField();
        main_coev_settings_jLabel1 = new javax.swing.JLabel();
        main_coev_settings_jLabel2 = new javax.swing.JLabel();
        main_coev_settings_chkPKarel = new javax.swing.JCheckBox();
        main_coev_settings_chkOKarel = new javax.swing.JCheckBox();
        main_coev_settings_jLabel3 = new javax.swing.JLabel();
        main_coev_settings_txtPKA = new javax.swing.JTextField();
        main_coev_settings_jLabel4 = new javax.swing.JLabel();
        main_coev_settings_txtPKC = new javax.swing.JTextField();
        main_coev_settings_cmbFace = new javax.swing.JComboBox();
        main_coev_settings_chkBeepers = new javax.swing.JCheckBox();
        main_coev_settings_jScrollPane1 = new javax.swing.JScrollPane();
        main_coev_settings_lstBeepers = new javax.swing.JList();
        main_coev_settings_jLabel5 = new javax.swing.JLabel();
        main_coev_settings_jLabel6 = new javax.swing.JLabel();
        main_coev_settings_txtAddPKA = new javax.swing.JTextField();
        main_coev_settings_txtAddPKC = new javax.swing.JTextField();
        main_coev_settings_jLabel7 = new javax.swing.JLabel();
        main_coev_settings_txtAddPKB = new javax.swing.JTextField();
        main_coev_settings_jLabel8 = new javax.swing.JLabel();
        main_coev_settings_btnAddPK = new javax.swing.JButton();
        main_coev_settings_chkBeeperBag = new javax.swing.JCheckBox();
        main_coev_settings_txtBeeperBag = new javax.swing.JTextField();
        main_coev_settings_chkBeepersPos = new javax.swing.JCheckBox();
        main_coev_settings_txtBeepersPos = new javax.swing.JTextField();
        main_coev_settings_btnClear = new javax.swing.JButton();
        main_eval = new javax.swing.JPanel();
        main_help_options_container = new javax.swing.JPanel();
        main_help_options_btnSwitch = new javax.swing.JToggleButton();
        main_help_options_container1 = new javax.swing.JPanel();
        main_status_container = new javax.swing.JPanel();
        main_status_lblStatus = new javax.swing.JLabel();

        main_pop_world.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                main_pop_worldKeyReleased(evt);
            }
        });

        main_pop_world_karel.setMnemonic('K');
        main_pop_world_karel.setText("Karel");
        main_pop_world_karel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                main_pop_world_handler(evt);
            }
        });

        main_pop_world_karel_north.setMnemonic('N');
        main_pop_world_karel_north.setText("hacial el Norte");
        main_pop_world_karel_north.setActionCommand("KN");
        main_pop_world_karel_north.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                main_pop_world_handler(evt);
            }
        });
        main_pop_world_karel.add(main_pop_world_karel_north);

        main_pop_world_karel_south.setMnemonic('S');
        main_pop_world_karel_south.setText("hacia el Sur");
        main_pop_world_karel_south.setActionCommand("KS");
        main_pop_world_karel_south.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                main_pop_world_handler(evt);
            }
        });
        main_pop_world_karel.add(main_pop_world_karel_south);

        main_pop_world_karel_east.setMnemonic('E');
        main_pop_world_karel_east.setText("hacia el Este");
        main_pop_world_karel_east.setActionCommand("KE");
        main_pop_world_karel_east.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                main_pop_world_handler(evt);
            }
        });
        main_pop_world_karel.add(main_pop_world_karel_east);

        main_pop_world_karel_west.setMnemonic('O');
        main_pop_world_karel_west.setText("hacia el Oeste");
        main_pop_world_karel_west.setActionCommand("KW");
        main_pop_world_karel_west.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                main_pop_world_handler(evt);
            }
        });
        main_pop_world_karel.add(main_pop_world_karel_west);

        main_pop_world.add(main_pop_world_karel);
        main_pop_world.add(main_pop_world_jSeparator1);

        main_pop_world_beepers_mni0.setMnemonic('0');
        main_pop_world_beepers_mni0.setText("0 zumbadores / beepers");
        main_pop_world_beepers_mni0.setActionCommand("0");
        main_pop_world_beepers_mni0.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                main_pop_world_handler(evt);
            }
        });
        main_pop_world.add(main_pop_world_beepers_mni0);

        main_pop_world_beepers_mni1.setMnemonic('1');
        main_pop_world_beepers_mni1.setText("1 zumbador / beeper");
        main_pop_world_beepers_mni1.setActionCommand("1");
        main_pop_world_beepers_mni1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                main_pop_world_handler(evt);
            }
        });
        main_pop_world.add(main_pop_world_beepers_mni1);

        main_pop_world_beepers_mni2.setMnemonic('2');
        main_pop_world_beepers_mni2.setText("2 zumbadores / beepers");
        main_pop_world_beepers_mni2.setActionCommand("2");
        main_pop_world_beepers_mni2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                main_pop_world_handler(evt);
            }
        });
        main_pop_world.add(main_pop_world_beepers_mni2);

        main_pop_world_beepers_mni3.setMnemonic('3');
        main_pop_world_beepers_mni3.setText("3 zumbadores / beepers");
        main_pop_world_beepers_mni3.setActionCommand("3");
        main_pop_world_beepers_mni3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                main_pop_world_handler(evt);
            }
        });
        main_pop_world.add(main_pop_world_beepers_mni3);

        main_pop_world_beepers_mni4.setMnemonic('4');
        main_pop_world_beepers_mni4.setText("4 zumbadores / beepers");
        main_pop_world_beepers_mni4.setActionCommand("4");
        main_pop_world_beepers_mni4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                main_pop_world_handler(evt);
            }
        });
        main_pop_world.add(main_pop_world_beepers_mni4);

        main_pop_world_beepers_mni5.setMnemonic('5');
        main_pop_world_beepers_mni5.setText("5 zumbadores / beepers");
        main_pop_world_beepers_mni5.setActionCommand("5");
        main_pop_world_beepers_mni5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                main_pop_world_handler(evt);
            }
        });
        main_pop_world.add(main_pop_world_beepers_mni5);

        main_pop_world_beepers_mni6.setMnemonic('6');
        main_pop_world_beepers_mni6.setText("6 zumbadores / beepers");
        main_pop_world_beepers_mni6.setActionCommand("6");
        main_pop_world_beepers_mni6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                main_pop_world_handler(evt);
            }
        });
        main_pop_world.add(main_pop_world_beepers_mni6);

        main_pop_world_beepers_mni7.setMnemonic('7');
        main_pop_world_beepers_mni7.setText("7 zumbadores / beepers");
        main_pop_world_beepers_mni7.setActionCommand("7");
        main_pop_world_beepers_mni7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                main_pop_world_handler(evt);
            }
        });
        main_pop_world.add(main_pop_world_beepers_mni7);

        main_pop_world_beepers_mni8.setMnemonic('8');
        main_pop_world_beepers_mni8.setText("8 zumbadores / beepers");
        main_pop_world_beepers_mni8.setActionCommand("8");
        main_pop_world_beepers_mni8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                main_pop_world_handler(evt);
            }
        });
        main_pop_world.add(main_pop_world_beepers_mni8);

        main_pop_world_beepers_mni9.setMnemonic('9');
        main_pop_world_beepers_mni9.setText("9 zumbadores / beepers");
        main_pop_world_beepers_mni9.setActionCommand("9");
        main_pop_world_beepers_mni9.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                main_pop_world_handler(evt);
            }
        });
        main_pop_world.add(main_pop_world_beepers_mni9);

        main_pop_world_beepers_mniN.setMnemonic('N');
        main_pop_world_beepers_mniN.setText("N zumbadores / beepers");
        main_pop_world_beepers_mniN.setActionCommand("N");
        main_pop_world_beepers_mniN.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                main_pop_world_handler(evt);
            }
        });
        main_pop_world.add(main_pop_world_beepers_mniN);

        main_pop_world_beepers_mniInf.setMnemonic('I');
        main_pop_world_beepers_mniInf.setText("INFINITOS zumbadores / beepers");
        main_pop_world_beepers_mniInf.setActionCommand("INFINITOS");
        main_pop_world_beepers_mniInf.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                main_pop_world_handler(evt);
            }
        });
        main_pop_world.add(main_pop_world_beepers_mniInf);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        setName("frame"); // NOI18N
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        main_tabKarelArenaTabs.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                main_tabKarelArenaTabsStateChanged(evt);
            }
        });

        main_world_painter.setBackground(new java.awt.Color(220, 220, 220));

        main_world_painter_scrY.setVisibleAmount(0);
        main_world_painter_scrY.addAdjustmentListener(new java.awt.event.AdjustmentListener() {
            public void adjustmentValueChanged(java.awt.event.AdjustmentEvent evt) {
                main_world_painter_scrYAdjustmentValueChanged(evt);
            }
        });

        main_world_painter_pnlPainter.setBackground(new java.awt.Color(200, 200, 200));
        main_world_painter_pnlPainter.addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                main_world_painter_pnlPainterMouseWheelMoved(evt);
            }
        });
        main_world_painter_pnlPainter.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                main_world_painter_pnlPainterMouseClicked(evt);
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                main_world_painter_pnlPainterMousePressed(evt);
            }
        });
        main_world_painter_pnlPainter.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                main_world_painter_pnlPainterComponentResized(evt);
            }
        });
        main_world_painter_pnlPainter.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                main_world_painter_pnlPainterKeyPressed(evt);
            }
            public void keyReleased(java.awt.event.KeyEvent evt) {
                main_world_painter_pnlPainterKeyReleased(evt);
            }
        });

        javax.swing.GroupLayout main_world_painter_pnlPainterLayout = new javax.swing.GroupLayout(main_world_painter_pnlPainter);
        main_world_painter_pnlPainter.setLayout(main_world_painter_pnlPainterLayout);
        main_world_painter_pnlPainterLayout.setHorizontalGroup(
            main_world_painter_pnlPainterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 710, Short.MAX_VALUE)
        );
        main_world_painter_pnlPainterLayout.setVerticalGroup(
            main_world_painter_pnlPainterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 431, Short.MAX_VALUE)
        );

        main_world_painter_scrX.setOrientation(javax.swing.JScrollBar.HORIZONTAL);
        main_world_painter_scrX.setVisibleAmount(0);
        main_world_painter_scrX.addAdjustmentListener(new java.awt.event.AdjustmentListener() {
            public void adjustmentValueChanged(java.awt.event.AdjustmentEvent evt) {
                main_world_painter_scrXAdjustmentValueChanged(evt);
            }
        });

        javax.swing.GroupLayout main_world_painterLayout = new javax.swing.GroupLayout(main_world_painter);
        main_world_painter.setLayout(main_world_painterLayout);
        main_world_painterLayout.setHorizontalGroup(
            main_world_painterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(main_world_painterLayout.createSequentialGroup()
                .addComponent(main_world_painter_scrY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(main_world_painter_pnlPainter, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addComponent(main_world_painter_scrX, javax.swing.GroupLayout.DEFAULT_SIZE, 733, Short.MAX_VALUE)
        );
        main_world_painterLayout.setVerticalGroup(
            main_world_painterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(main_world_painterLayout.createSequentialGroup()
                .addGroup(main_world_painterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(main_world_painter_pnlPainter, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(main_world_painter_scrY, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 431, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(main_world_painter_scrX, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        main_world_options_drawing.setBackground(new java.awt.Color(220, 220, 220));

        main_world_options_jLabel1.setFont(new java.awt.Font("Tahoma", 1, 10));
        main_world_options_jLabel1.setText("Mochila:");

        main_world_options_txtBeepersBag.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        main_world_options_txtBeepersBag.setText("0");
        main_world_options_txtBeepersBag.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                main_world_options_txtBeepersBagKeyReleased(evt);
            }
        });

        main_world_options_btnInfBag.setFont(new java.awt.Font("Times New Roman", 1, 11));
        main_world_options_btnInfBag.setIcon(new javax.swing.ImageIcon(getClass().getResource("/application/karel/images/karel/infinity.png"))); // NOI18N
        main_world_options_btnInfBag.setToolTipText("infinitos beepers");
        main_world_options_btnInfBag.setActionCommand("infBag");
        main_world_options_btnInfBag.setFocusable(false);
        main_world_options_btnInfBag.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                main_world_options_btnInfBagActionPerformed(evt);
            }
        });

        main_world_options_btnNew.setIcon(new javax.swing.ImageIcon(getClass().getResource("/application/karel/images/world/world_new.png"))); // NOI18N
        main_world_options_btnNew.setToolTipText("mundo nuevo");
        main_world_options_btnNew.setActionCommand("new_world");
        main_world_options_btnNew.setFocusable(false);
        main_world_options_btnNew.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                main_world_options_btnNewActionPerformed(evt);
            }
        });

        main_world_options_btnSave.setIcon(new javax.swing.ImageIcon(getClass().getResource("/application/karel/images/world/world_save.png"))); // NOI18N
        main_world_options_btnSave.setToolTipText("guardar mundo");
        main_world_options_btnSave.setActionCommand("save_world");
        main_world_options_btnSave.setFocusable(false);
        main_world_options_btnSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                main_world_options_btnSaveActionPerformed(evt);
            }
        });

        main_world_options_btnSaveAs.setIcon(new javax.swing.ImageIcon(getClass().getResource("/application/karel/images/world/world_saveas.png"))); // NOI18N
        main_world_options_btnSaveAs.setToolTipText("guardar mundo como");
        main_world_options_btnSaveAs.setActionCommand("saveas_world");
        main_world_options_btnSaveAs.setFocusable(false);
        main_world_options_btnSaveAs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                main_world_options_btnSaveAsActionPerformed(evt);
            }
        });

        main_world_options_btnOpen.setIcon(new javax.swing.ImageIcon(getClass().getResource("/application/karel/images/world/world_open.png"))); // NOI18N
        main_world_options_btnOpen.setToolTipText("abrir mundo");
        main_world_options_btnOpen.setFocusable(false);
        main_world_options_btnOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                main_world_options_btnOpenActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout main_world_options_drawingLayout = new javax.swing.GroupLayout(main_world_options_drawing);
        main_world_options_drawing.setLayout(main_world_options_drawingLayout);
        main_world_options_drawingLayout.setHorizontalGroup(
            main_world_options_drawingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(main_world_options_drawingLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(main_world_options_btnNew, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(main_world_options_btnOpen, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(main_world_options_btnSave, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(main_world_options_btnSaveAs, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(main_world_options_drawingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(main_world_options_jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(main_world_options_txtBeepersBag, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(main_world_options_btnInfBag, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(371, 371, 371))
        );
        main_world_options_drawingLayout.setVerticalGroup(
            main_world_options_drawingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, main_world_options_drawingLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(main_world_options_drawingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(main_world_options_btnNew, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(main_world_options_btnSaveAs, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, main_world_options_drawingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(main_world_options_btnSave, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 50, Short.MAX_VALUE)
                        .addComponent(main_world_options_btnOpen, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 50, Short.MAX_VALUE))
                    .addComponent(main_world_options_btnInfBag, javax.swing.GroupLayout.DEFAULT_SIZE, 50, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, main_world_options_drawingLayout.createSequentialGroup()
                        .addComponent(main_world_options_jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(main_world_options_txtBeepersBag, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );

        javax.swing.GroupLayout main_worldLayout = new javax.swing.GroupLayout(main_world);
        main_world.setLayout(main_worldLayout);
        main_worldLayout.setHorizontalGroup(
            main_worldLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, main_worldLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(main_worldLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(main_world_painter, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(main_world_options_drawing, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 733, Short.MAX_VALUE))
                .addContainerGap())
        );
        main_worldLayout.setVerticalGroup(
            main_worldLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(main_worldLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(main_world_options_drawing, javax.swing.GroupLayout.PREFERRED_SIZE, 69, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(main_world_painter, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        main_tabKarelArenaTabs.addTab("Mundo", main_world);

        main_code_options_coding.setBackground(new java.awt.Color(220, 220, 220));

        main_code_options_btnNew.setIcon(new javax.swing.ImageIcon(getClass().getResource("/application/karel/images/code/code_new.png"))); // NOI18N
        main_code_options_btnNew.setToolTipText("codigo nuevo");
        main_code_options_btnNew.setActionCommand("new_code");
        main_code_options_btnNew.setFocusable(false);
        main_code_options_btnNew.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                main_code_options_btnNewActionPerformed(evt);
            }
        });

        main_code_options_btnSave.setIcon(new javax.swing.ImageIcon(getClass().getResource("/application/karel/images/code/code_save.png"))); // NOI18N
        main_code_options_btnSave.setToolTipText("guardar codigo");
        main_code_options_btnSave.setActionCommand("save_code");
        main_code_options_btnSave.setFocusable(false);
        main_code_options_btnSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                main_code_options_btnSaveActionPerformed(evt);
            }
        });

        main_code_options_btnSaveAs.setIcon(new javax.swing.ImageIcon(getClass().getResource("/application/karel/images/code/code_saveas.png"))); // NOI18N
        main_code_options_btnSaveAs.setToolTipText("guardar codigo como");
        main_code_options_btnSaveAs.setActionCommand("saveas_code");
        main_code_options_btnSaveAs.setFocusable(false);
        main_code_options_btnSaveAs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                main_code_options_btnSaveAsActionPerformed(evt);
            }
        });

        main_code_options_btnOpen.setIcon(new javax.swing.ImageIcon(getClass().getResource("/application/karel/images/code/code_open.png"))); // NOI18N
        main_code_options_btnOpen.setToolTipText("abrir codigo");
        main_code_options_btnOpen.setActionCommand("open_code");
        main_code_options_btnOpen.setFocusable(false);
        main_code_options_btnOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                main_code_options_btnOpenActionPerformed(evt);
            }
        });

        main_code_options_btnCompile.setIcon(new javax.swing.ImageIcon(getClass().getResource("/application/karel/images/code/code_compile.png"))); // NOI18N
        main_code_options_btnCompile.setToolTipText("compilar");
        main_code_options_btnCompile.setActionCommand("compile_code");
        main_code_options_btnCompile.setFocusable(false);
        main_code_options_btnCompile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                main_code_options_btnCompileActionPerformed(evt);
            }
        });

        main_code_options_btnCopy.setIcon(new javax.swing.ImageIcon(getClass().getResource("/application/karel/images/code/code_copy.png"))); // NOI18N
        main_code_options_btnCopy.setToolTipText("copiar");
        main_code_options_btnCopy.setActionCommand("copy_text");
        main_code_options_btnCopy.setFocusable(false);
        main_code_options_btnCopy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                main_code_options_btnCopyActionPerformed(evt);
            }
        });

        main_code_options_btnPaste.setIcon(new javax.swing.ImageIcon(getClass().getResource("/application/karel/images/code/code_paste.png"))); // NOI18N
        main_code_options_btnPaste.setToolTipText("pegar");
        main_code_options_btnPaste.setActionCommand("paste_text");
        main_code_options_btnPaste.setFocusable(false);
        main_code_options_btnPaste.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                main_code_options_btnPasteActionPerformed(evt);
            }
        });

        main_code_options_btnCut.setIcon(new javax.swing.ImageIcon(getClass().getResource("/application/karel/images/code/code_cut.png"))); // NOI18N
        main_code_options_btnCut.setToolTipText("cortar");
        main_code_options_btnCut.setActionCommand("cut_text");
        main_code_options_btnCut.setFocusable(false);
        main_code_options_btnCut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                main_code_options_btnCutActionPerformed(evt);
            }
        });

        main_code_options_btnUndo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/application/karel/images/code/code_undo.png"))); // NOI18N
        main_code_options_btnUndo.setToolTipText("deshacer");
        main_code_options_btnUndo.setActionCommand("undo_code");
        main_code_options_btnUndo.setFocusable(false);
        main_code_options_btnUndo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                main_code_options_btnUndoActionPerformed(evt);
            }
        });

        main_code_options_btnRedo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/application/karel/images/code/code_redo.png"))); // NOI18N
        main_code_options_btnRedo.setToolTipText("rehacer");
        main_code_options_btnRedo.setActionCommand("redo_code");
        main_code_options_btnRedo.setFocusable(false);
        main_code_options_btnRedo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                main_code_options_btnRedoActionPerformed(evt);
            }
        });

        main_code_options_btnChangeLang.setIcon(new javax.swing.ImageIcon(getClass().getResource("/application/karel/images/code/code_java.png"))); // NOI18N
        main_code_options_btnChangeLang.setToolTipText("cambiar a pascal");
        main_code_options_btnChangeLang.setActionCommand("java");
        main_code_options_btnChangeLang.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/application/karel/images/code/code_pascal.png"))); // NOI18N
        main_code_options_btnChangeLang.setFocusable(false);
        main_code_options_btnChangeLang.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                main_code_options_btnChangeLangActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout main_code_options_codingLayout = new javax.swing.GroupLayout(main_code_options_coding);
        main_code_options_coding.setLayout(main_code_options_codingLayout);
        main_code_options_codingLayout.setHorizontalGroup(
            main_code_options_codingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(main_code_options_codingLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(main_code_options_btnNew, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(main_code_options_btnOpen, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(main_code_options_btnSave, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(main_code_options_btnSaveAs, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(main_code_options_btnCopy, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(main_code_options_btnCut, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(main_code_options_btnPaste, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(main_code_options_btnUndo, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(main_code_options_btnRedo, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(main_code_options_btnCompile, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(main_code_options_btnChangeLang, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(77, Short.MAX_VALUE))
        );
        main_code_options_codingLayout.setVerticalGroup(
            main_code_options_codingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(main_code_options_codingLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(main_code_options_codingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(main_code_options_btnChangeLang, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(main_code_options_btnCompile, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(main_code_options_btnRedo, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(main_code_options_btnUndo, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(main_code_options_btnPaste, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(main_code_options_btnCut, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(main_code_options_btnCopy, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(main_code_options_btnSave, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(main_code_options_btnOpen, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(main_code_options_btnSaveAs, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(main_code_options_btnNew, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        main_code_editor_txtCode.setFont(new java.awt.Font("Courier New", 0, 12));
        main_code_editor_txtCode.setSelectionColor(new java.awt.Color(153, 204, 255));
        main_code_editor_txtCode.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                main_code_editor_txtCodeKeyTyped(evt);
            }
        });
        main_code_editor_container.setViewportView(main_code_editor_txtCode);

        javax.swing.GroupLayout main_codeLayout = new javax.swing.GroupLayout(main_code);
        main_code.setLayout(main_codeLayout);
        main_codeLayout.setHorizontalGroup(
            main_codeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, main_codeLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(main_codeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(main_code_options_coding, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(main_code_editor_container, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 733, Short.MAX_VALUE))
                .addContainerGap())
        );
        main_codeLayout.setVerticalGroup(
            main_codeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(main_codeLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(main_code_options_coding, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(4, 4, 4)
                .addComponent(main_code_editor_container, javax.swing.GroupLayout.DEFAULT_SIZE, 453, Short.MAX_VALUE)
                .addContainerGap())
        );

        main_tabKarelArenaTabs.addTab("Codigo", main_code);

        main_exec_options_debug.setBackground(new java.awt.Color(220, 220, 220));

        main_exec_options_btnRun.setIcon(new javax.swing.ImageIcon(getClass().getResource("/application/karel/images/exec/exec_run.png"))); // NOI18N
        main_exec_options_btnRun.setToolTipText("correr programa");
        main_exec_options_btnRun.setActionCommand("run_program");
        main_exec_options_btnRun.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                main_exec_options_btnRunActionPerformed(evt);
            }
        });

        main_exec_options_btnRestart.setIcon(new javax.swing.ImageIcon(getClass().getResource("/application/karel/images/exec/exec_restart.png"))); // NOI18N
        main_exec_options_btnRestart.setToolTipText("inicializar");
        main_exec_options_btnRestart.setActionCommand("restart");
        main_exec_options_btnRestart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                main_exec_options_btnRestartActionPerformed(evt);
            }
        });

        main_exec_options_btnNextStep.setIcon(new javax.swing.ImageIcon(getClass().getResource("/application/karel/images/exec/exec_step.png"))); // NOI18N
        main_exec_options_btnNextStep.setToolTipText("siguiente instruccion");
        main_exec_options_btnNextStep.setActionCommand("next_step");
        main_exec_options_btnNextStep.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                main_exec_options_btnNextStepActionPerformed(evt);
            }
        });
        main_exec_options_btnNextStep.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                main_exec_options_btnNextStepKeyTyped(evt);
            }
        });

        main_exec_options_jLabel1.setFont(new java.awt.Font("Tahoma", 1, 10));
        main_exec_options_jLabel1.setText("Retardo:");

        main_exec_options_txtSleep.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        main_exec_options_txtSleep.setText("500");
        main_exec_options_txtSleep.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                main_exec_options_txtSleepKeyReleased(evt);
            }
        });

        javax.swing.GroupLayout main_exec_options_debugLayout = new javax.swing.GroupLayout(main_exec_options_debug);
        main_exec_options_debug.setLayout(main_exec_options_debugLayout);
        main_exec_options_debugLayout.setHorizontalGroup(
            main_exec_options_debugLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(main_exec_options_debugLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(main_exec_options_btnRun, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(main_exec_options_btnNextStep, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(main_exec_options_btnRestart, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(main_exec_options_debugLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(main_exec_options_txtSleep)
                    .addComponent(main_exec_options_jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, 67, Short.MAX_VALUE))
                .addContainerGap(484, Short.MAX_VALUE))
        );
        main_exec_options_debugLayout.setVerticalGroup(
            main_exec_options_debugLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, main_exec_options_debugLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(main_exec_options_debugLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(main_exec_options_debugLayout.createSequentialGroup()
                        .addComponent(main_exec_options_jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(main_exec_options_txtSleep, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(main_exec_options_btnRestart, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(main_exec_options_debugLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(main_exec_options_btnRun, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(main_exec_options_btnNextStep, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );

        main_exec_splitpane.setDividerLocation(250);

        main_exec_display_txtCode.setEditable(false);
        main_exec_display_txtCode.setFont(new java.awt.Font("Courier New", 0, 12));
        main_exec_display_txtCode.setDoubleBuffered(true);
        main_exec_display_txtCode.setFocusable(false);
        main_exec_code_codecontainer.setViewportView(main_exec_display_txtCode);

        main_exec_splitpane.setLeftComponent(main_exec_code_codecontainer);

        javax.swing.GroupLayout main_exec_painter_pnlPainterLayout = new javax.swing.GroupLayout(main_exec_painter_pnlPainter);
        main_exec_painter_pnlPainter.setLayout(main_exec_painter_pnlPainterLayout);
        main_exec_painter_pnlPainterLayout.setHorizontalGroup(
            main_exec_painter_pnlPainterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 477, Short.MAX_VALUE)
        );
        main_exec_painter_pnlPainterLayout.setVerticalGroup(
            main_exec_painter_pnlPainterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 449, Short.MAX_VALUE)
        );

        main_exec_splitpane.setRightComponent(main_exec_painter_pnlPainter);

        javax.swing.GroupLayout main_execLayout = new javax.swing.GroupLayout(main_exec);
        main_exec.setLayout(main_execLayout);
        main_execLayout.setHorizontalGroup(
            main_execLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, main_execLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(main_execLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(main_exec_splitpane, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 733, Short.MAX_VALUE)
                    .addComponent(main_exec_options_debug, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        main_execLayout.setVerticalGroup(
            main_execLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(main_execLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(main_exec_options_debug, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(main_exec_splitpane, javax.swing.GroupLayout.DEFAULT_SIZE, 451, Short.MAX_VALUE)
                .addContainerGap())
        );

        main_tabKarelArenaTabs.addTab("Ejecucion", main_exec);

        main_coev_options_container.setBackground(new java.awt.Color(220, 220, 220));

        main_coev_options_btnNew.setIcon(new javax.swing.ImageIcon(getClass().getResource("/application/karel/images/coev/coev_new.png"))); // NOI18N
        main_coev_options_btnNew.setToolTipText("nueva condicion");
        main_coev_options_btnNew.setActionCommand("new_world");
        main_coev_options_btnNew.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                main_coev_options_btnNewActionPerformed(evt);
            }
        });

        main_coev_options_btnSave.setIcon(new javax.swing.ImageIcon(getClass().getResource("/application/karel/images/coev/coev_save.png"))); // NOI18N
        main_coev_options_btnSave.setToolTipText("guardar condicion");
        main_coev_options_btnSave.setActionCommand("save_world");
        main_coev_options_btnSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                main_coev_options_btnSaveActionPerformed(evt);
            }
        });

        main_coev_options_btnSaveAs.setIcon(new javax.swing.ImageIcon(getClass().getResource("/application/karel/images/coev/coev_saveas.png"))); // NOI18N
        main_coev_options_btnSaveAs.setToolTipText("guardar condicion como");
        main_coev_options_btnSaveAs.setActionCommand("saveas_world");
        main_coev_options_btnSaveAs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                main_coev_options_btnSaveAsActionPerformed(evt);
            }
        });

        main_coev_options_btnOpen.setIcon(new javax.swing.ImageIcon(getClass().getResource("/application/karel/images/coev/coev_open.png"))); // NOI18N
        main_coev_options_btnOpen.setToolTipText("abrir condicion");
        main_coev_options_btnOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                main_coev_options_btnOpenActionPerformed(evt);
            }
        });

        main_coev_options_btnReadWorld.setIcon(new javax.swing.ImageIcon(getClass().getResource("/application/karel/images/coev/coev_read_world.png"))); // NOI18N
        main_coev_options_btnReadWorld.setToolTipText("leer mundo actual");
        main_coev_options_btnReadWorld.setActionCommand("saveas_world");
        main_coev_options_btnReadWorld.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                main_coev_options_btnReadWorldActionPerformed(evt);
            }
        });

        main_coev_options_btnReadExec.setIcon(new javax.swing.ImageIcon(getClass().getResource("/application/karel/images/coev/coev_read_exec.png"))); // NOI18N
        main_coev_options_btnReadExec.setToolTipText("leer mundo ejecutar");
        main_coev_options_btnReadExec.setActionCommand("saveas_world");
        main_coev_options_btnReadExec.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                main_coev_options_btnReadExecActionPerformed(evt);
            }
        });

        main_coev_options_btnTrackKarel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/application/karel/images/coev/coev_track_karel.png"))); // NOI18N
        main_coev_options_btnTrackKarel.setToolTipText("buscar a karel");
        main_coev_options_btnTrackKarel.setActionCommand("saveas_world");
        main_coev_options_btnTrackKarel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                main_coev_options_btnTrackKarelActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout main_coev_options_containerLayout = new javax.swing.GroupLayout(main_coev_options_container);
        main_coev_options_container.setLayout(main_coev_options_containerLayout);
        main_coev_options_containerLayout.setHorizontalGroup(
            main_coev_options_containerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(main_coev_options_containerLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(main_coev_options_btnNew, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(main_coev_options_btnOpen, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(main_coev_options_btnSave, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(5, 5, 5)
                .addComponent(main_coev_options_btnSaveAs, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(main_coev_options_btnReadWorld, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(main_coev_options_btnReadExec, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(main_coev_options_btnTrackKarel, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(326, 326, 326))
        );
        main_coev_options_containerLayout.setVerticalGroup(
            main_coev_options_containerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(main_coev_options_containerLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(main_coev_options_containerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(main_coev_options_btnTrackKarel, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(main_coev_options_btnReadExec, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(main_coev_options_btnReadWorld, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(main_coev_options_btnOpen, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(main_coev_options_btnNew, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(main_coev_options_btnSave, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(main_coev_options_btnSaveAs, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        main_coev_container.setDividerLocation(260);
        main_coev_container.setDividerSize(7);

        main_coev_painter_container.setBackground(new java.awt.Color(220, 220, 220));

        main_coev_painter_scrY.setVisibleAmount(0);
        main_coev_painter_scrY.addAdjustmentListener(new java.awt.event.AdjustmentListener() {
            public void adjustmentValueChanged(java.awt.event.AdjustmentEvent evt) {
                main_coev_painter_scrYAdjustmentValueChanged(evt);
            }
        });

        main_coev_painter_pnlPainter.setBackground(new java.awt.Color(200, 200, 200));
        main_coev_painter_pnlPainter.addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                main_coev_painter_pnlPainterMouseWheelMoved(evt);
            }
        });
        main_coev_painter_pnlPainter.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                main_coev_painter_pnlPainterMouseClicked(evt);
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                main_coev_painter_pnlPainterMousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                main_coev_painter_pnlPainterMouseReleased(evt);
            }
        });
        main_coev_painter_pnlPainter.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                main_coev_painter_pnlPainterComponentResized(evt);
            }
        });
        main_coev_painter_pnlPainter.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                main_coev_painter_pnlPainterMouseDragged(evt);
            }
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                main_coev_painter_pnlPainterMouseMoved(evt);
            }
        });
        main_coev_painter_pnlPainter.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                main_coev_painter_pnlPainterKeyPressed(evt);
            }
            public void keyReleased(java.awt.event.KeyEvent evt) {
                main_coev_painter_pnlPainterKeyReleased(evt);
            }
        });

        javax.swing.GroupLayout main_coev_painter_pnlPainterLayout = new javax.swing.GroupLayout(main_coev_painter_pnlPainter);
        main_coev_painter_pnlPainter.setLayout(main_coev_painter_pnlPainterLayout);
        main_coev_painter_pnlPainterLayout.setHorizontalGroup(
            main_coev_painter_pnlPainterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 442, Short.MAX_VALUE)
        );
        main_coev_painter_pnlPainterLayout.setVerticalGroup(
            main_coev_painter_pnlPainterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 426, Short.MAX_VALUE)
        );

        main_coev_painter_scrX.setOrientation(javax.swing.JScrollBar.HORIZONTAL);
        main_coev_painter_scrX.setVisibleAmount(0);
        main_coev_painter_scrX.addAdjustmentListener(new java.awt.event.AdjustmentListener() {
            public void adjustmentValueChanged(java.awt.event.AdjustmentEvent evt) {
                main_coev_painter_scrXAdjustmentValueChanged(evt);
            }
        });

        javax.swing.GroupLayout main_coev_painter_containerLayout = new javax.swing.GroupLayout(main_coev_painter_container);
        main_coev_painter_container.setLayout(main_coev_painter_containerLayout);
        main_coev_painter_containerLayout.setHorizontalGroup(
            main_coev_painter_containerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(main_coev_painter_containerLayout.createSequentialGroup()
                .addComponent(main_coev_painter_scrY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(main_coev_painter_pnlPainter, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addComponent(main_coev_painter_scrX, javax.swing.GroupLayout.DEFAULT_SIZE, 465, Short.MAX_VALUE)
        );
        main_coev_painter_containerLayout.setVerticalGroup(
            main_coev_painter_containerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(main_coev_painter_containerLayout.createSequentialGroup()
                .addGroup(main_coev_painter_containerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(main_coev_painter_pnlPainter, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(main_coev_painter_scrY, javax.swing.GroupLayout.DEFAULT_SIZE, 426, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(main_coev_painter_scrX, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        main_coev_container.setRightComponent(main_coev_painter_container);

        main_coev_settings_container.setLayout(null);

        main_coev_settings_chkTurn.setFont(new java.awt.Font("Tahoma", 1, 10));
        main_coev_settings_chkTurn.setText("gira-izquierda/turnleft()");
        main_coev_settings_container.add(main_coev_settings_chkTurn);
        main_coev_settings_chkTurn.setBounds(10, 60, 180, 20);

        main_coev_settings_txtPick.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        main_coev_settings_txtPick.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                main_coev_settings_txtInsKeyReleased(evt);
            }
        });
        main_coev_settings_container.add(main_coev_settings_txtPick);
        main_coev_settings_txtPick.setBounds(200, 80, 45, 18);

        main_coev_settings_chkIns.setFont(new java.awt.Font("Tahoma", 1, 10));
        main_coev_settings_chkIns.setText("instrucciones:");
        main_coev_settings_container.add(main_coev_settings_chkIns);
        main_coev_settings_chkIns.setBounds(0, 20, 190, 21);

        main_coev_settings_txtMove.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        main_coev_settings_txtMove.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                main_coev_settings_txtInsKeyReleased(evt);
            }
        });
        main_coev_settings_container.add(main_coev_settings_txtMove);
        main_coev_settings_txtMove.setBounds(200, 40, 45, 18);

        main_coev_settings_txtIns.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        main_coev_settings_txtIns.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                main_coev_settings_txtInsKeyReleased(evt);
            }
        });
        main_coev_settings_container.add(main_coev_settings_txtIns);
        main_coev_settings_txtIns.setBounds(200, 20, 45, 18);

        main_coev_settings_txtTurn.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        main_coev_settings_txtTurn.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                main_coev_settings_txtInsKeyReleased(evt);
            }
        });
        main_coev_settings_container.add(main_coev_settings_txtTurn);
        main_coev_settings_txtTurn.setBounds(200, 60, 45, 18);

        main_coev_settings_chkMove.setFont(new java.awt.Font("Tahoma", 1, 10));
        main_coev_settings_chkMove.setText("avanza/move():");
        main_coev_settings_container.add(main_coev_settings_chkMove);
        main_coev_settings_chkMove.setBounds(10, 40, 180, 20);

        main_coev_settings_chkPick.setFont(new java.awt.Font("Tahoma", 1, 10));
        main_coev_settings_chkPick.setText("coge-zumbador/pickbeeper():");
        main_coev_settings_container.add(main_coev_settings_chkPick);
        main_coev_settings_chkPick.setBounds(10, 100, 180, 20);

        main_coev_settings_chkPut.setFont(new java.awt.Font("Tahoma", 1, 10));
        main_coev_settings_chkPut.setText("deja-zumbador/putbeeper():");
        main_coev_settings_container.add(main_coev_settings_chkPut);
        main_coev_settings_chkPut.setBounds(10, 80, 180, 20);

        main_coev_settings_txtPut.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        main_coev_settings_txtPut.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                main_coev_settings_txtInsKeyReleased(evt);
            }
        });
        main_coev_settings_container.add(main_coev_settings_txtPut);
        main_coev_settings_txtPut.setBounds(200, 100, 45, 18);

        main_coev_settings_jLabel1.setFont(new java.awt.Font("Tahoma", 1, 11));
        main_coev_settings_jLabel1.setText("Restricciones durante la ejecucion:");
        main_coev_settings_container.add(main_coev_settings_jLabel1);
        main_coev_settings_jLabel1.setBounds(0, 0, 197, 14);

        main_coev_settings_jLabel2.setFont(new java.awt.Font("Tahoma", 1, 11));
        main_coev_settings_jLabel2.setText("Condiciones al terminar la ejecucion:");
        main_coev_settings_container.add(main_coev_settings_jLabel2);
        main_coev_settings_jLabel2.setBounds(0, 190, 206, 14);

        main_coev_settings_chkPKarel.setFont(new java.awt.Font("Tahoma", 1, 10));
        main_coev_settings_chkPKarel.setText("Posicion Karel");
        main_coev_settings_container.add(main_coev_settings_chkPKarel);
        main_coev_settings_chkPKarel.setBounds(0, 210, 110, 21);

        main_coev_settings_chkOKarel.setFont(new java.awt.Font("Tahoma", 1, 10));
        main_coev_settings_chkOKarel.setText("Orientacion Karel");
        main_coev_settings_container.add(main_coev_settings_chkOKarel);
        main_coev_settings_chkOKarel.setBounds(0, 235, 130, 21);

        main_coev_settings_jLabel3.setFont(new java.awt.Font("Tahoma", 1, 10));
        main_coev_settings_jLabel3.setText("A:");
        main_coev_settings_container.add(main_coev_settings_jLabel3);
        main_coev_settings_jLabel3.setBounds(125, 215, 11, 13);

        main_coev_settings_txtPKA.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        main_coev_settings_txtPKA.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                main_coev_settings_txtInsKeyReleased(evt);
            }
        });
        main_coev_settings_container.add(main_coev_settings_txtPKA);
        main_coev_settings_txtPKA.setBounds(140, 210, 45, 20);

        main_coev_settings_jLabel4.setFont(new java.awt.Font("Tahoma", 1, 10));
        main_coev_settings_jLabel4.setText("C:");
        main_coev_settings_container.add(main_coev_settings_jLabel4);
        main_coev_settings_jLabel4.setBounds(187, 215, 10, 13);

        main_coev_settings_txtPKC.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        main_coev_settings_container.add(main_coev_settings_txtPKC);
        main_coev_settings_txtPKC.setBounds(200, 210, 45, 20);

        main_coev_settings_cmbFace.setFont(new java.awt.Font("Tahoma", 0, 10));
        main_coev_settings_cmbFace.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "NORTE", "ESTE", "SUR", "OESTE" }));
        main_coev_settings_cmbFace.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                main_coev_settings_cmbFaceActionPerformed(evt);
            }
        });
        main_coev_settings_container.add(main_coev_settings_cmbFace);
        main_coev_settings_cmbFace.setBounds(140, 235, 105, 19);

        main_coev_settings_chkBeepers.setFont(new java.awt.Font("Tahoma", 1, 10));
        main_coev_settings_chkBeepers.setText("Zumbadores / beepers en una posicion:");
        main_coev_settings_container.add(main_coev_settings_chkBeepers);
        main_coev_settings_chkBeepers.setBounds(0, 260, 240, 21);

        main_coev_settings_lstBeepers.setFont(new java.awt.Font("Courier New", 0, 11));
        main_coev_settings_lstBeepers.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                main_coev_settings_lstBeepersValueChanged(evt);
            }
        });
        main_coev_settings_lstBeepers.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                main_coev_settings_lstBeepersKeyPressed(evt);
            }
        });
        main_coev_settings_jScrollPane1.setViewportView(main_coev_settings_lstBeepers);

        main_coev_settings_container.add(main_coev_settings_jScrollPane1);
        main_coev_settings_jScrollPane1.setBounds(10, 290, 150, 150);

        main_coev_settings_jLabel5.setFont(new java.awt.Font("Tahoma", 1, 10));
        main_coev_settings_jLabel5.setText("Agregar:");
        main_coev_settings_container.add(main_coev_settings_jLabel5);
        main_coev_settings_jLabel5.setBounds(170, 290, 50, 13);

        main_coev_settings_jLabel6.setFont(new java.awt.Font("Tahoma", 1, 10));
        main_coev_settings_jLabel6.setText("A:");
        main_coev_settings_container.add(main_coev_settings_jLabel6);
        main_coev_settings_jLabel6.setBounds(180, 310, 20, 13);

        main_coev_settings_txtAddPKA.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        main_coev_settings_txtAddPKA.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                main_coev_settings_txtInsKeyReleased(evt);
            }
        });
        main_coev_settings_container.add(main_coev_settings_txtAddPKA);
        main_coev_settings_txtAddPKA.setBounds(200, 310, 45, 20);

        main_coev_settings_txtAddPKC.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        main_coev_settings_txtAddPKC.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                main_coev_settings_txtInsKeyReleased(evt);
            }
        });
        main_coev_settings_container.add(main_coev_settings_txtAddPKC);
        main_coev_settings_txtAddPKC.setBounds(200, 335, 45, 20);

        main_coev_settings_jLabel7.setFont(new java.awt.Font("Tahoma", 1, 10));
        main_coev_settings_jLabel7.setText("C:");
        main_coev_settings_container.add(main_coev_settings_jLabel7);
        main_coev_settings_jLabel7.setBounds(180, 340, 20, 13);

        main_coev_settings_txtAddPKB.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        main_coev_settings_txtAddPKB.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                main_coev_settings_txtInsKeyReleased(evt);
            }
        });
        main_coev_settings_container.add(main_coev_settings_txtAddPKB);
        main_coev_settings_txtAddPKB.setBounds(200, 360, 45, 20);

        main_coev_settings_jLabel8.setFont(new java.awt.Font("Tahoma", 1, 10));
        main_coev_settings_jLabel8.setText("Z/B:");
        main_coev_settings_container.add(main_coev_settings_jLabel8);
        main_coev_settings_jLabel8.setBounds(170, 365, 30, 13);

        main_coev_settings_btnAddPK.setFont(new java.awt.Font("Tahoma", 1, 10));
        main_coev_settings_btnAddPK.setText("agregar");
        main_coev_settings_btnAddPK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                main_coev_settings_btnAddPKActionPerformed(evt);
            }
        });
        main_coev_settings_container.add(main_coev_settings_btnAddPK);
        main_coev_settings_btnAddPK.setBounds(170, 390, 75, 23);

        main_coev_settings_chkBeeperBag.setFont(new java.awt.Font("Tahoma", 1, 10));
        main_coev_settings_chkBeeperBag.setText("zumba/beep en la mochila:");
        main_coev_settings_container.add(main_coev_settings_chkBeeperBag);
        main_coev_settings_chkBeeperBag.setBounds(0, 120, 190, 21);

        main_coev_settings_txtBeeperBag.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        main_coev_settings_txtBeeperBag.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                main_coev_settings_txtInsKeyReleased(evt);
            }
        });
        main_coev_settings_container.add(main_coev_settings_txtBeeperBag);
        main_coev_settings_txtBeeperBag.setBounds(200, 120, 45, 18);

        main_coev_settings_chkBeepersPos.setFont(new java.awt.Font("Tahoma", 1, 10));
        main_coev_settings_chkBeepersPos.setText("zumba/beep en una posicion:");
        main_coev_settings_container.add(main_coev_settings_chkBeepersPos);
        main_coev_settings_chkBeepersPos.setBounds(0, 140, 190, 21);

        main_coev_settings_txtBeepersPos.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        main_coev_settings_txtBeepersPos.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                main_coev_settings_txtInsKeyReleased(evt);
            }
        });
        main_coev_settings_container.add(main_coev_settings_txtBeepersPos);
        main_coev_settings_txtBeepersPos.setBounds(200, 140, 45, 18);

        main_coev_settings_btnClear.setFont(new java.awt.Font("Tahoma", 1, 10));
        main_coev_settings_btnClear.setText("limpiar");
        main_coev_settings_btnClear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                main_coev_settings_btnClearActionPerformed(evt);
            }
        });
        main_coev_settings_container.add(main_coev_settings_btnClear);
        main_coev_settings_btnClear.setBounds(170, 415, 75, 23);

        main_coev_container.setLeftComponent(main_coev_settings_container);

        javax.swing.GroupLayout main_coevLayout = new javax.swing.GroupLayout(main_coev);
        main_coev.setLayout(main_coevLayout);
        main_coevLayout.setHorizontalGroup(
            main_coevLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, main_coevLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(main_coevLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(main_coev_options_container, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 733, Short.MAX_VALUE)
                    .addComponent(main_coev_container, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 733, Short.MAX_VALUE))
                .addContainerGap())
        );
        main_coevLayout.setVerticalGroup(
            main_coevLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(main_coevLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(main_coev_options_container, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(main_coev_container, javax.swing.GroupLayout.DEFAULT_SIZE, 451, Short.MAX_VALUE)
                .addContainerGap())
        );

        main_tabKarelArenaTabs.addTab("Condiciones de evaluacion", main_coev);

        main_help_options_container.setBackground(new java.awt.Color(220, 220, 220));

        main_help_options_btnSwitch.setIcon(new javax.swing.ImageIcon(getClass().getResource("/application/karel/images/help/help_change_view.png"))); // NOI18N
        main_help_options_btnSwitch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                main_help_options_btnSwitchActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout main_help_options_containerLayout = new javax.swing.GroupLayout(main_help_options_container);
        main_help_options_container.setLayout(main_help_options_containerLayout);
        main_help_options_containerLayout.setHorizontalGroup(
            main_help_options_containerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(main_help_options_containerLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(main_help_options_btnSwitch, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(673, Short.MAX_VALUE))
        );
        main_help_options_containerLayout.setVerticalGroup(
            main_help_options_containerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, main_help_options_containerLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(main_help_options_btnSwitch, javax.swing.GroupLayout.DEFAULT_SIZE, 50, Short.MAX_VALUE)
                .addContainerGap())
        );

        main_help_options_container1.setBackground(new java.awt.Color(220, 220, 220));

        javax.swing.GroupLayout main_help_options_container1Layout = new javax.swing.GroupLayout(main_help_options_container1);
        main_help_options_container1.setLayout(main_help_options_container1Layout);
        main_help_options_container1Layout.setHorizontalGroup(
            main_help_options_container1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 733, Short.MAX_VALUE)
        );
        main_help_options_container1Layout.setVerticalGroup(
            main_help_options_container1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 451, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout main_evalLayout = new javax.swing.GroupLayout(main_eval);
        main_eval.setLayout(main_evalLayout);
        main_evalLayout.setHorizontalGroup(
            main_evalLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, main_evalLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(main_evalLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(main_help_options_container1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(main_help_options_container, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        main_evalLayout.setVerticalGroup(
            main_evalLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(main_evalLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(main_help_options_container, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(main_help_options_container1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        main_tabKarelArenaTabs.addTab("Ayuda", main_eval);

        main_status_container.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        javax.swing.GroupLayout main_status_containerLayout = new javax.swing.GroupLayout(main_status_container);
        main_status_container.setLayout(main_status_containerLayout);
        main_status_containerLayout.setHorizontalGroup(
            main_status_containerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(main_status_lblStatus, javax.swing.GroupLayout.DEFAULT_SIZE, 754, Short.MAX_VALUE)
        );
        main_status_containerLayout.setVerticalGroup(
            main_status_containerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(main_status_lblStatus, javax.swing.GroupLayout.DEFAULT_SIZE, 11, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(main_status_container, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(main_tabKarelArenaTabs)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(main_tabKarelArenaTabs, javax.swing.GroupLayout.DEFAULT_SIZE, 579, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(main_status_container, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // -----------------------------------------------------------------
    // <editor-fold defaultstate="collapsed" desc="Global Event methods">	
    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        String pattern = "";
        if( this.world_change )
            pattern = "EL MUNDO";
        if( this.code_change )
            pattern = pattern + (pattern.length() > 0 ? ", " : "") + "EL CODIGO";
        if( this.coev_change )
            pattern = pattern + (pattern.length() > 0 ? " y " : "") + "LA CONFIGURACION DE EVALUACION";

        if( pattern.length() > 0 && JOptionPane.showConfirmDialog(this, "Se han hecho cambios en : \n" + pattern + "\nDeseas descartarlos?", "Descartar", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION )
        {
            this.setDefaultCloseOperation( JFrame.DO_NOTHING_ON_CLOSE);
            return;
        }
        this.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
    }//GEN-LAST:event_formWindowClosing
    	
    private void main_tabKarelArenaTabsStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_main_tabKarelArenaTabsStateChanged
        String color_status = "blue";
        int index = this.main_tabKarelArenaTabs.getSelectedIndex();
        switch( index )
        {
            case 0: // world tab
                this.main_world_painter_pnlPainter.requestFocus();
                color_status = this.world_change ? "red" : "blue";
                break;
            case 1: // code tab
                this.main_code_editor_txtCode.requestFocus();
                color_status = this.code_change ? "red" : "blue";
                break;
            case 2: // exec tab
                this.main_exec_options_btnRestart.requestFocus();
                if( this.ka_exec.is_running() )
                    return;
                this.execWorldInit();
                if( this.main_exec_display_txtCode.getText().compareTo( this.main_code_editor_txtCode.getText() ) != 0 )
                    JOptionPane.showMessageDialog( this, "Cuidado, no has compilado el programa" );
                this.main_exec_options_btnRun.requestFocus();
                break;
            case 3: // coev tab
                break;
        }
        this.display_status(color_status);
    }//GEN-LAST:event_main_tabKarelArenaTabsStateChanged

    private void main_pop_world_handler(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_main_pop_world_handler
        if( this.pop_world_click_event == null )
            return;
        String data = evt.getActionCommand().trim();
        this.do_selected_pop_world_menu_option(data);
    }//GEN-LAST:event_main_pop_world_handler

    private void main_pop_worldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_main_pop_worldKeyReleased
        if( this.pop_world_click_event == null )
            return;
        char data = Character.toUpperCase( evt.getKeyChar() );
        if( ('0' <= data && data <= '9') || data == 'N' || data == 'I' || data == 'K' )
            this.do_selected_pop_world_menu_option( String.valueOf(data) );
    }//GEN-LAST:event_main_pop_worldKeyReleased
                       
	
    // </editor-fold>

    // -----------------------------------------------------------------
    // <editor-fold defaultstate="collapsed" desc="World Event methods">
    private void main_world_options_btnNewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_main_world_options_btnNewActionPerformed
        if( this.world_change && !this.confirm_discard_changes("EL MUNDO") )
            return;
        this.reset_world();
    }//GEN-LAST:event_main_world_options_btnNewActionPerformed

    private void main_world_options_btnOpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_main_world_options_btnOpenActionPerformed
        if( this.world_change && !this.confirm_discard_changes("EL MUNDO") )
            return;

        final SimpleFileFilter filter = new SimpleFileFilter( "Mundo de Karel", new String [] { "mdo" } );
        this.fc.setDialogTitle( "Abrir mundo" );
        this.fc.setFileFilter( filter );
        if( this.fc.showOpenDialog( this ) == JFileChooser.APPROVE_OPTION )
        {
            try {
                KarelWorldManager.read_world(this.fc.getSelectedFile().getPath(), this.env);
                this.reset_world_scroll_values();
                this.main_world_options_txtBeepersBag.setText("" + this.env.getKarel().getNumBeepers());
                if( this.env.getKarel().getNumBeepers() == this.env.getInfiniteBeepersValue() )
                    this.main_world_options_txtBeepersBag.setText("INFINITO");
                this.update_status(0, this.fc.getSelectedFile().getPath(), "blue");
                this.set_world_change_values(false);
            } catch( FileNotFoundException fne ) {
                JOptionPane.showMessageDialog( this, "No se encuentra el archivo: " + this.fc.getSelectedFile().getName(), "Error", JOptionPane.ERROR_MESSAGE);
            } catch( KarelException kae ){
                 JOptionPane.showMessageDialog( this, "El formato del mundo: " + this.fc.getSelectedFile().getName() + " es incorrecto", "Error", JOptionPane.ERROR_MESSAGE);
            } catch( Exception exc ) {
                JOptionPane.showMessageDialog( this, "Error al intentar leer archivo: " + this.fc.getSelectedFile().getName(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_main_world_options_btnOpenActionPerformed
    
    private void main_world_options_btnSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_main_world_options_btnSaveActionPerformed
        if( this.current_world_file.isEmpty() )
        {
            this.main_world_options_btnSaveAsActionPerformed(null);
            return;
        }
        if( this.world_change && this.save_world(this.current_world_file) )
            this.set_world_change_values(false);
    }//GEN-LAST:event_main_world_options_btnSaveActionPerformed
    
    private void main_world_options_btnSaveAsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_main_world_options_btnSaveAsActionPerformed
        final SimpleFileFilter filter = new SimpleFileFilter( "Mundo de Karel", new String [] { "mdo" } );
        this.fc.setDialogTitle( "Guardar mundo" );
        this.fc.setFileFilter( filter );
        if( this.fc.showSaveDialog( this ) == JFileChooser.APPROVE_OPTION )
        {
            String filename = this.fc.getSelectedFile().toString();
            filename += !filename.toLowerCase().endsWith( ".mdo" ) ? ".mdo" : "";
            File file = new File( filename );
            if( file.exists() && !this.confirm_overwrite(file.getName()) )
                return;
            this.save_world( filename );
            this.update_status(0, filename, "blue");
            this.set_world_change_values(false);
        }
    }//GEN-LAST:event_main_world_options_btnSaveAsActionPerformed
    
    private void main_world_options_btnInfBagActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_main_world_options_btnInfBagActionPerformed
        this.main_world_options_txtBeepersBag.setText("INFINITO");
        this.main_world_options_txtBeepersBagKeyReleased(null);
    }//GEN-LAST:event_main_world_options_btnInfBagActionPerformed
            
    private void main_world_options_txtBeepersBagKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_main_world_options_txtBeepersBagKeyReleased
        this.check_beeper_bag_content(evt);
    }//GEN-LAST:event_main_world_options_txtBeepersBagKeyReleased
        
    private void main_world_painter_pnlPainterKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_main_world_painter_pnlPainterKeyPressed
        this.world_press_control = (evt.getKeyCode() == KeyEvent.VK_CONTROL);
        if( this.main_pop_world.isVisible() )
            this.main_pop_worldKeyReleased(evt);
        else if( '0' <= evt.getKeyChar() && evt.getKeyChar() <= '9' &&
                 ((KarelPanel)this.main_world_painter_pnlPainter).getState() == KarelPanel.BEEPER )
        {
            ((KarelPanel)this.main_world_painter_pnlPainter).setBeeperQtty( evt.getKeyChar() - '0');
            ((KarelPanel)this.main_world_painter_pnlPainter).paint();
        }
    }//GEN-LAST:event_main_world_painter_pnlPainterKeyPressed
    
    private void main_world_painter_pnlPainterKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_main_world_painter_pnlPainterKeyReleased
        this.world_press_control = false;
    }//GEN-LAST:event_main_world_painter_pnlPainterKeyReleased
    
    private void main_world_painter_pnlPainterMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_main_world_painter_pnlPainterMouseWheelMoved
        if( !this.world_press_control )
            this.main_world_painter_scrY.setValue( this.main_world_painter_scrY.getValue() + evt.getWheelRotation() );
        else
            this.main_world_painter_scrX.setValue( this.main_world_painter_scrX.getValue() - evt.getWheelRotation() );
    }//GEN-LAST:event_main_world_painter_pnlPainterMouseWheelMoved

	private void main_world_painter_pnlPainterMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_main_world_painter_pnlPainterMousePressed
        if( evt.getButton() == MouseEvent.BUTTON1 )
            this.set_world_change_values(true);
    }//GEN-LAST:event_main_world_painter_pnlPainterMousePressed
    
    private void main_world_painter_pnlPainterMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_main_world_painter_pnlPainterMouseClicked
        if( evt.getButton() == MouseEvent.BUTTON3 && evt.getClickCount() == 1 )
        {
            this.main_pop_world.show(this.main_world_painter_pnlPainter, evt.getX(), evt.getY());
            this.pop_world_click_event = evt;
            return;
        }
        if( evt.getButton() == MouseEvent.BUTTON1 )
            this.set_world_change_values(true);
    }//GEN-LAST:event_main_world_painter_pnlPainterMouseClicked
    
    private void main_world_painter_scrXAdjustmentValueChanged(java.awt.event.AdjustmentEvent evt) {//GEN-FIRST:event_main_world_painter_scrXAdjustmentValueChanged
        if( this.world_resize )
            return;

        KarelPanel p = (KarelPanel)(this.main_world_painter_pnlPainter);
        p.moveWorld( this.main_world_painter_scrX.getValue(),
                     this.main_world_painter_scrY.getMaximum() - this.main_world_painter_scrY.getValue());
    }//GEN-LAST:event_main_world_painter_scrXAdjustmentValueChanged
 
    private void main_world_painter_scrYAdjustmentValueChanged(java.awt.event.AdjustmentEvent evt) {//GEN-FIRST:event_main_world_painter_scrYAdjustmentValueChanged
        this.main_world_painter_scrXAdjustmentValueChanged(evt);
    }//GEN-LAST:event_main_world_painter_scrYAdjustmentValueChanged
    
    private void main_world_painter_pnlPainterComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_main_world_painter_pnlPainterComponentResized
        Rectangle world = ( (KarelPanel)(this.main_world_painter_pnlPainter) ).getWorld();

        this.world_resize = true;
        this.main_world_painter_scrY.setMaximum( this.env.numCalles() - world.height);
        this.main_world_painter_scrX.setMaximum( this.env.numAvenidas() - world.width );
        if( this.loading_app )
            this.main_world_painter_scrY.setValue( this.env.numCalles() - world.height);
        this.world_resize = false;
        this.main_world_painter_scrYAdjustmentValueChanged(null);
    }//GEN-LAST:event_main_world_painter_pnlPainterComponentResized
    
    // </editor-fold>

    // -----------------------------------------------------------------
    // <editor-fold defaultstate="collapsed" desc="Code Event methods">
    private void main_code_options_btnNewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_main_code_options_btnNewActionPerformed
        if( this.code_change && !this.confirm_discard_changes("EL CODIGO") )
            return;
        this.reset_code();
    }//GEN-LAST:event_main_code_options_btnNewActionPerformed

    private void main_code_options_btnOpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_main_code_options_btnOpenActionPerformed
        if( this.code_change && !this.confirm_discard_changes("EL CODIGO") )
            return;
        
        final SimpleFileFilter filter = new SimpleFileFilter( "Codigo de Karel", new String [] { "txt" } );
        this.fc.setDialogTitle( "Abrir codigo" );
        this.fc.setFileFilter( filter );
        if( this.fc.showSaveDialog( this ) == JFileChooser.APPROVE_OPTION )
        {
            try
            {
                BufferedReader r = new BufferedReader( new FileReader( this.fc.getSelectedFile() ) );
                String str, code = "";
                while( (str = r.readLine()) != null )
                    code += str + "\n";
                r.close();
                this.main_code_editor_txtCode.setText( code );
                this.update_status(1, this.fc.getSelectedFile().getPath(), "blue");
                this.set_code_change_values(false);
            } catch( FileNotFoundException ex ) { 
                JOptionPane.showMessageDialog( this, "No se encuentra el archivo: " + this.fc.getSelectedFile().getName(), "Error", JOptionPane.ERROR_MESSAGE);
            } catch( IOException ex ) {
                JOptionPane.showMessageDialog( this, "Error al intentar leer el archivo: " + this.fc.getSelectedFile().getName(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_main_code_options_btnOpenActionPerformed
    
    private void main_code_options_btnSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_main_code_options_btnSaveActionPerformed
        if( this.current_code_file.isEmpty() )
        {
            this.main_code_options_btnSaveAsActionPerformed(null);
            return;
        }
        if( this.code_change && this.save_code(this.current_code_file) )
            this.set_code_change_values(false);
    }//GEN-LAST:event_main_code_options_btnSaveActionPerformed

    private void main_code_options_btnSaveAsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_main_code_options_btnSaveAsActionPerformed
        final SimpleFileFilter filter = new SimpleFileFilter( "Codigo de Karel", new String [] { "txt" } );
        this.fc.setDialogTitle( "Guardar mundo" );
        this.fc.setFileFilter( filter );
        if( this.fc.showSaveDialog( this ) == JFileChooser.APPROVE_OPTION )
        {
            String filename = this.fc.getSelectedFile().toString();
            filename += !filename.toLowerCase().endsWith( ".txt" ) ? ".txt" : "";
            File file = new File( filename );
            if( file.exists() && !this.confirm_overwrite(file.getName()) )
                return;
            this.save_code( filename );
            this.update_status(1, filename, "blue");
            this.set_code_change_values(false);
        }
    }//GEN-LAST:event_main_code_options_btnSaveAsActionPerformed

    private void main_code_editor_txtCodeKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_main_code_editor_txtCodeKeyTyped
        if( evt.getKeyChar() == '\t' )
        {
            this.main_code_editor_txtCode.select( this.main_code_editor_txtCode.getCaretPosition() - 1,
                                                  this.main_code_editor_txtCode.getCaretPosition() );
            this.main_code_editor_txtCode.replaceSelection( "    " );
        }
        if( evt.getKeyChar() == '\n' )
        {
            String text = this.main_code_editor_txtCode.getText().replaceAll( "\\r", "" );
            int poscaret = this.main_code_editor_txtCode.getCaretPosition(), 
                prev = text.lastIndexOf( '\n', poscaret - 2 ) + 1, i = prev;
            while( i < poscaret && Character.isWhitespace( text.charAt(i) ) && text.charAt(i) != '\n' )
                i++;
            this.main_code_editor_txtCode.replaceSelection( text.substring( prev, i ) );
        }

        this.set_code_change_values(true);
    }//GEN-LAST:event_main_code_editor_txtCodeKeyTyped
    
    private void main_code_options_btnCopyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_main_code_options_btnCopyActionPerformed
        this.main_code_editor_txtCode.copy();
        this.main_code_editor_txtCode.requestFocus();
    }//GEN-LAST:event_main_code_options_btnCopyActionPerformed

    private void main_code_options_btnCutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_main_code_options_btnCutActionPerformed
        this.main_code_editor_txtCode.cut();
        this.main_code_editor_txtCode.requestFocus();
    }//GEN-LAST:event_main_code_options_btnCutActionPerformed
    
    private void main_code_options_btnPasteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_main_code_options_btnPasteActionPerformed
        this.main_code_editor_txtCode.paste();
        this.main_code_editor_txtCode.requestFocus();
    }//GEN-LAST:event_main_code_options_btnPasteActionPerformed
    
    private void main_code_options_btnUndoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_main_code_options_btnUndoActionPerformed
        this.undo_redo_action(0);
    }//GEN-LAST:event_main_code_options_btnUndoActionPerformed

    private void main_code_options_btnRedoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_main_code_options_btnRedoActionPerformed
        this.undo_redo_action(1);
    }//GEN-LAST:event_main_code_options_btnRedoActionPerformed

    private void main_code_options_btnCompileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_main_code_options_btnCompileActionPerformed
        if( this.ka_exec.is_running() )
            this.main_exec_options_btnRestartActionPerformed(null);

        String text = this.main_code_editor_txtCode.getText().replaceAll( "\\r", "" ), strOut = "Codigo compilado";
        int messageType = JOptionPane.INFORMATION_MESSAGE;
        int to, from;

        if( this.main_code_options_btnChangeLang.getActionCommand().equals("java") )
            this.ka_exec.set_compile_lang(KarelExecutor.CompileLang.LANG_JAVA);
        else
            this.ka_exec.set_compile_lang(KarelExecutor.CompileLang.LANG_PASCAL);

        KarelConsole kc = new KarelConsole(this.ka_exec);
        if( kc.compile(text) )
            this.main_exec_display_txtCode.setText( text );
        else
        {
            messageType = JOptionPane.ERROR_MESSAGE;
            KarelException kaexc = kc.get_karel_exception();
            ParseError     kapar = kc.get_karel_parseerror();
            if( kaexc != null )
            {
                strOut = "Posibles errores. Se esperaba:\n" + kaexc.getMessage();
                if( kaexc.hasSource() )
                    this.main_code_editor_txtCode.select( kaexc.from(), kaexc.to() );
            }
            else if( kapar != null )
            {
                String strError = "", errortok[] = kapar.toString().split("\\s+");
                for( int i = 0, n = errortok.length; i < n; i++ )
                    //if( errortok[i].equals("expected") )
                        strError += kc.parse_error(errortok[i]) + "\n";
                strOut = "Posibles errores. Se esperaba:\n" + strError + "\n";
                to   = this.main_code_editor_txtCode.getText().indexOf( '\n', kapar.getOrigPosition() );
                from = this.main_code_editor_txtCode.getText().substring( 0, kapar.getOrigPosition() ).lastIndexOf( '\n' );
                this.main_code_editor_txtCode.select( from, to );
            }
            else
                strOut = "Codigo vacio";
            this.main_code_editor_txtCode.requestFocus();
        }
        JOptionPane.showMessageDialog(this, strOut, "Compilacion", messageType);
    }//GEN-LAST:event_main_code_options_btnCompileActionPerformed

    private void main_code_options_btnChangeLangActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_main_code_options_btnChangeLangActionPerformed
        Icon enable  = this.main_code_options_btnChangeLang.getIcon();
        Icon disable = this.main_code_options_btnChangeLang.getDisabledIcon();
        this.main_code_options_btnChangeLang.setIcon( disable );
        this.main_code_options_btnChangeLang.setDisabledIcon( enable );

        if( this.main_code_options_btnChangeLang.getActionCommand().equals("java") )
        {
            KarelArena.parser = this.parser_pascal;
            KarelArena.parser.setUtility( KarelUtility.utility );
            this.code_document.setHighlightStyle( KarelLexerPascal.class );
            this.main_code_options_btnChangeLang.setActionCommand("pascal");
        }
        else
        {
            KarelArena.parser = this.parser_java;
            KarelArena.parser.setUtility( KarelUtility.utility );
            this.code_document.setHighlightStyle( KarelLexerJava.class );
            this.main_code_options_btnChangeLang.setActionCommand("java");
        }
        
    }//GEN-LAST:event_main_code_options_btnChangeLangActionPerformed
    // </editor-fold>

    // -----------------------------------------------------------------
    // <editor-fold defaultstate="collapsed" desc="Exec Event methods">
    private void main_exec_options_btnRunActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_main_exec_options_btnRunActionPerformed
        this.exec_stepping = false;
        if( this.ka_exec.is_running() && this.ka_exec.is_paused() )
        {
            this.ka_exec.setSleep( Integer.valueOf(this.main_exec_options_txtSleep.getText()) );
            this.exec_stepping = false;
            this.ka_exec.setPaused( false );
            this.main_exec_options_btnRun.setEnabled( false );
            return;
        }
        this.main_exec_options_btnRestartActionPerformed(null);
        this.main_exec_options_btnRun.setEnabled( false );
        this.ka_exec.setPaused( false );
        this.ka_exec.setDebugging( false );
        this.ka_exec.setSleep( Integer.valueOf(this.main_exec_options_txtSleep.getText()) );
        this.ka_exec.run( this.count_id++ );
    }//GEN-LAST:event_main_exec_options_btnRunActionPerformed

    private void main_exec_options_btnNextStepActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_main_exec_options_btnNextStepActionPerformed
        if( !this.ka_exec.is_running() )
            this.main_exec_options_btnRunActionPerformed(null);
        this.ka_exec.setDebugging( true );
        this.main_exec_options_btnRun.setEnabled( true );
        this.ka_exec.setPaused( false );
        this.ka_exec.setSleep(0);
        this.exec_stepping = true;
    }//GEN-LAST:event_main_exec_options_btnNextStepActionPerformed
        
    private void main_exec_options_btnNextStepKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_main_exec_options_btnNextStepKeyTyped
        if( evt.getKeyChar() == ' ' || evt.getKeyChar() == '\n' )
            this.main_exec_options_btnNextStepActionPerformed(null);
    }//GEN-LAST:event_main_exec_options_btnNextStepKeyTyped

    private void main_exec_options_btnRestartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_main_exec_options_btnRestartActionPerformed
        this.ka_exec.stop();
        this.execWorldInit();
    }//GEN-LAST:event_main_exec_options_btnRestartActionPerformed
	
    private void main_exec_options_txtSleepKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_main_exec_options_txtSleepKeyReleased
        this.check_sleep_content(evt);
    }//GEN-LAST:event_main_exec_options_txtSleepKeyReleased
	
    // </editor-fold>
    
    // -----------------------------------------------------------------
    // <editor-fold defaultstate="collapsed" desc="Coev Event methods">
    private void main_coev_options_btnNewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_main_coev_options_btnNewActionPerformed
        if( this.coev_change &&
            !this.confirm_discard_changes("LA CONFIGURACION DE EVALUACION") )
            return;
        this.reset_coev_settings();
    }//GEN-LAST:event_main_coev_options_btnNewActionPerformed

    private void main_coev_options_btnOpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_main_coev_options_btnOpenActionPerformed
        if( this.coev_change && !this.confirm_discard_changes("LA CONFIGURACION DE EVALUACION") )
            return;

        final SimpleFileFilter filter = new SimpleFileFilter( "Condiciones de Karel", new String [] { "kec" } );
        this.fc.setDialogTitle( "Abrir condiciones" );
        this.fc.setFileFilter( filter );
        if( this.fc.showOpenDialog( this ) == JFileChooser.APPROVE_OPTION )
        {
            try {
                KarelKECManager.read_kec(this.fc.getSelectedFile().getPath(), this.coev_kec_file);
                this.update_status(3, this.fc.getSelectedFile().getPath(), "blue");
                this.load_settings_from_kec(this.coev_kec_file);
                this.set_coev_change_values(false);
            } catch( FileNotFoundException fne ) {
                JOptionPane.showMessageDialog( this, "No se encuentra el archivo: " + this.fc.getSelectedFile().getName(), "Error", JOptionPane.ERROR_MESSAGE);
            } catch( KarelException kae ){
                 JOptionPane.showMessageDialog( this, "El formato del archivo de condiciones: " + this.fc.getSelectedFile().getName() + " es incorrecto", "Error", JOptionPane.ERROR_MESSAGE);
            } catch( Exception exc ) {
                JOptionPane.showMessageDialog( this, "Error al intentar leer archivo: " + this.fc.getSelectedFile().getName(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_main_coev_options_btnOpenActionPerformed

    private void main_coev_options_btnSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_main_coev_options_btnSaveActionPerformed
        if( this.current_coev_file.isEmpty() )
        {
            this.main_coev_options_btnSaveAsActionPerformed(null);
            return;
        }
        if( this.coev_change && !this.save_coev_kec(this.current_coev_file, this.coev_kec_file) )
            this.set_code_change_values(false);
    }//GEN-LAST:event_main_coev_options_btnSaveActionPerformed

    private void main_coev_options_btnSaveAsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_main_coev_options_btnSaveAsActionPerformed
        final SimpleFileFilter filter = new SimpleFileFilter( "Condiciones de Karel", new String [] { "kec" } );
        this.fc.setDialogTitle( "Guardar mundo" );
        this.fc.setFileFilter( filter );
        if( this.fc.showSaveDialog( this ) == JFileChooser.APPROVE_OPTION )
        {
            String filename = this.fc.getSelectedFile().toString();
            filename += !filename.toLowerCase().endsWith( ".kec" ) ? ".kec" : "";
            File file = new File( filename );
            if( file.exists() && !this.confirm_overwrite(file.getName()) )
                return;
            if( this.save_coev_kec( filename, this.coev_kec_file) )
            {
                this.update_status(3, filename, "blue");
                this.set_coev_change_values(false);
            }
        }
    }//GEN-LAST:event_main_coev_options_btnSaveAsActionPerformed

    private void main_coev_options_btnReadWorldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_main_coev_options_btnReadWorldActionPerformed
        this.read_coev_world(0);
    }//GEN-LAST:event_main_coev_options_btnReadWorldActionPerformed

    private void main_coev_options_btnReadExecActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_main_coev_options_btnReadExecActionPerformed
        this.read_coev_world(1);
    }//GEN-LAST:event_main_coev_options_btnReadExecActionPerformed
    
    private void main_coev_options_btnTrackKarelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_main_coev_options_btnTrackKarelActionPerformed
        this.track_karel();
    }//GEN-LAST:event_main_coev_options_btnTrackKarelActionPerformed

    private void main_coev_painter_scrXAdjustmentValueChanged(java.awt.event.AdjustmentEvent evt) {//GEN-FIRST:event_main_coev_painter_scrXAdjustmentValueChanged
        if( this.coev_resize )
            return;
        KarelPanel p = (KarelPanel)(this.main_coev_painter_pnlPainter);
        p.moveWorld( this.main_coev_painter_scrX.getValue(),
                     this.main_coev_painter_scrY.getMaximum() - this.main_coev_painter_scrY.getValue());
    }//GEN-LAST:event_main_coev_painter_scrXAdjustmentValueChanged

    private void main_coev_painter_scrYAdjustmentValueChanged(java.awt.event.AdjustmentEvent evt) {//GEN-FIRST:event_main_coev_painter_scrYAdjustmentValueChanged
        this.main_coev_painter_scrXAdjustmentValueChanged(null);
    }//GEN-LAST:event_main_coev_painter_scrYAdjustmentValueChanged

    private void main_coev_painter_pnlPainterKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_main_coev_painter_pnlPainterKeyPressed
        this.coev_press_control = (evt.getKeyCode() == KeyEvent.VK_CONTROL);
        if( this.main_pop_world.isVisible() )
            this.main_pop_worldKeyReleased(evt);
        else if( '0' <= evt.getKeyChar() && evt.getKeyChar() <= '9' &&
                 ((KarelPanel)this.main_coev_painter_pnlPainter).getState() == KarelPanel.BEEPER )
        {
            ((KarelPanel)this.main_coev_painter_pnlPainter).setBeeperQtty( evt.getKeyChar() - '0');
            ((KarelPanel)this.main_coev_painter_pnlPainter).paint();
        }
    }//GEN-LAST:event_main_coev_painter_pnlPainterKeyPressed

    private void main_coev_painter_pnlPainterKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_main_coev_painter_pnlPainterKeyReleased
        this.coev_press_control = false;
    }//GEN-LAST:event_main_coev_painter_pnlPainterKeyReleased

    private void main_coev_painter_pnlPainterMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_main_coev_painter_pnlPainterMouseWheelMoved
        if( !this.coev_press_control )
            this.main_coev_painter_scrY.setValue( this.main_coev_painter_scrY.getValue() + evt.getWheelRotation() );
        else
            this.main_coev_painter_scrX.setValue( this.main_coev_painter_scrX.getValue() - evt.getWheelRotation() );
    }//GEN-LAST:event_main_coev_painter_pnlPainterMouseWheelMoved
    
    private void main_coev_painter_pnlPainterComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_main_coev_painter_pnlPainterComponentResized
        Rectangle coev = ((KarelPanel)(this.main_coev_painter_pnlPainter)).getWorld();

        this.coev_resize = true;
        this.main_coev_painter_scrX.setMaximum( this.env.numAvenidas() - coev.width );
        this.main_coev_painter_scrY.setMaximum( this.env.numCalles() - coev.height );
        this.coev_resize = false;
        this.main_coev_painter_scrYAdjustmentValueChanged(null);
    }//GEN-LAST:event_main_coev_painter_pnlPainterComponentResized

    private void main_coev_settings_txtInsKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_main_coev_settings_txtInsKeyReleased
        this.check_positive_content((javax.swing.JTextField)evt.getSource(), 0, 65535, false);
        this.set_coev_change_values(true);
    }//GEN-LAST:event_main_coev_settings_txtInsKeyReleased

    private void main_coev_settings_cmbFaceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_main_coev_settings_cmbFaceActionPerformed
        if( this.loading_app )
            return;
        this.set_coev_change_values(true);
    }//GEN-LAST:event_main_coev_settings_cmbFaceActionPerformed

	private void main_coev_settings_lstBeepersKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_main_coev_settings_lstBeepersKeyPressed
        if( evt.getKeyCode() == KeyEvent.VK_DELETE )
        {
            this.remove_coev_beepers_selected();
            this.main_coev_settings_lstBeepers.setSelectedIndex(0);
        }
        ((KarelPanel)(this.main_coev_painter_pnlPainter)).paint();
    }//GEN-LAST:event_main_coev_settings_lstBeepersKeyPressed
	
    private void main_coev_settings_btnAddPKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_main_coev_settings_btnAddPKActionPerformed
        if( this.main_coev_settings_txtAddPKA.getText().trim().isEmpty() ||
                this.main_coev_settings_txtAddPKC.getText().trim().isEmpty() ||
                this.main_coev_settings_txtAddPKB.getText().trim().isEmpty() ) {
            JOptionPane.showMessageDialog(this, "Error al insertar: NO puede haber campos vacios", "Error", JOptionPane.OK_OPTION);
            return;
        }
        try {
            int a = Integer.parseInt(this.main_coev_settings_txtAddPKA.getText());
            int c = Integer.parseInt(this.main_coev_settings_txtAddPKC.getText());
            int b = Integer.parseInt(this.main_coev_settings_txtAddPKB.getText());
            if( a == 0 || c == 0 )
                throw new NumberFormatException("");
            this.add_coev_beeper(a, c, b);
        } catch( NumberFormatException nfe){
            if( nfe.getMessage().length() == 0 )
                JOptionPane.showMessageDialog(this, "Error al insertar: la avenida o calle NO pueden ser 0", "Error", JOptionPane.OK_OPTION);
            else
                JOptionPane.showMessageDialog(this, "Error al insertar: algunos datos NO son numericos", "Error", JOptionPane.OK_OPTION);
        }
        ((KarelPanel)(this.main_coev_painter_pnlPainter)).paint();
    }//GEN-LAST:event_main_coev_settings_btnAddPKActionPerformed

    private void main_coev_settings_btnClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_main_coev_settings_btnClearActionPerformed
        this.coev_beeper.clear();
        ((KarelPanel)(this.main_coev_painter_pnlPainter)).paint();
    }//GEN-LAST:event_main_coev_settings_btnClearActionPerformed
	
    private void main_coev_painter_pnlPainterMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_main_coev_painter_pnlPainterMouseMoved
        this.process_coev_mouse_event(evt, "display");
    }//GEN-LAST:event_main_coev_painter_pnlPainterMouseMoved
	
    private void main_coev_painter_pnlPainterMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_main_coev_painter_pnlPainterMouseClicked
        // process add or remove beepers
        if( evt.getButton() == MouseEvent.BUTTON1 )
            this.process_coev_mouse_event(evt, "add:click");
        //
        else if( evt.getButton() == MouseEvent.BUTTON3 && evt.getClickCount() == 1 )
        {
            this.main_pop_world.show(this.main_coev_painter_pnlPainter, evt.getX(), evt.getY());
            this.pop_world_click_event = evt;
        }
    }//GEN-LAST:event_main_coev_painter_pnlPainterMouseClicked
	
    private void main_coev_painter_pnlPainterMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_main_coev_painter_pnlPainterMousePressed
        this.coev_press_button = evt.getClickCount();
        this.set_coev_change_values(true);
    }//GEN-LAST:event_main_coev_painter_pnlPainterMousePressed

    private void main_coev_painter_pnlPainterMouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_main_coev_painter_pnlPainterMouseDragged
        if( this.coev_press_button == 1 )
            this.process_coev_mouse_event(evt, "add:drag");
        else if( this.coev_press_button == 2 )
            this.process_coev_mouse_event(evt, "remove:drag");
    }//GEN-LAST:event_main_coev_painter_pnlPainterMouseDragged
	
    private void main_coev_painter_pnlPainterMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_main_coev_painter_pnlPainterMouseReleased
        this.coev_press_button = 0;
    }//GEN-LAST:event_main_coev_painter_pnlPainterMouseReleased

    private void main_coev_settings_lstBeepersValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_main_coev_settings_lstBeepersValueChanged
        this.coev_beeper_selected.clear();
        int indices[] = this.main_coev_settings_lstBeepers.getSelectedIndices();
        for( int i = 0, n = indices.length; i < n; i++ )
            this.coev_beeper_selected.addElement( this.coev_beeper.get(indices[i]) );
        ((KarelPanel)(this.main_coev_painter_pnlPainter)).paint();
    }//GEN-LAST:event_main_coev_settings_lstBeepersValueChanged

    // </editor-fold>

    // -----------------------------------------------------------------
    // <editor-fold defaultstate="collapsed" desc="Help Event methods">
    private void main_help_options_btnSwitchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_main_help_options_btnSwitchActionPerformed
        try 
        {
            if( this.main_help_options_btnSwitch.isSelected() )
                UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
            else
                UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception e){}
    }//GEN-LAST:event_main_help_options_btnSwitchActionPerformed
    
    // </editor-fold>

    // -----------------------------------------------------------------
    // <editor-fold defaultstate="collapsed" desc="Global Objects">
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel main_code;
    private javax.swing.JScrollPane main_code_editor_container;
    private javax.swing.JTextPane main_code_editor_txtCode;
    private javax.swing.JButton main_code_options_btnChangeLang;
    private javax.swing.JButton main_code_options_btnCompile;
    private javax.swing.JButton main_code_options_btnCopy;
    private javax.swing.JButton main_code_options_btnCut;
    private javax.swing.JButton main_code_options_btnNew;
    private javax.swing.JButton main_code_options_btnOpen;
    private javax.swing.JButton main_code_options_btnPaste;
    private javax.swing.JButton main_code_options_btnRedo;
    private javax.swing.JButton main_code_options_btnSave;
    private javax.swing.JButton main_code_options_btnSaveAs;
    private javax.swing.JButton main_code_options_btnUndo;
    private javax.swing.JPanel main_code_options_coding;
    private javax.swing.JPanel main_coev;
    private javax.swing.JSplitPane main_coev_container;
    private javax.swing.JButton main_coev_options_btnNew;
    private javax.swing.JButton main_coev_options_btnOpen;
    private javax.swing.JButton main_coev_options_btnReadExec;
    private javax.swing.JButton main_coev_options_btnReadWorld;
    private javax.swing.JButton main_coev_options_btnSave;
    private javax.swing.JButton main_coev_options_btnSaveAs;
    private javax.swing.JButton main_coev_options_btnTrackKarel;
    private javax.swing.JPanel main_coev_options_container;
    private javax.swing.JPanel main_coev_painter_container;
    private javax.swing.JPanel main_coev_painter_pnlPainter;
    private javax.swing.JScrollBar main_coev_painter_scrX;
    private javax.swing.JScrollBar main_coev_painter_scrY;
    private javax.swing.JButton main_coev_settings_btnAddPK;
    private javax.swing.JButton main_coev_settings_btnClear;
    private javax.swing.JCheckBox main_coev_settings_chkBeeperBag;
    private javax.swing.JCheckBox main_coev_settings_chkBeepers;
    private javax.swing.JCheckBox main_coev_settings_chkBeepersPos;
    private javax.swing.JCheckBox main_coev_settings_chkIns;
    private javax.swing.JCheckBox main_coev_settings_chkMove;
    private javax.swing.JCheckBox main_coev_settings_chkOKarel;
    private javax.swing.JCheckBox main_coev_settings_chkPKarel;
    private javax.swing.JCheckBox main_coev_settings_chkPick;
    private javax.swing.JCheckBox main_coev_settings_chkPut;
    private javax.swing.JCheckBox main_coev_settings_chkTurn;
    private javax.swing.JComboBox main_coev_settings_cmbFace;
    private javax.swing.JPanel main_coev_settings_container;
    private javax.swing.JLabel main_coev_settings_jLabel1;
    private javax.swing.JLabel main_coev_settings_jLabel2;
    private javax.swing.JLabel main_coev_settings_jLabel3;
    private javax.swing.JLabel main_coev_settings_jLabel4;
    private javax.swing.JLabel main_coev_settings_jLabel5;
    private javax.swing.JLabel main_coev_settings_jLabel6;
    private javax.swing.JLabel main_coev_settings_jLabel7;
    private javax.swing.JLabel main_coev_settings_jLabel8;
    private javax.swing.JScrollPane main_coev_settings_jScrollPane1;
    private javax.swing.JList main_coev_settings_lstBeepers;
    private javax.swing.JTextField main_coev_settings_txtAddPKA;
    private javax.swing.JTextField main_coev_settings_txtAddPKB;
    private javax.swing.JTextField main_coev_settings_txtAddPKC;
    private javax.swing.JTextField main_coev_settings_txtBeeperBag;
    private javax.swing.JTextField main_coev_settings_txtBeepersPos;
    private javax.swing.JTextField main_coev_settings_txtIns;
    private javax.swing.JTextField main_coev_settings_txtMove;
    private javax.swing.JTextField main_coev_settings_txtPKA;
    private javax.swing.JTextField main_coev_settings_txtPKC;
    private javax.swing.JTextField main_coev_settings_txtPick;
    private javax.swing.JTextField main_coev_settings_txtPut;
    private javax.swing.JTextField main_coev_settings_txtTurn;
    private javax.swing.JPanel main_eval;
    private javax.swing.JPanel main_exec;
    private javax.swing.JScrollPane main_exec_code_codecontainer;
    private javax.swing.JTextPane main_exec_display_txtCode;
    private javax.swing.JButton main_exec_options_btnNextStep;
    private javax.swing.JButton main_exec_options_btnRestart;
    private javax.swing.JButton main_exec_options_btnRun;
    private javax.swing.JPanel main_exec_options_debug;
    private javax.swing.JLabel main_exec_options_jLabel1;
    private javax.swing.JTextField main_exec_options_txtSleep;
    private javax.swing.JPanel main_exec_painter_pnlPainter;
    private javax.swing.JSplitPane main_exec_splitpane;
    private javax.swing.JToggleButton main_help_options_btnSwitch;
    private javax.swing.JPanel main_help_options_container;
    private javax.swing.JPanel main_help_options_container1;
    private javax.swing.JPopupMenu main_pop_world;
    private javax.swing.JMenuItem main_pop_world_beepers_mni0;
    private javax.swing.JMenuItem main_pop_world_beepers_mni1;
    private javax.swing.JMenuItem main_pop_world_beepers_mni2;
    private javax.swing.JMenuItem main_pop_world_beepers_mni3;
    private javax.swing.JMenuItem main_pop_world_beepers_mni4;
    private javax.swing.JMenuItem main_pop_world_beepers_mni5;
    private javax.swing.JMenuItem main_pop_world_beepers_mni6;
    private javax.swing.JMenuItem main_pop_world_beepers_mni7;
    private javax.swing.JMenuItem main_pop_world_beepers_mni8;
    private javax.swing.JMenuItem main_pop_world_beepers_mni9;
    private javax.swing.JMenuItem main_pop_world_beepers_mniInf;
    private javax.swing.JMenuItem main_pop_world_beepers_mniN;
    private javax.swing.JPopupMenu.Separator main_pop_world_jSeparator1;
    private javax.swing.JMenu main_pop_world_karel;
    private javax.swing.JMenuItem main_pop_world_karel_east;
    private javax.swing.JMenuItem main_pop_world_karel_north;
    private javax.swing.JMenuItem main_pop_world_karel_south;
    private javax.swing.JMenuItem main_pop_world_karel_west;
    private javax.swing.JPanel main_status_container;
    private javax.swing.JLabel main_status_lblStatus;
    private javax.swing.JTabbedPane main_tabKarelArenaTabs;
    private javax.swing.JPanel main_world;
    private javax.swing.JButton main_world_options_btnInfBag;
    private javax.swing.JButton main_world_options_btnNew;
    private javax.swing.JButton main_world_options_btnOpen;
    private javax.swing.JButton main_world_options_btnSave;
    private javax.swing.JButton main_world_options_btnSaveAs;
    private javax.swing.JPanel main_world_options_drawing;
    private javax.swing.JLabel main_world_options_jLabel1;
    private javax.swing.JTextField main_world_options_txtBeepersBag;
    private javax.swing.JPanel main_world_painter;
    private javax.swing.JPanel main_world_painter_pnlPainter;
    private javax.swing.JScrollBar main_world_painter_scrX;
    private javax.swing.JScrollBar main_world_painter_scrY;
    // End of variables declaration//GEN-END:variables
    // </editor-fold>
}
