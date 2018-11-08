/*
 * TODO: Add License Header
 */
package org.n52.scidb.wcs.controller;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import org.n52.scidb.wcs.model.RasterPixelArray;
import org.n52.scidb.wcs.services.SciDBService;
import org.n52.scidb.wcs.model.RasterPixelArrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
@EnableConfigurationProperties(RasterPixelArrays.class)
@RestController
@RequestMapping("/wms")
public class WPSController {

    private static final int BUFFER_SIZE = 4096;
    public static final Logger LOG = LoggerFactory.getLogger(WPSController.class);

    @Autowired
    SciDBService sciService;

    HttpHeaders headers = new HttpHeaders();

    @RequestMapping(value = "insertGeoTiff", method = RequestMethod.POST)
    public ResponseEntity<?> insertGeoTiff(
            @RequestParam("fileURL") String fileURL
    ) {
        RasterPixelArray result = null;

        String sessionID = sciService.new_session();
        if (sessionID == null || sessionID.isEmpty()) {
            sciService.release_session(sessionID);
            return new ResponseEntity("{\"error\": \"The service could not create a new sciDB session.\"}", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if (fileURL == null || fileURL.isEmpty()) {
            sciService.release_session(sessionID);
            return new ResponseEntity("{\"error\": \"Missing required request parameter 'fileURL'.\"}", HttpStatus.BAD_REQUEST);
        }

        // get the file:
        URL url = null;
        try {
            url = new URL(fileURL);
        } catch (MalformedURLException ex) {
            LOG.error(ex.getMessage());
            return new ResponseEntity("{\"error\": \"The specified fileURL is malformed.\"}", HttpStatus.BAD_REQUEST);
        }
        HttpURLConnection httpConn;
        try {
            httpConn = (HttpURLConnection) url.openConnection();
        } catch (IOException ex) {
            LOG.error(ex.getMessage());
            return new ResponseEntity("{\"error\": \"" + ex.getMessage() + "\"}", HttpStatus.BAD_REQUEST);
        }
        int responseCode;
        try {
            responseCode = httpConn.getResponseCode();
        } catch (IOException ex) {
            LOG.error(ex.getMessage());
            return new ResponseEntity("{\"error\": \"" + ex.getMessage() + "\"}", HttpStatus.BAD_REQUEST);
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

            System.out.println("Content-Type = " + contentType);
            System.out.println("Content-Disposition = " + disposition);
            System.out.println("Content-Length = " + contentLength);
            System.out.println("fileName = " + fileName);

            // opens input stream from the HTTP connection
            InputStream inputStream;
            try {
                inputStream = httpConn.getInputStream();
            } catch (IOException ex) {
                LOG.error(ex.getMessage());
                return new ResponseEntity("{\"error\": \"" + ex.getMessage() + "\"}", HttpStatus.BAD_REQUEST);
            }
            ClassLoader classLoader = getClass().getClassLoader();
            File resourcesDirectory = new File("opt/scidb/16.9/DB-scidb/0/0/");
            String saveFilePath = resourcesDirectory.getAbsolutePath()+File.separator + fileName;

            // opens an output stream to save into file
            FileOutputStream outputStream;
            try {
                outputStream = new FileOutputStream(saveFilePath);
            } catch (FileNotFoundException ex) {
                LOG.error(ex.getMessage());
                return new ResponseEntity("{\"error\": \"" + ex.getMessage() + "\"}", HttpStatus.BAD_REQUEST);
            }

            int bytesRead = -1;
            byte[] buffer = new byte[BUFFER_SIZE];
            try {
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            } catch (IOException ex) {
                LOG.error(ex.getMessage());
                return new ResponseEntity("{\"error\": \"" + ex.getMessage() + "\"}", HttpStatus.BAD_REQUEST);
            }

            try {
                outputStream.close();
                inputStream.close();
            } catch (IOException ex) {
                LOG.error(ex.getMessage());
                return new ResponseEntity("{\"error\": \"" + ex.getMessage() + "\"}", HttpStatus.BAD_REQUEST);
            }

        } else {
            System.out.println("No file to download. Server replied HTTP code: " + responseCode);
        }
        httpConn.disconnect();
        return new ResponseEntity("{\"ok\": \"done\"}", HttpStatus.OK);
    };

}
