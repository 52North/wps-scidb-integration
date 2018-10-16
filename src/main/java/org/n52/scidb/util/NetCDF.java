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

    public NetCDF() {
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

        Dimension lonDim = writer.addDimension(null, "longitude", resultWidth);
        Dimension latDim = writer.addDimension(null, "latitude", resultHeight);
        Dimension timeDim = writer.addDimension(null, "time", time);

        List<Dimension> dims = new ArrayList<>();
        dims.add(lonDim);
        dims.add(latDim);
        dims.add(timeDim);

        int amount = pixels[0].split(",").length;

        for (int val = 0; val < amount; val++) {

            Variable v = writer.addVariable(null, "a" + (val + 1), DataType.DOUBLE, dims);
            v.addAttribute(new Attribute("units", "unknown"));
            Array data = Array.factory(int.class, new int[]{3}, new int[]{1, 2, 3});
            v.addAttribute(new Attribute("scale", data));
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
        } catch (IOException ex) {
            LOG.error("writer.openExisting() failed at opening file." + ex.getMessage());
            return null;
        }

        for (int val = 0; val < amount; val++) {
            Variable v_new = writer.findVariable("a" + (val + 1));
            int[] shape = v_new.getShape();
            ArrayDouble A = new ArrayDouble.D3(shape[0], shape[1], shape[2]);
            Index ima = A.getIndex();
            for (int t = 0; t < shape[2]; t++) {
                for (int i = 0; i < shape[0]; i++) {
                    for (int j = 0; j < shape[1]; j++) {
                        int index = t * (resultHeight * resultWidth) + (i * resultHeight) + j;
                        String[] pixel = pixels[index].split(",");
                        String pValue = pixel[val];
                        A.setDouble(ima.set(i, j, t), (double) (Integer.parseInt(pValue)));
                    }
                }
            }

            int[] origin = new int[3];
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
