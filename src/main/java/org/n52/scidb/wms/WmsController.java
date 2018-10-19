/*
 * TODO: Add License Header
 */
 /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.n52.scidb.wms;

import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Optional;
import org.n52.scidb.util.ImagePNG;
import org.n52.scidb.util.NetCDF;
import org.n52.scidb.util.OutputFormat;
import org.n52.scidb.wcs.exception.TimeStampNotFoundException;
import org.n52.scidb.wcs.model.AreaOfInterest;
import org.n52.scidb.wcs.model.AreaOfInterests;
import org.n52.scidb.wcs.model.Layer;
import org.n52.scidb.wcs.model.Style;
import org.n52.scidb.wcs.model.Styles;
import org.n52.scidb.wcs.services.SciDBService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Maurin Radtke <m.radtke@52north.org>
 */
@Component
@RestController
@RequestMapping("/wms")
public class WmsController {

    public static final Logger LOG = LoggerFactory.getLogger(WmsController.class);

    @Autowired
    SciDBService sciService;

    HttpHeaders headers = new HttpHeaders();
    HttpHeaders jsonHeaders = new HttpHeaders();

    URI storageUri;

    @Autowired
    AreaOfInterests aois;

    @Autowired
    Styles styles;

    @RequestMapping(value = "/getMapWMS", method = RequestMethod.GET)
    public ResponseEntity getMap(
            @RequestParam("LAYERS") String wmsLayers,
            @RequestParam("STYLES") String wmsStyles,
            @RequestParam("CRS") String wmsCRS,
            @RequestParam("BBOX") String wmsBbox,
            @RequestParam("WIDTH") String wmsWidth,
            @RequestParam("HEIGHT") String wmsHeight,
            @RequestParam("FORMAT") String wmsFormat,
            @RequestParam("TRANSPARANT") Optional<Boolean> wmsTransparent,
            @RequestParam("BGCOLOR") Optional<String> wmsBgColor,
            @RequestParam("EXCEPTIONS") Optional<String> wmsExceptions,
            @RequestParam("TIME") Optional<String> wmsTime,
            @RequestParam("ELEVATION") Optional<String> elevation
    ) {
        long start_total = System.currentTimeMillis();

        // 1. manage LAYERS:
        AreaOfInterest aoi = null;
        ArrayList<Layer> layerStack = new ArrayList<>();
        String[] layersList = wmsLayers.split(",");
        for (String layerName : layersList) {
            Layer currentLayer = aois.getLayerByName(layerName);
            if (currentLayer == null) {
                return new ResponseEntity("{\"error\": \"Layer '" + layerName + "' not found.\"}", jsonHeaders, HttpStatus.NOT_FOUND);
            }
            layerStack.add(currentLayer);
            // GET AreaOfInterest:
            aoi = aois.getAOIByLayerName(layerName);
            if (aoi == null) {
                return new ResponseEntity("{\"error\": \"Layer '" + layerName + "' not found.\"}", jsonHeaders, HttpStatus.NOT_FOUND);
            }
        }

        // manage STYLES:
        String[] stylesList = wmsStyles.split(",");
        ArrayList<Style> matchingStyles = new ArrayList();
        for (int i = 0; i < stylesList.length; i++) {
            String styleName = stylesList[i];
            Style matchedStyle = styles.getStyleByName(styleName);
            if (styleName.isEmpty()) {
                Layer layer = layerStack.get(i);
                matchedStyle = layer.getDefaultStyle();
            }
            if (matchedStyle == null) {
                return new ResponseEntity<>("{\"error\": \"Requested Style '" + styleName + "' not found.\"}", HttpStatus.BAD_REQUEST);
            }
            matchingStyles.add(matchedStyle);
        }

        // 2. manage TIME:
        SimpleDateFormat ISO8601DATEFORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        ArrayList<Integer> timeStack = new ArrayList<>();
        if (wmsTime.isPresent()) {
            String[] timeList = wmsTime.get().split(",");
            for (String timeParam : timeList) {
                Date date;
                String timeStamp = timeParam.replaceAll("\\+0([0-9]){1}\\:00", "+0$100");
                try {
                    date = ISO8601DATEFORMAT.parse(timeStamp);
                } catch (ParseException ex) {
                    LOG.error("IllegalArgument '" + timeParam + "' for TIME parameter.");
                    return new ResponseEntity("{\"error\": \"Unsupported TimeStamp format for TIME parameter '" + timeParam + "'.\"}", jsonHeaders, HttpStatus.BAD_REQUEST);
                }
                int timeStampIndex;
                try {
                    timeStampIndex = aoi.getIndexByTimeStamp(date);
                    timeStack.add(timeStampIndex + 1);
                } catch (TimeStampNotFoundException ex) {
                    LOG.error("Timestamp '" + timeParam + "' not found.");
                    return new ResponseEntity("{\"error\": \"Timestamp '" + timeParam + "' not found.\"}", jsonHeaders, HttpStatus.BAD_REQUEST);
                }
            }
        }

        // 3. manage BBOX:
        double x_min = aoi.getMinX();
        double x_max = aoi.getMaxX();
        double y_min = aoi.getMinY();
        double y_max = aoi.getMaxY();

        String[] bboxParams = wmsBbox.split(",");
        double bboxMinX = Double.parseDouble(bboxParams[0]);
        double bboxMinY = Double.parseDouble(bboxParams[1]);
        double bboxMaxX = Double.parseDouble(bboxParams[2]);
        double bboxMaxY = Double.parseDouble(bboxParams[3]);

        if (bboxMinX < x_min) {
            return new ResponseEntity("{\"error\": \"BoundingBox parameter 1st argument '" + bboxMinX + "' exceeds AreaOfInterest '" + aoi.getName() + "'s coverage of '" + x_min + "'.\"}", jsonHeaders, HttpStatus.NOT_FOUND);
        }
        if (bboxMinY > y_max) {
            return new ResponseEntity("{\"error\": \"BoundingBox parameter 2nd argument '" + bboxMinY + "' exceeds AreaOfInterest '" + aoi.getName() + "'s coverage of '" + y_max + "'.\"}", jsonHeaders, HttpStatus.NOT_FOUND);
        }
        if (bboxMaxX > x_max) {
            return new ResponseEntity("{\"error\": \"BoundingBox parameter 3rd argument '" + bboxMaxX + "' exceeds AreaOfInterest '" + aoi.getName() + "'s coverage of '" + x_max + "'.\"}", jsonHeaders, HttpStatus.NOT_FOUND);
        }
        if (bboxMaxY < y_min) {
            return new ResponseEntity("{\"error\": \"BoundingBox parameter 4th argument '" + bboxMaxY + "' exceeds AreaOfInterest '" + aoi.getName() + "'s coverage of '" + y_min + "'.\"}", jsonHeaders, HttpStatus.NOT_FOUND);
        }
        if (bboxMinX > bboxMaxX) {
            return new ResponseEntity("{\"error\": \"Invalid Bounding Box Arguments. Lower Corner '" + bboxMinX + "' is greather than Upper Corner '" + bboxMaxX + "'.\"}", jsonHeaders, HttpStatus.BAD_REQUEST);
        }
        if (bboxMinY > bboxMaxY) {
            return new ResponseEntity("{\"error\": \"Invalid Bounding Box Arguments. Lower Corner '" + bboxMinY + "' is greather than Upper Corner '" + bboxMaxY + "'.\"}", jsonHeaders, HttpStatus.BAD_REQUEST);
        }
        int height = aoi.getHeight();
        int width = aoi.getWidth();

        // calculate between pixels from bbox and SciDB:
        int xStart = getPixelCoordinate(bboxMinX,
                x_min, x_max, width);
        int xEnd = getPixelCoordinate(bboxMaxX,
                x_min, x_max, width);
        int yEnd = height - getPixelCoordinate(bboxMinY,
                y_min, y_max, height);
        int yStart = height - getPixelCoordinate(bboxMaxY,
                y_min, y_max, height);

        int tStart = timeStack.get(0);
        int tEnd = timeStack.get(timeStack.size() - 1);

        String sessionID = sciService.new_session();
        if (sessionID == null || sessionID.isEmpty()) {
            sciService.release_session(sessionID);
            return null;
        }
        // scan array for validation:
        String query = "between(" + aoi.getScidbArrayName() + "," + tStart + "," + xStart + "," + yStart + "," + tEnd + "," + (xEnd - 1) + "," + (yEnd - 1) + ")";
        long time_start = System.currentTimeMillis();
        String response = sciService.execute_query(query, "dcsv", sessionID);
        long time_end = System.currentTimeMillis();
        long time_diff = (time_end - time_start) / 1000;
        time_start = System.currentTimeMillis();

        // 4. manage wmsWidth and Height:
        int resultWidth;
        try {
            resultWidth = Integer.parseInt(wmsWidth);
        } catch (NumberFormatException nfe) {
            return new ResponseEntity("{\"error\": \"WIDTH parameter '" + wmsWidth + "' is not supported.\"}", jsonHeaders, HttpStatus.BAD_REQUEST);
        }
        int resultHeight;
        try {
            resultHeight = Integer.parseInt(wmsHeight);
        } catch (NumberFormatException nfe) {
            return new ResponseEntity("{\"error\": \"HEIGHT parameter '" + wmsHeight + "' is not supported.\"}", jsonHeaders, HttpStatus.BAD_REQUEST);
        }
        String[] pixels = getSampledResultPixel(
                (tEnd - tStart + 1),
                (xEnd - xStart),
                (yEnd - yStart),
                resultWidth,
                resultHeight,
                sciService.readCells(sessionID, (tEnd - tStart + 1), (xEnd - xStart), (yEnd - yStart)));

        if (response.startsWith("UserQueryException")) {
            return new ResponseEntity("{\"error\": \"Array '" + aoi.getName() + "' not found.\", \"exception:\": \"" + response + "\"}", jsonHeaders, HttpStatus.NOT_FOUND);
        } else {
            LOG.info(" ");
            LOG.info("^^^^^^^^^^^^^^^^^^^^^getMap()^^^^^^^^^^^^^^^^^^^^^^^^^");
            LOG.info("SCIDB: between operation:         " + time_diff + "s.");
            switch (wmsFormat) {
                case "image/png":
                    OutputFormat of = new ImagePNG();
                    byte[] media = (byte[]) of.getOutputFormat(
                            layerStack,
                            matchingStyles,
                            (tEnd - tStart + 1),
                            (xEnd - xStart),
                            (yEnd - yStart),
                            pixels,
                            resultWidth,
                            resultHeight
                    );
                    time_end = System.currentTimeMillis();
                    time_diff = (time_end - time_start) / 1000;
                    LOG.info("JAVA: filling output pixel:       " + time_diff + "s.");
                    headers = new HttpHeaders();
                    headers.setContentType(MediaType.IMAGE_JPEG);
                    headers.setContentType(MediaType.IMAGE_PNG);
                    long end_total = System.currentTimeMillis();
                    LOG.info("TOTAL:                            " + (end_total - start_total) / 1000 + "s.");
                    LOG.info("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
                    LOG.info(" ");
                    return new ResponseEntity(media, headers, HttpStatus.OK);
                case "netCDF":
                    of = new NetCDF(x_min, y_min, x_max, y_max);
                    media = (byte[]) of.getOutputFormat(
                            layerStack,
                            matchingStyles,
                            (tEnd - tStart + 1),
                            (xEnd - xStart),
                            (yEnd - yStart),
                            pixels,
                            resultWidth,
                            resultHeight);
                    time_end = System.currentTimeMillis();
                    time_diff = (time_end - time_start) / 1000;
                    LOG.info("JAVA: filling output pixel:       " + time_diff + "s.");
                    headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                    end_total = System.currentTimeMillis();
                    LOG.info("TOTAL:                            " + (end_total - start_total) / 1000 + "s.");
                    LOG.info("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
                    LOG.info(" ");
                    return new ResponseEntity(media, headers, HttpStatus.OK);
                default:
                    //
                    break;
            }
            return new ResponseEntity("{\"error\": \"Unsupported OutputFormat '" + wmsFormat + "'.\"}", jsonHeaders, HttpStatus.BAD_REQUEST);
        }
    }

    private int getPixelCoordinate(double coord, double min, double max, int range) {
        return (int) ((coord - min)
                / (max - min) * range);
    }

    private String[] getSampledResultPixel(
            int time,
            int width,
            int height,
            int resultWidth,
            int resultHeight,
            String[] readCells) {
        String[] result = new String[time * resultWidth * resultHeight];

        double ratioResultWidth = ((double) width / resultWidth);
        double ratioResultHeight = ((double) height / resultHeight);
        int resultPixelCounter = 0;
        for (int t = 0; t < time; t++) {
            for (int rwStep = 0; rwStep < resultWidth; rwStep++) {
                for (int rhStep = 0; rhStep < resultHeight; rhStep++) {
                    int h = (int) (rhStep * ratioResultHeight);
                    int w = (int) (rwStep * ratioResultWidth);
                    int origIndex = t * (width * height)
                            + w * height + h;
                    String currValue = readCells[origIndex];
                    result[resultPixelCounter] = currValue;
                    resultPixelCounter++;
                }
            }
        }

        return result;
    }

}
