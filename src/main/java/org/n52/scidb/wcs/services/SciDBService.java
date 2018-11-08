/*
 * TODO: Add License Header
 */
package org.n52.scidb.wcs.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 *
 * @author Maurin Radtke <m.radtke@52north.org>
 */
@Component
public class SciDBService {

    @Value("${shim.address}")
    private String serveraddress;
    @Value("${shim.port}")
    private String serverport;

    private static final Logger LOG = LoggerFactory.getLogger(SciDBService.class);

    private static final String SHIM_NEWSESSION = "/new_session";
    private static final String SHIM_RELEASESESSION = "/release_session";
    private static final String SHIM_EXECUTEQUERY = "/execute_query";
    private static final String SHIM_READLINES = "/read_lines";
    private static final String SHIM_READBYTES = "/read_bytes";
    private static final String SHIM_UPLOADFILE = "/upload_file";
    private static final String SHIM_UPLOAD = "/upload";
    private static final String SHIM_CANCEL = "/cancel";

    private static final String NOT_FOUND = "NOT_FOUND";

    private static String parsToUrlString(Map<String, String> pars) {
        if (pars.isEmpty()) {
            return "";
        }
        String out = "?";
        try {
            String k0 = (String) pars.keySet().toArray()[0];
            out += k0 + "=" + URLEncoder.encode(pars.get(k0), "UTF-8");
            pars.remove(k0);

            for (Map.Entry<String, String> entry : pars.entrySet()) {
                out += "&" + entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), "UTF-8");
            }
            return out;
        } catch (UnsupportedEncodingException ex) {
            return out;
        }
    }

    private String performHTTPGet(URL url, boolean keep_newline) {
        LOG.info("performing HTTP Request on '" + url + "'.");
        long time_start = System.currentTimeMillis();
        try {
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            int responseCode = con.getResponseCode();

            if (responseCode != HttpURLConnection.HTTP_OK) {
                LOG.error("HTTP GET returned code " + responseCode + " on request: ");
                LOG.error("" + url);
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getErrorStream()));
                String line;
                StringBuilder response = new StringBuilder();
                while ((line = in.readLine()) != null) {
                    response.append(line);
                    if (keep_newline) {
                        response.append("\n");
                    }
                }
                LOG.error(response.toString());
                return response.toString();
            } else {
                LOG.debug("HTTP GET returned HTTP_OK");
            }
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String line;
            StringBuffer response = new StringBuffer();
            while ((line = in.readLine()) != null) {
                response.append(line);
                if (keep_newline) {
                    response.append("\n");
                }
            }
            in.close();
            con.disconnect();
            return response.toString();
        } catch (IOException ex) {
            LOG.error("Error during HTTP Request with URL '" + url + "'. Error: " + ex);
        }
        return null;
    }

    public String new_session() {
        try {
            URL url = new URL("http://" + serveraddress + ":" + serverport + SHIM_NEWSESSION);
            return performHTTPGet(url, false);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }

    public String execute_query(String afl, String outputFormat, String sessionID) {
        try {
            if (sessionID == null) {
                sessionID = new_session();
            }
            HashMap<String, String> parameters = new HashMap<>();
            parameters.put("id", sessionID);
            parameters.put("query", afl);
//          parameters.put("release", "0");
            if (outputFormat != null && outputFormat.length() > 0) {
                parameters.put("save", outputFormat);
            }
//          parameters.put("stream", "1");

            URL url = new URL("http://" + serveraddress + ":" + serverport + SHIM_EXECUTEQUERY + parsToUrlString(parameters));
            return performHTTPGet(url, false);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }

    public String read_lines(String sessionID) {
        if (sessionID == null) {
            return null;
        }
        HashMap<String, String> pars = new HashMap<>();
        pars.put("id", sessionID);
        pars.put("n", "0");

        try {
            URL url = new URL("http://" + serveraddress + ":" + serverport + SHIM_READLINES + parsToUrlString(pars));
            String response = performHTTPGet(url, true);
            return response;
        } catch (MalformedURLException ex) {
            LOG.error("Shim read_lines failed: " + ex);
        }
        return null;
    }

    public String[] readCells(String sessionID, int tStart, int wStart, int hStart, int tEnd, int wEnd, int hEnd, int sciArrayWidth, int sciArrayHeight) {
        String[] result = new String[(tEnd - tStart + 1) * (wEnd - wStart) * (hEnd - hStart)];
        String[] tempResult = new String[(tEnd - tStart + 1) * (wEnd - wStart) * (hEnd - hStart)];
        if (sessionID == null) {
            return null;
        }
        HashMap<String, String> pars = new HashMap<>();
        pars.put("id", sessionID);
        pars.put("n", "0");

        long time_start = System.currentTimeMillis();
        try {
            URL url = new URL("http://" + serveraddress + ":" + serverport + SHIM_READLINES + parsToUrlString(pars));
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            int responseCode = con.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                LOG.error("HTTP GET returned code " + responseCode);
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getErrorStream()));
                String line;
                StringBuilder response = new StringBuilder();
                while ((line = in.readLine()) != null) {
                    response.append(line);
                    response.append("\n");
                }
                LOG.error(response.toString());
                return null;
            } else {
                LOG.info("HTTP GET returned HTTP_OK");
            }
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String line;
            StringBuffer response = new StringBuffer();
            line = in.readLine();
            int counter = 0;
            boolean skippedEntry = true;
            while ((line = in.readLine()) != null) {
                String value = line.substring(line.indexOf(" ") + 1);
                tempResult[counter] = value;
                counter++;
            }
            in.close();
            con.disconnect();
            LOG.info(url.getPath());

            long time_end = System.currentTimeMillis();
            long time_diff = (time_end - time_start) / 1000;
            LOG.info("between returned " + ((tEnd - tStart) * (wEnd - wStart) * (hEnd - hStart)) + " cells within " + time_diff + " sec.");
            int index = 0;
            int indexResult = 0;
            for (int countT = tStart; countT <= tEnd; countT++) {
                for (int countW = wStart; countW < wEnd; countW++) {
                    for (int countH = hStart; countH < hEnd; countH++) {
                        if ((countT >= 0)
                                && (countW >= 0)
                                && (countH >= 0)
                                && (countW <= sciArrayWidth - 1)
                                && (countH <= sciArrayHeight - 1)) {
                            result[indexResult] = tempResult[index];
                            index++;
                        } else {
                            result[indexResult] = NOT_FOUND;
                        }
                        indexResult++;
                    }
                }
            }
            return result;
        } catch (IOException ex) {
            LOG.error("Error during HTTP GET request to Shim: " + ex);
        }
        return result;
    }

    public String[] readCells(String sessionID, int time, int width, int height) {
        String[] result = new String[time * width * height];
        if (sessionID == null) {
            return null;
        }
        HashMap<String, String> pars = new HashMap<>();
        pars.put("id", sessionID);
        pars.put("n", "0");

        long time_start = System.currentTimeMillis();
        try {
            URL url = new URL("http://" + serveraddress + ":" + serverport + SHIM_READLINES + parsToUrlString(pars));

//          String response = performHTTPGet(url, true);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            int responseCode = con.getResponseCode();

            if (responseCode != HttpURLConnection.HTTP_OK) {
                LOG.error("HTTP GET returned code " + responseCode);
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getErrorStream()));
                String line;
                StringBuilder response = new StringBuilder();
                while ((line = in.readLine()) != null) {
                    response.append(line);
                    response.append("\n");
                }
                LOG.error(response.toString());
                return null;
            } else {
                LOG.info("HTTP GET returned HTTP_OK");
            }
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String line;
            StringBuffer response = new StringBuffer();
            line = in.readLine();
            int counter = 0;
            while ((line = in.readLine()) != null) {
                String value = line.substring(line.indexOf(" ") + 1);
                result[counter] = value;
                counter++;
            }
            in.close();
            con.disconnect();
            LOG.info(url.getPath());

            long time_end = System.currentTimeMillis();
            long time_diff = (time_end - time_start) / 1000;
            LOG.info("between returned " + (time * width * height) + " cells within " + time_diff + " sec.");
            return result;
        } catch (IOException ex) {
            LOG.error("Error during HTTP GET request to Shim: " + ex);
        }
        return null;
    }

    public void release_session(String sessionID) {
        if (sessionID == null) {
            return;
        }
        HashMap<String, String> pars = new HashMap<>();
        pars.put("id", sessionID);
        try {
            URL url = new URL("http://" + serveraddress + ":" + serverport + SHIM_RELEASESESSION + parsToUrlString(pars));
            String response = performHTTPGet(url, true);
            return;
        } catch (MalformedURLException ex) {
            LOG.error("Shim read_lines failed: " + ex);
        }
        return;
    }

}
