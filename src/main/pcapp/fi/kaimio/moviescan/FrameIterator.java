/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fi.kaimio.moviescan;

import java.awt.image.RenderedImage;
import java.util.Iterator;

/**
FrameIterator is used to access frames of a project/scene sequentically
 */
public class FrameIterator implements Iterator<FrameDescriptor> {

    /**
     Project this iterator is associated with
     */
    private Project project;
    
    /**
     Current scene we are iterating
     */
    Scene currentScene;
    
    /**
     Current strip we are iterating
     */
    ScanStrip currentStrip;
    
    
    int sceneFrame = -1;
    
    /**
     Number of the frame that should be returned with the next() method. If 
     negative, the frame currentFrame+1 is returned.
     */
    private int nextFrameNum = -1;

    /**
     Create a new iterator. This should not be called directly, use 
     project.iterator() instead
     @param project
     */
     
    FrameIterator( Project project ) {
        this.project = project;
        currentScene = project.getScene();
        currentStrip = null;
    }

    /**
     Ís there a frame after the current frame?
     @return
     */
    public boolean hasNext() {
        return (sceneFrame+1) < currentScene.getFrameCount();
    }
    
    /**
     Is there a frame before the current frame?
     @return
     */
    public boolean hasPrev() {
        return sceneFrame > 0;
    }

    /**
     Move iterator to the next frame
     @return Next frame
     */
    public FrameDescriptor next() {
        if ( nextFrameNum >= 0 ) {
            sceneFrame = nextFrameNum;
            nextFrameNum = -1;
        } else {
            sceneFrame++;
        }
        ScanStrip nextStrip = currentScene.getFrameStrip( sceneFrame );
        if ( nextStrip != currentStrip ) {
            if ( currentStrip != null ) {
                currentStrip.releaseStripImage();
            }
            nextStrip.reserveStripImage();
            currentStrip = nextStrip;
        }
        FrameDescriptor d = new FrameDescriptor( currentStrip, currentScene.getStripFrameNum( sceneFrame ) );
        System.err.printf( "Frame %d, strip %s[%d]\n", sceneFrame, d.getStrip().getName(), d.getStripFrameNum() );
        return d;
    }

    /**
     Move iterator to the previous frame
     @return Previous frame
     */
    public FrameDescriptor prev() {
        
        sceneFrame--;

        ScanStrip nextStrip = currentScene.getFrameStrip( sceneFrame );
        if ( nextStrip != currentStrip ) {
            if ( currentStrip != null ) {
                currentStrip.releaseStripImage();
            }
            nextStrip.reserveStripImage();
            currentStrip = nextStrip;
        }
        FrameDescriptor d = new FrameDescriptor( currentStrip, currentScene.getStripFrameNum( sceneFrame ) );
        return d;
    }

    public void remove() {
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    public int getCurrentFrameNum() {
        return sceneFrame;
    }
    
    /**
     Move the iterator so that calling next() will return a certain frame
     @param n 
     */
    public void setNextFrameNum( int n ) {
        nextFrameNum = n;
    }

}
