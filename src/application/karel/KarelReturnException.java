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
public class KarelReturnException extends KarelException
{
	private static final long serialVersionUID = 0x002e71124;

	/** Creates a new instance of KarelException */
    public KarelReturnException()
    {
        super( "Returning from call" );
    }
}
