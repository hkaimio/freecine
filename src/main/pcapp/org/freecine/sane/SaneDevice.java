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

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author harri
 */
public class SaneDevice {

    static Sane sane = Sane.INSTANCE;
    Map<String, Integer> optionIds = null;
    SaneOptionDescriptor[] optionDesc = null;
    Pointer deviceHandle;
    boolean isOpen = false;
    
    public SaneDevice( String name ) throws SaneException {
        PointerByReference handleRef = new PointerByReference();
        int status = sane.sane_open( name, handleRef );
        isOpen = true;
        if ( status == 0 ) {
            deviceHandle = handleRef.getValue();
        } else {
            throw new SaneException( "Error opening device " + name );
        }
    }

    private void initOptions() {
        IntByReference optCountRef = new IntByReference();
        sane.sane_control_option( deviceHandle, 0, 0, optCountRef, null );
        int optCount = optCountRef.getValue();


        optionIds = new HashMap<String, Integer>();
        optionDesc = new SaneOptionDescriptor[optCount];
        for ( int n = 0; n < optCount; n++ ) {
            SaneOptionDescriptor od = sane.sane_get_option_descriptor( deviceHandle, n );
            optionIds.put( od.name, n );
            optionDesc[n] = od;
        }
    }
    
    public SaneOptionDescriptor getOptionDesc( String option ) {
        if ( optionIds == null ) {
            initOptions();
        }
        if ( optionIds.containsKey(option)) {
            return optionDesc[optionIds.get(option) ];
        }
        return null;
    }
    
    /**
     Set value of an option
     
     @param option Name of the option
     @param valueNew value for the option
     @throws fi.kaimio.moviescan.SaneException If the option is not known, the
     given value cannot be used or some other error happens.
     */
    public void setOption( String option, String value ) throws SaneException  {
        if ( optionIds == null ) {
            initOptions();
        }
        int id = -1;
        if ( optionIds.containsKey(option ) ) {
            id = optionIds.get( option );
            SaneOptionDescriptor desc = optionDesc[id];
            if ( desc.getType() == OptionType.SANE_TYPE_STRING ) {
                if ( desc.size >= value.length() ) {
                    int status = sane.sane_control_option(deviceHandle, id, 1, value, null );
                    if ( status != 0 ) {
                        throw new SaneException( "Error setting option " + option + " to " + value );
                    }
                } else {
                    throw new SaneException( "Length of option " + option + " cannot exceed " + desc.size );
                }
            } else {
                throw new SaneException( "Option " + option + "  is not a String" );
            }
        } else {
            throw new SaneException( "No option named " + option );
        }
    }
    
    public void setOption( String option, int[] value ) throws SaneException {
        if ( optionIds == null ) {
            initOptions();
        }
        int id = -1;
        if ( optionIds.containsKey(option ) ) {
            id = optionIds.get( option );
            SaneOptionDescriptor desc = optionDesc[id];
            if ( desc.getType() == OptionType.SANE_TYPE_INT ) {
                if ( desc.size == value.length * 4 ) {
                    int status = sane.sane_control_option(deviceHandle, id, 1, value, null );
                    if ( status != 0 ) {
                        throw new SaneException( "Error setting option " + option );
                    }
                } else {
                    throw new SaneException( "Length of option " + option + " must be " + desc.size/4 );
                }
            } else {
                throw new SaneException( "Option " + option + "  is not an integer" );
            }
        } else {
            throw new SaneException( "No option named " + option );
        }
        
    }
    
    public void setOption( String option, int value ) throws SaneException {
        int[] a = {value};
        setOption( option, a );
    }
    
    public void setOption( String option, FixedPointNumber[] value ) throws SaneException {
        if ( optionIds == null ) {
            initOptions();
        }
        int id = -1;
        if ( optionIds.containsKey(option ) ) {
            id = optionIds.get( option );
            SaneOptionDescriptor desc = optionDesc[id];
            if ( desc.getType() == OptionType.SANE_TYPE_FIXED ) {
                int[] v = new int[value.length];
                for ( int n = 0 ; n < value.length; n++ ) {
                    v[n] = value[n].getVal();
                }
                if ( desc.size == value.length * 4 ) {
                    int status = sane.sane_control_option(deviceHandle, id, 1, v, null );
                    if ( status != 0 ) {
                        throw new SaneException( "Error setting option " + option );
                    }
                } else {
                    throw new SaneException( "Length of option " + option + " must be " + desc.size/4 );
                }
            } else {
                throw new SaneException( "Option " + option + "  is not an integer" );
            }
        } else {
            throw new SaneException( "No option named " + option );
        }
        
    }
    
    public void setOption( String option, FixedPointNumber value ) throws SaneException {
        FixedPointNumber[] a = {value};
        setOption( option, a );
    }    
    
    public void startScan() throws SaneException {
        int status = sane.sane_start(deviceHandle);
        if ( status != 0 ) {
            throw new SaneException( "Error while starting scanning" );
        }
    }
    
    public ScanParameter getScanParameter() throws SaneException {
        ScanParameter param = new ScanParameter();
        int status = sane.sane_get_parameters(deviceHandle, param);
        if ( status != 0 ) {
            throw new SaneException( "Error while getting scan parameter" );            
        }
        return param;
    }
    
    public void read( byte[] data ) throws SaneException {
        int pos = 0;
        byte[] arr = new byte[32752];
        Buffer b = ByteBuffer.wrap( arr, 0, arr.length );
        while ( pos < data.length ) {
            int readSize = Math.min( 32752, data.length - pos );
            
            IntByReference bytesRead = new IntByReference();
            int status = sane.sane_read(deviceHandle, b, readSize, bytesRead);
            if ( status != 0 ) {
                throw new SaneException( "Error reading scan data" );
            }
            for ( int n = 0; n < bytesRead.getValue(); n++ ) {
                data[pos+n] = arr[n];
            }
            pos += bytesRead.getValue();            
        }
    }

    /**
     Read data from scanner as 16 bit samples. Reading continues until the give 
     array is filled.
     
     @param data Array in which the samples are stored
     @throws fi.kaimio.sane.SaneException If an error occurs during reading
     */
    public void read( short[] data ) throws SaneException {
        read( data, data.length );
    }
    
    /**
     Read data from scanner as 16 bit samples. 
     
     @param data Array in which the samples are stored
     @param sampelsToRead Number of samples to read
     @throws fi.kaimio.sane.SaneException If an error occurs during reading
     */
    public void read( short[] data, int samplesToRead ) throws SaneException {
        int pos = 0;
        short[] arr = new short[16636];
        Buffer b = ShortBuffer.wrap( arr, 0, arr.length );
        while ( pos < samplesToRead ) {
            int readSize = Math.min( 32752, 2 * (data.length - pos) );
            
            IntByReference bytesRead = new IntByReference();
            int status = sane.sane_read(deviceHandle, b, readSize, bytesRead);
            if ( status != 0 ) {
                throw new SaneException( "Error reading scan data" );
            }
            for ( int n = 0; n < bytesRead.getValue()/2; n++ ) {
                data[pos+n] = arr[n];
            }
            pos += bytesRead.getValue()/2;            
        }
    }

    
    public void close() {
        sane.sane_close(deviceHandle);
        isOpen = false;
    }
    
    @Override
    protected void finalize() throws Throwable {
        if ( isOpen ) {
            close();
        }
        super.finalize();
    }
}
