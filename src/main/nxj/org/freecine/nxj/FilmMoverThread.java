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

package org.freecine.nxj;

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
