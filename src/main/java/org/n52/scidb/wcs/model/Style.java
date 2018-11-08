/*
 * TODO: Add License Header
 */
 /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.n52.scidb.wcs.model;

import java.io.Serializable;
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
    private Double globalOpacity;
    private ColorMap colorMap;

    public Style() {
    }

    public Style(String seName, Channel redChannel, Channel greenChannel, Channel blueChannel, ColorMap colorMap, double opacity) {
        this.sldName = seName;
        this.redChannel = redChannel;
        this.greenChannel = greenChannel;
        this.blueChannel = blueChannel;
        this.globalOpacity = opacity;
        this.colorMap = colorMap;
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

    public ColorMap getColorMap() {
        return colorMap;
    }

    public void setColorMap(ColorMap colorMap) {
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
        alpha = 255;
        if (this.globalOpacity != null
                && this.globalOpacity >= 0
                && this.globalOpacity <= 1) {
            alpha = (int) (globalOpacity * 255);
        }
    }

    public int getStyleAppliedPixel(String[] cellValue, int startAttributeIndex) {
        if (cellValue[0].equals("NOT_FOUND")) {
            return (0 << 24)
                    | (0 << 16)
                    | (0 << 8)
                    | 0;
        }
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
        return (alpha << 24)
                | this.colorMap.getRenderedPixel(value);
    }

}
