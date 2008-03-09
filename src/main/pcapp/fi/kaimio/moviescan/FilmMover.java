/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fi.kaimio.moviescan;

import java.io.IOException;
import java.util.Set;

/**
  Interface for controlling the film mover
 
 */
public interface FilmMover {
    
    /**
     Start moving film to prepare for scanning of next strip. THis function 
     returns asynchronously - use getState() find out when the film is ready
     */
    void moveFilm() throws FilmMoverException;
    
    /**
     Get the state of film mover
     @return
     */
    Set<FilmMoverState> getState() throws FilmMoverException;
}
