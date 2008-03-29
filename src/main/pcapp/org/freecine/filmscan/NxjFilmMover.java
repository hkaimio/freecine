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

package org.freecine.filmscan;

import org.freecine.common.FilmMoverCommand;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.EnumSet;
import java.util.Set;
import java.util.logging.Logger;
import lejos.pc.comm.NXTComm;
import lejos.pc.comm.NXTCommException;
import lejos.pc.comm.NXTCommFactory;
import lejos.pc.comm.NXTInfo;

/**
  Host side SW for controlling NXJ based film mover.
 
 */
public class NxjFilmMover implements FilmMover {

    private static Logger log = Logger.getLogger( NxjFilmMover.class.getName() );
    
    NXTComm nxtComm;
    

    NXTInfo nxtInfo;
    
    DataInputStream inDat;
    
    DataOutputStream outDat;
    
    /**
     Creates a new NxjFilmMover
     
     @throws fi.kaimio.moviescan.FilmMoverException If no connected film mover 
     was found or other error occurred.
     */
    public NxjFilmMover() throws FilmMoverException {
        nxtComm = NXTCommFactory.createNXTComm( NXTCommFactory.USB );

        NXTInfo[] nxtInfos = null;
        log.fine( "Starting..." );
        try {
            nxtInfos = nxtComm.search( null, NXTCommFactory.USB );
        } catch ( NXTCommException e ) {
            log.warning( "Exception in search" );
        }

        if ( nxtInfos == null || nxtInfos.length == 0 ) {
            log.warning( "No NXT Found" );
            throw new FilmMoverException( "No NXJ found" );
        }

        try {
            nxtInfo = nxtInfos[0];
            nxtComm.open( nxtInfo );
        } catch ( NXTCommException e ) {
            log.warning( "Exception in open" );
            throw new FilmMoverException( 
                    "Exception while opening: " + e.getMessage(), e );
        }
        
        InputStream is = nxtComm.getInputStream();
        OutputStream os = nxtComm.getOutputStream();
        inDat = new DataInputStream( is );
        outDat = new DataOutputStream( os );

    }
    
    /**
     Close the connection to NXT
     @throws java.io.IOException If the operation fails.
     */
    void close() throws IOException {
        nxtComm.close();
    }
    
    @Override
    public void moveFilm() throws FilmMoverException {
        try {
            outDat.writeInt( FilmMoverCommand.CMD_MOVE_FILM );
            int ret = inDat.readInt();
            if ( ret != FilmMoverCommand.RET_OK ) {
                FilmMoverException e = new FilmMoverException( "Error returned while starting film mover" );
                log.throwing("NxjFilmMover", "moveFilm()", e );
                throw e;
            }
        } catch ( IOException e ) {
            throw new FilmMoverException( e.getMessage(), e );
        }
    }

    @Override
    public Set<FilmMoverState> getState() throws FilmMoverException {
        EnumSet<FilmMoverState> ret = EnumSet.noneOf(FilmMoverState.class);
        try {
            outDat.writeInt( FilmMoverCommand.CMD_GET_STATE );
            int state = inDat.readInt();
            if ( (state & FilmMoverCommand.STATE_FILM_MOVING ) != 0 ) {
                ret.add( FilmMoverState.FILM_MOVING );
            }
            if ( (state & FilmMoverCommand.STATE_LAST_MOVE_FINISHED ) != 0 ) {
                ret.add( FilmMoverState.LAST_MOVE_FINISHED );
            }
            if ( (state & FilmMoverCommand.STATE_REEL_MOVING ) != 0 ) {
                ret.add( FilmMoverState.REEL_MOVING );
            }
            
        } catch ( IOException e ) {
            throw new FilmMoverException( e.getMessage(), e );
        }
        return ret;
    }

}
