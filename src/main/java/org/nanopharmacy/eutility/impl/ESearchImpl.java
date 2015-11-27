package org.nanopharmacy.eutility.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.nanopharmacy.utils.Utils;
import static org.nanopharmacy.utils.Utils.XML.getXML;

/**
 * ESearch (text searches). Entrez es un sistema de consulta y recuperación
 * desarrollado por el National Center for Biotechnology Information (NCBI).
 * Puede utilizar Entrez para acceder a varias bases de datos enlazadas que el
 * NCBI hospeda.
 *
 * @see eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi Responds to a text
 * query with the list of matching UIDs in a given database (for later use in
 * ESummary, EFetch or ELink), along with the term translations of the query.
 *
 * @author carlos.ramos
 * @version 07/10/2015
 * @see
 * <a href="http://www.ncbi.nlm.nih.gov/Class/MLACourse/Original8Hour/Entrez/">Entrez
 * NCBI</a>
 */
public class ESearchImpl {

    //private static final Logger log = SWBUtils.getLogger(ESearchImpl.class);

    public final Pattern prognosisPtrn = Pattern.compile(".*prognosis.*", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    public final Pattern treatmentPtrn = Pattern.compile(".*treatment.*", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    public final Pattern predictPtrn = Pattern.compile(".*predict.*", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    public static final String Db_GENE = "gene";     // Base de datos hospedada por NCBI
    public static final String Db_MEDGEN = "medgen"; // Base de datos hospedada por NCBI
    public static final String Db_GTR = "gtr";       // Base de datos hospedada por NCBI
    public static final String Db_PUBMED = "pubmed"; // Base de datos hospedada por NCBI
    public static final String Db_PMC = "pmc";       // Base de datos hospedada por NCBI

    public static final String Elem_SrhRES = "eSearchResult"; // Elemento en el DTD de E-utility
    public static final String Elem_COUNT = "Count";         // Elemento en el DTD de E-utility
    public static final String Elem_QryKEY = "QueryKey";      // Elemento en el DTD de E-utility
    public static final String Elem_WebENV = "WebEnv";        // Elemento en el DTD de E-utility
    public static final String Elem_DocSummary = "DocumentSummary";  // Elemento en el DTD de E-utility
    public static final String Elem_NAME = "Name";            // Elemento en el DTD de E-utility 
    public static final String Elem_ORGANISM = "Organism";    // Elemento en el DTD de E-utility 
    public static final String Elem_SciNAME = "ScientificName";  // Elemento en el DTD de E-utility
    public static final String Attr_UID = "uid";                 // Atributo en el DTD de E-utility
    public static final String Val_HomoSapiens = "Homo sapiens"; // Valor en el DTD de E-utility

    public static final int OFF_SET = 10000;
    public static final String RET_MAX = "10000";         // Numero maximo de registros en el query de esearch
    public static final String Token_RetMax = "@rtmx_";   // numero maximo de registros a incluir en la descarga
    public static final String Token_RetStart = "@strt_"; // indice de registro en que debe iniciar la descarga de informacion
    public static final String Token_DbNAME = "@db_";     // nombre de la base de datos en la que se realiza la busqueda/extraccion
    public static final String Token_GENE = "@gene_";     // Mock up para el nombre del gen en el query de esearch
    public static final String TOKEN_ALTMOL = "@altMol_";  // valor a sustituir por el simbolo de la alteracion molecular y sus alias
    public static final String Token_QryKEY = "@qrykey_"; // valor del parametro query_key generado por una busqueda previa
    public static final String Token_WebENV = "@webenv_"; // valor del parametro WebEnv generado por una busqueda previa
    
    public static final String ERROR_GENE_INFO_NOT_FOUND = "NO_GENE_INFO_FOUND";     // Base de datos hospedada por NCBI
    
    /** año de inicio de periodo de busqueda */
    public static final String Token_LY = "@ly_";
    
    /** mes de inicio de periodo de busqueda */
    public static final String Token_LM = "@lm_";
    
    /** dia de inicio de periodo de busqueda */
    public static final String Token_LD = "@ld_";
    
    /** año de termino de periodo de busqueda */
    public static final String Token_UY = "@uy_";
    
    /** mes de termino de periodo de busqueda */
    public static final String Token_UM = "@um_";
    
    /** dia de termino de periodo de busqueda */
    public static final String Token_UD = "@ud_";

    /**
     * Indica la URL para las busquedas de informaci&oacute;n de genes
     */
    public static final String CMD_ESearch = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=" +
            Db_GENE + "&term=((" + Token_GENE +
            "%5BGene%20Name%5D)%20AND%20homo%20sapiens%5BOrganism%5D)%20AND%20alive%5Bprop%5D&usehistory=y&retmode=xml";
    
    /**
     * Indica la URL para la peticion de la informacion coincidente con la busqueda de informacion de genes
     */
    public static final String CMD_ESummary = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=" +
            Token_DbNAME + "&query_key=" + Token_QryKEY + "&WebEnv=" + Token_WebENV + "&retmode=xml";
    
    /**
     * Indica la URL para las busquedas de informaci&oacute;n de enfermedades relacionadas a un gen
     */
    public static final String CMD_ESearchD = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=" +
            Token_DbNAME + "&term=(" + Token_GENE +
            ")%20AND%20%22diseases%22%5BFilter%5D&usehistory=y&retmode=xml&retmax=" + RET_MAX;
    
    public static final String CMD_ESummaryD = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=" +
            Token_DbNAME + "&query_key=" + Token_QryKEY + "&WebEnv=" + Token_WebENV + "&retmode=xml";

    public static final String CMD_ESearchL = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=" +
            Token_DbNAME + "&term=" + Token_GENE + "&usehistory=y&retmode=xml&retmax=" + RET_MAX;
    
    public static final String CMD_ESummaryL = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=" +
            Token_DbNAME + "&query_key=" + Token_QryKEY + "&WebEnv=" + Token_WebENV + "&retmode=xml";

    /**
     * Indica la URL para las busquedas de informaci&oacute;n de articulos en las bases de datos PubMed y PubMed Central
     */
    public static final String CMD_ESearchP = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=" +
            Token_DbNAME + "&term=" + "(%22" + Token_LY + "%2F" + Token_LM + "%2F" + Token_LD + "%22%5BPDat%5D%20:%20%22" +
    Token_UY + "%2F" + Token_UM + "%2F" + Token_UD + "%22%5BPDat%5D%20AND%20%22humans%22%5BMeSH%20Terms%5D%20AND%20%22has%20abstract%22%5BFilter%5D)%20AND%20(%22" +
    Token_GENE + "%22%5BAll%20Fields%5D%20" + TOKEN_ALTMOL + ")&usehistory=y&retmode=xml";
    //+ Token_GENE + "%20%5BAll%20Fields%5D%20AND%20(%22" + Token_LY + "%2F" + 
//            Token_LM + "%2F" + Token_LD + "%22%5BPDat%5D%20:%20%22" + Token_UY + "%2F" + Token_UM + "%2F" +
//            Token_UD + "%22%5BPDat%5D%20AND%20%22humans%22%5BMeSH%20Terms%5D)&usehistory=y&retmode=xml";
    //("2010/10/08"[PDat] : "2015/10/06"[PDat] AND "humans"[MeSH Terms] AND "has abstract"[Filter]) AND ("BRCA1"[All Fields] OR "c.3548A>G"[All Fields] OR "3548A>G"[All Fields] OR "K1183R"[All Fields])
    
    public static final String CMD_EFetch = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=" +
            Token_DbNAME + "&query_key=" + Token_QryKEY + "&WebEnv=" + Token_WebENV + "&retmode=xml&retstart=" +
            Token_RetStart + "&retmax=" + Token_RetMax;
    
    public static final String Url_NBCI = "http://www.ncbi.nlm.nih.gov/";

    public static final String CMD_ESearchGene = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=" +
            Token_DbNAME + "&term=((" + Token_GENE +
            "%5BGene%20Name%5D)%20AND%20homo%20sapiens%5BOrganism%5D)%20AND%20alive%5Bprop%5D&retmode=xml";

    /**
     * Toma la url CMD_ESearchP y remplaza los tokens <code>Token_LY</code>,
     * <code>Token_LM</code>, <code>Token_LD</code>, <code>Token_UY</code>,
     * <code>Token_UM</code> y <code>Token_UD</code> por los valores de año, mes
     * y día entre la fecha del día de hoy y la misma fecha pero
     * <code>ellapsedYears</code> años atrás
     *
     * @param cmd cadena de caracteres en la que se remplazar&aacute;n los par&aacute;metros de consulta.
     * @param ellapsedYears n&uacute;mero de años atr&aacute;s a partir de la fecha actual para realizar una b&uacute;squeda,
     * es excluyente con {@code ellapsedDays}
     * @param ellapsedDays n&uacute;mero de d&iacute;as atr&aacute;s a partir de la fecha actual para realizar una b&uacute;squeda,
     * es excluyente con {@code ellapsedYears}
     * @return Un string que representa <code>CMD_ESearchP</code> con los
     * parámetros del query correspondientes a las fechas de consulta
     */
    private String getEllapsedTimeQuery(String cmd, final int ellapsedYears, final int ellapsedDays) {
        GregorianCalendar tq = new GregorianCalendar();
        cmd = cmd.replaceFirst(Token_UY, Integer.toString(tq.get(Calendar.YEAR)))
                .replaceFirst(Token_UM, Integer.toString(tq.get(Calendar.MONTH) + 1))
                .replaceFirst(Token_UD, Integer.toString(tq.get(Calendar.DATE)));
        if (ellapsedYears > 0) {
            tq.add(Calendar.YEAR, -ellapsedYears);
        } else if (ellapsedDays > 0) {
            tq.add(Calendar.DAY_OF_YEAR, -ellapsedDays);
        }
        cmd = cmd.replaceFirst(Token_LY, Integer.toString(tq.get(Calendar.YEAR)))
                .replaceFirst(Token_LM, Integer.toString(tq.get(Calendar.MONTH) + 1))
                .replaceFirst(Token_LD, Integer.toString(tq.get(Calendar.DATE)));
        return cmd;
    }

    /**
     * Contruye y devuelve un objeto Json que contiene la información básica de
     * un gen.
     *
     * @param geneName s&iacute;mbolo del gen, por ejemplo: "SF3B1".
     * @return un objeto JSON con la informaci&oacute;n b&aacute;sica del {@code geneName} proporcionado.
     * @throws org.nanopharmacy.eutility.impl.NoDataException
     * @throws org.nanopharmacy.eutility.impl.UseHistoryException
     * @throws java.net.MalformedURLException
     * @throws java.net.ProtocolException
     * @throws java.io.IOException
     */
    public JSONObject getGeneInfo(final String geneName)
            throws NoDataException, UseHistoryException, MalformedURLException, ProtocolException, IOException {
        
        JSONObject gene = null;
        Element docSum;
        boolean errorHappened = false;
        try {
            docSum = getGeneDom(geneName);
        } catch (NoDataException nde) {
            if (nde.getMessage().equals(ESearchImpl.ERROR_GENE_INFO_NOT_FOUND)) {
                docSum = null;
                errorHappened = true;
            } else {
                throw nde;
            }
        }
        
        if (docSum != null) {
            try {
                JSONObject geneData = new JSONObject();
                geneData.put("summary", docSum.getChildText("Summary"));
                geneData.put("nomSymbol", docSum.getChildText("NomenclatureSymbol"));
                geneData.put("nomName", docSum.getChildText("NomenclatureName"));
                geneData.put("sciName", docSum.getChild("Organism").getChildText("ScientificName"));
                geneData.put("altNames", docSum.getChildText("OtherAliases"));
                geneData.put("loc", docSum.getChildText("MapLocation"));
                geneData.put("desc", docSum.getChildText("Description"));
                geneData.put("name", docSum.getChildText("Name"));
                geneData.put("id", docSum.getChild("Mim").getChildText("int"));
                try {
                    gene = new JSONObject();
                    gene.put("gene", geneData);
                } catch (JSONException jse) {
                    Logger.getLogger(ESearchImpl.class.getName()).log(Level.SEVERE, null, jse);
                    throw new NoDataException("no se pudo crear el objeto json");
                }
            } catch (JSONException jse) {
                Logger.getLogger(ESearchImpl.class.getName()).log(Level.SEVERE, null, jse);
            }/*finally {
                
             }*/
        } else {
            if (errorHappened) {
                JSONObject errorData = new JSONObject();
                errorData.put("error", ESearchImpl.ERROR_GENE_INFO_NOT_FOUND);
                errorData.put("geneName", geneName);
                gene.put("error", errorData);
            }
        }
        return gene;
    }

    /**
     * Entrega un JDOM Elemento que representa la informaci&oacute;n b&aacute;sica de un gen
     * relacionado a humanos. Esta informaci&oacute;n es obtenida usando el sistema
     * de consultas Entrez de la NCBI.
     * @param geneName s&iacute;mbolo del gen, por ejemplo: {@literal SF3B1}.
     * @return El objeto JSON con la información básica del gen.
     * @throws org.nanopharmacy.eutility.impl.NoDataException
     * @throws org.nanopharmacy.eutility.impl.UseHistoryException
     * @throws java.net.MalformedURLException
     * @throws java.net.ProtocolException
     * @throws java.io.IOException
     */
    public Element getGeneDom(final String geneName) throws NoDataException, UseHistoryException,
            MalformedURLException, ProtocolException, IOException {
        Element res = null;
        Document doc;
        URL cmd;
        String spec = CMD_ESearch.replaceFirst(Token_GENE, geneName);
        HttpURLConnection conex;
        //spec = CMD_ESearch.replaceFirst(Token_DbNAME, Db_GENE);
        //spec = CMD_ESearch.replaceFirst(Token_GENE, geneName);
        cmd = new URL(spec);
        conex = (HttpURLConnection) cmd.openConnection();
        conex.setConnectTimeout(30000);
        conex.setReadTimeout(60000);
        conex.setRequestMethod("GET");
        conex.setDoOutput(true);
        conex.connect();
        try {
            InputStream in = conex.getInputStream();
            doc = getXML(in);
        } catch (JDOMException jde) {
            doc = null;
        } finally {
            conex.disconnect();
        }

        if (doc != null) {
            Element elem;
            XPath lXPath;
            String qryKey, webEnv;
            try {
                lXPath = XPath.newInstance(Elem_SrhRES + "/" + Elem_QryKEY);
                elem = (Element) lXPath.selectSingleNode(doc);
                if (elem == null) {
                    throw new UseHistoryException("no se encontraron los datos para el parametro de consulta: queryKey");
                }
                qryKey = elem.getValue();
                lXPath = XPath.newInstance(Elem_SrhRES + "/" + Elem_WebENV);
                elem = (Element) lXPath.selectSingleNode(doc);
                if (elem == null) {
                    throw new UseHistoryException("no se encontraron los datos para el parametro de consulta: WebEnv");
                }
                webEnv = elem.getValue();
            } catch (JDOMException jde) {
                qryKey = null;
                webEnv = null;
            }

            if (qryKey == null || webEnv == null) {
                throw new UseHistoryException("entrez tal vez no reconocio la consulta, por lo que no devolvio queryKey ni WebEnv");
            }
            spec = CMD_ESummary.replaceFirst(Token_DbNAME, Db_GENE);
            spec = spec.replaceFirst(Token_QryKEY, qryKey);
            spec = spec.replaceFirst(Token_WebENV, webEnv);
            cmd = new URL(spec);
            conex = (HttpURLConnection) cmd.openConnection();
            conex.setConnectTimeout(30000);
            conex.setReadTimeout(60000);
            conex.setRequestMethod("GET");
            conex.setDoOutput(true);
            conex.connect();
            try {
                InputStream in = conex.getInputStream();
                doc = getXML(in);
            } catch (JDOMException jde) {
                doc = null;
            } finally {
                conex.disconnect();
            }

            if (doc != null) {
                try {
                    lXPath = XPath.newInstance("//" + Elem_DocSummary + "[" + Elem_NAME + "=\"" + geneName + "\" and " + Elem_ORGANISM + "/" + Elem_SciNAME + "=\"" + Val_HomoSapiens + "\"]");
                    res = (Element) lXPath.selectSingleNode(doc);
                    if( res==null ) {
                        throw new NoDataException("NO_GENE_INFO_FOUND");
                    }
                } catch (JDOMException jde) {
                    throw new NoDataException("no se encontro un elemento docsummary para el gen con nombre " + geneName + " y organismo Homo sapiens");
                }
            } // if esummary
        } // if esearch
        return res;
    }

    /**
     * Construye y devuelve un arreglo JSON que contiene la informaci&oacute;n sobre
     * enfermedades relacionadas con un gen. Esta informaci&oacute;n es obtenida usando 
     * {@link getDiseasesDom(String)}.
     * @param geneName s&iacute;mbolo del gen, por ejemplo: {@literal SF3B1}.
     * @return un arreglo JSON con la lista de enfermedades relacionadas con el gen especificado.
     * @throws org.nanopharmacy.eutility.impl.NoDataException
     * @throws org.nanopharmacy.eutility.impl.UseHistoryException
     * @throws java.net.MalformedURLException
     * @throws java.net.ProtocolException
     * @throws java.io.IOException
     */
    public JSONArray getDiseasesInfo(final String geneName)
            throws NoDataException, UseHistoryException, MalformedURLException, ProtocolException, IOException {
        
        JSONArray diseases = new JSONArray();
        Element docSumSet;
        docSumSet = getDiseasesDom(geneName);
        if (docSumSet != null) {
            JSONObject alteration;

            List<Element> docSumList = docSumSet.getChildren("DocumentSummary");
            for (Element docSum : docSumList) {
                alteration = new JSONObject();
                try {
                    alteration.put("title", docSum.getChildText("Title"));
                    alteration.put("definition", docSum.getChildText("Definition"));
                    alteration.put("conceptId", docSum.getChildText("ConceptId"));
                    diseases.put(alteration);
                } catch (JSONException jse) {
                    Logger.getLogger(ESearchImpl.class.getName()).log(Level.SEVERE, null, jse);
                }
            }
        }
        return diseases;
    }

    /**
     * Entrega un elemento JDOM que representa la informaci&oacute;n sobre enfermedades
     * relacionadas con un gen. Esta informaci&oacute;n es obtenida desde
     * el sistema de consultas Entrez de la NCBI.
     * @param geneName s&iacute;mbolo del gen, por ejemplo: {@literal SF3B1}.
     * @return Un elemento JDOM con la estructura <em>DocumentSummarySet</em>
     * que contiene la lista de enfermedades relacionadas con el gen proporcionado.
     * @throws org.nanopharmacy.eutility.impl.NoDataException
     * @throws org.nanopharmacy.eutility.impl.UseHistoryException
     * @throws java.net.MalformedURLException
     * @throws java.net.ProtocolException
     * @throws java.io.IOException
     */
    public Element getDiseasesDom(final String geneName)
            throws NoDataException, UseHistoryException, MalformedURLException, ProtocolException, IOException {
        
        Element res = null;
        Document doc;
        URL cmd;
        String spec;
        HttpURLConnection conex;
        spec = CMD_ESearchD.replaceFirst(Token_DbNAME, Db_MEDGEN);
        spec = spec.replaceFirst(Token_GENE, geneName);
        cmd = new URL(spec);
        conex = (HttpURLConnection) cmd.openConnection();
        conex.setConnectTimeout(30000);
        conex.setReadTimeout(60000);
        conex.setRequestMethod("GET");
        conex.setDoOutput(true);
        conex.connect();
        try {
            InputStream in = conex.getInputStream();
            doc = getXML(in);
        } catch (JDOMException jde) {
            doc = null;
        } finally {
            conex.disconnect();
        }

        if (doc != null) {
            Element elem;
            XPath lXPath;
            String qryKey, webEnv;
            try {
                lXPath = XPath.newInstance(Elem_SrhRES + "/" + Elem_QryKEY);
                elem = (Element) lXPath.selectSingleNode(doc);
                if (elem == null) {
                    throw new UseHistoryException("no se encontraron los datos para el parametro de consulta: queryKey=");
                }
                qryKey = elem.getValue();
                lXPath = XPath.newInstance(Elem_SrhRES + "/" + Elem_WebENV);
                elem = (Element) lXPath.selectSingleNode(doc);
                if (elem == null) {
                    throw new UseHistoryException("no se encontraron los datos para el parametro de consulta: WebEnv=");
                }
                webEnv = elem.getValue();
            } catch (JDOMException jde) {
                qryKey = null;
                webEnv = null;
            }
            
            if (qryKey == null || webEnv == null) {
                throw new UseHistoryException("entrez tal vez no reconocio la consulta, por lo que no devolvio queryKey ni WebEnv");
            }
            spec = CMD_ESummaryD.replaceFirst(Token_DbNAME, Db_MEDGEN);
            spec = spec.replaceFirst(Token_QryKEY, qryKey);
            spec = spec.replaceFirst(Token_WebENV, webEnv);
            cmd = new URL(spec);
            conex = (HttpURLConnection) cmd.openConnection();
            conex.setConnectTimeout(30000);
            conex.setReadTimeout(60000);
            conex.setRequestMethod("GET");
            conex.setDoOutput(true);
            conex.connect();
            try {
                InputStream in = conex.getInputStream();
                doc = getXML(in);
            } catch (JDOMException jde) {
                doc = null;
            } finally {
                conex.disconnect();
            }

            if (doc != null) {
                try {
                    lXPath = XPath.newInstance("//DocumentSummarySet");
                    res = (Element) lXPath.selectSingleNode(doc);
                    if (res == null) {
                        throw new NoDataException("no se encontraron elementos DocumentSummary para el gen con nombre " + geneName);
                    }

                } catch (JDOMException jde) {
                    throw new NoDataException("no se encontro un elemento docsummary para el gen con nombre " + geneName + " y organismo Homo sapiens");
                }
            } // if esummary
        } // if esearch
        return res;
    }

    /**
     * Contruye y devuelve un arreglo Json que contiene la información sobre
     * pruebas de laboratorio relacionadas con un gen. Esta información es
     * obtenida usando el método getTestingLabDom(String).
     *
     * @param geneName El parámetro geneName define el nombre del gen, por
     * ejemplo: "SF3B1".
     * @return El arreglo JSON con la lista de pruebas de laboratorio
     * relacionadas con dicho gen.
     * @throws org.nanopharmacy.eutility.impl.NoDataException
     * @throws org.nanopharmacy.eutility.impl.UseHistoryException
     * @throws java.net.MalformedURLException
     * @throws java.net.ProtocolException
     * @throws java.io.IOException
     */
    public JSONArray getTestingLabInfo(final String geneName)
            throws NoDataException, UseHistoryException, MalformedURLException, ProtocolException, IOException {
        List<Element> conditionLst;
        JSONArray dataLst = new JSONArray();
        Element docSumSet;
        docSumSet = getTestingLabDom(geneName);
        if (docSumSet != null) {
            JSONArray conds;
            JSONObject aux;

            List<Element> docSumList = docSumSet.getChildren("DocumentSummary");
            for (Element docSum : docSumList) {
                aux = new JSONObject();
                try {
                    aux.put("testName", docSum.getChildText("TestName"));
                    conditionLst = docSum.getChild("ConditionList").getChildren("Condition");
                    conds = new JSONArray();
                    for (Element cond : conditionLst) {
                        conds.put(cond.getChildText("Name"));
                    }
                    aux.put("ConditionList", conds);
                    dataLst.put(aux);
                } catch (JSONException jse) {
                    Logger.getLogger(ESearchImpl.class.getName()).log(Level.SEVERE, null, jse);
                }

            }
        }
        return dataLst;
    }

    /**
     * Entrega un JDOM Elemento que representa la información sobre pruebas de
     * laboratorio relacionadas con un gen. Esta información es obtenida usando
     * el sistema de consultas Entrez de la NCBI.
     *
     * @param geneName El parámetro geneName define el nombre del gen, por
     * ejemplo: "SF3B1".
     * @return Un elemento JDOM con la estructura <em>DocumentSummarySet</em>
     * que contiene la lista de pruebas de laboratorio relacionadas con el gen
     * en cuestión.
     * @throws org.nanopharmacy.eutility.impl.NoDataException
     * @throws org.nanopharmacy.eutility.impl.UseHistoryException
     * @throws java.net.MalformedURLException
     * @throws java.net.ProtocolException
     * @throws java.io.IOException
     */
    public Element getTestingLabDom(final String geneName)
            throws NoDataException, UseHistoryException, MalformedURLException, ProtocolException, IOException {
        Element res = new Element("DocumentSummarySet");
        Document doc;
        URL cmd;
        String spec;
        HttpURLConnection conex;
        spec = CMD_ESearchL.replaceFirst(Token_DbNAME, Db_GTR);
        spec = spec.replaceFirst(Token_GENE, geneName);
        cmd = new URL(spec);
        conex = (HttpURLConnection) cmd.openConnection();
        conex.setConnectTimeout(30000);
        conex.setReadTimeout(60000);
        conex.setRequestMethod("GET");
        conex.setDoOutput(true);
        conex.connect();
        try {
            InputStream in = conex.getInputStream();
            doc = getXML(in);
        } catch (JDOMException jde) {
            doc = null;
        } finally {
            conex.disconnect();
        }
        if (doc != null) {
            Element elem;
            List<Element> idList = null;
            XPath lXPath;
            String qryKey, webEnv;
            try {
                lXPath = XPath.newInstance(Elem_SrhRES + "/" + Elem_QryKEY);
                elem = (Element) lXPath.selectSingleNode(doc);
                if (elem == null) {
                    throw new UseHistoryException("no se encontraron los datos para el parametro de consulta: queryKey");
                }
                qryKey = elem.getValue();

                lXPath = XPath.newInstance(Elem_SrhRES + "/" + Elem_WebENV);
                elem = (Element) lXPath.selectSingleNode(doc);
                if (elem == null) {
                    throw new UseHistoryException("no se encontraron los datos para el parametro de consulta: WebEnv=");
                }
                webEnv = elem.getValue();

                lXPath = XPath.newInstance("//Id");
                idList = lXPath.selectNodes(doc);
            } catch (JDOMException jde) {
                qryKey = null;
                webEnv = null;
            }
////////////////////////////////////////////////////////////////////////////////            
            if (qryKey == null || webEnv == null) {
                throw new UseHistoryException("entrez tal vez no reconocio la consulta, por lo que no devolvio queryKey ni WebEnv");
            }
            spec = CMD_ESummaryL.replaceFirst(Token_DbNAME, Db_GTR);
            spec = spec.replaceFirst(Token_QryKEY, qryKey);
            spec = spec.replaceFirst(Token_WebENV, webEnv);
            cmd = new URL(spec);
            conex = (HttpURLConnection) cmd.openConnection();
            conex.setConnectTimeout(30000);
            conex.setReadTimeout(60000);
            conex.setRequestMethod("GET");
            conex.setDoOutput(true);
            conex.connect();
            try {
                InputStream in = conex.getInputStream();
                doc = getXML(in);
            } catch (JDOMException jde) {
                doc = null;
            } finally {
                conex.disconnect();
            }
            if (doc != null) {
                try {
                    lXPath = XPath.newInstance("//DocumentSummarySet");
                    elem = (Element) lXPath.selectSingleNode(doc);
                    if (elem == null) {
                        throw new NoDataException("no se encontraron elementos DocumentSummary para el gen con nombre " + geneName);
                    }

                    List<String> ids = getValues(idList);
                    List<Element> docSumList = elem.getChildren("DocumentSummary");
                    for (Element docSum : docSumList) {
                        String uid = docSum.getAttributeValue("uid");
                        if (ids.contains(uid)) {
                            res.addContent((Element) docSum.clone());
                            ids.remove(uid);
                        }
                    }
//XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
//String xmlString = outputter.outputString(res);
//System.out.println("\nres="+xmlString);
                } catch (JDOMException jde) {
                    throw new NoDataException("no se encontro un elemento docsummary para el gen con nombre " + geneName + " y organismo Homo sapiens");
                }
            } // if esummary
        } // if esearch
        return res;
    }

    /**
     * Construye y devuelve un objeto Json que contiene la informaci&oacute;n sobre las
     * publicaciones m&eacute;dicas clasificadas en dos criterios. Las publicaciones
     * que no cumplen con una relevancia mayor a cero y las que tienen cierta
     * relevancia. Para aquellas publicaciones o art&iacute;culos con una relevancia
     * mayor a cero, incluyen las propiedades del
     * identificador del artículo, y la propiedad {@literal ranking} con el valor de la
     * relevancia correspondiente y se encuentran en el arreglo
     * <em>outstanding</em>. Los art&iacute;culos con una relevancia de valor igual a
     * cero, se encuentran en un arreglo de nombre <em>rejected</em> el cual
     * contiene el identificador de cada uno de estos art&iacute;culos.
     *
     * @param geneName s&iacute;mbolo del gen a buscar en los art&iacute;culos, por ejemplo: {@literal SF3B1}
     * @param molecularAlt s&iacute;mbolo de la alteraci&oacute;n molecular relacionada con el gen. Por ejemplo: {@literal Lys700Glu}.
     * @param ellapsedYears n&uacute;mero de años a considerar para la fecha de publicaci&oacute;n de los art&iacute;culos,
     * es excluyente con {@code ellapsedDays}
     * @param ellapsedDays n&uacute;mero de d&iacute;as atr&aacute;s a partir de la fecha actual para realizar una b&uacute;squeda,
     * es excluyente con {@code ellapsedYears}
     * coincidentes. La b&uacute;squeda comprende ese n&uacute;mero de años at&aacute;s a partir del día actual.
     * @return un objeto JSON con la informaci&oacute;n de las publicaciones m&eacute;dicas
     * referentes a los valores de los par&aacute;metros. Este objeto incluye dos
     * arreglos: El arrego "outstanding" contiene las publicaciones provenientes
     * de PubMed y de PMC. El arreglo "rejected" contiene el <code>pmid</code> de
     * las publicaciones rechazadas del repositorio PubMed y <code>pmc</code> de
     * las publicaciones rechazadas desde el repocitorio PMC.
     * @throws org.nanopharmacy.eutility.impl.NoDataException
     * @throws org.nanopharmacy.eutility.impl.UseHistoryException
     * @throws java.net.MalformedURLException
     * @throws java.net.ProtocolException
     * @throws java.io.IOException
     */
    public JSONObject getPublicationsInfo(final String geneName, final String molecularAlt,
            final int ellapsedYears, final int ellapsedDays)
            throws NoDataException, UseHistoryException, MalformedURLException, ProtocolException, IOException {
        
        JSONObject publications = new JSONObject();// publicaciones aceptadas y rechazadas
        JSONArray outstanding = new JSONArray();   // publicaciones aceptadas en el resultado final
        JSONArray rejected = new JSONArray();      // publicaciones rechazadas debido a su ranking menor a 2

        List<Element> pubmedArtList;
        List<String> accepted;
        List<Element> abstractLst;
        Document doc;
        doc = getPublicationsDom(geneName, molecularAlt, ellapsedYears, ellapsedDays);

        XPath lXPath;
        JSONObject article, abstrct;
        JSONArray abstracts;
        String pmid;
        int rank, rankMax;

        try {
            lXPath = XPath.newInstance("//article");
            pubmedArtList = lXPath.selectNodes(doc);
        } catch (JDOMException jde) {
            return null;
        }
        accepted = new ArrayList<>(pubmedArtList.size());
        for (Element pubmedArt : pubmedArtList) {
            pmid = pubmedArt.getChildText("pmid");
            if (accepted.contains(pmid)) {
                System.out.println("pmid repetido: " + pmid);
                continue;
            }
            accepted.add(pmid);
            rankMax = 0;
            abstractLst = pubmedArt.getChildren("abstract");
            try {
                abstracts = new JSONArray();
                for (Element abs : abstractLst) {
                    if (abs.getChildText("rank") == null) {
                        continue;
                    }
                    abstrct = new JSONObject();
                    abstrct.put("label", abs.getChildText("label"));
                    abstrct.put("text", abs.getChildText("text"));

                    abstrct.put("prognosis", abs.getChildText("prognosis"));
                    abstrct.put("prediction", abs.getChildText("prediction"));
                    abstrct.put("treatment", abs.getChildText("treatment"));

                    rank = Integer.parseInt(abs.getChildText("rank"));
                    abstracts.put(abstrct);
                    if (rank > rankMax) {
                        rankMax = rank;
                    }
                }
                if (rankMax > 0) {
                    article = new JSONObject();
                    article.put("pmid", pmid);
                    if (pubmedArt.getChild("pmc") != null) {
                        article.put("pmc", pubmedArt.getChildText("pmc"));
                    }
                    article.put("ranking", rankMax);
                    article.put("abstract", abstracts);
                    article.put("articleTitle", pubmedArt.getChildText("title"));
                    article.put("url", pubmedArt.getChildText("url"));
                    article.put("author", pubmedArt.getChildText("author"));
                    article.put("reference", pubmedArt.getChildText("reference"));

                    outstanding.put(article);
                } else {
                    article = new JSONObject();
                    if (pubmedArt.getChild("pmc") != null) {
                        article.put("pmc", pubmedArt.getChildText("pmc"));
                    } else {
                        article.put("pmid", pmid);
                    }
                    rejected.put(article);
                }
            } catch (JSONException | NumberFormatException jse) {
                Logger.getLogger(ESearchImpl.class.getName()).log(Level.SEVERE, null, jse);
            }
        } // for
        //}
        System.out.println("total de recuperados = " + pubmedArtList.size());
        System.out.println("total de aceptados (xml) = " + accepted.size());
        System.out.println("total de aceptados (json) = " + outstanding.length());

        try {
            publications.put("outstanding", outstanding);
            publications.put("rejected", rejected);
        } catch (Exception jse) {
            Logger.getLogger(ESearchImpl.class.getName()).log(Level.SEVERE, null, jse);
        }
        return publications;
    }

    /**
     * Entrega un elemento JDOM que representa la informaci&oacute;n sobre
     * publicaciones m&eacute;dicas relacionadas con un gen. Esta informaci&oacute;n es
     * obtenida usando las bases de datos PubMed y PubMed Central del sistema de
     * consultas Entrez de la NCBI.
     *
     * @param geneName s&iacute;mbolo del gen, por ejemplo: {@literal SF3B1}.
     * @param molecularAlt s&iacute;mbolo de la alteraci&oacute;n gen&eacute;tica relacionada con el gen
     * <code>geneName</code>
     * @param ellapsedYears n&uacute;mero de años a considerar para la fecha de publicaci&oacute;n de los art&iacute;culos coicidentes,
     * es excluyente con {@code ellapsedDays}
     * @param ellapsedDays n&uacute;mero de d&iacute;as atr&aacute;s a partir de la fecha actual para realizar una b&uacute;squeda,
     * es excluyente con {@code ellapsedYears}
     * @return Un documento JDOM con la estructura <em>PubmedArticleSet</em> que
     * contiene dos elementos <em>ArticleList</em>, el primero corresponde a los datos extraidos de
     * la base de datos PubMed y el segundo de PMC.
     * @throws org.nanopharmacy.eutility.impl.NoDataException
     * @throws org.nanopharmacy.eutility.impl.UseHistoryException
     * @throws java.net.MalformedURLException
     * @throws java.net.ProtocolException
     * @throws java.io.IOException
     */
    public Document getPublicationsDom(final String geneName, final String molecularAlt,
            int ellapsedYears, final int ellapsedDays)
            throws NoDataException, UseHistoryException, MalformedURLException, ProtocolException, IOException {
        
        Document doc = new Document(new Element("PubmedArticleSet"));
        Element elem;
        elem = getPubMedDom(Db_PUBMED, geneName, molecularAlt, ellapsedYears, ellapsedDays);
        doc.getRootElement().addContent(elem);
        elem = getPMCDom(Db_PMC, geneName, molecularAlt, ellapsedYears, ellapsedDays);
        doc.getRootElement().addContent(elem);
        return doc;
    }

    /**
     * Entrega un JDOM Elemento que representa la información sobre
     * publicaciones médicas relacionadas con un gen. Esta información es
     * obtenida usando la base de datos PubMed (pubmed) del sistema de consultas
     * Entrez de la NCBI.
     *
     * @param dbName Nombre de la base de datos de consulta.
     * @param geneName El parámetro geneName define el nombre del gen, por
     * ejemplo: "SF3B1".
     * @param molecularAlt La alteración genética relacionada con el gen
     * <code>geneName</code>
     * @param ellapsedYears El número de años hacia atrás para realizar la
     * búsqueda. La búsqueda comprende a partir de un día como el actual este
     * valor de años atrás hasta el día actual,
     * es excluyente con {@code ellapsedDays}
     * @param ellapsedDays n&uacute;mero de d&iacute;as atr&aacute;s a partir de la fecha actual para realizar una b&uacute;squeda,
     * es excluyente con {@code ellapsedYears}
     * @return Un elemento JDOM con la estructura <em>ArticleList</em> que
     * contiene la publicaciones médicas relacionadas con el gen en cuestión.
     * @throws org.nanopharmacy.eutility.impl.NoDataException
     * @throws org.nanopharmacy.eutility.impl.UseHistoryException
     * @throws java.net.MalformedURLException
     * @throws java.net.ProtocolException
     * @throws java.io.IOException
     */
    public Element getPubMedDom(final String dbName, final String geneName, final String molecularAlt,
            int ellapsedYears, final int ellapsedDays)
            throws NoDataException, UseHistoryException, MalformedURLException, ProtocolException, IOException {
        
        Element root = new Element("ArticleList");
        Document doc;
        URL cmd;
        HttpURLConnection conex;
        String spec;
        spec = getEllapsedTimeQuery(CMD_ESearchP, ellapsedYears, ellapsedDays);
        spec = spec.replaceFirst(TOKEN_ALTMOL, getQueryValue(molecularAlt));
        spec = spec.replaceFirst(Token_DbNAME, dbName);
        spec = spec.replaceFirst(Token_GENE, geneName);
        System.out.println("\nURL:\n" + spec);
        cmd = new URL(spec);
        conex = (HttpURLConnection) cmd.openConnection();
        conex.setConnectTimeout(30000);
        conex.setReadTimeout(60000);
        conex.setRequestMethod("GET");
        conex.setDoOutput(true);
        conex.connect();
        try {
            InputStream in = conex.getInputStream();
            doc = getXML(in);
        } catch (JDOMException jde) {
            doc = null;
        } finally {
            conex.disconnect();
        }
        if (doc != null) {
            int count = 0;
            Element elem;
            List<Element> nodes;
            XPath lXPath;
            String qryKey, webEnv;

            try {
                lXPath = XPath.newInstance("/" + Elem_SrhRES + "/" + Elem_COUNT);
                elem = (Element) lXPath.selectSingleNode(doc);
                if (elem == null) {
                    throw new UseHistoryException("no se encontraron los datos para el parametro de consulta: Count");
                }
                try {
                    count = Integer.parseInt(elem.getText());
                } catch (NumberFormatException nfe) {
                    throw new NoDataException("el valor de consulta Count es ilegible");
                }

                lXPath = XPath.newInstance(Elem_SrhRES + "/" + Elem_QryKEY);
                elem = (Element) lXPath.selectSingleNode(doc);
                if (elem == null) {
                    throw new UseHistoryException("no se encontraron los datos para el parametro de consulta: queryKey");
                }
                qryKey = elem.getValue();

                lXPath = XPath.newInstance(Elem_SrhRES + "/" + Elem_WebENV);
                elem = (Element) lXPath.selectSingleNode(doc);
                if (elem == null) {
                    throw new UseHistoryException("no se encontraron los datos para el parametro de consulta: WebEnv=");
                }
                webEnv = elem.getValue();

                lXPath = XPath.newInstance("//Id");
                nodes = lXPath.selectNodes(doc);
            } catch (JDOMException jde) {
                qryKey = null;
                webEnv = null;
            }
////////////////////////////////////////////////////////////////////////////////            
            if (qryKey == null || webEnv == null) {
                throw new UseHistoryException("entrez tal vez no reconocio la consulta, por lo que no devolvio queryKey ni WebEnv");
            }
            
            if (count > 0) {
                spec = CMD_EFetch.replaceFirst(Token_DbNAME, Db_PUBMED);
                spec = spec.replaceFirst(Token_QryKEY, qryKey);
                spec = spec.replaceFirst(Token_WebENV, webEnv);
                spec = spec.replaceFirst(Token_RetStart, "0");
                spec = spec.replaceFirst(Token_RetMax, Integer.toString(count));
                cmd = new URL(spec);
                conex = (HttpURLConnection) cmd.openConnection();
                conex.setConnectTimeout(30000);
                conex.setReadTimeout(60000);
                conex.setRequestMethod("GET");
                conex.setDoOutput(true);
                conex.connect();
                try {
                    InputStream in = conex.getInputStream();
                    doc = getXML(in);
                } catch (JDOMException jde) {
                    doc = null;
                } finally {
                    conex.disconnect();
                }
                if (doc != null) {
                    Element art, abs;
                    List<Element> abstractLst;
                    String pmid, author, month, year, value;
                    StringBuilder r;
                    int rank;
                    Matcher m;

                    try {
                        lXPath = XPath.newInstance("//PubmedArticleSet");
                        elem = (Element) lXPath.selectSingleNode(doc);
                        if (elem == null) {
                            throw new NoDataException("no se encontraron elementos DocumentSummary para el gen con nombre " + geneName);
                        }

                        //List<String> ids = getValues(nodes);
                        List<Element> pubmedArtList = elem.getChildren("PubmedArticle");
                        for (Element pubmedArt : pubmedArtList) {
                            String articleTite = null;
                            if (pubmedArt.getChild("MedlineCitation").getChild("Article").getChild("Abstract") == null) {
                                continue;
                            }
                            Document d = new Document((Element) (pubmedArt.clone()));
                            art = new Element("article");

                            abstractLst = pubmedArt.getChild("MedlineCitation").getChild("Article")
                                    .getChild("Abstract").getChildren("AbstractText");
                            for (Element e : abstractLst) {
                                abs = new Element("abstract");

                                elem = new Element("label");
                                elem.setText(e.getAttributeValue("Label") == null ? "Unlabeled" : abs.getAttributeValue("Label"));
                                abs.addContent(elem);

                                elem = new Element("text");
                                value = e.getValue();
                                elem.setText(value);
                                abs.addContent(elem);

                                m = prognosisPtrn.matcher(value);
                                elem = new Element("prognosis");
                                elem.setText(m.matches() ? "1" : "0");
                                abs.addContent(elem);
                                m = treatmentPtrn.matcher(value);
                                elem = new Element("treatment");
                                elem.setText(m.matches() ? "1" : "0");
                                abs.addContent(elem);
                                m = predictPtrn.matcher(value);
                                elem = new Element("prediction");
                                elem.setText(m.matches() ? "1" : "0");
                                abs.addContent(elem);

                                rank = Utils.getRanking(value, geneName, molecularAlt);
                                elem = new Element("rank");
                                elem.setText(Integer.toString(rank));
                                abs.addContent(elem);

                                art.addContent(abs);
                            }

                            elem = new Element("title");
                            articleTite = pubmedArt.getChild("MedlineCitation").getChild("Article").getChildText("ArticleTitle");
                            elem.setText(articleTite);
                            art.addContent(elem);
                            pmid = pubmedArt.getChild("MedlineCitation").getChildText("PMID");
                            elem = new Element("pmid");
                            elem.setText(pmid);
                            art.addContent(elem);
                            elem = new Element("url");
                            elem.setText(Url_NBCI + Db_PUBMED + "/" + pmid);
                            art.addContent(elem);

                            //System.out.println("1.." + pubmedArt.getChild("MedlineCitation").getChild("Article").getChild("AuthorList"));
                            //System.out.println("2..." + pubmedArt.getChild("MedlineCitation").getChild("Article").getChild("AuthorList").getChildren("Author"));
                            r = new StringBuilder();
                            if (pubmedArt.getChild("MedlineCitation").getChild("Article").getChild("AuthorList") != null) {
                                nodes = pubmedArt.getChild("MedlineCitation").getChild("Article").getChild("AuthorList").getChildren("Author");
                                elem = nodes.get(0);

                                author = elem.getChildText("LastName") + ", " + elem.getChildText("Initials");
                                elem = new Element("author");
                                elem.setText(author);
                                art.addContent(elem);


                                for (Element e : nodes) {
                                    r.append(e.getChildText("LastName")).append(", ").append(e.getChildText("Initials")).append("; ");
                                }
                            }
                            //Referencia al articulo
                            elem = pubmedArt.getChild("MedlineCitation").getChild("Article").getChild("Journal").getChild("JournalIssue");
                            r.append("(").append(elem.getChild("PubDate").getChildText("Year")).append("). ");
                            r.append(articleTite);
                            r.append(". ISSN:").append(elem.getParentElement().getChildText("ISSN"));
                            r.append(", vol.").append(elem.getChildText("Volume"));
                            r.append(", issue ").append(elem.getChildText("Issue")).append(". ");
                            r.append(elem.getChild("PubDate").getChildText("Month") == null
                                    ? ""
                                    : " " + elem.getChild("PubDate").getChildText("Month"));
                            r.append(elem.getChild("PubDate").getChildText("Day") == null
                                    ? ""
                                    : " " + elem.getChild("PubDate").getChildText("Day"));
                            r.append(".");
                            elem = new Element("reference");
                            elem.setText(r.toString());
                            art.addContent(elem);

                            root.addContent(art);
                        }
    //XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
    //String xmlString = outputter.outputString(root);
    //System.out.println("\nres="+xmlString);
                    } catch (JDOMException jde) {
                        throw new NoDataException("no se encontro un elemento docsummary para el gen con nombre " + geneName + " y organismo Homo sapiens");
                    }
                } // if esummary
            }// if count > 0
        } // if esearch
        return root;
    }

    /**
     * Entrega un JDOM Elemento que representa la información sobre
     * publicaciones médicas relacionadas con un gen. Esta información es
     * obtenida usando la base de datos PubMed Central (pmc) del sistema de
     * consultas Entrez de la NCBI.
     *
     * @param dbName Nombre de la base de datos de consulta.
     * @param geneName El parámetro geneName define el nombre del gen, por
     * ejemplo: "SF3B1".
     * @param molecularAlt La alteración genética relacionada con el gen
     * <code>geneName</code>
     * @param ellapsedYears El número de años hacia atrás para realizar la
     * búsqueda. La búsqueda comprende a partir de un día como el actual este
     * valor de años atrás hasta el día actual,
     * es excluyente con {@code ellapsedDays}
     * @param ellapsedDays n&uacute;mero de d&iacute;as atr&aacute;s a partir de la fecha actual para realizar una b&uacute;squeda,
     * es excluyente con {@code ellapsedYears}
     * @return Un elemento JDOM con la estructura <em>ArticleList</em> que
     * contiene la publicaciones médicas relacionadas con el gen en cuestión.
     * @throws org.nanopharmacy.eutility.impl.NoDataException
     * @throws org.nanopharmacy.eutility.impl.UseHistoryException
     * @throws java.net.MalformedURLException
     * @throws java.net.ProtocolException
     * @throws java.io.IOException
     */
    public Element getPMCDom(final String dbName, final String geneName, final String molecularAlt,
            int ellapsedYears, final int ellapsedDays)
            throws NoDataException, UseHistoryException, MalformedURLException, ProtocolException, IOException {
        Element root = new Element("ArticleList");
        Document doc;
        URL cmd;
        HttpURLConnection conex;
        String spec;
        spec = getEllapsedTimeQuery(CMD_ESearchP, ellapsedYears, ellapsedDays);
        spec = spec.replaceFirst(TOKEN_ALTMOL, getQueryValue(molecularAlt));
        spec = spec.replaceFirst(Token_DbNAME, dbName);
        spec = spec.replaceFirst(Token_GENE, geneName);
        //System.out.println("\nURL:\n" + spec);
        cmd = new URL(spec);
        conex = (HttpURLConnection) cmd.openConnection();
        conex.setConnectTimeout(30000);
        conex.setReadTimeout(60000);
        conex.setRequestMethod("GET");
        conex.setDoOutput(true);
        conex.connect();
        try {
            InputStream in = conex.getInputStream();
            doc = getXML(in);
        } catch (JDOMException jde) {
            doc = null;
        } finally {
            conex.disconnect();
        }
        if (doc != null) {
            int count = 0;
            Element elem;
            List<Element> nodes = null;
            XPath lXPath;
            String qryKey, webEnv;
            try {
                lXPath = XPath.newInstance("/" + Elem_SrhRES + "/" + Elem_COUNT);
                elem = (Element) lXPath.selectSingleNode(doc);
                if (elem == null) {
                    throw new UseHistoryException("no se encontraron los datos para el parametro de consulta: Count");
                }
                try {
                    count = Integer.parseInt(elem.getText());
                } catch (NumberFormatException nfe) {
                    throw new NoDataException("el valor de consulta Count es ilegible");
                }

                lXPath = XPath.newInstance("/" + Elem_SrhRES + "/" + Elem_QryKEY);
                elem = (Element) lXPath.selectSingleNode(doc);
                if (elem == null) {
                    throw new UseHistoryException("no se encontraron los datos para el parametro de consulta: queryKey");
                }
                qryKey = elem.getValue();

                lXPath = XPath.newInstance("/" + Elem_SrhRES + "/" + Elem_WebENV);
                elem = (Element) lXPath.selectSingleNode(doc);
                if (elem == null) {
                    throw new UseHistoryException("no se encontraron los datos para el parametro de consulta: WebEnv=");
                }
                webEnv = elem.getValue();

                lXPath = XPath.newInstance("//Id");
                nodes = lXPath.selectNodes(doc);
            } catch (JDOMException jde) {
                qryKey = null;
                webEnv = null;
            }
////////////////////////////////////////////////////////////////////////////////            
            if (qryKey == null || webEnv == null) {
                throw new UseHistoryException("entrez tal vez no reconocio la consulta, por lo que no devolvio queryKey ni WebEnv");
            }

            if (count > 0) {
                spec = CMD_EFetch.replaceFirst(Token_DbNAME, Db_PMC);
                spec = spec.replaceFirst(Token_QryKEY, qryKey);
                spec = spec.replaceFirst(Token_WebENV, webEnv);
                spec = spec.replaceFirst(Token_RetStart, "0");
                spec = spec.replaceFirst(Token_RetMax, Integer.toString(count));

                cmd = new URL(spec);
                conex = (HttpURLConnection) cmd.openConnection();
                conex.setConnectTimeout(30000);
                conex.setReadTimeout(60000);
                conex.setRequestMethod("GET");
                conex.setDoOutput(true);
                conex.connect();
                try {
                    InputStream in = conex.getInputStream();
                    doc = getXML(in);
                } catch (JDOMException jde) {
                    doc = null;
                } finally {
                    conex.disconnect();
                }
                if (doc != null) {
                    Element art, abs;
                    String pmc, author, day, month, year, value;
                    StringBuilder r;
                    Document d;
                    int rank;

                    try {
                        lXPath = XPath.newInstance("//pmc-articleset");
                        elem = (Element) lXPath.selectSingleNode(doc);
                        if (elem == null) {
                            throw new NoDataException("no se encontraron elementos DocumentSummary para el gen con nombre " + geneName);
                        }

                        List<Element> pubmedArtList = elem.getChildren("article");
                        for (Element pubmedArt : pubmedArtList) {
                            pubmedArt.removeChild("body");
                            pubmedArt.removeChild("back");
                            elem = pubmedArt.getChild("front").getChild("article-meta");
                            elem.removeChild("history");
                            elem.removeChild("permissions");
                            elem.removeChild("kwd-group");
                            elem.removeChild("custom-meta-group");

                            d = new Document((Element) (pubmedArt.clone()));
                            //String pmid = pubmedArt.getChild("MedlineCitation").getChildText("PMID");
                            //if(ids.contains(pmid)) {
                            art = new Element("article");
                            elem = new Element("title");
                            elem.setText(pubmedArt.getChild("front").getChild("article-meta").getChild("title-group").getChild("article-title").getValue());
                            art.addContent(elem);
                            elem = new Element("pmid");
                            lXPath = XPath.newInstance("//article-id[@pub-id-type=\"pmid\"]");
                            elem.setText(((Element) lXPath.selectSingleNode(d)).getValue());
                            art.addContent(elem);
                            elem = new Element("pmc");
                            lXPath = XPath.newInstance("//article-id[@pub-id-type=\"pmc\"]");
                            pmc = ((Element) lXPath.selectSingleNode(d)).getValue();
                            elem.setText(pmc);
                            art.addContent(elem);
                            elem = new Element("url");
                            elem.setText("http://www.ncbi.nlm.nih.gov/pmc/articles/PMC" + pmc);
                            art.addContent(elem);

                            lXPath = XPath.newInstance("//contrib-group/contrib");
                            nodes = lXPath.selectNodes(d);
                            elem = nodes.get(0);
                            if (elem.getChild("name") != null) {
                                author = elem.getChild("name").getChildText("surname") + ", " + elem.getChild("name").getChildText("given-names");
                                elem = new Element("author");
                                elem.setText(author);
                                art.addContent(elem);
                            }

                            r = new StringBuilder();
                            for (Element e : nodes) {
                                if (e.getChild("name") == null) {
                                    continue;
                                }
                                r.append(e.getChild("name").getChildText("surname")).append(", ").append(e.getChild("name").getChildText("given-names")).append("; ");
                            }

                            lXPath = XPath.newInstance("//pub-date[@pub-type='epub']");
                            elem = (Element) lXPath.selectSingleNode(d);
                            if (elem == null) {
                                lXPath = XPath.newInstance("//pub-date[@pub-type='ppub']");
                                elem = (Element) lXPath.selectSingleNode(d);
                                if (elem == null) {
                                    lXPath = XPath.newInstance("//pub-date[starts-with(@pub-type, 'pmc')]");
                                    elem = (Element) lXPath.selectSingleNode(d);
                                    if (elem == null) {
                                        day = null;
                                        month = null;
                                        year = null;
                                    } else {
                                        day = elem.getChildText("day");
                                        month = elem.getChildText("month");
                                        year = elem.getChildText("year");
                                    }
                                } else {
                                    day = elem.getChildText("day");
                                    month = elem.getChildText("month");
                                    year = elem.getChildText("year");
                                }
                            } else {
                                day = elem.getChildText("day");
                                month = elem.getChildText("month");
                                year = elem.getChildText("year");
                            }
                            if (year != null) {
                                r.append("(").append(year).append("). ");
                            }
                            
                            //título y revista de la referencia
                            r.append(pubmedArt.getChild("front").getChild("article-meta").getChild("title-group").getChild("article-title").getValue());
                            lXPath = XPath.newInstance("//journal-meta/journal-title-group/journal-title");
                            elem = (Element) lXPath.selectSingleNode(d);
                            if (elem != null) {
                                r.append(". ");
                                r.append(elem.getValue());
                            }
                            
                            lXPath = XPath.newInstance("//journal-meta/issn[@pub-type='epub']");
                            elem = (Element) lXPath.selectSingleNode(d);
                            if (elem == null) {
                                lXPath = XPath.newInstance("//journal-meta/issn[@pub-type='ppub']");
                                elem = (Element) lXPath.selectSingleNode(d);
                                if (elem != null) {
                                    r.append(". ISSN:");
                                    r.append(elem.getValue());
                                }
                            } else {
                                r.append(". ISSN:");
                                r.append(elem.getValue());
                            }

                            elem = pubmedArt.getChild("front").getChild("article-meta").getChild("volume");
                            if (elem != null) {
                                r.append(", vol.");
                                r.append(elem.getValue());
                            }

                            elem = pubmedArt.getChild("front").getChild("article-meta").getChild("issue");
                            if (elem != null) {
                                r.append(", issue ");
                                r.append(pubmedArt.getChild("front").getChild("article-meta").getChild("issue").getValue()).append(". ");
                            }

                            if (month != null) {
                                try {
                                    r.append(Utils.Months[Integer.parseInt(month)]);
                                    if (day != null) {
                                        r.append(" ");
                                        r.append(day);
                                        r.append(".");
                                    }
                                } catch (Exception e) {
                                }
                            }
                            elem = new Element("reference");
                            elem.setText(r.toString());
                            art.addContent(elem);

                            lXPath = XPath.newInstance("//abstract/sec");
                            nodes = lXPath.selectNodes(d);
                            if (nodes.isEmpty()) {
                                if (pubmedArt.getChild("front").getChild("article-meta").getChild("abstract") == null) {
                                    continue;
                                } else {
                                    abs = new Element("abstract");

                                    elem = new Element("label");
                                    elem.setText("Unlabeled");
                                    abs.addContent(elem);

                                    elem = new Element("text");
                                    value = pubmedArt.getChild("front").getChild("article-meta").getChild("abstract").getChildText("p");
                                    elem.setText(value);
                                    abs.addContent(elem);

                                    elem = new Element("prognosis");
                                    elem.setText(value.contains("prognosis") ? "1" : "0");
                                    abs.addContent(elem);
                                    elem = new Element("treatment");
                                    elem.setText(value.contains("treatment") ? "1" : "0");
                                    abs.addContent(elem);
                                    elem = new Element("prediction");
                                    elem.setText(value.contains("predict") ? "1" : "0");
                                    abs.addContent(elem);

                                    rank = Utils.getRanking(value, geneName, molecularAlt);
                                    elem = new Element("rank");
                                    elem.setText(Integer.toString(rank));
                                    abs.addContent(elem);

                                    art.addContent(abs);
                                }
                            } else {
                                for (Element e : nodes) {
                                    abs = new Element("abstract");

                                    elem = new Element("label");
                                    elem.setText(e.getChildText("title"));
                                    abs.addContent(elem);

                                    elem = new Element("text");
                                    value = e.getChildText("p");
                                    elem.setText(value);
                                    abs.addContent(elem);

                                    elem = new Element("prognosis");
                                    elem.setText(value.contains("prognosis") ? "1" : "0");
                                    abs.addContent(elem);
                                    elem = new Element("treatment");
                                    elem.setText(value.contains("treatment") ? "1" : "0");
                                    abs.addContent(elem);
                                    elem = new Element("prediction");
                                    elem.setText(value.contains("predict") ? "1" : "0");
                                    abs.addContent(elem);

                                    rank = Utils.getRanking(value, geneName, molecularAlt);
                                    elem = new Element("rank");
                                    elem.setText(Integer.toString(rank));
                                    abs.addContent(elem);

                                    art.addContent(abs);
                                }
                            }
                            root.addContent(art);
                            //res.addContent((Element)pubmedArt.clone());
                            //ids.remove(pmid);
                            //}
                        } // for
    //XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
    //String xmlString = outputter.outputString(root);
    //System.out.println("\n\nres="+xmlString);
                    } catch (JDOMException jde) {
                        throw new NoDataException("no se encontro un elemento docsummary para el gen con nombre " + geneName + " y organismo Homo sapiens");
                    }
                } // if efetch
            } //if count > 0
        } // if esearch
        return root;
    }

    /**
     * Devuelve una lista con los valores contenidos dentro de otra lista de
     * elementos JDOM.
     *
     * @param children El parámetro children define la lista de elementos a los
     * que se les extraerá su valor contenido y se agregará a una nueva lista.
     * @return La lista de valores contenidos en la lista de elementos hijos de
     * alguna jerarquía mayor.
     * @throws org.nanopharmacy.eutility.impl.NoDataException
     * @throws org.nanopharmacy.eutility.impl.UseHistoryException
     * @throws java.net.MalformedURLException
     * @throws java.net.ProtocolException
     * @throws java.io.IOException
     */
    private List<String> getValues(List<Element> children) {
        List values = new ArrayList(children.size());
        for (Element e : children) {
            values.add(e.getValue());
        }
        return values;
    }
    
    /**
     * Valida la existencia de un gen en la BD.
     * @param geneName Simbolo del gen a validar
     * @return true en caso de existir y false caso contrario
     * @throws NoDataException
     * @throws UseHistoryException
     * @throws MalformedURLException
     * @throws ProtocolException
     * @throws IOException 
     */
     public boolean hasGeneBD(final String geneName)
            throws NoDataException, UseHistoryException, MalformedURLException, ProtocolException, IOException {
        boolean isValid = false;
        Document doc;
        URL cmd;
        String spec;
        HttpURLConnection conex;
        spec = CMD_ESearchGene.replaceFirst(Token_DbNAME, Db_GENE);
        spec = spec.replaceFirst(Token_GENE, geneName);
        cmd = new URL(spec);
        conex = (HttpURLConnection) cmd.openConnection();
        conex.setConnectTimeout(30000);
        conex.setReadTimeout(60000);
        conex.setRequestMethod("GET");
        conex.setDoOutput(true);
        conex.connect();
        try {
            InputStream in = conex.getInputStream();
            doc = getXML(in);
        } catch (JDOMException jde) {
            doc = null;
        } finally {
            conex.disconnect();
        }

        if (doc != null) {
            Element elem;
            XPath lXPath;
            String qryKey;
            try {
                lXPath = XPath.newInstance(Elem_SrhRES + "/Count");
                elem = (Element) lXPath.selectSingleNode(doc);
                if (elem != null) {
                    qryKey = elem.getValue();
                    if(Integer.parseInt(qryKey) > 0) {
                        isValid = true;
                    }
                }
            } catch (JDOMException jde) {
            }
        } 
        return isValid;
    }

    /**
     * Genera la cadena correspondiente al criterio de alteracion molecular que se usara
     * en las consultas a las bases de datos PubMed y PubMed Central
     * @param molAlteration el simbolo de la alteracion molecular, junto con sus alias, separados por coma cada uno
     * @return la cadena correspondiente al criterio de alteracion molecular que se usara
     * en las consultas a las bases de datos PubMed y PubMed Central. Si {@code molAlteration} es una cadena nula o vacia
     * se devuelve una cadena vacia.
     */
    private String getQueryValue(String molAlteration) {
        
        StringBuilder criteriaMA = new StringBuilder(64);
        if (molAlteration != null && !molAlteration.isEmpty()) {
            String[] aliases = molAlteration.split(",");

            for (String alias : aliases) {
                criteriaMA.append("%20OR%20%22");
                criteriaMA.append(alias.replaceAll(">", "%3E"));
                criteriaMA.append("%22%5BAll%20Fields%5D");
            }
        }
        return criteriaMA.toString();
    }
}
