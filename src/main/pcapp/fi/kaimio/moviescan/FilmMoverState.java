/*
 Copyringht (c) 2008 Harri Kaimio
 */

package fi.kaimio.moviescan;

/**
 State codes for film mover state
 
 */
public enum FilmMoverState {
    /**
     The film is currently moving
     */
    FILM_MOVING,
    /**
     The last move command has been completed succesfully. Note that this may
     not be true even if the film is not moving, as it can be stuck.
     */
    LAST_MOVE_FINISHED,
    
    /**
     The film reel is moving
     */
    REEL_MOVING;

}
