package fi.kaimio.moviescan;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */



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
