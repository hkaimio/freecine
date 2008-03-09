/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fi.kaimio.sane;

/**
 *
 * @author harri
 */
public enum FrameFormat {
    SANE_FRAME_GRAY,
    SANE_FRAME_RGB,
    SANE_FRAME_RED,
    SANE_FRAME_GREEN,
    SANE_FRAME_BLUE;
        
    public static FrameFormat get( int n ) {
        return values()[n];
    }
        

}
