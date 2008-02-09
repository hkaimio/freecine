package fi.kaimio.moviescan;

public class Perforation {

    public int x;
    public int y;
    PerforationSeries series;
    
    private boolean isInitialized = false;
    private int startY;
    private int rightBorder;
    private int leftBorder;
    private int blackCount;
    private int whiteCount;
    
    public int getRightBorder() {
        return rightBorder;
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
                rightBorder = n;
            } else if ( whiteCount > 50 ) {
                break;
            }
        }
    }
    
    public boolean processLine( int[] whiteColumnStarts, int[] blackColumnStarts, int y ) {
        if ( y < startY + 50 ) {
            calcStartEdge( whiteColumnStarts );
        }

        // Calculate the number of pixels turned black
        for ( int n = 0 ; n < rightBorder ; n++ ) {
            if ( blackColumnStarts[n] == y ) {
                blackCount++;
            }
        }
        
        // If enough columns have turned black, the perforation is ready
        if ( blackCount * 10 > whiteCount * 9 ) {
            x = rightBorder;
            this.y = ( startY + y ) >> 1;
            return true;
        }
        return false;
    } 
}
