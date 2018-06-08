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
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 *
 * @author Maurin Radtke <m.radtke@52north.org>
 */
//@Component
//@Service
@ConfigurationProperties("org.n52.scidb.wcs")
public class RasterPixelArrays implements Serializable {
    
    private static final long serialVersionUID = 1L;

    private ArrayList<RasterPixelArray> rasterPixelArrays;

    public RasterPixelArrays() {
        this.rasterPixelArrays = new ArrayList();
    }

    public ArrayList<RasterPixelArray> getRasterPixelArrays() {
        return rasterPixelArrays;
    }

    public void setRasterPixelArrays(ArrayList<RasterPixelArray> rasterPixelArrays) {
        this.rasterPixelArrays = rasterPixelArrays;
    }
    
    public void addRasterPixelArray(RasterPixelArray rpa) {
        this.rasterPixelArrays.add(rpa);
    }
    
    public RasterPixelArray getArrayByName(String arrayName) {
        for (RasterPixelArray current : this.rasterPixelArrays) {
            if (current.getArrayName().equals(arrayName)) {
                return current;
            }
        }
        return null;
    }

}