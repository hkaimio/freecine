/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
