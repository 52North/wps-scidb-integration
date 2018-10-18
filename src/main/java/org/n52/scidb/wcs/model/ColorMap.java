/*
 * TODO: Add License Header
 */
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.n52.scidb.wcs.model;

import java.awt.Color;

/**
 *
 * @author Maurin Radtke <m.radtke@52north.org>
 */
public abstract class ColorMap {
    
    private Color fallBackColor;
    private String lookUpValue;

    public abstract int getRenderedPixel(int value);

    public Color getFallBackColor() {
        return fallBackColor;
    }

    public void setFallBackColor(Color fallBackColor) {
        this.fallBackColor = fallBackColor;
    }

    public String getLookUpValue() {
        return lookUpValue;
    }

    public void setLookUpValue(String lookUpValue) {
        this.lookUpValue = lookUpValue;
    }
    
}
