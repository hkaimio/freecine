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

/**
 *
 * @author harri
 */
public class PointCluster {
    int centroidX;
    int centroidY;
    int sumX = 0;
    int sumY = 0;
    int pointCount = 0;

    public void addPoint( int x, int y ) {
        sumX += x;
        sumY += y;
        pointCount++;
        centroidX = sumX / pointCount;
        centroidY = sumY / pointCount;
    }
    
    public int getCentroidX() {
        return centroidX;
    }
    
    public int getCentroidY() {
        return centroidY;
    }
    
    public int getPointCount() {
        return pointCount;
    }
    
    public int getSqDist( int x, int y ) {
        int dx = centroidX - x;
        int dy = centroidY - y;
        return dx*dx+dy*dy;
    }
}
