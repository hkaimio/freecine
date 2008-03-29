/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.freecine.swingui;

import org.freecine.filmscan.FilmMover;
import org.freecine.filmscan.FilmMoverException;
import org.freecine.filmscan.Project;
import org.freecine.filmscan.ScanAnalysisListener;
import org.freecine.filmscan.ScanStrip;
import org.freecine.sane.FixedPointNumber;
import org.freecine.sane.SaneDevice;
import org.freecine.sane.SaneException;
import org.freecine.sane.ScanParameter;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.io.IOException;
import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;
import javax.media.jai.TiledImage;
import javax.media.jai.operator.ConstantDescriptor;
import javax.media.jai.operator.TransposeDescriptor;
import org.jdesktop.application.Application;
import org.jdesktop.application.Task;

/**
 SwingWorker background task for scanning film. 
 */
public class ScanTask extends Task<Void, Void> implements ScanAnalysisListener {

    private SaneDevice dev;
    private Rectangle scanArea;
    private Project prj;
    private FilmMover filmMover;

    /**
     Creates a new ScanTask object
     @param dev The scanner that will be used
     @param scanArea Scan area 
     @param prj the project in which scanned frames are added
     */
    public ScanTask( SaneDevice dev, FilmMover mover, Rectangle scanArea, Project prj ) {
        super( Application.getInstance() );
        this.dev = dev;
        this.scanArea = scanArea;
        this.prj = prj;
        this.filmMover = mover;
    }
    /**
    Width of the tiles used for storing the scanned iamge
     */
    static final int TILE_WIDTH = 256;
    /**
    Height of the tiles used for storing the scanned image
     */
    static final int TILE_HEIGHT = 256;
    TiledImage image;

    public RenderedImage getImage() {
        return image;
    }
    
    int stripCount = 0;

    /**
     Main loop for this background task. Loop until error happens or the task 
     is cancelled
     @return
     @throws fi.kaimio.sane.SaneException If scanning fails
     @throws java.io.IOException If saving project changes fails
     */
    protected Void doInBackground() throws SaneException, IOException {

        initScanner();
        while ( true ) {
            PlanarImage img = scan();
            img = TransposeDescriptor.create( img, TransposeDescriptor.ROTATE_90, null );
            if ( isCancelled() ) {
                img.dispose();
                break;
            }
            ScanStrip strip = ScanStrip.create( img, this );
            if ( isCancelled() ) {
                strip.dispose();
                img.dispose();
                break;
            }
            prj.addScanStrip( strip );
            prj.save();
            strip.dispose();
            img.dispose();
            System.gc();
            System.gc();
            System.gc();
            message( "movingFilmMsg" );
            try {
                filmMover.moveFilm();
            } catch (FilmMoverException e ) {
                message( "filmMoveFailedMsg", e.getMessage() );
                break;
            }
            
            stripCount++;   
        }
        return null;
    }
    
    /**
     Initializes the scanner before scanning the first strip
     @throws fi.kaimio.sane.SaneException If initialization fails
     */
    private void initScanner() throws SaneException {
        // Setup the scanner
        message( "initMessage" );
        dev.setOption( "mode", "Color" );
        dev.setOption( "depth", 16 );
        dev.setOption( "tl-x", new FixedPointNumber( 30 << 16 ) );
        dev.setOption( "tl-y", new FixedPointNumber( 48 << 16 ) );
        dev.setOption( "br-x", new FixedPointNumber( 190 << 16 ) );
        dev.setOption( "br-y", new FixedPointNumber( 55 << 16 ) );
        dev.setOption( "resolution", 4800 );
        dev.setOption( "source", "Transparency Unit" );

        // Set gamma correction
        dev.setOption( "gamma-correction", "User defined" );
        int[] gammaTable = new int[256];
        for ( int n = 0; n < gammaTable.length; n++ ) {
            gammaTable[n] = n;
        }
        dev.setOption( "red-gamma-table", gammaTable );
        dev.setOption( "blue-gamma-table", gammaTable );
        dev.setOption( "green-gamma-table", gammaTable );
        
    }
    
    /**
     Scan a single strip with settings set in initScanner()
     @return The scanned image
     @throws fi.kaimio.sane.SaneException If an error occurred during scanning.
     */
    private PlanarImage scan() throws SaneException {
        // Scan image
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
        
        
                
        image =
                new TiledImage( 0, 0,
                params.getPixelsPerLine(), params.getLines(),
                0, 0, tileSampleModel, tileColorModel );

        ImageLayout layout = new ImageLayout( image );
        RenderingHints hints = new RenderingHints( JAI.KEY_IMAGE_LAYOUT, layout );
        RenderedImage constImg = ConstantDescriptor.create((float)params.getPixelsPerLine(), (float)params.getLines(), new Short[]{0,0,0}, hints );
        image.set( constImg );
        
        SampleModel scanSampleModel =
                RasterFactory.createPixelInterleavedSampleModel(
                DataBuffer.TYPE_USHORT,
                params.getPixelsPerLine(), TILE_HEIGHT,
                3, 3 * params.getPixelsPerLine(), new int[]{0, 1, 2} );

        int line = 0;
        int totalLines = params.getLines();
        setProgress( line, 0, totalLines );
        message( "scanProgressMessage", stripCount, line, totalLines );
        while ( line < totalLines ) {
            int linesLeft = params.getLines() - line;
            int samplesLeft = linesLeft * params.getBytesPerLine() * 8 / params.getDepth();
            dev.read( data, Math.min( data.length, samplesLeft ) );
            Raster raster =
                    RasterFactory.createWritableRaster( scanSampleModel, db,
                    new Point( 0, line ) ); 
            image.setData( raster );
            line += TILE_HEIGHT;
            setProgress(line < totalLines ? line : totalLines, 0, totalLines);
            message( "scanProgressMessage", stripCount, 
                    line < totalLines ? line : totalLines, totalLines );
        }

        return image;
        
    }

    /**
     Callback that is called by {@link ScanStrip} to inforrm about progress
     @param currentLine The last pixel line analyzed
     @param totalLines Total nubmer of lines
     */
    public void scanAnalysisProgress( int currentLine, int totalLines ) {
        message( "analysisProgressMessage", stripCount, currentLine, totalLines );
        setProgress( currentLine, 0, totalLines);
    }

    /**
     Callback that is called by {@link ScanStrip} After the analysis is ready
     
     @param perfCount Number of perforations found
     */
    public void scanAnalysisComplete( int perfCount ) {
        message( "analysisCompleteMessage", stripCount, perfCount );
    }
    
}
