/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fi.kaimio.sane;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author harri
 */
public class SaneOptionDescriptor extends Structure {
    
    public static class SaneRange extends Structure {
        public int min;
        public int max;
        public int quant;
        
        SaneRange( Pointer p ) {
            useMemory( p );
            read();
        }
    };
    
    public String name;
    public String title;
    public String desc;
    public int type;
    public int unit;
    public int size;
    public int cap;
    
    public int constraintType;
    
    public Pointer constraints;

    public SaneOptionDescriptor() {}
    
    public ConstraintType getConstraintType() {
        return ConstraintType.values()[constraintType];
    }
    
    
    public OptionType getType() {
        return OptionType.get( type );
    }
    
    public Object getConstraints() {
        if ( constraints == null ) {
            return null;
        }
        Object ret = null;
        switch ( getConstraintType() ) {
            case SANE_CONSTRAINT_RANGE:
                ret = new SaneRange( constraints );
                break;
            case SANE_CONSTRAINT_STRING_LIST:
                 {
                    int offset = 0;
                    List<String> retStr = new ArrayList<String>();
                    Pointer strPtr;
                    while ( (strPtr = constraints.getPointer(offset)) != null ) {
                        retStr.add( strPtr.getString(0) );
                        offset += Pointer.SIZE;
                    }
                    ret = retStr;
                }
                break;
            case SANE_CONSTRAINT_WORD_LIST:
                 {
                    int count = constraints.getInt( 0 );                    
                    int[] retInt = constraints.getIntArray(4, count);
                    ret = retInt;
                }
                break;
            default:
                // No constraints set, return null
        }
        return ret;
    }
    
    SaneOptionDescriptor( Pointer p ) {
        useMemory(p);
        read();
//        switch ( constraintType ) {
//            case 1 : // CONSTRAINT_RANGE
//                constraints.setType(SaneRange.class);
//                
//        }
    }
}
