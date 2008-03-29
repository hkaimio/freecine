/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.freecine.filmscan;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.digester.Digester;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 Project object keeps strck of all inforation related the project (i.e. 
 information about scan strips and scenes
 */
public class Project implements Iterable<FrameDescriptor> {

    private static Logger log = Logger.getLogger( Project.class.getName() );
    /**
     Home directory for the project
     */
    private File dir;
    
    private Map<String, ScanStrip> loadedStrips = new HashMap<String, ScanStrip>();
    
    /**
     The scene into which new scans are added
     */
    private Scene scene;
    
    /**
     Creates a new project
     @param dir The directory of the project
     */
    public Project( File dir ) {
        this.dir = dir;
        scene = new Scene();
    }
    
    /**
     Get project object for given directory.
     <p>
     If dir is an existing directory with valid project.xml file in it, laod the 
     project and return it. If there is no project in the directory, return a 
     new empty project.
     
     @param dir Project directory
     @return The project in directory or new project if no project exists there
     */
    static public Project getProject( File dir ) {
        File projectFile = new File( dir, "project.xml" );
        Project ret = null;
        if ( projectFile.exists() ) {
            Digester d = new Digester();
            d.push( "prj_dir_stack", dir );
            d.addRuleSet( new ProjectRuleSet("" ));
            try  {
                ret =  (Project) d.parse(projectFile );
            } catch ( IOException e ) {
                log.severe( "IO error reading " + projectFile.getPath() + ": " + e.getMessage() );
            } catch ( SAXException e ) {
                log.severe( "Parse error reading " + projectFile.getPath() + ": " + e.getMessage() );                
            }
        } else {
            try {
                if ( !dir.exists() && !dir.mkdirs() ) {
                    log.warning("Cannot crete project directory" );
                    return null;
                }
                ret = new Project( dir );
                ret.save();
            } catch ( IOException ex ) {
                return null;
            }
        }
        return ret;
    }
    
    /**
    Add a scan strip to the project, saving it to image file if it is not yet 
    saved.
    @param strip
     */
    public void addScanStrip( ScanStrip strip ) {
        if ( strip.getName() == null ) {
            File scanDir = new File( dir, "scan" );
            if ( !scanDir.exists() ) {
                if ( !scanDir.mkdir()  ) {
                    log.severe( "Cannot create directory" );
                    return;
                }
            }
            int num = getLastScanFile() + 1;
            String name = String.format( "scan_%04d", num );
            // Save the image
            RenderedImage img = strip.stripImage;
            File imgFile = new File( scanDir, name + ".tif" );
            saveImage( img, imgFile );
            saveStripInfo( strip, new File( scanDir, name + ".xml" ) );
            strip.setName( name );
            strip.setFile( imgFile );
        }
        loadedStrips.put( strip.getName(), strip );
        scene.addFrames( strip, 0, strip.getFrameCount() );
    }

    /**
     Get the scan strip with given name
     @param name Name of the strip
     @return The strip with given name or <code>null</code> if no such strip 
     exists
     */
    public ScanStrip getScanStrip( String name ) {
        ScanStrip ret = null;
        if ( loadedStrips.containsKey( name ) ) {
            ret = loadedStrips.get( name );
        } else {
            File stripDescFile = new File( dir, "scan/" + name + ".xml" );
            File stripImgFile = new File( dir, "scan/" + name + ".tif" );
            if ( stripDescFile.exists() ) {
                ret = ScanStrip.loadStrip( stripDescFile, stripImgFile );
                loadedStrips.put( name, ret );
                ret.setName(name);
            } else {
                ret = new ScanStrip();
                loadedStrips.put( name, ret );
                ret.setName( name );
            }
        }
        return ret;
    }

    public void addScene( Scene s ) {
        scene = s;
    }
    
    public Scene getScene() {
        return scene;
    }
    
    /**
     Get the project directory
     @return Project directory
     */
    public File getProjectDir()  {
        return dir;
    }
    
    
    /**
     Save the project info in project directory (in file project.xml)
     @throws IOException if the project cannot be saved
     */
    public void save() throws IOException {
        File f = File.createTempFile( "tmproject", ".xml", dir );
        try {
            // Write the strip info to a file
            StreamResult streamResult = new StreamResult( f );
            SAXTransformerFactory tf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
            TransformerHandler hd = tf.newTransformerHandler();
            Transformer serializer = hd.getTransformer();
            serializer.setOutputProperty( OutputKeys.ENCODING, "ISO-8859-1" );
            serializer.setOutputProperty( OutputKeys.INDENT, "yes" );
            hd.setResult( streamResult );
            hd.startDocument();
            writeXml( hd );
            hd.endDocument();
        } catch ( Exception e ) {
            throw new IOException( "Error saving project: " + e.getMessage(), e );
        }
        if ( !f.renameTo( new File( dir, "project.xml" ) ) ) {
            // TODO: proper error handling 
            log.warning( "cound ot rename project file" );
        }
    }
    
    public void writeXml( TransformerHandler hd ) throws SAXException {
        AttributesImpl atts = new AttributesImpl();
        hd.startElement( "", "", "project", atts );
        hd.startElement( "", "", "scenes", atts );
        scene.writeXml( hd );
        hd.endElement( "", "", "scenes" );
        hd.endElement( "", "", "project" );
    }
    
    private static class ScanFileFilter implements FilenameFilter {

        public boolean accept( File dir, String fname ) {
            return fname.matches("^scan_[0-9][0-9][0-9][0-9].tif$");
        }
        
    }

    
    
    private int getLastScanFile() {
        int max = 0;
        File scanDir = new File( dir, "scan" );
        for ( File f : scanDir.listFiles( new ScanFileFilter() ) ) {
            String num = f.getName().substring(5, 9);
            int n = Integer.parseInt( num );
            max = Math.max( max, n );
        }
        return max;
    }

    private void saveImage( RenderedImage img, File file ) {
        // Find a writer for that file extensions
        // Try to determine the file type based on extension
        String ftype = "jpg";
        String imageFname = file.getName();
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
                ios = ImageIO.createImageOutputStream( file );
                writer.setOutput( ios );
                // Set some parameters
                ImageWriteParam param = writer.getDefaultWriteParam();
                writer.write( null, new IIOImage( img, null, null ), param );

                // Cleanup
                ios.flush();

            } catch ( IOException ex ) {
                log.severe( "Error saving image " + file.getAbsolutePath() + ": " 
                        + ex.getMessage() );
            } finally {
                if ( ios != null ) {
                    try {
                        ios.close();
                    } catch ( IOException e ) {
                        log.severe( "Error closing output stream: " + e.getMessage() );
                    }
                }
                writer.dispose();
            }
        }
    }

    private void saveStripInfo( ScanStrip strip, File file ) {
        try {
            StreamResult streamResult = new StreamResult( file );
            SAXTransformerFactory tf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
            TransformerHandler hd = tf.newTransformerHandler();
            Transformer serializer = hd.getTransformer();
            serializer.setOutputProperty( OutputKeys.ENCODING, "ISO-8859-1" );
            serializer.setOutputProperty( OutputKeys.INDENT, "yes" );
            hd.setResult( streamResult );
            hd.startDocument();
            strip.writeXml( hd );
            hd.endDocument();
        } catch ( SAXException ex ) {
            log.severe( "Error saving " + file.getAbsolutePath() + ": " + ex.getMessage() );
        } catch ( TransformerConfigurationException ex ) {
            log.severe( "Error saving " + file.getAbsolutePath() + ": " + ex.getMessage() );
        }
    }

    public Iterator<FrameDescriptor> iterator() {
        return new FrameIterator( this );
    }

}
