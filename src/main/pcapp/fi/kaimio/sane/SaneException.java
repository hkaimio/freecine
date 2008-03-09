/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fi.kaimio.sane;

/**
 *
 * @author harri
 */
public class SaneException extends Exception {
    public SaneException( String msg ) {
        super ( msg );
    }
    
    public SaneException( String msg, Exception cause ) {
        super( msg, cause );
    }

}
