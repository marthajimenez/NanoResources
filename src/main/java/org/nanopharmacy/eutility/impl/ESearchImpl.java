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
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
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
 * ESearch (text searches).
 * Entrez es un sistema de consulta y recuperación desarrollado por el National 
 * Center for Biotechnology Information (NCBI). Puede utilizar Entrez para acceder 
 * a varias bases de datos enlazadas que el NCBI hospeda.
 * @see eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi
 * Responds to a text query with the list of matching UIDs in a given database 
 * (for later use in ESummary, EFetch or ELink), along with the term translations 
 * of the query.
 * 
 * @author carlos.ramos
 * @version 07/10/2015
 * @see <a href="http://www.ncbi.nlm.nih.gov/Class/MLACourse/Original8Hour/Entrez/">Entrez NCBI</a>
 */
public class ESearchImpl {
        
    public static final String Db_GENE = "gene";     // Base de datos hospedada por NCBI
    public static final String Db_MEDGEN = "medgen"; // Base de datos hospedada por NCBI
    public static final String Db_GTR = "gtr";       // Base de datos hospedada por NCBI
    public static final String Db_PUBMED = "pubmed"; // Base de datos hospedada por NCBI
    
    public static final String Elem_SrhRES = "eSearchResult"; // Elemento en el DTD de E-utility
    public static final String Elem_QryKEY = "QueryKey";      // Elemento en el DTD de E-utility
    public static final String Elem_WebENV = "WebEnv";        // Elemento en el DTD de E-utility
    public static final String Elem_DocSummary = "DocumentSummary";  // Elemento en el DTD de E-utility
    public static final String Elem_NAME = "Name";            // Elemento en el DTD de E-utility 
    public static final String Elem_ORGANISM = "Organism";    // Elemento en el DTD de E-utility 
    public static final String Elem_SciNAME = "ScientificName";  // Elemento en el DTD de E-utility
    public static final String Attr_UID = "uid";                 // Atributo en el DTD de E-utility
    public static final String Val_HomoSapiens = "Homo sapiens"; // Valor en el DTD de E-utility
    
    public static final String RET_MAX = "10000";         // Número máximo de registros en el query de esearch
    public static final String Token_DbNAME = "@db_";
    public static final String Token_GENE = "@gene_";     // Mock up para el nombre del gen en el query de esearch
    public static final String Token_QryKEY = "@qrykey_";
    public static final String Token_WebENV = "@webenv_";
    public static final String Token_LY = "@ly_";
    public static final String Token_LM = "@lm_";
    public static final String Token_LD = "@ld_";
    public static final String Token_UY = "@uy_";
    public static final String Token_UM = "@um_";
    public static final String Token_UD = "@ud_";
    
    // ESearch (text searches)
    // eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi
    public static final String CMD_ESearch  = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db="+Token_DbNAME+"&term=(("+Token_GENE+"%5BGene%20Name%5D)%20AND%20homo%20sapiens%5BOrganism%5D)%20AND%20alive%5Bprop%5D&usehistory=y&retmode=xml&retmax="+RET_MAX;
    // ESummary (document summary downloads)
    // eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi
    public static final String CMD_ESummary  = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db="+Token_DbNAME+"&query_key="+Token_QryKEY+"&WebEnv="+Token_WebENV+"&retmode=xml";
    
    public static final String CMD_ESearchD = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db="+Token_DbNAME+"&term=("+Token_GENE+")%20AND%20%22diseases%22%5BFilter%5D&usehistory=y&retmode=xml&retmax="+RET_MAX;
    public static final String CMD_ESummaryD = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db="+Token_DbNAME+"&query_key="+Token_QryKEY+"&WebEnv="+Token_WebENV+"&retmode=xml";
    
    public static final String CMD_ESearchL = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db="+Token_DbNAME+"&term="+Token_GENE+"&usehistory=y&retmode=xml&retmax="+RET_MAX;
    public static final String CMD_ESummaryL = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db="+Token_DbNAME+"&query_key="+Token_QryKEY+"&WebEnv="+Token_WebENV+"&retmode=xml";
    
    public static final String CMD_ESearchP = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db="+Token_DbNAME+"&term="+Token_GENE+"%20%5BAll%20Fields%5D%20AND%20(%22"+Token_LY+"%2F"+Token_LM+"%2F"+Token_LD+"%22%5BPDat%5D%20:%20%22"+Token_UY+"%2F"+Token_UM+"%2F"+Token_UD+"%22%5BPDat%5D%20AND%20%22humans%22%5BMeSH%20Terms%5D)&usehistory=y&retmode=xml&retmax="+RET_MAX;
    public static final String CMD_EFetch = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db="+Token_DbNAME+"&query_key="+Token_QryKEY+"&WebEnv="+Token_WebENV+"&retmode=xml";
    public static final String Url_NBCI = "http://www.ncbi.nlm.nih.gov/";
    
     /**
     * Toma la url CMD_ESearchP y remplaza los tokens <code>Token_LY</code>, <code>Token_LM</code>,
     * <code>Token_LD</code>, <code>Token_UY</code>, <code>Token_UM</code> y <code>Token_UD</code>
     * por los valores de año, mes y día entre la fecha del día de hoy y la misma fecha pero
     * <code>ellapsedYears</code> años atrás
     * @param ellapsedYears El parámetro ellapsedYears define el número de años
     * atrás a partir de la fecha actual para realizar una búsqueda
     * @return Un string que representa <code>CMD_ESearchP</code> con los parámetros del query
     * correspondientes a las fechas de consulta
     */
    private String getEllapsedTimeQuery(int ellapsedYears) {
        GregorianCalendar tq = new GregorianCalendar();
        String cmd = CMD_ESearchP;
        cmd = cmd.replaceFirst(Token_UY, Integer.toString(tq.get(Calendar.YEAR)))
                .replaceFirst(Token_UM, Integer.toString(tq.get(Calendar.MONTH)+1))
                .replaceFirst(Token_UD, Integer.toString(tq.get(Calendar.DATE)));
        tq.add(Calendar.YEAR, -ellapsedYears);
        cmd = cmd.replaceFirst(Token_LY, Integer.toString(tq.get(Calendar.YEAR)))
                .replaceFirst(Token_LM, Integer.toString(tq.get(Calendar.MONTH)+1))
                .replaceFirst(Token_LD, Integer.toString(tq.get(Calendar.DATE)));
        return cmd;
    }
    
    /**
     * Contruye y devuelve un objeto Json que contiene la información básica de un gen.
     * @param geneName El parámetro geneName efine el nombre del gen, por ejemplo:
     * "SF3B1".
     * @return El objeto JSON con la información básica del genUn string que representa <code>CMD_ESearchP</code> con los parámetros del query
     * correspondientes a las fechas de consulta.
     * @throws com.nanopharmacia.eutility.impl.NoDataException
     * @throws com.nanopharmacia.eutility.impl.UseHistoryException
     * @throws java.net.MalformedURLException
     * @throws java.net.ProtocolException
     * @throws java.io.IOException
     */
    public JSONObject getGeneInfo(final String geneName)
            throws NoDataException, UseHistoryException, MalformedURLException
            , ProtocolException, IOException
    {
        JSONObject gene = null;
        Element docSum;
        docSum = getGeneDom(geneName);
        if(docSum != null) {
            JSONObject geneData = new JSONObject();
            try {
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
                }catch(JSONException jse) {
                    Logger.getLogger(ESearchImpl.class.getName()).log(Level.SEVERE, null, jse);
                    throw new NoDataException("no se pudo crear el objeto json");
                }
            }catch(JSONException jse) {
                Logger.getLogger(ESearchImpl.class.getName()).log(Level.SEVERE, null, jse);
            }/*finally {
                
            }*/
        }
        return gene;
    }
    
    /**
     * Entrega un JDOM Elemento que representa la información básica de un gen 
     * relacionada unicamente con organismos humandos. Esta información es obtenida 
     * usando el sistema de consultas Entrez de la NCBI.
     * @param geneName El parámetro geneName define el nombre del gen, por ejemplo:
     * "SF3B1".
     * @return El objeto JSON con la información básica del gen.
     * @throws com.nanopharmacia.eutility.impl.NoDataException
     * @throws com.nanopharmacia.eutility.impl.UseHistoryException
     * @throws java.net.MalformedURLException
     * @throws java.net.ProtocolException
     * @throws java.io.IOException
     */
    public Element getGeneDom(final String geneName)
            throws NoDataException, UseHistoryException, MalformedURLException
            , ProtocolException, IOException
    {
        Element res = null;
        Document doc;
        URL cmd;
        String spec;
        HttpURLConnection conex;
        spec = CMD_ESearch.replaceFirst(Token_DbNAME, Db_GENE);
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
        }catch(JDOMException jde) {
            doc = null;
        }finally {
            conex.disconnect();
        }
        
        if(doc!=null)
        {
            Element elem;
            XPath lXPath;
            String qryKey, webEnv;
            try {
                lXPath = XPath.newInstance(Elem_SrhRES+"/"+Elem_QryKEY);
                elem = (Element)lXPath.selectSingleNode(doc);
                if( elem==null ) {
                    throw new UseHistoryException("no se encontraron los datos para el parametro de consulta: queryKey");
                }
                qryKey = elem.getValue();
                lXPath = XPath.newInstance(Elem_SrhRES+"/"+Elem_WebENV);
                elem = (Element)lXPath.selectSingleNode(doc);
                if(elem==null) {
                    throw new UseHistoryException("no se encontraron los datos para el parametro de consulta: WebEnv");
                }
                webEnv = elem.getValue();
            }catch(JDOMException jde) {
                qryKey = null;
                webEnv = null;
            }
            
            if(qryKey==null || webEnv==null) {
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
            }catch(JDOMException jde) {
                doc = null;
            }finally {
                conex.disconnect();
            }
            
            if(doc!=null)
            {
                try {
                    lXPath = XPath.newInstance("//"+Elem_DocSummary+"["+Elem_NAME+"=\""+geneName+"\" and "+Elem_ORGANISM+"/"+Elem_SciNAME+"=\""+Val_HomoSapiens+"\"]" );
                    res = (Element)lXPath.selectSingleNode(doc);
//                    if( res==null ) {
//                        throw new NoDataException("no se encontro un elemento docsummary para el gen con nombre "+geneName+" y organismo Homo sapiens");
//                    }
                }catch(JDOMException jde) {
                    throw new NoDataException("no se encontro un elemento docsummary para el gen con nombre "+geneName+" y organismo Homo sapiens");
                }
            } // if esummary
        } // if esearch
        return res;
    }
    
    /**
     * Contruye y devuelve un arreglo Json que contiene la información sobre
     * enfermedades y síndromes relacionados con un gen. Esta información es obtenida
     * usando el método getDiseasesDom(String).
     * @param geneName El parámetro geneName define el nombre del gen, por ejemplo:
     * "SF3B1".
     * @return El arreglo JSON con la lista de enfermedades y síndromes relacionados
     * con dicho gen.
     * @throws com.nanopharmacia.eutility.impl.NoDataException
     * @throws com.nanopharmacia.eutility.impl.UseHistoryException
     * @throws java.net.MalformedURLException
     * @throws java.net.ProtocolException
     * @throws java.io.IOException
     */
    public JSONArray getDiseasesInfo(final String geneName)
            throws NoDataException, UseHistoryException, MalformedURLException
            , ProtocolException, IOException
    {
        //JSONObject data = new JSONObject();
        JSONArray diseases = new JSONArray();
        Element docSumSet;
        docSumSet = getDiseasesDom(geneName);
        if(docSumSet != null) {
            JSONObject alteration;
            
            List<Element> docSumList = docSumSet.getChildren("DocumentSummary");
            for(Element docSum : docSumList) {
                alteration = new JSONObject();
                /*List itChilds = docSum.getChildren();
                Iterator itChilds1 = itChilds.iterator();
                System.out.println("------------------------");
                while (itChilds1.hasNext()) {
                    Object next = itChilds1.next();
                    System.out.println("child: " + next.toString());
                }*/
                try {
                    alteration.put("title", docSum.getChildText("Title"));
                    alteration.put("definition", docSum.getChildText("Definition"));
                    alteration.put("conceptId", docSum.getChildText("ConceptId"));
                    diseases.put(alteration);
                }catch(JSONException jse) {
                    Logger.getLogger(ESearchImpl.class.getName()).log(Level.SEVERE, null, jse);
                }
            }
            /*try {
                data.put("alterations", alterations);
            }catch(JSONException jse) {
                throw new NoDataException("no se pudo crear el objeto json");
            }*/
        }
        return diseases;
    }
    
    /**
     * Entrega un JDOM Elemento que representa la información sobre enfermedades y
     * síndromes relacionados con un gen. Esta información es obtenida usando el 
     * sistema de consultas Entrez de la NCBI.
     * @param geneName El parámetro geneName define el nombre del gen, por ejemplo:
     * "SF3B1".
     * @return Un elemento JDOM con la estructura <em>DocumentSummarySet</em> que 
     * contiene la lista de enfermedades y síndromes relacionadas con el gen en 
     * cuestión.
     * @throws com.nanopharmacia.eutility.impl.NoDataException
     * @throws com.nanopharmacia.eutility.impl.UseHistoryException
     * @throws java.net.MalformedURLException
     * @throws java.net.ProtocolException
     * @throws java.io.IOException
     */
    public Element getDiseasesDom(final String geneName)
            throws NoDataException, UseHistoryException, MalformedURLException
            , ProtocolException, IOException
    {
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
        }catch(JDOMException jde) {
            doc = null;
        }finally {
            conex.disconnect();
        }
        
        if(doc!=null)
        {
            Element elem;
            XPath lXPath;
            String qryKey, webEnv;
            try {
                lXPath = XPath.newInstance(Elem_SrhRES+"/"+Elem_QryKEY);
                elem = (Element)lXPath.selectSingleNode(doc);
                if( elem==null ) {
                    throw new UseHistoryException("no se encontraron los datos para el parametro de consulta: queryKey=");
                }
                qryKey = elem.getValue();
                lXPath = XPath.newInstance(Elem_SrhRES+"/"+Elem_WebENV);
                elem = (Element)lXPath.selectSingleNode(doc);
                if(elem==null) {
                    throw new UseHistoryException("no se encontraron los datos para el parametro de consulta: WebEnv=");
                }
                webEnv = elem.getValue();
            }catch(JDOMException jde) {
                qryKey = null;
                webEnv = null;
            }
////////////////////////////////////////////////////////////////////////////////            
            if(qryKey==null || webEnv==null) {
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
            }catch(JDOMException jde) {
                doc = null;
            }finally {
                conex.disconnect();
            }
            
            if(doc!=null)
            {
                try {
                    lXPath = XPath.newInstance("//DocumentSummarySet");
                    res = (Element)lXPath.selectSingleNode(doc);
                    if( res==null ) {
                        throw new NoDataException("no se encontraron elementos DocumentSummary para el gen con nombre "+geneName);
                    }

                }catch(JDOMException jde) {
                    throw new NoDataException("no se encontro un elemento docsummary para el gen con nombre "+geneName+" y organismo Homo sapiens");
                }
            } // if esummary
        } // if esearch
        return res;
    }
    
    /**
     * Contruye y devuelve un arreglo Json que contiene la información sobre
     * pruebas de laboratorio relacionadas con un gen. Esta información es obtenida
     * usando el método getTestingLabDom(String).
     * @param geneName El parámetro geneName define el nombre del gen, por ejemplo:
     * "SF3B1".
     * @return El arreglo JSON con la lista de pruebas de laboratorio relacionadas
     * con dicho gen.
     * @throws com.nanopharmacia.eutility.impl.NoDataException
     * @throws com.nanopharmacia.eutility.impl.UseHistoryException
     * @throws java.net.MalformedURLException
     * @throws java.net.ProtocolException
     * @throws java.io.IOException
     */
    public JSONArray getTestingLabInfo(final String geneName)
            throws NoDataException, UseHistoryException, MalformedURLException
            , ProtocolException, IOException
    {
        List<Element> conditionLst;
        JSONArray dataLst = new JSONArray();
        Element docSumSet;
        docSumSet = getTestingLabDom(geneName);
        if(docSumSet != null) {
            JSONArray conds;
            JSONObject aux;
            
            List<Element> docSumList = docSumSet.getChildren("DocumentSummary");
            for(Element docSum : docSumList) {
                aux = new JSONObject();
                try {
                    aux.put("testName", docSum.getChildText("TestName"));
                    conditionLst = docSum.getChild("ConditionList").getChildren("Condition");
                    conds = new JSONArray();
                    for(Element cond : conditionLst) {
                        conds.put(cond.getChildText("Name"));
                    }
                    aux.put("ConditionList", conds);
                    dataLst.put(aux);
                }catch(JSONException jse) {
                    Logger.getLogger(ESearchImpl.class.getName()).log(Level.SEVERE, null, jse);
                }
                
            }
        }
        return dataLst;
    }
    
    /**
     * Entrega un JDOM Elemento que representa la información sobre pruebas de 
     * laboratorio relacionadas con un gen. Esta información es obtenida usando el 
     * sistema de consultas Entrez de la NCBI.
     * @param geneName El parámetro geneName define el nombre del gen, por ejemplo:
     * "SF3B1".
     * @return Un elemento JDOM con la estructura <em>DocumentSummarySet</em> que 
     * contiene la lista de pruebas de laboratorio relacionadas con el gen en 
     * cuestión.
     * @throws com.nanopharmacia.eutility.impl.NoDataException
     * @throws com.nanopharmacia.eutility.impl.UseHistoryException
     * @throws java.net.MalformedURLException
     * @throws java.net.ProtocolException
     * @throws java.io.IOException
     */
    public Element getTestingLabDom(final String geneName)
            throws NoDataException, UseHistoryException, MalformedURLException
            , ProtocolException, IOException
    {
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
        }catch(JDOMException jde) {
            doc = null;
        }finally {
            conex.disconnect();
        }
        if(doc!=null)
        {
            Element elem;
            List<Element> idList = null;
            XPath lXPath;
            String qryKey, webEnv;
            try {
                lXPath = XPath.newInstance(Elem_SrhRES+"/"+Elem_QryKEY);
                elem = (Element)lXPath.selectSingleNode(doc);
                if( elem==null ) {
                    throw new UseHistoryException("no se encontraron los datos para el parametro de consulta: queryKey");
                }
                qryKey = elem.getValue();
                
                lXPath = XPath.newInstance(Elem_SrhRES+"/"+Elem_WebENV);
                elem = (Element)lXPath.selectSingleNode(doc);
                if(elem==null) {
                    throw new UseHistoryException("no se encontraron los datos para el parametro de consulta: WebEnv=");
                }
                webEnv = elem.getValue();
                
                lXPath = XPath.newInstance("//Id");
                idList = lXPath.selectNodes(doc);
            }catch(JDOMException jde) {
                qryKey = null;
                webEnv = null;
            }
////////////////////////////////////////////////////////////////////////////////            
            if(qryKey==null || webEnv==null) {
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
            }catch(JDOMException jde) {
                doc = null;
            }finally {
                conex.disconnect();
            }
            if(doc!=null)
            {
                try {
                    lXPath = XPath.newInstance("//DocumentSummarySet");
                    elem = (Element)lXPath.selectSingleNode(doc);
                    if( elem==null ) {
                        throw new NoDataException("no se encontraron elementos DocumentSummary para el gen con nombre "+geneName);
                    }
                    
                    List<String> ids = getValues(idList);
                    List<Element> docSumList = elem.getChildren("DocumentSummary");
                    for(Element docSum : docSumList) {
                        String uid = docSum.getAttributeValue("uid");
                        if(ids.contains(uid)) {
                            res.addContent((Element)docSum.clone());
                            ids.remove(uid);
                        }
                    }
//XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
//String xmlString = outputter.outputString(res);
//System.out.println("\nres="+xmlString);
                }catch(JDOMException jde) {
                    throw new NoDataException("no se encontro un elemento docsummary para el gen con nombre "+geneName+" y organismo Homo sapiens");
                }
            } // if esummary
        } // if esearch
        return res;
    }
    
    /**
     * Contruye y devuelve un objeto Json que contiene la información sobre las
     * publicaciones médicas clasificadas en dos criterios. Las publicaciones que
     * no cumplen con una relevancia mayor a cero y las que tienen cierta relevancia.
     * Para aquellas publicaciones o artículos con una relevancia mayor a cero, 
     * incluyen las propiedad <em>pmid</em> con el valor del identificador del 
     * artículo, y la propiedad “ranking” con el valor de la relevancia correspondiente.
     * Además, se encuentran en el arreglo <em>outstanding</em>.
     * Los artículos con una relevancia de valor cero, se encuentran en un arreglo 
     * de nombre <em>rejected</em> el cual contiene el identificadore de cada
     * uno de estos artículos.
     * @param geneName El parámetro geneName define el nombre del gen en cuestión, 
     * por ejemplo: SF3B1.
     * @param molecularAlt La alteración molecular relacionada con el gen en cuestión
     * @param ellapsedYears El número de años hacia atrás para realizar la búsqueda.
     * La búsqueda comprende a partir de un día como el actual este valor de años
     * atrás hasta el día actual.
     * @return El objeto JSON con la información básica del genUn string que 
     * representa <code>CMD_ESearchP</code> con los parámetros del query
     * correspondientes a las fechas de consulta.
     * @throws com.nanopharmacia.eutility.impl.NoDataException
     * @throws com.nanopharmacia.eutility.impl.UseHistoryException
     * @throws java.net.MalformedURLException
     * @throws java.net.ProtocolException
     * @throws java.io.IOException
     */
    public JSONObject getPublicationsInfo(final String geneName, final String molecularAlt, int ellapsedYears)
            throws NoDataException, UseHistoryException, MalformedURLException
            , ProtocolException, IOException
    {
        List<Element> abstractLst, authorLst;
        JSONObject publications = new JSONObject();
        JSONArray outstanding = new JSONArray();
        JSONArray rejected = new JSONArray();
        Element docSumSet;
        docSumSet = getPublicationsDom(geneName, ellapsedYears);
        long startTime = System.currentTimeMillis();
        if(docSumSet != null) {
            Element journal, issue;
            JSONArray abstracts;
            JSONObject aux, abstrct;
            int rank, rankMax;
            String text, pmid;
            StringBuilder r;
            
            List<Element> pubmedArtList = docSumSet.getChildren("PubmedArticle");
            for(Element pubmedArt : pubmedArtList) {
                if(pubmedArt.getChild("MedlineCitation").getChild("Article").getChild("Abstract")==null) {
                    continue;
                }
                rankMax=0;
                abstractLst = pubmedArt.getChild("MedlineCitation").getChild("Article")
                        .getChild("Abstract").getChildren("AbstractText");
                
                pmid = pubmedArt.getChild("MedlineCitation").getChildText("PMID");
                aux = new JSONObject();
                try {
                    abstracts = new JSONArray();
                    for(Element abs : abstractLst) {
                        abstrct = new JSONObject();
                        abstrct.put("label", abs.getAttributeValue("Label")==null?"Unlabeled":abs.getAttributeValue("Label"));
                        text = abs.getValue();
                        abstrct.put("text", text);
                        rank = Utils.getRanking(text, geneName, molecularAlt);
                        abstracts.put(abstrct);
                        if(rank > rankMax) {
                            rankMax = rank;
                        }
                    }
                    if(rankMax>0) {
                        aux.put("pmid", pmid);
                        aux.put("ranking", rankMax);
                        aux.put("abstract", abstracts);
                        aux.put("articleTitle", pubmedArt.getChild("MedlineCitation").getChild("Article")
                                .getChildText("ArticleTitle"));

                        aux.put("url", Url_NBCI+Db_PUBMED+"/"+pmid);

                        r = new StringBuilder();
                        authorLst = pubmedArt.getChild("MedlineCitation").getChild("Article").getChild("AuthorList").getChildren("Author");
                        for(Element author : authorLst) {
                            r.append(author.getChildText("LastName")).append(", ").append(author.getChildText("Initials")).append("., ");
                        }
                        journal = pubmedArt.getChild("MedlineCitation").getChild("Article").getChild("Journal");
                        issue = journal.getChild("JournalIssue");
                        r.append("(").append(issue.getChild("PubDate").getChildText("Year")).append("). ");
                        r.append(journal.getChildText("Title"));
                        r.append(". ISSN:").append(journal.getChildText("ISSN"));
                        r.append(", vol.").append(issue.getChildText("Volume"));
                        r.append(", issue ").append(issue.getChildText("Issue")).append(". ");
                        r.append(issue.getChild("PubDate").getChildText("Month"));
                        r.append(issue.getChild("PubDate").getChildText("Day")==null
                                ?""
                                :
                                " "+issue.getChild("PubDate").getChildText("Day"));

                        aux.put("reference", r.toString());
                        outstanding.put(aux);
                    }else {
                        rejected.put(new JSONObject().put("pmid", pmid));
                    }
                }catch(Exception jse) {
                    Logger.getLogger(ESearchImpl.class.getName()).log(Level.SEVERE, null, jse);
                }
            } // for
        }
        try {
            publications.put("outstanding", outstanding);
            publications.put("rejected", rejected);
        }catch(Exception jse) {
            Logger.getLogger(ESearchImpl.class.getName()).log(Level.SEVERE, null, jse);
        }
         long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        System.out.println("Creación del JSON de Publicaciones: " + elapsedTime);
        return publications;
    }
    
    /**
     * Entrega un JDOM Elemento que representa la información sobre publicaciones 
     * médicas relacionadas con un gen. Esta información es obtenida usando el 
     * sistema de consultas Entrez de la NCBI.
     * @param geneName El parámetro geneName define el nombre del gen, por ejemplo:
     * "SF3B1".
     * @param ellapsedYears El número de años hacia atrás para realizar la búsqueda.
     * La búsqueda comprende a partir de un día como el actual este valor de años
     * atrás hasta el día actual.
     * @return Un elemento JDOM con la estructura <em>PubmedArticleSet</em> que 
     * contiene la publicaciones médicas relacionadas con el gen en 
     * cuestión.
     * @throws com.nanopharmacia.eutility.impl.NoDataException
     * @throws com.nanopharmacia.eutility.impl.UseHistoryException
     * @throws java.net.MalformedURLException
     * @throws java.net.ProtocolException
     * @throws java.io.IOException
     */
    public Element getPublicationsDom(final String geneName, int ellapsedYears)
            throws NoDataException, UseHistoryException, MalformedURLException
            , ProtocolException, IOException
    {
        long startTime = System.currentTimeMillis();
        Element res = new Element("PubmedArticleSet");
        Document doc;
        URL cmd;
        HttpURLConnection conex;
        String spec;
        spec = getEllapsedTimeQuery(ellapsedYears);
        spec = spec.replaceFirst(Token_DbNAME, Db_PUBMED);
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
        }catch(JDOMException jde) {
            doc = null;
        }finally {
            conex.disconnect();
        }
        if(doc!=null)
        {
            Element elem;
            List<Element> idList = null;
            XPath lXPath;
            String qryKey, webEnv;
            try {
                lXPath = XPath.newInstance(Elem_SrhRES+"/"+Elem_QryKEY);
                elem = (Element)lXPath.selectSingleNode(doc);
                if( elem==null ) {
                    throw new UseHistoryException("no se encontraron los datos para el parametro de consulta: queryKey");
                }
                qryKey = elem.getValue();
                
                lXPath = XPath.newInstance(Elem_SrhRES+"/"+Elem_WebENV);
                elem = (Element)lXPath.selectSingleNode(doc);
                if(elem==null) {
                    throw new UseHistoryException("no se encontraron los datos para el parametro de consulta: WebEnv=");
                }
                webEnv = elem.getValue();
                
                lXPath = XPath.newInstance("//Id");
                idList = lXPath.selectNodes(doc);
            }catch(JDOMException jde) {
                qryKey = null;
                webEnv = null;
            }
////////////////////////////////////////////////////////////////////////////////            
            if(qryKey==null || webEnv==null) {
                throw new UseHistoryException("entrez tal vez no reconocio la consulta, por lo que no devolvio queryKey ni WebEnv");
            }
            spec = CMD_EFetch.replaceFirst(Token_DbNAME, Db_PUBMED);
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
            }catch(JDOMException jde) {
                doc = null;
            }finally {
                conex.disconnect();
            }
            if(doc!=null)
            {
                try {
                    lXPath = XPath.newInstance("//PubmedArticleSet");
                    elem = (Element)lXPath.selectSingleNode(doc);
                    if( elem==null ) {
                        throw new NoDataException("no se encontraron elementos DocumentSummary para el gen con nombre "+geneName);
                    }
                    
                    List<String> ids = getValues(idList);
                    List<Element> pubmedArtList = elem.getChildren("PubmedArticle");
                    for(Element pubmedArt : pubmedArtList) {
                        String pmid = pubmedArt.getChild("MedlineCitation").getChildText("PMID");
                        if(ids.contains(pmid)) {
                            res.addContent((Element)pubmedArt.clone());
                            ids.remove(pmid);
                        }
                    }
//XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
//String xmlString = outputter.outputString(res);
//System.out.println("\nres="+xmlString);
                }catch(JDOMException jde) {
                    throw new NoDataException("no se encontro un elemento docsummary para el gen con nombre "+geneName+" y organismo Homo sapiens");
                }
            } // if esummary
        } // if esearch
         long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        System.out.println("Extracción de info de Publicaciones: " + elapsedTime);
        return res;
    }
    
    /**
     * Devuelve una lista con los valores contenidos dentro de otra lista de
     * elementos JDOM.
     * @param children El parámetro children define la lista de elementos a los
     * que se les extraerá su valor contenido y se agregará a una nueva lista.
     * @return La lista de valores contenidos en la lista de elementos hijos de
     * alguna jerarquía mayor.
     * @throws com.nanopharmacia.eutility.impl.NoDataException
     * @throws com.nanopharmacia.eutility.impl.UseHistoryException
     * @throws java.net.MalformedURLException
     * @throws java.net.ProtocolException
     * @throws java.io.IOException
     */
    private List<String> getValues(List<Element> children) {
        List values = new ArrayList(children.size());
        for(Element e : children) {
            values.add(e.getValue());
        }
        return values;
    }
}
