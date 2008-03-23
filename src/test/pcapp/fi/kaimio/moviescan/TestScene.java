/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fi.kaimio.moviescan;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.digester.Digester;
import org.testng.annotations.Test;
import static org.testng.AssertJUnit.*;
import org.xml.sax.SAXException;

/**
 Test cases for Scene class
 */
public class TestScene {
    
    /**
     Test access to scene frames & splitting/joining scenes
     */
    @Test
    public void testFrameAccess() {
        ScanStrip sc1 = new ScanStrip();
        ScanStrip sc2 = new ScanStrip();
        
        for ( int n = 0; n < 30 ; n++ ) {
            sc1.addPerforation( 100, n*800 );
            sc2.addPerforation( 100, n*800 );
        }
        
        Scene scene = new Scene();
        scene.addFrames(sc1, 3, 27 );
        scene.addFrames(sc2, 0, 25 );
        assertEquals( 52, scene.getFrameCount() );
        
        RenderedImage img = scene.getFrame( 3 );
        
        Scene scene1 = scene.split(0, 30 );
        Scene scene2 = scene.split(30, 22 );
        assertEquals( 30, scene1.getFrameCount() );
        assertEquals( 22, scene2.getFrameCount() );
        assertEquals( sc2, scene2.getFrameStrip(0 ));
        assertEquals( 4, scene2.getStripFrameNum(1));
        
        scene1.append(scene2);
        
        for ( int n = 0 ; n < scene.getFrameCount() ; n++ ) {
            System.out.println( n );
            assertEquals( scene.getFrameStrip(n), scene1.getFrameStrip(n));
            assertEquals( scene.getStripFrameNum(n), scene1.getStripFrameNum(n));
        }
    }
    
    @Test
    public void testRangeChanges() {
        ScanStrip sc1 = new ScanStrip();
        ScanStrip sc2 = new ScanStrip();
        
        for ( int n = 0; n < 32 ; n++ ) {
            sc1.addPerforation( 100, n*800 );
            sc2.addPerforation( 100, n*800 );
        }
        
        Scene scene = new Scene();
        scene.addFrames(sc1, 3, 27 );
        scene.addFrames(sc2, 0, 25 );
        
        assertTrue(scene.getFrameStrip(27) == sc2 );
        assertEquals( 0, scene.getStripFrameNum(27) );
        
        sc2.setFirstUsable( 1 );
        assertTrue(scene.getFrameStrip(27) == sc2 );
        assertEquals( 1, scene.getStripFrameNum(27) );
        
        // Disabling 2 first frames should not affect the scene
        sc1.setFirstUsable( 2 );
        assertEquals( 3, scene.getStripFrameNum( 0 ));
        sc1.setFirstUsable( 4 );
        assertEquals( 4, scene.getStripFrameNum( 0 ));
        assertEquals( 2, scene.getStripFrameNum( 27 ));
        sc1.setFirstUsable( 2 );
        assertEquals( 3, scene.getStripFrameNum( 0 ));
        assertEquals( 1, scene.getStripFrameNum( 27 ));
    }
    
    /**
     Test import & export to XML
     @throws java.io.IOException
     @throws javax.xml.transform.TransformerConfigurationException
     @throws org.xml.sax.SAXException
     */
    @Test
    public void testXml() throws IOException, TransformerConfigurationException, SAXException {
        Project prj = new Project( new File(".") );
        ScanStrip sc1 = prj.getScanStrip( "strip1" );
        ScanStrip sc2 = prj.getScanStrip( "strip2" );
        for ( int n = 0; n < 30; n++ ) {
            sc1.addPerforation( 100, n * 800 );
            sc2.addPerforation( 100, n * 800 );
        }

        Scene scene = new Scene();
        scene.addFrames( sc1, 3, 27 );
        scene.addFrames( sc2, 0, 25 );
        File f = File.createTempFile( "scene_test", "xml" );

        // Write the strip info to a file
        StreamResult streamResult = new StreamResult( f );
        SAXTransformerFactory tf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
        TransformerHandler hd = tf.newTransformerHandler();
        Transformer serializer = hd.getTransformer();
        serializer.setOutputProperty( OutputKeys.ENCODING, "ISO-8859-1" );
        serializer.setOutputProperty( OutputKeys.INDENT, "yes" );
        hd.setResult( streamResult );
        hd.startDocument();
        scene.writeXml( hd );
        hd.endDocument();
        
        Digester d = new Digester();
        d.addRuleSet( new SceneRuleSet(prj, ""));
        Scene parsedScene = (Scene) d.parse( f );

        assertEquals(scene.getFrameCount(), parsedScene.getFrameCount() );
        
        for ( int n = 0 ; n < scene.getFrameCount() ; n++ ) {
            System.out.println( n );
            assertTrue( scene.getFrameStrip(n) == parsedScene.getFrameStrip(n));
            assertEquals( scene.getStripFrameNum(n), parsedScene.getStripFrameNum(n));
        }
        
        
    }

}
