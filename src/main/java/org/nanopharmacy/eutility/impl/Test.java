/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.nanopharmacy.eutility.impl;

import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author jose.jimenez
 */
public class Test {
    
    public static void main(String[] args) {
        
        ESearchImpl search = new ESearchImpl();
        try {
            System.out.println("Ejecutando...");
            for (int i = 0; i < 3; i++) {
                JSONObject resp = search.getPublicationsInfo("AKT1", "c.49G>A", 0, 0, (short) (i * 12), (short) ((i + 1) * 12));
                if (resp.has("outstanding")) {
                    System.out.println("Para almacenar: " + resp.getJSONArray("outstanding").length());
                }
                if (resp.has("rejected")) {
                    System.out.println("Rechazados: " + resp.getJSONArray("rejected").length());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}
