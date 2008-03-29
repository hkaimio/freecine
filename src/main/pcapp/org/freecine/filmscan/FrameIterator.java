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

package org.freecine.filmscan;

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
