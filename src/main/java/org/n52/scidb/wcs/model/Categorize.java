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
import java.util.ArrayList;

/**
 *
 * @author Maurin Radtke <m.radtke@52north.org>
 */
public class Categorize extends ColorMap {

    private Color startColor;
    private ArrayList<StyleColorMapEntry> entries;

    public Categorize(String lookUpValue, Color startColor, ArrayList<StyleColorMapEntry> colorEntries, Color fallBackColor) {
        super.setFallBackColor(fallBackColor);
        super.setLookUpValue(lookUpValue);
        this.startColor = startColor;
        this.entries = colorEntries;
    }

    public Color getStartColor() {
        return startColor;
    }

    public void setStartColor(Color startColor) {
        this.startColor = startColor;
    }

    public ArrayList<StyleColorMapEntry> getEntries() {
        return entries;
    }

    public void setEntries(ArrayList<StyleColorMapEntry> entries) {
        this.entries = entries;
    }

    public void addColorEntry(StyleColorMapEntry entry) {
        if (entries == null) {
            entries = new ArrayList<StyleColorMapEntry>();
        }
        this.entries.add(entry);
    }

    @Override
    public int getRenderedPixel(int value) {
        boolean rendered = false;
        Color c = null;
        if (!entries.isEmpty()) {
            if (value < entries.get(0).getQuantity()) {
                c = this.startColor;
                return (c.getRed() << 16)
                        | (c.getGreen() << 8)
                        | (c.getBlue());
            }
        }
        for (StyleColorMapEntry entry : entries) {
            if (value >= entry.getQuantity()) {
                c = entry.getColor();
                rendered = true;
            } else {
                break;
            }
        }
        if (rendered) {
            return (c.getRed() << 16)
                    | (c.getGreen() << 8)
                    | (c.getBlue());
        }
        return (super.getFallBackColor().getAlpha() << 24)
                | (super.getFallBackColor().getRed() << 16)
                | (super.getFallBackColor().getGreen() << 8)
                | super.getFallBackColor().getBlue();
    }

}
