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
public class RasterPixelArray implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private double minX;
    private double minY;
    private double maxX;
    private double maxY;
    private int epsg;
    private int numBands;
    private int height;
    private int widht;
    private String arrayName;

    public RasterPixelArray(int epsg, double minX, double minY, double maxX, double maxY, int numBands, int height, int widht, String arrayName) {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
        this.numBands = numBands;
        this.height = height;
        this.widht = widht;
        this.arrayName = arrayName;
        this.epsg = epsg;
    }

    public int getNumBands() {
        return numBands;
    }

    public void setNumBands(int numBands) {
        this.numBands = numBands;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidht() {
        return widht;
    }

    public void setWidht(int widht) {
        this.widht = widht;
    }

    public String getArrayName() {
        return arrayName;
    }

    public void setArrayName(String arrayName) {
        this.arrayName = arrayName;
    }

    public double getMinX() {
        return minX;
    }

    public void setMinX(double minX) {
        this.minX = minX;
    }

    public double getMinY() {
        return minY;
    }

    public void setMinY(double minY) {
        this.minY = minY;
    }

    public double getMaxX() {
        return maxX;
    }

    public void setMaxX(double maxX) {
        this.maxX = maxX;
    }

    public double getMaxY() {
        return maxY;
    }

    public void setMaxY(double maxY) {
        this.maxY = maxY;
    }

    public int getEpsg() {
        return epsg;
    }

    public void setEpsg(int epsg) {
        this.epsg = epsg;
    }
    
    @Override
    public String toString() {
        return "RasterArray[Bbox=("+minX+","+minY+"|"+maxX+","+maxY+"), numBands="+numBands+", arrayName="+arrayName+"]";
    }

}