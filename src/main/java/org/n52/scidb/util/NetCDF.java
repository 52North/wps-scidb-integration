/*
 * TODO: Add License Header
 */
 /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.n52.scidb.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import org.n52.scidb.wcs.model.AreaOfInterests;
import org.n52.scidb.wcs.model.Layer;
import org.n52.scidb.wcs.model.Style;
import org.n52.scidb.wcs.services.SciDBService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import ucar.ma2.Array;
import ucar.ma2.ArrayDouble;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

/**
 *
 * @author Maurin Radtke <m.radtke@52north.org>
 */
@Component
public class NetCDF implements OutputFormat {

    public static final Logger LOG = LoggerFactory.getLogger(NetCDF.class);

    @Autowired
    SciDBService sciService;

    HttpHeaders headers = new HttpHeaders();
    HttpHeaders jsonHeaders = new HttpHeaders();

    @Autowired
    AreaOfInterests aois;

    private double x1;
    private double y1;
    private double x2;
    private double y2;

    public NetCDF() {

    }

    public NetCDF(double x1, double y1, double x2, double y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    @Override
    public byte[] getOutputFormat(ArrayList<Layer> layers, ArrayList<Style> styles, int time, int width, int height, String[] pixels, int resultWidth, int resultHeight) {
        String uuid = UUID.randomUUID().toString();
        String location = "wms/" + uuid + ".nc";

        NetcdfFileWriter writer = null;
        try {
            writer = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, location, null);
        } catch (IOException ex) {
            LOG.error("Creating netCDF3 fileWriter failed at initialization.");
            return null;
        }

        Dimension latDim = writer.addDimension(null, "lat", resultHeight);
        Dimension lonDim = writer.addDimension(null, "lon", resultWidth);
        Dimension timeDim = writer.addDimension(null, "time", time);

        List<Dimension> dims = new ArrayList<>();
        dims.add(latDim);
        dims.add(lonDim);
        dims.add(timeDim);

        Variable lat = writer.addVariable(null, "lat", DataType.FLOAT, "lat");
        lat.addAttribute(new Attribute("units", "degrees_north"));
        Variable lon = writer.addVariable(null, "lon", DataType.FLOAT, "lon");
        lon.addAttribute(new Attribute("units", "degrees_east"));

        Variable timeV = writer.addVariable(null, "time", DataType.INT, "time");
        timeV.addAttribute(new Attribute("units", "hours since 1990-01-01"));

        Style layerStyle = styles.get(0);

        int amount = pixels[0].split(",").length;

        for (int val = 0; val < amount; val++) {
            Variable v = writer.addVariable(null, "a" + (val + 1), DataType.DOUBLE, "lat lon time");
            v.addAttribute(new Attribute("units", "unknown"));
        }

        double ratioLongitude = (x2 - x1) / resultWidth;
        double ratioLatitude = (y2 - y1) / resultHeight;

        float[] lats = new float[resultHeight];
        for (int i = 0; i < resultHeight; i++) {
            lats[i] = (float) (y1 + i * ratioLatitude);
        }
        float[] lons = new float[resultWidth];
        for (int i = 0; i < resultWidth; i++) {
            lons[i] = (float) (x1 + i * ratioLongitude);
        }

        try {
            writer.create();
            writer.close();
        } catch (IOException ex) {
            LOG.error("writer.create() failed at creation." + ex.getMessage());
            return null;
        }

        try {
            writer = NetcdfFileWriter.openExisting(location);
            Variable lat_new = writer.findVariable("lat");
            writer.write(lat_new, Array.factory(lats));
            Variable lon_new = writer.findVariable("lon");
            writer.write(lon_new, Array.factory(lons));
        } catch (InvalidRangeException ex) {
            LOG.error("error with range: " + ex);
        } catch (IOException ex) {
            LOG.error("writer.openExisting() failed at opening file." + ex.getMessage());
            return null;
        }

        for (int val = 0; val < amount; val++) {

            int[] origin = new int[]{0, 0, 0};
            int[] time_origin = new int[]{0};

            Variable v_new = writer.findVariable("a" + (val + 1));
            int[] shape = v_new.getShape();
            ArrayDouble A = new ArrayDouble.D3(shape[0], shape[1], shape[2]);
            Index ima = A.getIndex();
            for (int t = 0; t < shape[2]; t++) {
                for (int i = 0; i < shape[1]; i++) {
                    for (int j = 0; j < shape[0]; j++) {
                        int index = t * (resultHeight * resultWidth) + (i * resultHeight) + j;
                        String[] pixel = pixels[index].split(",");

                        int p = layerStyle.getStyleAppliedPixel(pixel, layers.get(0).getStartAttributeIndex());

                        // longitude:
//                        String pValue = pixel[val];
                        A.setDouble(
                                ima.set(
                                        shape[0] - j - 1, // latitude
                                        i, // longitude
                                        //                                        ((int) ratioLongitude * i + x1),  // longitude
                                        t // time
                                ) // latitude
                                ,
                                 p);
//                        A.setDouble(ima.set(i, j, t), (double) (Integer.parseInt(pValue)));
                    }
                }
            }
            try {
                writer.write(v_new, origin, A);
            } catch (Exception ex) {
                LOG.error("writer.write failed at writing." + ex.getMessage());
                return null;
            }
        }

        try {
            writer.close();
        } catch (IOException ex) {
            LOG.error("writer.close() failed at closing." + ex.getMessage());
            return null;
        }

        Path fileStoragePath = Paths.get("wms", uuid + ".nc");
        File f = new File(fileStoragePath.toUri());

        // get DataBufferBytes from Raster
        byte[] fileContent;

        try {
            fileContent = Files.readAllBytes(f.toPath());
            return fileContent;
        } catch (IOException ex) {
            LOG.error(ex.getMessage());
            return null;
        }

    }

    @Override
    public Object getOutputFormat(ArrayList<Layer> layers, ArrayList<Style> styles, int time, int width, int height, String[] pixels) {
        return getOutputFormat(layers, styles, time, width, height, pixels, width, height);
    }

}
