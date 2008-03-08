/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fi.kaimio.sane;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 *
 * @author harri
 */
public class SaneDeviceDescriptor extends Structure {
    
    SaneDeviceDescriptor( Pointer p ) {
        useMemory(p);
        read();
    }
    
    public SaneDeviceDescriptor() {}
    
    public String name;
    public String vendor;
    public String model;
    public String type;
}
