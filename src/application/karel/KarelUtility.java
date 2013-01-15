/*
 * KarelUtility.java
 *
 * Created on January 6, 2007, 7:08 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package application.karel;

import com.instil.lgi.*;

/**
 *
 * @author Edgar A. Duenez-Guzman
 */
public class KarelUtility implements GrammarUtility
{
    public static int RET_VAL = 6;
    public static int FIRST_BLANK = 36;
    public static int LAST_BLANK = 40;
    public static int FIRST_TERMINAL = 26;
    public static int LAST_TERMINAL = 35;
    public static KarelUtility utility = new KarelUtility();
    private KarelUtility() {}
    public void optimizeParsedTree( Block b )
    {
        //Remove empty parsed blocks.
        TreeOptimizer.removeEmpty( b );

        //Remove all comments and blanks.
        if( b.getType() < FIRST_BLANK || b.getType() > LAST_BLANK )
            for( int i = FIRST_BLANK; i <= LAST_BLANK; i++ )
                TreeOptimizer.removeAllOfType( b, i );
        
        //Lexical analysis gets cropped
        for( int i = FIRST_TERMINAL; i <= LAST_TERMINAL; i++ )
            TreeOptimizer.removeChildrenOfType( b, i );

        TreeOptimizer.removeChildrenOfType( b, RET_VAL );
        
        //Collapse the long branches
        TreeOptimizer.collapse( b, new int[] { 24, 23, 22, 21, 20, 27, 28, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19 } );
    }
    public void optimizeParseError( ParseError e )
    {
        e.dropCauseTypes( -ParsingExpressionGrammar.LITERAL );
        for( int i = FIRST_BLANK; i <= LAST_BLANK; i++ )
            e.dropCauseTypes( i );
        e.dropCauseTypes( -ParsingExpressionGrammar.SEQUENCE );
    }
}
