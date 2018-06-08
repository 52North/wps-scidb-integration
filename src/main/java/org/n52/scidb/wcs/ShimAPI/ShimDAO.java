/*
 * TODO: Add License Header
 */
///*
// * TODO: Add License Header
// */
// /*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package org.n52.scidb.wcs.ShimAPI;
//
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.net.HttpURLConnection;
//import java.net.URL;
//import java.util.ArrayList;
//import java.util.List;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
///**
// *
// * @author Maurin Radtke <m.radtke@52north.org>
// */
////@Component
//public class ShimDAO {
//    
//    private static final Logger LOG = LoggerFactory.getLogger(ShimDAO.class);
//    
//    private String performHTTPGet(URL url, boolean keep_newline) {
//        try {
//            HttpURLConnection con = (HttpURLConnection) url.openConnection();
//            //con.setReadTimeout(10000);
//            con.setRequestMethod("GET");
//            LOG.debug("Performing HTTP GET: " + url);
//            int responseCode = con.getResponseCode();
//
//            if (responseCode != HttpURLConnection.HTTP_OK) {
//                LOG.error("HTTP GET returned code " + responseCode);
//            } else {
//                LOG.debug("HTTP GET returned HTTP_OK");
//            }
//            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
//            String line;
//            StringBuffer response = new StringBuffer();
//            while ((line = in.readLine()) != null) {
//                response.append(line);
//                if (keep_newline) {
//                    response.append("\n");
//                }
//            }
//            in.close();
//            con.disconnect();
//            return response.toString();
//        } catch (IOException ex) {
//            LOG.error("Error during HTTP GET request to Shim: " + ex);
//        }
//        return null;
//    }
//
//    public List<String> postNewSession() {
//        try {
//            URL url = new URL("http://localhost:8080/new_session");
//            ArrayList<String> results = new ArrayList<>();
//            results.add(performHTTPGet(url, false));
//            return results;
//        } catch (Exception e) {
//            LOG.error(e.getMessage(), e);
//            return null;
//        }
//    }
//
//}
