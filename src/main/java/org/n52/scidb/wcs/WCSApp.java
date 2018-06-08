/*
 * TODO: Add License Header
 */
 /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.n52.scidb.wcs;

import org.n52.scidb.wcs.model.RasterPixelArrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

/**
 *
 * @author Maurin Radtke <m.radtke@52north.org>
 */
@SpringBootApplication
@ComponentScan({"org.n52.scidb.wcs"})
@ComponentScan({"org.n52.scidb.wcs.model"})
@EnableConfigurationProperties(RasterPixelArrays.class)
public class WCSApp {

    private static final Logger LOG = LoggerFactory.getLogger(WCSApp.class);

    public static void main(String[] args) {
        new SpringApplicationBuilder(WCSApp.class)
                .properties("server.port,server.servlet.contextPath")
                .run(args);
    }

}
