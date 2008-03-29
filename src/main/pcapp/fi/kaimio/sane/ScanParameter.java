/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fi.kaimio.sane;

import com.sun.jna.Structure;

/**
 *
 * @author harri
 */
public class ScanParameter extends Structure {
    
    public int format;
    public boolean lastFrame;
    public int bytesPerLine;
    public int pixelsPerLine;
    public int lines;
    public int depth;
    
    public FrameFormat getFormat( )  {
        return FrameFormat.get( format );
    }

    public boolean isLastFrame() {
        return lastFrame;
    }

    public int getBytesPerLine() {
        return bytesPerLine;
    }

    public int getPixelsPerLine() {
        return pixelsPerLine;
    }

    public int getLines() {
        return lines;
    }

    public int getDepth() {
        return depth;
    }

}
