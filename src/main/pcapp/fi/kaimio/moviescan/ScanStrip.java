/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fi.kaimio.moviescan;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.util.ArrayList;
import java.util.List;
import javax.media.jai.Interpolation;
import javax.media.jai.KernelJAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;
import javax.media.jai.RenderedOp;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;
import javax.media.jai.operator.AffineDescriptor;
import javax.media.jai.operator.ConvolveDescriptor;
import javax.media.jai.operator.CropDescriptor;
import javax.media.jai.operator.FormatDescriptor;

/**
 ScanStrip represents strip of film scanned at once. It provides methods for 
 identifying perforations and frames in the strip as well as for getting 
 individual frames out of the scan.
 */
public class ScanStrip {
    
    PlanarImage stripImage;

    /**
     Height of a single scanned frame
     */
    static final int FRAME_HEIGHT = 800;
    
    /**
     Width of a single scanned frame
     */
    static final int FRAME_WIDTH = 800 * 4 / 3;
    /**
     Minimum radius of perforation corner rounding in pixels
     */
    double minCornerRadius = 20.0;

    /**
     Minimum radius of perforation corner rounding in pixels
     */  
    double maxCornerRadius = 25.0;

    /**
     Minimum value for image gradient that is interpreted as an edge. It seems 
     that this value does not matter much.
     */
    static final double EDGE_MIN_GRADIENT = 10.0;

    /**
     Minimum Hough transform hits needed to count a pixel as a perforation 
     corner candidate. Should be low enough so that all corners get at least 
     some hits - there are other mechanisms in place to remove false positives.
     */
    static final int CORNER_MIN_HOUGH = 6;
    
    /**
    Minimum distance between the center points of upper and lowe corners 
    that are considered to belong to same perforation
     */
    static final int CC_MIN_DIST = 160;
    
    /**
    Minimum distance between the center points of upper and lowe corners 
    that are considered to belong to same perforation
     */
    static final int CC_MAX_DIST = 180;
    
    /**
     Radius of a point cluster. Perforation location candidates that
     are within this discance of the cluster centroid are considered to belong 
     to the same cluster
     */
    static private int MAX_CLUSTER_RADIUS = 7;
    
    /**
     Minimum number of candidate positions needed in a cluster before is is 
     considered a real perforation
     */
    static int MIN_POINTS_IN_CLUSTER = 10;
    
    /**
     LIst of cluster of åerforation location candidates close to each other
     */
    List<PointCluster> pointClusters = new ArrayList<PointCluster>();
    
    /**
     List ofthe perforations found.
     */
    List<Perforation> perforations;
    
    
    /**
     Constructs a new ScanStrip object
     @param img Image of the scan
     */
    public ScanStrip( PlanarImage img ) {
        stripImage = img;
    }
    
    public int getFrameCount() {
        if ( perforations == null ) {
            findPerforations();
        }
        return perforations.size();
    }
    
    /**
     Get the nth frame of the scan
     @param n frame to get
     @return RemderedImage based on the sanned strip, rotated and cropped 
     according to frame dimesions. 
     */
    public RenderedImage getFrame( int n ) {
        if ( perforations == null ) {
            findPerforations();
        }
        
        // Calculate the top left corner from perforations
        int tlx = (perforations.get( n - 1 ).y + perforations.get( n ).y) >> 1;
        int tly = perforations.get( n - 1 ).x;
        int w = Math.min( FRAME_WIDTH, stripImage.getWidth() - tly );
        AffineTransform xform = getFrameXform( n );
        RenderedOp rotated = 
                AffineDescriptor.create( stripImage, xform, 
                Interpolation.getInstance( Interpolation.INTERP_BICUBIC ), 
                null, null );
        int minx = rotated.getMinX();
        int miny = rotated.getMinY();
        int rw = rotated.getWidth();
        int rh = rotated.getHeight();
        RenderedOp frame = 
                CropDescriptor.create( rotated, (float) 0, (float) 0, 
                (float) w, (float) FRAME_HEIGHT, null );
        return frame;
    }
    
    /**
     Free the associated resources (i.e. the image of the scnaned strip
     */
    public void dispose() {
        stripImage.dispose();
        stripImage = null;
    }

    /**
     Calculate the affine transform from scanStrip to a single frame (frame 
     rotated to straight position, top left corner translated to (0,0)
     @param frame
     @return
     */
    AffineTransform getFrameXform( int frame ) {
        /**
         Estimate film rotation from max 5 perforations
         */ 
         int f1 = frame-1;
         int f2 = frame+1;
         int x1 = (f1 >= 0) ? perforations.get( f1 ).x : perforations.get(0).x;
         int x2 = (f2 < perforations.size() ) ? 
             perforations.get( f2 ).x : perforations.get( perforations.size()-1 ).x;
         int y1 = (f1 >= 0) ?  perforations.get( f1 ).y : perforations.get(0).y;
         int y2 = (f2 < perforations.size() ) ?  
             perforations.get( f2 ).y : perforations.get( perforations.size()-1 ).y;
         double rot = Math.atan2((double)x2-x1, (double)(y2-y1) );
         // Translate the center of perforation to origin
         
         AffineTransform xform = new AffineTransform();
         xform.translate( 0, FRAME_HEIGHT/2 );
         xform.rotate( rot );
         xform.translate(-perforations.get(frame).x, -perforations.get(frame).y);
         System.out.println( String.format( "frame %d: (%d, %d), rot %f", 
                 frame,perforations.get(frame).x, -perforations.get(frame).y, rot ));         
         return xform;
    }
    
    private void findPerforations() {
        houghTransform();
        filterPerforations();
    }

    /**
     Try to find perforation corners using (modified) Hough transform. After the
     hough transform, matching pairs of top and bottom corners are found and
     clustered into pointClusterws list.
     */
    void houghTransform() {
        
        // Siebel transform of stripImage
        KernelJAI sxKernel = new KernelJAI( 3 , 3 , 
                new float[]{-1.0f, 0.0f, 1.0f,
                            -2.0f, 0.0f, 2.0f,
                            -1.0f, 0.0f, 1.0f });
        KernelJAI syKernel = new KernelJAI( 3 , 3 , 
                new float[]{-1.0f, -2.0f, -1.0f,
                             0.0f,  0.0f,  0.0f,
                             1.0f,  2.0f,  1.0f });
        
        RenderedImage dblImg = FormatDescriptor.create( stripImage, DataBuffer.TYPE_DOUBLE, null );
        RenderedImage sxImg = ConvolveDescriptor.create(dblImg, sxKernel, null );
        RenderedImage syImg = ConvolveDescriptor.create(dblImg, syKernel, null );

        SampleModel sm = sxImg.getSampleModel(  );
        int nbands = sm.getNumBands(  );
        double[] sxPixel = new double[nbands];
        double[] syPixel = new double[nbands];
        
        /*
         We are interested only in the left side of the strip as the 
         perforations are there
         */
        Rectangle perfArea = 
                new Rectangle(0, 0, 
                stripImage.getWidth()/4, stripImage.getHeight() );
        RectIter sxIter = RectIterFactory.create(sxImg, perfArea );
        RectIter syIter = RectIterFactory.create(syImg, perfArea );
        
        int width = (int)perfArea.getWidth();
        int height = (int)perfArea.getHeight();
        
        /*
         We use 2 accumulators - one for detecting the upper right corner,
         one for lower right corner. As the original is huge and the detaile we 
         are looking for are tiny, we use a sliding window that stores only the 
         relevant part of accumulator.
         */
        int accumHeight = (int) maxCornerRadius + 2;
        int[][] startAccum = 
                new int[(int) (maxCornerRadius - minCornerRadius)][width*accumHeight];
        int[][] endAccum = 
                new int[(int) (maxCornerRadius - minCornerRadius)][width*accumHeight];
        
        /*
         Debugging image - get rid of this!!!
         */
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

        List<Point> startCorners = new ArrayList<Point>();
        List<Point> endCorners = new ArrayList<Point>();
        int y = 0;
        int maxVal = 0;
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
                double isq = sxPixel[0] * sxPixel[0] + syPixel[0] * syPixel[0];
                if ( isq > EDGE_MIN_GRADIENT * EDGE_MIN_GRADIENT ) {
                    // This seems like a border
                    if ( syPixel[0] <= 0 && sxPixel[0] >= 0 ) {
                        // Upper right corner candidate
                        double intensity = Math.sqrt(isq);
                        for ( double r = minCornerRadius; r < maxCornerRadius ; r+= 1.0 ) {                    
                            double cx = (double)x - r * sxPixel[0] / intensity;
                            double cy = (double)y - r * syPixel[0] / intensity;
                            if ( cx > 0.0 ) {
                                int accumLine = (int)cy % accumHeight;
                                startAccum[(int)(r-minCornerRadius)][(int)cx + width * accumLine]++;
                                if ( startAccum[(int)(r-minCornerRadius)][(int)cx + width * accumLine] > maxVal ) {
                                    maxVal = startAccum[(int)(r-minCornerRadius)][(int)cx + width * accumLine];
                                }
                            }
                        }
                    } 
                    if ( syPixel[0] >= 0 && sxPixel[0] >= 0 ) {
                        // Lower right corner candidate
                        double intensity = Math.sqrt(isq);
                        for ( double r = minCornerRadius; r < maxCornerRadius ; r+= 1.0 ) {
                            double cx = (double)x - r * sxPixel[0] / intensity;
                            double cy = (double)y - r * syPixel[0] / intensity;
                            if ( cx > 0.0 && cy > 0.0 ) {
                                int accumLine = (int)cy % accumHeight;
                                endAccum[(int)(r-minCornerRadius)][(int)cx + width * accumLine]++;
                                if ( endAccum[(int)(r-minCornerRadius)][(int)cx + width * accumLine] > maxVal ) {
                                    maxVal = endAccum[(int)(r-minCornerRadius)][(int)cx + width * accumLine];
                                }
                            }
                        }
                    }  
                }
                x++;
            }
            y++;
            
            /*
             1 line processed - check if there are corner candidates in the 
             accumulator line we are going to overwrite
             */
            int y2 = y - accumHeight;
            int l = y % accumHeight;
            if ( y2 > 0 ) {
                for ( int n = 0; n < perfArea.getWidth(); n++ ) {
                    for ( int r = 0; r < (int) (maxCornerRadius - minCornerRadius); r++ ) {
                        if ( startAccum[r][n + width * l] >= CORNER_MIN_HOUGH ) {
                            // Is this a local maxima?
                            int val = startAccum[r][n + width * l];
                            if ( val == getLocalMaxima( startAccum, r, n, y, width  ) ) {
                                startCorners.add( new Point(n, y) );
                                System.out.println( String.format( "Found corner, quality = %d, r = %d, (%d, %d)", val, r, n, y ) );
                                // imageDataSingleArray[n+width*y] = (byte) 0xff;
                            }
                        }
                        if ( endAccum[r][n + width * l] > CORNER_MIN_HOUGH ) {
                            // Is this a local maxima?
                            int val = endAccum[r][n + width * l];
                            if ( val == getLocalMaxima( endAccum, r, n, y2, width  ) ) {
                                endCorners.add( new Point(n, y2) );
                                System.out.println( String.format( "Found end corner, quality = %d, r = %d, (%d, %d)", val, r, n, y2 ) );
                                // imageDataSingleArray[n+width*y2] = (byte) 0x80;
                            }
                        }
                    }
                }
            }
            // Zero the line just analyzed - it will be reused for the next line
            for ( int n = 0; n < perfArea.getWidth(); n++ ) {
                for ( int r = 0; r < (int) (maxCornerRadius - minCornerRadius); r++ ) {
                    startAccum[r][n + width * (y % accumHeight)] = 0;
                    endAccum[r][n + width * (y % accumHeight)] = 0;
                }
            }
        }
        /*
         Find perforations, i.e. pairs of start and end corners that are within
         the specified range from each other
         */
        for ( Point sp : startCorners ) {
            for ( Point ep : endCorners ) {
                if ( ep.y - sp.y > CC_MAX_DIST ) {
                    break;
                }
                if ( Math.abs( ep.x - sp.x ) < 10 && ep.y - sp.y > CC_MIN_DIST ) {
                    Perforation p = new Perforation();
                    p.x = (ep.x + sp.x ) >> 1;
                    p.y = (ep.y + sp.y ) >> 1;
                    // imageDataSingleArray[p.x+width*p.y] = (byte) 0x40;
                    addPointToCluster( p.x, p.y );                    
                }
            }
        }
        
        System.out.println( String.format( "%d clusters:", pointClusters.size() ) );
        for ( PointCluster c : pointClusters ) {
            System.out.println( String.format("  (%d, %d) %d points", 
                    c.getCentroidX(), c.getCentroidY(), c.getPointCount() ) );
            // imageDataSingleArray[c.getCentroidX()+width*c.getCentroidY()] = (byte) 0xff;
        }

    }

    /**
     Helper function to find the local maxima in the accumulator in the 
     neighborhood of a pixel.
     @param accum The accumulator we are looking at
     @param r Index if the accumulator array corresponding for the radius we are 
     looking for.
     @param x X coordinate of the pixel
     @param y Y coordinate of the pixel
     @param width Width of the window 
     @return Return Maximum value in the neighborhood
     */
    int getLocalMaxima( int[][] accum, int r, int x, int y, int width ) {
        int max = 0;
        int ris = Math.max( 0, r - 1 );
        int rie = Math.min( (int) (maxCornerRadius - minCornerRadius), r + 1 );
        int xis = Math.max( 0, x - 2 );
        int xie = Math.min( accum[0].length, x + 2 );
        int yis = Math.max( 0, y - 2 );
        int yie = y + 2;
        int height = accum[0].length / width;
        for ( int ri = ris; ri < rie; ri++ ) {
            for ( int xi = xis; xi < xie; xi++ ) {
                for ( int yi = yis; yi < yie; yi++ ) {
                    int i = accum[ri][xi + width * (yi % height)];
                    if ( i > max ) {
                        max = i;
                    }
                }
            }
        }
        return max;
    }

    /**
     Chieck if a point is close to a known cluster in pointCLusters list add 
     adds it there, or creates a new cluster if there are no clusters nearby.
     @param x X coordinate of the point
     @param y Y coordinate of the point
     */
    private void addPointToCluster( int x, int y ) {
        PointCluster closestCluster = null;
        int closestClusterSqDist = Integer.MAX_VALUE;
        for ( PointCluster c : pointClusters ) {
            int sqDist = c.getSqDist( x, y );
            if ( sqDist < closestClusterSqDist ) {
                closestClusterSqDist = sqDist;
                closestCluster = c;
            }
        }
        if ( closestClusterSqDist < MAX_CLUSTER_RADIUS * MAX_CLUSTER_RADIUS ) {
            closestCluster.addPoint( x, y );
        } else {
            PointCluster c = new PointCluster();
            c.addPoint( x, y );
            pointClusters.add( c );
        }
    }
    
    /**
     Determine the most likely locations of the perforations based on clusters
     of location candidates found by the Hough transform. Stores the best found
     perforation series in perforations.
     */
    private void filterPerforations() {
        perforations = new ArrayList<Perforation>();
        
        /*
         Find the clusters that have enough hits to be considered as perofrations
         */
        for ( PointCluster c : pointClusters ) {
            if ( c.getPointCount() > MIN_POINTS_IN_CLUSTER ) {
                Perforation p = new Perforation();
                p.x = c.getCentroidX();
                p.y = c.getCentroidY();
                perforations.add( p );
            }
        }
      
        /*
         Create candidate series from the perforations and select the one that 
         looks best
         */
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
        
        // Select the best series
        perforations =  best.getPerforations( stripImage.getHeight() );
        System.out.println( "" + perforations.size() + " frames found" );
    }
    
}