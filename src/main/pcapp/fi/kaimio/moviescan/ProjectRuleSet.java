/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fi.kaimio.moviescan;

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
