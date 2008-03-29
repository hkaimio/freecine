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


import lejos.nxt.SensorPort;
import lejos.nxt.TouchSensor;

/**
 Thread to control film tightness & rotate the film reel as needed
 */
public class ReelControl extends Thread {
    
    /**
     UI,USB command handler & controller
     */
    FilmMover ctrl;
    
    /**
     Motor used to rotate the reel
     */
    Motor reelMotor;
    
    /**
     Sensor used to detect looseness of film
     */
    TouchSensor ts;
    
    /**
     Construct the object
     @param motor Motor used for rotationg reel
     @param sensorPort Port into which the touch sensor is connected.
     */
    public ReelControl( FilmMover ctrl, Motor motor, SensorPort sensorPort ) {
        this.ctrl = ctrl;
        reelMotor = motor;
        reelMotor.setSpeed( 200 );
        ts = new TouchSensor( sensorPort );
    }

    /**
     Main thread loop
     */
    public void run() {
        while ( true ) {
            boolean isPressed = ts.isPressed();
            if ( isPressed ) {
                LCD.drawString( "Film tight", 0, 0 );
            } else {
                LCD.drawString( "Film loose", 0, 0 );
            }
            if ( isPressed == false && !isMoving ) {
                isMoving = true;
                LCD.drawString( "Reel running", 0, 10 );
                // ctrl.sendMessage( FilmMover.STATE_REEL_MOVING );
                reelMotor.backward();
            } else if ( isPressed == true && isMoving ) {
                isMoving = false;
                LCD.drawString( "Reel stopped", 0, 10 );
                reelMotor.stop();
                // ctrl.sendMessage( FilmMover.STATE_REEL_STOPPED );

            }
            try {
                Thread.sleep( 100 );
            } catch ( InterruptedException e ) {
            // No action needed, just continue
            }
        }        
    }
    
    boolean isMoving = false;
    
    public boolean isMoving() {
        return isMoving; 
    }
}
