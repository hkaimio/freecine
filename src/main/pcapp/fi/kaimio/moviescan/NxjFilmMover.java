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
