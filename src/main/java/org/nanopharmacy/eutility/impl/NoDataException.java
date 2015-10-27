package org.nanopharmacy.eutility.impl;

/**
 * La clase {@code NoDataException} indica condiciones para las que no se reciben
 * resultados de un repositorio de datos.
 * 
 * @author carlos.ramos
 * @version 07/10/2015
 */
public class NoDataException extends Exception{
    /**
     * Construye una nueva excepción con mensaje detallado {@code null}.
     */
    public NoDataException() {
        super();
    }
    
    /**
     * Construye una nueva excepción con un mensaje específico detallado.
     *
     * @param   message   el mensaje detallado. El mensaje detallado se guarda
     * para su posterior recuperación por el método {@link #getMessage()}.
     */
    public NoDataException(String message) {
        super(message);
    }
}
