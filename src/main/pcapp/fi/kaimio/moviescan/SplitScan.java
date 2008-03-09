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
import java.awt.image.BufferedImage;
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
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
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
import javax.media.jai.operator.BandSelectDescriptor;
import javax.media.jai.operator.ConvolveDescriptor;
import javax.media.jai.operator.CropDescriptor;
import javax.media.jai.operator.FormatDescriptor;
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
    
    private RenderedImage findPerforationEdges( RenderedImage img ) {
        maskImage = img;
        RenderedOp redBand = BandSelectDescriptor.create( img, new int[] {0}, null );
        RenderedOp greenBand = BandSelectDescriptor.create( img, new int[] {1}, null );
        RenderedOp blueBand = BandSelectDescriptor.create( img, new int[] {2}, null );
        RenderedOp minrg = MinDescriptor.create(redBand, greenBand, null );
        RenderedOp min = MinDescriptor.create(minrg, blueBand, null );
        RenderedOp maxf = MaxFilterDescriptor.create( min, MaxFilterDescriptor.MAX_MASK_SQUARE, 10, null );
        return maxf;
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
    
    List<Perforation> perforations = new ArrayList<Perforation>();
    List<Perforation> perfCandidates = new ArrayList<Perforation>();
    
    // List<Integer> perfY = new ArrayList<Integer>(  );
    // List<Integer> perfX = new ArrayList<Integer>(  );
    static final int PERF_HOLE_THRESHOLD = 50;
    static final int MIN_LINES = 120;
    static final int MEDIAN_WINDOW = 100;
    
    static final int WHITE_THRESHOLD = 64000;
    static final int BLACK_THRESHOLD = 3000;
    private int getColorCategory( int level ) {
        if ( level < BLACK_THRESHOLD ) {
            return 0;
        }
        if ( level > WHITE_THRESHOLD ) {
            return 2;
        }
        return 1;
    }
    
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
        
//        System.out.println( String.format( "%d perforations found", perfCandidates.size()));
//        int clusterCount = 34;
//        int[] cx = new int[clusterCount];
//        int[] cy = new int[clusterCount];
//        for ( int n = 0; n < clusterCount ; n++ ) {
//            cx[n] = 100;
//            cy[n] = n * 800 + 200;
//        }
//        KMeans km = new KMeans( perfCandidates, clusterCount, cx, cy );
//        for ( int n = 0 ; n < km.getClusterCount() ; n++ ) {
//            int kx = km.getCentroidX(n);
//            int ky = km.getCentroidY(n);
//            if ( kx < width && ky < img.getHeight() ) {
//                 imageDataSingleArray[kx+width*ky] = (byte) 0xff;                
//            }
//        }
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
    
    int[] pictureBorder;
    
    private void findPerfHolePoints( RenderedImage img ) {
        perfBorderX = new int[ img.getHeight()];
        /**
         The perforations should be in the 1/3 of the leftmost image.
         */
        Rectangle perfArea = new Rectangle(0, 0, img.getWidth()/4, img.getHeight() );
        RectIter iter = RectIterFactory.create(img, perfArea );
        SampleModel sm = img.getSampleModel(  );
        int nbands = sm.getNumBands(  );
        pictureBorder = new int[(int)perfArea.getHeight() ];
        int[] pixel = new int[nbands];
        int x=0, y=-1;
        int perfStartY = -1;
        int perfEndY = -1;
        boolean isPerforation = false;
        int linesToDecide = -1;
        int perfAreaWidth = (int) perfArea.getWidth();
        int lastLinePixels[] = new int[perfAreaWidth];
        int lastWhiteStarts[] = new int[perfAreaWidth];
        int lastBlackStart[] =  new int[perfAreaWidth];
        int[] lastLines = new int[MEDIAN_WINDOW];        
        int[] firstRunStartWindow = new int[MEDIAN_WINDOW];
        for ( int n = 0 ; n < MEDIAN_WINDOW ; n++ ) {
            firstRunStartWindow[n] = (int) perfArea.getWidth();
        }
        int n = 0;
        int lastPerfStartedAtLine = 0;
        System.out.println( "Finding perforations..." );
        int lastLineRunStart = perfAreaWidth;
        
        Set<Perforation> unfinishedPerfs = new HashSet<Perforation>();
        while ( !iter.nextLineDone() ) {
            y++;
            int pixelsInLine = 0;
            int firstRunStart = perfAreaWidth;
            perfBorderX[y] = 0;
            x = 0;
            iter.startPixels();
            int runLength = 0;
            int runStart = Integer.MAX_VALUE;
            
            // X coordinate of last non-white pixel
            int lastNonWhite = 0;
            // X coordinate of last non-black pixel
            int lastNonBlack = 0;
            // X coordinate where the last black are ends
            int blackEnd = perfAreaWidth;
            // X coordinate where the last white area ends
            int whiteEnd = perfAreaWidth;
            // Histogram of number of black, "grey" and white pixels 50 pixels back
            int histBack[] = new int[3];
            int histFwd[] = new int[3];
            boolean pictureBorderFound = false;
            while( !iter.nextPixelDone()  ) {
                iter.getPixel( pixel );
                                
                if ( pixel[0] > WHITE_THRESHOLD ) {
                    if ( lastLinePixels[x] <= WHITE_THRESHOLD ) {
                        lastWhiteStarts[x] = y;
                    }
                } else {
                    if ( lastLinePixels[x] > WHITE_THRESHOLD ) {
                        lastBlackStart[x] = y;
                    }
                }
                lastLinePixels[x] = pixel[0];
                
                // Update the histogram windows, "back" for columns [x-60,x-30], "fwd" for [x-29, x]
                if ( x >= 60 ) {
                    int p = getColorCategory( lastLinePixels[x-60] );
                    histBack[p]--;
                }
                if ( x >= 30 ) {
                    int p = getColorCategory( lastLinePixels[x-30] );
                    histBack[p]++;
                    histFwd[p]--;
                }
                int p = getColorCategory( lastLinePixels[x] );
                histFwd[p]++;
                
                if ( x > 60 && !pictureBorderFound ) {
                    if ( histBack[0] < 2 && histBack[1] < 2 && histFwd[2] < 2 ) {
                        // Only white backwards, no white ahead -> picture border
                        pictureBorder[y] = x;
                        pictureBorderFound = true;
                    } else if ( histBack[2] < 2 && histBack[1] < 2 && histFwd[0] < 2 ) {
                        // Only black backwards, no black ahead -> picture border
                        pictureBorder[y] = x;
                        pictureBorderFound = true;
                    }
                }                
                if ( pixel[0] > WHITE_THRESHOLD ) {
                    if ( runStart > x ) {
                        runStart = x;
                        runLength++;
                    }
                    pixelsInLine++;
                    if ( runLength > PERF_HOLE_THRESHOLD && firstRunStart > runStart ) {
                        firstRunStart = runStart;
                    }
                } else if ( pixelsInLine > PERF_HOLE_THRESHOLD ) {
                    /*
                     There are enough white pixels in this line that 
                     this looks like a perforation. Store the right border &
                     continue
                     */
                    perfBorderX[y] = x;
                }
                x++;
            }
            
            /*
             Process this line for ongoing perforations
             */
            Set<Perforation> finished = new HashSet<Perforation>();
            for ( Perforation p : unfinishedPerfs ) {
                if ( p.processLine(lastWhiteStarts, lastBlackStart, y) ) {
                    
                    Perforation lastP = p; 
                    if ( perforations.size() > 0 ) {
                        lastP = perforations.get( perforations.size() - 1 );
                    }
                    perforations.add( p );
                    finished.add( p );
                    System.out.println(
                            String.format( "Found perforation at (%d, %d) %+d, %+d",
                            p.x, p.y,
                            p.x - lastP.x, p.y - lastP.y ) );
                    // Save image of the perforation
                    int imageY = p.y - 200;
                    imageY = Math.max( 0, imageY );
                    saveDebugImage( maskImage, "hole",
                            0, imageY,
                            300, Math.min( 400, maskImage.getHeight() - imageY ) );
                }
            }
            unfinishedPerfs.removeAll( finished );
            
            /* 
             Calculate the number of columns in which a white area (possible 
             perforation) has started in last 10 lines
            */
            int whiteStarted = 0;
            int my = Math.max( y-10, lastPerfStartedAtLine+2 );
            for ( int whiteStartLine : lastWhiteStarts ) {
                if ( whiteStartLine > my ) {
                    whiteStarted++;
                }
            }
            if ( whiteStarted > 50 ) {
                Perforation perf = new Perforation( lastWhiteStarts, y );
                System.out.println( "Started new perforation at line " + y );
                unfinishedPerfs.add( perf );
                lastPerfStartedAtLine = y;
            }
        }
        
        saveBorder( pictureBorder );
    }
    
    static final int PERF_DIST_TOL_PIXELS = 20;
    static final int PERF_DISTANCE = 800;
    static private double Y_TOL = 0.025;
    static private double MAX_K = 20.0 / 800.0;
    
    
    Deque<Perforation> currentPath = new ArrayDeque<Perforation>();
    int optimalPathError = Integer.MAX_VALUE;
    
    private void findPerfPath( Perforation p, Perforation p1, Perforation p2, int maxDdx, int missCount ) {
        // Are there perforations missing between last and current perforations?
        currentPath.addLast( p );
        if ( currentPath.size() > 40 ) {
            System.out.println( "Path length " + currentPath.size() );
        }
        int prevy = (p1 != null ) ? p1.y : 0;
        int dy = p.y - prevy;
        int numFrames = (int) Math.round( (double) dy / PERF_DISTANCE );
        if ( numFrames < 1 ) {
            numFrames = 1;
        } 
        missCount += numFrames-1;
        
        // Calculate the 2nd derivate maximum
        if ( p2 != null ) {
            int ddx = Math.abs( p2.x + p.x - 2 * p1.x );
            maxDdx = Math.max( ddx, maxDdx );
        }

        // How many frames are missing from the series?
        int framesAfter = ( maskImage.getHeight() - p.y ) / PERF_DISTANCE;
        
        int q = maxDdx + 10 * (missCount+ framesAfter);
        
        if ( q < optimalPathError ) {
            optimalPathError = q;
            System.out.print( "Error " + q + ": " );
            for ( Perforation perf : currentPath ) {
                System.out.print(  String.format( "(%d, %d) ", perf.x, perf.y ) );
            }
            System.out.println();
        }
        
        // Find the optimal path from all possible continuations
        
        for ( Perforation nextPerf : p.getNextPerfCandidates() ) {
            findPerfPath(nextPerf, p, p1, maxDdx, missCount);
        }
        currentPath.removeLast();
    }
    
    
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
    
    private int getMedianPictureBorder( int start, int end ) {
        int[] points = Arrays.copyOfRange(pictureBorder, start, end);
        Arrays.sort(points);
        int firstNonNull = 0;
        while ( firstNonNull < points.length && points[firstNonNull] == 0 ) {
            firstNonNull++;
        }
        return ( firstNonNull < points.length ) ? points[firstNonNull+points.length >> 1] : 0;
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

            t.houghTransform( maskImg );
            t.filterPerforations();
            long analysisTime = System.currentTimeMillis() - startTime;
            System.out.println( "Image analyzed in " + ((double) analysisTime) / 1000.0 );
            String outTmpl = String.format( "tmp/frame_%04d_%%02d.png", scanNum );
            System.out.printf("file name templace %s\n", outTmpl );
            t.fnameTmpl = outTmpl;
            t.saveFrames( img, outTmpl );
            scanImg.dispose();
            System.gc();
            System.gc();
            System.gc();
            long saveTime = System.currentTimeMillis() - analysisTime - startTime;
            System.out.println( "Images saved in " + ((double) saveTime) / 1000.0 );
            try {
                moveFilm( mover );
              
            } catch ( FilmMoverException e ) {
                System.err.println(e.getMessage() );
            } 
        }
    }
    
    static void moveFilm( FilmMover m ) throws FilmMoverException {
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

    static RenderedImage scanImage( SaneDevice dev ) {
        try {
            ScanParameter params = dev.getScanParameter();
            dev.startScan();
            params = dev.getScanParameter();
            int size = params.getBytesPerLine() * params.getLines() * 8 / params.getDepth();
            short[] data = new short[size];
            dev.read( data );
            DataBufferUShort db = new DataBufferUShort( data, size );
            SampleModel sampleModel =
                    RasterFactory.createPixelInterleavedSampleModel( DataBuffer.TYPE_USHORT,
                    params.getPixelsPerLine(),
                    params.getLines(), 3, 3 * params.getPixelsPerLine(), new int[]{0, 1, 2} );
            // Create a compatible ColorModel.
            ColorModel colorModel = PlanarImage.createColorModel( sampleModel );
            // Create a WritableRaster.
            Raster raster = RasterFactory.createWritableRaster( sampleModel, db,
                    new Point( 0, 0 ) );
            TiledImage tiledImage = new TiledImage( 0, 0, params.getPixelsPerLine(), params.getLines(), 0, 0,
                    sampleModel,
                    colorModel );
            // Set the data of the tiled image to be the raster.
            tiledImage.setData( raster );
            return tiledImage;

        } catch ( SaneException e ) {
            System.err.println( e.getMessage() );
        }
        return null;
    }
}
