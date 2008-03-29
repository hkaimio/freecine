/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.freecine.swingui;

import com.sun.media.jai.util.SunTileCache;
import java.awt.Dimension;
import javax.media.jai.JAI;
import org.jdesktop.application.SingleFrameApplication;

/**
 Startup logic for Moviescan application
 */
public class Moviescan extends SingleFrameApplication {

    MainWindow mainWindow;
    
    @Override
    protected void initialize( String[] args ) {
        JAI.setDefaultTileSize( new Dimension( 64, 64 ) );
        JAI.getDefaultInstance().setTileCache( new SunTileCache( 100 * 1024 * 1024 ) );
        JAI.getDefaultInstance().getTileScheduler().setParallelism( 4 );
    }
    
    @Override
    protected void startup() {
        mainWindow = new MainWindow();
        mainWindow.setVisible( true );
    }

    public static void main( String[] args ) {
        launch( Moviescan.class, args );
    }

}
