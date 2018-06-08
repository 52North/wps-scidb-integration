/*
 * TODO: Add License Header
 */
package org.n52.scidb.wcs.controller;

import java.awt.image.BufferedImage;
import org.n52.scidb.wcs.model.RasterPixelArray;
import org.n52.scidb.wcs.services.SciDBService;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.logging.Level;
import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import org.opengis.referencing.operation.TransformException;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.OverviewPolicy;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.n52.scidb.wcs.model.PathToFile;
import org.n52.scidb.wcs.model.RasterPixelArrays;
import org.opengis.coverage.grid.GridCoordinates;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.InvalidParameterValueException;
import org.opengis.parameter.ParameterValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Maurin Radtke <m.radtke@52north.org>
 */
@Component
@EnableConfigurationProperties(RasterPixelArrays.class)
@RestController
@RequestMapping("/api")
public class RestApiController {

    public static final Logger LOG = LoggerFactory.getLogger(RestApiController.class);

    @Autowired
    SciDBService sciService;
    
    HttpHeaders headers = new HttpHeaders();

    RasterPixelArrays arrays;

    @PostConstruct
    private void init() {
        LOG.info("loading arrays from filesystem...");
        // load RasterPixelArrays from file:
        Path fileStoragePath = Paths.get("opt", "scidb", "16.9", "DB-scidb", "0", "0", "array" + "storage.txt");
        URI storageUri = fileStoragePath.toUri();
        FileInputStream fis;
        try {
            fis = new FileInputStream(new File(storageUri));
            ObjectInputStream ois = new ObjectInputStream(fis);
            arrays = (RasterPixelArrays) ois.readObject();
            ois.close();
            LOG.info("...loaded " + arrays.getRasterPixelArrays().size() + " arrays from filesystem.");
        } catch (FileNotFoundException ex) {
            LOG.info("loading stored arrays aborted: filesystem not found.");
            arrays = new RasterPixelArrays();
        } catch (IOException ex) {
            LOG.error("loading stored arrays failed: " + ex.getMessage());
            arrays = new RasterPixelArrays();
        } catch (ClassNotFoundException ex) {
            LOG.error("loading stored arrays failed: " + ex.getMessage());
            arrays = new RasterPixelArrays();
        }
    }

    @RequestMapping(value = "/arrays", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<RasterPixelArrays> getArrays() {
        return new ResponseEntity(arrays, HttpStatus.OK);
    }

    @RequestMapping(value = "/arrays/{arrayName}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<RasterPixelArray> getArray(@PathVariable String arrayName) {
        RasterPixelArray result = null;
        for (RasterPixelArray rpa : arrays.getRasterPixelArrays()) {
            if (rpa.getArrayName().equals(arrayName)) {
                return new ResponseEntity(rpa, HttpStatus.OK);
            }
        }
        return new ResponseEntity(result, HttpStatus.NOT_FOUND);
    }

    @RequestMapping(value = "/arrays/{arrayName}/saveAsBinary", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<String> saveAsBinary(
            @PathVariable String arrayName) {
        String sessionID = sciService.new_session();
        if (sessionID == null || sessionID.isEmpty()) {
            sciService.release_session(sessionID);
            return new ResponseEntity("{\"error\": \"The service could not create a new sciDB session.\"}", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        String query = "save(" + arrayName + ", '/opt/scidb/16.9/DB-scidb/0/0/binary.out', -2, '(uint16)')";
        String result = sciService.execute_query(query, "", sessionID);
        sciService.release_session(sessionID);
        String response = "{\"success\": \"" + result + "\"}";
        return new ResponseEntity(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/arrays/loadFromBinary", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<String> loadFromBinary(
            @RequestParam String rows,
            @RequestParam String cols) {
        String sessionID = sciService.new_session();
        if (sessionID == null || sessionID.isEmpty()) {
            sciService.release_session(sessionID);
            return new ResponseEntity("{\"error\": \"The service could not create a new sciDB session.\"}", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        // create array:
        String query = "create array array" + sessionID + ", <b1:uint16>[i=0:" + rows + ":0:1; j=0:" + cols + ":0:1]";
        sciService.execute_query(query, "", sessionID);

        // load file into array:
        query = "load(array" + sessionID + ", '/opt/scidb/16.9/DB-scidb/0/0/binary.out', -2, (uint16))";
        long time_start = System.currentTimeMillis();
        String response = sciService.execute_query(query, "", sessionID);
        long time_end = System.currentTimeMillis();
        long time_diff = (time_end - time_start) / 1000;
        LOG.info("loaded binary file into 2-dim array in " + time_diff + " sec.");
        sciService.release_session(sessionID);
        return new ResponseEntity("{\"success\": \"" + response + "\"}", HttpStatus.OK);
    }

    @RequestMapping(value = "/arrays", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public ResponseEntity<RasterPixelArray> createArrayFromFile(
            @RequestBody PathToFile path) {

        String sessionID = sciService.new_session();
        if (sessionID == null || sessionID.isEmpty()) {
            sciService.release_session(sessionID);
            return new ResponseEntity("{\"error\": \"The service could not create a new sciDB session.\"}", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        RasterPixelArray result = null;
        if (path == null || path.getPath() == null || path.getPath().isEmpty()) {
            sciService.release_session(sessionID);
            return new ResponseEntity("{\"error\": \"Missing required request body 'path'.\"}", HttpStatus.BAD_REQUEST);
        }

        String filePath = path.getPath();
        File file = new File(filePath);
        try {
            StringBuilder sb = new StringBuilder();

            ParameterValue<OverviewPolicy> policy = AbstractGridFormat.OVERVIEW_POLICY.createValue();
            policy.setValue(OverviewPolicy.IGNORE);

            //this will basically read 4 tiles worth of data at once from the disk...
            ParameterValue<String> gridsize = AbstractGridFormat.SUGGESTED_TILE_SIZE.createValue();

            //Setting read type: use JAI ImageRead (true) or ImageReaders read methods (false)
            ParameterValue<Boolean> useJaiRead = AbstractGridFormat.USE_JAI_IMAGEREAD.createValue();
            useJaiRead.setValue(true);

            GeoTiffReader reader = new GeoTiffReader(file);
            GridEnvelope dimensions = reader.getOriginalGridRange();
            GridCoordinates maxDimensions = dimensions.getHigh();
            int w = maxDimensions.getCoordinateValue(0) + 1;
            int h = maxDimensions.getCoordinateValue(1) + 1;
//            w = 100;
//            h = 100;
            int numBands = reader.getGridCoverageCount();

            GridCoverage2D coverage = reader.read(
                    new GeneralParameterValue[]{policy, gridsize, useJaiRead}
            );
            GridGeometry2D geometry = coverage.getGridGeometry();

            // get raster file bounding box:
            DirectPosition lowerCorner = coverage.getEnvelope().getLowerCorner();
            Double minX = lowerCorner.getCoordinate()[0];
            Double minY = lowerCorner.getCoordinate()[1];
            DirectPosition upperCorner = coverage.getEnvelope().getUpperCorner();
            Double maxX = upperCorner.getCoordinate()[0];
            Double maxY = upperCorner.getCoordinate()[1];
            LOG.info("BBOX:" + minX + "," + minY + "," + maxX + "," + maxY);

            int previous = 0;
            int percent = 0;
            for (int i = 0; i < w; i++) {
                for (int j = 0; j < h; j++) {

                    org.geotools.geometry.Envelope2D pixelEnvelop
                            = geometry.gridToWorld(new GridEnvelope2D(i, j, 1, 1));
                    double lat = pixelEnvelop.getCenterY();
                    double lon = pixelEnvelop.getCenterX();

                    int[] vals = new int[numBands];
                    coverage.evaluate(new GridCoordinates2D(i, j), vals);

                    // get Bandvalue:
                    for (int band = 0; band < vals.length; band++) {
                        // get pixel value at [i,j] for band [band]
                        sb.append(vals[band]);
                        if (band + 1 != vals.length) {
                            sb.append(",");
                        }
                    }
                    if (j + 1 < h) {
                        sb.append('\n');
                    }
                }
                if (i + 1 < w) {
                    sb.append('\n');
                }
                percent = (i * 100 / w);
                if (previous < percent) {
                    System.out.println((percent + 1) + "%");
                    previous = percent;
                }
            }
            sb.append('\n');
            // create local *.csv fileArray:
            Path fileRscPath = Paths.get("opt", "scidb", "16.9", "DB-scidb", "0", "0", "array" + sessionID + ".csv");
            URI fileUri = fileRscPath.toUri();
            PrintWriter pw = new PrintWriter(
                    new File(fileUri)
            );
            pw.write(sb.toString());
            pw.close();

            // create 1-dimensional source array:
            String query = "create array array" + sessionID + " <b1:uint16>[i=0:" + ((w * h) - 1) + "]";
            sciService.execute_query(query, "", sessionID);

            // load data from src file into 1-dimensional array:
            query = "load(array" + sessionID + ", 'array" + sessionID + ".csv', -2, 'csv')";
            sciService.execute_query(query, "", sessionID);

            // create 2-dimensional target array:
            query = "create array array" + sessionID + "2 ";
            for (int i = 0; i < numBands; i++) {
                query += "<b";
                query += (i + 1);
                query += ":uint16>";
            }
            query += "[i=0:" + (w - 1) + ":0:1; j=0:" + (h - 1) + ":0:1]";
            sciService.execute_query(query, "", sessionID);

            // store reshaped 2-dimensional array:
            query = "store(reshape(array" + sessionID + ",<";
            for (int i = 0; i < numBands; i++) {
                query += "b" + (i + 1) + ":uint16";
                if (i + 1 < numBands) {
                    query += ",";
                }
            }
            query += ">[i=0:" + (w - 1) + ":0:1; j=0:" + (h - 1) + ":0:1]),array" + sessionID + "2)";

            long time_start = System.currentTimeMillis();
            sciService.execute_query(query, "", sessionID);
            long time_end = System.currentTimeMillis();
            long time_diff = (time_end - time_start) / 1000;
            LOG.info("stored 2-dim array within " + time_diff + " sec.");

            result = new RasterPixelArray(
                    1,
                    minX,
                    minY,
                    maxX,
                    maxY,
                    numBands,
                    h,
                    w,
                    "array" + sessionID + "2"
            );
            arrays.addRasterPixelArray(result);

            // scan array for validation:
            query = "show(array" + sessionID + "2)";
            sciService.execute_query(query, "dcsv", sessionID);
            String response = sciService.read_lines(sessionID);
            LOG.info(response);

            // persist RasterPixelArrays to file:
            Path fileStoragePath = Paths.get("opt", "scidb", "16.9", "DB-scidb", "0", "0", "array" + "storage.txt");
            URI storageUri = fileStoragePath.toUri();
            FileOutputStream fus = new FileOutputStream(new File(storageUri));
            ObjectOutputStream ous = new ObjectOutputStream(fus);
            ous.writeObject(arrays);
            ous.close();
        } catch (IOException | IndexOutOfBoundsException | InvalidParameterValueException | TransformException e) {
            sciService.release_session(sessionID);
            return new ResponseEntity("{\"error\": \"" + e + "\"}", HttpStatus.BAD_REQUEST);
        }

        sciService.release_session(sessionID);
        return new ResponseEntity(result, HttpStatus.CREATED);
    }

    @RequestMapping(value = "/new_session", method = RequestMethod.GET)
    public ResponseEntity<String> postNewSession() {
        String result = sciService.new_session();
        return new ResponseEntity(result, HttpStatus.OK);
    }

    @RequestMapping(value = "/execute_query", method = RequestMethod.GET)
    public ResponseEntity<String> executeQuery(
            @RequestParam("query") Optional<String> query,
            @RequestParam("save") Optional<String> save,
            @RequestParam("id") Optional<String> sessionID) {
        String pQuery = null;
        String pSave = null;
        String pId = null;
        if (sessionID.isPresent()) {
            pId = sessionID.get();
        }
        if (query.isPresent()) {
            pQuery = query.get();
        }
        if (save.isPresent()) {
            pSave = save.get();
        }
        String result = sciService.execute_query(pQuery, pSave, pId);
        return new ResponseEntity<String>(result, HttpStatus.OK);
    }

    @RequestMapping(value = "/read_lines", method = RequestMethod.GET)
    public ResponseEntity<String> readLines(
            @RequestParam("id") Optional<String> sessionID) {
        String result;
        if (sessionID.isPresent()) {
            result = sciService.read_lines(sessionID.get());
        } else {
            result = sciService.read_lines(null);
        }
        return new ResponseEntity<String>(result, HttpStatus.OK);
    }

    @RequestMapping(value = "/release_session", method = RequestMethod.GET)
    public ResponseEntity<String> release_session(
            @RequestParam("id") Optional<String> sessionID) {
        if (sessionID.isPresent()) {
            sciService.release_session(sessionID.get());
        } else {
            sciService.release_session(null);
        }
        return new ResponseEntity<String>("", HttpStatus.OK);
    }
    
    @RequestMapping(value = "getMap", method = RequestMethod.GET, produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> getMap(
            @RequestParam("LAYERS") String layerName,
            @RequestParam("BBOX") String bbox) {
        RasterPixelArray array = arrays.getArrayByName(layerName);
        if (array == null) {
            headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
            LOG.error("Layer '" + layerName + "' not found.");
            return new ResponseEntity("{\"error\": \"Layer '" + layerName + "' not found.\"}", headers, HttpStatus.NOT_FOUND);
        }

        double x_min = array.getMinX();
        double x_max = array.getMaxX();
        double y_min = array.getMinY();
        double y_max = array.getMaxY();
        // get BBOX param:
        String[] bboxParams = bbox.split(",");
        double bboxMinX = Double.parseDouble(bboxParams[0]);
        double bboxMinY = -Double.parseDouble(bboxParams[3]);
        double bboxMaxX = Double.parseDouble(bboxParams[2]);
        double bboxMaxY = -Double.parseDouble(bboxParams[1]);
        headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
        if (bboxMinX < x_min) {
            return new ResponseEntity("{\"error\": \"BoundingBox parameter 1st argument '" + bboxMinX + "' exceeds layer '" + layerName + "'s coverage of '" + x_min + "'.\"}", headers, HttpStatus.NOT_FOUND);
        }
        if (-bboxMinY > y_max) {
            return new ResponseEntity("{\"error\": \"BoundingBox parameter 4th argument '" + -bboxMinY + "' exceeds layer '" + layerName + "'s coverage of '" + y_max + "'.\"}", headers, HttpStatus.NOT_FOUND);
        }
        if (bboxMaxX > x_max) {
            return new ResponseEntity("{\"error\": \"BoundingBox parameter 3rd argument '" + bboxMaxX + "' exceeds layer '" + layerName + "'s coverage of '" + x_max + "'.\"}", headers, HttpStatus.NOT_FOUND);
        }
        if (-bboxMaxY < y_min) {
            return new ResponseEntity("{\"error\": \"BoundingBox parameter 2nd argument '" + -bboxMaxY + "' exceeds layer '" + layerName + "'s coverage of '" + y_min + "'.\"}", headers, HttpStatus.NOT_FOUND);
        }
        if (bboxMinX > bboxMaxX) {
            return new ResponseEntity("{\"error\": \"Invalid Bounding Box Arguments. Lower Corner '" + bboxMinX + "' is greather than Upper Corner '" + bboxMaxX + "'.\"}", headers, HttpStatus.BAD_REQUEST);
        }
        if (bboxMinY > bboxMaxY) {
            return new ResponseEntity("{\"error\": \"Invalid Bounding Box Arguments. Lower Corner '" + -bboxMinY + "' is greather than Upper Corner '" + -bboxMaxY + "'.\"}", headers, HttpStatus.BAD_REQUEST);
        }
        int height = array.getHeight();
        int width = array.getWidht();

        // calculate between pixels from bbox:
        int xStart = getPixelCoordinate(bboxMinX,
                x_min, x_max, width);
        int xEnd = getPixelCoordinate(bboxMaxX,
                x_min, x_max, width);
        int yStart = getPixelCoordinate(bboxMinY,
                y_min, y_max, height);
        int yEnd = getPixelCoordinate(bboxMaxY,
                y_min, y_max, height);

        String sessionID = sciService.new_session();
        if (sessionID == null || sessionID.isEmpty()) {
            sciService.release_session(sessionID);
            return null;
        }

        // scan array for validation:
        String query = "between(" + layerName + "," + xStart + "," + yStart + "," + xEnd + "," + yEnd + ")";
        String response = sciService.execute_query(query, "dcsv", sessionID);
        if (response.startsWith("UserQueryException")) {
            return new ResponseEntity("{\"error\": \"Layer '" + layerName + "' not found.\"}", headers, HttpStatus.NOT_FOUND);
        }
        byte[] media = getImageFromPixelArray(
                (xEnd - xStart + 1),
                (yEnd - yStart + 1),
                sciService.readPixels(sessionID, (xEnd - xStart + 1), (yEnd - yStart + 1)));
        return new ResponseEntity(media, HttpStatus.OK);
    }

    private int getPixelCoordinate(double coord, double min, double max, int range) {
        return (int) ((coord - min)
                / (max - min) * range);
    }

    private byte[] getImageFromPixelArray(int width, int height, int[] pixels) {
        try {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    int a = 255; //red
                    int r = pixels[x * height + y]; //red
                    int g = pixels[x * height + y]; //green
                    int b = pixels[x * height + y]; //blue
                    int p = (a << 24) | (r << 16) | (g << 8) | b;
                    image.setRGB(x, y, p);
                }
            }
            Path fileStoragePath = Paths.get("opt", "scidb", "16.9", "DB-scidb", "0", "0", "Output.png");
            File f = new File(fileStoragePath.toUri());
            try {
                ImageIO.write(image, "png", f);
            } catch (Exception ex) {
                LOG.error(ex + " " + ex.getMessage());
            }
            BufferedImage bufferedImage = null;
            try {
                bufferedImage = ImageIO.read(f);
            } catch (IOException ex) {
                java.util.logging.Logger.getLogger(WPSController.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }

            // get DataBufferBytes from Raster
            byte[] fileContent = Files.readAllBytes(f.toPath());
            return fileContent;
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(WPSController.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

}
