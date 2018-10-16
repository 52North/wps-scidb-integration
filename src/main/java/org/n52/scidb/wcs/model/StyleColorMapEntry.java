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
import java.io.Serializable;

/**
 *
 * @author Maurin Radtke <m.radtke@52north.org>
 */
public class StyleColorMapEntry implements Serializable, Comparable<StyleColorMapEntry> {

    private Color color;
    private double quantity;
    private double opacity;

    public StyleColorMapEntry(String color, Double quantity, Double opacity) {
        this.color = Color.decode(color);
        this.quantity = quantity;
        if (opacity != null
                && opacity >= 0
                && opacity <= 1) {
            this.opacity = opacity;
        } else {
            this.opacity = 1.0;
        }
    }

    public double getOpacity() {
        return opacity;
    }

    public void setOpacity(double opacity) {
        this.opacity = opacity;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = Color.decode(color);
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    @Override
    public int compareTo(StyleColorMapEntry t) {
        return Double.compare(this.getQuantity(), t.getQuantity());
    }

}
