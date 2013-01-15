/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package application.karel;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;

import com.instil.lgi.*;
import java.io.IOException;

/**
 *
 * @author Administrador
 */
public class KarelConsole{

    private boolean loadError = false;
    private static PackratParser parser;
    private KarelExecutor exec;
    private KarelException kaexc;
    private ParseError kapar;

    KarelConsole()
    {
        this.exec = new KarelExecutor( null );
        this.exec.setExecutionListener( null );
    }

    KarelConsole( KarelExecutor ke )
    {
        this.exec = ke;
    }

    public boolean is_load_error()
    {
        return this.loadError;
    }

    public void set_compile_lang( KarelExecutor.CompileLang __cl)
    {
        this.exec.set_compile_lang(__cl);
    }

    public void load_parser()
    {
        try
        {
            if( this.exec.get_compile_lang() == KarelExecutor.CompileLang.LANG_JAVA )
                KarelConsole.parser = PackratParser.createParser( this.getClass().getClassLoader().getResource( "application/karel/KarelSyntax.java.txt" ) );
            else if( this.exec.get_compile_lang() == KarelExecutor.CompileLang.LANG_PASCAL )
                KarelConsole.parser = PackratParser.createParser( this.getClass().getClassLoader().getResource( "application/karel/KarelSyntax.pascal.txt" ) );
            KarelConsole.parser.setUtility( KarelUtility.utility );
        } catch( IOException ioexc ){
            this.loadError = true;
        }
    }

    public boolean compile(String code)
    {
        this.load_parser();
        
        File file = new File( code );
        if( file.exists() )
        {
            try{
                BufferedReader r = new BufferedReader( new FileReader( code ) );
                String str = "";
                code = "";
                while( (str = r.readLine()) != null )
                    code += str + "\n";
                r.close();
            } catch( Exception e ){
            }
        }
        return compile_private(code);
    }

    public KarelException get_karel_exception()
    {
        return kaexc;
    }

    public ParseError get_karel_parseerror()
    {
        return kapar;
    }

    public String parse_error( final String perror)
    {
        return perror;
    }

    private boolean compile_private(String code)
    {
        kaexc = null;
        kapar = null;
        Block parsed;
        
        if( !code.equals( "" ) )
        {
            parsed = KarelConsole.parser.parse( code );
            if( parsed != Block.NO_PARSE )
            {
                try
                {
                    exec.compile( parsed );
                    return true;
                } catch( KarelException e ){
                    kaexc = e;
                }
            }
            else
                kapar = KarelConsole.parser.getParseError();
        }
        return false;
    }

}
