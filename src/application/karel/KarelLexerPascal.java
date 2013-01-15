/*
 * KarelLexer.java
 *
 * Created on January 6, 2007, 7:30 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package application.karel;

import com.Ostermiller.Syntax.Lexer.*;
import java.io.*;
import com.instil.lgi.*;
import java.util.regex.Matcher;

/**
 *
 * @author Edgar A. Duenez-Guzman
 */
public class KarelLexerPascal implements Lexer
{
    private PackratParser parser;
    private Reader zzReader;
    private int current = 0, line = 0;
    // Try to parse a reserved word.
    private final String[] rsrv = { 
            "'iniciar-programa'", "'finalizar-programa'",
            "'define-nueva-instruccion'", "'como'",
            "'inicio'", "'fin'",
            "'si'", "'entonces'", "'sino'", "'repetir'", "'veces'", "'mientras'", "'hacer'",
            "'avanza'", "'gira-izquierda'", "'coge-zumbador'", "'deja-zumbador'",
            "'apagate'", "'sal-de-instruccion'"};
    private final char[] buffer = new char[ 1024 ];

    public KarelLexerPascal( Reader reader )
    {
        zzReader = reader;
        parser   = KarelArena.parser;
    }

    public KarelLexerPascal( InputStream inputstream )
    {
        this( ( (Reader)( new InputStreamReader( inputstream ) ) ) );
    }
    
    public Token getNextToken( boolean flag, boolean flag1 ) throws IOException
    {
        Token token;
        for( token = getNextToken(); token != null && ( !flag1 && token.isWhiteSpace() || !flag && token.isComment() ); token = getNextToken() );
        return token;
    }

    public void reset( Reader reader, int i, int j, int k ) throws IOException
    {
        zzReader = reader;
        line = i;
        current = j;
        //column = k;
    }
    public Token getNextToken() throws IOException
    {
        zzReader.mark( 1024 );
        int sz = zzReader.read( buffer );
        if( sz == -1 )
            return null;
        String text = new String( buffer, 0, sz ), temptext = text.toLowerCase();
        Block b;
        Matcher m;

        b = parser.parse( temptext, KarelExecutor.BlockType.Comment.ordinal() );
        if( b != Block.NO_PARSE )
            return accept( b );
        b = parser.parse( temptext, KarelExecutor.BlockType.Space.ordinal() );
        if( b != Block.NO_PARSE )
            return accept( b );
        b = parser.parse( temptext, KarelExecutor.BlockType.BooleanFunction.ordinal() );
        if( b != Block.NO_PARSE )
            return accept( b );

        for( int i = 0; i < rsrv.length; i++ )
        {
            if( parser.getGrammar().getPattern( rsrv[i] ) != null )
            {
                m = parser.getGrammar().getPattern( rsrv[i] ).matcher( temptext );
                if( m.lookingAt() )
                    return accept( text, m );
            }
        }

        // Try to parse a terminal.
        for( int i = KarelUtility.FIRST_TERMINAL; i <= KarelUtility.LAST_TERMINAL; i++ )
        {
            b = parser.parse( temptext, i );
            if( b != Block.NO_PARSE )
                return accept( b );
        }

        return new KarelToken( KarelToken.ERROR, "", 0, current, current );
    }
    private Token accept( String text, Matcher m ) throws IOException
    {
        int end = current + m.end();
        Token t = new KarelToken( KarelToken.RESERVED, text.substring( 0, m.end() ), 0, current, end, Token.INITIAL_STATE );
        //System.out.println( t );
        current = end;
        zzReader.reset();
        zzReader.skip( m.end() );
        return t;
    }
    private Token accept( Block b ) throws IOException
    {
        int ty = b.getType();
        if( b.getNumChildren() > 0 && b.getType() != KarelExecutor.BlockType.Comment.ordinal() )
            b = b.getChild( 0 );
        int end = current + b.getEnd();
        Token t = new KarelToken( ty, b.getSubtext(), line, current, end, Token.INITIAL_STATE );
        //System.out.println( t );
        current = end;
        zzReader.reset();
        zzReader.skip( b.getEnd() );
        return t;
    }
}
