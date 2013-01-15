/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package application.karel;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 *
 * @author Kuko
 */
public class KarelKECManager
{
    public KarelKECManager()
    {

    }

    public static void read_kec(String filename, KarelKECEnvironment kke)
        throws FileNotFoundException, IOException, KarelException
    {
        BufferedInputStream bin = new BufferedInputStream(new FileInputStream(filename));
        KarelWorldManager.KarelMDOToken kmt[] = new KarelWorldManager.KarelMDOToken[10];

        // Try to read all tokens
        for( int i = 0; i < 10; i++ )
        {
            kmt[i] = new KarelWorldManager.KarelMDOToken();
            KarelWorldManager.KarelMDOToken.read_next_token(kmt[i], bin);
            if( kmt[i].p1 != 0 && kmt[i].p1 != 1 )
                throw new KarelException("El formato de las condiciones es incorrecto");
        }

        // Check Karel orientation
        if( kmt[8].p1 == 1 && (kmt[8].p2 < 0 || kmt[8].p2 > 3)  )
            throw new KarelException("El formato de las condiciones es incorrecto");

        KarelWorldManager.KarelMDOToken beeper[] = new KarelWorldManager.KarelMDOToken[kmt[9].p2];
        for( int i = 0, n = kmt[9].p2; i < n; i++ )
        {
            beeper[i] = new KarelWorldManager.KarelMDOToken();
            KarelWorldManager.KarelMDOToken.read_next_token(beeper[i], bin);
        }
        KarelKECEnvironment.KECTokenType token[] = KarelKECEnvironment.KECTokenType.values();
        for( int i = 0; i < token.length; i++ )
            kke.set(token[i], kmt[i]);
        kke.add_beepers(beeper);
    }

    public static void write_kec(String filename, KarelKECEnvironment kke)
        throws IOException, KarelException
    {
        BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(filename));
        KarelKECEnvironment.KECTokenType token[] = KarelKECEnvironment.KECTokenType.values();
        
        // Check all tokens
        for( int i = 0; i < 10; i++ )
        {
            if( kke.get(token[i]).p1 != 0 && kke.get(token[i]).p1 != 1 )
                throw new KarelException("El formato de las condiciones es incorrecto");
        }
        if( kke.is_set(token[8]) && (kke.get(token[8]).p1 < 0 || kke.get(token[8]).p1 > 3) ) // Karel orientation
            throw new KarelException("El formato de las condiciones es incorrecto");

        if( kke.is_set(token[9]) && kke.get(token[9]).p2 != kke.beeper.length ) // Beepers on the world
            throw new KarelException("El formato de las condiciones es incorrecto");

        // No basic problemas
        for( int i = 0; i < 10; i++ )
            KarelWorldManager.KarelMDOToken.write_token( kke.get(token[i]), bout );        
        for( int i = 0, n = kke.get(token[9]).p2; i < n; i++ )
            KarelWorldManager.KarelMDOToken.write_token( kke.beeper[i], bout );
        bout.close();
    }
}
