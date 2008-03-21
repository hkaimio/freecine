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
public class FrameIterator implements Iterator<RenderedImage> {

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
     Create a new iterator. This should not be called directly, use 
     project.iterator() instead
     @param project
     */
     
    FrameIterator( Project project ) {
        this.project = project;
        currentScene = project.getScene();
        currentStrip = null;
    }

    
    public boolean hasNext() {
        return (sceneFrame+1) < currentScene.getFrameCount();
    }

    public RenderedImage next() {
        sceneFrame++;
        ScanStrip nextStrip = currentScene.getFrameStrip( sceneFrame );
        if ( nextStrip != currentStrip ) {
            if ( currentStrip != null ) {
                currentStrip.releaseStripImage();
            }
            nextStrip.reserveStripImage();
            currentStrip = nextStrip;
        }
        return currentStrip.getFrame( currentScene.getStripFrameNum( sceneFrame ) );
    }

    public void remove() {
        throw new UnsupportedOperationException( "Not supported yet." );
    }

}
