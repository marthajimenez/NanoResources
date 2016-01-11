/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.nanopharmacy.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.ProtocolException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.nanopharmacy.eutility.impl.ESearchImpl;
import org.nanopharmacy.eutility.impl.NoDataException;
import org.nanopharmacy.eutility.impl.UseHistoryException;
//import org.nanopharmacy.utils.Utils.BD;
import org.semanticwb.datamanager.DataList;
import org.semanticwb.datamanager.DataMgr;
import org.semanticwb.datamanager.DataObject;
import org.semanticwb.datamanager.SWBDataSource;
import org.semanticwb.datamanager.SWBScriptEngine;
import org.nanopharmacy.util.parser.html.HTMLParser;

/**
 * <p>
 * Clase que contiene utiler&iacute;as para manejo de XML, ENG y Text.</p>
 *
 * @author martha.jimenez
 */
public class Utils {

    private static String contextPath;

    public static String getContextPath() {
        return contextPath;
    }

    public static void setContextPath(String contextPath) {
        Utils.contextPath = contextPath;
    }

    /**
     * Defines the size used for creating arrays that will be used in I/O
     * operations.
     * <p>
     * Define el tama&ntilde;o utilizado en la creaci&oacute;n de arrays que
     * ser&aacute;n utilizados en operaciones de entrada/salida.</p>
     */
    private static int bufferSize = 8192;

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
            return rank;
        }

        final String content = text.toLowerCase();
        boolean containsPrognosis = content.contains("prognosis");
        boolean containsTreatment = content.contains("treatment");
        boolean containsPredict = content.contains("predict");

        if (content.contains(molecularAlt)) {
            rank = 10;
        } else if (containsPrognosis && containsTreatment && containsPredict) {
            rank = 8;
        } else if ((containsPrognosis && containsTreatment) || (containsPrognosis && containsPredict)
                || (containsTreatment && containsPredict)) {
            rank = 6;
        } else if (containsPrognosis || containsTreatment || containsPredict) {
            rank = 2;
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
         * @param is objeto {@code InputStream} que ser&aacute; tranformado a un
         * objeto de tipo {@code Document}
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
            SWBScriptEngine engine = DataMgr.getUserScriptEngine("/public/NanoSources.js", null, false);
            SWBDataSource ds = engine.getDataSource(titleDataSource);

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
            SWBScriptEngine engine = DataMgr.getUserScriptEngine("/public/NanoSources.js", null, false);
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
            if(property!=null){
                data.put(property, valueProp != null ? valueProp : valProp); 
            }
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
         * 
         * @param ds
         * @param namesString
         * @param values
         * @param namesInt
         * @param valuesInt
         * @return
         * @throws IOException 
         */
        
         public static Iterator<DataObject> getDataList(DataObject obj) {
            return obj.getDataObject("response").getDataList("data").iterator();
        }

        /**
         * Guarda las enfermedades que esten asociadas a un Gen en
         * espec&iacute;fico y que no existan en la BD de la aplicaci&oacute;n.
         *
         * @param publications Objeto JSON que define las publicaciones que
         * ser&aacute;n guardadas para la b&uacute;squeda
         * @param idSearch identificador de la b&uacute;squeda que ser&aacute;
         * almacenada.
         * @param countNewArt Numero de articulos nuevos
         * @param countRecommended Numero de articulos recomendados
         * @return String concatenado con el n&uacute;mero de art&iacute;culos
         * nuevos y recomendados
         * @throws IOException si durante la ejecuci&oacute;n ocurre
         * alg&uacute;n problema con la generaci&oacute;n o escritura de la
         * respuesta
         * @throws java.lang.InterruptedException Se lanza cuando un hilo
         * est&aacute; esperando, para dormir, o de lo contrario ocupada, y el
         * hilo se interrumpe, ya sea antes o durante la actividad.
         */
        public static String saveNewArticles(JSONObject publications, String idSearch, int countNewArt, int countRecommended)
                throws IOException, InterruptedException {
            SWBScriptEngine engine = DataMgr.getUserScriptEngine("/public/NanoSources.js", null, false);
            SWBDataSource ds = engine.getDataSource("Article");
            SWBDataSource dsSearch = engine.getDataSource("Search");
            SWBDataSource dsArtSearch = engine.getDataSource("Art_Search");
            DataObject datObjSearch = dsSearch.fetchObjById(idSearch);

            JSONArray arrOutstanding = publications.getJSONArray("outstanding");
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
                //Hace una petición a la BD a traves de la propiedad "pmcid" del artículo
                if (pmc != 0) {
                    obj = getDataProperty(ds, "pmcid", null, pmc);
                    rows = obj.getDataObject("response").getInt("totalRows");
                }
                //Sino trae información la consulta, hace una segunda petición a la BD a traves
                // de la propiedad "pmid" 
                if (rows == 0 && pmid != 0) {
                    obj = getDataProperty(ds, "pmid", null, pmid);
                    rows = obj.getDataObject("response").getInt("totalRows");
                }

                if (rows == 0) {
                    //Si el articulo no existe, guardar el objeto 
                    dataNewArticle = setPropArticle(ds, art, pmid, pmc);
                    idArticle = dataNewArticle.getDataObject("response").getDataObject("data").getString("_id");
                    countNewArt++;
                    if (ranking > 5) {
                        countRecommended++;
                    }
                } else {
                    //si ya existe el articulo, obtiene la información del artículo
                    dataNewArticle = obj;
                    idArticle = dataNewArticle.getDataObject("response").getDataList("data").getDataObject(0).getString("_id");
                    //Aqui inicia temporal en lo que se modifica lo de Jose
                    //Consulta la tabla de asociación entre articulos y búsquedas y si ya existe la relación, continua con el siguiente articulo 
                    String[] propertiesName = {"article", "search"};
                    String[] propertiesValues = {idArticle, idSearch};
                    DataObject obj1 = getDataProperty(dsArtSearch, propertiesName, propertiesValues, null, null);
                    rows = obj1.getDataObject("response").getInt("totalRows");
                    //Reviso si ya existe esta asociacion de busqueda y articulo, salgo
                    if (rows > 0) {
                        continue;
                    } else {
                        //Sino existe asociacion significa que es nuevo para la busqueda
                        countNewArt++;
                        if (ranking > 5) {
                            countRecommended++;
                        }
                    }
                    //Aqui termina en lo que se modifica lo de Jose
/*Remover esto cuando ya funione lo de Jose
                     countNewArt++;
                     if (ranking > 5) {
                     countRecommended++;
                     }*/
                }
                //almacena la asociación entre una búsqueda y un artículo
                DataObject newArtSearch = new DataObject();
                newArtSearch.put("search", idSearch);
                newArtSearch.put("article", idArticle);
                newArtSearch.put("ranking", ranking);
                newArtSearch.put("status", 1);
                dsArtSearch.addObj(newArtSearch);
                // if (ranking > 5) {
                //     countRecommended++;
                // }
                // countNewArt++;
            }
            arrOutstanding = null;
            //asigna el número de artículos nuevos
            datObjSearch.put("notification", countNewArt);
            datObjSearch.put("recommended", countRecommended);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String date = sdf.format(new Date());
            datObjSearch.put("lastUpdate", date);
            datObjSearch.put("created", date);
            datObjSearch.put("monthYearOld", 0);
            dsSearch.updateObj(datObjSearch);
            return countNewArt + "," + countRecommended;
        }

        /**
         * Copia las relaciones de un esquema de busqueda existente a uno nuevo,
         * colocando todos los articulos como nuevos.
         *
         * @param newSearchId Identificador del nuevo esquema de busqueda
         * (Search)
         * @param localSearch Objeto con la informcacion del esquema de busquda
         * a clonar.
         * @return Una cadena con el numero de articulos recomendados y nuevos
         * separados por una coma
         */
        public static String saveLocalNewArticles(String newSearchId, DataObject localSearch, int maxMonth) {
            SWBScriptEngine engine = DataMgr.getUserScriptEngine("/public/NanoSources.js", null, false);
            SWBDataSource dsSearch = engine.getDataSource("Search");
            SWBDataSource dsArtSearch = engine.getDataSource("Art_Search");
            SWBDataSource dsArticle = engine.getDataSource("Article");
            int countNewArt = 0, countRecommended = 0;
            try {
                DataObject obj = getDataProperty(dsArtSearch, "search", localSearch.getString("_id"), 0);
                if (obj != null) {
                    int rows = obj.getDataObject("response").getInt("totalRows");
                    if (rows != 0) {
                        DataList list = obj.getDataObject("response").getDataList("data");
                        int actualMonth = ((Calendar.getInstance().get(Calendar.YEAR)) * 12) + (Calendar.getInstance().get(Calendar.MONTH) + 1);
                        for (int j = 0; j < list.size(); j++) {
                            DataObject artSearch = list.getDataObject(j);
                            DataObject article = dsArticle.fetchObjById(artSearch.getString("article"));
                            if ((actualMonth - (article.getInt("publicationYear") * 12
                                    + article.getInt("publicationMonth"))) <= maxMonth) {
                                artSearch.put("status", 1);
                                artSearch.put("search", newSearchId);
                                countNewArt++;
                                if (Integer.valueOf(artSearch.getString("ranking")) > 5) {
                                    countRecommended++;
                                }
                                artSearch.remove("_id");
                                dsArtSearch.addObj(artSearch);
                            }
                        }
                    }
                }
                DataObject datObjSearch = dsSearch.fetchObjById(newSearchId);
                datObjSearch.put("notification", countNewArt);
                datObjSearch.put("recommended", countRecommended);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                String date = sdf.format(new Date());
                datObjSearch.put("lastUpdate", date);
                dsSearch.updateObj(datObjSearch);

            } catch (IOException ex) {
                Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
            }
            return countNewArt + "," + countRecommended;
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
         * @throws java.lang.InterruptedException Se lanza cuando un hilo
         * est&aacute; esperando, para dormir, o de lo contrario ocupada, y el
         * hilo se interrumpe, ya sea antes o durante la actividad.
         */
        public static void saveUpdateArticles(JSONObject publications, String idSearch) throws IOException, InterruptedException {
            SWBScriptEngine engine = DataMgr.getUserScriptEngine("/public/NanoSources.js", null, false);
            SWBDataSource ds = engine.getDataSource("Article");
            SWBDataSource dsSearch = engine.getDataSource("Search");
            SWBDataSource dsArtSearch = engine.getDataSource("Art_Search");
            DataObject datObjSearch = dsSearch.fetchObjById(idSearch);

            JSONArray arrOutstanding = publications.getJSONArray("outstanding");
            int countNewArt = datObjSearch.get("notification") != null ? datObjSearch.getInt("notification") : 0;
            int countRecommended = datObjSearch.get("recommeded") != null ? datObjSearch.getInt("recommeded") : 0;
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
                String abstractTxt = "";
                int status = 0;
                int rows = 0;
                //Hace una petición a la BD a traves de la propiedad "pmcid" del artículo
                if (pmc != 0) {
                    obj = getDataProperty(ds, "pmcid", null, pmc);
                    rows = obj.getDataObject("response").getInt("totalRows");
                }
                //Sino trae información la consulta, hace una segunda petición a la BD a traves
                // de la propiedad "pmid"    
                if (rows == 0 && pmid != 0) {
                    obj = getDataProperty(ds, "pmid", null, pmid);
                    rows = obj.getDataObject("response").getInt("totalRows");
                }

                if (rows == 0) {
                    //Si el articulo no existe, guardar el objeto 
                    dataNewArticle = setPropArticle(ds, art, pmid, pmc);
                    idArticle = dataNewArticle.getDataObject("response").getDataObject("data").getString("_id");
                    abstractTxt = dataNewArticle.getDataObject("response").getDataObject("data").getString("abstract");
                    status = 1;
                    countNewArt++;
                    if (ranking > 5) {
                        countRecommended++;
                    }
                } else {
                    //si ya existe el articulo, obtiene la información del artículo
                    dataNewArticle = obj;
                    idArticle = dataNewArticle.getDataObject("response").getDataList("data").getDataObject(0).getString("_id");
                    abstractTxt = dataNewArticle.getDataObject("response").getDataList("data").getDataObject(0).getString("abstract");
                    
                    //Consulta la tabla de asociación entre articulos y búsquedas y si ya existe la relación, continua con el siguiente articulo 
                    String[] propertiesName = {"article", "search"};
                    String[] propertiesValues = {idArticle, idSearch};
                    DataObject obj1 = getDataProperty(dsArtSearch, propertiesName, propertiesValues, null, null);
                    rows = obj1.getDataObject("response").getInt("totalRows");
                    //Reviso si ya existe esta asociacion de busqueda y articulo, salgo
                    if (rows > 0) {
                        continue;
                    } else {
                        //Sino existe asociacion significa que es nuevo para la busqueda
                        countNewArt++;
                        if (ranking > 5) {
                            countRecommended++;
                        }
                    }
                    
                }
                art = null;
                
                //almacena la asociación entre una búsqueda y un artículo
                DataObject newArtSearch = new DataObject();
                newArtSearch.put("search", idSearch);
                newArtSearch.put("article", idArticle);
                newArtSearch.put("ranking", ranking);
                newArtSearch.put("status", status);
                dsArtSearch.addObj(newArtSearch);

            }
            arrOutstanding = null;
            //asigna el número de artículos nuevos y recomendados
            datObjSearch.put("notification", countNewArt);
            datObjSearch.put("recommended", countRecommended);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String date = sdf.format(new Date());
            datObjSearch.put("lastUpdate", date);
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
         * @throws java.lang.InterruptedException Se lanza cuando un hilo
         * est&aacute; esperando, para dormir, o de lo contrario ocupada, y el
         * hilo se interrumpe, ya sea antes o durante la actividad.
         * @throws IOException si durante la ejecuci&oacute;n ocurre
         * alg&uacute;n problema con la generaci&oacute;n o escritura de la
         * respuesta
         */
        private static DataObject setPropArticle(SWBDataSource ds, JSONObject art, int pmid, int pmc)
                throws IOException, InterruptedException {
            DataObject newArticle = new DataObject();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

            if (pmid != 0) {
                newArticle.put("pmid", pmid);
            }
            if (pmc != 0) {
                newArticle.put("pmcid", pmc);
            }
            if (art.has("articleTitle")) {
                newArticle.put("title", TEXT.parseTextJson(TEXT.parseHTML((art.getString("articleTitle")))));
                newArticle.put("titleSort", TEXT.parseTextJson(TEXT.parseHTML((art.getString("articleTitle")))).toLowerCase());
            }
            if (art.has("url")) {
                newArticle.put("link", art.getString("url"));
            }
            if (art.has("reference")) {
                newArticle.put("reference", TEXT.parseTextJson(TEXT.parseHTML((art.getString("reference")))));
            }
            if (art.has("author")) {
                newArticle.put("autor", art.getString("author"));
                newArticle.put("autorSort", art.getString("author").toLowerCase());
            }
            newArticle.put("publicationYear", art.has("publicationYear")
                    && !art.getString("publicationYear").equalsIgnoreCase("")
                            ? Integer.parseInt(art.getString("publicationYear"))
                            : Calendar.getInstance().get(Calendar.YEAR));
            int month = 1;
            if (art.has("publicationMonth") && !art.getString("publicationMonth").equalsIgnoreCase("")) {
                try {
                    month = Integer.parseInt(art.getString("publicationMonth"));
                } catch (NumberFormatException ex) {
                }
            }
            newArticle.put("publicationMonth", month);

            StringBuilder sbf = new StringBuilder();
            JSONArray abstractTxt = art.getJSONArray("abstract");
            for (int j = 0; j < abstractTxt.length(); j++) {
                JSONObject abstTxt = abstractTxt.getJSONObject(j);
                if (!abstTxt.getString("label").equalsIgnoreCase("Unlabeled")) {
                    sbf.append("<strong>");
                    sbf.append(TEXT.parseTextJson(TEXT.parseHTML(abstTxt.getString("label"))));
                    sbf.append("</strong><br>");
                }
                sbf.append(TEXT.parseTextJson(TEXT.parseHTML(abstTxt.getString("text"))));
                if ((j + 1) != abstractTxt.length()) {
                    sbf.append("<br>");
                }
                if (abstTxt.has("prognosis")) {
                    newArticle.put("prognosis", abstTxt.getInt("prognosis"));
                }
                if (abstTxt.has("prediction")) {
                    newArticle.put("prediction", abstTxt.getInt("prediction"));
                }
                if (abstTxt.has("treatment")) {
                    newArticle.put("treatment", abstTxt.getInt("treatment"));
                }
            }
            if (sbf.length() > 0) {
                newArticle.put("abstract", sbf.toString());//TEXT.parseTextJson(TEXT.parseHTML(sbf.toString()))
            }
            String date = sdf.format(new Date());
            newArticle.put("lastUpdate", date);
            DataObject dataNewArticle = ds.addObj(newArticle);
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
         * @throws java.lang.InterruptedException Se lanza cuando un hilo
         * est&aacute; esperando, para dormir, o de lo contrario ocupada, y el
         * hilo se interrumpe, ya sea antes o durante la actividad.
         */
        public static void setNewDisease(JSONArray arrayDiseases, String idGene) throws IOException, InterruptedException {
            SWBScriptEngine engine = DataMgr.getUserScriptEngine("/public/NanoSources.js", null, false);
            SWBDataSource ds = engine.getDataSource("CancerType");
            SWBDataSource dsGeneCancer = engine.getDataSource("Gene_Cancer");

            for (int i = 0; i < arrayDiseases.length(); i++) {
                JSONObject obj = arrayDiseases.getJSONObject(i);
                String title = "";
                String definition = "";
                String conceptId = "";
                if (obj.has("title")) {
                    title = TEXT.parseTextJson(TEXT.parseHTML(obj.getString("title")));
                }
                if (obj.has("definition")) {
                    definition = TEXT.parseTextJson(TEXT.parseHTML(obj.getString("definition")));
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
         * @throws java.lang.InterruptedException Se lanza cuando un hilo
         * est&aacute; esperando, para dormir, o de lo contrario ocupada, y el
         * hilo se interrumpe, ya sea antes o durante la actividad.
         */
        public static void setUpdateDisease(JSONArray arrayDiseases, String idGene) throws IOException, InterruptedException {
            SWBScriptEngine engine = DataMgr.getUserScriptEngine("/public/NanoSources.js", null, false);
            SWBDataSource ds = engine.getDataSource("CancerType");
            SWBDataSource dsGeneCancer = engine.getDataSource("Gene_Cancer");

            for (int i = 0; i < arrayDiseases.length(); i++) {
                JSONObject obj = arrayDiseases.getJSONObject(i);
                String title = "";
                String definition = "";
                String conceptId = "";
                if (obj.has("title")) {
                    title = TEXT.parseTextJson(TEXT.parseHTML(obj.getString("title")));
                }
                if (obj.has("definition")) {
                    definition = TEXT.parseTextJson(TEXT.parseHTML(obj.getString("definition")));
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
                    }
                    //Agrega la relación del gen y cancer la BD
                    DataObject newGeneCancer = new DataObject();
                    newGeneCancer.put("gene", idGene);
                    newGeneCancer.put("cancer", idDisease);
                    dsGeneCancer.addObj(newGeneCancer);
                }
            }

        }

        /**
         * Asocia a una b&uacute;squeda, un conjunto de art&iacute;culos
         * extra&iacute;dos de una BD externa. Los art&iacute;culos est&aacute;n
         * relacionados a un gen, una alteraci&oacute;n molecular y un
         * n&uacute;mero de años en espec&iacute;fico
         *
         * @param artYearsOld N&uacute;mero de años que ser&aacute; utilizado
         * para la b&uacute;squeda de art&iacute;culos.
         * @param gene S&iacute;mbolo del gen que ser&aacute; utilizado para la
         * b&uacute;squeda de art&iacute;culos.
         * @param altMolecular Nombre de la alteraci&oacute;n molecular que
         * ser&aacute; utilizada para la b&uacute;squeda de art&iacute;culos.
         * @param id Identificador de la b&uacute;squeda a la que ser&aacute;n
         * asociados los art&iacute;culos.
         * @return Un arreglo JSON con el n&uacute;mero de elementos nuevos y
         * recomendados; o un error si hubo un problema al almacenar la
         * informaci&oacute;n.
         * @throws NoDataException Indica condiciones para las que no se reciben
         * resultados de un repositorio de datos a una BD externa.
         * @throws UseHistoryException Indica condiciones para las no se logran
         * interpretar los valores clave de consulta a BD externa
         * @throws ProtocolException Indica que hay un error en el protocolo
         * subyacente, como un error de TCP.
         * @throws IOException si durante la ejecuci&oacute;n ocurre
         * alg&uacute;n problema con la generaci&oacute;n o escritura de la
         * respuesta
         * @throws java.lang.InterruptedException Se lanza cuando un hilo
         * est&aacute; esperando, para dormir, o de lo contrario ocupada, y el
         * hilo se interrumpe, ya sea antes o durante la actividad.
         */
        public static JSONObject getPublication(int artYearsOld, String gene, String altMolecular, String id, String geneId, String alterationId)
                throws NoDataException, UseHistoryException, ProtocolException, IOException, InterruptedException {
            ESearchImpl esearch = new ESearchImpl();
            JSONObject obj = new JSONObject();
            int monthInc = 6;
            int months = artYearsOld * 12;
            int tmpNotification = 0;
            int tmpRecommended = 0;
            SWBScriptEngine engine = DataMgr.getUserScriptEngine("/public/NanoSources.js", null, false);
            SWBDataSource ds = engine.getDataSource("Search");
            DataObject existSearch = getDataProperty(ds, new String[]{"gene", "altMolecular"}, new String[]{geneId, alterationId}, null, null);
            //System.out.println(existSearch);
            DataList list = existSearch.getDataObject("response").getDataList("data");
            DataObject tmpSearch = null, tmpSearchLow = null, tmpSearchUp = null;
            boolean getExternalPblications = true;
            int yearLow = 0, yearUp = 100, init = 0;
            for (int i = 0; i < list.size(); i++) {
                DataObject search = list.getDataObject(i);
                if (!search.getString("_id").equals(id)) {
                    if (search.getInt("artYearsOld") == artYearsOld) {
                        tmpSearch = search;
                        break;
                    } else if (search.getInt("artYearsOld") < artYearsOld) {
                        if (search.getInt("artYearsOld") > yearLow) {
                            yearLow = search.getInt("artYearsOld");
                            tmpSearchLow = search;
                        }
                    } else if (search.getInt("artYearsOld") > artYearsOld) {
                        if (search.getInt("artYearsOld") < yearUp) {

                            yearUp = search.getInt("artYearsOld");
                            tmpSearchUp = search;
                        }
                    }

                }
            }
            DataObject finalTmpSearch = null;
            if (tmpSearch != null) {
                getExternalPblications = false;
                finalTmpSearch = tmpSearch;
                init = months;
            } else if (tmpSearchUp != null) {
                getExternalPblications = false;
                finalTmpSearch = tmpSearchUp;
                init = months;
            } else if (tmpSearchLow != null) {
                finalTmpSearch = tmpSearchLow;
                init = yearLow * 12;
            }

            if (finalTmpSearch != null) {
                String res = saveLocalNewArticles(id, finalTmpSearch, init);
                String[] temp = res.split(",");
                if (temp.length == 2) {
                    tmpNotification = Integer.parseInt(temp[0]);
                    tmpRecommended = Integer.parseInt(temp[1]);
                }
            }
            if (getExternalPblications) {
                for (int m = init; m < months; m += monthInc) {
                    JSONObject dataArt = esearch.getPublicationsInfo(gene, altMolecular, 0, 0, m, m + monthInc);
                    if (dataArt != null) {

                        if (dataArt.has("error")) {
                            engine = DataMgr.getUserScriptEngine("/public/NanoSources.js", null, false);
                            ds.removeObjById(id);
                            obj = dataArt;
                        } else if (dataArt.has("outstanding")) {
                            String res = saveNewArticles(dataArt, id, tmpNotification, tmpRecommended);
                            String[] temp = res.split(",");
                            if (temp.length == 2) {
                                tmpNotification = Integer.parseInt(temp[0]);
                                tmpRecommended = Integer.parseInt(temp[1]);
                            }
                        }
                        dataArt = null;
                    }
                }
            }
            obj.put("recommended", tmpRecommended);
            obj.put("notification", tmpNotification);
            return obj;
        }

        /**
         * Valida que exista un &uacute;nico tipo de alteraci&oacute;n molecular
         * para un gen.
         *
         * @param idGen identificador del gen a validar
         * @param nameAltMol nombre de la alteraci&oacute;n molecular a validar
         * @return {@code boolean} que representa si existe o no el registro con
         * los par&aacute;metros proporcionados
         * @throws IOException si durante la ejecuci&oacute;n ocurre
         * alg&uacute;n problema con la generaci&oacute;n o escritura de la
         * respuesta
         */
        public static boolean isValidAltMol(String idGen, String nameAltMol) throws IOException {
            boolean isValid = false;
            SWBScriptEngine engine = DataMgr.getUserScriptEngine("/public/NanoSources.js", null, false);
            SWBDataSource dsAlterationMolecular = engine.getDataSource("AlterationMolecular");
            String[] propertiesName = {"gene", "name"};
            String[] propertiesValues = {idGen, nameAltMol};
            DataObject obj = getDataProperty(dsAlterationMolecular, propertiesName, propertiesValues, null, null);
            int i = obj.getDataObject("response").getInt("totalRows");
            if (i == 0) {
                isValid = true;
            }
            return isValid;
        }

        /**
         * Valida que exista un &uacute;nico tipo de c&aacute;ncer para un gen.
         *
         * @param idGen identificador del gen a validar
         * @param nameDisease nombre del tipo de c&aacute;ncer a validar
         * @return {@code boolean} que representa si existe o no el registro con
         * los par&aacute;metros proporcionados
         * @throws IOException si durante la ejecuci&oacute;n ocurre
         * alg&uacute;n problema con la generaci&oacute;n o escritura de la
         * respuesta
         */
        public static boolean isValidCancerType(String idGen, String nameDisease) throws IOException {
            boolean isValid = true;
            SWBScriptEngine engine = DataMgr.getUserScriptEngine("/public/NanoSources.js", null, false);
            SWBDataSource dsGeneCancerType = engine.getDataSource("Gene_Cancer");
            String idCancerType = getIdProperty("CancerType", "name", nameDisease);
            if (idCancerType != null) {
                String[] propertiesName = {"gene", "cancer"};
                String[] propertiesValues = {idGen, idCancerType};
                DataObject obj = getDataProperty(dsGeneCancerType, propertiesName, propertiesValues, null, null);
                int i = obj.getDataObject("response").getInt("totalRows");
                if (i != 0) {
                    isValid = false;
                }
            }
            return isValid;
        }

        public static void removeUserData(String userId) {
            try {
                SWBScriptEngine engine = DataMgr.getUserScriptEngine("/public/NanoSources.js", null, false);
                SWBDataSource dataSource;
                DataObject obj;
                dataSource = engine.getDataSource("Search");
                obj = getDataProperty(dataSource, "user", userId, 0);
                if (obj != null) {
                    int rows = obj.getDataObject("response").getInt("totalRows");
                    if (rows != 0) {
                        DataList list = obj.getDataObject("response").getDataList("data");
                        for (int j = 0; j < list.size(); j++) {
                            DataObject schemeList = list.getDataObject(j);
                            removeSchemeData(schemeList.getString("_id"));
                            dataSource.removeObjById(schemeList.getString("_id"));
                        }

                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        public static int removeSchemeData(String schemeId) {
            try {
                SWBScriptEngine engine = DataMgr.getUserScriptEngine("/public/NanoSources.js", null, false);
                SWBDataSource dataSource;
                DataObject obj;
                dataSource = engine.getDataSource("Art_Search");
                obj = getDataProperty(dataSource, "search", schemeId, 0);
                if (obj != null) {
                    int rows = obj.getDataObject("response").getInt("totalRows");
                    if (rows != 0) {
                        DataList list = obj.getDataObject("response").getDataList("data");
                        for (int j = 0; j < list.size(); j++) {
                            DataObject genList = list.getDataObject(j);
                            dataSource.removeObjById(genList.getString("_id"));
                        }

                    }
                }
                dataSource = engine.getDataSource("Report");
                obj = getDataProperty(dataSource, "search", schemeId, 0);
                if (obj != null) {
                    int rows = obj.getDataObject("response").getInt("totalRows");
                    if (rows != 0) {
                        DataList list = obj.getDataObject("response").getDataList("data");
                        for (int j = 0; j < list.size(); j++) {
                            DataObject genList = list.getDataObject(j);
                            dataSource.removeObjById(genList.getString("_id"));
                        }
                    }
                }
                
                dataSource = engine.getDataSource("Analize");
                obj = getDataProperty(dataSource, "search", schemeId, 0);
                if (obj != null) {
                    int rows = obj.getDataObject("response").getInt("totalRows");
                    if (rows != 0) {
                        DataList list = obj.getDataObject("response").getDataList("data");
                        for (int j = 0; j < list.size(); j++) {
                            DataObject genList = list.getDataObject(j);
                            dataSource.removeObjById(genList.getString("_id"));
                        }
                    }
                }

            } catch (IOException ex) {
                ex.printStackTrace();
                Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
            }
            return 0;
        }

        /**
         * Remueve las im&aacute;genes del sistema de archivos.
         * 
         * @param imageId Identificador de la imagen a eliminar.
         */
        public static void removeImages(String imageId) {
            try {
                SWBScriptEngine engine = DataMgr.getUserScriptEngine("/public/NanoSources.js", null, false);
                SWBDataSource dataSource = engine.getDataSource("Images");
                DataObject obj = getDataProperty(dataSource, "_id", imageId, 0);
                if (obj != null) {
                    int rows = obj.getDataObject("response").getInt("totalRows");
                    if (rows != 0) {
                        DataList list = obj.getDataObject("response").getDataList("data");
                        for (int i = 0; i < list.size(); i++) {
                            DataObject imageObj = list.getDataObject(i);
                            if (imageObj.getDataList("src") != null) {
                                for (int j = 0; j < imageObj.getDataList("src").size(); j++) {
                                    DataObject src = imageObj.getDataList("src").getDataObject(j);
                                    if (src.getString("id") != null) {
                                        String fileDir = Utils.getContextPath()+ "/" +  src.getString("id");
                                        File file = new File(fileDir);
                                        if (file.exists()) {
                                            file.delete();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
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
         * Extracts all the text in a HTML document.
         * <p>
         * Extrae todo el texto de un documento HTML.</p>
         *
         * @param txt a string representing the content of a HTML document
         * @return a string representing all the text in the HTML document. un
         * objeto string que representa todo el texto contenido en el documento
         * HTML.
         * @throws java.io.IOException if an I/O error occurs.
         * <p>
         * si ocurre cualquier error de E/S.</p>
         * @throws java.lang.InterruptedException if this execution thread is
         * interrupted by another thread.
         * <p>
         * si este hilo de ejecuci&oacute;n es interrumpido por otro hilo.</p>
         * @throws IOException Signals that an I/O exception has occurred.
         * @throws InterruptedException the interrupted exception
         */
        public static String parseHTML(String txt) throws IOException,
                InterruptedException {

            String ret = null;
            if (txt != null) {
                HTMLParser parser = new HTMLParser(new StringReader(txt));
                ret = parser.getText();
            }
            return ret;
        }

        /**
         * Parsea un texto, cambiando las comillas dobles por el c&oacute;digo
         * equivalente en HTML.
         *
         * @param txt Cadena inicial que contiene comillas dobles
         * @return Cadena modificada con los codigos HTML correspondientes a las
         * dobles comillas.
         */
        private static String parseTextJson(String txt) {
            txt = txt.replaceAll("\"", "&quot;");
            txt = txt.replaceAll('\u0022' + "", "&quot;");
            txt = txt.replaceAll('\u201c' + "", "&quot;");
            txt = txt.replaceAll('\u201d' + "", "&quot;");
            txt = txt.replaceAll('\u201f' + "", "&quot;");
            txt = txt.replaceAll('\u275d' + "", "&quot;");
            txt = txt.replaceAll('\u275e' + "", "&quot;");
            txt = txt.replaceAll('\u301d' + "", "&quot;");
            txt = txt.replaceAll('\u301e' + "", "&quot;");
            txt = txt.replaceAll('\uff02' + "", "&quot;");
            return txt;
        }
    }

    /**
     * Supplies several I/O functions commonly used, involving streams, files,
     * strings and entire file system structures.
     * <p>
     * Provee varias funciones de E/S utilizadas com&uacute;nmente, involucrando
     * flujos, arhcivos, cadenas y estructuras completas del sistema de
     * archivos.</p>
     */
    public static class IO {

        /**
         * Reads a reader and creates a string with that content.
         * <p>
         * Lee un objeto Reader y crea un objeto string con el contenido
         * le&iacute;do.</p>
         *
         * @param in an input stream to read its content
         * @return a string whose content is the same as for the input stream
         * read. un objeto string cuyo contenido es el mismo que el del objeto
         * inputStream le&iacute;do.
         * @throws IOException if the input stream received is {@code null}.
         * <p>
         * Si el objeto inputStream recibido tiene un valor {@code null}.</p>
         */
        public static String readReader(Reader in) throws IOException {
            if (in == null) {
                throw new IOException("Input Stream null");
            }
            StringBuffer buf = new StringBuffer();
            char[] bfile = new char[Utils.bufferSize];
            int x;
            while ((x = in.read(bfile, 0, Utils.bufferSize)) > -1) {
                String aux = new String(bfile, 0, x);
                buf.append(aux);
            }
            in.close();
            return buf.toString();
        }

    }
}
