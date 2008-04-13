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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 Panel for showing the preview image and selecting area that will be scanned.
 @author  harri
 */
public class PreviewPane extends javax.swing.JPanel {
    
    static private Logger log = Logger.getLogger( PreviewPane.class.getName() );
    
    /** Creates new form PreviewPane */
    public PreviewPane() {
        super();
        log.setLevel( Level.FINE );
        initComponents();
    }

    BufferedImage previewImage;
    BufferedImage scaledImage;
    
    int magnWidth = 100;
    
    public void setPreviewImage( BufferedImage img ) {
        BufferedImage oldPreview = previewImage;
        previewImage = img;
        scaledImage = null;
        firePropertyChange( "previewImage", oldPreview, img );
        repaint();
    }
    
    public BufferedImage getPreviewImage() {
        return previewImage;
    }
    
    
    
    Rectangle2D selection;
    
    public Rectangle2D getSelection() {
        return selection;
    }
    
    public void setSelection( Rectangle2D s ) {
        Rectangle2D oldSel = selection;
        selection = s;
        firePropertyChange( "selection", oldSel, s );
        repaint();
    }
    
    /**
     Scanner coordinate associated with top left corner of the preview image
     */
    Rectangle2D previewArea = new Rectangle2D.Double( 0.0, 0.0, 21.0, 29.7 );
    
    /**
     Get the scanner space coordinate for the top left corner of preview image
     @return The top left point coordinate
     */
    public Rectangle2D getPreviewArea() {
        return previewArea;
    }
    
    /**
     Set the coordinate associated with top left corner of preview image
     @param p
     */
    public void setPreviewArea( Rectangle2D p ) {
        Rectangle2D oldArea = previewArea;
        previewArea = p;
        firePropertyChange( "previewArea", oldArea, p );
        repaint();
    }

    /**
     Paint the component
     @param g
     */
    @Override
    public void paint( Graphics g ) {
        log.entering( "PreviewPane", "paint" );
        super.paint( g );
        
        if ( previewImage != null ) {
            if ( scaledImage == null ) {
                scaleImage();
            }
            ((Graphics2D) g).drawRenderedImage( scaledImage, null );
            
            // Draw the magnification window
            int imageX = (int)((double)lastMouseX / scale) - magnWidth / 2;
            int imageY = (int)((double)lastMouseY / scale) - magnWidth / 2;
            
            imageX = Math.max( 0, imageX );
            imageX = Math.min( previewImage.getWidth()-magnWidth, imageX );
            imageY = Math.max( 0, imageY );
            imageY = Math.min( previewImage.getHeight() - magnWidth, imageY );
            BufferedImage magnImage = previewImage.getSubimage( imageX, imageY, magnWidth, magnWidth );
            ((Graphics2D) g).drawImage( magnImage, null, 0, getHeight() - magnWidth );
            
        }
        
        if ( selection != null && scaledImage != null ) {
            // Calculate device space coordinates for the selection rectangle
            double w = scaledImage.getWidth();
            double h = scaledImage.getHeight();
            double tlx = w * (( selection.getMinX()-previewArea.getMinX() ) / previewArea.getWidth());
            double tly = h * (( selection.getMinY()-previewArea.getMinY() ) / previewArea.getHeight());
            double selW = w * selection.getWidth() / previewArea.getWidth();
            double selH = h * selection.getHeight() / previewArea.getHeight();
            Graphics2D g2 = (Graphics2D)g.create();
            
            // Draw the area twice to get altering black/white lines
            g2.setStroke( new BasicStroke( 1.0f, BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_ROUND, 1.0f,
                    new float[]{5.0f, 5.0f}, 0.0f ) );
            g2.drawRect( (int) tlx, (int) tly, (int) selW, (int) selH );
            g2.setStroke( new BasicStroke( 1.0f, BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_ROUND, 1.0f,
                    new float[]{5.0f, 5.0f}, 5.0f ) );
            g2.setColor(Color.WHITE );
            g2.drawRect( (int) tlx, (int) tly, (int) selW, (int) selH );
        }
        log.exiting( "PreviewPane", "paint" );
    }
    
    double scale = 1.0;
    
    /**
     Calculate scaledIamge from previewImage so that it fits in the control. The
     scale of scaledImage is stored in scale member.
     */
    private void scaleImage() {
        int iw = previewImage.getWidth();
        int ih = previewImage.getHeight();
        int compWidth = getWidth();
        int compHeight = getHeight();
        
        double xscale = (double)compWidth / (double) iw;
        double yscale = (double)compHeight / (double) ih;
        scale = Math.min( xscale, yscale );
        AffineTransform xform = AffineTransform.getScaleInstance( scale, scale );
        AffineTransformOp xformOp = new AffineTransformOp( xform, null );

        scaledImage = xformOp.createCompatibleDestImage( previewImage, null );
        xformOp.filter( previewImage, scaledImage );
    }    
    
    /** This method is called from within the constructor to
     initialize the form.
     WARNING: Do NOT modify this code. The content of this method is
     always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setName("Form"); // NOI18N
        addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                formMousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                formMouseReleased(evt);
            }
        });
        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                formMouseDragged(evt);
            }
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                formMouseMoved(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 476, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 576, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    int dragStartX = -1; 
    int dragStartY = -1; 
    
    /**
     Mouse pressed - star selection of a new area
     @param evt
     */
    private void formMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMousePressed
        log.entering("PreviewPane", "formMousePressed");
        dragStartX = evt.getX();
        dragStartY = evt.getY();
        log.exiting( "PreviewPane", "formMousePressed" );
    }//GEN-LAST:event_formMousePressed

    private void formMouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseDragged
        log.entering("PreviewPane", "formMouseDragged");

        lastMouseX = evt.getX();
        lastMouseY = evt.getY();
        
        if ( scaledImage == null ) {
            return;
        }
        Point2D p1 = screenToDev( dragStartX, dragStartY );
        Point2D p2 = screenToDev( evt.getX(), evt.getY() );
        Rectangle2D s = new Rectangle2D.Double(p1.getX(), p1.getY(), 0.0, 0.0);
        s.add( p2 );
        setSelection( s );
        log.exiting("PreviewPane", "formMouseDragged");
    }//GEN-LAST:event_formMouseDragged

    private void formMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseReleased
        log.entering( "PreviewPane", "formMouseReleased" );
        if ( scaledImage == null ) {
            return;
        }
        Point2D p1 = screenToDev( dragStartX, dragStartY );
        Point2D p2 = screenToDev( evt.getX(), evt.getY() );
        Rectangle2D s = new Rectangle2D.Double(p1.getX(), p1.getY(), 0.0, 0.0);
        s.add( p2 );
        setSelection( s );
        log.severe( "new area " + s );
        log.exiting("PreviewPane", "formMouseDragged");
    }//GEN-LAST:event_formMouseReleased

    int lastMouseX = 0;
    int lastMouseY = 0;
    
    private void formMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseMoved
        lastMouseX = evt.getX();
        lastMouseY = evt.getY();
        repaint();
    }//GEN-LAST:event_formMouseMoved
    
    /**
     Convert position from screen coordinates to scanner device coordinates.
     To call this function, the scaledImage must exist as it defines the mapping 
     screen space coordinate system
     @param x
     @param y
     @return
     */
    private Point2D screenToDev( int x, int y ) {
        double w = scaledImage.getWidth();
        double h = scaledImage.getHeight();
        double devx = previewArea.getMinX() + previewArea.getWidth() * (x/w);
        double devy = previewArea.getMinY() + previewArea.getHeight() * (y/h);
        return new Point2D.Double( devx, devy );
    }


    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
    
}
