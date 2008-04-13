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

import static org.testng.AssertJUnit.*;
import org.testng.annotations.Test;

/**
 *
 * @author harri
 */
public class TestFixedPointNumber {
    
    @Test
    public void testConversion() {
        FixedPointNumber n1 = FixedPointNumber.valueOf( 30 );
        assertEquals( n1, new FixedPointNumber( 30 << 16 ) );              
    }
    
    private int signum( int n ) {
        if ( n < 0 ) return -1;
        if ( n == 0 ) return 0;
        return 1;
    }
    
    @Test
    public void testCompare() {
        FixedPointNumber[] arr = {
            new FixedPointNumber( Integer.MIN_VALUE ),
            new FixedPointNumber( Integer.MIN_VALUE + 1 ),
            new FixedPointNumber( 0 ),
            new FixedPointNumber( 1 ),
            new FixedPointNumber( Integer.MAX_VALUE - 1 ),
            new FixedPointNumber( Integer.MAX_VALUE )
        };
        
        for ( int i = 0 ; i < arr.length ; i++ ) {
            for ( int j = 0 ; j < arr.length ; j++ ) {
                assertEquals( "" + arr[i] + " < " + arr[j] + " don't match", signum( i-j ), signum( arr[i].compareTo( arr[j] ) ) );
            }
        }
    }

}
