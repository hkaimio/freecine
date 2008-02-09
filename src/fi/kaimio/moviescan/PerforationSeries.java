/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fi.kaimio.moviescan;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author harri
 */
class PerforationSeries {
    // Y coordinate of last performation
    int lastY = -1;
    // X coordinate of last performation
    int lastX = -1;
    // X coordinate of the 2nd last performation
    int lastX2 = -1;
    // Number of performations missing from the series
    int missCount = 0;
    // Maximum second derivative of X coordinate
    int maxDdx = 0;
    
    /**
     Assumed distance between perforation points in pixels
     */
    static private int PERF_DISTANCE = 800;
    static private int MIN_PERFORATION_COUNT = 30;
    
    /**
     Maximum tolerance for variance in perforation Y coordinate
     */
    static private int Y_TOLERANCE = 20;
    
    /**
     Tolerance how much real film dimensions are allowed to deviate
     */
    static private double Y_TOL = 0.025;
    
    List <Perforation> perforations = 
            new ArrayList<Perforation>();
    
    public boolean addIfFits( Perforation p ) {
        boolean isFit = false;
        if ( perforations.size() == 0 ) {
            isFit = true;
            perforations.add( p );
            p.series = this;
            missCount = p.y / PERF_DISTANCE;
            lastY = p.y;
            lastX = p.x;
        } else {
            // This was not the first point
            int dy = p.y - lastY;
            
            // How many frames are between last perforation and this one?
            int numFrames = (int) Math.round( (double) dy / PERF_DISTANCE );
            int devPixels = Math.abs( dy - PERF_DISTANCE * numFrames );
            double devRel = (double) devPixels / (PERF_DISTANCE * numFrames );
            int res = dy % PERF_DISTANCE;
            if ( numFrames > 0 && ( devRel < Y_TOL ) ) {
                isFit = true;
                perforations.add( p );
                p.series = this;
                missCount += numFrames-1;
                lastY = p.y;
                lastX2 = lastX;
                lastX = p.x;
                if ( lastX2 > 0 ) {
                    int ddx = Math.abs( lastX2 + p.x - 2 * lastX );
                    maxDdx = Math.max( ddx, maxDdx );
                }
            }
        }
        
        return isFit;
        
    }
    
    public int getQuality() {
        
        int missesAtEnd = 0;
        if ( lastY < PERF_DISTANCE * MIN_PERFORATION_COUNT ) {
            missesAtEnd = (PERF_DISTANCE*MIN_PERFORATION_COUNT - lastY) / PERF_DISTANCE;
        }
        int quality =  maxDdx + 10 * (missCount+missesAtEnd);
        System.out.println( "  maxDdx      = " + maxDdx );
        System.out.println( "  missCount   = " + missCount );
        System.out.println( "  missesAtEnd = " + missesAtEnd );
        System.out.println( "  quality     = " + quality );
        return quality;
    }
    
    public List<Perforation> getPerforations() {
        List<Perforation> ret = new ArrayList<Perforation>();
        Perforation lastPerf = null;
        for ( Perforation p : perforations ) {
            if ( lastPerf != null ) {
                if ( p.y - lastPerf.y > PERF_DISTANCE+Y_TOLERANCE ) {
                    // Perforation is missing here, add new ones at constant distance
                    int addCount = ( p.y - lastPerf.y + 2 * Y_TOLERANCE ) / PERF_DISTANCE;
                    int dy = (p.y-lastPerf.y ) / addCount;
                    int dx = (p.x-lastPerf.x ) / addCount;
                    for ( int n = 1 ; n < addCount ; n++ ) {
                        Perforation newP = new Perforation();
                        newP.x = lastPerf.x + n * dx;
                        newP.y = lastPerf.y + n * dy;
                        newP.series = this;
                        ret.add(newP);
                    }
                }
            }
            ret.add( p );
        }
        return ret;
    }
}
