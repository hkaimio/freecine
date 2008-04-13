/*
Copyright (C) 2008 Harri Kaimio
 
This file is part of Freecine
 
Freecine is free software; you can redistribute it and/or modify it 
under the terms of the GNU General Public License as published by the Free 
Software Foundation; either version 3 of the License, or (at your option) 
any later version.
 
This program is distributed in the hope that it will be useful, but WITHOUT 
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
You should have received a copy of the GNU General Public License along with 
this program; if not, see <http://www.gnu.org/licenses>.
 
Additional permission under GNU GPL version 3 section 7
 
If you modify this Program, or any covered work, by linking or combining it 
with Java Advanced Imaging (or a modified version of that library), containing 
parts covered by the terms of Java Distribution License, or leJOS, containing 
parts covered by the terms of Mozilla Public License, the licensors of this 
Program grant you additional permission to convey the resulting work. 
 */

package org.freecine.swingui;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;
import org.freecine.sane.FixedPointNumber;
import org.freecine.sane.SaneDevice;
import org.freecine.sane.SaneException;
import org.freecine.sane.SaneOptionDescriptor;
import org.freecine.sane.SaneOptionDescriptor.SaneRange;
import org.freecine.sane.ScanParameter;
import org.jdesktop.application.Application;
import org.jdesktop.application.Task;

/**
 Background task for scanning a preview image of the whole scanner area
 */
public class ScanPreviewTask  extends Task<BufferedImage, BufferedImage> {

    Logger log = Logger.getLogger( ScanPreviewTask.class.getName() );

    private SaneDevice dev;
    private int TILE_WIDTH;
    
    /**
     Create a new preview task
     @param app Application this task is associated with
     @param dev ScanDevice to use
     */
    public ScanPreviewTask( Application app, SaneDevice dev ) {
        super( app );
        this.dev = dev;
        log.setLevel(Level.FINE);
    }
    
    /**
     Area of the preview scan, in device coordinates.
     */
    Rectangle2D previewArea = null;
    
    /**
     Get the area of preview scan
     @return Preview area, in device coordinates.
     */
    public Rectangle2D getPreviewArea() {
        return previewArea;
    }

    /**
     Do the actual scanning in background thread.
     @return The scanned preview image.
     @throws java.lang.Exception
     */
    @Override
    protected BufferedImage doInBackground() throws Exception {
        initScanner();    
        return scan();
    }
    
    /**
     Initializes the scanner before scanning the preview image
     @throws fi.kaimio.sane.SaneException If initialization fails
     */
    private void initScanner() throws SaneException {
        // Setup the scanner
        message( "initMessage" );
        dev.setOption( "mode", "Color" );
        dev.setOption( "depth", 16 );
        dev.setOption( "resolution", 200 );
        dev.setOption( "source", "Transparency Unit" );
        
        // Find out the maximum scanning area
        FixedPointNumber minX = null;
        FixedPointNumber maxX = null;
        FixedPointNumber minY = null;
        FixedPointNumber maxY = null;
        SaneOptionDescriptor xdesc = dev.getOptionDesc( "tl-x" );        
        switch ( xdesc.getConstraintType() ) {
            case SANE_CONSTRAINT_RANGE:
                SaneRange r = (SaneRange) xdesc.getConstraints();
                minX = new FixedPointNumber( r.min );
                maxX = new FixedPointNumber( r.max );
                break;
            case SANE_CONSTRAINT_WORD_LIST:
                int[] values = (int[]) xdesc.getConstraints();
                minX = new FixedPointNumber( values[0] );
                maxX = new FixedPointNumber( values[values.length-1] );
                break;
            default:
                throw new SaneException( "Cannod determinen scan glass area" );
        }
        SaneOptionDescriptor ydesc = dev.getOptionDesc( "tl-y" );        
        switch ( ydesc.getConstraintType() ) {
            case SANE_CONSTRAINT_RANGE:
                SaneRange r = (SaneRange) ydesc.getConstraints();
                minY = new FixedPointNumber( r.min );
                maxY = new FixedPointNumber( r.max );                
                break;
            case SANE_CONSTRAINT_WORD_LIST:
                int[] values = (int[]) ydesc.getConstraints();
                minY = new FixedPointNumber( values[0] );
                maxY = new FixedPointNumber( values[values.length-1] );
                break;
            default:
                throw new SaneException( "Cannod determinen scan glass area" );
        }
//        if ( maxX.toDouble() > 8 * 25.4 ) {
//            maxX = FixedPointNumber.valueOf( 8 * 25.4 );
//        }
//        if ( maxY.toDouble() > 10 * 25.4 ) {
//            maxY = FixedPointNumber.valueOf( 10 * 25.4 );
//        }
        minX = dev.setOption( "tl-x", minX );
        minY = dev.setOption( "tl-y", minY );
        maxX = dev.setOption( "br-x", maxX );
        maxY = dev.setOption( "br-y", maxY );
        previewArea = new Rectangle2D.Double( minX.toDouble(), minY.toDouble(), 
                maxX.subtract(minX).toDouble(), maxY.subtract(minY).toDouble() );
        
        ScanParameter param = dev.getScanParameter();
        System.out.println( "Scan parameters: " + param.getPixelsPerLine() + " x " + param.getLines() );
        
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
    private BufferedImage scan() throws SaneException {
        int STRIP_HEIGHT = 256;
        
        // Scan image
        ScanParameter params = dev.getScanParameter();
        dev.startScan();
        params = dev.getScanParameter();

        // Read 1 strip of tiles at time
        int totalSamples =
                params.getLines() * params.getBytesPerLine() * 8 / params.getDepth();
        short[] data = new short[ totalSamples];
        DataBufferUShort db = new DataBufferUShort( data,  totalSamples );
        SampleModel sampleModel =
                RasterFactory.createPixelInterleavedSampleModel(
                DataBuffer.TYPE_USHORT,
                params.getPixelsPerLine(), params.getLines(),
                3, 3 * params.getPixelsPerLine(), new int[]{0, 1, 2} );
        ColorModel colorModel = PlanarImage.createColorModel( sampleModel );
        
        
        WritableRaster raster = Raster.createWritableRaster(sampleModel, db, new Point(0,0) );      
        
        BufferedImage image =
                new BufferedImage( colorModel, raster, false, null );

        int pos = 0;
        message( "scanProgressMessage", pos, totalSamples );
        log.fine( "Scanning image..." );
        while ( pos < totalSamples ) {
            int samplesToRead = Math.min( 16384, data.length - pos );
            dev.read( data, pos, samplesToRead );
            pos+= samplesToRead;
            setProgress(pos, 0, totalSamples);
            message( "scanProgressMessage",  
                    pos, totalSamples );
            log.fine( "Read " + pos + " samples" );
        }

        dev.close();
        return image;
        
    }

}
