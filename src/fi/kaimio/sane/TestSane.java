/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fi.kaimio.sane;

import fi.kaimio.sane.SaneException;
import fi.kaimio.moviescan.*;
import com.sun.jna.IntegerType;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import java.awt.Point;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.io.File;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;
import javax.media.jai.TiledImage;

/**
 *
 * @author harri
 */
public class TestSane {

    static Sane sane = Sane.INSTANCE;

    public static void main( String args[] ) {
        IntByReference version = new IntByReference();
        int ret = sane.sane_init( version, null );
        System.out.println( "Sane init returner " + ret );
        System.out.println( "Sane version = " + version.getValue() );
        SaneDeviceDescriptor d = new SaneDeviceDescriptor();
        PointerByReference dList = new PointerByReference();
        ret = sane.sane_get_devices( dList, true );
        long offset = 0;
        int deviceCount = 0;
        Pointer devPtr = null;
        SaneDevice dev = null;
        while ( (devPtr = dList.getValue().getPointer( offset )) != null ) {
            deviceCount++;
            offset += Pointer.SIZE;
            d = new SaneDeviceDescriptor( devPtr );
            System.out.println( "Device " + deviceCount +  "(" + d.name + "): " + d.model );
            try {
                dev= new SaneDevice( d.name );
                dev.setOption( "mode", "Color" );
                dev.setOption( "depth", 16 );
                dev.setOption( "tl-x", new FixedPointNumber( 20 << 16 ) );
                dev.setOption( "tl-y", new FixedPointNumber( 48  << 16 ) );
                dev.setOption( "br-x", new FixedPointNumber( 190 << 16 ) );
                dev.setOption( "br-y", new FixedPointNumber( 55 << 16 ) );
                dev.setOption( "resolution",4800 );
                dev.setOption( "source", "Transparency Unit" );
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
                        params.getLines(), 3, 3 * params.getPixelsPerLine(), new int[] {0,1,2});
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
                saveImage( tiledImage, new File("testscan" + deviceCount + ".tif" ));

            } catch ( SaneException e ) {
                System.err.println( e.getMessage() );
            }
            dev.close();
        // printOptions( d.name );
        }
        System.out.println( "Sane init returner " + ret );
        System.out.println( "Sane version = " + version.getValue() );

    }

    static public void scan() {


    }

    static void printOptions( String deviceName ) {
        PointerByReference devRef = new PointerByReference();
        int ret = sane.sane_open( deviceName, devRef );
        Pointer dev = devRef.getValue();

        SaneOptionDescriptor fd = sane.sane_get_option_descriptor( dev, 0 );
        IntByReference optCountRef = new IntByReference();
        sane.sane_control_option( dev, 0, 0, optCountRef, null );
        int optCount = optCountRef.getValue();

        Map<String, SaneOptionDescriptor> options = new HashMap<String, SaneOptionDescriptor>();
        for ( int n = 0; n < optCount; n++ ) {
            SaneOptionDescriptor od = sane.sane_get_option_descriptor( dev, n );
            System.out.println( "Option: " + od.name );
            System.out.println( "  " + od.title );
            System.out.println( "  " + od.desc );
            System.out.print( "  Type: " + OptionType.values()[od.type] );
            if ( od.size > 1 ) {
                System.out.println( "[" + od.size + "]" );
            } else {
                System.out.println();
            }
            System.out.println( "  Units: " + OptionUnit.values()[od.unit] );
            options.put( od.name, od );
            switch ( od.getConstraintType() ) {
                case SANE_CONSTRAINT_RANGE:
                    SaneOptionDescriptor.SaneRange range = (SaneOptionDescriptor.SaneRange) od.getConstraints();
                    System.out.println( "  accepted values: " + range.min + ".." + range.max + " * " + range.quant );
                    break;
                case SANE_CONSTRAINT_STRING_LIST:
                    List<String> strs = (List<String>) od.getConstraints();
                    for ( String str : strs ) {
                        System.out.println( "  " + str );
                    }
                    break;
                case SANE_CONSTRAINT_WORD_LIST:
                    int[] ints = (int[]) od.getConstraints();
                    System.out.print( "  Values: " );
                    boolean isFirst = true;
                    for ( int i : ints ) {
                        if ( isFirst ) {
                            isFirst = false;
                        } else {
                            System.out.print( ", " );
                        }
                        System.out.print( i );
                    }
                    System.out.println();
                    break;
            }
        }
        sane.sane_close( dev );
    }
    
    static void saveImage( RenderedImage img, File f ) {
        ImageWriter writer = null;
        Iterator iter = ImageIO.getImageWritersByFormatName( "TIFF" );
        if ( iter.hasNext() ) {
            writer = (ImageWriter) iter.next();
        }
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
}
