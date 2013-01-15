package application.karel;

import javax.swing.*;

public class DefaultHandler implements ExceptionHandler
{
	private KarelArenaListener listener;
	private JPanel container;

	public DefaultHandler( KarelArenaListener lst, JPanel con )
	{
	    this.container = con;
	    this.listener = lst;
	}

    public void karelException( int ID, KarelException e )
    {
        boolean has_exception = false;
        String exception[] = {"Interrupted"}, msg = e.getMessage();
        for( int i = 0, n = exception.length; i < n && !has_exception; i++ )
            has_exception = exception[i].equals(msg);
        if( !has_exception )
            JOptionPane.showMessageDialog( this.container, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }

    public void stackOverflow( int ID, StackOverflowError e )
    {
        JOptionPane.showMessageDialog( this.container, "Se ha excedido el l\u00EDmite de recursi\u00F3n", "Error", JOptionPane.ERROR_MESSAGE);
    }

    public void notCompiledException( int ID, NullPointerException e )
    {
        JOptionPane.showMessageDialog( this.container, "Necesitas primero compilar", "Error", JOptionPane.ERROR_MESSAGE);
    }

    public void initializationException( int ID, NullPointerException e )
    {
        JOptionPane.showMessageDialog( this.container, "Necesitas primero inicializar", "Error", JOptionPane.ERROR_MESSAGE);
    }

    public void generalException( int ID, Exception e )
    {
        JOptionPane.showMessageDialog( this.container, "Error general: " + e.toString(), "Error", JOptionPane.ERROR_MESSAGE);
    }

    public void doneExecuting( int ID )
    {
        listener.doneExecuting();
    }

    public void compilationException( KarelException e )
    {
        JOptionPane.showMessageDialog( this.container, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}
