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

import org.freecine.filmscan.FrameDescriptor;
import org.freecine.filmscan.Project;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import org.jdesktop.application.Application;
import org.jdesktop.application.Task;

/**
 Background task for saving all frames in the project
 @author harri
 */
class SaveFramesTask extends Task<Object, Void> {
    
    /**
     Project that will be saved
     */
    private Project prj;
    
    /**
     Directory where the frame files willb e saved.
     */
    private File dir;

    /**
     Create a new SaveFramesTask
     @param app Application
     @param prj Project we are saving
     @param dir Directory where the frames will be saved
     */
    SaveFramesTask( Application app, Project prj, File dir ) {
        // Runs on the EDT.  Copy GUI state that
        // doInBackground() depends on from parameters
        // to SaveFramesTask fields, here.
        super( app );
        this.prj = prj;
        this.dir = dir;
    }

    /**
     Main loop for executing the task
     @return null
     */
    @Override
    protected Object doInBackground() {
        int frame = 0;
        int totalFrames = prj.getScene().getFrameCount();
        ColorConverter conv = new ColorConverter();
        conv.setBlack( prj.getScene().getBlack() );
        conv.setWhite( prj.getScene().getWhite() );
        for ( FrameDescriptor fd : prj ) {
            if ( isCancelled() ) {
                return null;
            }
            String fname = String.format( "frame_%05d.png", frame );
            message( "savingFrameMsg", fname );
            File f = new File( dir, fname );
            RenderedImage img = fd.getFrame();
            conv.setSourceImage( img );
            saveImage( conv.getConvertedImage(), f );
            setProgress( frame, 0, totalFrames );
            frame++;
        }
        return null;
    }

    /**
     Called when the task succeeds
     @param result
     */
    @Override
    protected void succeeded( Object result ) {
    }
    
    /**
     Helper function to save image into a  file
     @param img The image to be saved
     @param f File into which the image will be saved
     */
    private void saveImage( RenderedImage img, File f ) {
        // Find a writer for that file extensions
        // Try to determine the file type based on extension
        String ftype = "jpg";
        String imageFname = f.getName();
        int extIndex = imageFname.lastIndexOf( "." ) + 1;
        if ( extIndex > 0 ) {
            ftype = imageFname.substring( extIndex );
        }

        ImageWriter writer = null;
        Iterator iter = ImageIO.getImageWritersBySuffix( ftype );
        writer = (ImageWriter) iter.next();

        if ( writer != null ) {
            ImageOutputStream ios = null;
            try {
                // Prepare output file
                ios = ImageIO.createImageOutputStream( f );
                writer.setOutput( ios );
                // Set some parameters
                ImageWriteParam param = writer.getDefaultWriteParam();
                writer.write( null, new IIOImage( img, null, null ), param );

                // Cleanup
                ios.flush();

            } catch ( IOException ex ) {
                failed( ex );
            } finally {
                if ( ios != null ) {
                    try {
                        ios.close();
                    } catch ( IOException e ) {
                        failed( e );
                    }
                }
                writer.dispose();
            }
        }        
    }    
}
