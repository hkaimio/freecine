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

import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import javax.media.jai.JAI;
import javax.media.jai.LookupTableJAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.ColorConvertDescriptor;
import javax.media.jai.operator.LookupDescriptor;


/**
 Wrapper around the iamge processing chain from source image in  linear color 
 space tho the final image in sRGB color space.
 
 @author harri
 */
class ColorConverter {

    /**
     Source image
     */
    RenderedImage srcImg;
    
    /**
     Image with lut for back/white point & exposure adjustment applied
     */
    RenderedOp lutImg;
    
    /**
     The final iamge converted to sRGB color space
     */
    RenderedOp convertedImg;
    
    /**
     Black point
     */
    int black = 0;
    
    /**
     White point
     */
    int white = 0xffff;
    
    /**
     PropertyChangeSupport for for notifications
     */
    PropertyChangeSupport changeSupport;

    /**
     Parameter block for the lookup table operation
     */
    private ParameterBlockJAI lutParams;

    public ColorConverter() {
        super();
        changeSupport = new PropertyChangeSupport( this );
        lutParams = new ParameterBlockJAI( new LookupDescriptor() );
    }
    
    /**
     Add a new listener
     @param l
     */
    public void addPropertyChangeListener( PropertyChangeListener l ) {
        changeSupport.addPropertyChangeListener(l);
    }

    /**
     Remove a listener
     @param l
     */
    public void removePropertyChangeListener( PropertyChangeListener l ) {
        changeSupport.removePropertyChangeListener(l);
    }

    /**
     Set black point
     @param newBlack Pixel value for thee new black point
     */
    public void setBlack( int newBlack ) {
        int oldBlack = black;
        black = newBlack;
        calcLUT();
        changeSupport.firePropertyChange( "black", oldBlack, newBlack );
    }

    /**
     Get current black point
     @return
     */
    public int getBlack() {
        return black;
    }

    /**
     Set white point
     @param newWhite
     */
    public void setWhite( int newWhite ) {
        int oldWhite = white;
        white = newWhite;
        calcLUT();
        changeSupport.firePropertyChange( "white", oldWhite, newWhite );
    }

    /**
     Get current white point
     @return
     */
    public int getWhite() {
        return white;
    }
    
    /**
     Set the source image for the pipeline
     @param img
     */
    public void setSourceImage( RenderedImage img ) {
        RenderedImage oldSrc = srcImg;
        srcImg = img;
        lutParams.setSource( srcImg, 0 );
        updateLutImgParams();
        changeSupport.firePropertyChange( "sourceImage", oldSrc, srcImg );
    }
    
    /**
     Get the resulting image
     @return
     */
    public RenderedOp getConvertedImage() {
        if ( convertedImg == null ) {
            buildPipeline();
        }
        return convertedImg;
    }
    
    /**
     Update parameter block of the lutImg if it has been created
     */
    private void updateLutImgParams() {
        if ( lutImg != null ) {
            lutImg.setParameterBlock(lutParams);
        }
    }
    
        /**
     Lookup table for correcting exposure in linear color space
     */
    
    short[] lut = new short[0x10000];

    /**
     Build the pipeline
     */
    private void buildPipeline() {
        // Apply LUT to correct black and white points
        calcLUT();
        
        lutImg = JAI.create( "lookup", lutParams );
        ColorModel cm =
                new ComponentColorModel( ColorSpace.getInstance( ColorSpace.CS_sRGB ),
                false, false, ColorModel.OPAQUE, DataBuffer.TYPE_USHORT );

        convertedImg = ColorConvertDescriptor.create( lutImg, cm, null  );
    }

    
    /**
     Calculate the LUT for exposure correction
     */
    private void calcLUT() {

        for ( int n = 0; n < lut.length; n++ ) {
            double src = ((double) n) / 65535.0;
            double dst = ((double) (n - black)) / ((double) (white - black));
            int dstInt = (int) (65535.0 * dst);
            dstInt = Math.max( 0, dstInt );
            dstInt = Math.min( 65535, dstInt );
            lut[n] = (short) dstInt;
        }
        LookupTableJAI jailut = new LookupTableJAI( lut, true );
        lutParams.setParameter( "table", jailut );
        updateLutImgParams();
    }
    
}
