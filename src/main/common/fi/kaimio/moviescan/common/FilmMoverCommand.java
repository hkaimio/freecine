/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fi.kaimio.moviescan.common;

/**
 Constants for communication between PC and NXT
 */
public class FilmMoverCommand {
    /**
     Move the film one strip length
     */
    public final static int CMD_MOVE_FILM = 0x0001;
    
    /**
     Quit the porgram
     */
    public final static int CMD_QUIT = 0x0002;

    /**
     Get state of the film mover
     */
    public final static int CMD_GET_STATE = 0x0003;
    
    /**
     Return code if the command was successful
     */
    public final static int RET_OK = 0x1000;
    
    /**
     Return code if the command could not be executed
     */
    public final static int RET_NOK = 0x1001;
    
    /**
     Return code if the command was not understood
     */
    public final static int RET_UNKNOWN = 0x1002;
    
    /**
     Status bit for indicating that the film is currently moving
     */
    public final static int STATE_FILM_MOVING = 1;
    
    /**
     Status bit for indicating that the last film move command has been 
     finished successfully
     */
    public final static int STATE_LAST_MOVE_FINISHED = 1 << 1;
    
    
    /**
     Status bit to indicate that the film reel is moving
     */
    public final static int STATE_REEL_MOVING = 1 << 2;
    

}
