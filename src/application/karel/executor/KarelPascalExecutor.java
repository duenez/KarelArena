/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package application.karel.executor;

import application.karel.ExecutionListener;
import application.karel.KarelEnvironment;
import application.karel.KarelException;
import application.karel.KarelExecutor;
import application.karel.KarelReturnException;
import com.instil.lgi.Block;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.Stack;

/**
 *
 * @author Kuko
 */
public class KarelPascalExecutor implements KarelExecutorHandler
{
    private KarelExecutor ka_exec = null;
    private KarelEnvironment ka_env = null;
    private KarelProcedureManager pas_proc_man = null;
    private ExecutionListener ka_exe_lis = null;

    public Block compile( Block b )
    {
        Block p;
        b = b.getChild(1);
        this.pas_proc_man = new KarelProcedureManager();
        Block constructor = ( b.getChild(0).getType() == KarelExecutor.BlockType.ConstructorDeclaration.ordinal() ? b.getChild(0) : b.getChild(1) );

        for( int k = 0; k < b.getNumChildren()-1; k++ )
            if( b.getChild(k).getType() < 0 )
            {
                p = b.getChild(k);
                for( int i = 0; i < p.getNumChildren(); i++ )
                    this.pas_proc_man.add( p.getChild(i) );
            }
        this.pas_proc_man.refresh();
        this.checkProcNames(b);
        return constructor;
    }

    private void checkProcNames( Block b )
    {
        if( b.getType() == KarelExecutor.BlockType.CallStatement.ordinal() )
            this.pas_proc_man.get( b.getChild(0).getSubtext() );

        for( int i = 0; i < b.getNumChildren(); i++ )
            this.checkProcNames( b.getChild(i) );
    }

    public void set_values(KarelExecutor __ka_exec,
                            KarelEnvironment __ka_env,
                            ExecutionListener __ka_exe_lis)
    {
        this.ka_exec    = __ka_exec;
        this.ka_env     = __ka_env;
        this.ka_exe_lis = __ka_exe_lis;
    }

    /**
     * Dumb way of doing it... no reusing of entries... potentially slow if Java
     * optimizer is not intelligent enough. This is why I am emcapsulating it in
     * this method, so that if we need to optimize it, we can change it here,
     * and be done everywhere.
     * @param execStack
     * @param b
     */
    private void pushChild( Stack<Entry<Block,Integer>> execStack, Block b )
    {
        execStack.push( new SimpleEntry<Block, Integer>( b, 0 ) );
    }
    private void popChild( Stack<Entry<Block,Integer>> execStack )
    {
        execStack.pop();
    }

    public void execute( Stack<Entry<Block,Integer>> execStack, Stack<Object> retStack )
    {
        Block b = execStack.peek().getKey();
        Entry<Block,Integer> execEntry = execStack.peek();
        KarelExecutor.BlockType bt;

        if( b.getType() < 0 )
            bt = KarelExecutor.BlockType.DUMMY;
        else
            bt = KarelExecutor.BlockType.values()[ b.getType() ];

        switch( bt )
        {
            case Decimal:
                popChild( execStack );
                retStack.push( Integer.parseInt( b.getSubtext() ) );
                break;
            case Identifier:
                popChild( execStack );
                retStack.push( ka_env.getLocal( b.getSubtext() ) );
                break;
            case Block:
                popChild( execStack );
                pushChild( execStack, b.getChild(1) );
                break;
            case CallStatement:
                String func = b.getChild(0).getSubtext();
                Block p = this.pas_proc_man.get( func );
                int arg;
                //SimpleEntry<String,Block> en = new SimpleEntry<String, Block>( func, p );
                if( execEntry.getValue() == 0 )
                {
                    if( b.getChild(1).getNumChildren() == 3 )   //Has an argument
                    {
                        pushChild( execStack, b.getChild(1).getChild(1) );
                        execEntry.setValue( 1 );    //Prepare to retrieve the value
                        break;
                    }
                    else
                        retStack.push( 0 );
                    execEntry.setValue( 1 );    //Prepare to retrieve the value
                }
                if( execEntry.getValue() == 1 )
                {
                    arg = (Integer)retStack.pop();
                    this.ka_env.pushStack( func );
                    if( b.getChild(1).getNumChildren() == 3 )   //Has an argument
                        if( p.getChild(1).getChild(1) != null && p.getChild(1).getChild(1).getNumChildren() == 3 )   //Receives an argument
                            this.ka_env.createLocal( p.getChild(1).getChild(1).getChild(1).getSubtext(), arg );
                        else
                            throw new KarelException( "La funci\u00F3n '" + b.getChild(0).getSubtext() + "' no tiene argumentos." );
                    pushChild( execStack, p.getChild(3) );
                    execEntry.setValue( 2 );    //Prepare to finish the function call
                }
                else if( execEntry.getValue() == 2 )    //Clean-up after execution of the function
                {
                    this.ka_env.popStack();
                    popChild( execStack );
                    //retStack.pop();
                }
                break;
            case IfStatement:
                switch( execEntry.getValue() )
                {
                    case 0: //Execute the condition
                        pushChild( execStack, b.getChild(1) );
                        execEntry.setValue( 1 );    //Prepare to execute if needed
                        break;
                    case 1: //Execute the body
                        boolean bool = (Boolean)retStack.pop();
                        if( bool )
                        {
                            pushChild( execStack, b.getChild(3) );
                            execEntry.setValue( 2 );    //Prepare to clean-up
                        }
                        else if( b.getNumChildren() > 4 )    //It has an (optional) else
                        {
                            pushChild( execStack, b.getChild(4).getChild(0).getChild(1) );
                            execEntry.setValue( 2 );    //Prepare to clean-up
                        }
                        else    //The condition is false, and there is no else
                            popChild( execStack );
                        break;
                    case 2: //Clean-up
                        popChild( execStack );
                        //retStack.pop();
                        break;
                }
                break;
            case WhileStatement:
                switch( execEntry.getValue() )
                {
                    case 0: //Execute the condition
                        pushChild( execStack, b.getChild(1) );
                        execEntry.setValue( 1 );    //Prepare to execute if needed
                        break;
                    case 1: //Execute the body
                        boolean bool = (Boolean)retStack.pop();
                        if( bool )
                        {
                            pushChild( execStack, b.getChild(3) );
                            execEntry.setValue( 0 );    //Prepare to re-test
                        }
                        else    //The condition is false
                            popChild( execStack );
                        break;
                    //case 2: //Clean-up and re-test
                    //    pushChild( execStack, b.getChild(2) );
                    //    execEntry.setValue( 1 );    //Prepare to execute if needed
                        //retStack.pop(); //Cleanup dangling return value
                    //    break;
                }
                break;
            case IterateStatement:
                switch( execEntry.getValue() )
                {
                    case 0: //Obtain the integer
                        pushChild( execStack, b.getChild(1) );
                        execEntry.setValue( 1 );
                        break;
                    case 1:
                        int n = (Integer) retStack.pop();
                        popChild( execStack );
                        for( int i = 0; i < n; i++ )
                            pushChild( execStack, b.getChild(3) );
                        break;
                }
                break;
            case ReturnStatement:
                popChild( execStack );
                throw new KarelReturnException();
            case TurnoffStatement:
                popChild( execStack );
                throw new KarelException( "La ejecuci\u00F3n ha terminado normalmente" );
            case TurnleftStatement:
                this.ka_exec.sleep();
                this.ka_env.getKarel().turnLeft();
                this.ka_exe_lis.paint_enviroment();
                popChild( execStack );
                break;
            case MoveStatement:
                this.ka_exec.sleep();
                this.ka_env.getKarel().move();
                this.ka_exe_lis.paint_enviroment();
                popChild( execStack );
                break;
            case PickbeeperStatement:
                this.ka_exec.sleep();
                this.ka_env.getKarel().pickBeeper();
                this.ka_exe_lis.paint_enviroment();
                popChild( execStack );
                break;
            case PutbeeperStatement:
                this.ka_exec.sleep();
                this.ka_env.getKarel().putBeeper();
                this.ka_exe_lis.paint_enviroment();
                popChild( execStack );
                break;
            case Expression:
                switch( execEntry.getValue() )
                {
                    case 0: //Obtain the first boolean
                        pushChild( execStack, b.getChild(1).getChild(0).getChild(0).getChild(1) );
                        execEntry.setValue(1);
                        break;
                    case 1: //Check if we need the second and execute it
                        if( (Boolean)retStack.pop() )
                            retStack.push( true );
                        else
                        {
                            popChild( execStack );  //The answer will be in the retStack at the end
                            pushChild( execStack, b.getChild(0) );
                        }
                        break;
                }
                break;
            case AndClause:
                switch( execEntry.getValue() )
                {
                    case 0: //Obtain the first boolean
                        pushChild( execStack, b.getChild(1).getChild(0).getChild(0).getChild(1) );
                        execEntry.setValue(1);
                        break;
                    case 1: //Check if we need the second and execute it
                        if( !(Boolean)retStack.pop() )
                            retStack.push( false );
                        else
                        {
                            popChild( execStack );  //The answer will be in the retStack at the end
                            pushChild( execStack, b.getChild(0) );
                        }
                        break;
                }
                break;
            case NotClause:
                switch( execEntry.getValue() )
                {
                    case 0: //Obtain the boolean
                        pushChild( execStack, b.getChild(1) );
                        execEntry.setValue(1);
                        break;
                    case 1: //Negate it
                        retStack.push( ! (Boolean)retStack.pop() );
                        popChild( execStack );
                        break;
                }
                break;
            case AtomClause:
                switch( execEntry.getValue() )
                {
                    case 0: //Obtain the boolean
                        if( b.getChild(0).getNumChildren() == 3 )   //Parenthesis
                        {
                            popChild( execStack );
                            pushChild( execStack, b.getChild(0).getChild(1) );
                        }
                        else if( b.getNumChildren() == 1 )
                        {
                            popChild( execStack );
                            pushChild( execStack, b.getChild(0) );
                        }
                        else    //iszero(...)
                        {
                            pushChild( execStack, b.getChild(2) );
                            execEntry.setValue( 1 );    //Get the integer to compare to zero
                        }
                        break;
                    case 1:
                        popChild( execStack );
                        retStack.push( (Integer)( retStack.pop() ) == 0 );
                        break;
                }
            case BooleanFunction:
                this.ka_exec.sleep();
                retStack.push( this.ka_env.test( this.pascal_to_java_boolean_function(b.getChild(0).getSubtext()) ) );
                popChild( execStack );
                break;
            case IntExp:
                int v;
                switch( execEntry.getValue() )
                {
                    case 0: //Obtain the integer
                        if( b.getChild(0).getNumChildren() > 0 )
                        {
                            if( b.getChild(0).getChild(0).getSubtext().equals( "pred" ) )
                            {
                                pushChild( execStack, b.getChild(0).getChild(2) );
                                execEntry.setValue( -1 );    //Will subtract one
                            }
                            else if( b.getChild(0).getChild(0).getSubtext().equals( "succ" ) )
                            {
                                pushChild( execStack, b.getChild(0).getChild(2) );
                                execEntry.setValue( 1 );    //Will add one
                            }
                        }
                        else    //It is just a single expression, just execute it.
                        {
                            popChild( execStack );
                            pushChild( execStack, b.getChild(0) );
                        }
                        break;
                    case -1:
                    case 1:
                        v = (Integer) retStack.pop();
                        retStack.push( v + execEntry.getValue() );
                        popChild( execStack );
                        break;
                }
                break;
            default:
                popChild( execStack );
                for( int i = b.getNumChildren()-1; i >= 0; i-- )
                    pushChild( execStack, b.getChild(i) );
                break;
        }
    }

    public String pascal_to_java_boolean_function(String boolExp)
    {
        if( boolExp.equals( "frente-libre" ) )
            return "frontIsClear";
        if( boolExp.equals( "frente-bloqueado" ) )
            return "frontIsBlocked";
        if( boolExp.equals( "izquierda-libre" ) )
            return "leftIsClear";
        if( boolExp.equals( "izquierda-bloqueda" ) )
            return "leftIsBlocked";
        if( boolExp.equals( "derecha-libre" ) )
            return "rightIsClear";
        if( boolExp.equals( "derecha-bloqueda" ) )
            return "rightIsBlocked";


        if( boolExp.equals( "junto-a-zumbador" ) )
            return "nextToABeeper";
        if( boolExp.equals( "no-junto-a-zumbador" ) )
            return "notNextToABeeper";
        if( boolExp.equals( "algun-zumbador-en-la-mochila" ) )
            return "anyBeepersInBeeperBag";
        if( boolExp.equals( "ningun-zumbador-en-la-mochila" ) )
            return "noBeepersInBeeperBag";

        if( boolExp.equals( "orientado-al-norte" ) )
            return "facingNorth";
        if( boolExp.equals( "no-orientado-al-norte" ) )
            return "notFacingNorth";
        if( boolExp.equals( "orientado-al-oeste" ) )
            return "facingWest";
        if( boolExp.equals( "no-orientado-al-oeste" ) )
            return "notFacingWest";
        if( boolExp.equals( "orientado-al-sur" ) )
            return "facingSouth";
        if( boolExp.equals( "no-orientado-al-sur" ) )
            return "notFacingSouth";
        if( boolExp.equals( "orientado-al-este" ) )
            return "facingEast";
        if( boolExp.equals( "no-orientado-al-este" ) )
            return "notFacingEast";
        return "";
    }
}