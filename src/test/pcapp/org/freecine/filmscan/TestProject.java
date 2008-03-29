/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.freecine.filmscan;

import org.freecine.filmscan.ScanStrip;
import org.freecine.filmscan.Project;
import org.freecine.filmscan.ProjectRuleSet;
import static org.testng.AssertJUnit.*;
import java.io.File;
import java.io.IOException;
import org.apache.commons.digester.Digester;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

/**
 Test cases for the Project class
 */
public class TestProject {
    
    @Test
    public void testProjectSaveLoad() throws IOException, SAXException {
        File prjdir = File.createTempFile("projectest", "" );
        prjdir.delete();
        prjdir.mkdir();
        Project prj = new Project( prjdir );
        ScanStrip sc1 = prj.getScanStrip( "strip1" );
        ScanStrip sc2 = prj.getScanStrip( "strip2" );
        for ( int n = 0; n < 30; n++ ) {
            sc1.addPerforation( 100, n * 800 );
            sc2.addPerforation( 100, n * 800 );
        }
        prj.addScanStrip(sc1);
        prj.addScanStrip(sc2);

        prj.save();
        
        // try to load the project
        
        Digester d = new Digester();
        d.addRuleSet( new ProjectRuleSet(""));
        d.push( "prj_dir_stack", prjdir );
        Project prj2 = (Project) d.parse( new File( prjdir, "project.xml" ) );
        assertEquals( prjdir, prj2.getProjectDir() );
               
    }

}
