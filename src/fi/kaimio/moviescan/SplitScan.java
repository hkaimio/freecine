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
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
    private boolean debug = false;

    static Logger log = Logger.getLogger( SplitScan.class.getName() );
    
    private PlanarImage readImage( String fname ) {
        PlanarImage img = JAI.create( "fileload", fname );
        return img;        
    }
    
    private RenderedImage findPerforationEdges( PlanarImage img ) {
        RenderedOp blueBand = BandSelectDescriptor.create( img, new int[] {2}, null );
        return BinarizeDescriptor.create(blueBand, (Double) 64500.0, null );
    }
    
    
    List<Point> perfPixels = new ArrayList<Point>( 1000000 );
    int[] pixelsInLine = null;
    int[] perfBorderX = null;
    List<Integer> perfY = new ArrayList<Integer>(  );
    static final int PERF_HOLE_THRESHOLD = 50;
    static final int MIN_LINES = 20;


    private void findPerfHolePoints( RenderedImage img ) {
        pixelsInLine = new int[ img.getHeight()];
        perfBorderX = new int[ img.getHeight()];
        /**
         The perforations should be in the 1/3 of the leftmost image.
         */
        Rectangle perfArea = new Rectangle(0, 0, img.getWidth()/3, img.getHeight() );
        RectIter iter = RectIterFactory.create(img, perfArea );
        SampleModel sm = img.getSampleModel(  );
        int nbands = sm.getNumBands(  );
        int[] pixel = new int[nbands];

        int perfPixelCount = 0;
        int x=0, y=0;
        System.err.println( "Finding perforations..." );
        while ( !iter.nextLineDone() ) {
            pixelsInLine[y] = 0;
            perfBorderX[y] = 0;
            x = 0;
            iter.startPixels();
            while( !iter.nextPixelDone()  ) {
                iter.getPixel( pixel );
                if ( pixel[0] > 0 ) {
                    perfPixelCount++;
                    pixelsInLine[y]++;
                } else if ( pixelsInLine[y] > PERF_HOLE_THRESHOLD ) {
                    /*
                     There are enough white pixels in this line that we 
                     this looks like a perforation. Store the right border &
                     continue
                     */
                    perfBorderX[y] = x;
                    break;
                }
                x++;
            }
            log.log(Level.FINE, "Line " + y + ", " + pixelsInLine[y] + "pixels" );
            y++;
        }
        
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
                        System.err.println( "Found perforation at " + perfCenterY );
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
    
    
    final static int frameStartX = 0;
    final static int frameWidth = 1100;
    final static int frameHeight = 800;

    private int getFrameLeft( int starty, int endy ) {
        ArrayList<Integer> perfBorderPoints = new ArrayList<Integer>(400);
        for ( int n = starty; n < endy; n++ ) {
            if ( perfBorderX[n] > 0 ) {
                perfBorderPoints.add( perfBorderX[n] );
            }
        }
        Collections.sort( perfBorderPoints );
        return perfBorderPoints.get( perfBorderPoints.size() >> 1 );
    }
    
    private void saveFrames( RenderedImage scanImage, String fnameTmpl ) {
            for ( int n = 1; n < perfY.size() - 1; n++ ) {
            String fname = String.format( fnameTmpl, (Integer) n );
            System.err.println( "Saving frame " + fname );
            int startY = (perfY.get( n - 1 ) + perfY.get( n )) >> 1;
            int startX = getFrameLeft(perfY.get( n - 1 ), perfY.get( n ));
            System.out.println( "" + startX + "\t" + startY );
            int w = Math.min( frameWidth, scanImage.getWidth() - startX );
            RenderedOp frame = CropDescriptor.create( scanImage, (float) startX, (float) startY, (float) w, (float) frameHeight, null );

            FileStoreDescriptor.create( frame, fname, "TIFF", null, null, null );
            if ( debug ) {
                String debugFname = String.format( "debug_%05d.png", (Integer) n );
                BufferedImage debugLayer = new BufferedImage( frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_RGB );
                for ( int row = 0; row < frame.getHeight(); row++ ) {
                    debugLayer.getRaster().setPixel( perfBorderX[startY + row], row, new int[]{255, 255, 255} );
                }
                try {
                    ImageIO.write( debugLayer, "PNG", new File( debugFname ) );
                } catch ( IOException ex ) {
                    Logger.getLogger( SplitScan.class.getName() ).log( Level.SEVERE, null, ex );
                }
            }
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
        String fnameTmpl = "frame_%05d.tif";
        if ( args.length > 1 ) {
            fnameTmpl = args[1];
        }
        t.saveFrames( img, fnameTmpl );
    }

}
