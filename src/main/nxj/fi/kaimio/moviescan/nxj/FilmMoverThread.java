/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fi.kaimio.moviescan.nxj;

import lejos.nxt.LCD;
import lejos.nxt.Motor;

/**
 *
 * @author harri
 */
/**
 Control the movement of film in film hodler

 */
public class FilmMoverThread extends Thread {
    
    /**
     The command/ui controller
     */
    FilmMover ctrl = null;
    
    /**
     Motor used to move film
     */
    Motor filmMoverMotor;
    
    
    /**
     How much the motor must be rotated to move the film one scan strip length
     (in degrees)
     */
    int STRIP_ROTATION_DEGREES = 360;
    
    /**
     Create a new object
     @param moverMotor The motor used to rotate the film
     */
    public FilmMoverThread( FilmMover ctrl, Motor moverMotor ) {
        this.ctrl = ctrl;
        filmMoverMotor = moverMotor;
    }
    
    /**
     <code>true</code> if the film is moving
     */
    boolean isMoving = false;
    
    /**
     Main loop of this thread
     */
    public void run() {
        while ( true ) {
            try {
                synchronized ( this ) {
                    wait();
                }
            } catch ( InterruptedException ex ) {
                // Wait was interrupted, just continue without moving film
                continue;
            }
            // ctrl.sendMessage( FilmMover.STATE_FILM_MOVING );
            startFilmMove();
            while ( filmMoverMotor.isRotating() ) {
                LCD.drawString( "Rotating", 0, 10 );
                try {
                    Thread.sleep( 100 );
                } catch ( InterruptedException e ) {
                    // No action needed
                }
            }
            LCD.drawString( "Stopped  ", 0, 10 );
            isMoving = false;
            // ctrl.sendMessage( FilmMover.STATE_FILM_MOVE_FINISHED );
        }
    }

    /**
     Start to move the film
     */
    public synchronized void moveFilm() {
        if ( !isMoving ) {
            notify();
        }
    }

    /**
     Check whether the film is currently moving
     @return <code>true</code> if the film is moving <code>false</code> otherwise
     */
    boolean isMoving() {
        return isMoving;
    }
    
    /**
     Start the motor & set object internal state accordingly
     */
    private void startFilmMove() {
        isMoving = true;
        filmMoverMotor.rotate( STRIP_ROTATION_DEGREES, true );
    }

}
