/*
 * TODO: Add License Header
 */
 /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.n52.scidb.wcs.controller;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;
import javax.annotation.PostConstruct;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.OverviewPolicy;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.referencing.NamedIdentifier;
import org.n52.scidb.wcs.exception.ReadingGeoTiffMetadataException;
import org.n52.scidb.wcs.exception.TimeStampNotFoundException;
import org.n52.scidb.wcs.model.AreaOfInterest;
import org.n52.scidb.wcs.model.AreaOfInterests;
import org.n52.scidb.wcs.model.Channel;
import org.n52.scidb.wcs.model.Layer;
import org.n52.scidb.wcs.model.PathToFile;
import org.n52.scidb.wcs.model.Style;
import org.n52.scidb.wcs.model.StyleColorMapEntry;
import org.n52.scidb.wcs.services.SciDBService;
import org.opengis.coverage.grid.GridCoordinates;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.InvalidParameterValueException;
import org.opengis.parameter.ParameterValue;
import org.opengis.referencing.ReferenceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Maurin Radtke <m.radtke@52north.org>
 */
@RestController
@RequestMapping("/api")
public class InsertGeoTiffController {

    private static final int BUFFER_SIZE = 4096;

    public static final Logger LOG = LoggerFactory.getLogger(InsertGeoTiffController.class);

    @Autowired
    SciDBService sciService;

    HttpHeaders headers = new HttpHeaders();
    HttpHeaders jsonHeaders = new HttpHeaders();

    URI storageUri;

    @Autowired
    AreaOfInterests aois;

    AreaOfInterest newAoI;

    // ######################## GeoTiff Metadata fields ########################
    private int width;
    private int height;
    private double minX;
    private double minY;
    private double maxX;
    private double maxY;
    private String epsg;
    private String crs;

    @PostConstruct
    private void init() {
        jsonHeaders.add("Content-Type", "application/json");
//        String sessionID = sciService.new_session();
//        sciService.execute_query("remove(DHUENNTALSPERRE)", "", sessionID);
    }

    @RequestMapping(value = "aois/{aoi}/layer", method = RequestMethod.POST)
    public ResponseEntity<?> postLayer(
            @PathVariable("aoi") String aoiName,
            @RequestParam("fileURL") String fileURL,
            @RequestParam("timeStamp") String timeStamp,
            @RequestParam("layerName") String layerName,
            @RequestParam("layerColorComponents") String layerColorComponents
    ) {
        long post_total_start = System.currentTimeMillis();
        long post_createInsert_start, post_createInsert_end;
        long post_java_start, post_java_end;
        post_java_start = System.currentTimeMillis();
        // get AoI:
        AreaOfInterest areaOfInterest = aois.getAreaOfInterestByName(aoiName);
        if (areaOfInterest == null) {
            LOG.warn("{\"Warn\": \"AreaOfInterest '" + aoiName + "' not found.\"}");
            return new ResponseEntity<>("{\"error\": \"AreaOfInterest '" + aoiName + "' not found.\"}", jsonHeaders, HttpStatus.NOT_FOUND);
        }
        newAoI = new AreaOfInterest();

        // get timeStamp Date:
        SimpleDateFormat ISO8601DATEFORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        timeStamp = timeStamp.replaceAll("\\+0([0-9]){1}\\:00", "+0$100");
        Date date;
        try {
            date = ISO8601DATEFORMAT.parse(timeStamp);
        } catch (ParseException ex) {
            LOG.error("Unsupported timestamp format." + ex);
            return new ResponseEntity<>("{\"error\": \"Provided timestamp '" + timeStamp + "' is not supported. Supported timestamp formats are ISO8601 formats.\"}", jsonHeaders, HttpStatus.BAD_REQUEST);
        }

        // get file from fileUrl:
        if (!saveFileToDisk(fileURL)) {
            LOG.error("Could not download file from fileURL '" + fileURL + "'.");
            return new ResponseEntity<>("{\"error\": \"Could not download file from file URL '" + fileURL + "'.\"}", jsonHeaders, HttpStatus.BAD_REQUEST);
        }

        // get GeoTiff file metadata:
        try {
            determineGeoTiffMetadata(fileURL);
        } catch (ReadingGeoTiffMetadataException e) {
            LOG.error("{\"error\": \"Reading the Metadata of the GeoTiff threw an Exception: " + e + ".\"}");
            return new ResponseEntity<>("{.\"error\": \"Reading Metadata of file '" + fileURL + "' failed.\"}", jsonHeaders, HttpStatus.BAD_REQUEST);
        }

        // compare validity for GeoTiff metadata with AoI metadata:
        if (areaOfInterest.getScidbArrayName() == null) {
            areaOfInterest.setScidbArrayName(areaOfInterest.getName());
        }

        if (areaOfInterest.getTimeStamps().isEmpty()) {
            newAoI.setWidth(width);
            newAoI.setHeight(height);
            newAoI.setMinX(minX);
            newAoI.setMinY(minY);
            newAoI.setMaxX(maxX);
            newAoI.setMaxY(maxY);
            newAoI.setCrs(crs);
            newAoI.setEpsg(epsg);
            newAoI.setTimeStamps(areaOfInterest.getTimeStamps());
            newAoI.setLayerMapping(areaOfInterest.getLayerMapping());
            newAoI.setScidbArrayName(areaOfInterest.getScidbArrayName());
            newAoI.setTotalAttributes(areaOfInterest.getTotalAttributes());
            newAoI.setName(areaOfInterest.getName());
        } else {
            if ((areaOfInterest.getWidth() != width)
                    || (areaOfInterest.getHeight() != height)
                    || (areaOfInterest.getMinX() != minX)
                    || (areaOfInterest.getMaxX() != maxX)
                    || (areaOfInterest.getMinY() != minY)
                    || (areaOfInterest.getMaxY() != maxY)
                    || (!areaOfInterest.getCrs().equals(crs))
                    || (!areaOfInterest.getEpsg().equals(epsg))) {
                LOG.error("AreaOfInterest '" + areaOfInterest.getName() + "'s Metadata do not match with Metadata of inserting GeoTiff file.!");
                return new ResponseEntity<>("{\"error\": \"AreaOfInterest '" + areaOfInterest.getName() + "'s Metadata do not match with Metadata of inserting GeoTiff file.\"}", jsonHeaders, HttpStatus.BAD_REQUEST);
            } else {
                newAoI.setWidth(width);
                newAoI.setHeight(height);
                newAoI.setMinX(minX);
                newAoI.setMaxX(maxX);
                newAoI.setMinY(minY);
                newAoI.setMaxY(maxY);
                newAoI.setCrs(crs);
                newAoI.setEpsg(epsg);
                newAoI.setTimeStamps(areaOfInterest.getTimeStamps());
                ArrayList<Layer> layerList = new ArrayList<>();
                for (Layer layer : areaOfInterest.getLayerMapping()) {
                    layerList.add(layer);
                }
                newAoI.setLayerMapping(layerList);
                newAoI.setScidbArrayName(areaOfInterest.getScidbArrayName());
                newAoI.setTotalAttributes(areaOfInterest.getTotalAttributes());
                newAoI.setName(areaOfInterest.getName());
            }
        }

        // mark all indices as nullabled
        HashMap<Integer, Integer> indexMap = new HashMap<>();
        ArrayList<Layer> newLayers = new ArrayList();

        String[] layersNames = layerName.split(",");
        String[] colorComponents = layerColorComponents.split(",");
        if (layersNames.length != colorComponents.length) {
            LOG.error("{\"error\": \"Number of layerNames '" + layersNames.length + "' does not match with number of layerColorComponents '" + colorComponents.length + "'.\"}");
            return new ResponseEntity<>("{\"error\": \"Number of layerNames '" + layersNames.length + "' does not match with number of layerColorComponents '" + colorComponents.length + "'.\"}", HttpStatus.BAD_REQUEST);
        }
        int layerCounter = 1;
        for (int i = 0; i < layersNames.length; i++) {
            String currentLayerName = layersNames[i];
            // parse layer color components:
            int layerColorComponent;
            try {
                layerColorComponent = Integer.parseInt(colorComponents[i]);
            } catch (NumberFormatException e) {
                LOG.error("{\"error\": \"The LayerColorComponent '" + colorComponents[i] + "' is not of type Integer.\"}", HttpStatus.NOT_ACCEPTABLE);
                return new ResponseEntity<>("{\"error\": \"LayerColorComponent '" + colorComponents[i] + "' is not of type Integer.\"}", HttpStatus.NOT_ACCEPTABLE);
            }
            // get layers from aoi if existent, else create new:
            Layer layer = newAoI.getLayerByName(currentLayerName);
            if (layer == null) {
                int numberTotalAttributes = newAoI.getTotalAttributes();
                Style newDefaultStyle = new Style(
                        "Default Style Layer " + currentLayerName,
                        new Channel("" + (numberTotalAttributes + 1)),
                        new Channel("" + (numberTotalAttributes + 2)),
                        new Channel("" + (numberTotalAttributes + 3)),
                        null,
                        1);

                Layer newLayer = new Layer(
                        currentLayerName,
                        numberTotalAttributes + 1,
                        numberTotalAttributes + layerColorComponent,
                        newDefaultStyle);
                newAoI.addLayer(newLayer);
                newLayers.add(newLayer);
                newAoI.setTotalAttributes(newAoI.getTotalAttributes() + (newLayer.getEndAttributeIndex() - newLayer.getStartAttributeIndex() + 1));
                layer = newLayer;
            }
            for (int j = layer.getStartAttributeIndex(); j <= layer.getEndAttributeIndex(); j++) {
                indexMap.put(j, layerCounter);
                layerCounter++;
            }
        }
        for (int i = 1; i <= newAoI.getTotalAttributes(); i++) {
            indexMap.putIfAbsent(i, -1);
        }

        String sessionID = sciService.new_session();
        if (sessionID == null || sessionID.isEmpty()) {
            sciService.release_session(sessionID);
            LOG.error("The service could not create a new SciDB session.");
            return new ResponseEntity<>("{\"error\": \"The service could not create a new SciDB session.\"}", jsonHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        post_java_end = System.currentTimeMillis();
        LOG.info(" ");
        LOG.info("^^^^^^^^^^^^^^^  insertGeoTiff()  ^^^^^^^^^^^^^^^^^^^^");

        if (areaOfInterest.getTimeStamps().isEmpty()) {
            // (A) create new SciDBArray
            LOG.info("{\"job\": \"(A) Create new SciDBArray.\"}");
            String query = "show(" + newAoI.getScidbArrayName() + ")";
            String response = sciService.execute_query(query, "dcsv", sessionID);
            if (response.contains("SCIDB_LE_ARRAY_DOESNT_EXIST")) {
                createArray(newAoI, sessionID, layersNames, colorComponents);
            }
            if (!createInsertArray(fileURL, newAoI, sessionID, indexMap)) {
                return new ResponseEntity<>("{\"error\": \"Could not create an array for the inserted GeoTiff.\"}", jsonHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
            };
            addInsertArrayOnTopOf(areaOfInterest, sessionID, date);
        } else {
            int dateIndex;
            try {
                dateIndex = areaOfInterest.getIndexByTimeStamp(date);
                if (hasNewLayers(areaOfInterest, layersNames)) {
                    // TODO: implement case: timestamp alrdy existent
                    LOG.info("{\"job\": \"(D) SciArray um Attribute erweitern.\"}");
                } else {
                    // TODO: implement case: timestamp alrdy existent
                    LOG.info("{\"error\": \"Case (E) is occured. Now you know the problem. go fix it and try again.\"}");
                }
            } catch (TimeStampNotFoundException ex) {
                if (hasNewLayers(areaOfInterest, layersNames)) {
//                    LOG.info("{\"job\": \"(C) SciDBArray um Attribute der neuen Layer erweitern.\"}");
                    addAttributeToArray(newAoI, sessionID, newLayers);
                }
                if (!createInsertArray(fileURL, newAoI, sessionID, indexMap)) {
                    return new ResponseEntity<>("{\"error\": \"Could not create an array for the inserted GeoTiff.\"}", jsonHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
                };
                addInsertArrayOnTopOf(newAoI, sessionID, date);
            }
            post_createInsert_end = System.currentTimeMillis();
        }

        this.aois.upgradeAreaOfInterest(newAoI);

        long post_total_end = System.currentTimeMillis();
        LOG.info("TOTAL:                                " + (post_total_end - post_total_start) / 1000 + "s.");
        return new ResponseEntity<>(newAoI, jsonHeaders, HttpStatus.OK);
    }

    private void createArray(AreaOfInterest aoi, String sessionID, String[] layersNames, String[] colorComponents) {
        // create 3-dimensional areaOfInterest array:
        String query = "create array " + aoi.getScidbArrayName() + " <";
        int countColors = 0;
        for (int i = 0; i < layersNames.length; i++) {
            for (int j = 0; j < Integer.parseInt(colorComponents[i]); j++) {
                query += "a";
                query += countColors + (j + 1);
                query += ":uint8 null";
                if ((i + 1 < layersNames.length) || (j + 1 != Integer.parseInt(colorComponents[i]))) {
                    query += ",";
                }
            }
            countColors = countColors + Integer.parseInt(colorComponents[i]);
        }
        query += ">[t=0:*:0:10; x=0:" + (width - 1) + ":0:5000; y=0:" + (height - 1) + ":0:5000]";
        sciService.execute_query(query, "", sessionID);
    }

    private void addAttributeToArray(AreaOfInterest aoi, String sessionID, ArrayList<Layer> newLayers) {
        String newSciDBArrayName = "a" + UUID.randomUUID().toString().replaceAll("-", "");
        int countAttrs = 0;
        ArrayList<Layer> aoiLayers = aoi.getLayerMapping();
        for (Layer layer : aoiLayers) {
            countAttrs += layer.getEndAttributeIndex() - layer.getStartAttributeIndex() + 1;
        }
        String allAttr = "";
        for (int i = 1; i <= countAttrs; i++) {
            allAttr += "a" + i + ":uint8 null";
            if (i != countAttrs) {
                allAttr += ",";
            }
        }
        String query = "create array " + newSciDBArrayName + " <" + allAttr
                + ">[t=0:*:0:10; x=0:" + (width - 1) + ":0:5000; y=0:" + (height - 1) + ":0:5000]";
        sciService.execute_query(query, "", sessionID);

        // save old values to newSciDBArray:
        query = "store(cast(apply(" + aoi.getScidbArrayName() + ",";

        int startIndex = newLayers.get(0).getStartAttributeIndex();
        int endIndex = newLayers.get(newLayers.size() - 1).getEndAttributeIndex();
        for (int i = startIndex; i <= endIndex; i++) {
            query += "a" + i + ",null";
            if (i != endIndex) {
                query += ",";
            }
        }
        query += "),<";
        for (int i = 1; i <= endIndex; i++) {
            query += "a" + i + ":uint8 null";
            if (i != endIndex) {
                query += ",";
            }
        }
        query += "> [t=0:*:0:10; x=0:" + (width - 1) + ":0:5000; y=0:" + (height - 1)
                + ":0:5000])," + newSciDBArrayName + ")";
        sciService.execute_query(query, "", sessionID);
        newAoI.setScidbArrayName(newSciDBArrayName);
    }

    private boolean createInsertArray(String fileURL, AreaOfInterest aoi, String sessionID, HashMap<Integer, Integer> indexMap) {
        PathToFile fileName = new PathToFile();
        long time_java_start = System.currentTimeMillis();
        fileName.setPath(fileURL.substring(fileURL.lastIndexOf('/') + 1));
        String rasterFilePath = "opt/scidb/18.1/DB-scidb/0/0/" + fileName.getPath();
        File rasterFile = new File(rasterFilePath);
        ParameterValue<OverviewPolicy> policy = AbstractGridFormat.OVERVIEW_POLICY.createValue();
        policy.setValue(OverviewPolicy.IGNORE);
        // this will basically read 4 tiles worth of data at once from the disk...
        ParameterValue<String> gridsize = AbstractGridFormat.SUGGESTED_TILE_SIZE.createValue();
        // Setting read type: use JAI ImageRead (true) or ImageReaders read methods (false)
        ParameterValue<Boolean> useJaiRead = AbstractGridFormat.USE_JAI_IMAGEREAD.createValue();
        useJaiRead.setValue(false);
        try {
            GeoTiffReader reader = new GeoTiffReader(rasterFile);
            int numBands = reader.getGridCoverageCount();

            GridCoverage2D coverage = reader.read(
                    new GeneralParameterValue[]{policy, gridsize, useJaiRead}
            );
            int percent = 0;
            int old = 0;

            // create binary file:
            String uuid = UUID.randomUUID().toString();
            // String uuid = "test123";
            Path fileRscPath = Paths.get("opt", "scidb", "18.1", "DB-scidb", "0", "0", uuid + ".out");
            URI fileUri = fileRscPath.toUri();
            FileOutputStream fileWriter = new FileOutputStream(new File(fileUri), true);

            int[] vals = new int[Math.max(numBands * 4, newAoI.getTotalAttributes())];
            // fill binary file with bytes:

            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    coverage.evaluate(new GridCoordinates2D(i, j), vals);
                    // get Bandvalue:
                    for (int attr = 0; attr < indexMap.size(); attr++) {
                        try {
                            int geotiffAttr = indexMap.get((Integer) (attr + 1));
                            if (geotiffAttr == -1) {
                                fileWriter.write((byte) 00);
                                fileWriter.write((byte) 00);
                            } else {
                                fileWriter.write((byte) 255);
                                fileWriter.write((byte) vals[geotiffAttr - 1]);
                            }
                        } catch (NullPointerException e) {
                            fileWriter.write((byte) 00);
                            fileWriter.write((byte) 00);
                        }
                    }
                }
                percent = i * 100 / width;
                if (percent > old) {
                    old = percent;
                    LOG.info("Iterating input pixels: " + percent + "%");
                }
            }
            fileWriter.close();
            LOG.info("Iterating input pixels:100%");
            long time_java_end = System.currentTimeMillis();
            LOG.info("JAVA: reading input pixels            :       " + (time_java_end - time_java_start) / 1000 + "s.");

            // create toInsert array:
            ArrayList<Layer> allLayers = new ArrayList<>();
            for (Layer layer : newAoI.getLayerMapping()) {
                allLayers.add(layer);
            }
            int oldTotalLAttributes = aoi.getTotalAttributes();
            String query = "create array toInsert <";
            int countColors = 0;

            for (int i = 0; i < allLayers.size(); i++) {
                Layer currentLayer = allLayers.get(i);
                int endIndex = currentLayer.getEndAttributeIndex() - currentLayer.getStartAttributeIndex() + 1;
                for (int j = 0; j < endIndex; j++) {
                    query += "a";
                    query += countColors + (j + 1);
                    query += ":uint8 null";
                    if ((i + 1 < allLayers.size()) || (j + 1 != endIndex)) {
                        query += ",";
                    }
                }
                countColors = countColors + endIndex;
            }
            query += ">[i=0:" + (width - 1) + ":0:5000; j=0:" + (height - 1) + ":0:5000]";

            long time_start = System.currentTimeMillis();
            sciService.execute_query(query, "", sessionID);
            query = "load(toInsert,'" + uuid + ".out', -2, '(";
            for (int i = 0; i < allLayers.size(); i++) {
                Layer currentLayer = allLayers.get(i);
                int currentIndex = currentLayer.getEndAttributeIndex() - currentLayer.getStartAttributeIndex() + 1;
                for (int j = 0; j < currentIndex; j++) {
                    query += "uint8 null";
                    if ((i + 1 < allLayers.size()) || (j + 1 != currentIndex)) {
                        query += ",";
                    }
                }
            }
            query += ")')";
            sciService.execute_query(query, "", sessionID);
            long time_end = System.currentTimeMillis();
            long time_diff = (time_end - time_start) / 1000;
            LOG.info("SCIDB: load from file                 :       " + time_diff + "s.");
            return true;
        } catch (Exception e) {
            LOG.error("" + e);
            LOG.error("{\"error\": \" " + e.getMessage() + ".\"}");
            return false;
        }
    }

    private void addInsertArrayOnTopOf(AreaOfInterest aoi, String sessionID, Date date) {
        int timeStamps = aoi.getTimeStamps().size();
        String query = "insert(redimension(apply(toInsert, t, " + (timeStamps + 1) + ", x, i, y, j), " + aoi.getScidbArrayName() + "), " + aoi.getScidbArrayName() + ")";
        long time_start = System.currentTimeMillis();
        sciService.execute_query(query, "", sessionID);
        aoi.addTimeStamp(date);
        // remove toInsert array
        query = "remove(toInsert)";
        sciService.execute_query(query, "", sessionID);
        long time_end = System.currentTimeMillis();
        long time_diff = (time_end - time_start) / 1000;
        LOG.info("SCIDB: insert(redimension(...)) :       " + time_diff + " s.");
    }

    private void determineGeoTiffMetadata(String fileURL) throws ReadingGeoTiffMetadataException {
        PathToFile fileName = new PathToFile();
        fileName.setPath(fileURL.substring(fileURL.lastIndexOf('/') + 1));
        String filePath = "opt/scidb/18.1/DB-scidb/0/0/" + fileName.getPath();
        File file = new File(filePath);
        try {
            ParameterValue<OverviewPolicy> policy = AbstractGridFormat.OVERVIEW_POLICY.createValue();
            policy.setValue(OverviewPolicy.IGNORE);

            //this will basically read 4 tiles worth of data at once from the disk...
            ParameterValue<String> gridsize = AbstractGridFormat.SUGGESTED_TILE_SIZE.createValue();

            //Setting read type: use JAI ImageRead (true) or ImageReaders read methods (false)
            ParameterValue<Boolean> useJaiRead = AbstractGridFormat.USE_JAI_IMAGEREAD.createValue();
            useJaiRead.setValue(true);

            GeoTiffReader reader = new GeoTiffReader(file);
            GridEnvelope dimensions = reader.getOriginalGridRange();
            crs = reader.getCoordinateReferenceSystem().getName().getCode();
            Set<ReferenceIdentifier> col = reader.getCoordinateReferenceSystem().getIdentifiers();
            col.stream().forEach(refIden -> {
                if (refIden instanceof NamedIdentifier) {
                    NamedIdentifier ni = (NamedIdentifier) refIden;
                    epsg = ni.getCode();
                }
            });

            GridCoordinates maxDimensions = dimensions.getHigh();
            width = maxDimensions.getCoordinateValue(0) + 1;
            height = maxDimensions.getCoordinateValue(1) + 1;
//            width = 300;
//            height = 300;

            GridCoverage2D coverage = reader.read(
                    new GeneralParameterValue[]{policy, gridsize, useJaiRead}
            );

            // get raster file bounding box:
            DirectPosition lowerCorner = coverage.getEnvelope().getLowerCorner();
            minX = lowerCorner.getCoordinate()[0];
            minY = lowerCorner.getCoordinate()[1];
            DirectPosition upperCorner = coverage.getEnvelope().getUpperCorner();
            maxX = upperCorner.getCoordinate()[0];
            maxY = upperCorner.getCoordinate()[1];
            LOG.info("BBOX:" + minX + "," + minY + "," + maxX + "," + maxY);

        } catch (IOException | IndexOutOfBoundsException | InvalidParameterValueException e) {
            LOG.error(e + ": " + e.getMessage());
            throw new ReadingGeoTiffMetadataException(e.getMessage());
        }
    }

    private boolean hasNewLayers(AreaOfInterest aoi, String[] layers) {
        for (String layerName : layers) {
            Layer currLayer = aoi.getLayerByName(layerName);
            if (currLayer == null) {
                return true;
            }
        }
        return false;
    }

    private boolean saveFileToDisk(String fileURL) {
        if (fileURL == null || fileURL.isEmpty()) {
            LOG.error("{\"error\": \"Missing required request parameter 'fileURL'.\"}");
            return false;
        }

        // get the file:
        URL url;
        try {
            url = new URL(fileURL);
        } catch (MalformedURLException ex) {
            LOG.error("{\"error\": \"The specified fileURL is malformed.\"}");
            return false;
        }
        HttpURLConnection httpConn;
        try {
            httpConn = (HttpURLConnection) url.openConnection();
        } catch (IOException ex) {
            LOG.error("{\"error\": \"" + ex.getMessage() + "\"}");
            return false;
        }
        int responseCode;
        try {
            responseCode = httpConn.getResponseCode();
        } catch (IOException ex) {
            LOG.error("{\"error\": \"" + ex.getMessage() + "\"}");
            return false;
        }

        // always check HTTP response code first
        if (responseCode == HttpURLConnection.HTTP_OK) {
            String fileName = "";
            String disposition = httpConn.getHeaderField("Content-Disposition");
            String contentType = httpConn.getContentType();
            int contentLength = httpConn.getContentLength();

            if (disposition != null) {
                // extracts file name from header field
                int index = disposition.indexOf("filename=");
                if (index > 0) {
                    fileName = disposition.substring(index + 10,
                            disposition.length() - 1);
                }
            } else {
                // extracts file name from URL
                fileName = fileURL.substring(fileURL.lastIndexOf("/") + 1,
                        fileURL.length());
            }

            LOG.info("Content-Type = " + contentType);
            LOG.info("Content-Disposition = " + disposition);
            LOG.info("Content-Length = " + contentLength);
            LOG.info("fileName = " + fileName);

            // opens input stream from the HTTP connection
            InputStream inputStream;
            try {
                inputStream = httpConn.getInputStream();
            } catch (IOException ex) {
                LOG.error("{\"error\": \"" + ex.getMessage() + "\"}");
                return false;
            }
            ClassLoader classLoader = getClass().getClassLoader();
            File resourcesDirectory = new File("opt/scidb/18.1/DB-scidb/0/0/");
            String saveFilePath = resourcesDirectory.getAbsolutePath() + File.separator + fileName;

            // opens an output stream to save into file
            FileOutputStream outputStream;
            try {
                outputStream = new FileOutputStream(saveFilePath);
            } catch (FileNotFoundException ex) {
                LOG.error("{\"error\": \"" + ex.getMessage() + "\"}");
                return false;
            }

            int bytesRead = -1;
            byte[] buffer = new byte[BUFFER_SIZE];
            try {
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            } catch (IOException ex) {
                LOG.error("{\"error\": \"" + ex.getMessage() + "\"}");
                return false;
            }

            try {
                outputStream.close();
                inputStream.close();
            } catch (IOException ex) {
                LOG.error("{\"error\": \"" + ex.getMessage() + "\"}");
                return false;
            }
        } else {
            LOG.error("{\"error\": \"Server connection error. HTTP code: " + responseCode + "\"");
            return false;
        }
        httpConn.disconnect();
        return true;
    }

}
