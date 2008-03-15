package fi.kaimio.moviescan;


/**
 Seimple class for representing a location of film perforation.
 @author Harri Kaimio
 */
public class Perforation {

    public int x;
    public int y;
    PerforationSeries series;
    
    public Perforation() {
    }

    Perforation( int x, int y ) {
        this.x = x;
        this.y = y;
    }
}
