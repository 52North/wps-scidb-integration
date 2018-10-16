/*
 * TODO: Add License Header
 */
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.n52.scidb.wcs.model;

import org.n52.scidb.wcs.exception.TimeStampNotFoundException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

/**
 *
 * @author Maurin Radtke <m.radtke@52north.org>
 */
public class AreaOfInterest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private int height;
    private int width;
    private double minX;
    private double minY;
    private double maxX;
    private double maxY;
    private ArrayList<Layer> layerMapping;
    private int totalAttributes;
    private ArrayList<Date> timeStamps;
    private String scidbArrayName;
    private String epsg;
    private String crs;

    public AreaOfInterest() {
        this.layerMapping = new ArrayList();
        this.totalAttributes = 0;
        this.timeStamps = new ArrayList();
    }

    public AreaOfInterest(String name, int width, int height, double minX, double minY, double maxX, double maxY, String scidbArrayName, String epsg, String crs) {
        this.name = name;
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
        this.epsg = epsg;
        this.crs = crs;
        this.layerMapping = new ArrayList();
        this.totalAttributes = 0;
        this.timeStamps = new ArrayList();
        this.scidbArrayName = scidbArrayName;
    }

    public String getEpsg() {
        return epsg;
    }

    public void setEpsg(String epsg) {
        this.epsg = epsg;
    }

    public String getCrs() {
        return crs;
    }

    public void setCrs(String crs) {
        this.crs = crs;
    }

    public String getScidbArrayName() {
        return scidbArrayName;
    }

    public void setScidbArrayName(String scidbArrayName) {
        this.scidbArrayName = scidbArrayName;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public Layer getLayerByName(String layerName) {
        for (Layer lm : this.layerMapping) {
            if (lm.getLayerName().equals(layerName)) {
                return lm;
            }
        }
        return null;
    }

    public int getTotalAttributes() {
        return totalAttributes;
    }

    public void setTotalAttributes(int totalAttributes) {
        this.totalAttributes = totalAttributes;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<Layer> getLayerMapping() {
        return layerMapping;
    }

    public void setLayerMapping(ArrayList<Layer> layerMapping) {
        this.layerMapping = layerMapping;
    }

    public ArrayList<Date> getTimeStamps() {
        return timeStamps;
    }

    public void setTimeStamps(ArrayList<Date> timeStamps) {
        this.timeStamps = timeStamps;
    }

    public void addTimeStamp(Date timeStamp) {
        this.timeStamps.add(timeStamp);
    }

    public void addLayer(Layer layer) {
        this.layerMapping.add(layer);
    }

    public int getIndexByTimeStamp(Date date) throws TimeStampNotFoundException {
        for (int i = 0; i < this.timeStamps.size(); i++) {
            Date currTimeStamp = this.timeStamps.get(i);
            if (date.equals(currTimeStamp)) {
                return i;
            }
        }
        throw new TimeStampNotFoundException();
    }

}
