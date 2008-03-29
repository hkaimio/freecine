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


package org.freecine.filmscan;
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
