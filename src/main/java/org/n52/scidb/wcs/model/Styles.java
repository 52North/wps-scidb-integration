/*
 * TODO: Add License Header
 */
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.n52.scidb.wcs.model;

import java.io.Serializable;
import java.util.ArrayList;
import org.springframework.stereotype.Component;

/**
 *
 * @author Maurin Radtke <m.radtke@52north.org>
 */
@Component
public class Styles implements Serializable{
    
    private ArrayList<Style> stylesList;
    
    public Styles(){
        this.stylesList = new ArrayList();
    }

    public ArrayList<Style> getStylesList() {
        return stylesList;
    }

    public void setStylesList(ArrayList<Style> stylesList) {
        this.stylesList = stylesList;
    }
    
    public boolean addStyle(Style style) {
        for (Style currStyle : this.stylesList) {
            if (currStyle.getSldName().equals(style.getSldName())) {
                return false;
            }
        }
        this.stylesList.add(style);
        return true;
    }
    
    public Style getStyleByName(String styleName) {
        for (Style style : this.stylesList) {
            if (style.getSldName().equals(styleName)) {
                return style;
            }
        }
        return null;
    }
    
}