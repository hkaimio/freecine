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

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.Rule;
import org.apache.commons.digester.RuleSetBase;
import org.xml.sax.Attributes;

/**
 *
 * @author harri
 */
public class SceneRuleSet extends RuleSetBase {

    /**
     Project this rule set is associated with. The project is used to fetch the 
     scan strips.
     */
    private Project project;
    
    /**
     XML path prefix for the elements
     */
    private String prefix;

    /**
     Rule for handling the <framerange> element
     */
    private static class FrameRangeRule extends Rule {
        private Project project;
                
        public FrameRangeRule( Project prj ) {
            this.project = prj;
        }
        
        @Override
        public void begin( String namespace, String name, Attributes attributes) {
            Scene scene = (Scene) getDigester().peek();
            String stripName = attributes.getValue("strip");
            ScanStrip s = getProject().getScanStrip(stripName);
            String startStr = attributes.getValue("start");
            int start = (startStr != null ) ? Integer.parseInt(startStr ) : 0;
            String countStr = attributes.getValue("count");
            int count = (countStr != null) ? Integer.parseInt( countStr ) : 0;
            scene.addFrames( s, start, count );

        }

        private Project getProject() {
            if ( project != null ) {
                return project;
            }

            Object obj = digester.peek(1);
            if ( obj instanceof Project ) {
                return (Project) obj;
            }
            return null;
        }
    }
    
    
    public SceneRuleSet( Project prj, String prefix ) {
        this.project = prj;
        this.prefix = prefix;
    }

    
    @Override
    public void addRuleInstances( Digester d ) {
        d.addObjectCreate(prefix + "scene", Scene.class );
        d.addRule( prefix+"scene/frames/framerange", new FrameRangeRule(project));
    }

}
