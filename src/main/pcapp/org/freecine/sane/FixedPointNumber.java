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

/**
 Fixed point number used when setting parameters for Sane.
 
 Sane uses 32 bit fixed point number (with 16 bit integer, 16 bit fractional 
 part for many parameters. This class provides conversions and basic arithmetic 
 operations for those.
 
 */
public class FixedPointNumber implements Comparable {
    
    final static int SANE_FIXED_SCALE_SHIFT = 16;
 
    /**
     Value of the number, shifted left SANE_FIXED_SCALE_SHIFT bits
     */
    private int val;
    
    /**
     Creates a new fixed point number from the SANE internal representation of it
     @param fpval
     */
    public FixedPointNumber( int fpval ) {
        val = fpval;
    }
    
    /**
     Get the representation that Sane uses for this number
     @return
     */
    public int getVal() {
        return val;
    }
    
    /**
     Test for equality
     @param o The object to test with
     @return true if this object and o are equal, false otherwise
     */
    @Override
    public boolean equals( Object o ) {
        if ( o == this ) {
            return true;
        }
        if ( !(o instanceof FixedPointNumber) ) {
            return false;
        }
        FixedPointNumber f = (FixedPointNumber) o;
        return f.val == val;
    }

    /**
     Calculate hash code for this object
     @return
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + this.val;
        return hash;
    }
    
    /**
     Compare with another FixedPointNumber
     @param o
     @return
     */
    public int compareTo( Object o ) {
        FixedPointNumber f = (FixedPointNumber) o;
        if ( val > f.val ) {
            return 1;
        }
        if ( val < f.val ) {
            return -1;
        }
        return 0;
    }
    
    /**
     Add two fixed point numbers
     @param f The number to add to this.
     @return FixedPointNumber with value this+f
     */     
    public FixedPointNumber add( FixedPointNumber f ) {
        return new FixedPointNumber( this.val + f.val );
    }
    
    /**
     Subtract two fixed point numbers
     @param f The number to subtract from this.
     @return FixedPointNumber with value this-f
     */     
    public FixedPointNumber subtract( FixedPointNumber f ) {
        return new FixedPointNumber( this.val - f.val );
    }
    
    /**
     Convert this fixed point nubmer to double
     @return double with value that is as close as possible to value of this.
     */
    public double toDouble() {
        return (double)val / (double)( 1 << SANE_FIXED_SCALE_SHIFT );
    }
    
    /**
     Convert the number to string
     @return
     */
    @Override
    public String toString() {
        int intPart = val >> SANE_FIXED_SCALE_SHIFT;
        int fracPart = val & ( (1 << SANE_FIXED_SCALE_SHIFT) -1);
        String str = "" + intPart;
        if ( fracPart != 0 ) {
            str += " " + fracPart + "/" + (1 << SANE_FIXED_SCALE_SHIFT );
        }
        return str;
    }
    
    /**
     Create a new fixed point number whose value is close to given double 
     precision number
     @param v
     @return
     */
    public static FixedPointNumber valueOf( double v ) {
        double v2 = v * (double)(1<<SANE_FIXED_SCALE_SHIFT);
        return new FixedPointNumber( (int)v2 );        
    }
}
