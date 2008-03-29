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

package org.freecine.common;

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
