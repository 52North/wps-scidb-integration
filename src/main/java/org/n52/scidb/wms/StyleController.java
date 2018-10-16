/*
 * TODO: Add License Header
 */
 /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.n52.scidb.wms;

import java.io.ByteArrayInputStream;
import javax.annotation.PostConstruct;

import java.io.InputStream;
import java.util.ArrayList;
import org.geotools.filter.LiteralExpressionImpl;
import org.geotools.sld.SLDConfiguration;
import org.geotools.styling.ChannelSelectionImpl;
import org.geotools.styling.ColorMap;
import org.geotools.styling.ColorMapEntry;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.NamedLayer;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.RasterSymbolizerImpl;
import org.geotools.styling.Rule;
import org.geotools.styling.SelectedChannelType;
import org.geotools.styling.StyleImpl;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.styling.Symbolizer;
import org.geotools.xml.Configuration;
import org.geotools.xml.Parser;

import org.n52.scidb.wcs.model.AreaOfInterests;
import org.n52.scidb.wcs.model.Style;
import org.n52.scidb.wcs.model.Channel;
import org.n52.scidb.wcs.model.StyleColorMapEntry;
import org.n52.scidb.wcs.model.Styles;
import org.opengis.filter.expression.Expression;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Maurin Radtke <m.radtke@52north.org>
 */
@RestController
@RequestMapping("/api")
public class StyleController {

    private static final int BUFFER_SIZE = 4096;

    public static final Logger LOG = LoggerFactory.getLogger(StyleController.class);

    HttpHeaders headers = new HttpHeaders();
    HttpHeaders jsonHeaders = new HttpHeaders();

    @Autowired
    AreaOfInterests aois;

    @Autowired
    Styles styles;

    @PostConstruct
    private void init() {
        jsonHeaders.add("Content-Type", "application/json");
    }

    @RequestMapping(value = "/styles", method = RequestMethod.GET)
    public ResponseEntity<?> getStyles() {
        return new ResponseEntity<>(styles, HttpStatus.OK);
    }

    @RequestMapping(value = "/styles", method = RequestMethod.POST, consumes = {MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<?> postStyle(
            @RequestBody String styleXml
    ) {
        Style insertedStyle = parseSLD(styleXml);
        if (insertedStyle == null) {
            return new ResponseEntity<>("{\"error\": \"Unsupported STYLES request.\"}", HttpStatus.BAD_REQUEST);
        }
        if (styles.addStyle(insertedStyle)) {
            return new ResponseEntity<>(insertedStyle, jsonHeaders, HttpStatus.CREATED);
        } else {
            return new ResponseEntity<>("{\"error\": \"Style with name '" + insertedStyle.getSldName() + "' already exists.\"}", HttpStatus.CONFLICT);
        }
    }

    private Style parseSLD(String xmlSLD) {
        Style resultStyle = null;
        SelectedChannelType redChannelType = null;
        SelectedChannelType greenChannelType = null;
        SelectedChannelType blueChannelType = null;
        String colorMapType = "ramp";
        ArrayList<StyleColorMapEntry> colorMap = new ArrayList<>();
        double opacity = 1;
        Configuration config = new SLDConfiguration();
        Parser parser = new Parser(config);
        InputStream is = new ByteArrayInputStream(xmlSLD.getBytes());
        StyledLayerDescriptor sld;
        try {
            sld = (StyledLayerDescriptor) parser.parse(is);
        } catch (Exception ex) {
            LOG.warn("{\"error\": \"Exception while parsing style: " + ex.getMessage() + ".}");
            return null;
        }
        // get RasterSymbolizer:
        NamedLayer sl = (NamedLayer) sld.getStyledLayers()[0];
        if (sl.getName() == null) {
            LOG.warn("{\"error\": \"IllegalArgument: <NamedLayer> requires a <Name> Element.\"}");
            return null;
        }
        StyleImpl style = (StyleImpl) sl.getStyles()[0];
        FeatureTypeStyle fts = (FeatureTypeStyle) style.getFeatureTypeStyles()[0];  // fts
        Rule[] rules = fts.getRules();
        for (Rule rule : rules) {
            Symbolizer[] symbolizers = rule.getSymbolizers();
            for (Symbolizer symbolizer : symbolizers) {
                if (symbolizer instanceof RasterSymbolizerImpl) {
                    RasterSymbolizer rasterSymbolizer = (RasterSymbolizerImpl) symbolizer;
                    ChannelSelectionImpl cs = (ChannelSelectionImpl) rasterSymbolizer.getChannelSelection();
                    SelectedChannelType[] colorChannels = cs.getRGBChannels();
                    redChannelType = colorChannels[0];
                    greenChannelType = colorChannels[1];
                    blueChannelType = colorChannels[2];
                    Expression opacityExp = rasterSymbolizer.getOpacity();
                    String opacityStr = "1.0";
                    if (opacityExp != null) {
                        opacityStr = (String) opacityExp.evaluate(null);
                        try {
                            opacity = Double.parseDouble(opacityStr);
                        } catch (NumberFormatException e) {
                            LOG.warn("The given <Opacity>'" + opacityStr + "'</Opacity> is not a number.");
                        }
                    }
                    ColorMap rasterColorMap = (ColorMap) rasterSymbolizer.getColorMap();
                    if (rasterColorMap != null) {
                        ColorMapEntry[] colorMapEntries = (ColorMapEntry[]) rasterColorMap.getColorMapEntries();
                        for (int i = 0; i < colorMapEntries.length; i++) {
                            ColorMapEntry colorMapEntry = colorMapEntries[i];
                            if (colorMapEntry.getColor() == null
                                    || colorMapEntry.getQuantity() == null) {
                                LOG.warn("{\"error\": \"IllegalArgument: <ColorMapEntry> requires a 'color' and a 'quantity' attribute.\".}");
                                return null;
                            }
                            double colorOpacity = 1.0;
                            if (colorMapEntry.getOpacity() != null) {
                                Expression opExpr = colorMapEntry.getOpacity();
                                try {
                                    colorOpacity = (Double) opExpr.evaluate(null);
                                } catch (ClassCastException e) {
                                    LOG.warn("The given ColorMapEntry's Opacity '" + opExpr.evaluate(null) + "' is not a valid number.");
                                }
                            }
                            colorMap.add(
                                    new StyleColorMapEntry(
                                            (String) ((LiteralExpressionImpl) colorMapEntry.getColor()).getValue(),
                                            (Double) ((LiteralExpressionImpl) colorMapEntry.getQuantity()).getValue(),
                                            colorOpacity
                                    )
                            );
                        }
                    }
                    switch (rasterColorMap.getType()) {
                        case ColorMap.TYPE_INTERVALS:
                            colorMapType = "intervals";
                            break;
                        case ColorMap.TYPE_VALUES:
                            colorMapType = "values";
                            break;
                        case ColorMap.TYPE_RAMP:
                        default:
                            colorMapType = "ramp";
                            break;
                    }
                }
            }
        }
        int numberSelectedChannels = 0;
        Channel redChannel = null;
        if (redChannelType != null
                && redChannelType.getChannelName() != null
                && !redChannelType.getChannelName().isEmpty()) {
            redChannel = new Channel(redChannelType.getChannelName());
            numberSelectedChannels++;
        }
        Channel greenChannel = null;
        if (greenChannelType != null
                && greenChannelType.getChannelName() != null
                && !greenChannelType.getChannelName().isEmpty()) {
            greenChannel = new Channel(greenChannelType.getChannelName());
            numberSelectedChannels++;
        }
        Channel blueChannel = null;
        if (blueChannelType != null
                && blueChannelType.getChannelName() != null
                && !blueChannelType.getChannelName().isEmpty()) {
            blueChannel = new Channel(blueChannelType.getChannelName());
            numberSelectedChannels++;
        }
        if (numberSelectedChannels == 1) {
            return new Style(sl.getName(), redChannel, greenChannel, blueChannel, colorMap, colorMapType, opacity);
        } else {
            return new Style(sl.getName(), redChannel, greenChannel, blueChannel, null, opacity);
        }
    }

}
