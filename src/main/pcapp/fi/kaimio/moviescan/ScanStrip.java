/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fi.kaimio.moviescan;

import com.sun.media.jai.operator.ImageReadDescriptor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.KernelJAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;
import javax.media.jai.RenderedOp;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;
import javax.media.jai.operator.AffineDescriptor;
import javax.media.jai.operator.ConstantDescriptor;
import javax.media.jai.operator.ConvolveDescriptor;
import javax.media.jai.operator.FormatDescriptor;
import javax.media.jai.operator.OverlayDescriptor;
import javax.xml.transform.sax.TransformerHandler;
import org.apache.commons.digester.Digester;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 ScanStrip represents a strip of film scanned at once. It provides methods for 
 identifying perforations and frames in the strip as well as for getting 
 individual frames out of the scan.
 <p>
 
 <h2>Numbering of Frames</h2>
 
 Numbering of frames in strip is a bit complicated. First, the perforations are 
 nubmered from 0 to n, starting from "topmost" (i.e. first in time) perforation.
 The position and orientation of frame is determined based on 3 perforations, so 
 no frames are created for first and last perforations. So there are n-2 frames 
 and perforation n corresponds to frame n-1.
 <p>
 
 In addition to this it is possible to disable ther first or last frames with
 setFirstUsableFrame() and setLastUsableFrame() methods (if there are duplicate 
 frames in strips or the ends of the strip are not usable for other reason. 
 This does not affect frame numbering, impact of these is handled in {@link Scene}
 and in particular {@link FrameRange} classes.
 */
public class ScanStrip {
   
    private static Log log = LogFactory.getLog( ScanStrip.class.getName() );
    
    PlanarImage stripImage;
    
    int imageOrientation = 0;

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
     Order number of the first usable frame in the strip. Negative value indicates 
     that the value has not been set; in that case it willb e initialized to the 
     first full frame in the strip.
     */
    int firstUsableFrame = -1;
    
    /**
     Order number of the last usable frame in the strip
     */
    int lastUsableFrame = -1;
    
    private ScanAnalysisListener analysisListener;
    
    /**
     Constructs a new ScanStrip object
     @param img Image of the scan
     */
    public ScanStrip( PlanarImage img ) {
        stripImage = img;
    }

    public ScanStrip() {
        stripImage = null;
    }
    
    /**
     Factory method for creating a strip based on an image. The image is analyzed 
     and found perforations are added to the created iamge. In addition, caller 
     can supply an object that implements {@link ScanAnalysisListener} that will be
     notified of the progress of analysis.
     <p>
     Note that analysis will take considerable time!!!
     
     @param img image of the scan strip
     @param l Optional listener
     @return SCanStrip constructed from the image.
     */
    static public ScanStrip create( PlanarImage img, ScanAnalysisListener l ) {
        ScanStrip s = new ScanStrip( img );
        s.setAnalysisListener( l );
        s.findPerforations();
        return s;
    }
    
    private void setAnalysisListener( ScanAnalysisListener l ) {
        this.analysisListener = l;
    }
    /**
     Listeners that will be notified of changes
     */
    Set<ScanStripListener> listeners = new HashSet<ScanStripListener>();
    
    /**
     Add a new listener that will be notified of changes
     @param l The new listener
     */
    public void addScanStripListener( ScanStripListener l ) {
        listeners.add( l );
    }
    
    /**
    Remove a listener 
     @param l The listener to be removed
     */
    public void removeScanStripListener( ScanStripListener l ) {
        listeners.remove( l );
    }

    /**
     Notify all registered listeners about changes
     */
    private void notifyListeners() {
        for ( ScanStripListener l : listeners ) {
            l.scanStripChanged( this );
        }
    }
    
    String name;
    
    /**
     Set the name (in practice the name in which inforation about this strip is 
     saved) of this strip.
     
     @param name Name of the string
     */
    public void setName( String name ) {
        this.name = name;
    }

    /**
     Get the name of this strip
     @return
     */
    public String getName(  ) {
        return name;
    }
    
    File file;
    
    public void setFile( File f ) {
        file = f;
    }
    
    public File getFile() {
        return file;
    }

    public int getFrameCount() {
        if ( perforations == null ) {
            findPerforations();
        }
        // For now, assume that the last frame is not complete
        return perforations.size()-1;
    }
    
    /**
     Get the first frame from this strip that is usable
     @return Order number of the last usable frame
     */
    public int getFirstUsable() {
        if ( firstUsableFrame < 0 ) {
            firstUsableFrame =  
                    ( perforations.get(0).y > FRAME_HEIGHT/2 + 10 ) ? 0 : 1;
        }
        return firstUsableFrame;
    }

    /**
     Set the first usable frame from thsi strip
     @param frame Order nubmer of the first usable frame
     */
    public void setFirstUsable( int frame ) {
        firstUsableFrame = frame;
        notifyListeners();
    }
    
    /**
     Get the last frame from this strip that is usable
     @return Order number of the last usable frame
     */
    public int getLastUsable() {
        if ( lastUsableFrame < 0 ) {
            lastUsableFrame = getFrameCount()-1;
        }
        return lastUsableFrame;
    }

    /**
     Set the first usable frame from thsi strip
     @param frame Order nubmer of the first usable frame
     */
    public void setLastUsable( int frame ) {
        lastUsableFrame = frame;
        notifyListeners();
    }
    
    /**
     Depending on the exact alignment of this strip, the frame that corresponds 
     to the first perforation can be either full or partial. This function 
     calculates the order number of the preforation that corresponds to the first 
     <b>full</b> frame.
     @return Offset of the first frame
     */
    private int getFrameOffset() {
        return 0;
    }

    /**
     Check whether the frame that corresponds to the last perforation in this 
     strip is full
     @return
     */
    private boolean hasLastPerfFullFrame() {
        return false;
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
        
        AffineTransform xform = getFrameXform( n );
        RenderedOp rotated = 
                AffineDescriptor.create( stripImage, xform, 
                Interpolation.getInstance( Interpolation.INTERP_BICUBIC ), 
                null, null );
        

        ImageLayout layout = new ImageLayout();

        layout.setColorModel( stripImage.getColorModel() );
        RenderingHints hints = new RenderingHints( JAI.KEY_IMAGE_LAYOUT, layout );
        RenderedOp background =
                ConstantDescriptor.create( (float) FRAME_WIDTH,
                (float) FRAME_HEIGHT, new Short[]{0, 0, 0}, null );
        RenderedOp frame = OverlayDescriptor.create( background, rotated, hints );

        return frame;
    }
    
    
    /**
     Get image of the whole scan strip. Note that this method is not guaranteed 
     to succeed unless caller has called reserveStripImage first.
     @return RenderedImage of the scan strip.
     */
    public RenderedImage getStripImage() {
        return stripImage;
    }
    
    /**
     Free the associated resources (i.e. the image of the scanned strip
     */
    public void dispose() {
        stripImage.dispose();
        stripImage = null;
    }

    /**
     Add a perforation to the end of perforation series
     
     @param x X coordiante of the perforation
     @param y Y coordinate of the perforation
     */
    public void addPerforation( int x, int y ) {
        System.out.printf( "addPerforation %d, %d\n", x, y);
        if (perforations == null ) {
            perforations = new ArrayList<Perforation>();
        }
        Perforation p = new Perforation( x, y );
        perforations.add( p );
        notifyListeners();
    }
    
    /**
     Set the location of bnth perforation
     @param n Number of the perforation to set
     @param x New X coordiante
     @param y New Y coordinate
     */
    public void setPerforation( int n, int x, int y ) {
        perforations.set( n, new Perforation( x, y ) );
    }
    
    /**
     Get location of a perforation
     @param n Order number of the perofration hole
     @return 
     */
    public Perforation getPerforation( int n ) {
        if (perforations == null ) {
            findPerforations();
        }
        return perforations.get( n );
    }
    
    public int getPerforationCount() {
        if (perforations == null ) {
            findPerforations();
        }
        return perforations.size();
    }
    
    public List<Perforation> getPerforations() {
        return perforations;
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
//         System.out.println( String.format( "frame %d: (%d, %d), rot %f", 
//                 frame,perforations.get(frame).x, -perforations.get(frame).y, rot ));         
         return xform;
    }



    void setOrientation( int i ) {
        imageOrientation = i;
    }
    
    private void findPerforations() {
        houghTransform();
        filterPerforations();
        if ( analysisListener != null ) {
            analysisListener.scanAnalysisComplete( perforations.size() );
        }
        notifyListeners();
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

        List<Point> startCorners = new ArrayList<Point>();
        List<Point> endCorners = new ArrayList<Point>();
        int y = 0;
        int maxVal = 0;
        if ( analysisListener != null ) {
            analysisListener.scanAnalysisProgress( 0, height );
        }
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
            if ( (y % 100 == 1) &&  analysisListener != null ) {
                analysisListener.scanAnalysisProgress( y-1, height );
            }
        }

        if ( analysisListener != null ) {
            analysisListener.scanAnalysisProgress( height, height );
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
         looks best77
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

    public void writeXml( TransformerHandler hd ) throws SAXException {
        if ( perforations == null ) {
            findPerforations();
        }

        AttributesImpl atts = new AttributesImpl();
        atts.addAttribute("", "", "orientation", "CDATA", "0");
        atts.addAttribute("", "", "gamma", "CDATA", "1.0" );
        hd.startElement( "", "", "scan", atts );
        atts.clear();
        hd.startElement( "", "", "perforations", atts );
        for ( Perforation p: perforations ) {
            atts.addAttribute("", "", "x", "CDATA", Integer.toString( p.x ));
            atts.addAttribute("", "", "y", "CDATA", Integer.toString( p.y ));
            hd.startElement("", "", "perforation", atts );
            hd.endElement("", "", "perforation" );
        }
        hd.endElement( "", "", "perforations" );
        hd.endElement("", "", "scan");
        
        hd.endDocument();
    }
    
    /**
     Load a strip from file
     @param f The xml file that describes the strip
     @return
     */
    static public ScanStrip loadStrip( File descFile, File imgFile ) {
        ScanStrip strip = null;
        Digester d = new Digester();
        d.addRuleSet( new ScanStripRuleSet( "" ) );
        try {
            strip = (ScanStrip) d.parse( descFile );
            strip.setFile(imgFile);
        } catch ( Exception e ) {

        }
        return strip;
    }

    
    @Override
    public boolean equals( Object o ) {
        if ( o instanceof ScanStrip ) {
            ScanStrip s = (ScanStrip) o;
            if ( this.perforations.size() != s.perforations.size() ) {
                return false;
            }

            for ( int n = 0; n < perforations.size(); n++ ) {
                Perforation p = perforations.get( n );
                Perforation p2 = s.perforations.get( n );
                if ( p.x != p2.x || p.y != p2.y ) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + this.imageOrientation;
        hash = 67 * hash + (this.perforations != null ? this.perforations.hashCode() : 0);
        hash = 67 * hash + (this.name != null ? this.name.hashCode() : 0);
        return hash;
    }

    int refCount = 0;
    
    public void releaseStripImage() {
        refCount--;
        if ( refCount == 0 ) {
            stripImage.dispose();
        }
    }

    /**
     Inform that the image data will be needed by this object. If the image is
      not available, load it from disk.
     */
    public void reserveStripImage() {
        if ( refCount == 0 ) {
            loadImage();
        }
        refCount++;

    }

    /**
     Load the scan strip image from disk
     */
    private void loadImage() {
        ImageReader reader;
        try {
//        PlanarImage img = JAI.create( "fileload", fname );
            ImageInputStream istrm = new FileImageInputStream( file );
            reader = ImageIO.getImageReadersByFormatName( "TIFF" ).next();
            reader.setInput( istrm );
            ImageReadParam param = reader.getDefaultReadParam();
            
            /*
             Set the color mode to linear sRGB as we don't have a better profile
             for the scanner/film available
             */
            ColorSpace cs = ColorSpace.getInstance( ColorSpace.CS_LINEAR_RGB );
            ImageLayout layout = new ImageLayout();
            ColorModel cm = new ComponentColorModel( cs, false, false, 
                    ColorModel.OPAQUE, DataBuffer.TYPE_USHORT );
            layout.setColorModel( cm );
            RenderingHints hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, layout);
            stripImage = ImageReadDescriptor.create(istrm, 0, false, false, false, 
                    null, null, param, reader, hints );
            System.out.println( "Color model " + stripImage.getColorModel() );
            // BufferedImage inImg = reader.read( 0, param );            
        } catch ( FileNotFoundException ex ) {
            System.out.println( ex.getMessage() );
            log.error( "Strip file " + file + " not found", ex );
        } catch ( IOException ex ) {
            log.error( "IO error reading strip " + file + ": ", ex );
        }
    }



}
