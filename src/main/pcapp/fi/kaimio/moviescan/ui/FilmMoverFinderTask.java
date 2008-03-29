/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fi.kaimio.moviescan.ui;

import fi.kaimio.moviescan.FilmMover;
import fi.kaimio.moviescan.FilmMoverException;
import fi.kaimio.moviescan.NxjFilmMover;
import org.jdesktop.application.Application;
import org.jdesktop.application.Task;

/**
 Find an available {@link FilmMover} in the background
 */
public class FilmMoverFinderTask extends Task<FilmMover,Void> {
    
    public FilmMoverFinderTask( Application  app ) {
        super( app );
    }

    FilmMover mover = null;
    
    FilmMover getFilmMover() {
        return mover;
    }
    
    @Override
    protected FilmMover doInBackground() throws Exception {
        message( "lookingNxjMsg" );
        try {
        mover = new NxjFilmMover();
        } catch ( FilmMoverException e ) {
            message( "nxjFailedMsg", e.getMessage() );
            throw e;
        }
        message( "nxjFoundMsg" );
        return mover;
    }

}
