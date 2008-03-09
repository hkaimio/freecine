/*
 Copyright (c) 2008 Harri Kaimio
 */

package fi.kaimio.moviescan;

import fi.kaimio.moviescan.common.FilmMoverCommand;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.EnumSet;
import java.util.Set;
import lejos.pc.comm.NXTComm;
import lejos.pc.comm.NXTCommException;
import lejos.pc.comm.NXTCommFactory;
import lejos.pc.comm.NXTInfo;

/**
  Host side SW for controlling NXJ based film mover.
 
 */
public class NxjFilmMover implements FilmMover {

    NXTComm nxtComm;

    NXTInfo nxtInfo;
    
    DataInputStream inDat;
    
    DataOutputStream outDat;
    
    public NxjFilmMover() {
        nxtComm = NXTCommFactory.createNXTComm( NXTCommFactory.USB );
        

        NXTInfo[] nxtInfos = null;
        System.out.println( "Starting..." );
        try {
            nxtInfos = nxtComm.search( null, NXTCommFactory.USB );
        } catch ( NXTCommException e ) {
            System.out.println( "Exception in search" );
        }

        if ( nxtInfos.length == 0 ) {
            System.out.println( "No NXT Found" );
            System.exit( 1 );
        }

        try {
            nxtInfo = nxtInfos[0];
            nxtComm.open( nxtInfo );
        } catch ( NXTCommException e ) {
            System.out.println( "Exception in open" );
        }
        
        InputStream is = nxtComm.getInputStream();
        OutputStream os = nxtComm.getOutputStream();
        inDat = new DataInputStream( is );
        outDat = new DataOutputStream( os );

    }
    
    @Override
    public void moveFilm() throws FilmMoverException {
        try {
            outDat.writeInt( FilmMoverCommand.CMD_MOVE_FILM );
            int ret = inDat.readInt();
            if ( ret != FilmMoverCommand.RET_OK ) {
                throw new FilmMoverException( "Error returned while starting film mover" );
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
