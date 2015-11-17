/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.nanopharmacy.utils;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
//import org.nanopharmacy.utils.Utils.BD;
import org.semanticwb.datamanager.DataList;
import org.semanticwb.datamanager.DataMgr;
import org.semanticwb.datamanager.DataObject;
import org.semanticwb.datamanager.SWBDataSource;
import org.semanticwb.datamanager.SWBScriptEngine;

/**
 * <p>Clase que contiene utiler&iacute;as para manejo de XML, ENG y Text.</p>
 * @author martha.jimenez
 */
public class Utils {

    /**
     * Calcula el valor de relevancia de una publicación médica recuperada con
     * Entrez.
     *
     * @param text el abstract de la publicación. El texto del abstract de la
     * publicación o artículo.
     * @return Un valor entero indicando la relevancia de la publicación. Un
     * valor 10 indica la mayor relevancia ya que menciona la alteración
     * molecular; 8 si el abstract del artículo no menciona la alteración
     * molecular pero se mencionan más de una vertiente. 6 si solo se menciona
     * una vertiente; 2 si solo se menciona el nombre del gen referido con
     * {@code geneName}; y 0 en cualquier caso que no cumple con ninguno de los
     * criterios anterires. Una vertiente es cualquiera de las siguientes
     * palabras: prognosis, treatment o cualquier palabra que inicie con
     * predict.
     */
    public static int getRanking(String text, String geneName, String molecularAlt) {
        int rank = 0;
        if (text == null || geneName == null || molecularAlt == null) {
            return 0;
        }

        final String content = text.toLowerCase();
        if (content.contains(geneName)) {
            rank = 2;
        }
        if (content.contains("prognosis") || content.contains("treatment") || content.contains("predict")) {
            rank = 6;
        }
        if (content.contains("prognosis") && content.contains("treatment")
                || content.contains("prognosis") && content.contains("predict")
                || content.contains("treatment") && content.contains("predict")
                || content.contains("prognosis") && content.contains("treatment") && content.contains("predict")) {
            rank = 8;
        }
        if (content.contains(molecularAlt)) {
            rank = 10;
        }

        return rank;
    }

    /**
     * Representa la abreviatura de los meses
     */
    public static String[] Months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Agu", "Sep", "Oct", "Nov", "Dec"};

    /**
     * <p>
     * Provee funciones para la manipulaci&oacute;n de archivos XML.</p>
     */
    public static class XML {

        /**
         * Convierte un objeto {@code InputStream} a un objeto {@code Document}
         * 
         * @param is objeto {@code InputStream} que ser&aacute; tranformado a 
         * un objeto de tipo {@code Document}
         * @return objeto {@code Document}
         * @throws IOException si durante la ejecuci&oacute;n ocurre
         * alg&uacute;n problema con la generaci&oacute;n o escritura de la
         * respuesta
         * @throws JDOMException si durante la transformaci&oacute;n ocurre
         * alg&uacute;n problema con la generaci&oacute;n o escritura del 
         * documento.
         */
        public static org.jdom.Document getXML(InputStream is) throws IOException, JDOMException {
            SAXBuilder builder = new SAXBuilder();
            //String data = getStringFromInputStream(is);
            //byte[] data2 = data.getBytes();
            Document document = builder.build(is);
            //Document document = (Document) builder.build(new ByteArrayInputStream(data2));
            return document;
        }

    }

    /*
     * <p>
     * Provee funciones para la manipulaci&oacute;n del API de ENG.</p>
     */
    public static class ENG {

        /**
         * Valida que un registro con p&aacute;rametros espec&iacute;ficos
         * exista en una tabla de la BD.
         *
         * @param titleDataSource representa el nombre de la tabla o
         * {@code SWBDataSource}, en la cual se llevar&aacute; a cabo la
         * b&uacute;squeda
         * @param namesString arreglo de string con los nombres de las columnas
         * en las que ser&aacute;n buscados los par&aacute;metros. Este arreglo
         * contiene los nombres de las columnas que almacenen propiedades
         * {@code String}
         * @param values arreglo con los par&aacute;metros a buscar. Este
         * arreglo contiene las propiedades de tipo {@code String}
         * @param namesInt arreglo de string con los nombres de las columnas en
         * las que ser&aacute;n buscados los par&aacute;metros. Este arreglo
         * contiene los nombres de las columnas que almacenen propiedades
         * {@code int}
         * @param valuesInt arreglo con los par&aacute;metros a buscar. Este
         * arreglo contiene las propiedades de tipo {@code int}
         * @return {@code boolean} que representa si existe o no el registro con
         * los par&aacute;metros proporcionados
         * @throws IOException si durante la ejecuci&oacute;n ocurre
         * alg&uacute;n problema con la generaci&oacute;n o escritura de la
         * respuesta
         */
        public static boolean isValidObject(String titleDataSource, String[] namesString, String[] values,
                String[] namesInt, int[] valuesInt)
                throws IOException {
            boolean valid = false;
            SWBScriptEngine engine = DataMgr.getUserScriptEngine("/test/NanoSources.js", null, false);
            SWBDataSource ds = engine.getDataSource(titleDataSource);

            /*DataObject query = new DataObject();
             DataObject data = new DataObject();
             query.put("data", data);
             if (namesString != null) {
             for (int i = 0; i < namesString.length; i++) {
             data.put(namesString[i], values[i]);
             }
             }
             if (namesInt != null) {
             for (int i = 0; i < namesInt.length; i++) {
             data.put(namesInt[i], valuesInt[i]);
             }
             }

             DataObject obj = ds.fetch(query);*/
            DataObject obj = getDataProperty(ds, namesString, values, namesInt, valuesInt);

            int i = obj.getDataObject("response").getInt("totalRows");
            if (i == 0) {
                valid = true;
            }
            return valid;
        }

        /**
         * Obtiene el identificador de un registro en BD a partir de alguna
         * columna con valores &uacute;nicos.
         *
         * @param dataSource representa el nombre de la tabla o
         * {@code SWBDataSource}, en la cual se llevar&aacute; a cabo la
         * b&uacute;squeda
         * @param property es el nombre de la columna que contiene valores
         * &uacute;nicos
         * @param valueProp representa el valor o par&aacute;metro que
         * ser&aacute; buscado en la BD.
         * @return el identificador del registro en BD, en caso no existir
         * devuelve un null
         * @throws IOException si durante la ejecuci&oacute;n ocurre
         * alg&uacute;n problema con la generaci&oacute;n o escritura de la
         * respuesta
         */
        public static String getIdProperty(String dataSource, String property, String valueProp) throws IOException {
            String ret = null;
            SWBScriptEngine engine = DataMgr.getUserScriptEngine("/test/NanoSources.js", null, false);
            SWBDataSource ds = engine.getDataSource(dataSource);
            DataObject obj = getDataProperty(ds, property, valueProp, 0);
            if (obj != null) {
                int rows = obj.getDataObject("response").getInt("totalRows");
                if (rows != 0) {
                    DataList list = obj.getDataObject("response").getDataList("data");
                    for (int j = 0; j < list.size(); j++) {
                        DataObject genList = list.getDataObject(j);
                        if (genList.getString(property) != null && genList.getString(property).equals(valueProp)) {
                            if (genList.containsKey("_id")) {
                                ret = genList.getString("_id");
                            }
                            break;
                        }
                    }

                }
            }
            return ret;
        }

        /**
         * Obtiene un registro a una tabla de la BD, que coincida con un
         * p&aacute;rametro proporcionado.
         *
         * @param ds representa un objeto SWBDataSource el cual define la tabla
         * en la que se desea buscar el registro.
         * @param property representa el nombre de las columna en la que
         * ser&aacute; buscado el par&aacute;metro
         * @param valueProp representa el par&aacute;metro a buscar. Si el
         * par&aacute;metro no es de tipo {@code String}, el valor de esta
         * propiedad deber&aacute; ser null.
         * @param valProp representa el par&aacute;metro a buscar. Si el
         * par&aacute;metro no es de tipo {@code int}, el valor de esta
         * propiedad deber&aacute; ser 0.
         * @return el resultado de la busqueda en BD,devuelto en un objeto
         * {@code DataObject}
         * @throws IOException si durante la ejecuci&oacute;n ocurre
         * alg&uacute;n problema con la generaci&oacute;n o escritura de la
         * respuesta
         */
        public static DataObject getDataProperty(SWBDataSource ds, String property, String valueProp, int valProp) throws IOException {
            DataObject query = new DataObject();
            DataObject data = new DataObject();

            query.put("data", data);
            data.put(property, valueProp != null ? valueProp : valProp);
            DataObject obj = ds.fetch(query);
            return obj;
        }

        /**
         * Obtiene un registro a una tabla de la BD, que coincida con un
         * conjunto de p&aacute;rametros proporcionados.
         *
         * @param ds representa un objeto SWBDataSource el cual define la tabla
         * en la que se desea buscar el registro.
         * @param namesString arreglo de string con los nombres de las columnas
         * en las que ser&aacute;n buscados los par&aacute;metros. Este arreglo
         * contiene los nombres de las columnas que almacenen propiedades
         * {@code String}
         * @param values arreglo con los par&aacute;metros a buscar. Este
         * arreglo contiene las propiedades de tipo {@code String}
         * @param namesInt arreglo de string con los nombres de las columnas en
         * las que ser&aacute;n buscados los par&aacute;metros. Este arreglo
         * contiene los nombres de las columnas que almacenen propiedades
         * {@code int}
         * @param valuesInt arreglo con los par&aacute;metros a buscar. Este
         * arreglo contiene las propiedades de tipo {@code int}
         * @return el resultado de la busqueda en BD,devuelto en un objeto
         * {@code DataObject}
         * @throws IOException si durante la ejecuci&oacute;n ocurre
         * alg&uacute;n problema con la generaci&oacute;n o escritura de la
         * respuesta
         */
        public static DataObject getDataProperty(SWBDataSource ds, String[] namesString, String[] values,
                String[] namesInt, int[] valuesInt) throws IOException {
            DataObject query = new DataObject();
            DataObject data = new DataObject();

            query.put("data", data);
            if (namesString != null) {
                for (int i = 0; i < namesString.length; i++) {
                    data.put(namesString[i], values[i]);
                }
            }
            if (namesInt != null) {
                for (int i = 0; i < namesInt.length; i++) {
                    data.put(namesInt[i], valuesInt[i]);
                }
            }
            DataObject obj = ds.fetch(query);
            return obj;
        }

        /**
         * Guarda las enfermedades que esten asociadas a un Gen en
         * espec&iacute;fico y que no existan en la BD de la aplicaci&oacute;n.
         *
         * @param publications Objeto JSON que define las publicaciones que
         * ser&aacute;n guardadas para la b&uacute;squeda
         * @param idSearch identificador de la b&uacute;squeda que ser&aacute;
         * almacenada.
         * @throws IOException si durante la ejecuci&oacute;n ocurre
         * alg&uacute;n problema con la generaci&oacute;n o escritura de la
         * respuesta
         */
        public static void saveNewArticles(JSONObject publications, String idSearch) throws IOException {
            SWBScriptEngine engine = DataMgr.getUserScriptEngine("/test/NanoSources.js", null, false);
            SWBDataSource ds = engine.getDataSource("Article");
            SWBDataSource dsSearch = engine.getDataSource("Search");
            SWBDataSource dsArtSearch = engine.getDataSource("Art_Search");
            DataObject datObjSearch = dsSearch.fetchObjById(idSearch);

            JSONArray arrOutstanding = publications.getJSONArray("outstanding");
            int countNewArt = 0;
            for (int i = 0; i < arrOutstanding.length(); i++) {
                JSONObject art = arrOutstanding.getJSONObject(i);

                int pmid = 0, pmc = 0;
                if (art.has("pmid")) {
                    pmid = Integer.parseInt(art.getString("pmid"));
                }
                if (art.has("pmc")) {
                    pmc = Integer.parseInt(art.getString("pmc"));
                }
                int ranking = art.has("ranking") ? art.getInt("ranking") : null;
                DataObject obj = new DataObject();
                DataObject dataNewArticle = new DataObject();
                String idArticle = null;
                int rows = 0;
                //Hace una petición a la BD a traves de la propiedad "pmid" del artículo
                if (pmid != 0) {
                    obj = getDataProperty(ds, "pmid", null, pmid);
                    rows = obj.getDataObject("response").getInt("totalRows");
                }
                //Sino trae información la consulta, hace una segunda petición a la BD a traves
                // de la propiedad "pmcid"
                if (rows == 0 && pmc != 0) {
                    obj = getDataProperty(ds, "pmcid", null, pmc);
                    rows = obj.getDataObject("response").getInt("totalRows");
                }
                if (rows == 0) {
                    //Si el articulo no existe, guardar el objeto 
                    dataNewArticle = setPropArticle(ds, art, pmid, pmc);
                    idArticle = dataNewArticle.getDataObject("response").getDataObject("data").getString("_id");
                } else {
                    //si ya existe el articulo, obtiene la información del artículo
                    dataNewArticle = obj;
                    idArticle = dataNewArticle.getDataObject("response").getDataList("data").getDataObject(0).getString("_id");
                }
                //almacena la asociación entre una búsqueda y un artículo
                DataObject newArtSearch = new DataObject();
                newArtSearch.put("search", idSearch);
                newArtSearch.put("article", idArticle);
                newArtSearch.put("ranking", ranking);
                newArtSearch.put("status", 1);
                dsArtSearch.addObj(newArtSearch);
                countNewArt++;
            }
            //asigna el número de artículos nuevos
            datObjSearch.put("notification", countNewArt);
            dsSearch.updateObj(datObjSearch);
        }

        /**
         * Actualiza las enfermedades que esten asociadas a un Gen en
         * espec&iacute;fico y que no existan en la BD de la aplicaci&oacute;n.
         *
         * @param publications Objeto JSON que define las publicaciones que
         * ser&aacute;n actualizadas para la b&uacute;squeda
         * @param idSearch identificador de la b&uacute;squeda que ser&aacute;
         * actualizada.
         * @throws IOException si durante la ejecuci&oacute;n ocurre
         * alg&uacute;n problema con la generaci&oacute;n o escritura de la
         * respuesta
         */
        public static void saveUpdateArticles(JSONObject publications, String idSearch) throws IOException {
            SWBScriptEngine engine = DataMgr.getUserScriptEngine("/test/NanoSources.js", null, false);
            SWBDataSource ds = engine.getDataSource("Article");
            SWBDataSource dsSearch = engine.getDataSource("Search");
            SWBDataSource dsArtSearch = engine.getDataSource("Art_Search");
            DataObject datObjSearch = dsSearch.fetchObjById(idSearch);

            JSONArray arrOutstanding = publications.getJSONArray("outstanding");
            int countNewArt = 0;
            int countRecommended = 0;
            for (int i = 0; i < arrOutstanding.length(); i++) {//
                JSONObject art = arrOutstanding.getJSONObject(i);

                int pmid = 0, pmc = 0;
                if (art.has("pmid")) {
                    pmid = Integer.parseInt(art.getString("pmid"));
                }
                if (art.has("pmc")) {
                    pmc = Integer.parseInt(art.getString("pmc"));
                }
                int ranking = art.has("ranking") ? art.getInt("ranking") : null;
                DataObject obj = new DataObject();
                DataObject dataNewArticle = new DataObject();
                String idArticle = null;
                int status = 0;
                int rows = 0;
                //Hace una petición a la BD a traves de la propiedad "pmid" del artículo
                if (pmid != 0) {
                    obj = getDataProperty(ds, "pmid", null, pmid);
                    rows = obj.getDataObject("response").getInt("totalRows");
                }
                //Sino trae información la consulta, hace una segunda petición a la BD a traves
                // de la propiedad "pmcid"
                if (rows == 0 && pmc != 0) {
                    obj = getDataProperty(ds, "pmcid", null, pmc);
                    rows = obj.getDataObject("response").getInt("totalRows");
                }

                if (rows == 0) {
                    //Si el articulo no existe, guardar el objeto 
                    dataNewArticle = setPropArticle(ds, art, pmid, pmc);
                    idArticle = dataNewArticle.getDataObject("response").getDataObject("data").getString("_id");
                    status = 1;
                    countNewArt++;
                } else {
                    //si ya existe el articulo, obtiene la información del artículo
                    dataNewArticle = obj;
                    idArticle = dataNewArticle.getDataObject("response").getDataList("data").getDataObject(0).getString("_id");

                    //Consulta la tabla de asociación entre articulos y búsquedas y si ya existe la relación, continua con la siguiente enfermedad 
                    DataObject obj1 = getDataProperty(dsArtSearch, "article", idArticle, 0);
                    rows = obj1.getDataObject("response").getInt("totalRows");
                    int datRanking = 0, datStatus = 0;
                    boolean isValid = true;

                    if (rows > 0) {
                        DataList list = obj1.getDataObject("response").getDataList("data");
                        for (int j = 0; j < list.size(); j++) {
                            DataObject articleList = list.getDataObject(j);
                            if (articleList.getString("search") != null && articleList.getString("search").equals(idSearch)) {
                                if (articleList.containsKey("ranking")) {
                                    datRanking = articleList.getInt("ranking");
                                }
                                if (articleList.containsKey("status")) {
                                    datStatus = articleList.getInt("status");
                                }
                                isValid = false;
                                break;
                            }
                        }

                        if (datStatus == 1) {
                            countNewArt++;
                        } else if (datRanking > 5) {
                            countRecommended++;
                        }
                    }
                    //Compara que el articulo y la busqueda sean el mismo registro
                    if (!isValid) {
                        continue;
                    }
                }
                //almacena la asociación entre una búsqueda y un artículo
                DataObject newArtSearch = new DataObject();
                newArtSearch.put("search", idSearch);
                newArtSearch.put("article", idArticle);
                newArtSearch.put("ranking", ranking);
                newArtSearch.put("status", status);
                dsArtSearch.addObj(newArtSearch);

            }
            //asigna el número de artículos nuevos y recomendados
            datObjSearch.put("notification", countNewArt);
            datObjSearch.put("recommended", countRecommended);
            dsSearch.updateObj(datObjSearch);
        }

        /**
         * Se encarga de asignar las propiedades de un art&iacute;culo para
         * agregar un registro en la BD.
         *
         * @param ds DataSource que define los art&iacute;culos en la BD de la
         * aplicaci&oacute;n.
         * @param art Objeto JSON que define las caracter&iacute;sticas de un
         * art&iacute;culo obtenidas desde la BD de NCBI (pubmed y pmc)
         * @param pmid n&uacute;mero de identificador utilizado comunmente en la
         * BD de pubmed
         * @param pmc n&uacute;mero de identificador utilizado comunmente en la
         * BD de pmc
         * @return un DataObject que contiene la informaci&oacute;n del nuevo
         * art&iacute;culo almacenado en la BD de la aplicaci&oacute;n
         * @throws IOException si durante la ejecuci&oacute;n ocurre
         * alg&uacute;n problema con la generaci&oacute;n o escritura de la
         * respuesta
         */
        private static DataObject setPropArticle(SWBDataSource ds, JSONObject art,
                int pmid, int pmc) throws IOException {
            DataObject newArticle = new DataObject();
            DataObject dataNewArticle = new DataObject();
            if (pmid != 0) {
                newArticle.put("pmid", pmid);
            }
            if (pmc != 0) {
                newArticle.put("pmcid", pmc);
            }
            if (art.has("articleTitle")) {
                newArticle.put("title", TEXT.replaceSpecialCharacters((art.getString("articleTitle")), false));
            }
            if (art.has("url")) {
                newArticle.put("link", art.getString("url"));
            }
            if (art.has("reference")) {
                newArticle.put("reference", TEXT.replaceSpecialCharacters((art.getString("reference")), false));
            }
            if (art.has("author")) {
                newArticle.put("autor", art.getString("author"));
            }
            if (art.has("prognosis")) {
                newArticle.put("prognosis", art.getInt("prognosis"));
            }
            if (art.has("prediction")) {
                newArticle.put("prediction", art.getInt("prediction"));
            }
            if (art.has("treatment")) {
                newArticle.put("treatment", art.getInt("treatment"));
            }

            StringBuilder sbf = new StringBuilder();
            JSONArray abstractTxt = art.getJSONArray("abstract");
            for (int j = 0; j < abstractTxt.length(); j++) {
                JSONObject abstTxt = abstractTxt.getJSONObject(j);
                sbf.append(abstTxt.getString("label"));
                sbf.append(abstTxt.getString("text"));
            }
            if (sbf.length() > 0) {
                newArticle.put("abstract", TEXT.replaceSpecialCharacters(sbf.toString(), false));
            }
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String date = sdf.format(new Date());
            newArticle.put("lastUpdate", date);
            dataNewArticle = ds.addObj(newArticle);
            return dataNewArticle;
        }

        /**
         * Agrega las enfermedades asociadas a un Gen en la BD de la
         * aplicaci&oacute;n.
         *
         * @param arrayDiseases JSONArray que contiene la estructura de las
         * enfermedades asociadas al gen.
         * @param idGene simbolo del gen al cual est&aacute;n asociadas las
         * enfermedades.
         * @throws IOException si durante la ejecuci&oacute;n ocurre
         * alg&uacute;n problema con la generaci&oacute;n o escritura de la
         * respuesta
         */
        public static void setNewDisease(JSONArray arrayDiseases, String idGene) throws IOException {
            SWBScriptEngine engine = DataMgr.getUserScriptEngine("/test/NanoSources.js", null, false);
            SWBDataSource ds = engine.getDataSource("CancerType");
            SWBDataSource dsGeneCancer = engine.getDataSource("Gene_Cancer");

            for (int i = 0; i < arrayDiseases.length(); i++) {
                JSONObject obj = arrayDiseases.getJSONObject(i);
                String title = "";
                String definition = "";
                String conceptId = "";
                if (obj.has("title")) {
                    title = obj.getString("title");
                }
                if (obj.has("definition")) {
                    definition = obj.getString("definition");
                }
                if (obj.has("conceptId")) {
                    conceptId = obj.getString("conceptId");
                }

                if (conceptId != null && !conceptId.isEmpty()) {
                    //Consulta que exista la enfermedad en BD.
                    DataObject newDisease = getDataProperty(ds, "conceptId", conceptId, 0);
                    int rows = newDisease.getDataObject("response").getInt("totalRows");
                    String idDisease = null;
                    if (rows == 0) {
                        //Sino existe la enfermedad la agrega a la BD de la aplicaci&oacute;n
                        newDisease = new DataObject();
                        newDisease.put("name", title);
                        newDisease.put("summary", definition);
                        newDisease.put("conceptId", conceptId);
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                        String date = sdf.format(new Date());
                        newDisease.put("lastUpdate", date);
                        newDisease = ds.addObj(newDisease);
                        idDisease = newDisease.getDataObject("response").getDataObject("data").getString("_id");
                    } else {
                        //Si existe devuelve el identificador 
                        idDisease = newDisease.getDataObject("response").getDataList("data").getDataObject(0).getString("_id");
                    }
                    //Asocia la enfermedad al Gen
                    DataObject newGeneCancer = new DataObject();
                    newGeneCancer.put("gene", idGene);
                    newGeneCancer.put("cancer", idDisease);
                    dsGeneCancer.addObj(newGeneCancer);
                }
            }
        }

        /**
         * Actualiza las enfermedades que esten asociadas a un Gen en
         * espec&iacute;fico y que no existan en la BD de la aplicaci&oacute;n.
         *
         * @param arrayDiseases JSONArray que contiene la estructura de las
         * enfermedades asociadas al gen.
         * @param idGene simbolo del gen al cual est&aacute;n asociadas las
         * enfermedades.
         * @throws IOException si durante la ejecuci&oacute;n ocurre
         * alg&uacute;n problema con la generaci&oacute;n o escritura de la
         * respuesta
         */
        public static void setUpdateDisease(JSONArray arrayDiseases, String idGene) throws IOException {
            SWBScriptEngine engine = DataMgr.getUserScriptEngine("/test/NanoSources.js", null, false);
            SWBDataSource ds = engine.getDataSource("CancerType");
            SWBDataSource dsGeneCancer = engine.getDataSource("Gene_Cancer");

            for (int i = 0; i < arrayDiseases.length(); i++) {
                JSONObject obj = arrayDiseases.getJSONObject(i);
                String title = "";
                String definition = "";
                String conceptId = "";
                if (obj.has("title")) {
                    title = obj.getString("title");
                }
                if (obj.has("definition")) {
                    definition = obj.getString("definition");
                }
                if (obj.has("conceptId")) {
                    conceptId = obj.getString("conceptId");
                }

                if (conceptId != null && !conceptId.isEmpty()) {
                    //Consulta que exista la enfermedad en BD.
                    DataObject newDisease = getDataProperty(ds, "conceptId", conceptId, 0);
                    int rows = newDisease.getDataObject("response").getInt("totalRows");
                    String idDisease = null;
                    if (rows == 0) {
                        //Sino existe la enfermedad, la almacena
                        newDisease = new DataObject();
                        newDisease.put("name", title);
                        newDisease.put("summary", definition);
                        newDisease.put("conceptId", conceptId);
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                        String date = sdf.format(new Date());
                        newDisease.put("lastUpdate", date);
                        newDisease = ds.addObj(newDisease);
                        idDisease = newDisease.getDataObject("response").getDataObject("data").getString("_id");
                    } else {
                        //Si la enfermedad existe, obtiene su identificador de la consulta a la tabla de Enfermedades
                        idDisease = newDisease.getDataObject("response").getDataList("data").getDataObject(0).getString("_id");

                        //Consulta la tabla de asociación entre enfermedades y genes y si ya existe la relación, continua con la siguiente enfermedad 
                        String[] propertiesName = {"cancer", "gene"};
                        String[] propertiesValues = {idDisease, idGene};
                        DataObject obj1 = getDataProperty(dsGeneCancer, propertiesName, propertiesValues, null, null);
                        rows = obj1.getDataObject("response").getInt("totalRows");
                        if (rows > 0) {
                            continue;
                        }
                        /*DataObject obj1 = getDataProperty(dsGeneCancer, "cancer", idDisease, 0);
                         rows = obj1.getDataObject("response").getInt("totalRows");
                         boolean isValid = true;

                         if (rows > 0) {
                         DataList list = obj1.getDataObject("response").getDataList("data");
                         for (int j = 0; j < list.size(); j++) {
                         DataObject cancerList = list.getDataObject(j);
                         //Recorre 
                         if (cancerList.getString("gene") != null && cancerList.getString("gene").equals(idGene)) {
                         isValid = false;
                         break;
                         }
                         }
                         }
                         //Compara que el cancer y gen sean el mismo registro
                         if (!isValid) {
                         continue;
                         }*/
                    }
                    //Agrega la relación del gen y cancer la BD
                    DataObject newGeneCancer = new DataObject();
                    newGeneCancer.put("gene", idGene);
                    newGeneCancer.put("cancer", idDisease);
                    dsGeneCancer.addObj(newGeneCancer);
                }
            }
        }
    }

    /**
     * <p>
     * Provee funciones para la manipulaci&oacute;n de cadenas de texto tal como
     * reemplazo.</p>
     */
    public static class TEXT {

        /**
         * Reemplaza caracteres acentuados y espacios en blanco en {@code txt}.
         * Realiza los cambios respetando caracteres en may&uacute;sculas o
         * min&uacute;sculas los caracteres en blanco son reemplazados por
         * guiones bajos, cualquier s&iacute;mbolo diferente a gui&oacute;n bajo
         * es eliminado. <br>
         *
         * @param txt cadena con los caracteres que ser&aacute;n reemplazados
         * @param replaceSpaces indica si los espacios en blanco ser&oacute;n
         * reemplazados
         * @return una cadena sin caracteres especiales
         *
         */
        public static String replaceSpecialCharacters(String txt, boolean replaceSpaces) {
            StringBuffer ret = new StringBuffer();
            String aux = txt;
            //aux = aux.toLowerCase();
            aux = aux.replace('Á', 'A');
            aux = aux.replace('Ä', 'A');
            aux = aux.replace('Å', 'A');
            aux = aux.replace('Â', 'A');
            aux = aux.replace('À', 'A');
            aux = aux.replace('Ã', 'A');

            aux = aux.replace('É', 'E');
            aux = aux.replace('Ê', 'E');
            aux = aux.replace('È', 'E');
            aux = aux.replace('Ë', 'E');

            aux = aux.replace('Í', 'I');
            aux = aux.replace('Î', 'I');
            aux = aux.replace('Ï', 'I');
            aux = aux.replace('Ì', 'I');

            aux = aux.replace('Ó', 'O');
            aux = aux.replace('Ö', 'O');
            aux = aux.replace('Ô', 'O');
            aux = aux.replace('Ò', 'O');
            aux = aux.replace('Õ', 'O');

            aux = aux.replace('Ú', 'U');
            aux = aux.replace('Ü', 'U');
            aux = aux.replace('Û', 'U');
            aux = aux.replace('Ù', 'U');

            aux = aux.replace('Ñ', 'N');

            aux = aux.replace('Ç', 'C');
            aux = aux.replace('Ý', 'Y');

            aux = aux.replace('á', 'a');
            aux = aux.replace('à', 'a');
            aux = aux.replace('ã', 'a');
            aux = aux.replace('â', 'a');
            aux = aux.replace('ä', 'a');
            aux = aux.replace('å', 'a');

            aux = aux.replace('é', 'e');
            aux = aux.replace('è', 'e');
            aux = aux.replace('ê', 'e');
            aux = aux.replace('ë', 'e');

            aux = aux.replace('í', 'i');
            aux = aux.replace('ì', 'i');
            aux = aux.replace('î', 'i');
            aux = aux.replace('ï', 'i');

            aux = aux.replace('ó', 'o');
            aux = aux.replace('ò', 'o');
            aux = aux.replace('ô', 'o');
            aux = aux.replace('ö', 'o');
            aux = aux.replace('õ', 'o');

            aux = aux.replace('ú', 'u');
            aux = aux.replace('ù', 'u');
            aux = aux.replace('ü', 'u');
            aux = aux.replace('û', 'u');

            aux = aux.replace('ñ', 'n');

            aux = aux.replace('ç', 'c');
            aux = aux.replace('ÿ', 'y');
            aux = aux.replace('ý', 'y');

            if (replaceSpaces) {
                aux = aux.replace(' ', '_');
            }
            int l = aux.length();
            for (int x = 0; x < l; x++) {
                char ch = aux.charAt(x);
                if ((ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'z')
                        || (ch >= 'A' && ch <= 'Z') || ch == '_' || ch == '-') {
                    ret.append(ch);
                }
            }
            aux = ret.toString();
            return aux;
        }
    }
}
