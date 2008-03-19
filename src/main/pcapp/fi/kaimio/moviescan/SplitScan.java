/*
 * SplitScan.java
 * 
 * Created on Oct 7, 2007, 4:14:48 PM
 * 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fi.kaimio.moviescan;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.media.jai.operator.ImageReadDescriptor;
import com.sun.media.jai.util.SunTileCache;
import fi.kaimio.sane.FixedPointNumber;
import fi.kaimio.sane.Sane;
import fi.kaimio.sane.SaneDevice;
import fi.kaimio.sane.SaneDeviceDescriptor;
import fi.kaimio.sane.SaneException;
import fi.kaimio.sane.ScanParameter;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.ArrayList;
import java.util.ArrayList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
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
import javax.media.jai.KernelJAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;
import javax.media.jai.RenderedOp;
import javax.media.jai.TiledImage;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;
import javax.media.jai.operator.AffineDescriptor;
import javax.media.jai.operator.ConvolveDescriptor;
import javax.media.jai.operator.CropDescriptor;
import javax.media.jai.operator.FormatDescriptor;
import javax.media.jai.operator.TransposeDescriptor;
import javax.media.jai.operator.TransposeDescriptor;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import org.xml.sax.SAXException;

/**
 *
 * @author harri
 */
public class SplitScan {
    private static String projectDirName = "/home/harri/s8/tuhkimo";

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

    private void saveBorder( int[] pictureBorder ) {
        BufferedWriter w = null;
        try {
            File f = new File( "border.txt" );
            w = new BufferedWriter( new FileWriter( f ) );
            for ( int n = 0; n < pictureBorder.length ; n++ ) {
                w.write( String.format( "%d, %d\n", n, pictureBorder[n] ) );
                if ( n % 1000 == 0 ) {
                    System.err.println( "" + n + " lines written" );
                } 
            }
            w.close();
        } catch ( IOException ex ) {
            Logger.getLogger( SplitScan.class.getName() ).log( Level.SEVERE, null, ex );
        } finally {
            try {
                w.close();
            } catch ( IOException ex ) {
                Logger.getLogger( SplitScan.class.getName() ).log( Level.SEVERE, null, ex );
            }
        }
        
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
    
    private RenderedImage getRotatedImage( RenderedImage img ) {
        // TiledImage ti = new TiledImage( img, 64, 64 );
        return TransposeDescriptor.create(img, TransposeDescriptor.ROTATE_90, null);
    }
    
    List<Perforation> perforations = new ArrayList<Perforation>();
    List<Perforation> perfCandidates = new ArrayList<Perforation>();
    
    double minRadii = 20.0;
    double maxRadii = 25.0;
    
    static final int EDGE_MIN_GRADIENT = 0x10;
    static final int CORNER_MIN_HOUGH = 6;
    
    List<PointCluster> pointClusters = new ArrayList<PointCluster>();
    
    void houghTransform( RenderedImage img ) {
        maskImage = img; 
        KernelJAI sxKernel = new KernelJAI( 3 , 3 , 
                new float[]{-1.0f, 0.0f, 1.0f,
                            -2.0f, 0.0f, 2.0f,
                            -1.0f, 0.0f, 1.0f });
        KernelJAI syKernel = new KernelJAI( 3 , 3 , 
                new float[]{-1.0f, -2.0f, -1.0f,
                             0.0f,  0.0f,  0.0f,
                             1.0f,  2.0f,  1.0f });
        
        List<Point> startCorners = new ArrayList<Point>();
        List<Point> endCorners = new ArrayList<Point>();
        RenderedImage intImg = FormatDescriptor.create(img, DataBuffer.TYPE_DOUBLE, null );
        RenderedImage sxImg = ConvolveDescriptor.create(intImg, sxKernel, null );
        RenderedImage syImg = ConvolveDescriptor.create(intImg, syKernel, null );

        SampleModel sm = sxImg.getSampleModel(  );
        int nbands = sm.getNumBands(  );
        double[] sxPixel = new double[nbands];
        double[] syPixel = new double[nbands];
        
        Rectangle perfArea = new Rectangle(0, 0, img.getWidth()/4, img.getHeight() );
        RectIter sxIter = RectIterFactory.create(sxImg, perfArea );
        RectIter syIter = RectIterFactory.create(syImg, perfArea );
        int accumHeight = (int) maxRadii + 2;

        int width = (int)perfArea.getWidth(); // Dimensions of the image
        int height = (int)perfArea.getHeight();
        int[][] startAccum = new int[(int) (maxRadii - minRadii)][width*accumHeight];
        int[][] endAccum = new int[(int) (maxRadii - minRadii)][width*accumHeight];
        byte[] imageDataSingleArray = new byte[width * height];
        // Create a Data Buffer from the values on the single image array.
        DataBufferByte dbuffer = new DataBufferByte( imageDataSingleArray,
                width * height );
        // Create an int data sample model.
        SampleModel sampleModel =
                RasterFactory.createBandedSampleModel( DataBuffer.TYPE_BYTE,
                width,
                height,
                1 );
        // Create a compatible ColorModel.
        ColorModel colorModel = PlanarImage.createColorModel( sampleModel );
        // Create a WritableRaster.
        Raster raster = RasterFactory.createWritableRaster( sampleModel, dbuffer,
                new Point( 0, 0 ) );
        int y = 0;
        int maxVal = 0;
        double minAngle = Math.PI * 2.0;
        double maxAngle = 0.0;
        while ( !sxIter.nextLineDone() && !syIter.nextLineDone() ) {
            if ( y % 1000 == 0 && y > 0 ) {
                System.out.println( "" + y + " lines analyzed" );
            }
            sxIter.startPixels();
            syIter.startPixels();
            int x = 0;
            while ( !sxIter.nextPixelDone() && !syIter.nextPixelDone() ) {
                sxIter.getPixel( sxPixel );
                syIter.getPixel( syPixel );
                
                // imageDataSingleArray[x + width * y ] = (byte)Math.min( 255, ( Math.abs(sxPixel[0]) / 1024 ) );
                double isq = sxPixel[0] * sxPixel[0] + syPixel[0] * syPixel[0];
                if ( isq > EDGE_MIN_GRADIENT * EDGE_MIN_GRADIENT ) {
                    // This seems like a border
                    if ( syPixel[0] <= 0 && sxPixel[0] >= 0 ) {
                        double intensity = Math.sqrt(isq);
                        for ( double r = minRadii; r < maxRadii ; r+= 1.0 ) {
                            
                            double cx = (double)x - r * sxPixel[0] / intensity;
                            double cy = (double)y - r * syPixel[0] / intensity;
//                            if ( x > 110 && x < 130 && y > 230 && y < 260 && r == 20 ) {
//                                System.out.println( String.format( "(%d, %d) -> (%f , %f)", x, y, cx, cy ) );
//                            }
                            if ( cx > 0.0 ) {
                                int accumLine = (int)cy % accumHeight;
                                startAccum[(int)(r-minRadii)][(int)cx + width * accumLine]++;
                                if ( startAccum[(int)(r-minRadii)][(int)cx + width * accumLine] > maxVal ) {
                                    maxVal = startAccum[(int)(r-minRadii)][(int)cx + width * accumLine];
                                }
                            }
                        }
                    } 
                    if ( syPixel[0] >= 0 && sxPixel[0] >= 0 ) {
                        double intensity = Math.sqrt(isq);
                        for ( double r = minRadii; r < maxRadii ; r+= 1.0 ) {
                            
                            double cx = (double)x - r * sxPixel[0] / intensity;
                            double cy = (double)y - r * syPixel[0] / intensity;
//                            if ( x > 110 && x < 130 && y > 230 && y < 260 && r == 20 ) {
//                                System.out.println( String.format( "(%d, %d) -> (%f , %f)", x, y, cx, cy ) );
//                            }
                            if ( cx > 0.0 && cy > 0.0 ) {
                                int accumLine = (int)cy % accumHeight;
                                endAccum[(int)(r-minRadii)][(int)cx + width * accumLine]++;
                                if ( endAccum[(int)(r-minRadii)][(int)cx + width * accumLine] > maxVal ) {
                                    maxVal = endAccum[(int)(r-minRadii)][(int)cx + width * accumLine];
                                }
                            }
                        }
                    }  
                }
                x++;
            }
            y++;
            
            // Read & zero this line in accumulator
            int y2 = y - accumHeight;
            int l = y % accumHeight;
            if ( y2 > 0 ) {
                for ( int n = 0; n < perfArea.getWidth(); n++ ) {
                    for ( int r = 0; r < (int) (maxRadii - minRadii); r++ ) {
                        if ( startAccum[r][n + width * (y % accumHeight)] >= CORNER_MIN_HOUGH ) {
                            // Is this a local maxima?
                            int val = startAccum[r][n + width * (y % accumHeight)];
                            if ( val == getLocalMaxima( startAccum, r, n, y, width  ) ) {
                                startCorners.add( new Point(n, y) );
                                System.out.println( String.format( "Found corner, quality = %d, r = %d, (%d, %d)", val, r, n, y ) );
                                // imageDataSingleArray[n+width*y] = (byte) 0xff;
                            }
                        }
                        if ( endAccum[r][n + width * (y2 % accumHeight)] > CORNER_MIN_HOUGH ) {
                            // Is this a local maxima?
                            int val = endAccum[r][n + width * (y2 % accumHeight)];
                            if ( val == getLocalMaxima( endAccum, r, n, y2, width  ) ) {
                                endCorners.add( new Point(n, y2) );
                                System.out.println( String.format( "Found end corner, quality = %d, r = %d, (%d, %d)", val, r, n, y2 ) );
                                // imageDataSingleArray[n+width*y2] = (byte) 0x80;
                            }
                        }
                    }
                }
            }
            for ( int n = 0; n < perfArea.getWidth(); n++ ) {
                for ( int r = 0; r < (int) (maxRadii - minRadii); r++ ) {
                    startAccum[r][n + width * (y % accumHeight)] = 0;
                    endAccum[r][n + width * (y % accumHeight)] = 0;
                }
            }
        }
        // Find perforations
        int perfMinWidth = 160;
        int perfMaxWidth = 180;
        for ( Point sp : startCorners ) {
            for ( Point ep : endCorners ) {
                if ( ep.y - sp.y > perfMaxWidth ) {
                    break;
                }
                if ( Math.abs( ep.x - sp.x ) < 10 && ep.y - sp.y > perfMinWidth ) {
                    Perforation p = new Perforation();
                    p.x = (ep.x + sp.x ) >> 1;
                    p.y = (ep.y + sp.y ) >> 1;
                    perfCandidates.add( p );
                    imageDataSingleArray[p.x+width*p.y] = (byte) 0x40;
                    addPointToCluster( p.x, p.y );                    
                }
            }
        }
        
        System.out.println( String.format( "%d clusters:", pointClusters.size() ) );
        for ( PointCluster c : pointClusters ) {
            System.out.println( String.format("  (%d, %d) %d points", 
                    c.getCentroidX(), c.getCentroidY(), c.getPointCount() ) );
            imageDataSingleArray[c.getCentroidX()+width*c.getCentroidY()] = (byte) 0xff;
        }

        // Create a TiledIme using the SampleModel and ColorModel.
        TiledImage tiledImage = new TiledImage( 0, 0, width, height, 0, 0,
                sampleModel,
                colorModel );
        // Set the data of the tiled image to be the raster.
        tiledImage.setData( raster );
        JAI.create( "filestore", tiledImage, "debug_hough.tif", "TIFF" );
    }
    
    static private int maxClusterRadius = 7;
    
    private void addPointToCluster( int x, int y ) {
        PointCluster closestCluster = null;
        int closestClusterSqDist = Integer.MAX_VALUE;
        for ( PointCluster c : pointClusters ) {
            int sqDist = c.getSqDist(x, y);
            if ( sqDist < closestClusterSqDist ) {
                closestClusterSqDist = sqDist;
                closestCluster = c;
            }            
        }
        if ( closestClusterSqDist < maxClusterRadius * maxClusterRadius ) {
            closestCluster.addPoint(x, y);
        } else {
            PointCluster c = new PointCluster();
            c.addPoint(x, y);
            pointClusters.add( c );
        }
    }
    
    int getLocalMaxima( int[][] accum, int r, int x, int y, int width ) {
        int max = 0;
        int ris =  Math.max( 0, r-1 );
        int rie = Math.min( (int)(maxRadii-minRadii), r+1 );
        int xis = Math.max( 0, x-2 );
        int xie = Math.min( accum[0].length, x+2 );
        int yis = Math.max( 0, y-2 );
        int yie = y+2;
        int height = accum[0].length / width;
        for ( int ri = ris; ri < rie ; ri++ ) {
            for ( int xi = xis ; xi < xie ; xi++ ) {
                for ( int yi = yis ; yi < yie ; yi++ ) {
                    int i = accum[ri][xi + width * (yi%height)];
                    if ( i > max ) {
                        max = i;
                    }
                }                
            }
        }
        return max;
    }
    static final int PERF_DISTANCE = 800;
    
    
    Deque<Perforation> currentPath = new ArrayDeque<Perforation>();
    int optimalPathError = Integer.MAX_VALUE;
    
    
    private void filterPerforations() {
        perforations = new ArrayList<Perforation>();
        for ( PointCluster c : pointClusters ) {
            if ( c.getPointCount() > 10 ) {
                Perforation p = new Perforation();
                p.x = c.getCentroidX();
                p.y = c.getCentroidY();
                perforations.add( p );
            }
        }
      
        List<PerforationSeries> perfSeries = new ArrayList<PerforationSeries>();
        PerforationSeries best = null;
        int bestQuality = Integer.MAX_VALUE;
        for ( Perforation p : perforations ) {
            boolean fits = false;
            for ( PerforationSeries s : perfSeries ) {
                if ( s.addIfFits( p ) ) {
                    fits = true;
                }
            }
            if ( !fits ) {
                PerforationSeries s = new PerforationSeries();
                s.addIfFits( p );
                perfSeries.add( s );
            }
        }

        for ( PerforationSeries s : perfSeries ){
            int q = s.getQuality();
            System.out.print( "  Quality " + q );
            if ( q < bestQuality ) {
                best = s;
                bestQuality = q;
                System.out.println( " BEST" );
            } else {
                System.out.println();
            }
        }
        
        perforations =  best.getPerforations( maskImage.getHeight() );
//        for ( Perforation p : perforations ) {
//            p.x = getMedianPictureBorder( Math.max( 0, p.y-800), Math.min( maskImage.getHeight(), p.y+800) );
//        }
        System.out.println( "" + perforations.size() + " frames found" );
    }
    
    final static int frameStartX = 0;
    final static int frameWidth = 1100;
    final static int frameHeight = 800;
    
    AffineTransform getFrameXform( int frame ) {
        /**
         Estimate film rotation from max 5 perforations
         */ 
         int f1 = frame-1;
         int f2 = frame+1;
         int x1 = (f1 >= 0) ? perforations.get( f1 ).x : perforations.get(0).x;
         int x2 = (f2 < perforations.size() ) ? perforations.get( f2 ).x : perforations.get( perforations.size()-1 ).x;
         int y1 = (f1 >= 0) ?  perforations.get( f1 ).y : perforations.get(0).y;
         int y2 = (f2 < perforations.size() ) ?  perforations.get( f2 ).y : perforations.get( perforations.size()-1 ).y;
         double rot = Math.atan2((double)x2-x1, (double)(y2-y1) );
         // Translate the center of perforation to origin
         
         // AffineTransform xform = AffineTransform.getTranslateInstance( -perforations.get(frame).x, -perforations.get(frame).y );
         AffineTransform xform = new AffineTransform();
         xform.translate( 0, frameHeight/2 );
         xform.rotate( rot );
         xform.translate(-perforations.get(frame).x, -perforations.get(frame).y);
         System.out.println( String.format( "frame %d: (%d, %d), rot %f", 
                 frame,perforations.get(frame).x, -perforations.get(frame).y, rot ));
         
//         xform.preConcatenate( AffineTransform.getRotateInstance(rot));
//
//         xform.preConcatenate(AffineTransform.getTranslateInstance(0, frameHeight/2));
         return xform;
    }
    
    String fnameTmpl = "frame_%05d.tif";
    RenderedImage scanImage = null;
    
    private void saveFrame( int n ) throws IOException {            
        String fname = String.format( fnameTmpl, (Integer) n );
        
        int startY = (perforations.get( n - 1 ).y + perforations.get( n ).y) >> 1;
        int startX = perforations.get( n - 1 ).x;
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
        frame.dispose();
        rotated.dispose();
        System.gc();
        System.gc();

    }
    
    /**
     Helper function to save image into a TILL file
     @param img The image to be saved
     @param f File into which the image will be saved
     */
    private static void saveImage( RenderedImage img, File f ) {
        // Find a writer for that file extensions
        // Try to determine the file type based on extension
        String ftype = "jpg";
        String imageFname = f.getName();
        int extIndex = imageFname.lastIndexOf( "." ) + 1;
        if ( extIndex > 0 ) {
            ftype = imageFname.substring( extIndex );
        }

        ImageWriter writer = null;
        Iterator iter = ImageIO.getImageWritersBySuffix( ftype );
        writer = (ImageWriter) iter.next();

        if ( writer != null ) {
            ImageOutputStream ios = null;
            try {
                // Prepare output file
                ios = ImageIO.createImageOutputStream( f );
                writer.setOutput( ios );
                // Set some parameters
                ImageWriteParam param = writer.getDefaultWriteParam();
                writer.write( null, new IIOImage( img, null, null ), param );

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
    }
    
    private void saveFrames( RenderedImage scanImage, String fnameTmpl ) {
        RenderedOp frame = null;
        RenderedOp rotated = null;
        int frameNum = 1;
        for ( int n = 0; n < perforations.size(); n++ ) {
            String fname = String.format( fnameTmpl, (Integer) frameNum );
            System.out.println( "Saving frame " + fname );
            int startX = perforations.get( n ).x;
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
        } else {
            ddir = new File( "debug" );
            if ( !ddir.isDirectory() ) {
                ddir.mkdir();
            }

        }
        return new File( ddir, debugFname );
    }
    
    public static void usage() {
        System.err.println( "Usage: splitscan [-d|--debug] [-p projectdir] -m maskfile -o outfile_template infile" );
    }
    
    public static void parseArgs( String args[] ) {
        int n = 0;
        while ( n < args.length ) {
            if ( args[n].equals( "-d" ) || args[n].equals( "--debug" ) ) {
                isDebug = true;
                debugDir = args[n+1];
                n++;
            } else if ( args[n].equals( "-p" ) || args[n].equals( "--project" ) ) {
                projectDirName = args[n+1];
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
        File projectDir = new File( projectDirName );
        projectDir.mkdirs();
        Project prj = new Project(projectDir);
        parseArgs( args );
        IntByReference version = new IntByReference();
        Sane sane = Sane.INSTANCE;
        int ret = sane.sane_init( version, null );
        System.out.println( "Sane init returner " + ret );
        System.out.println( "Sane version = " + version.getValue() );

        // Find the right scanner
        PointerByReference dList = new PointerByReference();
        Pointer devPtr = null;
        SaneDevice dev = null;
        ret = sane.sane_get_devices( dList, true );
        int offset = 0;
        try {
            while ( (devPtr = dList.getValue().getPointer( offset )) != null ) {
                offset += Pointer.SIZE;
                SaneDeviceDescriptor d = new SaneDeviceDescriptor( devPtr );

                System.out.println( "Scanner " + d.name );
                if ( d.name.startsWith( "epson2" ) ) {
                    System.out.println( "Using device" + d.name );
                    dev = new SaneDevice( d.name );
                    break;
                }

            }
            if ( dev == null ) {
                System.err.println( "No scanner found" );
                System.exit(1);
            }
                
            dev.setOption( "mode", "Color" );
            dev.setOption( "depth", 16 );
            dev.setOption( "tl-x", new FixedPointNumber( 30 << 16 ) );
            dev.setOption( "tl-y", new FixedPointNumber( 48 << 16 ) );
            dev.setOption( "br-x", new FixedPointNumber( 190 << 16 ) );
            dev.setOption( "br-y", new FixedPointNumber( 55 << 16 ) );
            dev.setOption( "resolution", 4800 );
            dev.setOption( "source", "Transparency Unit" );
            
            // Set gamma correction
            dev.setOption("gamma-correction", "User defined");
            int[] gammaTable = new int[256];
            for ( int n = 0; n < gammaTable.length ; n++ ) {
                gammaTable[n] = n;
            }
            dev.setOption("red-gamma-table", gammaTable );
            dev.setOption("blue-gamma-table", gammaTable );
            dev.setOption("green-gamma-table", gammaTable );
        } catch ( SaneException e ) {
            System.out.println( "Error initializing Sane: " + e.getMessage() );
            System.exit( 1 );
        }

        // Initialize film mover
        FilmMover mover = new NxjFilmMover();
        System.out.println( "Initialized film mover" );
        
        log.setLevel( Level.FINE );
        JAI.setDefaultTileSize(new Dimension( 64, 64 ) );
        JAI.getDefaultInstance().setTileCache( new SunTileCache( 100*1024*1024 ) );
        JAI.getDefaultInstance().getTileScheduler().setParallelism( 4 );
        long startTime = System.currentTimeMillis();
        int scanNum = 0;
        while ( true ) {
            scanNum++;
            SplitScan t = new SplitScan();
            System.out.println( "Starting scan" );
        
            RenderedImage img = scanImage( dev );
            System.out.println( "Saving scan" );
            TiledImage scanImg = (TiledImage) img;
            System.out.println( "done, width " + img.getWidth() + " height " + img.getHeight() );
            RenderedImage maskImg = img;
            System.out.println( "Creating perforation mask" );
            if ( img.getWidth() > img.getHeight() ) {
                System.out.println( "Rotating image by 90 degrees" );
                img = t.getRotatedImage( img );
            }
            if ( maskImg.getWidth() > maskImg.getHeight() ) {
                maskImg = t.getRotatedImage( maskImg );
            }
            ScanStrip s = new ScanStrip( (RenderedOp) img );
            prj.addScanStrip( s );
            try {
            prj.save();
            } catch ( IOException e ) {
                System.err.println( e.getMessage() );
            }
            /*
            String outTmpl = String.format( "tmp/testframe_%04d_%%02d.png", scanNum );
            int frameCount = s.getFrameCount();
            for ( int n = 0 ; n < frameCount ; n++ ) {
                try {
                RenderedImage frame = s.getFrame(n);
                File f = new File( String.format( outTmpl, n) );
                System.out.println( "Saving frame " + f.getPath() );
                saveImage(frame, f);
                } catch ( Exception e ) {
                    System.out.println( "Error saving frame " + n + ": " + e.getMessage() );
                }
            }
             */
            s.dispose();
            System.gc();
            System.gc();
            System.gc();
            try {
                moveFilm( mover );
              
            } catch ( FilmMoverException e ) {
                System.err.println(e.getMessage() );
            } 
        }
    }
    
    static void moveFilm( FilmMover m ) throws FilmMoverException {
        if ( m == null ) {
            return;
        }
        m.moveFilm();
        while ( true ) {
            try {
                Thread.sleep( 1000 );
            } catch ( InterruptedException e ) {
            }
            Set<FilmMoverState> state = m.getState();
            if ( state.contains( FilmMoverState.LAST_MOVE_FINISHED ) && !state.contains( FilmMoverState.REEL_MOVING ) ) {
                break;
            }
        }
    }

    /**
     Width of the tiles used for storing the scanned iamge
     */
    static final int TILE_WIDTH = 256;
    
    /**
     Height of the tiles used for storing the scanned image
     */
    static final int TILE_HEIGHT = 256;
    
    /**
     Scan image and store it in TiledImage
     @param dev The scanner that will be user
     @return RenderedImage containing the scanned image.
     */
    static RenderedImage scanImage( SaneDevice dev ) {
        try {
            ScanParameter params = dev.getScanParameter();
            dev.startScan();
            params = dev.getScanParameter();
            
            // Read 1 strip of tiles at time
            int readSize = 
                    params.getBytesPerLine() * TILE_HEIGHT * 8 / params.getDepth();
            short[] data = new short[readSize];
            DataBufferUShort db = new DataBufferUShort( data, readSize );
            SampleModel tileSampleModel =
                    RasterFactory.createPixelInterleavedSampleModel( 
                    DataBuffer.TYPE_USHORT,
                    TILE_WIDTH, TILE_HEIGHT, 
                    3, 3 * TILE_WIDTH, new int[]{0, 1, 2} );
            ColorModel tileColorModel = PlanarImage.createColorModel( tileSampleModel );
            TiledImage tiledImage = 
                    new TiledImage( 0, 0, 
                    params.getPixelsPerLine(), params.getLines(), 
                    0, 0, tileSampleModel, tileColorModel );

            SampleModel scanSampleModel =
                    RasterFactory.createPixelInterleavedSampleModel( 
                    DataBuffer.TYPE_USHORT,
                    params.getPixelsPerLine(), TILE_HEIGHT, 
                    3, 3 * params.getPixelsPerLine(), new int[]{0, 1, 2} );
            
            int line = 0;
            while ( line < params.getLines() ) {
                int linesLeft = params.getLines() - line;
                int samplesLeft = linesLeft * params.getBytesPerLine() * 8 / params.getDepth();
                dev.read( data, Math.min( data.length, samplesLeft ) );
                Raster raster = 
                        RasterFactory.createWritableRaster( scanSampleModel, db,
                        new Point( 0, line ) );
                tiledImage.setData( raster );
                line += TILE_HEIGHT;
            }
            return tiledImage;

        } catch ( SaneException e ) {
            System.err.println( e.getMessage() );
        }
        return null;
    }
    

    private static void saveStripInfo( ScanStrip s, File file ) {
        try {
            StreamResult streamResult = new StreamResult( file );
            SAXTransformerFactory tf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
            TransformerHandler hd = tf.newTransformerHandler();
            Transformer serializer = hd.getTransformer();
            serializer.setOutputProperty( OutputKeys.ENCODING, "ISO-8859-1" );
            serializer.setOutputProperty( OutputKeys.INDENT, "yes" );
            hd.setResult( streamResult );
            hd.startDocument();
            s.writeXml( hd );
            hd.endDocument();
        } catch ( SAXException ex ) {
            Logger.getLogger( SplitScan.class.getName() ).log( Level.SEVERE, null, ex );
        } catch ( TransformerConfigurationException ex ) {
            Logger.getLogger( SplitScan.class.getName() ).log( Level.SEVERE, null, ex );
        }
    }    
}
