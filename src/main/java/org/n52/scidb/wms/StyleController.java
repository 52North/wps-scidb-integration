/*
 * TODO: Add License Header
 */
 /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.n52.scidb.wms;

import java.awt.Color;
import java.util.ArrayList;
import javax.annotation.PostConstruct;
import net.opengis.ogc.ExpressionType;
import net.opengis.ogc.impl.LiteralTypeImpl;
import net.opengis.se.CategorizeType;
import net.opengis.se.ChannelSelectionType;
import net.opengis.se.ColorMapType;
import net.opengis.se.FeatureTypeStyleType;
import net.opengis.se.InterpolateType;
import net.opengis.se.InterpolationPointType;
import net.opengis.se.ParameterValueType;
import net.opengis.se.RasterSymbolizerType;
import net.opengis.se.RuleType;
import net.opengis.se.SymbolizerType;
import net.opengis.sld.NamedLayerDocument.NamedLayer;
import net.opengis.sld.StyledLayerDescriptorDocument;
import net.opengis.sld.StyledLayerDescriptorDocument.StyledLayerDescriptor;
import net.opengis.sld.UserStyleDocument.UserStyle;
import net.opengis.se.ValueDocument;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;

import org.n52.scidb.wcs.model.AreaOfInterests;
import org.n52.scidb.wcs.model.Categorize;
import org.n52.scidb.wcs.model.Channel;
import org.n52.scidb.wcs.model.ColorMap;
import org.n52.scidb.wcs.model.Interpolate;
import org.n52.scidb.wcs.model.Style;
import org.n52.scidb.wcs.model.StyleColorMapEntry;
import org.n52.scidb.wcs.model.Styles;

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
import org.w3c.dom.Node;

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
        StyledLayerDescriptorDocument sldDoc;
        try {
            XmlObject xmlobj = XmlObject.Factory.parse(xmlSLD);

            if (xmlobj instanceof StyledLayerDescriptorDocument) {
                sldDoc = (StyledLayerDescriptorDocument) xmlobj;
                Object styledLayerDescriptor = sldDoc.getStyledLayerDescriptor();
                if (styledLayerDescriptor instanceof StyledLayerDescriptor) {
                    StyledLayerDescriptor sld = (StyledLayerDescriptor) styledLayerDescriptor;
                    NamedLayer[] namedLayers = sld.getNamedLayerArray();
                    for (NamedLayer namedLayer : namedLayers) {
                        UserStyle[] userStyles = namedLayer.getUserStyleArray();
                        for (UserStyle userStyle : userStyles) {
                            FeatureTypeStyleType[] featureTypeStyles = userStyle.getFeatureTypeStyleArray();
                            for (FeatureTypeStyleType featureTypeStyleType : featureTypeStyles) {
                                RuleType[] rules = featureTypeStyleType.getRuleArray();
                                for (RuleType rule : rules) {
                                    SymbolizerType[] symbolizers = rule.getSymbolizerArray();
                                    for (SymbolizerType symbolizer : symbolizers) {
                                        if (symbolizer instanceof RasterSymbolizerType) {
                                            ColorMap colorMap = null;

                                            RasterSymbolizerType rst = (RasterSymbolizerType) symbolizer;
                                            ParameterValueType opacPVT = rst.getOpacity();
                                            double opacity = 1.0;
                                            if (opacPVT.getExpressionArray(0) instanceof LiteralTypeImpl) {
                                                String opacStr = ((LiteralTypeImpl) opacPVT.getExpressionArray(0)).getStringValue();
                                                try {
                                                    opacity = Double.parseDouble(opacStr);
                                                } catch (NumberFormatException nfe) {
                                                    LOG.error("Unsupported Number Format: <ogc:Literal>'" + opacStr + "'</ogc:Literal> for <se:Opacity/>.");
                                                    return null;
                                                }
                                            }
                                            ChannelSelectionType chlSel = rst.getChannelSelection();
                                            Channel redChannel = null;
                                            if (chlSel.getRedChannel() != null) {
                                                redChannel = new Channel(chlSel.getRedChannel().getSourceChannelName());
                                            }
                                            Channel greenChannel = null;
                                            if (chlSel.getGreenChannel() != null) {
                                                greenChannel = new Channel(chlSel.getGreenChannel().getSourceChannelName());
                                            }
                                            Channel blueChannel = null;
                                            if (chlSel.getBlueChannel() != null) {
                                                blueChannel = new Channel(chlSel.getBlueChannel().getSourceChannelName());
                                            }

                                            ColorMapType cmt = rst.getColorMap();
                                            String fallbackValue = "#000000";
                                            if (cmt.getCategorize() != null) {
                                                CategorizeType ct = cmt.getCategorize();

                                                ParameterValueType lookupValuePVT = ct.getLookupValue();
                                                ExpressionType luv = lookupValuePVT.getExpressionArray(0);
                                                String lookUpValue = null;
                                                if (luv instanceof LiteralTypeImpl) {
                                                    lookUpValue = ((LiteralTypeImpl) luv).getStringValue();
                                                }
                                                Color startColor = null;
                                                ParameterValueType startColorPT = ct.getValueArray(0);
                                                ExpressionType scET = startColorPT.getExpressionArray(0);
                                                if (scET instanceof LiteralTypeImpl) {
                                                    startColor = Color.decode(
                                                            ((LiteralTypeImpl) scET).getStringValue()
                                                    );
                                                }
                                                ArrayList<StyleColorMapEntry> colorMapEntries = new ArrayList<>();

                                                ParameterValueType[] pvtypes = ct.getValueArray();
                                                ParameterValueType[] thresholds = ct.getThresholdArray();
                                                fallbackValue = ct.getFallbackValue();
                                                if (pvtypes.length == thresholds.length + 1) {
                                                    for (int i = 0; i < thresholds.length; i++) {
                                                        ParameterValueType pvt = pvtypes[i + 1];
                                                        ExpressionType et = pvt.getExpressionArray(0);
                                                        String colorValue = null;
                                                        if (et instanceof LiteralTypeImpl) {
                                                            LiteralTypeImpl lti = (LiteralTypeImpl) et;
                                                            colorValue = lti.getStringValue();
                                                        } else {
                                                            LOG.error("Unsupported ExpressionType '" + et.getClass() + "'.");
                                                            return null;
                                                        }

                                                        ParameterValueType threshold = thresholds[i];
                                                        et = threshold.getExpressionArray(0);
                                                        Double thresholdValue = null;
                                                        if (et instanceof LiteralTypeImpl) {
                                                            LiteralTypeImpl lti = (LiteralTypeImpl) et;
                                                            try {
                                                                thresholdValue = Double.parseDouble(lti.getStringValue());
                                                            } catch (NumberFormatException nfe) {
                                                                LOG.error("<se:Threshold><ogc:Literal>'" + lti.getStringValue() + "'</ogc:Literal></se:Threshold> must be of Format Number.");
                                                                return null;
                                                            }
                                                        } else {
                                                            LOG.error("Unsupported ExpressionType '" + et.getClass() + "'.");
                                                            return null;
                                                        }
                                                        StyleColorMapEntry scme = new StyleColorMapEntry(colorValue, thresholdValue);
                                                        colorMapEntries.add(scme);
                                                    }
                                                } else {
                                                    LOG.error("Number of values must be equal to the number of thresholds + 1.");
                                                    return null;
                                                }
                                                if (lookUpValue != null
                                                        && startColor != null) {
                                                    colorMap = new Categorize(lookUpValue, startColor, colorMapEntries, Color.decode(fallbackValue));
                                                }
                                            } else if (cmt.getInterpolate() != null ) {
                                                InterpolateType it = cmt.getInterpolate();
                                                ParameterValueType lookupValuePVT = it.getLookupValue();
                                                ExpressionType luv = lookupValuePVT.getExpressionArray(0);
                                                String lookUpValue = null;
                                                if (luv instanceof LiteralTypeImpl) {
                                                    lookUpValue = ((LiteralTypeImpl) luv).getStringValue();
                                                }
                                                ArrayList<StyleColorMapEntry> colorMapEntries = new ArrayList<>();
                                                InterpolationPointType[] interpolationPointTypes = it.getInterpolationPointArray();
                                                for (InterpolationPointType interPointType : interpolationPointTypes) {
                                                    double interpolValue = interPointType.getData();
                                                    ParameterValueType pvt = interPointType.getValue();
                                                    ExpressionType interpolColor = pvt.getExpressionArray(0);
                                                    if (interpolColor instanceof LiteralTypeImpl) {
                                                        String colorValue = ((LiteralTypeImpl) interpolColor).getStringValue();
                                                        StyleColorMapEntry scme = new StyleColorMapEntry(colorValue, interpolValue);
                                                        colorMapEntries.add(scme);
                                                    }
                                                }
                                                if (lookUpValue != null &&
                                                        colorMapEntries.size() > 0) {
                                                    colorMap = new Interpolate(lookUpValue, colorMapEntries, Color.decode(fallbackValue));
                                                }
                                            }
                                            resultStyle = new Style(namedLayer.getName(), redChannel, greenChannel, blueChannel, colorMap, 1);
                                        }
                                    }
                                }
                            }
                        }

                    }
                }

            }
        } catch (XmlException ex) {
            LOG.error("Could not parse input: " + ex);
        }
        return resultStyle;
    }

}
