/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fi.kaimio.moviescan;

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.RuleSetBase;

/**
 DIgester rule set for parsing XML representation of a scanStrip 
 @author harri
 */
public class ScanStripRuleSet extends RuleSetBase {
    private String prefix;
    
    /**
     Create a new rule set for parsion scna strips
     
     @param prefix XML path to the rule set element
     */
    public ScanStripRuleSet( String prefix ) {
        this.prefix = prefix;
    }

    @Override
    public void addRuleInstances( Digester d ) {
        d.addObjectCreate(prefix+"scan", ScanStrip.class );
        d.addSetProperties(prefix+"scan");
        d.addCallMethod(prefix + "scan/perforations/perforation", 
                "addPerforation", 2, new Class[]{Integer.class, Integer.class});
        d.addCallParam(prefix + "scan/perforations/perforation", 0, "x");
        d.addCallParam(prefix + "scan/perforations/perforation", 1, "y");
    }
}
