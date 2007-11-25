/*
 * SplitScan.java
 * 
 * Created on Oct 7, 2007, 4:14:48 PM
 * 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fi.kaimio.moviescan;

import com.sun.media.jai.operator.ImageReadDescriptor;
import com.sun.media.jai.opimage.AffineCRIF;
import com.sun.media.jai.util.SunTileCache;
import java.awt.Dimension;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.event.IIOReadProgressListener;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import javax.media.jai.TiledImage;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;
import javax.media.jai.operator.AffineDescriptor;
import javax.media.jai.operator.BandCombineDescriptor;
import javax.media.jai.operator.BandSelectDescriptor;
import javax.media.jai.operator.BinarizeDescriptor;
import javax.media.jai.operator.CropDescriptor;
import javax.media.jai.operator.MaxFilterDescriptor;
import javax.media.jai.operator.MinDescriptor;
import javax.media.jai.operator.TransposeDescriptor;
import javax.media.jai.operator.TransposeDescriptor;

/**
 *
 * @author harri
 */
public class SplitScan {
    private boolean debug = false;

    static Logger log = Logger.getLogger( SplitScan.class.getName() );
    
    static class ReadListener implements IIOReadProgressListener {
        int reads = 0;
        public void sequenceStarted( ImageReader source, int minIndex ) {
            System.err.println( "sequenceStarter " + minIndex );
        }

        public void sequenceComplete( ImageReader source ) {
            System.err.println( "sequenceComplete"  );
        }

        public void imageStarted( ImageReader source, int imageIndex ) {
            System.err.println( "imageStarted " + imageIndex );
        }

        public void imageProgress( ImageReader source, float percentageDone ) {
            System.err.println( "progress " + percentageDone );
        }

        public void imageComplete( ImageReader source ) {
            reads++;
            System.err.println( "imageComplete " + reads );
        }

        public void thumbnailStarted( ImageReader source, int imageIndex, int thumbnailIndex ) {
            System.err.println( "thumb started" + imageIndex );
        }

        public void thumbnailProgress( ImageReader source, float percentageDone ) {
            System.err.println( "thumb progress " + percentageDone );
        }

        public void thumbnailComplete( ImageReader source ) {
            System.err.println( "thumb compelte"  );
        }

        public void readAborted( ImageReader source ) {
            System.err.println( "read aborted" );
        }
        
    }
    
    ImageReader reader;

    private int median( int[] arr ) {
        int[] copy = Arrays.copyOf(arr, arr.length );
        Arrays.sort(copy);
        return copy[copy.length >> 1];
    }
    
    private RenderedImage readImage( String fname ) {
        try {
//        PlanarImage img = JAI.create( "fileload", fname );
            ImageInputStream istrm = new FileImageInputStream( new File( fname ) );
            reader = ImageIO.getImageReadersByFormatName( "TIFF" ).next();
            reader.setInput( istrm );
            ImageReadParam param = reader.getDefaultReadParam();
            // param.setSourceRegion( new Rectangle(0, 0, 1024, reader.getHeight(0 ) ) );
            ImageLayout layout = new ImageLayout();
            layout.setTileHeight(512);
            layout.setTileWidth(4096);
            RenderingHints hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, layout);
            RenderedOp img = ImageReadDescriptor.create(istrm, 0, false, false, false, 
                    null, null, param, reader, hints );
            // BufferedImage inImg = reader.read( 0, param );            
            return img;
        } catch ( FileNotFoundException ex ) {
            System.out.println( ex.getMessage() );
            Logger.getLogger( SplitScan.class.getName() ).log( Level.SEVERE, null, ex );
        } catch ( IOException ex ) {
            System.out.println( ex.getMessage() );
            Logger.getLogger( SplitScan.class.getName() ).log( Level.SEVERE, null, ex );
        }
        return null;
    }
    
    private void saveDebugImage( RenderedImage src, String desc, int x, int y,
            int w, int h ) {
        AffineTransform xform = AffineTransform.getTranslateInstance( 0, -y );
        RenderedOp perfTrans = AffineDescriptor.create( maskImage, xform, null, null, null );
        RenderedOp perfImage = CropDescriptor.create( perfTrans, 0.0f, 0.0f, (float)w, (float)h, null );
        
        // Find out the file name
        File f = getDebugImageFile( String.format( "%s_%05d", desc, y ) );
        try {
            ImageIO.write( perfImage, "TIFF", f );
        } catch ( IOException e ) {
            System.err.println( "Error saving hole: " + e.getMessage() );
        }
    }
    
    RenderedImage maskImage;
    
    private RenderedImage findPerforationEdges( RenderedImage img ) {
        maskImage = img;
        RenderedOp redBand = BandSelectDescriptor.create( img, new int[] {0}, null );
        RenderedOp greenBand = BandSelectDescriptor.create( img, new int[] {1}, null );
        RenderedOp blueBand = BandSelectDescriptor.create( img, new int[] {2}, null );
        RenderedOp minrg = MinDescriptor.create(redBand, greenBand, null );
        RenderedOp min = MinDescriptor.create(minrg, blueBand, null );
        RenderedOp maxf = MaxFilterDescriptor.create( min, MaxFilterDescriptor.MAX_MASK_SQUARE, 10, null );
        return BinarizeDescriptor.create(maxf, (Double) 60000.0, null );
    }
    
    private RenderedImage getRotatedImage( RenderedImage img ) {
        // TiledImage ti = new TiledImage( img, 64, 64 );
        return TransposeDescriptor.create(img, TransposeDescriptor.ROTATE_90, null);
    }
    
    private void writeTiled( File in ) {
        try {
            ImageInputStream istrm = new FileImageInputStream( in );
            ImageReader reader = ImageIO.getImageReadersByFormatName( "TIFF" ).next();
            reader.setInput( istrm );
            RenderedImage inImg = reader.readAsRenderedImage( 0, null );
            RenderedImage striped = new TiledImage( inImg, inImg.getWidth(), 512 );
            RenderedImage tiled = new TiledImage( striped, 512, 512 );
            ImageOutputStream output = ImageIO.createImageOutputStream( new File( "/tmp/tmpimage.tif" ) );
            ImageWriter writer = (ImageWriter) ImageIO.getImageWritersByFormatName( "TIFF" ).next();
            writer.setOutput( output );
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setTilingMode( ImageWriteParam.MODE_EXPLICIT );
            param.setTiling( 512, 512, 0, 0 );
            writer.write(null,
                     new IIOImage(tiled, null, reader.getImageMetadata(0)),
                     param);
        } catch ( IOException ex ) {
            Logger.getLogger( SplitScan.class.getName() ).log( Level.SEVERE, null, ex );
        }
                    
    }
    
    List<Point> perfPixels = new ArrayList<Point>( 1000000 );
    int[] pixelsInLine = null;
    int[] perfBorderX = null;
    List<Integer> perfY = new ArrayList<Integer>(  );
    List<Integer> perfX = new ArrayList<Integer>(  );
    static final int PERF_HOLE_THRESHOLD = 50;
    static final int MIN_LINES = 20;
    static final int MEDIAN_WINDOW = 100;

    

    private void findPerfHolePoints( RenderedImage img ) {
        perfBorderX = new int[ img.getHeight()];
        /**
         The perforations should be in the 1/3 of the leftmost image.
         */
        Rectangle perfArea = new Rectangle(0, 0, img.getWidth()/4, img.getHeight() );
        RectIter iter = RectIterFactory.create(img, perfArea );
        SampleModel sm = img.getSampleModel(  );
        int nbands = sm.getNumBands(  );
        int[] pixel = new int[nbands];
        int x=0, y=-1;
        int perfStartY = -1;
        int perfEndY = -1;
        boolean isPerforation = false;
        int linesToDecide = -1;
        int[] lastLines = new int[MEDIAN_WINDOW];
        int n = 0;
        System.out.println( "Finding perforations..." );
        while ( !iter.nextLineDone() ) {
            y++;
            int pixelsInLine = 0;
            perfBorderX[y] = 0;
            x = 0;
            iter.startPixels();
            while( !iter.nextPixelDone()  ) {
                iter.getPixel( pixel );
                if ( pixel[0] > 0 ) {
                    pixelsInLine++;
                } else if ( pixelsInLine > PERF_HOLE_THRESHOLD ) {
                    /*
                     There are enough white pixels in this line that 
                     this looks like a perforation. Store the right border &
                     continue
                     */
                    perfBorderX[y] = x;
                    break;
                }
                x++;
            }
            
            
            // Calculate median of white pixels in recent lines
            lastLines[n] = pixelsInLine;
            n++;
            if ( n >= MEDIAN_WINDOW ) {
                n = 0;
            }
            int medianPixels = median( lastLines );

            if ( y < MEDIAN_WINDOW ) {
                // The ring buffer is not yet full
                continue;
            }

            
            // Analyze this line
            if ( isPerforation ) {
                if ( medianPixels <= PERF_HOLE_THRESHOLD ) {
                    // The perforation ends here

                    perfEndY = y - (MEDIAN_WINDOW >> 1);
                    // So many black lines in a row that we can be pretty sure
                    int perfCenterY = (perfEndY + perfStartY) >> 1;
                    int perfCenterX = getFrameLeft( perfStartY, perfEndY );
                    if ( perfCenterX > 0 ) {
                        int prevX = 0;
                        int prevY = 0;
                        if ( perfX.size() >= 1 ) {
                            prevX = perfX.get( perfX.size() -1 );
                            prevY = perfY.get( perfY.size() -1 );
                        }
                        System.out.println( 
                                String.format( "Found perforation at (%d, %d) %+d, %+d", 
                                perfCenterX, perfCenterY,
                                perfCenterX-prevX, perfCenterY-prevY ) );
                        perfY.add( perfCenterY );
                        perfX.add( perfCenterX );
                        // Save image of the perforation
                        int imageY = perfCenterY-200;
                        imageY = Math.max( 0, imageY );
                        saveDebugImage( maskImage, "hole", 
                                0, imageY, 
                                300, Math.min( 400, maskImage.getHeight()-imageY ) );
                    }
                    isPerforation = false;
                }
            } else {
                // Not in a perforation
                if ( medianPixels > PERF_HOLE_THRESHOLD ) {
                    perfStartY = y - (MEDIAN_WINDOW >> 1);
                    isPerforation = true;
                }
            }
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
        return perfBorderPoints.size() > 0 ? 
            perfBorderPoints.get( perfBorderPoints.size() >> 1 ) : 0;
    }
    
    AffineTransform getFrameXform( int frame ) {
        /**
         Estimate film rotation from max 5 perforations
         */ 
         int f1 = frame-1;
         int f2 = frame+1;
         int x1 = (f1 >= 0) ? perfX.get( f1 ) : perfX.get(0);
         int x2 = (f2 < perfX.size() ) ? perfX.get( f2 ) : perfX.get( perfX.size()-1 );
         int y1 = (f1 >= 0) ?  perfY.get( f1 ) : perfX.get(0);
         int y2 = (f2 < perfX.size() ) ?  perfY.get( f2 ) : perfY.get( perfY.size()-1 );
         double rot = Math.atan2((double)x2-x1, (double)(y2-y1) );
         // Translate the center of perforation to origin
         AffineTransform xform = AffineTransform.getTranslateInstance( -perfX.get(frame), -perfY.get(frame) );
         
         xform.preConcatenate( AffineTransform.getRotateInstance(rot));
         
         xform.preConcatenate(AffineTransform.getTranslateInstance(0, frameHeight/2));
         return xform;
    }
    
    String fnameTmpl = "frame_%05d.tif";
    RenderedImage scanImage = null;
    
    private void saveFrame( int n ) throws IOException {            
        String fname = String.format( fnameTmpl, (Integer) n );
        
        int startY = (perfY.get( n - 1 ) + perfY.get( n )) >> 1;
        int startX = perfX.get( n - 1 );
        System.out.println( "Saving frame " + fname + 
                ", perforation at ("+ startX + ", " + startY + ")" );
        int w = Math.min( frameWidth, scanImage.getWidth() - startX );
        AffineTransform xform = getFrameXform( n );
        RenderedOp rotated = AffineDescriptor.create( scanImage, xform, Interpolation.getInstance( Interpolation.INTERP_BICUBIC ), null, null );
        int minx = rotated.getMinX();
        int miny = rotated.getMinY();
        int rw = rotated.getWidth();
        int rh = rotated.getHeight();
        RenderedOp frame = CropDescriptor.create( rotated, (float) 0, (float) 0, (float) w, (float) frameHeight, null );

//        double[][] cm = {
//            {0.0, 0.0, 0.0, 0.0},
//            {2.353675, -0.8596982, -0.3466206, 0.0},
//            {-0.7270595, 1.593924, 0.08920667, 0.0},
//            {-0.05648472, 0.1896461, 0.7934276, 0.0},
//            {0.0, 0.0, 0.0, 0.0}
//        };
//        frame = BandCombineDescriptor.create(frame, cm, null );
        // Find a writer for that file extensions
        ImageWriter writer = null;
        Iterator iter = ImageIO.getImageWritersByFormatName( "TIFF" );
        if ( iter.hasNext() ) {
            writer = (ImageWriter) iter.next();
        }
        if ( writer != null ) {
            ImageOutputStream ios = null;
            try {
                // Prepare output file
                ios = ImageIO.createImageOutputStream( new File( fname ) );
                writer.setOutput( ios );
                // Set some parameters
                ImageWriteParam param = writer.getDefaultWriteParam();
                writer.write( null, new IIOImage( frame, null, null ), param );

                // Cleanup
                ios.flush();

            } catch ( IOException ex ) {
                Logger.getLogger( SplitScan.class.getName() ).log( Level.SEVERE, null, ex );
            } finally {
                if ( ios != null ) {
                    try {
                        ios.close();
                    } catch ( IOException e ) {
                        System.err.println( "Error closing output stream" );
                    }
                }
                writer.dispose();
            }
        }
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
        frame.dispose();
        rotated.dispose();
        System.gc();
        System.gc();

    }
    
    private void saveFrames( RenderedImage scanImage, String fnameTmpl ) {
        RenderedOp frame = null;
        RenderedOp rotated = null;
        int frameNum = 1;
        for ( int n = 0; n < perfY.size(); n++ ) {
            String fname = String.format( fnameTmpl, (Integer) frameNum );
            System.out.println( "Saving frame " + fname );
            int startX = perfX.get( n );
            int w = Math.min( frameWidth, scanImage.getWidth() - startX );
            AffineTransform xform = getFrameXform( n );
            try {
                if ( frame == null ) {
                    rotated = AffineDescriptor.create( 
                            scanImage, xform, 
                            Interpolation.getInstance( Interpolation.INTERP_BICUBIC ), 
                            null, null );
                    frame = CropDescriptor.create( 
                            rotated, (float) 0, (float) 0, 
                            (float) w, (float) frameHeight, null );
                } else {
                    rotated.setParameter( xform, 0 );
                }
                // Find a writer for that file extensions
                ImageWriter writer = null;
                Iterator iter = ImageIO.getImageWritersByFormatName( "TIFF" );
                if ( iter.hasNext() ) {
                    writer = (ImageWriter) iter.next();
                }
                if ( writer != null ) {
                    ImageOutputStream ios = null;
                    try {
                        // Prepare output file
                        ios = ImageIO.createImageOutputStream( new File( fname ) );
                        writer.setOutput( ios );
                        // Set some parameters
                        ImageWriteParam param = writer.getDefaultWriteParam();
                        PlanarImage rendering = frame.getNewRendering();
                        writer.write( null, new IIOImage( rendering, null, null ), param );

                        // Cleanup
                        ios.flush();
                        rendering.dispose();

                    } catch ( IOException ex ) {
                        Logger.getLogger( SplitScan.class.getName() ).log( Level.SEVERE, null, ex );
                    } finally {
                        if ( ios != null ) {
                            try {
                                ios.close();
                            } catch ( IOException e ) {
                                System.err.println( "Error closing output stream" );
                            }
                        }
                        writer.dispose();
                    }
                }
                frame.dispose();
                rotated.dispose();
                System.gc();
                System.gc();
                frameNum++;
            } catch ( Exception e ) {
                System.err.println( "Error saving frame " + frameNum + ": " + e.getMessage() ); 
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

    static String fname;
    static String maskName;
    static String outTmpl = "frame_%05d.tif";
    static boolean isDebug = false;
    static String debugDir;
    
    static File getDebugImageFile( String desc ) {
        File inFile = new File( fname );
        String fn = inFile.getName();
        // Strip the ending of the file name
        int dotPos = fn.lastIndexOf( "." );
        if ( dotPos > 0 ) {
            fn = fn.substring( 0, dotPos );
        }
        String debugFname = String.format( "%s_%s.tif", fn, desc );
        File ddir = null;
        if ( debugDir != null ) {
            ddir = new File( debugDir );
        }
        return new File( ddir, debugFname );
    }
    
    public static void usage() {
        System.err.println( "Usage: splitscan [-d|--debug] -m maskfile -o outfile_template infile" );
    }
    
    public static void parseArgs( String args[] ) {
        int n = 0;
        while ( n < args.length ) {
            if ( args[n].equals( "-d" ) || args[n].equals( "--debug" ) ) {
                isDebug = true;
                debugDir = args[n+1];
                n++;
            } else if ( args[n].equals( "-o" ) ) {
                outTmpl = args[n+1];
                n++;
            } else if ( args[n].equals( "-m" ) ) {
                maskName = args[n+1];
                n++;
            } else {
                fname = args[n];
                break;
            }
            n++;
        }
        if ( n < args.length-1 ) {
            usage();
            System.exit( -1 );
        }
    }
    
    
    /**
     * @param args the command line arguments
     */
    public static void main( String[] args ) {
        parseArgs( args );
        log.setLevel( Level.FINE );
        JAI.setDefaultTileSize(new Dimension( 64, 64 ) );
        JAI.getDefaultInstance().setTileCache( new SunTileCache( 100*1024*1024 ) );
        SplitScan t = new SplitScan(  );
        long startTime = System.currentTimeMillis();
        System.out.println( "Reading image "  + fname );
        RenderedImage img = t.readImage( fname );
        System.out.println( "done, width " + img.getWidth() + " height " + img.getHeight() );
        RenderedImage maskImg = img;
        if ( maskName != null ) {
            System.out.println( "using " + maskName + " as mask" );
            maskImg = t.readImage( maskName );
            System.out.println( "done, width " + maskImg.getWidth() + " height " + maskImg.getHeight() );
        }
        System.out.println( "Creating perforation mask" );
        if ( img.getWidth() > img.getHeight() ) {
            System.out.println( "Rotating image by 90 degrees" );
            img = t.getRotatedImage(img);
            maskImg = t.getRotatedImage( maskImg );
        }
        RenderedImage binaryImg = t.findPerforationEdges( maskImg );
        t.scanImage = img;
        t.findPerfHolePoints( binaryImg );
        long analysisTime = System.currentTimeMillis() - startTime;
        System.out.println( "Image analyzed in " + ((double)analysisTime)/1000.0);
        t.fnameTmpl = outTmpl;
        t.saveFrames( img, outTmpl );
        long saveTime = System.currentTimeMillis() - analysisTime - startTime;
        System.out.println( "Images saved in " + ((double)saveTime)/1000.0);
    }

}
