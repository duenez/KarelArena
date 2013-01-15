/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package application.karel;

import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author Kuko
 */
public class KarelKECEnvironment
{
    public static enum KECTokenType {
        INST, MOVE, TURNLEFT, PICK, PUT,  BEEPBAG, BEEPPOS, POSKAREL, ORIKAREL, WORLDBEEP };

    public KarelKECEnvironment()
    {
        KarelWorldManager.KarelMDOToken tmp = new KarelWorldManager.KarelMDOToken(0, 0, 0);
        this.setting.put(KarelKECEnvironment.KECTokenType.INST,      tmp.clone());
        this.setting.put(KarelKECEnvironment.KECTokenType.MOVE,      tmp.clone());
        this.setting.put(KarelKECEnvironment.KECTokenType.TURNLEFT,  tmp.clone());
        this.setting.put(KarelKECEnvironment.KECTokenType.PICK,      tmp.clone());
        this.setting.put(KarelKECEnvironment.KECTokenType.PUT,       tmp.clone());
        this.setting.put(KarelKECEnvironment.KECTokenType.BEEPBAG,   tmp.clone());
        this.setting.put(KarelKECEnvironment.KECTokenType.BEEPPOS,   tmp.clone());
        this.setting.put(KarelKECEnvironment.KECTokenType.POSKAREL,  tmp.clone());
        this.setting.put(KarelKECEnvironment.KECTokenType.ORIKAREL,  tmp.clone());
        this.setting.put(KarelKECEnvironment.KECTokenType.WORLDBEEP, tmp.clone());
    }

    public boolean is_set(KarelKECEnvironment.KECTokenType sett)
    {
        return this.setting.get(sett).p1 == 1;
    }

    public boolean is_set(int a, int c)
    {
        for( int i = 0, n = this.beeper.length; i < n; i++ )
            if( this.beeper[i].p1 == a && this.beeper[i].p2 == c )
                return true;
        return false;
    }

    public boolean is_set(int a, int c, int beep)
    {
        for( int i = 0, n = this.beeper.length; i < n; i++ )
            if( this.beeper[i].p1 == a && this.beeper[i].p2 == c && this.beeper[i].p3 == beep )
                return true;
        return false;
    }

    public void set(KarelKECEnvironment.KECTokenType sett, KarelWorldManager.KarelMDOToken kmt)
    {
        this.setting.get(sett).p1 = kmt.p1;
        this.setting.get(sett).p2 = kmt.p2;
        this.setting.get(sett).p3 = kmt.p3;
    }

    public void add_beepers( KarelWorldManager.KarelMDOToken __beep[])
    {
        this.beeper = __beep.clone();
    }

    public KarelWorldManager.KarelMDOToken get(KarelKECEnvironment.KECTokenType sett)
    {
        return this.setting.get(sett);
    }

    public int get_beeper(int a, int c)
    {
        for( int i = 0, n = beeper.length; i < n; i++ )
            if( beeper[i].p1 == a && beeper[i].p2 == c )
                return beeper[i].p3;
        return -1;
    }

    public int get_beeper(int i)
    {
        return beeper[i].p3;
    }


    private Map<KECTokenType, KarelWorldManager.KarelMDOToken > setting = new TreeMap<KECTokenType, KarelWorldManager.KarelMDOToken>();
    public KarelWorldManager.KarelMDOToken beeper[] = null;
}
