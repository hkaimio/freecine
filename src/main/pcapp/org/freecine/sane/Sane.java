/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
