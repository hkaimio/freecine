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
        ScanStrip sc1 = new ScanStrip();
        ScanStrip sc2 = new ScanStrip();
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
