/*
 * This file is part of a syntax highlighting package
 * Copyright (C) 1999, 2000  Stephen Ostermiller
 * http://ostermiller.org/contact.pl?regarding=Syntax+Highlighting
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * See COPYING.TXT for details.
 */
package application.karel;

import com.Ostermiller.Syntax.Lexer.Token;

/**
 * A KarelToken is a token that is returned by a lexer that is lexing a karel
 * source file.  It has several attributes describing the token:
 * The type of token, the text of the token, the line number on which it
 * occurred, the number of characters into the input at which it started, and
 * similarly, the number of characters into the input at which it ended. <br>
 * The tokens should comply with the
 * Karel Language Specification.
 */
public class KarelToken extends Token
{
    public static final int ERROR = 10001;
    public static final int RESERVED = 10002;

    private int ID;
    private String contents;
    private int lineNumber;
    private int charBegin;
    private int charEnd;
    private int state;
    
    /**
     * Create a new token.
     * The constructor is typically called by the lexer
     *
     * @param ID the id number of the token
     * @param contents A string representing the text of the token
     * @param lineNumber the line number of the input on which this token started
     * @param charBegin the offset into the input in characters at which this token started
     * @param charEnd the offset into the input in characters at which this token ended
     */
    public KarelToken(int ID, String contents, int lineNumber, int charBegin, int charEnd)
    {
        this(ID, contents, lineNumber, charBegin, charEnd, Token.UNDEFINED_STATE);
    }
    
    /**
     * Create a new token.
     * The constructor is typically called by the lexer
     *
     * @param ID the id number of the token
     * @param contents A string representing the text of the token
     * @param lineNumber the line number of the input on which this token started
     * @param charBegin the offset into the input in characters at which this token started
     * @param charEnd the offset into the input in characters at which this token ended
     * @param state the state the tokenizer is in after returning this token.
     */
    public KarelToken(int ID, String contents, int lineNumber, int charBegin, int charEnd, int state)
    {
        this.ID = ID;
        this.contents = new String(contents);
        this.lineNumber = lineNumber;
        this.charBegin = charBegin;
        this.charEnd = charEnd;
        this.state = state;
    }
    
    /**
     * Get an integer representing the state the tokenizer is in after
     * returning this token.
     * Those who are interested in incremental tokenizing for performance
     * reasons will want to use this method to figure out where the tokenizer
     * may be restarted.  The tokenizer starts in Token.INITIAL_STATE, so
     * any time that it reports that it has returned to this state, the
     * tokenizer may be restarted from there.
     */
    public int getState()
    {
        return state;
    }
    
    /**
     * get the ID number of this token
     *
     * @return the id number of the token
     */
    public int getID()
    {
        return ID;
    }
    
    /**
     * get the contents of this token
     *
     * @return A string representing the text of the token
     */
    public String getContents()
    {
        return (new String(contents));
    }
    
    /**
     * get the line number of the input on which this token started
     *
     * @return the line number of the input on which this token started
     */
    public int getLineNumber()
    {
        return lineNumber;
    }
    
    /**
     * get the offset into the input in characters at which this token started
     *
     * @return the offset into the input in characters at which this token started
     */
    public int getCharBegin()
    {
        return charBegin;
    }
    
    /**
     * get the offset into the input in characters at which this token ended
     *
     * @return the offset into the input in characters at which this token ended
     */
    public int getCharEnd()
    {
        return charEnd;
    }
    
    /**
     * Checks this token to see if it is a reserved word.
     *
     * @return true if this token is a reserved word, false otherwise
     */
    public boolean isReservedWord()
    {
        return ID == RESERVED;
    }
    
    /**
     * Checks this token to see if it is an identifier.
     *
     * @return true if this token is an identifier, false otherwise
     */
    public boolean isIdentifier()
    {
        return ID == KarelExecutor.BlockType.Identifier.ordinal();
    }
    
    /**
     * Checks this token to see if it is a literal.
     *
     * @return true if this token is a literal, false otherwise
     */
    public boolean isLiteral()
    {
        return false;
    }
    
    /**
     * Checks this token to see if it is a Operator.
     *
     * @return true if this token is a Operator, false otherwise
     */
    public boolean isOperator()
    {
        return false;
    }
    
    /**
     * Checks this token to see if it is a comment.
     *
     * @return true if this token is a comment, false otherwise
     */
    public boolean isComment()
    {
        return ID == KarelExecutor.BlockType.Comment.ordinal();
    }
    
    /**
     * Checks this token to see if it is White Space.
     * Usually tabs, line breaks, form feed, spaces, etc.
     *
     * @return true if this token is White Space, false otherwise
     */
    public boolean isWhiteSpace()
    {
        return ID == KarelExecutor.BlockType.Space.ordinal();
    }
    
    /**
     * Checks this token to see if it is an Error.
     * Unfinished comments, numbers that are too big, unclosed strings, etc.
     *
     * @return true if this token is an Error, false otherwise
     */
    public boolean isError()
    {
        return ID == ERROR;
    }
    
    /**
     * A description of this token.  The description should
     * be appropriate for syntax highlighting.  For example
     * "comment" is returned for a comment.
     *
     * @return a description of this token.
     */
    public String getDescription()
    {
        if( isReservedWord() )
            return "reservedWord";
        else if( isIdentifier() )
            return "identifier";
        else if( isLiteral() )
            return "literal";
        else if( isOperator() )
            return "operator";
        else if( isComment() )
            return "comment";
        else if( isWhiteSpace() )
            return "whitespace";
        else if( isError() )
            return "error";
        else if( ID == KarelExecutor.BlockType.Decimal.ordinal() )
            return "literal";
        else if( ID == KarelExecutor.BlockType.BooleanFunction.ordinal() )
            return "name";
        else if( ID >= KarelUtility.FIRST_TERMINAL && ID <= KarelUtility.LAST_TERMINAL )
            return "operator";
        else
            return "unknown";
    }
    
    /**
     * get a String that explains the error, if this token is an error.
     *
     * @return a  String that explains the error, if this token is an error, null otherwise.
     */
    public String errorString()
    {
        if( isError() )
            return "Error on line " + lineNumber;
        return null;
    }
    
    /**
     * get a representation of this token as a human readable string.
     * The format of this string is subject to change and should only be used
     * for debugging purposes.
     *
     * @return a string representation of this token
     */
    public String toString()
    {
        return ("Token #" + Integer.toHexString(ID) + ": " + getDescription() + " Line " +
                lineNumber + " from " + charBegin + " to " + charEnd + " : " + contents);
    }
    
}
