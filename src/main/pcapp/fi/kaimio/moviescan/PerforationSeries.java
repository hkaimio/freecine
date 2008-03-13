/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fi.kaimio.moviescan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 Series of film (possible) perforations found from a scanned film strip. This 
 class includes methods of estimating quality of the series candidate as well as 
 for inter/extrapolating perforations missing from the series.
 */
class PerforationSeries {
    // Y coordinate of last performation added to the series
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
     TODO: This is now defined in several places
     */
    static private int PERF_DISTANCE = 800;
    
    /**
     Minimum number of perforations expected in a scan- if it is lower then the 
     quality function is decreased.
     TODO: This should come directly from the scan strip.
     */
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
    
    /**
     Add a perforation to the series if it fits. Perforation is considered a fit 
     if it is at the right distance (multiple of the distance between perforations)
     from the previous one.
     
     @param p The perforation that may be added
     @return true if the perforation fits, false otherwise.
     */
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
    
    /**
     Get a quality (actually error) function measuring the "quality" of the 
     series. The quality function is heuristic, based mostly on
     <ul>
     <li>Number of perforations missing from the series (in beginning, middle or 
     at the end</li>
     <li>Maximum second derivate of the perforation coordinates</li>
     </ul>
     @return The quality of the series, smaller is better (0 is the best)
     */
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
    
    int median( int[] arr ) {
        int[] copy = Arrays.copyOf(arr, arr.length );
        Arrays.sort(copy);
        return copy[copy.length >> 1];        
    }
    
    /**
     Get the perforation series, smoother and with interpolated missing frames
     @param maxY Maximum Y coordinate of the scanned strip
     @return List of perforations
     */
    public List<Perforation> getPerforations( int maxY ) {
        List<Perforation> ret = new ArrayList<Perforation>();
        Perforation lastPerf = null;
                
        for ( Perforation p : perforations ) {
            if ( lastPerf != null ) {
                if ( p.y - lastPerf.y > PERF_DISTANCE+Y_TOLERANCE ) {
                    // Perforation is missing here, add new ones at constant distance
                    int addCount = ( p.y - lastPerf.y + 2 * Y_TOLERANCE ) / PERF_DISTANCE;
                    int dy = (p.y-lastPerf.y ) / addCount;
                    int dx = (p.x-lastPerf.x ) / addCount;
                    System.out.println( String.format( "Interpolating %d images, dx = %d, dy = %d", addCount, dx, dy ));
                    for ( int n = 1 ; n < addCount ; n++ ) {
                        Perforation newP = new Perforation();
                        newP.x = lastPerf.x + n * dx;
                        newP.y = lastPerf.y + n * dy;
                        newP.series = this;
                        ret.add(newP);
                        System.out.println( String.format( "Added interpolated image( %s, %s )", newP.x, newP.y ) );
                    }
                }
            }
            ret.add( p );
            System.out.println( String.format("Added normal image      ( %s, %s )", p.x, p.y ) );
            lastPerf = p;
        }
        
        // Are we missing some perforations in the beginning?
        if ( ret.get( 0 ).y > PERF_DISTANCE && ret.size() > 1 ) {
            // Extrapolate the first frames based on the next ones
            int dx = ret.get( 1 ).x - ret.get( 0 ).x;
            int dy = ( ret.get( ret.size()-1 ).y - ret.get(0).y ) / (ret.size()-1);
            int y = ret.get(0).y-dy;
            int x = ret.get(0).x-dx;
            System.out.println( String.format( "Extrapolating beginning dx=%d, dy=%d, x=%d, y=%d", dx, dy, x, y ));
            while ( y > 0 ) {
                Perforation p = new Perforation();
                p.x = x;
                p.y = y;
                x -= dx;
                y -= dy;
                ret.add(0, p);
            }
        }
        
        // Low pass filter the perforation coordinates to decrease random errors
        for ( int n = 0; n < ret.size() ; n++ ) {
            int dx = 0;
            int ddx = 0;
            int ddy = 0;
            if ( n > 0  ) {
                dx = ret.get( n ).x - ret.get( n - 1 ).x;
            }
            if ( n > 0 && n < ret.size() -1 ) {
                ddx = ret.get( n+1 ).x + ret.get( n - 1 ).x - 2 * ret.get( n ).x;
                ddy = ret.get( n+1 ).y + ret.get( n - 1 ).y - 2 * ret.get( n ).y;
            }
            ret.get(n).x += ddx/2;
            ret.get(n).y += ddy/2;
        }
        

        return ret;
    }
}
