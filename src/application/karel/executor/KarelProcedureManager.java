/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package application.karel.executor;

import application.karel.KarelException;
import com.instil.lgi.Block;
import java.util.Vector;
import java.util.Arrays;
import java.util.Comparator;

/**
 *
 * @author Administrador
 */
public class KarelProcedureManager
{

    private Vector<Block> procs;
    private Vector<String> names;
    private Vector<Integer> indices;

    public KarelProcedureManager()
    {
        procs = new Vector<Block>();
        names = new Vector<String>();
        indices = new Vector<Integer>();
    }
    public void add( Block proc )
    {
        procs.add( proc );
        names.add( proc.getChild(1).getChild(0).getSubtext() );
    }
    public void refresh()
    {
        Integer[] indx = new Integer[ procs.size() ];
        for( int i = 0; i < indx.length; i++ )
            indx[i] = i;
        Arrays.sort( indx, new Comparator<Integer>() {
            public int compare( Integer i, Integer j )
            {
                return names.get( i ).compareTo( names.get(j) );
            }
        } );
        indices.clear();
        for( int i = 0; i < indx.length; i++ )
            indices.add( indx[i] );
    }
    public Block get( String name )
    {
        if( name.equals( "return" ) )
            return null;
        int low = 0, high = names.size()-1, middle, cmp;
        while( low <= high )
        {
            middle = (low + high)/2;
            cmp = names.get( indices.get(middle) ).compareTo( name );
            if( cmp == 0 )
                return procs.get( indices.get(middle) );
            if( cmp < 0 )
                low = middle + 1;
            else if( cmp > 0 )
                high = middle - 1;
        }
        throw new KarelException( "La funci\u00f3n '" + name + "' no existe" );
    }
}
