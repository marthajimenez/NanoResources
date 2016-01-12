/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.nanopharmacy.utils;

import java.io.IOException;
import java.nio.file.Files;
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

    /**
     * Lee archivo txt de glosario y lo inserta en el datasource Glossary
     *
     * @param path Direccion fisica del archivo txt
     */
    public static void loadGlossary(String path) {
        SWBScriptEngine engine = DataMgr.getUserScriptEngine("/public/NanoSources.js", null, false);
        final SWBDataSource dsGlossary = engine.getDataSource("Glossary");
        DataObject newGlossaryObj = new DataObject();
        try {
            lines = Files.lines(Paths.get(path));
            Iterator<String> iterator = lines.iterator();
            while (iterator.hasNext()) {
                newGlossaryObj = new DataObject();
                newGlossaryObj.put("key", iterator.next());
                //newGlossaryObj.put("definition", "");
                dsGlossary.addObj(newGlossaryObj);

            }
        } catch (IOException ex) {
            Logger.getLogger(Analizer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Retorna iterador con las frases del glosario
     *
     * @return Retorna objeto Iterator<DataObject>
     */
    public static Iterator<DataObject> getGlossaryList() {
        Iterator<DataObject> dataList = null;
        try {
            SWBScriptEngine engine = DataMgr.getUserScriptEngine("/public/NanoSources.js", null, false);
            SWBDataSource dsGlossary = engine.getDataSource("Glossary");
            DataObject dataProperty = Utils.ENG.getDataProperty(dsGlossary, null, null, 0);
            dataList = Utils.ENG.getDataList(dataProperty);
        } catch (IOException ex) {
            Logger.getLogger(Analizer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return dataList;
    }

    /**
     * Analiza el abstract de un articulo para encontrar ocurrencias de las
     * frases existentes en el glosario.
     *
     * @param idSearch Identificador de la busquedad
     * @param idArticle Identificador del articulo
     */
    public static int analizer(String idSearch, String idArticle) {
        int newRecommended = 0;
        try {
            SWBScriptEngine engine = DataMgr.getUserScriptEngine("/public/NanoSources.js", null, false);
            SWBDataSource dsArticle = engine.getDataSource("Article");
            DataObject obj = Utils.ENG.getDataProperty(dsArticle, "_id", idArticle, 0);
            int rows = obj.getDataObject("response").getInt("totalRows");
            if (rows > 0) {
                String abstractTxt = obj.getDataObject("response").getDataList("data").getDataObject(0).getString("abstract");
                //System.out.println("abstract: " + abstractTxt);
                newRecommended = Analizer.analizeAbstract(engine, abstractTxt, idSearch);
            }
        } catch (IOException ex) {
            Logger.getLogger(Analizer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return newRecommended;
    }

    /**
     * Califica la relevancia de los ariculos en base a las phases aceptadas
     *
     * @param engine Engine.
     * @param idSearch Identificador de la busqueda actual.
     * @param phrases Lista de frases aceptadas.
     */
    public static int reclassifyArticles(SWBScriptEngine engine, String idSearch, ArrayList<String> phrases) {
        int newRecommended = 0;
        try {
            SWBDataSource dsArtSearch = engine.getDataSource("Art_Search");
            SWBDataSource dsArticle = engine.getDataSource("Article");
            SWBDataSource dsSearch = engine.getDataSource("Search");
            DataObject dataArtSearch = Utils.ENG.getDataProperty(dsArtSearch, new String[]{"search"}, new String[]{idSearch}, null, null);
            int rows = dataArtSearch.getDataObject("response").getInt("totalRows");
            if (rows > 0) {
                DataList dataList = dataArtSearch.getDataObject("response").getDataList("data");
                newRecommended = reclassifyArticlesModel( engine, dataList , idSearch, phrases);
            }
        } catch (IOException ex) {
            Logger.getLogger(Analizer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return newRecommended;
    }

    public static int reclassifyArticlesModel(SWBScriptEngine engine,DataList dataList ,String idSearch, ArrayList<String> phrases) {
        SWBDataSource dsArticle = engine.getDataSource("Article");
        SWBDataSource dsArtSearch = engine.getDataSource("Art_Search");
        DataObject dataArticle;
        int newRecommended = 0;
        for (int i = 0; i < dataList.size(); i++) {
            if (dataList.getDataObject(i).getInt("status") == 1) {
                try {
                    String articleId = dataList.getDataObject(i).getString("article");
                    dataArticle = Utils.ENG.getDataProperty(dsArticle, "_id", articleId, 0);
                    int rows = dataArticle.getDataObject("response").getInt("totalRows");
                    if (rows > 0) {
                        String abstractTxt = dataArticle.getDataObject("response").getDataList("data").getDataObject(0).getString("abstract");
                        Iterator it = phrases.iterator();
                        int ranking = 0;
                        while (it.hasNext()) {
                            if (abstractTxt.contains(it.next().toString())) {
                                ranking++;
                            }
                        }
                        ranking = Math.round(((float) ranking / (float) phrases.size()) * 10);
                        System.out.println("Ranking: " + ranking);
                        dataList.getDataObject(i).put("ranking", ranking);
                        dsArtSearch.updateObj(dataList.getDataObject(i));
                        if (ranking > 5) {
                            newRecommended++;
                        }
                    }
                } catch (IOException ex) {
                    Logger.getLogger(Analizer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return newRecommended;
    }

    /**
     * Determina si una frase del glosario para una busqueda en especifico se
     * convierte en frase keyword
     *
     * @param totalArtsAccept total de articulos aceptados en una busqueda
     * @param frequency n&uacute;mero de veces que se ha detectado la frase
     * @param threshold bandera que permite definir el porcentaje para mover la
     * @param analizeObj Registro que contiene la frase
     * @param dsAnalize Tabla de la BD que contiene la frecuencia de aparicion
     * de las frases en una busqueda
     * @return un boolean si la frase ha pasado el umbral para ser considerada
     * como keyword
     */
    private static boolean calculateThreshold(int totalArtsAccept, int frequency, int threshold, DataObject analizeObj, SWBDataSource dsAnalize) {
        boolean isTreshold = false;
        float percentAccept = 0.66F;
        float percentReject = 0.51F;
        float percent = (float) frequency / (float) totalArtsAccept;
        try {
            if (threshold == 0) { // No esta entre las frases aceptadas
                if (percent > percentAccept) {
                    analizeObj.put("threshold", 1);
                    System.out.println("Entro : " + analizeObj.getString("key"));
                    dsAnalize.updateObj(analizeObj);
                    isTreshold = true;
                }
            } else { // Esta entre las frases aceptadas
                if (percent < percentReject) {
                    //System.out.println("Salio : " + analizeObj.getString("key"));
                    analizeObj.put("threshold", 0);
                    dsAnalize.updateObj(analizeObj);
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(Analizer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return isTreshold;
    }

    /**
     *
     * @param engine
     * @param abstractTxt
     * @param idSearch
     */
    public static int analizeAbstract(SWBScriptEngine engine, String abstractTxt, String idSearch) {
        int newRecommended = 0;
        try {
            SWBDataSource dsAnalize = engine.getDataSource("Analize");
            SWBDataSource dsArtSearch = engine.getDataSource("Art_Search");
            DataObject obj = Utils.ENG.getDataProperty(dsArtSearch, new String[]{"search"}, new String[]{idSearch}, new String[]{"status"}, new int[]{2});//status
            int totalArtsAccept = obj.getDataObject("response").getInt("totalRows");
            Iterator<DataObject> it = Analizer.getGlossaryList();
            ArrayList phrases = new ArrayList();
            while (it.hasNext()) {
                String s = it.next().getString("key");
                int frequency = 1;
                Pattern pattern = Pattern.compile("^" + Pattern.quote(s) + "\\W|\\W"
                        + Pattern.quote(s) + "\\W|\\W" + Pattern.quote(s) + "$",
                        Pattern.DOTALL + Pattern.CASE_INSENSITIVE);
                if (pattern.matcher(abstractTxt).find()) {
                    try {
                        obj = Utils.ENG.getDataProperty(dsAnalize, new String[]{"search", "key"}, new String[]{idSearch, s}, null, null);
                        int rows = obj.getDataObject("response").getInt("totalRows");
                        DataObject analizeObj = new DataObject();

                        if (rows > 0) {
                            analizeObj = obj.getDataObject("response").getDataList("data").getDataObject(0);
                            frequency = analizeObj.getInt("frequency") + 1;
                            analizeObj.put("frequency", frequency);
                            dsAnalize.updateObj(analizeObj);
                        } else {
                            analizeObj.put("search", idSearch);
                            analizeObj.put("key", s);
                            analizeObj.put("frequency", 1);
                            analizeObj.put("threshold", 0);
                            dsAnalize.addObj(analizeObj);
                        }

                    } catch (IOException ex) {
                        Logger.getLogger(Analizer.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            boolean calculateThreshold = false;
            if (totalArtsAccept > 2) {
                DataObject analizeObj = new DataObject();
                obj = Utils.ENG.getDataProperty(dsAnalize, new String[]{"search"}, new String[]{idSearch}, null, null);
                Iterator<DataObject> dataList = Utils.ENG.getDataList(obj);
                while (dataList.hasNext()) {
                    analizeObj = dataList.next();
                    if (Analizer.calculateThreshold(totalArtsAccept, analizeObj.getInt("frequency"),
                            analizeObj.containsKey("threshold") ? analizeObj.getInt("threshold") : 0, analizeObj, dsAnalize)) {
                        calculateThreshold = true;
                    }
                    if (analizeObj.containsKey("threshold") && analizeObj.getInt("threshold") == 1) {
                        phrases.add(analizeObj.getString("key"));
                    }
                }
            }
            if (phrases.size() > 0 && calculateThreshold) {
                newRecommended = Analizer.reclassifyArticles(engine, idSearch, phrases);
            }
        } catch (IOException ex) {
            Logger.getLogger(Analizer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return newRecommended;
    }

}
