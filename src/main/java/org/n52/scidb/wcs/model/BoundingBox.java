/*
 * TODO: Add License Header
 */
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.n52.scidb.wcs.model;

/**
 *
 * @author Maurin Radtke <m.radtke@52north.org>
 */
public class BoundingBox {
    
    private double x_min, x_max, y_min, y_max;

    public BoundingBox(double x_min, double x_max, double y_min, double y_max) {
        this.x_min = x_min;
        this.x_max = x_max;
        this.y_min = y_min;
        this.y_max = y_max;
    }

    public double getX_min() {
        return x_min;
    }

    public void setX_min(double x_min) {
        this.x_min = x_min;
    }

    public double getX_max() {
        return x_max;
    }

    public void setX_max(double x_max) {
        this.x_max = x_max;
    }

    public double getY_min() {
        return y_min;
    }

    public void setY_min(double y_min) {
        this.y_min = y_min;
    }

    public double getY_max() {
        return y_max;
    }

    public void setY_max(double y_max) {
        this.y_max = y_max;
    }
    
}
