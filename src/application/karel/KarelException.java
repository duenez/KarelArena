/*
 * KarelException.java
 *
 * Created on January 6, 2007, 7:47 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package application.karel;

/**
 *
 * @author Edgar A. Duenez-Guzman
 */
public class KarelException extends RuntimeException
{
	private static final long serialVersionUID = 0x0038367104;
	private int from, to;
    /** Creates a new instance of KarelException */
    public KarelException( String msg )
    {
        this( msg, -1, -1 );
    }
    public KarelException( String msg, int f, int t )
    {
        super( msg );
        from = f;
        to = t;
    }
    public boolean hasSource() { return from >= 0; }
    public int from() { return from; }
    public int to() { return to; }
}
