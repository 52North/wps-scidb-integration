/*
 * TODO: Add License Header
 */
package org.n52.scidb.wcs.controller;

import java.awt.image.BufferedImage;
import org.n52.scidb.wcs.services.SciDBService;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
// import java.time. <-- alternative Date API
import java.util.Optional;
import java.util.logging.Level;
import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import org.apache.commons.io.IOUtils;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.n52.scidb.wcs.model.AreaOfInterest;
import org.n52.scidb.wcs.model.AreaOfInterests;
import org.n52.scidb.wcs.model.Layer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Maurin Radtke <m.radtke@52north.org>
 */
@Component
@RestController
@RequestMapping("/api")
public class RestApiController {

    private static final int BUFFER_SIZE = 4096;

    public static final Logger LOG = LoggerFactory.getLogger(RestApiController.class);

    @Autowired
    SciDBService sciService;

    HttpHeaders headers = new HttpHeaders();
    HttpHeaders jsonHeaders = new HttpHeaders();

    URI storageUri;

    @Autowired
    AreaOfInterests aois;

    @PostConstruct
    private void init() {
        jsonHeaders.add("Content-Type", "application/json");

        LOG.info("loading arrays from filesystem...");
        // load Layers from file:
        Path fileStoragePath = Paths.get("wms", "arraystorage.asdf");
        storageUri = fileStoragePath.toUri();
//        FileInputStream fis;
//        try {
//            fis = new FileInputStream(new File(storageUri));
//            ObjectInputStream ois = new ObjectInputStream(fis);
//            arrays = (Layers) ois.readObject();
//            ois.close();
//            for (Layer rpa : arrays.getRasterPixelArrays()) {
//                int timeStampIndex = rpa.getTimeStampIndex();
//                // query shim for availability of array:
//                String sessionID = sciService.new_session();
//                if (sessionID == null || sessionID.isEmpty()) {
//                    sciService.release_session(sessionID);
//                    LOG.error("The service could not create a new sciDB session.");
//                }
//                String query = "show(" + SCIDB_ARRAY_NAME + ")";
//                String shimResponse = sciService.execute_query(query, "dcsv", sessionID);
//                if (shimResponse.contains("SCIDB_LE_ARRAY_DOESNT_EXIST")) {
//                    LOG.info("Could not find SciDB Core array instance for previously inserted GeoTiff.");
//                    LOG.info("Creating a new SciDB Core array instance");
//                    arrays = new Layers();
//                    LOG.info("Removed all SciDB arrays.");
//                }
//                // TODO: check SCIDB_ARRAY_NAME for existence of array with timeStampIndex.
//                sciService.release_session(sessionID);
//                LOG.info(shimResponse);
//            }
//            LOG.info("...loaded " + arrays.getRasterPixelArrays().size() + " files from filesystem.");
//            // persist Layers to file:
//            FileOutputStream fus = new FileOutputStream(new File(storageUri));
//            ObjectOutputStream ous = new ObjectOutputStream(fus);
//            ous.writeObject(arrays);
//            ous.close();
//        } catch (FileNotFoundException ex) {
//            LOG.info("loading stored arrays aborted: filesystem not found.");
//            arrays = new Layers();
//        } catch (IOException ex) {
//            LOG.error("loading stored arrays failed: " + ex.getMessage());
//            arrays = new Layers();
//        } catch (ClassNotFoundException ex) {
//            LOG.error("loading stored arrays failed: " + ex.getMessage());
//            arrays = new Layers();
//        }
//        aois = new AreaOfInterests();
        if (aois == null) {
            aois = new AreaOfInterests();
        }
    }

    @RequestMapping(value = "/aois", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<AreaOfInterests> getAOIs() {
        return new ResponseEntity<>(aois, jsonHeaders, HttpStatus.OK);
    }

    @RequestMapping(value = "/aois/{aoi}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<?> getAOI(
            @PathVariable String aoi) {
        AreaOfInterest result = aois.getAreaOfInterestByName(aoi);
        if (result == null) {
            return new ResponseEntity<>("{\"error\": \"AreaOfInterest '" + aoi + "' not found.\"}", HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(result, jsonHeaders, HttpStatus.OK);
    }

    @RequestMapping(value = "/aois", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<?> postAOI(
            @RequestParam String name) {
        AreaOfInterest result = aois.getAreaOfInterestByName(name);
        if (result != null) {
            return new ResponseEntity<>("{\"error\": \"AreaOfInterest '" + name + "' already exists.\"}", HttpStatus.CONFLICT);
        }
        AreaOfInterest aoi = new AreaOfInterest();
        aoi.setName(name);
        aois.addAreaOfInterest(aoi);
        // persist AOI:
        try {
            FileOutputStream fus = new FileOutputStream(new File(storageUri));
            ObjectOutputStream ous = new ObjectOutputStream(fus);
            ous.writeObject(aois);
            ous.close();
        } catch (IOException ex) {
            LOG.error(ex + ": " + ex.getMessage());
            return new ResponseEntity("{\"error\": \"" + ex + "\"}", jsonHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(aoi, jsonHeaders, HttpStatus.OK);
    }

//    @RequestMapping(value = "/aois/{aoi}/layers", method = RequestMethod.GET, produces = "application/json")
//    public ResponseEntity<?> getAOILayers(
//            @PathVariable String aoi) {
//        AreaOfInterest area = aois.getAreaOfInterestByName(aoi);
//        if (area == null) {
//            return new ResponseEntity<>("{\"error\": \"AreaOfInterest '" + aoi + "' not found.\"}", HttpStatus.NOT_FOUND);
//        }
//        ArrayList<Layer> result = area.getLayers();
//        return new ResponseEntity<>(result, jsonHeaders, HttpStatus.OK);
//    }
    @RequestMapping(value = "/aois/{aoi}/layers/{layerName}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<?> getArray(
            @PathVariable String aoi,
            @PathVariable String layerName) {
        AreaOfInterest area = aois.getAreaOfInterestByName(aoi);
        if (area == null) {
            return new ResponseEntity<>("{\"error\": \"AreaOfInterest '" + aoi + "' not found.\"}", jsonHeaders, HttpStatus.NOT_FOUND);
        }
        Layer result = area.getLayerByName(layerName);
        if (result == null) {
            return new ResponseEntity<>("{\"error\": \"Layer '" + layerName + "' not found.\"}", jsonHeaders, HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(result, jsonHeaders, HttpStatus.OK);
    }

    @RequestMapping(value = "getWMSCapabilities", method = RequestMethod.GET, produces = "application/xml")
    public @ResponseBody
    byte[] getWMSCapabilities() {
        try {
            Namespace xlinkNamespace = Namespace.getNamespace("xlink", "https://www.w3.org/1999/xlink");

            Element root = new Element("WMSCapabilities");

            Document doc = new Document();

            Element service = new Element("Service");
            service.addContent(new Element("Name").addContent("WMS"));
            service.addContent(new Element("Title").addContent("SciDB-WMS"));
            service.addContent(new Element("Abstract").addContent("The Spatial SciDB WMS Service"));
            service.addContent(new Element("KeywordList")
                    .addContent(new Element("Keyword")
                            .addContent("SciDB"))
                    .addContent(new Element("Keyword")
                            .addContent("WMS"))
                    .addContent(new Element("Keyword")
                            .addContent("some more")));

            Element capability = new Element("Capability");

            Element request = new Element("Request");
            request.addContent(new Element("GetCapabilities")
                    .addContent(new Element("Format")
                            .addContent("text/xml"))
                    .addContent(new Element("DCPType")
                            .addContent(new Element("HTTP")
                                    .addContent(new Element("Get")
                                            .addContent(new Element("OnlineResource")
                                                    .setAttribute("href", "http://localhost:8081/api/getWMSCapabilities", xlinkNamespace)
                                            )))));
            request.addContent(new Element("GetMap")
                    .addContent(new Element("Format")
                            .addContent("image/png"))
                    .addContent(new Element("Format")
                            .addContent("image/jpeg"))
                    .addContent(new Element("DCPType")
                            .addContent(new Element("HTTP")
                                    .addContent(new Element("Get")
                                            .addContent(new Element("OnlineResource")
                                                    .setAttribute("href", "http://localhost:8081/api/getWMSCapabilities", xlinkNamespace)
                                            )))));
            capability.addContent(request);

            Element exception = new Element("Exception");
            exception.addContent(new Element("Format")
                    .addContent("XML"));
            capability.addContent(exception);

            for (AreaOfInterest aoi : aois.getAoiArray()) {

                Element layer = new Element("Layer");
                layer.addContent(new Element("Title")
                        .addContent(aoi.getName()));

//                for (Layer aoiLayer : aoi.getLayers()) {
//
//                    if (aoiLayer.getLayerName() != null) {
//
//                        Element innerLayer = new Element("Layer")
//                                .addContent(new Element("Name")
//                                        .addContent(aoi.getName()));
//                        layer.addContent(innerLayer);
//
//                    }
//
//                }
                capability.addContent(layer);
            }

            root.addContent(service);
            root.addContent(capability);

            doc.setRootElement(root);
            Path wmsGetCapabilitiesPath = Paths.get("wms", "wmsGetCapabilities.xml");
            File wmsGetCapabilitiesFile = new File(wmsGetCapabilitiesPath.toUri());

            XMLOutputter outter = new XMLOutputter();
            outter.setFormat(Format.getPrettyFormat());
            outter.output(doc, new FileWriter(wmsGetCapabilitiesFile));

            // System.out.println("File saved!");
            InputStream in = new FileInputStream(wmsGetCapabilitiesFile);
            return IOUtils.toByteArray(in);

        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(RestApiController.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
        return null;
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
            @RequestParam("id") Optional<String> sessionID
    ) {
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
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @RequestMapping(value = "/read_lines", method = RequestMethod.GET)
    public ResponseEntity<String> readLines(
            @RequestParam("id") Optional<String> sessionID
    ) {
        String result;
        if (sessionID.isPresent()) {
            result = sciService.read_lines(sessionID.get());
        } else {
            result = sciService.read_lines(null);
        }
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @RequestMapping(value = "/release_session", method = RequestMethod.GET)
    public ResponseEntity<String> release_session(
            @RequestParam("id") Optional<String> sessionID
    ) {
        if (sessionID.isPresent()) {
            sciService.release_session(sessionID.get());
        } else {
            sciService.release_session(null);
        }
        return new ResponseEntity<>("", HttpStatus.OK);
    }

}