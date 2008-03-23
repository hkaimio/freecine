/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fi.kaimio.moviescan;

import java.awt.image.RenderedImage;

/**
 FrameDescriptor collects all information about a single frame, including 
 information about the source of the frame and needed corrections
 */
public class FrameDescriptor {

    private ScanStrip strip;
    private int frameNum;

    FrameDescriptor( ScanStrip strip, int stripFrameNum ) {
        this.strip = strip;
        this.frameNum = stripFrameNum;
    }

    /**
     Get the strip from which this frame is from
     @return
     */
    public ScanStrip getStrip() {
        return strip;
    }

    /**
     Get the order number of the frame in the strip
     @return
     */
    public int getStripFrameNum() {
        return frameNum;
    }
    
    /**
     Get image of the frame in question
     @return RenderedImage of the frame
     */
    public RenderedImage getFrame() {
        return strip.getFrame( frameNum );
    }
            
}
