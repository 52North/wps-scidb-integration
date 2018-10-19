/*
 * TODO: Add License Header
 */
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.n52.scidb.util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import org.apache.commons.lang3.math.NumberUtils;
import org.n52.scidb.wcs.model.AreaOfInterests;
import org.n52.scidb.wcs.model.Layer;
import org.n52.scidb.wcs.model.Style;
import org.n52.scidb.wcs.services.SciDBService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/**
 *
 * @author Maurin Radtke <m.radtke@52north.org>
 */
@Component
public class ImagePNG implements OutputFormat {

    public static final Logger LOG = LoggerFactory.getLogger(ImagePNG.class);

    @Autowired
    SciDBService sciService;

    HttpHeaders headers = new HttpHeaders();
    HttpHeaders jsonHeaders = new HttpHeaders();

    @Autowired
    AreaOfInterests aois;

    public byte[] getOutputFormat(ArrayList<Layer> layers, ArrayList<Style> styles, int time, int width, int height, String[] pixels, int resultWidth, int resultHeight) {
        BufferedImage image = new BufferedImage(time * resultWidth, resultHeight, BufferedImage.TYPE_INT_ARGB);
        int old = -1;
        Style layerStyle = styles.get(0);
        
        for (int t = 0; t < time; t++) {
            for (int x = 0; x < resultWidth; x++) {
                for (int y = 0; y < resultHeight; y++) {
                    int index = t * (resultWidth * resultHeight) + (x * resultHeight) + y;
                    if (index != old + 1) {
                        LOG.error("GETMAP PIXEL INDEX ERROR");
                    }
                    old = index;
                    String[] pixel = pixels[index].split(",");
                    
                    int p = layerStyle.getStyleAppliedPixel(pixel, layers.get(0).getStartAttributeIndex());
                    image.setRGB(t * (resultWidth - 1) + x, y, p);
                }
            }
        }
        LOG.info("filling image: 100%");
        Path fileStoragePath = Paths.get("wms", "Output.png");
        File f = new File(fileStoragePath.toUri());
        try {
            ImageIO.write(image, "png", f);
        } catch (Exception ex) {
            LOG.error(ex + " : " + ex.getMessage());
            return null;
        }
        BufferedImage bufferedImage;
        try {
            bufferedImage = ImageIO.read(f);
        } catch (IOException ex) {
            LOG.error(ex + " : " + ex.getMessage());
            return null;
        }

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
