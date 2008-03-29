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

package org.freecine.sane;

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
