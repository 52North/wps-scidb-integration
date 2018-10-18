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
public class Interpolate extends ColorMap {

    private ArrayList<StyleColorMapEntry> entries;

    public Interpolate(String lookUpValue, ArrayList<StyleColorMapEntry> colorEntries, Color fallBackColor) {
        super.setFallBackColor(fallBackColor);
        super.setLookUpValue(lookUpValue);
        this.entries = colorEntries;
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
        for (int i = 0; i < this.entries.size() - 1; i++) {
            StyleColorMapEntry lowerBound = this.entries.get(i);
            StyleColorMapEntry higherBound = this.entries.get(i + 1);
            if (value >= lowerBound.getQuantity() &&
                    value < higherBound.getQuantity()) {
                int redStart = lowerBound.getColor().getRed();
                int greenStart = lowerBound.getColor().getGreen();
                int blueStart = lowerBound.getColor().getBlue();
                int redEnd = higherBound.getColor().getRed();
                int greenEnd = higherBound.getColor().getGreen();
                int blueEnd = higherBound.getColor().getBlue();
                double ratio = (value - lowerBound.getQuantity()) / (higherBound.getQuantity() - lowerBound.getQuantity());
                return (((int) (redStart + ratio * (redEnd - redStart))) << 16)
                        | (((int) (greenStart + ratio * (greenEnd - greenStart))) << 8)
                        | ((int) (blueStart + ratio * (blueEnd - blueStart)));
            }
        }
        return (super.getFallBackColor().getRed() << 16)
                | (super.getFallBackColor().getGreen() << 8)
                | super.getFallBackColor().getBlue();
    }

}
