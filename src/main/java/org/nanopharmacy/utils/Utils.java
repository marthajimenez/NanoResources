/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.nanopharmacy.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.semanticwb.datamanager.DataMgr;
import org.semanticwb.datamanager.DataObject;
import org.semanticwb.datamanager.SWBDataSource;
import org.semanticwb.datamanager.SWBScriptEngine;
import org.xml.sax.SAXException;

/**
 *
 * @author martha.jimenez
 */
public class Utils {

    /**
     * Representa la definicion del User-Agent a utilizar en algunas peticiones
     */
    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95";

    /**
     *
     */
    public static final String URL_ESEARCH = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi";

    public static final String URL_ESUMMARY = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi";

    /**
     *
     */
    public static final String BD_GENE = "gene";
    
    /**
     * Calcula el valor de relevancia de una publicación médica recuperada con
     * Entrez.
     * @param text el abstract de la publicación. El texto del abstract de la 
     * publicación o artículo.
     * @return Un valor entero indicando la relevancia de la publicación. Un valor
     * 10 indica la mayor relevancia ya que menciona la alteración molecular; 8
     * si el abstract del artículo no menciona la alteración molecular pero se 
     * mencionan más de una vertiente. 6 si solo se menciona una vertiente; 2 si
     * solo se menciona el nombre del gen referido con {@code geneName}; y 0 en 
     * cualquier caso que no cumple con ninguno de los criterios anterires.
     * Una vertiente es cualquiera de las siguientes palabras: prognosis, treatment
     * o cualquier palabra que inicie con predict.
     */    
    public static int getRanking(String text, String geneName, String molecularAlt) {
        int rank = 0;
        if(text==null || geneName==null || molecularAlt==null) {
            return 0;
        }
        
        final String content = text.toLowerCase();
        if(content.contains(geneName)) {
            rank=2;
        }
        if(content.contains("prognosis") || content.contains("treatment") || content.contains("predict")) {
            rank=6;   
        }
        if( content.contains("prognosis")&&content.contains("treatment")
                || content.contains("prognosis")&&content.contains("predict")
                || content.contains("treatment")&&content.contains("predict")
                || content.contains("prognosis")&&content.contains("treatment")&&content.contains("predict")) {
            rank=8;   
        }
        if(content.contains(molecularAlt)) {
            rank = 10;
        }
        
        return rank;
    }

    /**
     *
     */
    public enum BD {

        GENE;

        @Override
        public String toString() {
            if (this == GENE) {
                return "gene";
            }
            return super.toString(); //To change body of generated methods, choose Tools | Templates.
        }
    }

    /**
     *
     */
    public static class XML {

        /**
         *
         * @param gene
         * @return
         */
        public static HashMap<String, String> getParamsGene(String gene) {
            HashMap<String, String> params = new HashMap<>(2);
            String paramGene = "((" + gene
                    + "[Gene Name]) AND homo sapiens[Organism]) AND alive[prop]";
            params.put("db", BD.GENE + "");
            params.put("term", paramGene);
            params.put("usehistory", "y");
            params.put("retmode", "xml");
            return params;
        }

        public static HashMap<String, String> getParamsSummaryGene(String query_key, String webEnv) {
            HashMap<String, String> params = new HashMap<>(2);
            params.put("db", BD.GENE + "");
            params.put("query_key", query_key);
            params.put("WebEnv", webEnv);
            params.put("retmode", "xml");
            return params;
        }

        /**
         *
         * @param is
         * @return
         * @throws ParserConfigurationException
         * @throws IOException
         * @throws SAXException
         */
        public static org.jdom.Document getXML(InputStream is) throws IOException, JDOMException {
            SAXBuilder builder = new SAXBuilder();
            //String data = getStringFromInputStream(is);
            //byte[] data2 = data.getBytes();
            Document document = builder.build(is);
            //Document document = (Document) builder.build(new ByteArrayInputStream(data2));
            return document;
        }
        
        public static String getStringFromInputStream(InputStream is) {
            BufferedReader br = null;
            StringBuilder sb = new StringBuilder();

            String line;
            try {
                br = new BufferedReader(new InputStreamReader(is));
                while((line = br.readLine()) != null) {
                    sb.append(line);
                }
            }catch(IOException e) {
                e.printStackTrace(System.out);
            }finally {
                if(br != null) {
                    try {
                        br.close();
                    }catch(IOException e) {
                        e.printStackTrace(System.out);
                    }
                }
            }
            return sb.toString();
        }

        /**
         *
         * @param params
         * @param url
         * @param userAgent
         * @return
         * @throws UnsupportedEncodingException
         * @throws MalformedURLException
         * @throws IOException
         * @throws org.jdom.JDOMException
         */
        public static Document requestApiXML(HashMap<String, String> params, String url, String userAgent)
                throws UnsupportedEncodingException, MalformedURLException, IOException, JDOMException {
            CharSequence paramString = (null == params) ? "" : delimit(params.entrySet(), "&", "=", true);
            URL serverUrl = new URL(paramString.length() > 0
                    ? (url + "?" + paramString) : url);

            Document dom = null;
            HttpURLConnection conex = null;
            try {
                conex = (HttpURLConnection) serverUrl.openConnection();
                if (userAgent != null) {
                    conex.setRequestProperty("user-agent", userAgent);
                }
                conex.setConnectTimeout(30000);
                conex.setReadTimeout(60000);
                conex.setRequestMethod("GET");
                conex.setDoOutput(true);
                conex.connect();
                try (InputStream in = conex.getInputStream()) {
                    dom = getXML(in);
                }
            } finally {
                if (conex != null) {
                    conex.disconnect();
                }
            }
            return dom;
        }

        /**
         * Lee un flujo de datos y lo convierte en un {@code String} con su
         * contenido codificado en UTF-8
         *
         * @param data el flujo de datos a convertir
         * @return un {@code String} que representa el contenido del flujo de
         * datos especificado, codificado en UTF-8
         * @throws IOException si ocurre un problema en la lectura del flujo de
         * datos
         */
        private static String getResponse(InputStream data) throws IOException {
            StringBuilder response;
            try (Reader in = new BufferedReader(new InputStreamReader(data, "UTF-8"))) {
                response = new StringBuilder(256);
                char[] buffer = new char[1000];
                int charsRead = 0;
                while (charsRead >= 0) {
                    response.append(buffer, 0, charsRead);
                    charsRead = in.read(buffer);
                }
            }
            return response.toString();
        }

        /**
         * En base al contenido de la colecci&oacute;n recibida, arma una
         * secuencia de caracteres compuesta de los pares:
         * <p>
         * {@code Entry.getKey()} {@code equals} {@code Entry.getKey()} </p> Si
         * en la colecci&oacute;n hay m&aacute;s de una entrada, los pares (como
         * el anterior), se separan por {@code delimiter}.
         *
         * @param entries la colecci&oacute;n con la que se van a formar los
         * pares
         * @param delimiter representa el valor con que se van a separar los
         * pares a representar
         * @param equals representa el valor con el que se van a relacionar los
         * elementos de cada par a representar
         * @param doEncode indica si el valor representado en cada par, debe ser
         * codificado (UTF-8) o no
         * @return la secuencia de caracteres que representa el conjunto de
         * pares
         * @throws UnsupportedEncodingException en caso de ocurrir algun
         * problema en la codificaci&oacute;n a UTF-8 del valor de alg&uacute;n
         * par, si as&iacute; se indica en {@code doEncode}
         */
        private static CharSequence delimit(Collection<Map.Entry<String, String>> entries,
                String delimiter, String equals, boolean doEncode)
                throws UnsupportedEncodingException {

            if (entries == null || entries.isEmpty()) {
                return null;
            }
            StringBuilder buffer = new StringBuilder(64);
            boolean notFirst = false;
            for (Map.Entry<String, String> entry : entries) {
                if (notFirst) {
                    buffer.append(delimiter);
                } else {
                    notFirst = true;
                }
                CharSequence value = entry.getValue();
                buffer.append(entry.getKey());
                buffer.append(equals);
                buffer.append(doEncode ? encode(value) : value);
            }
            return buffer;
        }

        /**
         * Codifica el valor de {@code target} de acuerdo al c&oacute;digo de
         * caracteres UTF-8
         *
         * @param target representa el texto a codificar
         * @return un {@code String} que representa el valor de {@code target}
         * de acuerdo al c&oacute;digo de caracteres UTF-8
         * @throws UnsupportedEncodingException en caso de ocurrir algun
         * problema en la codificaci&oacute;n a UTF-8
         */
        private static String encode(CharSequence target) throws UnsupportedEncodingException {

            String result = "";
            if (target != null) {
                result = target.toString();
                result = URLEncoder.encode(result, "UTF8");
            }
            return result;
        }
    }
    
    public static class ENG {
        public static boolean isValidGen(String gen) throws IOException{
            boolean valid = false;
            SWBScriptEngine engine = DataMgr.getUserScriptEngine("/test/NanoSources.js", null, false);
            SWBDataSource ds = engine.getDataSource("Gene");

            DataObject query = new DataObject();
            DataObject data = new DataObject();
            query.put("data", data);
            data.put("symbol", gen);

            DataObject obj = ds.fetch(query);
            int i = obj.getDataObject("response").getInt("totalRows");
            if(i == 0) {
                valid = true;
            }
            return valid;
        }
        
        public static void saveNewArticles(JSONObject publications, String idSearch) throws IOException {
            SWBScriptEngine engine = DataMgr.getUserScriptEngine("/test/NanoSources.js", null, false);
            SWBDataSource ds = engine.getDataSource("Article");
            //SWBDataSource dsSearch = engine.getDataSource("Search");
            SWBDataSource dsArtSearch = engine.getDataSource("Art_Search");

            JSONArray arrOutstanding = publications.getJSONArray("outstanding");
            for (int i = 0; i < arrOutstanding.length()  ; i++) {//
                JSONObject art = arrOutstanding.getJSONObject(i);
                
                int pmid = 0, pmc = 0;
                if(art.has("pmid"))
                    pmid = Integer.parseInt(art.getString("pmid"));
                if(art.has("pmc"))
                    pmc = Integer.parseInt(art.getString("pmc"));
                int ranking = art.has("ranking") ? art.getInt("ranking") : null;
                DataObject query = new DataObject();
                DataObject data = new DataObject();
                DataObject obj = new DataObject();
                DataObject dataNewArticle = new DataObject();
                String idArticle = null;
                int rows = 0;
                if(pmid != 0) {
                    query.put("data", data);
                    data.put("pmid", pmid);
                    obj = ds.fetch(query);
                    rows = obj.getDataObject("response").getInt("totalRows");
                }
                if(rows == 0 && pmc != 0) {
                    query = new DataObject();
                    data = new DataObject();
                    query.put("data", data);
                    data.put("pmcid", pmc);
                    obj = ds.fetch(query);
                    rows = obj.getDataObject("response").getInt("totalRows");
                }
                if (rows == 0) {
                    //Guardar el objeto Article
                    DataObject newArticle = new DataObject();
                    if(pmid != 0) {
                        newArticle.put("pmid", pmid);
                    }
                    if(pmc != 0) {
                        newArticle.put("pmcid", pmc);
                    }
                    if(art.has("articleTitle")) {
                        newArticle.put("title", parseTextJson(art.getString("articleTitle")));
                    }
                    if(art.has("url")) {
                        newArticle.put("link", art.getString("url"));
                    }
                    if(art.has("reference")) {
                        newArticle.put("reference", parseTextJson(art.getString("reference")));
                    }
                    if(art.has("author")) {
                        newArticle.put("autor", art.getString("author"));
                    }
                    if(art.has("prognosis")) {
                        newArticle.put("prognosis", art.getInt("prognosis"));
                    }
                    if(art.has("prediction")) {
                        newArticle.put("prediction", art.getInt("prediction"));
                    }
                    if(art.has("treatment")) {
                        newArticle.put("treatment", art.getInt("treatment"));
                    }

                    StringBuilder sbf = new StringBuilder();
                    JSONArray abstractTxt = art.getJSONArray("abstract");
                    for (int j = 0; j < abstractTxt.length(); j++) {
                        JSONObject abstTxt = abstractTxt.getJSONObject(j);
                        sbf.append(abstTxt.getString("label"));
                        sbf.append(parseTextJson(abstTxt.getString("text")));
                    }
                    if(sbf.length() > 0) {
                        newArticle.put("abstract", parseTextJson(sbf.toString()));
                    }
                    dataNewArticle = ds.addObj(newArticle);
                    idArticle = dataNewArticle.getDataObject("response").getDataObject("data").getString("_id");
                    
                } else {
                    dataNewArticle = obj;
                   idArticle = dataNewArticle.getDataObject("response").getDataList("data").getDataObject(0).getString("_id");
                   
                }
                DataObject newArtSearch = new DataObject();
                newArtSearch.put("search", idSearch);
                newArtSearch.put("article", idArticle);
                newArtSearch.put("ranking", ranking);
                newArtSearch.put("status", 1);
                dsArtSearch.addObj(newArtSearch);
            }

            
        }
    }
    
    private static String parseTextJson(String txt) {
        //if(txt.contains("\"")){
            txt = txt.replaceAll("\"", "&quot;");
            txt = txt.replaceAll('\u0022' +"", "&quot;");
            txt = txt.replaceAll('\u201c' +"", "&quot;");
            txt = txt.replaceAll('\u201d' +"", "&quot;");
            //txt = txt.replaceAll('\u201e' +"", "\\\"");
            txt = txt.replaceAll('\u201f' +"", "&quot;");
            txt = txt.replaceAll('\u275d' +"", "&quot;");
            txt = txt.replaceAll('\u275e' +"", "&quot;");
            txt = txt.replaceAll('\u301d' +"", "&quot;");
            txt = txt.replaceAll('\u301e' +"", "&quot;");
            txt = txt.replaceAll('\uff02' +"", "&quot;");
        //}
        return txt;
    }
}
