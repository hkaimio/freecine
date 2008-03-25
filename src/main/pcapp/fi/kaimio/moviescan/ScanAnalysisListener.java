/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fi.kaimio.moviescan;

import java.util.EventListener;

/**
 Listener for following up scan analysis
 */
public interface ScanAnalysisListener extends EventListener {
    void scanAnalysisProgress( int currentLine, int totalLines );
    void scanAnalysisComplete( int perfCount );
}
