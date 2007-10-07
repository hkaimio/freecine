/*
 * SplitScan.java
 * 
 * Created on Oct 7, 2007, 4:14:48 PM
 * 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fi.kaimio.moviescan;

import java.awt.Point;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;
import javax.media.jai.operator.BandSelectDescriptor;
import javax.media.jai.operator.BinarizeDescriptor;
import javax.media.jai.operator.CropDescriptor;
import javax.media.jai.operator.FileStoreDescriptor;

/**
 *
 * @author harri
 */
public class SplitScan {

    static Logger log = Logger.getLogger( SplitScan.class.getName() );
    
    private PlanarImage readImage( String fname ) {
        PlanarImage img = JAI.create( "fileload", fname );
        return img;        
    }
    
    private RenderedImage findPerforationEdges( PlanarImage img ) {
        RenderedOp redBand = BandSelectDescriptor.create( img, new int[] {0}, null );
        return BinarizeDescriptor.create(redBand, (Double) 63000.0, null );
    }
    
    
    List<Point> perfPixels = new ArrayList<Point>( 1000000 );
    int[] pixelsInLine = null;
    List<Integer> perfY = new ArrayList<Integer>(  );
    static final int PERF_HOLE_THRESHOLD = 30;
    static final int MIN_LINES = 20;


    private void findPerfHolePoints( RenderedImage img ) {
        pixelsInLine = new int[ img.getHeight()];
        RectIter iter = RectIterFactory.create(img, null );
        SampleModel sm = img.getSampleModel(  );
        int nbands = sm.getNumBands(  );
        int[] pixel = new int[nbands];

        int perfPixelCount = 0;
        int x=0, y=0;
        while ( !iter.nextLineDone() ) {
            pixelsInLine[y] = 0;
            x = 0;
            iter.startPixels();
            while( !iter.nextPixelDone()  ) {
                iter.getPixel( pixel );
                if ( pixel[0] > 0 ) {
                    perfPixels.add( new Point( x, y ) );
                    perfPixelCount++;
                    pixelsInLine[y]++;
                }
                x++;
            }
            log.log(Level.FINE, "Line " + y + ", " + pixelsInLine[y] + "pixels" );
            y++;
        }
        log.log( Level.FINE, "" + perfPixels.size() + " perforation pixels found" );
        
        /**
         Find the performations in Y direction.          
         */
        
        
        int perfStartY = -1;
        int perfEndY = -1;
        boolean isPerforation = false;
        int linesToDecide = -1;
        for ( int row = 0; row < pixelsInLine.length ; row++ ) {
            if ( isPerforation ) {
                if ( pixelsInLine[row] < PERF_HOLE_THRESHOLD ) {
                    // The perforation ends here
                    if ( linesToDecide < 0 ) {
                        // This is a new candidate for ending the performation hole.
                        // Check MIN_LINES next lines to be sure
                        linesToDecide = MIN_LINES;
                        perfEndY = row;
                    } else if ( linesToDecide > 0 ) {
                        linesToDecide--;
                    } else {
                        // So many black lines in a row that we can be pretty sure
                        int perfCenterY = (perfEndY+perfStartY) >> 1;
                        perfY.add( perfCenterY );
                        isPerforation = false;
                        linesToDecide = -1;
                    }
                } else {
                    linesToDecide = -1;
                }
            } else {
                // Not in a perforation
                if ( pixelsInLine[row] > PERF_HOLE_THRESHOLD ) {
                    if ( linesToDecide < 0 ) {
                        perfStartY = row;
                        linesToDecide = MIN_LINES;
                    } else if ( linesToDecide > 0 ) {
                        linesToDecide--;
                    } else {
                        isPerforation = true;
                        linesToDecide = -1;
                    }
                } else {
                    linesToDecide = -1;
                }
            }
        }
        System.out.println( "Perforations:" );
        for ( Integer row : perfY ) {
            System.out.println( row );
        }
    }
    
    
    final static int frameStartX = 200;
    final static int frameWidth = 1200;
    final static int frameHeight = 800;
    
    private void saveFrames( RenderedImage scanImage ) {
        for ( int n = 1 ; n < perfY.size(  ) - 1 ; n++ ) {
            int startY = (perfY.get( n - 1 ) + perfY.get( n )) >> 1;
            System.out.println( ""+ perfY.get(n-1)+"\t" + perfY.get(n) + "\t" + startY );
            RenderedOp frame = CropDescriptor.create( scanImage, (float) frameStartX, (float) startY,
                    (float) frameWidth, (float) frameHeight, null );
            String fname = String.format( "frame_%05d.tif", (Integer)n );
            FileStoreDescriptor.create(frame, fname, "TIFF", null, null, null );
        }
    }
    
        /**
     * Get a proper image reader for a file based on file name extension.
     * @param f The file
     * @return Correct Reader or <CODE>null</CODE> if no proper reader is found.
     */
    static private ImageReader getImageReader( File f ) {
        ImageReader ret = null;
        if ( f != null ) {
            String fname = f.getName();
            int lastDotPos = fname.lastIndexOf( "." );
            if ( lastDotPos > 0 && lastDotPos < fname.length()-1 ) {
                String suffix = fname.substring( lastDotPos+1 );
                Iterator readers = ImageIO.getImageReadersBySuffix( suffix );
                if ( readers.hasNext() ) {
                    ret = (ImageReader)readers.next();
                }
            }
        }
        return ret;
    }

    
    
    
    /**
     * @param args the command line arguments
     */
    public static void main( String[] args ) {
        log.setLevel( Level.FINE );
        SplitScan t = new SplitScan(  );
        PlanarImage img = t.readImage( args[0] );
        RenderedImage binaryImg = t.findPerforationEdges( img );
        t.findPerfHolePoints( binaryImg );
        t.saveFrames( img );
    }

}
