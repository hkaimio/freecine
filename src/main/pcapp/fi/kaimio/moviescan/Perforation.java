package fi.kaimio.moviescan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Perforation {

    public int x;
    public int y;
    PerforationSeries series;
    
    private boolean isInitialized = false;
    private int startY;
    private int startEdgeRight;
    private int leftBorder;
    private int blackCount;
    private int whiteCount;
    List<Integer> rightEdgePoints = new ArrayList<Integer>();
    List<Perforation> nextPerfCandidates = new ArrayList<Perforation>();
    
    public int getRightBorder() {
        return startEdgeRight;
    }
    
    public Perforation() {
    }

    public Perforation( int[] whiteColumnStarts, int y ) {
        startY = y;
        
        /* 
         Find the rightmost pixel of the run that has recently changed 
         to white and therefore belongs to the perforation
         */
        calcStartEdge(whiteColumnStarts);
    }
    
    private void calcStartEdge( int[] whiteColumnStart ) {
        whiteCount = 0;
        for ( int n = 0 ; n < whiteColumnStart.length ; n++ ) {
            if ( whiteColumnStart[n] > startY-10 ) {
                whiteCount++;
                startEdgeRight = n;
            } else if ( whiteCount > 50 ) {
                break;
            }
        }
    }
    
    public void addNextPerfCandidate( Perforation p ) {
        nextPerfCandidates.add( p );
    }
    
    public List<Perforation> getNextPerfCandidates() {
        return nextPerfCandidates;
    }
    
    public boolean processLine( int[] whiteColumnStarts, int[] blackColumnStarts, int y ) {
        if ( y < startY + 50 ) {
            calcStartEdge( whiteColumnStarts );
        }

        // Calculate the number of pixels turned black
        for ( int n = 0 ; n < startEdgeRight ; n++ ) {
            if ( blackColumnStarts[n] == y ) {
                blackCount++;
            }
        }
        
        // Calculate the rightmost white point in this line
        int whitePixels = 0;
        for ( int n = 0; n < whiteColumnStarts.length; n++ ) {
            if ( blackColumnStarts[n] > whiteColumnStarts[n] ) {
                // this pixel is black
                if ( whitePixels > 50 ) {
                    rightEdgePoints.add( n );
                    break;
                }
            } else {
                // White pixel
                whitePixels++;
            }
        }
        
        
        // If enough columns have turned black, the perforation is ready
        if ( blackCount * 10 > whiteCount * 9 ) {
            // X coordinate is the median of pixels belonging to right border
            int medianRight = 0;
            if ( rightEdgePoints.size() > 0 ) {
                Collections.sort( rightEdgePoints  );
                medianRight = rightEdgePoints.get( rightEdgePoints.size() >> 1 );
            }
            x = startEdgeRight;
            if ( Math.abs( medianRight - startEdgeRight ) < 10 ) {
                x = medianRight;
            }
            this.y = (startY + y) >> 1;
            System.out.println( String.format( "  start edge right = %d, right edge median = %d (%+d), y = %d",
                    startEdgeRight, medianRight, medianRight - startEdgeRight, y ) );
            return true;
        }
        return false;
    } 
}
