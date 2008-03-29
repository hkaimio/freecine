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

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.ByReference;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import java.nio.Buffer;

/**
 *
 * @author harri
 */
public interface Sane extends Library {
    Sane INSTANCE = (Sane) Native.loadLibrary("sane", Sane.class );

    
    int sane_init( IntByReference versionCode, PointerByReference authCallback );
    int sane_get_devices( PointerByReference deviceListPointer, boolean localOnly );
    
    int sane_open( String deviceName, PointerByReference handle );
    
    void sane_close( Pointer handle );
    
    /**
     Start scanning
     @param handle Handle to the opened device
     @return status code
     */
    int sane_start( Pointer handle );
    
    int sane_get_parameters( Pointer handle, ScanParameter param );
    
    int sane_read( Pointer handle, Buffer data, int max_length, IntByReference length );
    
    SaneOptionDescriptor sane_get_option_descriptor( Pointer handle, int n );    
    
    /*
     Overloaded sane_control_option propotypes to marshal parameters correctly
     */
    
    int sane_control_option( Pointer handle, int n, int action, ByReference value, IntByReference i );
    int sane_control_option( Pointer handle, int n, int action, String value, IntByReference i );
    int sane_control_option( Pointer handle, int n, int action, int[] value, IntByReference i );
}
