/*
 * TODO: Add License Header
 */
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.n52.scidb.util;

import java.util.ArrayList;
import org.n52.scidb.wcs.model.Layer;
import org.n52.scidb.wcs.model.Style;

/**
 *
 * @author Maurin Radtke <m.radtke@52north.org>
 */
public interface OutputFormat {

    public Object getOutputFormat(ArrayList<Layer> layers, ArrayList<Style> styles, int time, int width, int height, String[] pixels);

    public Object getOutputFormat(ArrayList<Layer> layers, ArrayList<Style> styles, int time, int width, int height, String[] pixels, int resultWidth, int resultHeight);

}