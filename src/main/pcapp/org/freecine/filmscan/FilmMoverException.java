/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.freecine.filmscan;

/**
 *
 * @author harri
 */
public class FilmMoverException extends Exception {

    public FilmMoverException( String msg ) {
        super( msg );
    }

    public FilmMoverException( String msg, Exception e ) {
        super( msg, e );
    }

}
