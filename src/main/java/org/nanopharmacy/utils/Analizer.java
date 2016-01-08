/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.nanopharmacy.utils;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.semanticwb.datamanager.DataList;
import org.semanticwb.datamanager.DataMgr;
import org.semanticwb.datamanager.DataObject;
import org.semanticwb.datamanager.SWBDataSource;
import org.semanticwb.datamanager.SWBScriptEngine;

/**
 *
 * @author martha.jimenez
 */
public class Analizer {

    private static ArrayList<String> glosary;
    private static Stream<String> lines;

    public static void analizer(String idSearch, String idArticle) {
        try {
            lines = Files.lines(Paths.get("D:/Docs/Documents/glosary.txt"));
            lines.forEach(s -> {
                s = s.trim();
                glosary.add(s);
            });

            SWBScriptEngine engine = DataMgr.getUserScriptEngine("/public/NanoSources.js", null, false);
            SWBDataSource dsArticle = engine.getDataSource("Article");

            DataObject obj = Utils.ENG.getDataProperty(dsArticle, "_id", idArticle, 0);
            int rows = obj.getDataObject("response").getInt("totalRows");
            if (rows > 0) {
                String abstractTxt = obj.getDataObject("response").getDataList("data").getDataObject(0).getString("abstract");
                abstractTxt = abstractTxt.replaceAll("!", " ").replaceAll("\"", " ")
                        .replaceAll("#", " ").replaceAll("$", " ").replaceAll("%", " ").replaceAll("&", " ")
                        .replaceAll("'", " ").replaceAll("\\(", " ").replaceAll("\\)", " ").replaceAll("\\*", " ")
                        .replaceAll("\\+", " ").replaceAll(",", " ").replaceAll("-", " ").replaceAll("\\.", " ").replaceAll("/", " ");
                System.out.println("abstract: " + abstractTxt);
                analizeAbstract(engine, abstractTxt, idSearch);
            }

        } catch (IOException ex) {
            Logger.getLogger(Analizer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void recommededArticles(SWBScriptEngine engine, String idSearch, ArrayList<String> phrases) {
        try {
            SWBDataSource dsArtSearch = engine.getDataSource("Art_Search");
            SWBDataSource dsArticle = engine.getDataSource("Article");

            DataObject dataArtSearch = Utils.ENG.getDataProperty(dsArtSearch, new String[]{"search"}, new String[]{idSearch}, null, null);
            DataObject dataArticle;
            int rows = dataArtSearch.getDataObject("response").getInt("totalRows");
            if (rows > 0) {
                DataList dataList = dataArtSearch.getDataObject("response").getDataList("data");
                for (int i = 0; i < dataList.size(); i++) {
                    String articleId = dataList.getDataObject(i).getString("article");
                    dataArticle = Utils.ENG.getDataProperty(dsArticle, "_id", articleId, 0);
                    rows = dataArticle.getDataObject("response").getInt("totalRows");
                    if (rows > 0) {
                        String abstractTxt = dataArticle.getDataObject("response").getDataList("data").getDataObject(0).getString("abstract");
                        Iterator it = phrases.iterator();
                        while (it.hasNext()) {
                            if(abstractTxt.contains(it.next().toString())) {
                                break;
                                //Cambiar status a 4 (recopmendado) y incrementar en 1 el conteo de recomendados detro del esquema correspondiente 
                            }
                            
                        }
 
//                        isCustomRanking(engine, abstractTxt, idSearch, phrases); //Cambiar status a 4 (recopmendado) y incrementar en 1 el conteo de recomendados detro del esquema correspondiente 
                    }
                }

            }
        } catch (IOException ex) {
            Logger.getLogger(Analizer.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    /*public boolean isCustomRanking(SWBScriptEngine engine, String abstractTxt, String idSearch, ArrayList<String> phrases) {
        boolean isCustomRanking = false;
        try {
            SWBDataSource dsAnalize = engine.getDataSource("Analize");
            DataObject obj = Utils.ENG.getDataProperty(dsAnalize, new String[]{"search"}, new String[]{idSearch}, new String[]{"umbral"}, new int[]{1});
            int rows = obj.getDataObject("response").getInt("totalRows");
            if (rows > 0) {
                DataList dataList = obj.getDataObject("response").getDataList("data");
                for (int i = 0; i < dataList.size(); i++) {
                    String key = dataList.getDataObject(i).getString("key");
                    if (abstractTxt.contains(key)) {
                        isCustomRanking = true;
                        break;
                    }
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(Analizer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return isCustomRanking;
    }*/

    /**
     *
     * @param engine
     * @param idSearch
     * @param frequency n&uacute;mero de veces que se ha detectado la frase
     * @param threshold bandera que permite definir el porcentaje para mover la moda de una frase
     * @return
     */
    private boolean calculateThreshold(SWBScriptEngine engine, String idSearch, int frequency, int threshold) {
        boolean isTreshold = false;
        try {
            //Obtener num articulos
            SWBDataSource dsArtSearch = engine.getDataSource("Art_Search");
            DataObject obj = Utils.ENG.getDataProperty(dsArtSearch, new String[]{"search"}, new String[]{idSearch}, new String[]{"status"}, new int[]{2});//status
            int totalArtsAccept = obj.getDataObject("response").getInt("totalRows");
            //Calcular porcentaje
            //Programar algoritmo

        } catch (IOException ex) {
            Logger.getLogger(Analizer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return isTreshold;
    }

    public static void analizeAbstract(SWBScriptEngine engine, String abstractTxt, String idSearch) {
        SWBDataSource dsAnalize = engine.getDataSource("Analize");
//        lines.forEach(s -> {
        Iterator<String> it = glosary.listIterator(0);
        ArrayList phrases = null;
//        boolean hasChange = false;
        while (it.hasNext()) {
            String s = it.next();
            //s = s.trim();
            if (abstractTxt.contains(" " + s + " ")) {//Pattern.matches("[ ]"+Pattern.quote(s)+"[ ]"
                try {
                    DataObject obj = Utils.ENG.getDataProperty(dsAnalize, new String[]{"search", "key"}, new String[]{idSearch, s}, null, null);
                    int rows = obj.getDataObject("response").getInt("totalRows");

                    if (rows > 0) {
                        DataObject analizeObj = obj.getDataObject("response").getDataList("data").getDataObject(0);
                        analizeObj.put("frequency", analizeObj.getInt("frequency") + 1);

                        dsAnalize.updateObj(analizeObj);
                    } else {
                        DataObject newAnalizeObj = new DataObject();
                        newAnalizeObj.put("search", idSearch);
                        newAnalizeObj.put("key", s);
                        newAnalizeObj.put("frequency", 1);
                        dsAnalize.addObj(newAnalizeObj);
                    }
                    //Obtener num articulos
                    //Calcular porcentaje
                    //Detectar si existe nueva keyWord o dejo de ser moda alguna frase
                    //if(calculateThreshold) {
                    // phrases.add(key)
                    //}

                } catch (IOException ex) {
                    Logger.getLogger(Analizer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        if(phrases != null) {
            recommededArticles(engine, idSearch, phrases);
        }
//        });
    }

}
