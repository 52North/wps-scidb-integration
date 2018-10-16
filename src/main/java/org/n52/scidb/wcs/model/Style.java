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
import java.util.ArrayList;
import java.util.Collections;
import org.springframework.stereotype.Component;

/**
 *
 * @author Maurin Radtke <m.radtke@52north.org>
 */
@Component
public class Style implements Serializable {

    private String sldName;
    private Channel redChannel;
    private Channel greenChannel;
    private Channel blueChannel;
    private ArrayList<StyleColorMapEntry> colorMap;
    private String colorMapType;
    private Double globalOpacity;

    public Style() {
    }

    public Style(String sldName, Channel redChannel, Channel greenChannel, Channel blueChannel, ArrayList<StyleColorMapEntry> colorMap, double opacity) {
        this.sldName = sldName;
        this.redChannel = redChannel;
        this.greenChannel = greenChannel;
        this.blueChannel = blueChannel;
        this.colorMap = colorMap;
        this.colorMapType = "ramp";
        this.globalOpacity = opacity;
        setUpForApply();
    }

    public Style(String sldName, Channel redChannel, Channel greenChannel, Channel blueChannel, ArrayList<StyleColorMapEntry> colorMap, String colorMapType, double opacity) {
        this.sldName = sldName;
        this.redChannel = redChannel;
        this.greenChannel = greenChannel;
        this.blueChannel = blueChannel;
        this.colorMap = colorMap;
        this.colorMapType = colorMapType;
        this.globalOpacity = opacity;
        setUpForApply();
    }

    public String getSldName() {
        return sldName;
    }

    private void setSldName(String sldName) {
        this.sldName = sldName;
    }

    public double getOpacity() {
        return globalOpacity;
    }

    private void setOpacity(double opacity) {
        this.globalOpacity = opacity;
    }

    public String getColorMapType() {
        return colorMapType;
    }

    private void setColorMapType(String colorMapType) {
        this.colorMapType = colorMapType;
    }

    public Channel getRedChannel() {
        return redChannel;
    }

    private void setRedChannel(Channel redChannel) {
        this.redChannel = redChannel;
    }

    public Channel getGreenChannel() {
        return greenChannel;
    }

    private void setGreenChannel(Channel greenChannel) {
        this.greenChannel = greenChannel;
    }

    public Channel getBlueChannel() {
        return blueChannel;
    }

    private void setBlueChannel(Channel blueChannel) {
        this.blueChannel = blueChannel;
    }

    public ArrayList<StyleColorMapEntry> getColorMap() {
        return colorMap;
    }

    private void setColorMap(ArrayList<StyleColorMapEntry> colorMap) {
        this.colorMap = colorMap;
    }

    private int sciDBAttrRed = -1;
    private int sciDBAttrGreen = -1;
    private int sciDBAttrBlue = -1;
    private int alpha = 1;
    private boolean hasMultipleChannelsSelected = false;

    private void setUpForApply() {
        sciDBAttrRed = this.redChannel == null
                ? -1
                : Integer.parseInt(this.redChannel.getChannelName()) - 1;
        sciDBAttrGreen = this.greenChannel == null
                ? -1
                : Integer.parseInt(this.greenChannel.getChannelName()) - 1;
        sciDBAttrBlue = this.blueChannel == null
                ? -1
                : Integer.parseInt(this.blueChannel.getChannelName()) - 1;

        int channelCount = 0;
        if (sciDBAttrRed != -1) {
            channelCount++;
        }
        if (sciDBAttrGreen != -1) {
            channelCount++;
        }
        if (sciDBAttrBlue != -1) {
            channelCount++;
        }
        if (channelCount > 1) {
            hasMultipleChannelsSelected = true;
        }
        Collections.sort(this.colorMap);
        alpha = 255;
        if (this.globalOpacity != null
                && this.globalOpacity >= 0
                && this.globalOpacity <= 1) {
            alpha = (int) (globalOpacity * 255);
        }
    }

    public int getStyleAppliedPixel(String[] cellValue, int startAttributeIndex) {
        int cellValueRed = 0;
        int value = 0;
        if (sciDBAttrRed != -1) {
            cellValueRed = Integer.parseInt(cellValue[(startAttributeIndex - 1) + sciDBAttrRed]);
            value = cellValueRed;
        }
        int cellValueGreen = 0;
        if (sciDBAttrGreen != -1) {
            cellValueGreen = Integer.parseInt(cellValue[(startAttributeIndex - 1) + sciDBAttrGreen]);
            value = cellValueGreen;
        }
        int cellValueBlue = 0;
        if (sciDBAttrBlue != -1) {
            cellValueBlue = Integer.parseInt(cellValue[(startAttributeIndex - 1) + sciDBAttrBlue]);
            value = cellValueBlue;
        }
        int a = 255;
        if (hasMultipleChannelsSelected) {
            return (a << 24)
                    | (cellValueRed << 16)
                    | (cellValueGreen << 8)
                    | cellValueBlue;
        }
        switch (colorMapType) {
            case "values":
                return getPixelByValuesStyle(value);
            case "intervals":
                return getPixelByIntervalsStyle(value);
            case "ramp":
            default:
                return getPixelByRampStyle(value);
        }
    }

    private int getPixelByValuesStyle(int value) {
        Color c = Color.decode("#000000");
        boolean rendered = false;
        for (StyleColorMapEntry colorEntry : colorMap) {
            if (value == colorEntry.getQuantity()) {
                c = colorEntry.getColor();
                alpha = (int) (255 * colorEntry.getOpacity());
                rendered = true;
            } else {
                break;
            }
        }
        if (rendered) {
            return (alpha << 24) | (c.getRed() << 16) | (c.getGreen() << 8) | c.getBlue();
        } else {
            return (1 << 24) | (1 << 16) | (1 << 8) | 1;
        }
    }

    private int getPixelByIntervalsStyle(int value) {
        boolean rendered = false;
        Color c = null;
        for (StyleColorMapEntry colorEntry : colorMap) {
            if (value >= colorEntry.getQuantity()) {
                c = colorEntry.getColor();
                alpha = (int) (255 * colorEntry.getOpacity());
                rendered = true;
            } else {
                break;
            }
        }
        if (rendered) {
            return (alpha << 24) | (c.getRed() << 16) | (c.getGreen() << 8) | c.getBlue();
        } else {
            return (1 << 24) | (1 << 16) | (1 << 8) | 1;
        }
    }

    private int getPixelByRampStyle(int value) {
        for (int i = 0; i < colorMap.size() - 1; i++) {
            StyleColorMapEntry lowerBound = colorMap.get(i);
            StyleColorMapEntry higherBound = colorMap.get(i + 1);
            if (value >= lowerBound.getQuantity()
                    && value < higherBound.getQuantity()) {
                int redStart = lowerBound.getColor().getRed();
                int greenStart = lowerBound.getColor().getGreen();
                int blueStart = lowerBound.getColor().getBlue();
                int redEnd = higherBound.getColor().getRed();
                int greenEnd = higherBound.getColor().getGreen();
                int blueEnd = higherBound.getColor().getBlue();
                double ratio = (value - lowerBound.getQuantity()) / (higherBound.getQuantity() - lowerBound.getQuantity());
                alpha = (int) (255 * (lowerBound.getOpacity() + ratio * (higherBound.getOpacity() - lowerBound.getOpacity())));
                return (alpha << 24)
                        | (((int) (redStart + ratio * (redEnd - redStart))) << 16)
                        | (((int) (greenStart + ratio * (greenEnd - greenStart))) << 8)
                        | ((int) (blueStart + ratio * (blueEnd - blueStart)));
            }
        }
        return (1 << 24) | (1 << 16) | (1 << 8) | 1;
    }

}
