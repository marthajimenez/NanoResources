package org.nanopharmacy.eutility.impl;

/**
 * La clase {@code UseHistoryException} indica condiciones para las no se logran 
 * interpretar los valores clave de consulta incluidos en el parámetro WebEnv del
 * query de una consulta a Entrez. 
 * @author carlos.ramos
 * @version 07/10/2015
 */
public class UseHistoryException extends Exception{
    /**
     * Construye una nueva excepción con mensaje detallado {@code null}.
     */
    public UseHistoryException() {
        super();
    }
    
    /**
     * Construye una nueva excepción con un mensaje específico detallado.
     *
     * @param   message   el mensaje detallado. El mensaje detallado se guarda
     * para su posterior recuperación por el método {@link #getMessage()}.
     */
    public UseHistoryException(String message) {
        super(message);
    }   
}
