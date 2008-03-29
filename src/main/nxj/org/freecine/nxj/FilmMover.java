/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.freecine.nxj;

import org.freecine.common.FilmMoverCommand;
import lejos.nxt.*;
import java.io.*;
import lejos.nxt.comm.*;


/**
 Main class for the film moving logic, implemented in Java for NXT brick
 running leJOS.
 
 @author Harri Kaimio

 */
public class FilmMover {

    
    
    /**
     Thread for moving the film
     */
    FilmMoverThread filmMoverThread;
    
    /**
     Thread for handling film reel monitoring & motion
     */
    ReelControl reelThread;

    USBConnection conn = null;
    DataOutputStream dOut = null;
    DataInputStream dIn = null;

    
    /**
     Constructor. Creates & starts the threads for fiml movement & film reel 
     control.
     */
    public FilmMover() throws IOException {
        LightSensor ls = new LightSensor( SensorPort.S1 );
        ls.setFloodlight( false );
        Motor m = Motor.B;
        
        filmMoverThread = new FilmMoverThread( this, Motor.C );
        filmMoverThread.start(); 
	
        reelThread = new ReelControl( this, Motor.B, SensorPort.S1 );
        reelThread.start();
        
        Button rotButton = Button.ENTER;
        rotButton.addButtonListener( new ButtonListener() {

            public void buttonPressed( Button arg0 ) {
                filmMoverThread.moveFilm();
            }

            public void buttonReleased( Button arg0 ) {
            }
        
        } );
        
        Button escButton = Button.ESCAPE;
        escButton.addButtonListener( new ButtonListener() {

            public void buttonPressed( Button arg0 ) {
                System.exit(0);
            }

            public void buttonReleased( Button arg0 ) {
            }
        } );

        // Initialize USB communication
        conn = new USBConnection();
        dOut = conn.openDataOutputStream();
        dIn = conn.openDataInputStream();
    }


    /**
     Loop infinitely and parse commadns coming from USB connection
     
     @throws java.io.IOException If an I/O error occurs
     */
    void commandLoop() throws IOException {
        while ( true ) {
            int ret = FilmMoverCommand.RET_UNKNOWN;
            int cmd = dIn.readInt();
            if ( cmd == FilmMoverCommand.CMD_MOVE_FILM ) {
                if ( !filmMoverThread.isMoving() ) {
                    filmMoverThread.moveFilm();
                    ret = FilmMoverCommand.RET_OK;
                } else {
                    ret = FilmMoverCommand.RET_NOK;
                }
            } else if (cmd == FilmMoverCommand.CMD_GET_STATE ) {
                ret = 0;
                if ( filmMoverThread.isMoving() ) {
                    ret |= FilmMoverCommand.STATE_FILM_MOVING;
                } else {
                    ret |= FilmMoverCommand.STATE_LAST_MOVE_FINISHED;
                }
                if ( reelThread.isMoving() ) {
                    ret |= FilmMoverCommand.STATE_REEL_MOVING;
                }
            } else if ( cmd == FilmMoverCommand.CMD_QUIT ) {
                System.exit( 0 );
            }

            sendMessage( ret );
        }

    }
    
    void sendMessage( int msg ) {
        synchronized ( dOut ) {
            try {
                dOut.writeInt( msg );
                dOut.flush();
            } catch ( IOException ex ) {
            // No handling currently
            }
        }
    }
    
    public static void main(String [] args) throws Exception 
    {
        FilmMover app = new FilmMover();
        app.commandLoop();
    }
}

