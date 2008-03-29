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


package org.freecine.swingui;

import org.freecine.filmscan.FilmMover;
import org.freecine.filmscan.FilmMoverException;
import org.freecine.filmscan.NxjFilmMover;
import org.jdesktop.application.Application;
import org.jdesktop.application.Task;

/**
 Find an available {@link FilmMover} in the background
 */
public class FilmMoverFinderTask extends Task<FilmMover,Void> {
    
    public FilmMoverFinderTask( Application  app ) {
        super( app );
    }

    FilmMover mover = null;
    
    FilmMover getFilmMover() {
        return mover;
    }
    
    @Override
    protected FilmMover doInBackground() throws Exception {
        message( "lookingNxjMsg" );
        try {
        mover = new NxjFilmMover();
        } catch ( FilmMoverException e ) {
            message( "nxjFailedMsg", e.getMessage() );
            throw e;
        }
        message( "nxjFoundMsg" );
        return mover;
    }

}
