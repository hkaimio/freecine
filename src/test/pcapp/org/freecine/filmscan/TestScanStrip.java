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
 Test cases for ScanStrip class
 */
public class TestScanStrip {

    @Test
    public void testXmlExportImport() throws IOException, TransformerConfigurationException, SAXException {
        ScanStrip sc = new ScanStrip();
        sc.setOrientation( 90 );
        for ( int n = 100; n < 37000; n += 800 ) {
            sc.addPerforation( 100, n );
        }
        File f = File.createTempFile( "scanstrip_test", "xml" );

        // Write the strip info to a file
        StreamResult streamResult = new StreamResult( f );
        SAXTransformerFactory tf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
        TransformerHandler hd = tf.newTransformerHandler();
        Transformer serializer = hd.getTransformer();
        serializer.setOutputProperty( OutputKeys.ENCODING, "ISO-8859-1" );
        serializer.setOutputProperty( OutputKeys.INDENT, "yes" );
        hd.setResult( streamResult );
        hd.startDocument();
        sc.writeXml( hd );
        hd.endDocument();

        Digester d = new Digester();
        d.addRuleSet( new ScanStripRuleSet( "" ) );
        ScanStrip parsedSc = (ScanStrip) d.parse( f );
        assert parsedSc.equals( sc );
    }
    
    @Test
    public void testPerforationChange() {
        ScanStrip sc = new ScanStrip();
        sc.setOrientation( 90 );
        for ( int n = 100; n < 37000; n += 800 ) {
            sc.addPerforation( 100, n );
        }
        assertEquals( 47, sc.getPerforationCount() );
        Perforation p = sc.getPerforation(10);
        assertEquals( 100, p.x);
        assertEquals( 8100, p.y );
        sc.setPerforation(10, 90, 8110);
        
        p = sc.getPerforation(10);
        assertEquals( 90, p.x);
        assertEquals( 8110, p.y );
        assertEquals( 47, sc.getPerforationCount() );
        p = sc.getPerforation(11);
        assertEquals( 100, p.x);
        assertEquals( 8900, p.y );
    }
    
    @Test
    public void testLoadStrip() throws IOException, SAXException {
        File stripDesc = new File( "/home/harri/s8/tuhkimo/scan/scan_0002.xml" );
        Digester d = new Digester();
        d.addRuleSet( new ScanStripRuleSet( "" ) );
        ScanStrip strip = (ScanStrip) d.parse( stripDesc );
        
        File imgFile = new File( stripDesc.getParentFile(), "scan_0002.tif" );
        strip.setFile(imgFile);
        
        strip.reserveStripImage();
        RenderedImage img = strip.getFrame( 2 );
        strip.releaseStripImage();
        img = strip.getFrame(7);
    }

    private static class Listener implements ScanStripListener {

        public boolean notified = false;
        public void scanStripChanged( ScanStrip changedStrip ) {
            notified = true;
        }
        
    }
    @Test
    public void testNotifier() {
        ScanStrip sc = new ScanStrip();
        sc.setOrientation( 90 );
        for ( int n = 100; n < 37000; n += 800 ) {
            sc.addPerforation( 100, n );
        }
        
        Listener l = new Listener();
        
        sc.addScanStripListener(l);
        sc.addPerforation(100, 38000);
        assertTrue(l.notified);
        l.notified = false;
        sc.setFirstUsable( 2 );
        assertTrue(l.notified);
        l.notified = false;        
        sc.setLastUsable(30);
        assertTrue(l.notified);
        l.notified = false;
        sc.removeScanStripListener(l);
        sc.setFirstUsable(0 );
        assertFalse(l.notified);
    }
}
