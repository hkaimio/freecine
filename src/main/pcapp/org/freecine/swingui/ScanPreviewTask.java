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
    
    public ScanPreviewTask( Application app, SaneDevice dev ) {
        super( app );
        this.dev = dev;
        log.setLevel(Level.FINE);
    }

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
        dev.setOption( "resolution", 100 );
        dev.setOption( "source", "Transparency Unit" );
        
        // Find out the maximum scanning area
        int minX = -1;
        int maxX = -1;
        int minY = -1;
        int maxY = -1;
        SaneOptionDescriptor xdesc = dev.getOptionDesc( "tl-x" );        
        switch ( xdesc.getConstraintType() ) {
            case SANE_CONSTRAINT_RANGE:
                SaneRange r = (SaneRange) xdesc.getConstraints();
                minX = r.min;
                maxX = r.max;
                break;
            case SANE_CONSTRAINT_WORD_LIST:
                int[] values = (int[]) xdesc.getConstraints();
                minX = values[0];
                maxX = values[values.length-1];
                break;
            default:
                throw new SaneException( "Cannod determinen scan glass area" );
        }
        SaneOptionDescriptor ydesc = dev.getOptionDesc( "tl-y" );        
        switch ( ydesc.getConstraintType() ) {
            case SANE_CONSTRAINT_RANGE:
                SaneRange r = (SaneRange) ydesc.getConstraints();
                minY = r.min;
                maxY = r.max;
                break;
            case SANE_CONSTRAINT_WORD_LIST:
                int[] values = (int[]) ydesc.getConstraints();
                minY = values[0];
                maxY = values[values.length-1];
                break;
            default:
                throw new SaneException( "Cannod determinen scan glass area" );
        }
        dev.setOption( "tl-x", new FixedPointNumber( minX ) );
        dev.setOption( "tl-y", new FixedPointNumber( minY ) );
        dev.setOption( "br-x", new FixedPointNumber( maxX ) );
        dev.setOption( "br-y", new FixedPointNumber( maxY ) );

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

        return image;
        
    }

}
