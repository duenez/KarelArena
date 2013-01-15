/*
 * KarelEnvironment.java
 *
 * Created on January 6, 2007, 7:31 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package application.karel;

import java.awt.Point;
import java.util.*;
import java.util.ArrayList;

/**
 *
 * @author Edgar A. Duenez-Guzman
 */
public class KarelEnvironment implements Cloneable
{
    public static enum NativeType { MOVE, PICKBEEPER, PUTBEEPER, TURNLEFT };
    public static enum ConditionalType { frontIsClear, frontIsBlocked, leftIsClear, leftIsBlocked, rightIsClear, rightIsBlocked,
    	nextToABeeper, notNextToABeeper, anyBeepersInBeeperBag, noBeepersInBeeperBag,
    	facingNorth, notFacingNorth, facingWest, notFacingWest, facingSouth, notFacingSouth, facingEast, notFacingEast};
    public static enum Heading { EAST, NORTH, WEST, SOUTH, CENTER };

    private Karel karel;
    private Stack< Map<String, Integer> > locals;
    private Stack<String> execStack;
    private ExecutionListener listener;
    private int[][] beepers;
    private boolean[][] paredes;
    /** Creates a new instance of KarelEnvironment */
    public KarelEnvironment( int avenidas, int calles )
    {
        super();
        karel = new Karel( this );
        locals = new Stack< Map<String, Integer> >();
        execStack = new Stack<String>();
        clearStack();
        setWorldSize( avenidas, calles );
    }
    public void setWorldSize( int avenidas, int calles )
    {
        if( beepers != null && beepers.length == avenidas && beepers[0].length == calles )
        {
            clear();
            return;
        }
        beepers = new int[ avenidas ][ calles ];
        paredes = new boolean[ avenidas+1 ][ 2*calles+1 ];
        clear();
    }
    public void clear()
    {
        int calles = numCalles(), avenidas = numAvenidas();
        for( int i = 0; i < beepers.length; i++ )
            for( int j = 0; j < beepers[i].length; j++ )
                beepers[i][j] = 0;
        for( int i = 0; i < paredes.length; i++ )
            for( int j = 0; j < paredes[i].length; j++ )
                paredes[i][j] = false;
        for( int i = 0; i < avenidas+1; i++ )
        {
            paredes[i][2*calles] = true;
            paredes[i][0] = true;
        }
        for( int i = 0; i < calles; i++ )
        {
            paredes[0][2*i+1] = true;
            paredes[avenidas][2*i+1] = true;
        }
    }

    public int getInfiniteBeepersValue()
    {
        return this.karel.INIFINITE_BEEPERS;
    }
//-------------------- Variables & Execution Stack -----------------------
    public void pushStack() { pushStack( "Desconocido" ); }
    public void pushStack( String func )
    {
        locals.push( new HashMap<String,Integer>() );
        execStack.push( func );
        if( listener != null )
            listener.executionEvent( execStack );
    }
    public void popStack()
    {
        locals.pop();
        execStack.pop();
        if( listener != null )
            listener.executionEvent( execStack );
    }
    public void createLocal( String name, int val ) { locals.peek().put( name, val ); }
    public int getLocal( String name )
    {
        Map<String, Integer> lc = locals.peek();
        if( !lc.containsKey( name ) )
            throw new KarelException( "No existe variable '" + name + "'." );
        return locals.peek().get( name );
    }
    public void clearStack()
    {
        locals.clear();
        execStack.clear();
        execStack.push( "<Constructor>" );
        if( listener != null )
            listener.executionEvent( execStack );
    }
//-------------------- Variables -----------------------

    public Karel getKarel() { return karel; }
    public boolean isThereBeeper( int x, int y )
    {
        return beepers[x][y] > 0;
    }
    public boolean isThereWall( int x, int y, Heading dir )
    {
        switch( dir )
        {
            case EAST:
                return paredes[x+1][2*y+1];
            case NORTH:
                return paredes[x][2*(y+1)];
            case WEST:
                return paredes[x][2*y+1];
            case SOUTH:
                return paredes[x][2*y];
        }
        return false;
    }
    public int numAvenidas() { return beepers.length - 1; }
    public int numCalles() { return beepers[0].length - 1; }
    public void setWall( int x, int y, Heading dir ) { setWall( x, y, dir, true ); }
    public void unsetWall( int x, int y, Heading dir ) { setWall( x, y, dir, false ); }
    public void toggleWall( int x, int y, Heading dir ) { { setWall( x, y, dir, !isThereWall( x, y, dir ) ); } }
    public void setWall( int x, int y, Heading dir, boolean b )
    {
        if( ( x == numAvenidas() && dir.equals( Heading.EAST ) ) || 
            ( y == numCalles() && dir.equals( Heading.NORTH ) ) ||
            ( x == 0 && dir.equals( Heading.WEST ) ) ||
            ( y == 0 && dir.equals( Heading.SOUTH ) ) )
            return;
        y *= 2;
        try {
            switch( dir )
            {
                case EAST:
                    paredes[x+1][y+1] = b;
                    return;
                case NORTH:
                    paredes[x][y+2] = b;
                    return;
                case WEST:
                    paredes[x][y+1] = b;
                    return;
                case SOUTH:
                    paredes[x][y] = b;
                    return;
            }
        } catch( ArrayIndexOutOfBoundsException ex ) { return; }
        throw new RuntimeException( "The direction is not valid" );
    }
    public void setBeepers( int x, int y, int num )
    {
        if( num < 0 )
            throw new RuntimeException( "The number of beepers must be positive" );
        beepers[x][y] = num;
    }
    public int getBeepers( int x, int y )
    {
        return beepers[x][y];
    }

    public Point[] get_all_non_zero_beepers()
    {
        ArrayList<Point> beeper = new ArrayList<Point>();
        for( int i = 0, n = this.numAvenidas(); i < n; i++ )
            for( int j = 0, m = this.numCalles(); j < m; j++ )
               if( this.getBeepers(i, j) > 0 )
                   beeper.add( new Point(i, j) );

        return (Point[])(beeper.toArray(new Point[beeper.size()]));
    }

    public boolean test( String boolExp )
    {
    	karel.processConditional( ConditionalType.valueOf( boolExp ) );
        if( boolExp.equals( "frontIsClear" ) )
            return !isThereWall( karel.x, karel.y, karel.heading );
        if( boolExp.equals( "frontIsBlocked" ) )
            return isThereWall( karel.x, karel.y, karel.heading );
        if( boolExp.equals( "leftIsClear" ) )
            return !isThereWall( karel.x, karel.y, Heading.values()[(karel.heading.ordinal() + 1)&3] );
        if( boolExp.equals( "leftIsBlocked" ) )
            return isThereWall( karel.x, karel.y, Heading.values()[(karel.heading.ordinal() + 1)&3] );
        if( boolExp.equals( "rightIsClear" ) )
            return !isThereWall( karel.x, karel.y, Heading.values()[(karel.heading.ordinal() + 3)&3] );
        if( boolExp.equals( "rightIsBlocked" ) )
            return isThereWall( karel.x, karel.y, Heading.values()[(karel.heading.ordinal() + 3)&3] );

        if( boolExp.equals( "nextToABeeper" ) )
            return isThereBeeper( karel.x, karel.y );
        if( boolExp.equals( "notNextToABeeper" ) )
            return !isThereBeeper( karel.x, karel.y );
        if( boolExp.equals( "anyBeepersInBeeperBag" ) )
            return karel.beepers > 0;
        if( boolExp.equals( "noBeepersInBeeperBag" ) )
            return karel.beepers == 0;

        if( boolExp.equals( "facingNorth" ) )
            return karel.heading.equals( Heading.NORTH );
        if( boolExp.equals( "notFacingNorth" ) )
            return !karel.heading.equals( Heading.NORTH );
        if( boolExp.equals( "facingWest" ) )
            return karel.heading.equals( Heading.WEST );
        if( boolExp.equals( "notFacingWest" ) )
            return !karel.heading.equals( Heading.WEST );
        if( boolExp.equals( "facingSouth" ) )
            return karel.heading.equals( Heading.SOUTH );
        if( boolExp.equals( "notFacingSouth" ) )
            return !karel.heading.equals( Heading.SOUTH );
        if( boolExp.equals( "facingEast" ) )
            return karel.heading.equals( Heading.EAST );
        if( boolExp.equals( "notFacingEast" ) )
            return !karel.heading.equals( Heading.EAST );
        return false;
    }
    @Override public Object clone()
    {
        try {
            KarelEnvironment env = (KarelEnvironment) super.clone();
            env.beepers = (int[][]) beepers.clone();
            for( int i = 0; i < beepers.length; i++ )
                env.beepers[i] = (int[]) beepers[i].clone();

            env.paredes = (boolean[][]) paredes.clone();
            for( int i = 0; i < paredes.length; i++ )
                env.paredes[i] = (boolean[]) paredes[i].clone();

            env.karel = (Karel) karel.clone();
            env.karel.env = env;
            return env;
        } catch( CloneNotSupportedException e ) {}
        return null;
    }

    public class Karel implements Cloneable
    {
        private int INIFINITE_BEEPERS = 65535;

        private int natives[] = new int[NativeType.values().length];
        private int limitsN[] = new int[NativeType.values().length];
        private int conditionals[] = new int[ConditionalType.values().length];
        private int limitsC[] = new int[ConditionalType.values().length];
        private int totalLimitN, totalLimitC, totalCountN, totalCountC;
        private int x, y, beepers;
        private Heading heading;
        private KarelEnvironment env;
        public Karel( KarelEnvironment env ) { this (env, 0, 0, 0, Heading.EAST); }
        public Karel( KarelEnvironment env, int x, int y, int b, Heading h )
        {
            super();
            this.env = env;
            this.x = x;
            this.y = y;
            beepers = b;
            heading = h;
            for( NativeType n : NativeType.values() )
            	limitsN[n.ordinal()] = 100000;
            totalLimitN = 1000000;
            for( ConditionalType c : ConditionalType.values() )
            	limitsC[c.ordinal()] = 100000;
            totalLimitC = 1000000;
        }
        public void clearNativeCalls() { for( NativeType n : NativeType.values() ) natives[n.ordinal()] = 0; totalCountN = 0; }
        public void setNativeCallLimit( NativeType ty, int limit ) { limitsN[ ty.ordinal() ] = limit; }
        public void setNativeCallLimit( int limit ) { totalLimitN = limit; }
        public int getNativeCalls() { return totalCountN; }
        public int getNativeCalls( NativeType n ) { return natives[n.ordinal()]; }
        private void processNative( NativeType n )
        {
            natives[n.ordinal()]++;
            totalCountN++;
            if( natives[n.ordinal()] > limitsN[n.ordinal()] )
                throw new KarelException( "Se ha excedido el n\u00f4mero de llamadas nativas: " + n );
            if( totalCountN > totalLimitN )
                throw new KarelException( "Se ha excedido el n\u00f4mero total de llamadas nativas." );
        }

        public void clearConditionalCalls() { for( ConditionalType c : ConditionalType.values() ) conditionals[c.ordinal()] = 0; totalCountC = 0; }
        public void setConditionalCallLimit( ConditionalType ty, int limit ) { limitsC[ ty.ordinal() ] = limit; }
        public void setConditionalCallLimit( int limit ) { totalLimitC = limit; }
        public int getConditionalCalls() { return totalCountC; }
        public int getConditionalCalls( ConditionalType c ) { return conditionals[c.ordinal()]; }
        private void processConditional( ConditionalType c )
        {
        	conditionals[c.ordinal()]++;
            totalCountC++;
            if( conditionals[c.ordinal()] > limitsC[c.ordinal()] )
                throw new KarelException( "Se ha excedido el n\u00f4mero de condicionales: " + c );
            if( totalCountC > totalLimitC )
                throw new KarelException( "Se ha excedido el n\u00f4mero total de condicionales." );
        }

        public int get_infinite_beepers_value()
        {
            return this.INIFINITE_BEEPERS;
        }

        //Handler methods for natives
        public void move()
        {
            if( env.isThereWall( x, y, heading ) )
                throw new KarelException( "Karel trat\u00f3 de atravezar una pared." );
            switch( heading )
            {
                case EAST:
                    x++;
                    break;
                case NORTH:
                    y++;
                    break;
                case WEST:
                    x--;
                    break;
                case SOUTH:
                    y--;
                    break;
            }
            processNative( NativeType.MOVE );
        }
        public void pickBeeper()
        {
            if( !env.isThereBeeper( x, y ) )
                throw new KarelException( "No hay beepers que recoger en este sitio." );
            if( env.beepers[x][y] != this.INIFINITE_BEEPERS )
                env.beepers[x][y]--;
            if( this.beepers != this.INIFINITE_BEEPERS)
                beepers++;
            processNative( NativeType.PICKBEEPER );
        }
        public void putBeeper()
        {
            if( beepers <= 0 )
                throw new KarelException( "Karel no tienen beepers en su bolsa." );
            if( env.beepers[x][y] != this.INIFINITE_BEEPERS )
                env.beepers[x][y]++;
            if( this.beepers != this.INIFINITE_BEEPERS)
                beepers--;
            processNative( NativeType.PUTBEEPER );
        }
        public void turnLeft()
        {
            heading = Heading.values()[ (heading.ordinal()+1)&3 ];
            processNative( NativeType.TURNLEFT );
        }
        public int getX() { return x; }
        public int getY() { return y; }
        public void setLocation( int x, int y )
        {
            if( x < 0 )
                x = 0;
            if( y < 0 )
                y = 0;
            if( x >= numAvenidas() )
                x = numAvenidas()-1;
            if( y >= numCalles() )
                y = numCalles()-1;
            this.x = x;
            this.y = y;
        }
        public int getNumBeepers() { return beepers; }
        public void setNumBeepers( int b) { beepers = b; }
        public Heading getHeading() { return heading; }
        public void setHeading( Heading h ) { heading = h; }

        @Override
        public Object clone() throws CloneNotSupportedException
        {
            Karel k = (Karel) super.clone();
            k.beepers = beepers;
            k.heading = heading;
            k.x = x;
            k.y = y;
            k.natives = (int[])natives.clone();
            k.limitsN = (int[])limitsN.clone();
            k.conditionals = (int[])conditionals.clone();
            k.limitsC = (int[])limitsC.clone();
            k.env = env;
            return k;
        }
    }
    public ExecutionListener getListener() { return listener; }
    public void setListener( ExecutionListener listener ) { this.listener = listener; }
}
