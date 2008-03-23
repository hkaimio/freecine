/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fi.kaimio.moviescan;

import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.List;
import javax.xml.transform.sax.TransformerHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 Scene in this context is defined as a continuos part of film that is treated 
 in the same way in conversion.
 */
public class Scene implements FrameRangeChangeListener {
    
    /**
     Frames in this scene
     */
    List<FrameRange> frames = new ArrayList<FrameRange>();
    
    /**
     Create a new scene
     */
    public Scene() {}
    
    /**
     Add frames from a scann to end of this scene
     @param strip The scan strip
     @param firstFrame First frame to add
     @param frameCount Nubmer of frames to add
     */
    public void addFrames( ScanStrip strip, int firstFrame, int frameCount ) {
        FrameRange lastRange = getLastRange();
        FrameRange newRange = new FrameRange( strip, firstFrame, getFrameCount(), frameCount );
        frames.add( newRange );
        newRange.addFrameRangeChangeListener( this );
    }
    
    /**
     Get nubmer of frames in this scene
     @return
     */
    public int getFrameCount() {
        FrameRange lastRange = getLastRange();
        return lastRange != null ? lastRange.getSceneFirst() + lastRange.getFrameCount() : 0;
    }

    /**
     Set the crop bounds for this scene
     @param centerX Center x coodrinate
     @param centerY Center y coordinate
     @param rot Rotation in degrees clockwise
     @param width Width of the crop area
     */
    public void setCropBounds( double centerX, double centerY, double rot, double width ) {
        
    }
    
    /**
     Get a frame in this scene
     @param n Order number of the frame
     @return Image of the frame
     */
    public RenderedImage getFrame( int n ) {
        return null;
    }
    

    
    /**
     Get a part of the scene
     @param startFrame start frame of the new scene
     @param frameCount Nubmer of frames in the new scene
     @return New scene
     */
    public Scene split( int startFrame, int frameCount ) {
        Scene newScene = new Scene();
        int frame = 0;
        for ( FrameRange r : frames ) {
            if ( r.getSceneFirst()+r.getFrameCount() >= startFrame ) {
                int rangeStart = Math.max( 0, startFrame - r.getSceneFirst() );
                int numFrames = Math.min( r.getFrameCount()-rangeStart, frameCount-frame );
                newScene.addFrames(r.getStrip(), r.getStripFirst()+rangeStart, numFrames  );
                frame += numFrames;
                if ( frame == frameCount ) {
                    break;
                }
            }
        }
        return newScene;
    }
    
    /**
     Append another scene to the end of this scene-
     @param s The scene to append
     */
    public void append( Scene s ) {
        for ( FrameRange r : s.frames ) {
            addFrames(r.getStrip(),r.getStripFirst(),r.getFrameCount() );
        }
    }

    /**
     Called when a {@link FrameRange} of this scene has been changed, probably 
     due to disabling a frame that belongs to some of those.
     @param arg0
     */
    public void frameRangeChanged( FrameRange arg0 ) {
        int startFrame = 0;
        for ( FrameRange r : frames ) {
            r.setSceneFirst( startFrame );
            startFrame += r.getActiveFrameCount();
        }
    }

    
    private FrameRange getLastRange() {
        int rangeCount = frames.size();
        return rangeCount > 0 ? frames.get( frames.size() -1 ) : null;
    }
    
    /**
     Get the scan strip in which a given frame belongs
     @param frame the frame number
     @return ScanStrip that contains the frame
     */
    public ScanStrip getFrameStrip( int frame ) {
        FrameRange r = getRangeForFrame(frame);
        return r != null ? r.getStrip() : null;
    }
    
    /**
     Map the scene frame number to the nubmer of the frame in given strip
     @param frame
     @return
     */
    public int getStripFrameNum( int frame ) {
        FrameRange r = getRangeForFrame(frame);
        return r != null ? r.getStripFirstActive() + frame - r.getSceneFirst() : -1;
    }
    
    /**
     Find the FrameRange that contains given scene frame
     @param frame
     @return
     */
    private FrameRange getRangeForFrame( int frame ) {
        int low = 0;
        int high = frames.size();
        FrameRange r = null;
        while ( low < high ) {
            int n = (low + high) /2;
            r = frames.get( n );
            if ( r.getSceneFirst() <= frame && r.getSceneFirst()+r.getFrameCount() > frame ) {
                break;
            }
            if ( frame > r.getSceneFirst() ) {
                low = n;
            } else {
                high = n;
            }
        }
        return r;
    }
    
    public void writeXml( TransformerHandler hd ) throws SAXException {
        AttributesImpl atts = new AttributesImpl();
        hd.startElement("", "", "scene", atts);
        hd.startElement("", "", "frames", atts);
        for ( FrameRange r : frames ) {
            atts.clear();
            atts.addAttribute("", "", "strip", "CDATA", r.getStrip().getName() );
            atts.addAttribute("", "", "start", "CDATA", Integer.toString(r.getStripFirst() ) );
            atts.addAttribute("", "", "count", "CDATA", Integer.toString(r.getFrameCount() ) );
            hd.startElement("", "", "framerange", atts);
            hd.endElement("", "", "framerange");
        }
        hd.endElement("", "", "frames" );
        hd.endElement("", "", "scene" );

    }

}
