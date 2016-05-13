package org.nanopharmacy.eutility.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.nanopharmacy.utils.Utils;
import static org.nanopharmacy.utils.Utils.XML.getXML;

/**
 * Realiza peticiones de informacion al sistema Entrez. Entrez es un sistema de consulta y recuperacion
 * desarrollado por el National Center for Biotechnology Information (NCBI).
 * Se puede utilizar Entrez para acceder a varias bases de datos enlazadas que el NCBI hospeda.
 *
 * Hace uso de tres recursos de Entrez: esearch {@see eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi},
 * esummary {@see eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi} y efetch {@see eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi},
 * con el fin de obtener informacion de Genes, tipos de cancer asociados a los genes y artículos de investigacion
 * relacionados a los genes y a las alteraciones moleculares que se especifican en cada peticion realizada. Para 
 * mayor referencia, ver: http://www.ncbi.nlm.nih.gov/Class/MLACourse/Original8Hour/Entrez
 *
 * @version 1.0
 */
public class ESearchImpl {


    /** Definicion del patron de busqueda para la vertiente {@literal Prognosis} */
    private final Pattern prognosisPtrn = Pattern.compile(".*prognosis.*", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    
    /** Definicion del patron de busqueda para la vertiente {@literal Treatment} */
    private final Pattern treatmentPtrn = Pattern.compile(".*treatment.*", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    
    /** Definicion del patron de busqueda para la vertiente {@literal Prediction} */
    private final Pattern predictPtrn = Pattern.compile(".*predict.*", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    
    /** Constante que define el nombre de la base de datos en que se busca informacion de genes */
    private static final String Db_GENE = "gene";
    
    /** Constante que define el nombre de la base de datos en que se busca informacion de tipos de cancer */
    private static final String Db_MEDGEN = "medgen";
    
    /** Constante que define el nombre de la base de datos en que se busca informacion de estudios de laboratorio */
    private static final String Db_GTR = "gtr";
    
    /** Constante que define el nombre de la base de datos en que se busca informacion de articulos de investigacion */
    private static final String Db_PUBMED = "pubmed";
    
    /** Constante que define el nombre de la base de datos en que se busca informacion de articulos de investigacion */
    private static final String Db_PMC = "pmc";
    
    /** Etiqueta principal de XML que define la respuesta a una busqueda en el sistema Entrez */
    private static final String Elem_SrhRES = "eSearchResult";
    
    /** Etiqueta de XML que define el numero de resultados coincidentes con la busqueda realizada */
    private static final String Elem_COUNT = "Count";
    
    /** Etiqueta de XML que define el valor del parametro {@literal query_key} a utilizar 
     * en las peticiones a esummary y a efetch */
    private static final String Elem_QryKEY = "QueryKey";
    
    /** Etiqueta de XML que define el valor del parametro {@literal WebEnv} a utilizar 
     * en las peticiones a esummary y a efetch */
    private static final String Elem_WebENV = "WebEnv";
    
    /** Etiqueta principal de XML de la respuesta que contiene el detalle del gen */
    private static final String Elem_DocSummary = "DocumentSummary";
    
    /** Etiqueta de XML que contiene el nombre del gen */
    private static final String Elem_NAME = "Name";
    
    /** Etiqueta de XML que contiene el nombre del organismo */
    private static final String Elem_ORGANISM = "Organism";
    
    /** Etiqueta de XML que contiene el nombre cientifico de gen */
    private static final String Elem_SciNAME = "ScientificName";
    
    /** Constante que define el tipo de organismo a utilizar en las busquedas */
    private static final String Val_HomoSapiens = "Homo sapiens";
    
    /** Numero maximo de registros en el query de esearch */
    private static final String RET_MAX = "10000";
    
    /** Numero maximo de registros a incluir en la descarga por esummary o efetch */
    private static final String Token_RetMax = "@rtmx_";
    
    /** Indice del registro en que debe iniciar la descarga de informacion */
    private static final String Token_RetStart = "@strt_";
    
    /** Nombre de la base de datos en la que se realiza la busqueda/extraccion */
    private static final String Token_DbNAME = "@db_";
    
    /** Mock up para el nombre del gen en el query de esearch */
    private static final String Token_GENE = "@gene_";
    
    /** Valor a sustituir por el simbolo de la alteracion molecular y sus alias */
    private static final String TOKEN_ALTMOL = "@altMol_";
    
    /** Valor a sustituir por el parametro query_key generado por una busqueda previa */
    private static final String Token_QryKEY = "@qrykey_";
    
    /** Valor a sustituir por el parametro WebEnv generado por una busqueda previa */
    private static final String Token_WebENV = "@webenv_";
    
    /** Valor a sustituir por el año de inicio de periodo de busqueda */
    private static final String Token_LY = "@ly_";
    
    /** Valor a sustituir por el mes de inicio de periodo de busqueda */
    private static final String Token_LM = "@lm_";
    
    /** Valor a sustituir por el dia de inicio de periodo de busqueda */
    private static final String Token_LD = "@ld_";
    
    /** Valor a sustituir por el año de termino de periodo de busqueda */
    private static final String Token_UY = "@uy_";
    
    /** Valor a sustituir por el mes de termino de periodo de busqueda */
    private static final String Token_UM = "@um_";
    
    /** Valor a sustituir por el dia de termino de periodo de busqueda */
    private static final String Token_UD = "@ud_";

    /** Tipo de error utilizado cuando no se encuentra informacion */
    private static final String ERROR_INFO_NOT_FOUND = "NO_INFO_FOUND";
    
    /** Tipo de error utilizado cuando ocurre algun problema de comunicacion */
    private static final String ERROR_IN_COMM = "COMMUNICATION_PROBLEM";
    
    /** Tipo de error utilizado cuando ocurre algun problema no identificado */
    private static final String ERROR_GENERAL = "EXECUTION_ERROR";
    
    /** Indica la URL para las busquedas de informaci&oacute;n de genes     */
    private static final String CMD_ESearch = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=" +
            Db_GENE + "&term=((" + Token_GENE +
            "%5BGene%20Name%5D)%20AND%20homo%20sapiens%5BOrganism%5D)%20AND%20alive%5Bprop%5D&usehistory=y&retmode=xml";
    
    /** Indica la URL para la peticion de la informacion coincidente con la busqueda de informacion de genes */
    private static final String CMD_ESummary = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=" +
            Token_DbNAME + "&query_key=" + Token_QryKEY + "&WebEnv=" + Token_WebENV + "&retmode=xml";
    
    /** Indica la URL para las busquedas de informaci&oacute;n de enfermedades relacionadas a un gen */
    private static final String CMD_ESearchD = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=" +
            Token_DbNAME + "&term=(" + Token_GENE +
            ")%20AND%20%22diseases%22%5BFilter%5D&usehistory=y&retmode=xml&retmax=" + RET_MAX;
    
    /** Indica la URL para la peticion de la informacion coincidente con la busqueda de informacion de tipos de cancer */
    private static final String CMD_ESummaryD = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=" +
            Token_DbNAME + "&query_key=" + Token_QryKEY + "&WebEnv=" + Token_WebENV + "&retmode=xml";
    
    /** Indica la URL para las busquedas de informaci&oacute;n de pruebas de laboratorio */
    private static final String CMD_ESearchL = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=" +
            Token_DbNAME + "&term=" + Token_GENE + "&usehistory=y&retmode=xml&retmax=" + RET_MAX;
    
    /** Indica la URL para la peticion de la informacion coincidente con la busqueda de informacion de pruebas de laboratorio */
    private static final String CMD_ESummaryL = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=" +
            Token_DbNAME + "&query_key=" + Token_QryKEY + "&WebEnv=" + Token_WebENV + "&retmode=xml";
    
    /** Indica la URL para las busquedas de articulos en las bases de datos PubMed y PubMed Central */
    private static final String CMD_ESearchP = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=" +
            Token_DbNAME + "&term=" + "(%22" + Token_LY + "%2F" + Token_LM + "%2F" + Token_LD + "%22%5BPDat%5D%20:%20%22" +
            Token_UY + "%2F" + Token_UM + "%2F" + Token_UD +
            "%22%5BPDat%5D%20AND%20%22humans%22%5BMeSH%20Terms%5D%20AND%20%22has%20abstract%22%5BFilter%5D)%20AND%20(%22" +
            Token_GENE + "%22%5BAll%20Fields%5D%20" + TOKEN_ALTMOL + ")&usehistory=y&retmode=xml";
    
    /** Indica la URL para la peticion de la informacion coincidente con la busqueda de 
     * articulos en las bases de datos PubMed y PubMed Central */
    private static final String CMD_EFetch = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=" +
            Token_DbNAME + "&query_key=" + Token_QryKEY + "&WebEnv=" + Token_WebENV + "&retmode=xml&retstart=" +
            Token_RetStart + "&retmax=" + Token_RetMax;
    
    /** Indica la URL de la pagina de la NCBI */
    private static final String Url_NBCI = "http://www.ncbi.nlm.nih.gov/";
    
    /** Indica la URL para la busqueda de informacion de un gen con el fin de validar el simbolo proporcionado */
    private static final String CMD_ESearchGene = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=" +
            Token_DbNAME + "&term=((" + Token_GENE +
            "%5BGene%20Name%5D)%20AND%20homo%20sapiens%5BOrganism%5D)%20AND%20alive%5Bprop%5D&retmode=xml";
    
    /**
     * Toma la url especificada en {@code cmd} y remplaza los tokens {@link ESearchImpl.Token_LY},
     * {@link ESearchImpl.Token_LM}, {@link ESearchImpl.Token_LD}, {@link ESearchImpl.Token_UY},
     * {@link ESearchImpl.Token_UM} y {@link ESearchImpl.Token_UD} por los valores de año, mes
     * y die;a entre la fecha del dia de hoy y la misma fecha pero
     * {@link ESearchImpl.ellapsedYears} años atras
     * @param cmd cadena de caracteres en la que se remplazaran los parametros de consulta.
     * @param ellapsedYears numero de años atras a partir de la fecha actual para realizar una busqueda,
     * es excluyente con {@code ellapsedDays}
     * @param ellapsedDays numero de dias atras a partir de la fecha actual para realizar una busqueda,
     * es excluyente con {@code ellapsedYears}
     * @param initMonth numero de meses atras, que indica el inicio del periodo de busqueda.
     *        El uso de {@code initMonth} y {@code finalMonth} excluye el uso de los parametros {@code ellapsedYears} y {@code ellapsedDays}
     * @param finalMonth numero de meses atras, que indica el fin del periodo de busqueda.
     *        El uso de {@code initMonth} y {@code finalMonth} excluye el uso de los parametros {@code ellapsedYears} y {@code ellapsedDays}
     * @return Un string que representa {@link ESearchImpl.CMD_ESearchP} con los
     * parametros del query correspondientes a las fechas de consulta
     */
    private String getEllapsedTimeQuery(String cmd, final int ellapsedYears, final int ellapsedDays,
            final int initMonth, final int finalMonth) {
        
        GregorianCalendar tq = new GregorianCalendar();
        if (ellapsedYears > 0 || ellapsedDays > 0) {
            //calcula fechas en base a años o a dias
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
        } else if (initMonth >= 0 && finalMonth > 0) {
            tq.add(Calendar.MONTH, -initMonth);
            cmd = cmd.replaceFirst(Token_UY, Integer.toString(tq.get(Calendar.YEAR)))
                    .replaceFirst(Token_UM, Integer.toString(tq.get(Calendar.MONTH) + 1))
                    .replaceFirst(Token_UD, Integer.toString(tq.get(Calendar.DATE)));
            tq.add(Calendar.MONTH, initMonth);
            tq.add(Calendar.MONTH, -finalMonth);
            cmd = cmd.replaceFirst(Token_LY, Integer.toString(tq.get(Calendar.YEAR)))
                    .replaceFirst(Token_LM, Integer.toString(tq.get(Calendar.MONTH) + 1))
                    .replaceFirst(Token_LD, Integer.toString(tq.get(Calendar.DATE)));
        }
        return cmd;
    }

    /**
     * Construye y devuelve un objeto Json que contiene la informacion basica de
     * un gen.
     * @param geneName simbolo del gen, por ejemplo: "SF3B1".
     * @return un objeto JSON con la informacion basica del {@code geneName} proporcionado.
     * @throws org.nanopharmacy.eutility.impl.NoDataException En caso de no encontrar 
     *     informacion con los criterios especificados
     * @throws org.nanopharmacy.eutility.impl.UseHistoryException Si el parametro WebEnv 
     *     de la peticion no es reconocido por Entrez
     * @throws java.net.ProtocolException Si ocurre un error en el protocolo utilizado 
     *     durante la conexion con Entrez
     * @throws java.io.IOException En caso de que haya problemas con la lectura de la 
     *     respuesta del servidor
     */
    public JSONObject getGeneInfo(final String geneName)
            throws NoDataException, UseHistoryException, ProtocolException, IOException {
        
        JSONObject gene = null;
        Element docSum;
        boolean errorHappened = false;
        try {
            docSum = getGeneDom(geneName);
        } catch (NoDataException nde) {
            if (nde.getMessage().equals(ESearchImpl.ERROR_INFO_NOT_FOUND)) {
                docSum = null;
                errorHappened = true;
            } else {
                Logger.getLogger(ESearchImpl.class.getName()).log(Level.SEVERE, null, nde);
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
                gene = new JSONObject();
                JSONObject errorData = new JSONObject();
                errorData.put("error", ESearchImpl.ERROR_INFO_NOT_FOUND);
                errorData.put("geneName", geneName);
                gene.put("error", errorData);
            }
        }
        return gene;
    }

    /**
     * Entrega un elemento JDOM  que representa la informacion basica de un gen
     * relacionado a humanos. Esta informacion es obtenida usando el sistema
     * de consultas Entrez de la NCBI.
     * @param geneName simbolo del gen, por ejemplo: {@literal SF3B1}.
     * @return El objeto JSON con la informacion basica del gen.
     * @throws org.nanopharmacy.eutility.impl.NoDataException En caso de no encontrar 
     *     informaci&oacute;n con los criterios especificados
     * @throws org.nanopharmacy.eutility.impl.UseHistoryException Si el parametro WebEnv 
     *     de la petici&oacute;n no es reconocido por Entrez
     * @throws java.net.ProtocolException Si ocurre un error en el protocolo utilizado 
     *     durante la conexi&oacute;n con Entrez
     * @throws java.io.IOException En caso de que haya problemas con la lectura de la 
     *     respuesta del servidor
     */
    public Element getGeneDom(final String geneName) throws NoDataException, UseHistoryException,
            ProtocolException, IOException {
        
        Element res = null;
        Document doc;
        String spec = CMD_ESearch.replaceFirst(Token_GENE, geneName);
        doc = this.getExternalData(spec);
        
        if (doc != null) {
            Element elem;
            Element respRoot = doc.getRootElement();
            String qryKey, webEnv;
            elem = respRoot.getChild(ESearchImpl.Elem_QryKEY);
            if (elem == null) {
                throw new UseHistoryException("no se encontro el valor de: queryKey");
            }
            qryKey = elem.getValue();
            elem = respRoot.getChild(ESearchImpl.Elem_WebENV);
            if (elem == null) {
                throw new UseHistoryException("no se encontro el valor de: WebEnv");
            }
            webEnv = elem.getValue();
            doc = null;
            if (qryKey == null || webEnv == null) {
                throw new UseHistoryException("entrez no devolvio queryKey ni WebEnv");
            }
            spec = CMD_ESummary.replaceFirst(Token_DbNAME, Db_GENE);
            spec = spec.replaceFirst(Token_QryKEY, qryKey);
            spec = spec.replaceFirst(Token_WebENV, webEnv);
            doc = this.getExternalData(spec);
            boolean validResponse = false;
            Element summarySet = null;

            if (doc != null) {
                respRoot = doc.getRootElement();
                summarySet = respRoot.getChild("DocumentSummarySet");
                if (summarySet != null) {
                    validResponse = true;
                }
            } // if esummary
            if (validResponse) {
                List<Element> summaries = summarySet.getChildren(ESearchImpl.Elem_DocSummary);
                for (Element summary : summaries) {
                    if (summary.getChildText(ESearchImpl.Elem_NAME).equalsIgnoreCase(geneName) &&
                            summary.getChild(ESearchImpl.Elem_ORGANISM).getChildText(ESearchImpl.Elem_SciNAME).
                                    equalsIgnoreCase(ESearchImpl.Val_HomoSapiens)) {
                        res = summary;
                    }
                }
            } else {
                if (respRoot.getChild("ERROR") != null && respRoot.getChildText("ERROR").contains("Empty result")) {
                    throw new NoDataException(ESearchImpl.ERROR_INFO_NOT_FOUND);
                }
            }
        } // if esearch
        return res;
    }

    /**
     * Construye y devuelve un arreglo JSON que contiene la informacion sobre
     * enfermedades relacionadas con un gen. Esta informacion es obtenida usando 
     * {@link getDiseasesDom(String)}.
     * @param geneName simbolo del gen, por ejemplo: {@literal SF3B1}.
     * @return un arreglo JSON con la lista de enfermedades relacionadas con el gen especificado.
     * @throws org.nanopharmacy.eutility.impl.NoDataException En caso de no encontrar 
     *     informacion con los criterios especificados
     * @throws org.nanopharmacy.eutility.impl.UseHistoryException Si el parametro WebEnv 
     *     de la petici&oacute;n no es reconocido por Entrez
     * @throws java.net.ProtocolException Si ocurre un error en el protocolo utilizado 
     *     durante la conexion con Entrez
     * @throws java.io.IOException En caso de que haya problemas con la lectura de la 
     *     respuesta del servidor
     */
    public JSONArray getDiseasesInfo(final String geneName) throws NoDataException,
            UseHistoryException, ProtocolException, IOException {
        
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
     * Entrega un elemento JDOM que representa la informacion sobre enfermedades
     * relacionadas con un gen. Esta informacion es obtenida desde
     * el sistema de consultas Entrez de la NCBI.
     * @param geneName simbolo del gen, por ejemplo: {@literal SF3B1}.
     * @return Un elemento JDOM con la estructura <em>DocumentSummarySet</em>
     *     que contiene la lista de enfermedades relacionadas con el gen proporcionado.
     * @throws org.nanopharmacy.eutility.impl.NoDataException En caso de no encontrar 
     *     informaci&oacute;n con los criterios especificados
     * @throws org.nanopharmacy.eutility.impl.UseHistoryException Si el parametro WebEnv 
     *     de la peticion no es reconocido por Entrez
     * @throws java.net.ProtocolException Si ocurre un error en el protocolo utilizado 
     *     durante la conexi&oacute;n con Entrez
     * @throws java.io.IOException En caso de que haya problemas con la lectura de la 
     *     respuesta del servidor
     */
    public Element getDiseasesDom(final String geneName) throws NoDataException,
            UseHistoryException, ProtocolException, IOException {
        
        Element res = null;
        Document doc;
        String spec;
        spec = CMD_ESearchD.replaceFirst(Token_DbNAME, Db_MEDGEN);
        spec = spec.replaceFirst(Token_GENE, geneName);
        doc = this.getExternalData(spec);

        if (doc != null) {
            Element elem;
            Element respRoot = doc.getRootElement();
            String qryKey, webEnv;
            
            elem = respRoot.getChild(ESearchImpl.Elem_QryKEY);
            if (elem == null) {
                throw new UseHistoryException("No se encontro valor para el parametro de consulta: queryKey=");
            }
            qryKey = elem.getValue();
            elem = respRoot.getChild(ESearchImpl.Elem_WebENV);
            if (elem == null) {
                throw new UseHistoryException("No se encontro valor para el parametro de consulta: WebEnv=");
            }
            webEnv = elem.getValue();
            doc = null;
            respRoot = null;
            if (qryKey == null || webEnv == null) {
                throw new UseHistoryException("Entrez no devolvio queryKey ni WebEnv");
            }
            spec = CMD_ESummaryD.replaceFirst(Token_DbNAME, Db_MEDGEN);
            spec = spec.replaceFirst(Token_QryKEY, qryKey);
            spec = spec.replaceFirst(Token_WebENV, webEnv);
            doc = this.getExternalData(spec);

            if (doc != null) {
                respRoot = doc.getRootElement();
                res = respRoot.getChild("DocumentSummarySet");
                if (res == null) {
                    //throw new NoDataException("No se encontro el elemento DocumentSummarySet para el gen " + geneName);
                }
            } // if esummary
        } // if esearch
        return res;
    }

    /**
     * Construye y devuelve un JSONArray que contiene la informacion sobre
     * pruebas de laboratorio relacionadas con un gen. Esta informacion es
     * obtenida de {@link getTestingLabDom()}.
     *
     * @param geneName simbolo del gen, por ejemplo: {@literal SF3B1}.
     * @return un JSONArray con la lista de pruebas de laboratorio relacionadas con dicho gen.
     * @throws org.nanopharmacy.eutility.impl.NoDataException En caso de no encontrar 
     *     informaci&oacute;n con los criterios especificados
     * @throws org.nanopharmacy.eutility.impl.UseHistoryException Si el parametro WebEnv 
     *     de la petici&oacute;n no es reconocido por Entrez
     * @throws java.net.ProtocolException Si ocurre un error en el protocolo utilizado 
     *     durante la conexi&oacute;n con Entrez
     * @throws java.io.IOException En caso de que haya problemas con la lectura de la 
     *     respuesta del servidor
     */
    public JSONArray getTestingLabInfo(final String geneName)
            throws NoDataException, UseHistoryException, ProtocolException, IOException {
        
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
     * Entrega un elemento JDOM que representa la informacion sobre pruebas de
     * laboratorio relacionadas con un gen. Esta informacion es obtenida usando
     * el sistema de consultas Entrez de la NCBI.
     *
     * @param geneName simbolo del gen, por ejemplo: {@literal SF3B1}.
     * @return un elemento JDOM con la estructura {@literal DocumentSummarySet}
     * que contiene la lista de pruebas de laboratorio relacionadas con el gen
     * en cuestion.
     * @throws org.nanopharmacy.eutility.impl.NoDataException En caso de no encontrar 
     *     informaci&oacute;n con los criterios especificados
     * @throws org.nanopharmacy.eutility.impl.UseHistoryException Si el parametro WebEnv 
     *     de la petici&oacute;n no es reconocido por Entrez
     * @throws java.net.ProtocolException Si ocurre un error en el protocolo utilizado 
     *     durante la conexi&oacute;n con Entrez
     * @throws java.io.IOException En caso de que haya problemas con la lectura de la 
     *     respuesta del servidor
     */
    public Element getTestingLabDom(final String geneName)
            throws NoDataException, UseHistoryException, ProtocolException, IOException {
        
        Element res = new Element("DocumentSummarySet");
        Document doc;
        String spec;
        spec = CMD_ESearchL.replaceFirst(Token_DbNAME, Db_GTR);
        spec = spec.replaceFirst(Token_GENE, geneName);
        doc = this.getExternalData(spec);

        if (doc != null) {
            Element elem;
            Element respRoot = doc.getRootElement();
            List<Element> idList = null;
            String qryKey, webEnv;
            elem = respRoot.getChild(ESearchImpl.Elem_QryKEY);
            if (elem == null) {
//                System.out.println("no se encontro el valor de: queryKey");
                throw new UseHistoryException("no se encontro el valor de: queryKey");
            }
            qryKey = elem.getValue();

            elem = respRoot.getChild(ESearchImpl.Elem_WebENV);
            if (elem == null) {
//                System.out.println("no se encontro el valor de: WebEnv=");
                throw new UseHistoryException("no se encontro el valor de: WebEnv=");
            }
            webEnv = elem.getValue();

            idList = respRoot.getChild("IdList").getChildren("Id");
            doc = null;
            if (qryKey == null || webEnv == null) {
//                System.out.println("Entrez no devolvio queryKey ni WebEnv");
                throw new UseHistoryException("Entrez no devolvio queryKey ni WebEnv");
            }
            spec = CMD_ESummaryL.replaceFirst(Token_DbNAME, Db_GTR);
            spec = spec.replaceFirst(Token_QryKEY, qryKey);
            spec = spec.replaceFirst(Token_WebENV, webEnv);
            doc = this.getExternalData(spec);
            
            if (doc != null) {
                respRoot = doc.getRootElement();
                    elem = respRoot.getChild("DocumentSummarySet");
                    if (elem == null) {
//                        System.out.println("no se encontraro el elemento DocumentSummarySet para el gen " + geneName);
                        throw new NoDataException("no se encontraron elementos DocumentSummary para el gen " + geneName);
                    }
                    
                    List<String> ids = this.getValues(idList);
                    List<Element> docSumList = elem.getChildren("DocumentSummary");
                    for (Element docSum : docSumList) {
                        String uid = docSum.getAttributeValue("uid");
                        if (ids.contains(uid)) {
                            res.addContent((Element) docSum.clone());
                            ids.remove(uid);
                        }
                    }
            } // if esummary
        } // if esearch
        return res;
    }

    /**
     * Construye y devuelve un objeto Json que contiene la informacion sobre las
     * publicaciones medicas clasificadas en dos criterios. Las publicaciones
     * que no cumplen con una relevancia mayor a cero y las que tienen cierta
     * relevancia. Para aquellas publicaciones o articulos con una relevancia
     * mayor a cero, incluyen las propiedades del identificador del articulo,
     * y la propiedad {@literal ranking} con el valor de la
     * relevancia correspondiente y se encuentran en el arreglo
     * {@literal outstanding}.
     * @param geneName simbolo del gen a buscar en los articulos, por ejemplo: {@literal SF3B1}
     * @param molecularAlt simbolo de la alteracion molecular relacionada con el gen. Por ejemplo: {@literal Lys700Glu}.
     * @param ellapsedYears El numero de años hacia atras para realizar la
     *     busqueda. La busqueda comprende a partir de un dia con fecha como el actual,
     *     pero con este valor de años atras, hasta el dia actual. Es excluyente con {@code ellapsedDays} y con
     *     los valores de {@literal initMonth} y {@literal finalMonth}
     * @param ellapsedDays numero de dias atras a partir de la fecha actual para realizar una busqueda,
     * es excluyente con {@code ellapsedYears} y con los valores de {@literal initMonth} y {@literal finalMonth}
     * @param initMonth numero de meses atras, que indica el inicio del periodo de busqueda. 
     *        El uso de {@code initMonth} y {@code finalMonth} excluye el uso de los parametros
     *        {@code ellapsedYears} y {@code ellapsedDays}
     * @param finalMonth numero de meses atras, que indica el fin del periodo de busqueda.
     *        El uso de {@code initMonth} y {@code finalMonth} excluye el uso de los parametros 
     *        {@code ellapsedYears} y {@code ellapsedDays}
     * @return un objeto JSON con la informacion de las publicaciones medicas
     * referentes a los valores de los parametros. Este objeto incluye dos
     * arreglos: El arrego "outstanding" contiene las publicaciones provenientes
     * de PubMed y de PMC.
     * @throws org.nanopharmacy.eutility.impl.NoDataException En caso de no encontrar 
     *     informaci&oacute;n con los criterios especificados
     * @throws org.nanopharmacy.eutility.impl.UseHistoryException Si el parametro WebEnv 
     *     de la petici&oacute;n no es reconocido por Entrez
     * @throws java.net.ProtocolException Si ocurre un error en el protocolo utilizado 
     *     durante la conexi&oacute;n con Entrez
     * @throws java.io.IOException En caso de que haya problemas con la lectura de la 
     *     respuesta del servidor
     */
    public JSONObject getPublicationsInfo(final String geneName, final String molecularAlt,
            final int ellapsedYears, final int ellapsedDays, final int initMonth, final int finalMonth)
            throws NoDataException, UseHistoryException, ProtocolException, IOException {
        
        JSONObject publications = new JSONObject();// publicaciones aceptadas y rechazadas
        JSONArray outstanding = new JSONArray();   // publicaciones aceptadas en el resultado final
        //JSONArray rejected = new JSONArray();      // publicaciones rechazadas debido a su ranking menor a 2

        Document doc = null;
        Element respRoot = null;
        try {
            if (ellapsedYears > 0 || ellapsedDays > 0 || 
                    (initMonth >= 0 && finalMonth > 0 && initMonth < finalMonth)) {
                doc = getPublicationsDom(geneName, molecularAlt, ellapsedYears, ellapsedDays, initMonth, finalMonth);
                respRoot = doc.getRootElement();
            } else {
//                System.out.println("Periodo de busqueda mal definido");
                throw new NoDataException("Periodo de busqueda mal definido");
            }
        } catch (NoDataException nde) {
            JSONObject errorData = new JSONObject();
            errorData.put("error", ESearchImpl.ERROR_INFO_NOT_FOUND);
            errorData.put("msg", "No data for your search");
            publications.put("error", errorData);
//            System.out.println("No data for your search: " + nde);
            doc = null;
            Logger.getLogger(ESearchImpl.class.getName()).log(Level.SEVERE, null, nde);
        } catch (IOException ioe) {
            JSONObject errorData = new JSONObject();
            errorData.put("error", ESearchImpl.ERROR_IN_COMM);
            errorData.put("msg", "Communications problem");
            publications.put("error", errorData);
            System.out.println("Communications problem: " + ioe);
            doc = null;
            Logger.getLogger(ESearchImpl.class.getName()).log(Level.SEVERE, null, ioe);
        } catch (Exception e) {
            JSONObject errorData = new JSONObject();
            errorData.put("error", ESearchImpl.ERROR_GENERAL);
            errorData.put("msg", "General problem");
            publications.put("error", errorData);
            System.out.println("General problem: " + e);
            doc = null;
            Logger.getLogger(ESearchImpl.class.getName()).log(Level.SEVERE, null, e);
        }
        
        JSONObject article, abstrct;
        String pmid;
        int rank, rankMax;
        StringBuilder acceptedPubMed = new StringBuilder(512);
        int accepted = 0;
        int recovered = 0;

        Date timeNow = new Date(System.currentTimeMillis());
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:SSz");
//        System.out.println("Inicia creacion de JSON: " + format.format(timeNow));
        if (respRoot != null) {
//            System.out.println("Copiando elementos ...");
            for (Element articleList : (List<Element>) respRoot.getChildren()) {
                
//                System.out.println(" ... Elemento: " + articleList.getName());
                for (Element pubmedArt : (List<Element>) articleList.getChildren("article")) {
                    recovered++;
                    pmid = pubmedArt.getChildText("pmid");
                    if (pmid != null && !pmid.isEmpty() && acceptedPubMed.indexOf(pmid) >= 0) {
//                        System.out.println("pmid repetido: " + pmid);
                        continue;
                    } else if (pmid != null && !pmid.isEmpty()) {
                        acceptedPubMed.append(pmid).append(",");
                        accepted++;
                    }
                    rankMax = 0;
                    List<Element> abstractLst = pubmedArt.getChildren("abstract");
                    try {
                        JSONArray abstracts = new JSONArray();
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
                            if (pmid != null) {
                                article.put("pmid", pmid);
                            }
                            if (pubmedArt.getChild("pmc") != null) {
                                article.put("pmc", pubmedArt.getChildText("pmc"));
                            }
                            article.put("ranking", rankMax);
                            article.put("abstract", abstracts);
                            article.put("articleTitle", pubmedArt.getChildText("title"));
                            article.put("url", pubmedArt.getChildText("url"));
                            article.put("author", pubmedArt.getChildText("author"));
                            article.put("reference", pubmedArt.getChildText("reference"));
                            article.put("publicationYear", pubmedArt.getChildText("publicationYear"));
                            article.put("publicationMonth", pubmedArt.getChildText("publicationMonth"));
                            outstanding.put(article);
        //                } else {
        //                    article = new JSONObject();
        //                    if (pubmedArt.getChild("pmc") != null) {
        //                        article.put("pmc", pubmedArt.getChildText("pmc"));
        //                    } else {
        //                        article.put("pmid", pmid);
        //                    }
        //                    rejected.put(article);
                        }
                    } catch (JSONException | NumberFormatException jse) {
                        Logger.getLogger(ESearchImpl.class.getName()).log(Level.SEVERE, null, jse);
                    }
                } // for
                    
                    
                    
            }
//            System.out.println(">>>>Fin de copiado");
//                pubmedArtList = respRoot.getChildren("article");
        }
        doc = null;
//        System.out.println("total de recuperados = " + recovered);
//        System.out.println("total de no repetidos (xml) = " + accepted);
//        System.out.println("total de aceptados (json) = " + outstanding.length());

        try {
            if (!publications.has("error")) {
                publications.put("outstanding", outstanding);
                //publications.put("rejected", rejected);
            }
        } catch (Exception jse) {
            System.out.println("Error in getPublicationsInfo: " + jse);
            Logger.getLogger(ESearchImpl.class.getName()).log(Level.SEVERE, null, jse);
        }
        return publications;
    }

    /**
     * Entrega un elemento JDOM que representa la informacion sobre
     * publicaciones medicas relacionadas con un gen. Esta informacion es
     * obtenida usando las bases de datos PubMed y PubMed Central del sistema de
     * consultas Entrez de la NCBI.
     * @param geneName simbolo del gen, por ejemplo: {@literal SF3B1}.
     * @param molecularAlt simbolo de la alteraci&oacute;n gen&eacute;tica relacionada con el gen
     * @param ellapsedYears El numero de años hacia atras para realizar la
     *     busqueda. La busqueda comprende a partir de un dia con fecha como el actual,
     *     pero con este valor de años atras, hasta el dia actual. Es excluyente con {@code ellapsedDays} y con
     *     los valores de {@literal initMonth} y {@literal finalMonth}
     * @param ellapsedDays numero de dias atras a partir de la fecha actual para realizar una busqueda,
     * es excluyente con {@code ellapsedYears} y con los valores de {@literal initMonth} y {@literal finalMonth}
     * @param initMonth numero de meses atras, que indica el inicio del periodo de busqueda. 
     *        El uso de {@code initMonth} y {@code finalMonth} excluye el uso de los parametros
     *        {@code ellapsedYears} y {@code ellapsedDays}
     * @param finalMonth numero de meses atras, que indica el fin del periodo de busqueda.
     *        El uso de {@code initMonth} y {@code finalMonth} excluye el uso de los parametros 
     *        {@code ellapsedYears} y {@code ellapsedDays}
     * @return un documento JDOM con la estructura {@literal PubmedArticleSet} que
     * contiene dos elementos {@literal ArticleList}, el primero corresponde a los datos extraidos de
     * la base de datos PubMed y el segundo de PMC.
     * @throws org.nanopharmacy.eutility.impl.NoDataException En caso de no encontrar 
     *     informaci&oacute;n con los criterios especificados
     * @throws org.nanopharmacy.eutility.impl.UseHistoryException Si el parametro WebEnv 
     *     de la petici&oacute;n no es reconocido por Entrez
     * @throws java.net.ProtocolException Si ocurre un error en el protocolo utilizado 
     *     durante la conexi&oacute;n con Entrez
     * @throws java.io.IOException En caso de que haya problemas con la lectura de la 
     *     respuesta del servidor
     */
    public Document getPublicationsDom(final String geneName, final String molecularAlt,
            int ellapsedYears, final int ellapsedDays, final int initMonth, final int finalMonth)
            throws NoDataException, UseHistoryException, ProtocolException, IOException {
        
        Document doc = new Document(new Element("PubmedArticleSet"));
        Element elem;
        elem = getPubMedDom(Db_PUBMED, geneName, molecularAlt, ellapsedYears, ellapsedDays, initMonth, finalMonth);
        doc.getRootElement().addContent(elem);
        elem = getPMCDom(Db_PMC, geneName, molecularAlt, ellapsedYears, ellapsedDays, initMonth, finalMonth);
        doc.getRootElement().addContent(elem);
        return doc;
    }

    /**
     * Entrega un elemento JDOM que representa la informacion sobre
     * publicaciones medicas relacionadas con un gen y una alteracion molecular proporcionadas.
     * Esta informacion es obtenida usando la base de datos PubMed del sistema de consultas
     * Entrez de la NCBI.
     * @param dbName Nombre de la base de datos de consulta.
     * @param geneName simbolo del gen, por ejemplo: {@literal SF3B1}.
     * @param molecularAlt La alteracion molecular relacionada con el gen
     * @param ellapsedYears El numero de años hacia atras para realizar la
     *     busqueda. La busqueda comprende a partir de un dia con fecha como el actual,
     *     pero con este valor de años atras, hasta el dia actual. Es excluyente con {@code ellapsedDays} y con
     *     los valores de {@literal initMonth} y {@literal finalMonth}
     * @param ellapsedDays numero de dias atras a partir de la fecha actual para realizar una busqueda,
     * es excluyente con {@code ellapsedYears} y con los valores de {@literal initMonth} y {@literal finalMonth}
     * @param initMonth numero de meses atras, que indica el inicio del periodo de busqueda. 
     *        El uso de {@code initMonth} y {@code finalMonth} excluye el uso de los parametros
     *        {@code ellapsedYears} y {@code ellapsedDays}
     * @param finalMonth numero de meses atras, que indica el fin del periodo de busqueda.
     *        El uso de {@code initMonth} y {@code finalMonth} excluye el uso de los parametros 
     *        {@code ellapsedYears} y {@code ellapsedDays}
     * @return un elemento JDOM con la estructura {@literal ArticleList} que
     * contiene la publicaciones medicas relacionadas con el gen en cuestion.
     * @throws org.nanopharmacy.eutility.impl.NoDataException En caso de no encontrar 
     *     informacion con los criterios especificados
     * @throws org.nanopharmacy.eutility.impl.UseHistoryException Si el parametro WebEnv 
     *     de la petici&oacute;n no es reconocido por Entrez
     * @throws java.net.ProtocolException Si ocurre un error en el protocolo utilizado 
     *     durante la conexi&oacute;n con Entrez
     * @throws java.io.IOException En caso de que haya problemas con la lectura de la 
     *     respuesta del servidor
     */
    public Element getPubMedDom(final String dbName, final String geneName, final String molecularAlt,
            int ellapsedYears, final int ellapsedDays, final int initMonth, final int finalMonth)
            throws NoDataException, UseHistoryException, ProtocolException, IOException {
        
        Element root = new Element("ArticleList");
        Document doc;
        String spec;
        spec = getEllapsedTimeQuery(CMD_ESearchP, ellapsedYears, ellapsedDays, initMonth, finalMonth);
        spec = spec.replaceFirst(TOKEN_ALTMOL, getQueryValue(molecularAlt));
        spec = spec.replaceFirst(Token_DbNAME, dbName);
        spec = spec.replaceFirst(Token_GENE, geneName);
//        System.out.println("\nPubMed URL:\n" + spec);
        doc = this.getExternalData(spec);
        
        if (doc != null) {
            int count = 0;
            Element elem;
            List<Element> nodes;
            String qryKey, webEnv;

            Element respRoot = doc.getRootElement();
            elem = respRoot.getChild(ESearchImpl.Elem_COUNT);
            if (elem == null) {
                throw new UseHistoryException("no se encontro valor del parametro de consulta: Count");
            }
            try {
                count = Integer.parseInt(elem.getText());
            } catch (NumberFormatException nfe) {
                Logger.getLogger(ESearchImpl.class.getName()).log(Level.SEVERE, null, nfe);
                throw new NoDataException("el valor de consulta Count es ilegible");
            }

            elem = respRoot.getChild(ESearchImpl.Elem_QryKEY);
            if (elem == null) {
                throw new UseHistoryException("no se encontro valor de: queryKey");
            }
            qryKey = elem.getValue();

            elem = respRoot.getChild(ESearchImpl.Elem_WebENV);
            if (elem == null) {
                throw new UseHistoryException("no se encontro valor de: WebEnv=");
            }
            webEnv = elem.getValue();
            
            doc = null;
            elem = null;
            respRoot = null;
            
            if (qryKey == null || webEnv == null) {
                throw new UseHistoryException("Entrez no devolvio queryKey ni WebEnv");
            }
            
            if (count > 0) {
//                System.out.println("Articulos en busqueda PubMed: " + count);
                spec = CMD_EFetch.replaceFirst(Token_DbNAME, Db_PUBMED);
                spec = spec.replaceFirst(Token_QryKEY, qryKey);
                spec = spec.replaceFirst(Token_WebENV, webEnv);
                spec = spec.replaceFirst(Token_RetStart, "0");
                spec = spec.replaceFirst(Token_RetMax, Integer.toString(count));
                doc = this.getExternalData(spec);
                
                if (doc != null) {
                    List<Element> abstractLst;
                    String pmid, value;

                    respRoot = doc.getRootElement();
                    if (respRoot == null) {
                        throw new NoDataException("no se encontraron elementos PubmedArticle para el gen " + geneName);
                    }

                    int sinAbstract = 0;
                    int rankCero = 0;
                    List<Element> pubmedArtList = respRoot.getChildren("PubmedArticle");
                    for (Element pubmedArt : pubmedArtList) {
                        int rank = 0;
                        int globalRank = 0;
                        String articleTite = null;
                        String author = null;
                        String pubYear = null;
                        String pubMonth = null;
                        Element articleNode = pubmedArt.getChild("MedlineCitation").getChild("Article");

                        if (articleNode.getChild("Abstract") == null) {
                            sinAbstract++;
                            continue;
                        }
                        Element art = new Element("article");

                        abstractLst = articleNode.getChild("Abstract").getChildren("AbstractText");
                        String tmpPrognosis = "0";
                        String tmpTreatment = "0";
                        String tmpPrediction = "0";
                        Element abs = new Element("abstract");

                        for (Element e : abstractLst) {
                            elem = new Element("label");
                            elem.setText(e.getAttributeValue("Label") == null ? "Unlabeled" : e.getAttributeValue("Label"));
                            abs.addContent(elem);

                            elem = new Element("text");
                            value = e.getValue();
                            elem.setText(value);
                            abs.addContent(elem);

                            Matcher m = prognosisPtrn.matcher(value);
                            if(m.matches()) {
                                tmpPrognosis = "1";
                            }
                            Matcher m1 = treatmentPtrn.matcher(value);
                            if(m1.matches()) {
                                tmpTreatment = "1";
                            }
                            Matcher m2 = predictPtrn.matcher(value);
                             if(m2.matches()) {
                                tmpPrediction = "1";
                            }
                            rank = Utils.getRanking(value, geneName, molecularAlt);
                            elem = new Element("rank");
                            elem.setText(Integer.toString(rank));
                            abs.addContent(elem);
                            globalRank = globalRank < rank ? rank : globalRank;
                        }
                        if (globalRank == 0) {
                            rankCero++;
                            continue;  //No nos interesan estos articulos
                        }
                        elem = new Element("prognosis");
                        elem.setText(tmpPrognosis);
                        abs.addContent(elem);
                        elem = new Element("treatment");
                        elem.setText(tmpTreatment);
                        abs.addContent(elem);
                        elem = new Element("prediction");
                        elem.setText(tmpPrediction);
                        abs.addContent(elem);
                        art.addContent(abs);

                        elem = new Element("title");
                        articleTite = articleNode.getChildText("ArticleTitle");
                        elem.setText(articleTite);
                        art.addContent(elem);
                        pmid = pubmedArt.getChild("MedlineCitation").getChildText("PMID");
                        elem = new Element("pmid");
                        elem.setText(pmid);
                        art.addContent(elem);
                        elem = new Element("url");
                        elem.setText(Url_NBCI + Db_PUBMED + "/" + pmid);
                        art.addContent(elem);

                        StringBuilder r = new StringBuilder(256);
                        if (articleNode.getChild("AuthorList") != null) {
                            nodes = articleNode.getChild("AuthorList").getChildren("Author");
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
                        elem = articleNode.getChild("Journal").getChild("JournalIssue");
                        pubYear = elem.getChild("PubDate").getChildText("Year");
                        pubMonth = elem.getChild("PubDate").getChildText("Month");
                        r.append("(").append(pubYear).append("). ");
                        r.append(articleTite);
                        r.append(" ");
                        r.append(elem.getParentElement().getChildText("Title")); //titulo de revista
                        r.append(". ISSN:").append(elem.getParentElement().getChildText("ISSN"));
                        r.append(". vol.").append(elem.getChildText("Volume"));
                        r.append(". issue ").append(elem.getChildText("Issue")).append(". ");
                        r.append(pubMonth == null
                                ? ""
                                : " " + pubMonth);
                        r.append(elem.getChild("PubDate").getChildText("Day") == null
                                ? ""
                                : " " + elem.getChild("PubDate").getChildText("Day"));
                        r.append(".");
                        elem = new Element("reference");
                        elem.setText(r.toString());
                        art.addContent(elem);
                        elem = null;
                        elem = new Element("publicationYear");
                        elem.setText(pubYear);
                        art.addContent(elem);
                        elem = null;
                        elem = new Element("publicationMonth");
                        if (pubMonth != null) {
                            for (int i = 0; i < Utils.Months.length; i++) {
                                String monthName = Utils.Months[i];
                                if (monthName.equals(pubMonth)) {
                                    pubMonth = Integer.toString(i + 1);
                                    break;
                                }
                            }
                        }
                        elem.setText(pubMonth != null ? pubMonth : "");
                        art.addContent(elem);
                        root.addContent(art);
                        pubYear = null;
                        pubMonth = null;
                    }
//                    System.out.println("*** Sin abstract: " + sinAbstract);
//                    System.out.println("Con rank igual a cero: " + rankCero);
                } // if esummary
            }// if count > 0
        } // if esearch
        return root;
    }

    /**
     * Entrega un elemento JDOM que representa la informacion sobre
     * publicaciones medicas relacionadas con un gen y una alteracion molecular. Esta informacion es
     * obtenida usando la base de datos PubMed Central (pmc) del sistema de consultas Entrez de la NCBI.
     * @param dbName nombre de la base de datos de consulta.
     * @param geneName simbolo del gen, por ejemplo: {@literal SF3B1}.
     * @param molecularAlt La alteracion molecular relacionada con el gen
     * @param ellapsedYears El numero de años hacia atras para realizar la
     *     busqueda. La busqueda comprende a partir de un dia con fecha como el actual,
     *     pero con este valor de años atras, hasta el dia actual. Es excluyente con {@code ellapsedDays} y con
     *     los valores de {@literal initMonth} y {@literal finalMonth}
     * @param ellapsedDays numero de dias atras a partir de la fecha actual para realizar una busqueda,
     * es excluyente con {@code ellapsedYears} y con los valores de {@literal initMonth} y {@literal finalMonth}
     * @param initMonth numero de meses atras, que indica el inicio del periodo de busqueda. 
     *        El uso de {@code initMonth} y {@code finalMonth} excluye el uso de los parametros
     *        {@code ellapsedYears} y {@code ellapsedDays}
     * @param finalMonth numero de meses atras, que indica el fin del periodo de busqueda.
     *        El uso de {@code initMonth} y {@code finalMonth} excluye el uso de los parametros 
     *        {@code ellapsedYears} y {@code ellapsedDays}
     * @return un elemento JDOM con la estructura {@literal ArticleList} que
     * contiene la publicaciones medicas relacionadas con el gen y la alteracion molecular en cuestion.
     * @throws org.nanopharmacy.eutility.impl.NoDataException En caso de no encontrar 
     *     informacion con los criterios especificados
     * @throws org.nanopharmacy.eutility.impl.UseHistoryException Si el parametro WebEnv 
     *     de la petici&oacute;n no es reconocido por Entrez
     * @throws java.net.ProtocolException Si ocurre un error en el protocolo utilizado 
     *     durante la conexi&oacute;n con Entrez
     * @throws java.io.IOException En caso de que haya problemas con la lectura de la 
     *     respuesta del servidor
     */
    public Element getPMCDom(final String dbName, final String geneName, final String molecularAlt,
            int ellapsedYears, final int ellapsedDays, final int initMonth, final int finalMonth)
            throws NoDataException, UseHistoryException, ProtocolException, IOException {
        
        Element root = new Element("ArticleList");
        Document doc;
        String spec;
        spec = getEllapsedTimeQuery(CMD_ESearchP, ellapsedYears, ellapsedDays, initMonth, finalMonth);
        spec = spec.replaceFirst(TOKEN_ALTMOL, getQueryValue(molecularAlt));
        spec = spec.replaceFirst(Token_DbNAME, dbName);
        spec = spec.replaceFirst(Token_GENE, geneName);
//        System.out.println("\nPMC URL:\n" + spec);
        
        doc = this.getExternalData(spec);
        if (doc != null) {
            int count = 0;
            Element elem;
            String qryKey, webEnv;
            Element respRoot = doc.getRootElement();
            elem = respRoot.getChild(ESearchImpl.Elem_COUNT);
            if (elem == null) {
                throw new UseHistoryException("no se encontro el valor del parametro de consulta: Count");
            }
            try {
                count = Integer.parseInt(elem.getText());
            } catch (NumberFormatException nfe) {
                Logger.getLogger(ESearchImpl.class.getName()).log(Level.SEVERE, null, nfe);
                throw new NoDataException("el valor de consulta Count es ilegible");
            }

            elem = respRoot.getChild(ESearchImpl.Elem_QryKEY);
            if (elem == null) {
                throw new UseHistoryException("no se encontro el valor de: queryKey");
            }
            qryKey = elem.getValue();

            elem = respRoot.getChild(ESearchImpl.Elem_WebENV);
            if (elem == null) {
                throw new UseHistoryException("no se encontro el valor de: WebEnv=");
            }
            webEnv = elem.getValue();
            elem = null;
            
            if (qryKey == null || webEnv == null) {
                throw new UseHistoryException("Entrez no devolvio queryKey ni WebEnv");
            }
            
            if (count > 0) {
//                System.out.println("\nArticulos en busqueda PMC: " + count);
                spec = CMD_EFetch.replaceFirst(Token_DbNAME, Db_PMC);
                spec = spec.replaceFirst(Token_QryKEY, qryKey);
                spec = spec.replaceFirst(Token_WebENV, webEnv);
                spec = spec.replaceFirst(Token_RetStart, "0");
                spec = spec.replaceFirst(Token_RetMax, Integer.toString(count));
                doc = this.getExternalData(spec); //Obtiene el detalle de los articulos
                
                if (doc != null) {
                    String author, value;

                    try {
                        elem = doc.getRootElement();
                        if (elem == null) {
                            throw new NoDataException("no se encontraron elementos DocumentSummary para el gen " + geneName);
                        }
                        
                        List<Element> pubmedArtList = elem.getChildren("article");
                        int articulosEnXML = 0;
                        int rankCero = 0;
                        for (Element pubmedArt : pubmedArtList) {
                            try {
                                int rank = 0;
                                String pmid = null;
                                String pmc = null;
                                String day = null;
                                String month = null;
                                String year = null;
                                articulosEnXML++;
                                Element articleMetaNode = pubmedArt.getChild("front").getChild("article-meta");
                                int globalRank = 0;
                                Element art = new Element("article");

                                List<Element> abstractSecNodes = articleMetaNode.getChild("abstract") != null &&
                                                                    articleMetaNode.getChild("abstract").getChildren("sec") != null
                                                                ? articleMetaNode.getChild("abstract").getChildren("sec") : null;
                                if (abstractSecNodes == null || abstractSecNodes.isEmpty()) {
                                    if (articleMetaNode.getChild("abstract") == null) {
                                        continue;
                                    } else {
                                        for (Element abstractPart : (List<Element>) articleMetaNode.getChild("abstract").getChildren("p")) {
                                            Element abs = new Element("abstract");
                                            elem = new Element("label");
                                            elem.setText("Unlabeled");
                                            abs.addContent(elem);
                                            elem = new Element("text");
                                            value = abstractPart.getText();
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
                                            globalRank = globalRank < rank ? rank : globalRank;
                                        }
                                    }
                                } else {
                                    boolean atLeastOneAbstract = false;
                                    for (Element e : abstractSecNodes) {
                                        Element abs = new Element("abstract");

                                        elem = new Element("label");
                                        elem.setText(e.getChildText("title"));
                                        abs.addContent(elem);

                                        elem = new Element("text");
                                        value = e.getChildText("p");
                                        if (value != null) {
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
                                            globalRank = globalRank < rank ? rank : globalRank;
                                            art.addContent(abs);
                                            atLeastOneAbstract = true;
                                        }
                                    }
                                    if (!atLeastOneAbstract) {//No tiene texto en abstract
                                        continue;
                                    }
                                }
                                if (globalRank == 0) {
                                    rankCero++;
                                    continue;  //No nos interesan estos articulos
                                }
                                elem = new Element("title");
                                elem.setText(articleMetaNode.getChild("title-group").getChild("article-title").getValue());
                                art.addContent(elem);
                                elem = new Element("pmid");
                                List<Element> articleIdNodes = articleMetaNode.getChildren("article-id");
                                for (Element idNode : articleIdNodes) {
                                    if (idNode.getAttributeValue("pub-id-type").equalsIgnoreCase("pmid")) {
                                        pmid = idNode.getText();
                                    } else if (idNode.getAttributeValue("pub-id-type").equalsIgnoreCase("pmc")) {
                                        pmc = idNode.getText();
                                    }
                                }
                                elem.setText(pmid != null ? pmid : "");
                                art.addContent(elem);
                                elem = new Element("pmc");
                                elem.setText(pmc != null ? pmc : "");
                                art.addContent(elem);
                                elem = new Element("url");
                                elem.setText("http://www.ncbi.nlm.nih.gov/pmc/articles/PMC" + pmc);
                                art.addContent(elem);

                                StringBuilder r = new StringBuilder(256);
                                List<Element> authorsNodes = articleMetaNode.getChild("contrib-group") != null
                                        ? articleMetaNode.getChild("contrib-group").getChildren("contrib") : null;
                                if (authorsNodes != null) {
                                    elem = authorsNodes.get(0);
                                    if (elem.getChild("name") != null) {
                                        author = elem.getChild("name").getChildText("surname") +
                                                ", " + elem.getChild("name").getChildText("given-names");
                                        elem = new Element("author");
                                        elem.setText(author);
                                        art.addContent(elem);
                                    }

                                    for (Element e : authorsNodes) {
                                        if (e.getChild("name") == null) {
                                            continue;
                                        }
                                        r.append(e.getChild("name").getChildText("surname")).append(", ").
                                                append(e.getChild("name").getChildText("given-names")).append("; ");
                                    }
                                }
                                List<Element> datesNodes = articleMetaNode.getChildren("pub-date");

                                for (Element dateNode : datesNodes) {
                                    if (dateNode.getAttributeValue("pub-type").equalsIgnoreCase("epub")) {
                                        day = dateNode.getChildText("day");
                                        month = dateNode.getChildText("month");
                                        year = dateNode.getChildText("year");
                                        break;
                                    } else if (dateNode.getAttributeValue("pub-type").equalsIgnoreCase("ppub")) {
                                        day = dateNode.getChildText("day");
                                        month = dateNode.getChildText("month");
                                        year = dateNode.getChildText("year");
                                        break;
                                    } else if (year != null && dateNode.getAttributeValue("pub-type").startsWith("pmc")) {
                                        day = dateNode.getChildText("day");
                                        month = dateNode.getChildText("month");
                                        year = dateNode.getChildText("year");
                                    }
                                }
                                if (year != null) {
                                    r.append("(").append(year).append("). ");
                                }
                                
                                //titulo y revista de la referencia
                                r.append(articleMetaNode.getChild("title-group").getChild("article-title").getValue());

                                Element magazine = articleMetaNode.getChild("journal-title-group") != null
                                        ? articleMetaNode.getChild("journal-title-group").getChild("journal-title") : null;
                                if (magazine != null) {
                                    r.append(". ");
                                    r.append(magazine.getValue());
                                }
                                String epubISSN = null;
                                String ppubISSN = null;
                                List<Element> issnNodes = articleMetaNode.getChildren("issn");
                                for (Element issnNode : issnNodes) {
                                    if (issnNode.getAttributeValue("pub-type").equalsIgnoreCase("epub")) {
                                        epubISSN = issnNode.getText();
                                        break;
                                    } else if (issnNode.getAttributeValue("pub-type").equalsIgnoreCase("ppub")) {
                                        ppubISSN = issnNode.getText();
                                    }
                                }
                                if (epubISSN != null || ppubISSN != null) {
                                    r.append(". ISSN:");
                                    r.append(epubISSN != null ? epubISSN : ppubISSN);
                                }

                                elem = articleMetaNode.getChild("volume");
                                if (elem != null) {
                                    r.append(". vol.");
                                    r.append(elem.getValue());
                                }

                                elem = articleMetaNode.getChild("issue");
                                if (elem != null) {
                                    r.append(". issue ");
                                    r.append(articleMetaNode.getChild("issue").getValue()).append(". ");
                                }

                                if (month != null) {
                                    try {
                                        r.append(". ");
                                        r.append(Utils.Months[Integer.parseInt(month)-1]);
                                        if (day != null) {
                                            r.append(" ");
                                            r.append(day);
                                            r.append(".");
                                        }
                                    } catch (Exception e) {
                                        Logger.getLogger(ESearchImpl.class.getName()).log(Level.SEVERE, null, e);
                                    }
                                }
                                elem = new Element("reference");
                                elem.setText(r.toString());
                                art.addContent(elem);
                                elem = null;
                                elem = new Element("publicationYear");
                                elem.setText(year);
                                art.addContent(elem);
                                elem = null;
                                elem = new Element("publicationMonth");
                                elem.setText(month != null ? month : "");
                                art.addContent(elem);
                                elem = null;
                                root.addContent(art);
                            } catch (Exception e) {
                                Logger.getLogger(ESearchImpl.class.getName()).log(Level.SEVERE, null, e);
                            }
                        } // for pubmedArt
//                        System.out.println("Articulos en XML PMC: " + articulosEnXML);
//                        System.out.println("Excluidos con rank cero: " + rankCero);
                    } catch (Exception jde) {
                            Logger.getLogger(ESearchImpl.class.getName()).log(Level.SEVERE, null, jde);
                        System.out.println("Ha ocurrido un error mientras se obtiene informacion de PMC: " + jde);
                        throw new NoDataException("Ha ocurrido un error mientras se obtiene informacion de PMC");
                    }
                } // if efetch
            } //if count > 0
        } // if esearch
        return root;
    }

    /**
     * Genera una lista con los valores contenidos dentro de otra lista de elementos JDOM.
     * @param children El parametro children define la lista de elementos a los
     * que se les extraera su valor contenido y se agregara a una nueva lista.
     * @return La lista de valores contenidos en la lista de elementos hijos de
     * alguna jerarquia mayor.
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
     * @return {@literal true} en caso de existir y {@literal false} en el caso contrario
     * @throws org.nanopharmacy.eutility.impl.NoDataException En caso de no encontrar 
     *     informaci&oacute;n con los criterios especificados
     * @throws org.nanopharmacy.eutility.impl.UseHistoryException Si el parametro WebEnv 
     *     de la petici&oacute;n no es reconocido por Entrez
     * @throws java.net.MalformedURLException Si la ruta a la que se realiza la peticion HTTP
     *     no esta formada correctamente
     * @throws java.net.ProtocolException Si ocurre un error en el protocolo utilizado 
     *     durante la conexi&oacute;n con Entrez
     * @throws java.io.IOException En caso de que haya problemas con la lectura de la 
     *     respuesta del servidor
     */
     public boolean hasGeneBD(final String geneName) throws NoDataException, UseHistoryException,
             MalformedURLException, ProtocolException, IOException {
         
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
            Logger.getLogger(ESearchImpl.class.getName()).log(Level.SEVERE, null, jde);
        } finally {
            conex.disconnect();
        }

        if (doc != null) {
            Element elem;
            Element respRoot = doc.getRootElement();
            String qryKey;
            elem = respRoot.getChild(ESearchImpl.Elem_COUNT);
            if (elem != null) {
                qryKey = elem.getValue();
                if (Integer.parseInt(qryKey) > 0) {
                    isValid = true;
                }
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
    
    /**
     * Realiza peticiones HTTP a la ruta especificada, por el metodo GET, esperando obtener 
     * en respuesta un documento representado por un objeto {@link org.jdom.Document}
     * @param path ruta a la que se desea hacer la peticion, incluyendo los parametros necesarios
     * @return un {@link org.jdom.Document} que contiene la informacion de los articulos 
     * solicitados por la peticion o {@literal null} en caso de {@link org.jdom.JDOMException} o 
     * {@link java.net.MalformedURLException}
     * @throws IOException en caso de problemas con la lectura de la respuesta
     */
    private Document getExternalData(String path) throws IOException {
        
        Document doc;
        URL cmd;
        HttpURLConnection conex = null;
//        System.out.println("\nURL:\n" + path);
        cmd = new URL(path);
        try {
            conex = (HttpURLConnection) cmd.openConnection();
            conex.setConnectTimeout(30000);
            conex.setReadTimeout(1500000);
            conex.setRequestMethod("GET");
            conex.setDoOutput(true);
            conex.connect();
            InputStream in = conex.getInputStream();
            doc = getXML(in);
            in.close();
        } catch (JDOMException jde) {
            doc = null;
        } catch (MalformedURLException mue) {
            Logger.getLogger(ESearchImpl.class.getName()).log(Level.SEVERE, "Request to: " + path, mue);
            doc = null;
        } finally {
            if (conex != null) {
                conex.disconnect();
            }
            conex = null;
            cmd = null;
        }
        return doc;
    }
}
