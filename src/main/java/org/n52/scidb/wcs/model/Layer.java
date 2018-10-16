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

/**
 *
 * @author Maurin Radtke <m.radtke@52north.org>
 */
public class Layer implements Serializable {
 
    private String layerName;
    private Integer startAttributeIndex;
    private Integer endAttributeIndex;
    private Style defaultStyle;
    
    public Layer(String layerName, int startAttributeIndex, int endAttributeIndex, Style defaultStyle) {
        this.layerName = layerName;
        this.startAttributeIndex = startAttributeIndex;
        this.endAttributeIndex = endAttributeIndex;
        this.defaultStyle = defaultStyle;
    }

    public Style getDefaultStyle() {
        return defaultStyle;
    }

    public void setDefaultStyle(Style defaultStyle) {
        this.defaultStyle = defaultStyle;
    }
    
    public String getLayerName() {
        return layerName;
    }

    public void setLayerName(String layerName) {
        this.layerName = layerName;
    }

    public Integer getStartAttributeIndex() {
        return startAttributeIndex;
    }

    public void setStartAttributeIndex(Integer startAttributeIndex) {
        this.startAttributeIndex = startAttributeIndex;
    }

    public Integer getEndAttributeIndex() {
        return endAttributeIndex;
    }

    public void setEndAttributeIndex(Integer endAttributeIndex) {
        this.endAttributeIndex = endAttributeIndex;
    }
    
}