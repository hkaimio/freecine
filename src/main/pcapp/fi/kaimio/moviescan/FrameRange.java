package fi.kaimio.moviescan;

import java.util.HashSet;
import java.util.Set;


/**
 Describes a range of frames used from a single {@link ScanStrip} to a 
 {@link Scene}. 
 <p>
 FrameRnage includes a set of subsequent frames from a scan strip. The actual 
 frames (and number of them) depends on whether some frames in the strip are 
 marked unusable - these frames are not included. When frame that belongs to this 
 range is marked unusable/usable the associated scene is notified and number of 
 frames in that scene is changed accordingly.
 @author harri
 */
class FrameRange implements ScanStripListener {

    private ScanStrip strip;
    /**
    Number of frames in this range
     */
    private int frameCount;
    /**
    First frame from the strip that is is included in the range
     */
    private int stripFirst;
    
    /**
     First frame that is included in the range and usable in the strip
     */
    private int stripFirstActive;
    
    /**
     Last active that is included in the range and usable in the strip
     */
    private int stripLastActive;
    
    /**
    Number of the first frame in the scene
     */
    private int sceneFirst;

    public FrameRange( ScanStrip strip, int stripFirst, int sceneFirst, int frameCount ) {
        super();
        this.strip = strip;
        this.stripFirst = stripFirst;
        this.sceneFirst = sceneFirst;
        this.frameCount = frameCount;
        stripFirstActive = Math.max(stripFirst, strip.getFirstUsable() );
        stripLastActive = Math.min( stripFirst+frameCount-1, strip.getLastUsable() );
        strip.addScanStripListener( this );
    }

    Set<FrameRangeChangeListener> listeners = 
            new HashSet<FrameRangeChangeListener>();
    
    /**
     Add a new listener that will be notified of changes
     @param l The new listener
     */
    public void addFrameRangeChangeListener( FrameRangeChangeListener l ) {
        listeners.add( l );
    }
    
    /**
    Remove a listener 
     @param l The listener to be removed
     */
    public void removeFrameRangeChangeListener( FrameRangeChangeListener l ) {
        listeners.remove( l );
    }    
    
    /**
     Notify listeners that this frame range has been changed
     */
    private void notifyListeners() {
        for ( FrameRangeChangeListener l : listeners ) {
            l.frameRangeChanged( this );
        }
    }
    
    
    /**
     Get the number of frames intended to this range.
     @return
     */
    public int getFrameCount() {
        return frameCount;
    }
    
    public int getActiveFrameCount() {
        int f1 = Math.max( stripFirst, strip.getFirstUsable() );
        int f2 = Math.min( stripFirst+frameCount, strip.getLastUsable()+1 );
        return f2-f1;
    }

    public void setFrameCount( int frameCount ) {
        this.frameCount = frameCount;
    }

    public ScanStrip getStrip() {
        return strip;
    }

    public int getStripFirst() {
        return stripFirst;
    }

    public int getStripFirstActive() {
        return stripFirstActive;
    }
    
    public int getSceneFirst() {
        return sceneFirst;
    }

    public void setSceneFirst( int sceneFirst ) {
        this.sceneFirst = sceneFirst;
    }

    /**
     Called when the scan strip has changed. Check whether the change affects 
     this renge and notify listeners if it does.
     @param s
     */
    public void scanStripChanged( ScanStrip s ) {
        int fa = Math.max( stripFirst, strip.getFirstUsable() );
        int la = Math.min( stripFirst+frameCount-1, strip.getLastUsable() );
        if ( fa != stripFirstActive || la != stripLastActive ) {
            stripFirstActive = fa;
            stripLastActive = la;
            notifyListeners();
        }
        
    }

}
