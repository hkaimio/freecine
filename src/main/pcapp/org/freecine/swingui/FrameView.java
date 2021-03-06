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
import org.freecine.filmscan.Perforation;
import org.freecine.filmscan.ScanStrip;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.ColorModel;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import javax.media.jai.operator.ScaleDescriptor;

/**
 Simple viewer component for movie frames.
 
 @author  harri
 */
public class FrameView extends javax.swing.JPanel {
    
    ColorConverter conv;
    
    /** Creates new form FrameView */
    public FrameView() {
        initComponents();
        conv = new ColorConverter();
    }
    
    FrameViewMode mode = FrameViewMode.DRAW_FRAME;
    
    /**
     Get the current drawing mode
     @return The drawing mode, see {@link FrameViewMode} for details
     */
    public FrameViewMode getMode() {
        return mode;
    }
    
    /**
     Set the mode how the frame should be drawn
     @return One of the values defined in {@link FrameViewMode}
     */
    public void setMode( FrameViewMode mode ) {
        this.mode = mode;
        repaint();
    }
 
    FrameDescriptor currentFrame;
    
    /**
     The area of the current perforation in the screen or <code>null</code> if
     it is not visible.
     */
    Rectangle perfRect;
    
    /**
     Cached copy scaled to the screen resolution.
     */
    RenderedImage scaledImage = null;
    
    /**
     Set the current frame to be displayed
     @param d FrameDescriptor for the current frame
     */
    public void setFrame( FrameDescriptor d ) {
        this.currentFrame = d;
        conv.setSourceImage( d.getFrame() );
        setWhite( d.getWhite() );
        setBlack( d.getBlack() );
        scaledImage = null;
        repaint();
    }

    
    private int white = 0xffff;
    
    private int black = 0;

    /**
     Get the current white point
     @return
     */
    public int getWhite() {
        return conv.getWhite();
    }

    /**
     Set white point
     @param white
     */
    public void setWhite( int white ) {
        conv.setWhite(white);
        scaledImage = null;
        repaint();
    }

    /**
     Get current point
     @return
     */
    public int getBlack() {
        return conv.getBlack();
    }

    /**
     Set black point
     @param black
     */
    public void setBlack( int black ) {
        conv.setBlack(black);
        scaledImage = null;
        repaint();
    }
    
    
    /**
     Paint the current frame
     @param g
     */
    @Override
    public void paint( Graphics g ) {
        super.paint(g);
        switch ( mode ) {
            case DRAW_FRAME:
                paintFrame(g);
                break;
            case DRAW_PERFORATION:
                paintPerforation( g );
        }
    }
    
    /**
     Paint the current frame, with color/cropping corrections and scaled to 
     fit component size
     @param g
     */
    void paintFrame( Graphics g ) {
        if ( currentFrame == null ) {
            return;
        }
        if ( scaledImage == null || 
                (scaledImage.getWidth() != getWidth() && 
                scaledImage.getHeight() != getHeight() ) ) {
            
            RenderedImage img = conv.getConvertedImage();
            float scaleH = (float)getWidth()/(float)img.getWidth();
            float scaleV = (float)getHeight()/(float)img.getHeight();
            float scale = Math.min( scaleV, scaleH );
            scaledImage = ScaleDescriptor.create( img, scale, scale, 0.0f, 0.0f, null, null );
        }
        ((Graphics2D)g).drawRenderedImage(scaledImage, AffineTransform.getScaleInstance(1.0, 1.0));        
    }

    /**
     Paint the "raw" strip so that current perforation is centered and show the
     perforation boundaries as an overlay
     @param g
     */
    private void paintPerforation( Graphics g ) {
        if ( currentFrame == null ) {
            return;
        }
        RenderedImage stripImage = currentFrame.getStrip().getStripImage();
        Perforation p = currentFrame.getPerforation();
        int perfX = p.x;
        int perfY = p.y;
        
        // Try to set the perforation at the middle of the view
        int ty = perfY-(getHeight()/2);
        if ( ty < 0 ) {
            ty = 0;
        }
        if ( ty+getHeight() > stripImage.getHeight() ) {
            ty = stripImage.getHeight() - getHeight();
        }
        
        Graphics2D g2 = (Graphics2D) g.create();
        AffineTransform t = AffineTransform.getTranslateInstance(0, -ty);
        g2.drawRenderedImage(stripImage, t);
        
        // Next, draw the outline of the recognized perforation
        int perfHeight = 217;
        int perfWidth = 170;
        int cornerRadius = 25;
        
        perfRect = new Rectangle( perfX-perfWidth+cornerRadius, 
                perfY-ty-perfHeight/2, 
                perfWidth, perfHeight );
        
        g2.setColor( new Color( 255, 20, 20, 64 ) );
        g2.setStroke( new BasicStroke( 5 ) );
        g2.drawRoundRect( (int)perfRect.getMinX(), (int)perfRect.getMinY(), 
                perfWidth, perfHeight, cornerRadius*2, cornerRadius*2);

        g2.setColor( Color.RED );
        g2.setStroke( new BasicStroke( 1 ) );
        g2.drawRoundRect( (int)perfRect.getMinX(), (int)perfRect.getMinY(), 
                perfWidth, perfHeight, cornerRadius*2, cornerRadius*2);
        
        // If we are dragging the perforation, draw the new position
        
        if ( isDragging ) {
            g2.setStroke( new BasicStroke( 1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1.0f, new float[]{5.0f, 5.0f}, 0.0f ) );
        g2.drawRoundRect( 
                (int)perfRect.getMinX() + dragDx, 
                (int)perfRect.getMinY() + dragDy, 
                perfWidth, perfHeight, cornerRadius*2, cornerRadius*2);
        }
        
        
    }
    
    
    /** This method is called from within the constructor to
     initialize the form.
     WARNING: Do NOT modify this code. The content of this method is
     always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                formMousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                formMouseReleased(evt);
            }
        });
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
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
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    /**
     Called when the component is resized. Schedule repaint
     @param evt The resize event
     */
    private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized
        repaint();
    }//GEN-LAST:event_formComponentResized

    /**
     Called when mouse is moved (but not dragged). Set the cursor based on whether 
     we are on top of perforation or not
     @param evt
     */
    private void formMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseMoved
        if ( mode == FrameViewMode.DRAW_PERFORATION && perfRect != null ) {
            if ( perfRect.contains(evt.getPoint() ) ) {
                setCursor( Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR) );
            } else {
                setCursor( Cursor.getDefaultCursor() );
            }
        }
    }//GEN-LAST:event_formMouseMoved

    /**
     Start point of ongoin drag or <code>null</code> if no drag is ongoing
     */
    Point dragStart;
    /**
     X coordinate difference in current drag
     */
    int dragDx = 0;
    /**
     Y coordinate difference in current drag
     */
    int dragDy = 0;
    /**
     Is a drag ongoing?
     */
    boolean isDragging = false;
    
    /**
     Called when mouse is pressed. Store the position co calculate drag
     @param evt
     */
    private void formMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMousePressed
       if ( mode == FrameViewMode.DRAW_PERFORATION && perfRect != null ) {
            if ( perfRect.contains( evt.getPoint() ) ) {
                dragStart = evt.getPoint();
                isDragging = true;
            }
        }   
    }//GEN-LAST:event_formMousePressed

    /**
     Called when mouse is dragged. Update dragDx and dragDy & schedule a 
     repaint.
     @param evt
     */
    private void formMouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseDragged
        if ( dragStart != null ) {
            dragDx = evt.getX() - (int)dragStart.getX();
            dragDy = evt.getY() - (int)dragStart.getY();
            repaint();
        }
    }//GEN-LAST:event_formMouseDragged

    /**
     Called when mouse is released. If user was dragging the perforation, 
     update its position.
     @param evt
     */
    private void formMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseReleased
        if ( isDragging) {
            isDragging = false;
            dragStart = null;

            // Move the perforation
            ScanStrip strip = currentFrame.getStrip();
            int perfNum = currentFrame.getStripFrameNum();
            Perforation p = strip.getPerforation( perfNum );
            strip.setPerforation( perfNum, p.x + dragDx, p.y + dragDy );
            repaint();
        }
    }//GEN-LAST:event_formMouseReleased

    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
    
}
