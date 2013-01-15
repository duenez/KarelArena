/*
 * KarelExecutor.java
 *
 * Created on January 6, 2007, 7:48 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package application.karel;

import application.karel.executor.KarelExecutorHandler;
import application.karel.executor.KarelJavaExecutor;
import application.karel.executor.KarelPascalExecutor;
import com.instil.lgi.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.Stack;

/**
 *
 * @author Edgar A. Duenez-Guzman
 */

public class KarelExecutor
{

    /**
     * Stack of execution with pairs of blocks and integers. This is a meta
     * execStack, that is implemented to avoid the recursion limit of Java calls
     * that gets hit when executing Karel code. This essentially shifts the
     * execStack to heap space where there is no limit but the main memory of the
     * system.
     * The block represents the currently executing instruction.
     * The integer represents the stage within the block. (see Karel*Executor)
     */
    private Stack<Entry<Block,Integer>> execStack;
    /**
     * Stack of return values. Whenever a block finishes execution, it places
     * its return value in this stack. Thus, whenever another block needs it,
     * it can recover it and use it. Many of these might be null.
     */
    private Stack<Object> retStack;

    private KarelExecutorHandler executor = null;
    
    private KarelEnvironment env;
    private Block constructor;

    private volatile Thread runner;
    private volatile boolean debugging, paused;
    private volatile long sleep = 300;

    private ExecutionListener listener;
    private ExceptionHandler handler = null;

    public static enum CompileLang{ NO_LANG, LANG_JAVA, LANG_PASCAL };

    private CompileLang compile_lang = CompileLang.NO_LANG;

    /** Creates a new instance of LogoExecutor */
    public KarelExecutor( KarelEnvironment env )
    {
        this.env = env;
        this.set_compile_lang( CompileLang.LANG_JAVA );
    }

    public boolean is_debbuging( )                              { return this.debugging; }
    public boolean is_paused( )                                 { return this.paused; }
    public boolean is_running()                                 { return runner != null; }

    public void setExecutionListener( ExecutionListener l )     { listener = l; }
    public void setDebugging( boolean d )                       { debugging = d;}
    public void setPaused( boolean p )                          { paused = p; }
    public void setEnvironment( KarelEnvironment e )            { env = e; }
    public void setSleep( long v )                              { if( v >= 0 ) sleep = v; }
    public void setExceptionHandler(ExceptionHandler handler)   { this.handler = handler; }
    public void set_compile_lang(CompileLang _cl)
    {
        if( this.compile_lang == _cl )
            return;
        this.compile_lang = _cl;
        if( _cl == CompileLang.LANG_JAVA )
            executor = new KarelJavaExecutor();
        else if( _cl == CompileLang.LANG_PASCAL )
            executor = new KarelPascalExecutor();
        execStack = new Stack<Entry<Block,Integer>>();
        retStack = new Stack<Object>();
    }

    public KarelEnvironment getEnvironment()                    { return env; }
    public Thread getRunner()                                   { return runner; }
    public CompileLang get_compile_lang()                       { return this.compile_lang; }
    public long getSleep()                                      { return sleep; }
    public ExceptionHandler getExceptionHandler()               { return handler; }


    public void compile( Block b )
    {
        this.constructor = executor.compile(b);
    }

    public void stop()
    {
        runner = null;
        debugging = false;
        paused = false;
    }
    
    public void checkStop()
    {
        if( runner == null )
            throw new KarelException( "Interrupted" );
    }

    public void sleep()
    {
        if( sleep > 0 )
        	try {
        		Thread.sleep( sleep );
        	} catch( InterruptedException e ) {  }
        this.checkStop();
    }
    public void run( final int ID )
    {
        this.env.getKarel().clearNativeCalls();
        this.env.getKarel().clearConditionalCalls();
        this.executor.set_values(this, this.env, this.listener);

        runner = new Thread() {
            @Override public void run()
            {
                try {
                    //clear Karel function calls
                    env.clearStack();
                    //Clear the language execution execStack (parsing and executing Karel code)
                    execStack.clear();
                    retStack.clear();
                    execStack.push( new SimpleEntry<Block, Integer>( constructor.getChild(1), 0 ) );
                    execute();
                    throw new KarelException( "La ejecuci\u00F3n ha terminado normalmente" );
                }
                catch( KarelException e ) { handler.karelException( ID, e );}
                catch( StackOverflowError er ) { handler.stackOverflow( ID, er ); }
                catch( NullPointerException ex )
                {
                    if( constructor == null )
                    	handler.notCompiledException( ID, ex );
                    if( env == null )
                    	handler.initializationException( ID, ex );
                }
                catch( Exception e ) { handler.generalException( ID, e ); }
                handler.doneExecuting( ID );
            }
        };
        runner.start();
    }    

    public void execute()
    {
        while( !execStack.empty() )
        {
            this.checkStop();
            if( debugging )
            {
                Block b = execStack.peek().getKey();
                if( b.getType() >= 0 )
                {
                    boolean __wait_block = KarelExecutor.WAIT_BLOCK[ b.getType() ] == 1;
                    if( listener != null && __wait_block )
                        listener.executionEvent( b.getBegin(), b.getEnd() );
                    while( paused && __wait_block )
                    {
                        try {
                            Thread.sleep( 50 );
                        } catch( InterruptedException e ) {}
                    }
                }
            }
            try {
                executor.execute( execStack, retStack );
            } catch( KarelReturnException ex ) { /* Do nothing, just returned. */ }
        }
    }
    
	/*
     * WARNING: These numbers correspond to the order induced by the file
     * KarelSyntax.java.txt or any other syntax file
     */
    public static enum BlockType
    {
        ClassDeclaration, ClassBody, MethodDeclaration, ConstructorDeclaration, MethodDeclarator,
        FormalParameter, ResultType, OptionalArgument, Statement, Block, CallStatement, IfStatement,
        WhileStatement, IterateStatement, TurnoffStatement, ReturnStatement, TurnleftStatement,
        MoveStatement, PickbeeperStatement, PutbeeperStatement, Expression, AndClause, NotClause,
        AtomClause, BooleanFunction, IntExp, Identifier, Decimal, OR, AND, NOT, SEMICOLON, OPEN,
        CLOSE, CURLYOPEN, CURLYCLOSE, Spacing, Comment, Space, EndOfLine, EndOfFile, DUMMY
    };

    public static int WAIT_BLOCK[] = {
        0 /*ClassDeclaration*/,
        0 /*ClassBody*/,
        0 /*MethodDeclaration*/,
        0 /*ConstructorDeclaration*/,
        0 /*MethodDeclarator*/,
        0 /*FormalParameter*/,
        0 /*ResultType*/,
        0 /*OptionalArgument*/,
        0 /*Statement*/,
        0 /*Block*/,
        1 /*CallStatement*/,
        0 /*IfStatement*/,
        0 /*WhileStatement*/,
        0 /*IterateStatement*/,
        1 /*TurnoffStatement*/,
        1 /*ReturnStatement*/,
        1 /*TurnleftStatement*/,
        1 /*MoveStatement*/,
        1 /*PickbeeperStatement*/,
        1 /*PutbeeperStatement*/,
        0 /*Expression*/,
        1 /*AndClause*/,
        0 /*NotClause*/,
        0 /*AtomClause*/,
        1 /*BooleanFunction*/,
        0 /*IntExp*/,
        0 /*Identifier*/,
        0 /*Decimal*/,
        0 /*OR*/,
        0 /*AND*/,
        0 /*NOT*/,
        0 /*SEMICOLON*/,
        0 /*OPEN*/,
        0 /*CLOSE*/,
        0 /*CURLYOPEN*/,
        0 /*CURLYCLOSE*/,
        0 /*Spacing*/,
        0 /*Comment*/,
        0 /*Space*/,
        0 /*EndOfLine*/,
        0 /*EndOfFile*/
    };
}