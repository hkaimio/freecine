/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fi.kaimio.moviescan.ui;

import org.jdesktop.application.SingleFrameApplication;

/**
 Startup logic for Moviescan application
 */
public class Moviescan extends SingleFrameApplication {

    MainWindow mainWindow;
    
    @Override
    protected void startup() {
        mainWindow = new MainWindow();
        mainWindow.setVisible( true );
    }

    public static void main( String[] args ) {
        launch( Moviescan.class, args );
    }

}
