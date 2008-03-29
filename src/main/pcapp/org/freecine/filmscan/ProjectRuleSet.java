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

import java.io.File;
import org.apache.commons.digester.Digester;
import org.apache.commons.digester.ObjectCreationFactory;
import org.apache.commons.digester.RuleSetBase;
import org.xml.sax.Attributes;

/**
  Digester rules for parsing a Project object from XML file.
 <p>
 The project directory is expected to be stored into Digester stack with name 
 "prj_dir_stack".
 */
public class ProjectRuleSet extends RuleSetBase{
    private String prefix;

    public ProjectRuleSet( String prefix ) {
        this.prefix = prefix;
    }

    public static class ProjectCreationFactory implements ObjectCreationFactory {

        public ProjectCreationFactory() {
            super();
        }
        
        Digester digester;
        
        public Object createObject( Attributes attr ) throws Exception {
            File prjdir = (File) digester.peek( "prj_dir_stack" );
            if ( prjdir == null ) {
                prjdir = new File( "." );
            }
            Project prj = new Project( prjdir );
            return prj;
        }

        public Digester getDigester() {
            return digester;
        }

        public void setDigester( Digester d ) {
            digester = d;
        }
        
    }
    
    @Override
    public void addRuleInstances( Digester d ) {
        d.addFactoryCreate(prefix + "project", ProjectCreationFactory.class );
        d.addRuleSet( new SceneRuleSet(null, prefix + "project/scenes/" ) );
        d.addSetNext( prefix + "project/scenes/scene", "addScene" );
    }

}
