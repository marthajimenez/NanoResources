package org.nanopharmacy.ai;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.nanopharmacy.utils.Utils;
import org.semanticwb.datamanager.DataList;
import org.semanticwb.datamanager.DataMgr;
import org.semanticwb.datamanager.DataObject;
import org.semanticwb.datamanager.SWBDataSource;
import org.semanticwb.datamanager.SWBScriptEngine;

/**
 * Implementa un metodo de inteligencia artificial a traves del cual la aplicacion aprende y
 * mejora la clasificacion de los articulos recomendados en terminos de las preferencias que
 * manifiesta un usuario al aceptar articulos dentro de cada esquema de busqueda.
 * @author martha.jimenez
 */
public class Analizer {

    /**
     * Lee archivo txt de glosario e inserta su contenido en el datasource Glossary
     * @param path direccion fisica del archivo txt
     */
    public static void loadGlossary(String path) {
        SWBScriptEngine engine = DataMgr.getUserScriptEngine("/public/NanoSources.js", null, false);
        final SWBDataSource dsGlossary = engine.getDataSource("Glossary");
        DataObject newGlossaryObj = new DataObject();
        Stream<String> lines;
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
     * @return Retorna objeto Iterator<DataObject> que contiene las frases del glosario
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
     * @param idSearch identificador del esquema de busqueda al que esta asociado el articulo
     * @param idArticle identificador del articulo del que se va a analizar el abstract
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
     * Califica la relevancia de los articulos en base a las frases aceptadas
     * @param engine maquina de scripts proporcionada por SemanticWebBuilder
     * @param idSearch identificador del esquema de busqueda del que se desea calificar sus articulos
     * @param phrases lista de frases aceptadas como keywords para evaluar los abstracts de los articulos
     * @param isByUser indica si los esquemas de busqueda se asocian a los usuarios que los crean o no
     * @return el numero de articulos considerados {@literal recomendados} con base a la calificacion asignada.
     */
    public static int reclassifyArticles(SWBScriptEngine engine, String idSearch, ArrayList<String> phrases, boolean isByUser) {
        int newRecommended = 0;
        try {
            SWBDataSource dsArtSearch = engine.getDataSource("Art_Search");
            SWBDataSource dsArticle = engine.getDataSource("Article");
            DataObject dataArticle;
            DataObject dataArtSearch = Utils.ENG.getDataProperty(dsArtSearch, new String[]{"search"}, new String[]{idSearch}, null, null);
            int rows = dataArtSearch.getDataObject("response").getInt("totalRows");
            if (rows > 0) {
                DataList dataList = dataArtSearch.getDataObject("response").getDataList("data");
                for (int i = 0; i < dataList.size(); i++) {
                    if (((dataList.getDataObject(i).getInt("status") == 1 && (dataList.getDataObject(i).getInt("ranking") < 6)) && !isByUser)
                            || ((dataList.getDataObject(i).getInt("status") == 1 || dataList.getDataObject(i).getInt("status") == 4) && isByUser)) {
                        String articleId = dataList.getDataObject(i).getString("article");
                        dataArticle = Utils.ENG.getDataProperty(dsArticle, "_id", articleId, 0);
                        rows = dataArticle.getDataObject("response").getInt("totalRows");
                        if (rows > 0) {
                            String abstractTxt = dataArticle.getDataObject("response").getDataList("data").getDataObject(0).getString("abstract");
                            int ranking = Analizer.calculateRanking(phrases, abstractTxt, isByUser);
                            boolean isAlreadyRecommeded = true;
                            if (ranking < dataList.getDataObject(i).getInt("ranking")) {
                                ranking = dataList.getDataObject(i).getInt("ranking");
                            }
                            if (dataList.getDataObject(i).getInt("ranking") > 5) {
                                isAlreadyRecommeded = false;
                            }
                            System.out.println("Ranking: " + ranking);
                            dataList.getDataObject(i).put("ranking", ranking);
                            dsArtSearch.updateObj(dataList.getDataObject(i));
                            if (ranking > 5 && isAlreadyRecommeded) {
                                newRecommended++;
                            }
                        }
                    }
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(Analizer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return newRecommended;
    }

    /**
     * Determina el ranking del contenido de un abstract representado por {@code abstractTxt}.
     * @param phrases arreglo del conjunto de frases que han aparecido como de interes en un
     * esquema de busqueda
     * @param abstractTxt texto del abstract para el cual se determinara el ranking
     * @param isByUser indica si los esquemas de busqueda se asocian a los usuarios que los crean, o no
     * @return un valor entero que representa el ranking correspondiente al texto proporcionado,
     * en base a las {@code phrases} especificadas. El valor devuelto cumple con lo siguiente: 0 >= valorDevuelto <= 10
     */
    private static int calculateRanking(ArrayList<String> phrases, String abstractTxt, boolean isByUser) {
        Iterator it = phrases.iterator();
        int finalRanking = 0;
        int ranking = 0;
        while (it.hasNext()) {
            String phrase = it.next().toString();
            Pattern pattern = Pattern.compile("^" + Pattern.quote(phrase) + "\\W|\\W"
                    + Pattern.quote(phrase) + "\\W|\\W" + Pattern.quote(phrase) + "$",
                    Pattern.DOTALL + Pattern.CASE_INSENSITIVE);
            if (pattern.matcher(abstractTxt).find()) {
                ranking++;
            }
        }
        if (isByUser) {
            if (ranking > 0) {
                finalRanking = ranking + 6;
                if (finalRanking > 10) {
                    finalRanking = 10;
                }
            }
        } else {
            finalRanking = Math.round(((float) ranking / (float) (phrases.size())) * 10);
        }
        return finalRanking;
    }

    /**
     * Ejecuta el calculo del ranking para un articulo de acuerdo a la aparicion de uno o
     * mas keywords y determina si dicho articulo se considera recomendado o no, con base en el
     * valor del ranking obtenido
     * @param engine maquina de scripts proporcionada por SemanticWebBuilder
     * @param idSearch identificador del esquema de busqueda al que esta asociado el articulo
     * @param abstractTxt contenido del abstract de un articulo
     * @param artSearch identificador del registro que contiene la relacion entre el esquema 
     *        de busqueda y el articulo que contiene el abstract
     * @return un entero cuyo valor es 1 si el articulo es recomendado, o 0 si no es recomendado
     */
    public static int getUpdateArticleRanking(SWBScriptEngine engine, String idSearch, String abstractTxt, String artSearch) {
        int recommended = 0;
        try {
            SWBDataSource dsArtSearch = engine.getDataSource("Art_Search");
            DataObject datObjSearch = dsArtSearch.fetchObjById(artSearch);
            ArrayList<String> phrases = Analizer.getGlossaryThresholdSearch(engine, idSearch, false, true);
            if (phrases.size() > 0) {
                int ranking = Analizer.calculateRanking(phrases, abstractTxt, false);
                datObjSearch.put("ranking", ranking);
                dsArtSearch.updateObj(datObjSearch);
                if (ranking > 5) {
                    recommended = 1;
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(Analizer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return recommended;
    }

    /**
     * Obtiene el listado de keywords en específico para un esquema de busqueda en particular
     * @param engine la maquina de scripts proporcionada por SemanticWebBuilder.
     * @param idSearch identificador del esquema de busqueda del que se desea obtener los keywords
     * @param isAddByUser si es {@code true} indica que los keywods devueltos son las frases que 
     *        el usuario agregó al glosario y que actualmente tienen estatus de keyword
     * @param isUpdated si es {@code true} indica que las cadenas devueltas son terminos existentes 
     *        en el glosario que actualmente tienen estatus de keyword
     * @return {@code ArrayList<String>} con los keywords asociados al esquema de busqueda indicado
     */
    private static ArrayList getGlossaryThresholdSearch(SWBScriptEngine engine, String idSearch,
            boolean isAddByUser, boolean isUpdated) {
        
        ArrayList<String> phrases = new ArrayList<>();
        try {
            SWBDataSource dsAnalize = engine.getDataSource("Analize");
            String[] properties;
            int[] values;
            if (isUpdated) {
                properties = new String[] {"threshold"};
                values = new int[] {1};
            } else {
                if (isAddByUser) {
                    properties = new String[] {"threshold", "addByUser"};
                    values = new int[] {1, 1};
                } else {
                    properties = new String[] {"threshold", "addByUser"};
                    values = new int[] {1, 0};
                }
            }

            DataObject obj = Utils.ENG.getDataProperty(dsAnalize, new String[] {"search"},
                             new String[] {idSearch}, properties, values);
            int rows = obj.getDataObject("response").getInt("totalRows");
            if (rows > 0) {
                DataList dataList = obj.getDataObject("response").getDataList("data");
                for (int i = 0; i < dataList.size(); i++) {
                    if (dataList.getDataObject(i).getString("key") != null) {
                        phrases.add(dataList.getDataObject(i).getString("key"));
                    }
                }
            }
//            phrases.add("prognosis");
//            phrases.add("treatment");
//            phrases.add("prediction");
        } catch (IOException ex) {
            Logger.getLogger(Analizer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return phrases;
    }

    /**
     * Determina si una frase del glosario para una busqueda en especifico se
     * convierte en frase keyword
     * @param totalArtsAccept total de articulos aceptados en una busqueda
     * @param frequency n&uacute;mero de veces que se ha detectado la frase
     * @param threshold bandera que permite definir el umbral de una frase
     * @param analizeObj registro que contiene la frase
     * @param dsAnalize tabla de la BD que contiene la frecuencia de aparici&oacute;n
     * de las frases en una b&uacute;squeda
     * @return {@code true} si la frase ha pasado el umbral para ser considerada
     * como keyword, {@code false} de lo contrario
     */
    private static boolean calculateThreshold(int totalArtsAccept, int frequency, int threshold,
            DataObject analizeObj, SWBDataSource dsAnalize) {
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
                if (percent < percentReject && analizeObj.getInt("addByUser") == 0) {
                    System.out.println("Salio : " + analizeObj.getString("key"));
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
     * Analiza el abstract de un articulo, incrementa el contador de las apariciones de una
     * frase, determina si alguna frase se convierte en un keyword para
     * reclasificar los articulos que tienen estatus de nuevo.
     * @param engine la maquina de scripts provista por SemanticWebBuilder
     * @param abstractTxt representa el abstract de un articulo
     * @param idSearch identificador de un esquema de busqueda
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
                Pattern pattern = Pattern.compile("^" + Pattern.quote(s) + "\\W|\\W"
                        + Pattern.quote(s) + "\\W|\\W" + Pattern.quote(s) + "$",
                        Pattern.DOTALL + Pattern.CASE_INSENSITIVE);
                if (pattern.matcher(abstractTxt).find()) {
                    Analizer.increaseFrequency(dsAnalize, s, idSearch);
                }
            }
            boolean calculateThreshold = false;
            if (totalArtsAccept > 2) {
                obj = Utils.ENG.getDataProperty(dsAnalize, new String[]{"search"}, new String[]{idSearch}, null, null);
                Iterator<DataObject> dataList = Utils.ENG.getDataList(obj);
                while (dataList.hasNext()) {
                    DataObject analizeObj = dataList.next();
                    if (Analizer.calculateThreshold(totalArtsAccept, analizeObj.getInt("frequency"),
                            analizeObj.containsKey("threshold") ? analizeObj.getInt("threshold") : 0, analizeObj, dsAnalize)) {
                        calculateThreshold = true;
                    }
//                    if (analizeObj.containsKey("threshold") && analizeObj.getInt("threshold") == 1) {
//                        phrases.add(analizeObj.getString("key"));
//                    }
                }
                phrases = Analizer.getGlossaryThresholdSearch(engine, idSearch, false, false);

                if (phrases.size() > 0 && calculateThreshold) {
                    newRecommended = Analizer.reclassifyArticles(engine, idSearch, phrases, false);
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(Analizer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return newRecommended;
    }

    /**
     * Ejecuta la calificacion de los articulos asociados a un esquema de busqueda con base en el conjunto
     * de keywords actuales con el fin de aumentar el numero de articulos recomendados.
     * @param idSearch identificador del esquema de busqueda del que se desea modificar la 
     *        calificacion de los articulos relacionados
     * @return el numero de articulos clasificados como recomendados asociados al esquema de busqueda
     */
    public static int userReclassifyArticle(String key, String idSearch) {
        SWBScriptEngine engine = DataMgr.getUserScriptEngine("/public/NanoSources.js", null, false);
        ArrayList<String> thresholdList = Analizer.getGlossaryThresholdSearch(engine, idSearch, true, false);
        int reclassifyArticles = reclassifyArticles(engine, idSearch, thresholdList, true);
        return reclassifyArticles;
    }

    /**
     * Incrementa las ocurrencias de una frase en un esquema de busqueda.
     * @param dsAnalize SWBDataSource de la tabla que contiene las frases que
     * ocurren en una busqueda
     * @param s representa la frase que sera incrementada en las ocurrencias de
     * una busqueda
     * @param idSearch identificador del esquema de busqueda
     */
    private static void increaseFrequency(SWBDataSource dsAnalize, String s, String idSearch) {
        try {
            int frequency = 1;
            DataObject obj = Utils.ENG.getDataProperty(dsAnalize, new String[]{"search", "key"}, new String[]{idSearch, s}, null, null);
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
                analizeObj.put("frequency", frequency);
                analizeObj.put("threshold", 0);
                analizeObj.put("addByUser", 0);
                dsAnalize.addObj(analizeObj);
            }

        } catch (IOException ex) {
            Logger.getLogger(Analizer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Analiza el abstract de un articulo para decrementar el contador de las ocurrencias de las
     * frases existentes en el glosario de un esquema de busqueda.
     * @param idSearch identificador del esquema de busqueda asociado al articulo
     * @param idArticle identificador del articulo cuyo abstract tiene que analizarse
     */
    public static void analyzeRejected(String idSearch, String idArticle) {
        try {
            SWBScriptEngine engine = DataMgr.getUserScriptEngine("/public/NanoSources.js", null, false);
            SWBDataSource dsArticle = engine.getDataSource("Article");
            SWBDataSource dsAnalize = engine.getDataSource("Analize");
            DataObject obj = Utils.ENG.getDataProperty(dsArticle, "_id", idArticle, 0);
            int rows = obj.getDataObject("response").getInt("totalRows");
            if (rows > 0) {
                String abstractTxt = obj.getDataObject("response").getDataList("data").getDataObject(0).getString("abstract");
                obj = Utils.ENG.getDataProperty(dsAnalize, new String[]{"search"}, new String[]{idSearch}, null, null);
                rows = obj.getDataObject("response").getInt("totalRows");
                if (rows > 0) {
                    DataList list = obj.getDataObject("response").getDataList("data");
                    for (int j = 0; j < list.size(); j++) {
                        DataObject keyList = list.getDataObject(j);
                        String s = keyList.getString("key");
                        Pattern pattern = Pattern.compile("^" + Pattern.quote(s) + "\\W|\\W"
                                + Pattern.quote(s) + "\\W|\\W" + Pattern.quote(s) + "$",
                                Pattern.DOTALL + Pattern.CASE_INSENSITIVE);
                        if (pattern.matcher(abstractTxt).find()) {
                            keyList.put("frequency", keyList.getInt("frequency") - 1);
                            dsAnalize.updateObj(keyList);
                        }
                    }

                }
            }
        } catch (IOException ex) {
            Logger.getLogger(Analizer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
