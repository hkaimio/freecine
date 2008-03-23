/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fi.kaimio.moviescan;

import java.util.EventListener;

/**
 Event listener for receiving notifications to change in a {@link FrameRange}
 */
interface FrameRangeChangeListener extends EventListener {
    /**
     Called to notify that a frame range has been changed
     @param r The changed range
     */
    void frameRangeChanged( FrameRange r );

}
