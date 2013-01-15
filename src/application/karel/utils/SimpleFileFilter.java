/*
 * SimpleFileFilter.java
 *
 * Created on 4 de marzo de 2005, 12:35 AM
 */

package application.karel.utils;

import javax.swing.filechooser.*;

/**
 *
 * @author duenez
 */
public class SimpleFileFilter extends FileFilter
{
    private String description;
    private String[] extensions;
    /** Creates a new instance of SimpleFileFilter */
    public SimpleFileFilter( String d, String e ) { this( d, new String[] { e } ); }
    public SimpleFileFilter( String d, String[] e )
    {
        super();
        extensions = new String[ e.length ];
        for( int i = 0; i < e.length; i++ )
            extensions[i] = "." + e[i].toLowerCase();
        description = d + getExtensionsDesc();
    }
    private String getExtensionsDesc()
    {
        String extDesc = " (";
        for( int i = 0; i < extensions.length-1; i++ )
            extDesc += "*" + extensions[i] + ", ";
        extDesc += "*" + extensions[extensions.length-1] + ")";
        return extDesc;
    }
    public String getDescription() { return description; }
    public boolean accept( java.io.File f )
    {
        if( f.isDirectory() )
            return true;
        for( int i = 0; i < extensions.length; i++ )
            if( f.getName().toLowerCase().endsWith( extensions[i] ) )
                return true;
        return false;
    }
}
