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

