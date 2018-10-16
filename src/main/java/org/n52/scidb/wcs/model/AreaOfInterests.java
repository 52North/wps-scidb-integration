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
import java.util.ArrayList;
import org.springframework.stereotype.Component;

/**
 *
 * @author Maurin Radtke <m.radtke@52north.org>
 */
@Component
public class AreaOfInterests implements Serializable {

    private static final long serialVersionUID = 1L;

    private ArrayList<AreaOfInterest> aois;

    public AreaOfInterests() {
        this.aois = new ArrayList();
    }

    public ArrayList<AreaOfInterest> getAoiArray() {
        return aois;
    }

    public void setAoiArray(ArrayList<AreaOfInterest> aoiArray) {
        this.aois = aoiArray;
    }
    
    public AreaOfInterest getAOIByLayerName(String layerName) {
        for (AreaOfInterest aoi : this.aois) {
            ArrayList<Layer> layerList = aoi.getLayerMapping();
            for (Layer layer : layerList) {
                if (layer.getLayerName().equals(layerName)) {
                    return aoi;
                }
            }
        }
        return null;
    }

    public AreaOfInterest getAreaOfInterestByName(String aoiName) {
        for (AreaOfInterest aoi : this.aois) {
            if (aoi.getName().equals(aoiName)) {
                return aoi;
            }
        }
        return null;
    }
    
    public Layer getLayerByName(String layerName) {
        for (AreaOfInterest aoi : this.aois) {
            ArrayList<Layer> layerList = aoi.getLayerMapping();
            for (Layer layer : layerList) {
                if (layer.getLayerName().equals(layerName)) {
                    return layer;
                }
            }
        }
        return null;
    }
    
    public void addAreaOfInterest(AreaOfInterest aoi) {
        this.aois.add(aoi);
    }
    
    public void upgradeAreaOfInterest(AreaOfInterest aoi) {
        for (AreaOfInterest currAoI : this.aois) {
            if (currAoI.getName().equals(aoi.getName())) {
                currAoI.setCrs(aoi.getCrs());
                currAoI.setEpsg(aoi.getEpsg());
                currAoI.setHeight(aoi.getHeight());
                currAoI.setWidth(aoi.getWidth());
                currAoI.setMaxX(aoi.getMaxX());
                currAoI.setMinX(aoi.getMinX());
                currAoI.setMinY(aoi.getMinY());
                currAoI.setMaxY(aoi.getMaxY());
                currAoI.setTimeStamps(aoi.getTimeStamps());
                currAoI.setTotalAttributes(aoi.getTotalAttributes());
                currAoI.setScidbArrayName(aoi.getScidbArrayName());
                currAoI.setLayerMapping(aoi.getLayerMapping());
            }
        }
    }
}