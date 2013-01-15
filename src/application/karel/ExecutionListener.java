/*
 * ExecutionListener.java
 *
 * Created on January 15, 2007, 11:31 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package application.karel;

import java.util.Stack;

/**
 *
 * @author Edgar A. Duenez-Guzman
 */
public interface ExecutionListener
{
    public void executionEvent( int from, int to );
    public void executionEvent( Stack<String> exec );
    public void paint_enviroment();
}
