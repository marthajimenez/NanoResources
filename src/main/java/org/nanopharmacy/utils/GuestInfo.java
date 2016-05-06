package org.nanopharmacy.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;

/**
 * Gestiona el almacenamiento de la informacion proporcionada por los usuarios que 
 * emitieron comentarios a traves de la aplicacion
 * @author jose.jimenez
 */
public class GuestInfo extends HttpServlet {
    
    /** Nombre del archivo de texto que almacena la informacion de los usuarios que generaron un comentario */
    private final String FILENAME = "GuestInfo.csv";
    
    /** Instancia del objeto para escribir en bitacora de la aplicacion */
    private static final Logger LOG = Logger.getLogger(GuestInfo.class.getName());

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        
        StringBuilder response = new StringBuilder(128);
        PrintWriter out = resp.getWriter();
        
        StringBuilder inRequest = new StringBuilder(128);
        Reader reader = req.getReader();
        char[] input = new char[128];
        int inBuffer = reader.read(input);
        while (inBuffer > 0) {
            inRequest.append(input, 0, inBuffer);
            inBuffer = reader.read(input);
        }
        JSONObject guestInfo = new JSONObject(inRequest.toString());
        
        if (saveInfo(guestInfo)) {
            response.append("{status: 200, msg: \"success\"}");
        } else {
            response.append("{status: 500, msg: \"error saving data\"}");
        }
        out.println(response.toString());
    }
    
    /**
     * Almacena la informacion de los usuarios que generaron un comentario en un archivo de texto
     * @param guestInfo objeto JSON que contiene la informacion del usuario a almacenar; nombre completo, especialidad,
     *                  cuenta de correo, comentario del usuario y nombre de la compañia para la que trabaja
     */
    public synchronized boolean saveInfo(JSONObject guestInfo) {
        
        boolean infoSaved = false;
        String path = Utils.getContextPath();
        File file = new File(path + "/../" + this.FILENAME);
        System.out.println("Archivo: " + file.getAbsolutePath());
        FileWriter writer = null;
        
        boolean fileExists = file.exists();
        String name = !guestInfo.isNull("name") ? guestInfo.getString("name") : "";
        String specialty = !guestInfo.isNull("specialty") ? guestInfo.getString("specialty") : "";
        String company = !guestInfo.isNull("company") ? guestInfo.getString("company") : "";
        String email = !guestInfo.isNull("email") ? guestInfo.getString("email") : "";
        String comment = !guestInfo.isNull("comment") ? guestInfo.getString("comment") : "";
        try {
            writer = new FileWriter(file, true);
            if (!name.isEmpty() && !email.isEmpty()) {
                String comment2Write = comment.replaceAll("\n", " ").replaceAll("\r", "").replaceAll("\"", "'");
                StringBuilder textLine = new StringBuilder(256);
                if (!fileExists) {
                    textLine.append("\"Name\",\"Specialty\",\"Company\",\"E-mail\",\"Comments\"\n");
                }
                textLine.append("\"");
                textLine.append(name);
                textLine.append("\",\"");
                textLine.append(specialty);
                textLine.append("\",\"");
                textLine.append(company);
                textLine.append("\",\"");
                textLine.append(email);
                textLine.append("\",\"");
                textLine.append(comment2Write);
                textLine.append("\"\n");
                writer.append(textLine);
                System.out.println("Se almaceno en archivo:\n" + textLine.toString());
            }
            writer.flush();
            writer.close();
            infoSaved = true;
        } catch (IOException ioe) {
            GuestInfo.LOG.log(Level.SEVERE, "En la escritura de los datos de los usuarios invitados", ioe);
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ioe2) {
                }
            }
        }
        return infoSaved;
    }
}
