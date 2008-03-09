/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fi.kaimio.moviescan;

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
