/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fi.kaimio.moviescan;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 Project object keeps strck of all inforation related the project (i.e. 
 information about scan strips and scenes
 */
public class Project {

    /**
     Home directory for the project
     */
    private File dir;
    
    private Map<String, ScanStrip> loadedStrips = new HashMap<String, ScanStrip>();
    
    /**
     Creates a new project
     @param dir The directory of the project
     */
    public Project( File dir ) {
        this.dir = dir;
    }
    
    /**
     Get the scan strip with given name
     @param name Name of the strip
     @return The strip with given name or <code>null</code> if no such strip 
     exists
     */
    public ScanStrip getScanStrip( String name ) {
        ScanStrip ret = null;
        if ( loadedStrips.containsKey( name ) ) {
            ret = loadedStrips.get( name );
        } else {
            ret = new ScanStrip( );
            loadedStrips.put(name, ret);
            ret.setName(name);
        }
        return ret;
    }

}
