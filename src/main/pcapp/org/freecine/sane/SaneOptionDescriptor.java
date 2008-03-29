/*
Copyright (C) 2008 Harri Kaimio
 
This file is part of Freecine
 
Freecine is free software; you can redistribute it and/or modify it 
under the terms of the GNU General Public License as published by the Free 
Software Foundation; either version 3 of the License, or (at your option) 
any later version.
 
This program is distributed in the hope that it will be useful, but WITHOUT 
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
You should have received a copy of the GNU General Public License along with 
this program; if not, see <http://www.gnu.org/licenses>.
 
Additional permission under GNU GPL version 3 section 7
 
If you modify this Program, or any covered work, by linking or combining it 
with Java Advanced Imaging (or a modified version of that library), containing 
parts covered by the terms of Java Distribution License, or leJOS, containing 
parts covered by the terms of Mozilla Public License, the licensors of this 
Program grant you additional permission to convey the resulting work. 
 */

package org.freecine.sane;

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
