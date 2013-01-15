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
import java.util.ArrayList;

/**
 *
 * @author Kuko
 */
public class KarelWorldManager
{
    public KarelWorldManager()
    {
    }

    public static void read_world(String filename, KarelEnvironment ke )
        throws FileNotFoundException, IOException, KarelException
    {
        BufferedInputStream bin = new BufferedInputStream(new FileInputStream(filename));
        KarelWorldManager.KarelMDOToken kmt = new KarelMDOToken();

        // Read headings
        KarelWorldManager.KarelMDOToken.read_next_token(kmt, bin);
        if( kmt.p1 != 16715 ||  kmt.p2 != 17746 || kmt.p3 != 8268 )
            throw new KarelException("El formato del mundo es incorrecto");
        KarelWorldManager.KarelMDOToken.read_next_token(kmt, bin);
        if( kmt.p1 != 19791 ||  kmt.p2 != 11849 )
            throw new KarelException("El formato del mundo es incorrecto");

        // Karel World values
        KarelWorldManager.KarelMDOToken.read_next_token(kmt, bin);
        int w = kmt.p1, h = kmt.p2, beeperbag = kmt.p3;
        
        KarelWorldManager.KarelMDOToken.read_next_token(kmt, bin);
        int kx = kmt.p1 - 1, ky = kmt.p2 - 1, head = kmt.p3;

        KarelWorldManager.KarelMDOToken.read_next_token(kmt, bin);
        int walls = kmt.p1, beepers = kmt.p2;
        
        KarelMDOToken wall[]  = new KarelMDOToken[walls];
        KarelMDOToken beeper[] = new KarelMDOToken[beepers];

        for( int i = 0; i < walls; i++ )
        {
            KarelWorldManager.KarelMDOToken.read_next_token(kmt, bin);
            wall[i] = kmt.clone();
        }
        for( int i = 0; i < beepers; i++ )
        {
            KarelWorldManager.KarelMDOToken.read_next_token(kmt, bin);
            beeper[i] = kmt.clone();
        }
        bin.close();

        // Set values on Karel Enviroment
        ke.setWorldSize(w + 1, h + 1);
        ke.getKarel().setNumBeepers(beeperbag);
        ke.getKarel().setLocation(kx, ky);
        ke.getKarel().setHeading( KarelMDOToken.get_heading(head) );

        // Set walls
        int pnorth = 1, peast = 2, psouth = 4, pwest = 8, current_wall = 0;
        for( int i = 0; i < walls; i++ )
        {
            current_wall = wall[i].p3;
            if( (current_wall & pnorth) > 0 )
                ke.setWall(wall[i].p1 - 1, wall[i].p2 - 1, KarelEnvironment.Heading.NORTH);
            if( (current_wall & peast) > 0 )
                ke.setWall(wall[i].p1 - 1, wall[i].p2 - 1, KarelEnvironment.Heading.EAST);
            if( (current_wall & psouth) > 0 )
                ke.setWall(wall[i].p1 - 1, wall[i].p2 - 1, KarelEnvironment.Heading.SOUTH);
            if( (current_wall & pwest) > 0 )
                ke.setWall(wall[i].p1 - 1, wall[i].p2 - 1, KarelEnvironment.Heading.WEST);
        }
        for( int i = 0; i < beepers; i++ )
            ke.setBeepers(beeper[i].p1 - 1, beeper[i].p2 - 1, beeper[i].p3);
    }

    public static void write_world(String filename, KarelEnvironment ke )
        throws IOException
    {
        BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(filename));

        KarelWorldManager.KarelMDOToken.write_token(16715, 17746, 8268, bout);
        KarelWorldManager.KarelMDOToken.write_token(19791, 11849, 1, bout);

        // Karel World values
        int w = ke.numAvenidas(), h = ke.numCalles();
        KarelWorldManager.KarelMDOToken.write_token(w, h, ke.getKarel().getNumBeepers(), bout);
        KarelWorldManager.KarelMDOToken.write_token(ke.getKarel().getX() + 1, ke.getKarel().getY() + 1, KarelMDOToken.get_inverse_heading(ke.getKarel().getHeading()), bout);

        // Walls and beepers
        KarelEnvironment.Heading wallface[] = {KarelEnvironment.Heading.NORTH, KarelEnvironment.Heading.SOUTH,
                                               KarelEnvironment.Heading.EAST, KarelEnvironment.Heading.WEST};
        ArrayList<KarelMDOToken> wall    = new ArrayList<KarelMDOToken>();
        ArrayList<KarelMDOToken> beeper  = new ArrayList<KarelMDOToken>();
        int wall_mask = 0;
        for( int i = 0; i < w; i++ )
        {
            for( int j = 0; j < h; j++ )
            {
                wall_mask = 0;
                for( int k = 0; k < 4; k++ )
                    if( ke.isThereWall(i, j, wallface[k]) )
                        wall_mask = wall_mask | KarelMDOToken.get_wall_mask(wallface[k]);
                if( wall_mask > 0)
                    wall.add( new KarelMDOToken(i + 1, j + 1, wall_mask) );
                if( ke.isThereBeeper(i, j) )
                    beeper.add( new KarelMDOToken(i + 1, j + 1, ke.getBeepers(i, j)) );
            }
        }
        int walls = wall.size(), beepers = beeper.size();
        KarelWorldManager.KarelMDOToken.write_token(walls, beepers, 0, bout);
        KarelMDOToken kmt;
        for(int i = 0; i < walls; i++ )
        {
            kmt = wall.get(i);
            KarelWorldManager.KarelMDOToken.write_token(kmt.p1, kmt.p2, kmt.p3, bout);
        }
        for(int i = 0; i < beepers; i++ )
        {
            kmt = beeper.get(i);
            KarelWorldManager.KarelMDOToken.write_token(kmt.p1, kmt.p2, kmt.p3, bout);
        }
        bout.close();
    }

    public static class KarelMDOToken
    {
        public KarelMDOToken( )
        {
            
        }

        public KarelMDOToken( byte bytes[] )
        {
            if( bytes.length != 6 )
                return;
            this.build_token(bytes);
        }

        public KarelMDOToken( int __p1, int __p2, int __p3 )
        {
            this.p1 = __p1;
            this.p2 = __p2;
            this.p3 = __p3;
        }

        public static boolean read_next_token( KarelWorldManager.KarelMDOToken kmt, BufferedInputStream bin )
            throws IOException, KarelException
        {
            byte[] buffer  = new byte[6];
            if( bin.read(buffer) != 6 )
                throw new KarelException("El formato del mundo es incorrecto");
            kmt.build_token(buffer);
            return true;
        }

        public static void write_token( KarelWorldManager.KarelMDOToken kmt, BufferedOutputStream bout)
            throws IOException
        {
            kmt.build_bytes();
            bout.write(kmt.data);
        }

        public static void write_token( int __p1, int __p2, int __p3, BufferedOutputStream bout)
            throws IOException
        {
            KarelWorldManager.KarelMDOToken.write_token( new KarelMDOToken(__p1, __p2, __p3), bout);
        }
        
        public void build_token(byte bytes[])
        {
            this.p1 = this.build_int( bytes[0], bytes[1] );
            this.p2 = this.build_int( bytes[2], bytes[3] );
            this.p3 = this.build_int( bytes[4], bytes[5] );
        }

        private int build_int(byte b1, byte b2)
        {
            return this.to_int(b1) + 256 * this.to_int(b2);
        }

        private int to_int(byte b)
        {
            return b >= 0 ? b : 256 - Math.abs((int)(b));
        }

        public static KarelEnvironment.Heading get_heading(int h)
        {
            if( h == 2 )
                return KarelEnvironment.Heading.EAST;
            else if( h == 3 )
                return KarelEnvironment.Heading.SOUTH;
            else if( h == 4 )
                return KarelEnvironment.Heading.WEST;
            return KarelEnvironment.Heading.NORTH;
        }

        public static int get_inverse_heading(KarelEnvironment.Heading h)
        {
            if( h == KarelEnvironment.Heading.EAST )
                return 2;
            else if( h == KarelEnvironment.Heading.SOUTH )
                return 3;
            else if( h == KarelEnvironment.Heading.WEST )
                return 4;
            return 1;
        }

        public static int get_wall_mask(KarelEnvironment.Heading h)
        {
            if( h == KarelEnvironment.Heading.EAST )
                return 2;
            else if( h == KarelEnvironment.Heading.SOUTH )
                return 4;
            else if( h == KarelEnvironment.Heading.WEST )
                return 8;
            return 1;
        }

        private void build_bytes()
        {
            this.build_bytes(this.p1, this.p2, this.p3);
        }

        private void build_bytes( int __p1, int __p2, int __p3)
        {
            data[0] = (byte)(__p1 & 0xFF);
            data[1] = (byte)((__p1 & 0xFF00) >> 8 );
            data[2] = (byte)(__p2 & 0xFF);
            data[3] = (byte)((__p2 & 0xFF00) >> 8 );
            data[4] = (byte)(__p3 & 0xFF);
            data[5] = (byte)((__p3 & 0xFF00) >> 8 );
        }

        @Override public KarelMDOToken clone()
        {
            return new KarelMDOToken(this.p1, this.p2, this.p3);
        }

        public int p1 = -1, p2 = -1, p3 = -1;
        public byte data[] = new byte[6];
    }
}
