/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fi.kaimio.moviescan;

import java.util.EventListener;

/**
 Interface for getting notifications about changes to a {@link ScanStrip}
 */
public interface ScanStripListener extends EventListener {
    void scanStripChanged( ScanStrip changedStrip );
}
