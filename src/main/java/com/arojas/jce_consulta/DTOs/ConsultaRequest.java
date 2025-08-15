/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.arojas.jce_consulta.DTOs;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO para peticiones de consulta de datos en la JCE.
 * 
 * Este objeto encapsula los datos necesarios para realizar una consulta
 * de información ciudadana en el portal de la Junta Central Electoral.
 * 
 * @author A. Rojas
 * @version 1.0.0
 */
@Schema(description = "Datos necesarios para realizar una consulta en la JCE", example = """
    {
        "cedula": "00112345671",
        "incluirFoto": true,
        "formato": "completo"
    }
    """)
public record ConsultaRequest(

    @Schema(description = """
        Número de cédula de identidad dominicana.
        Puede incluir o no los guiones separadores.
        Debe tener exactamente 11 dígitos numéricos.

        Formato válido: XXXYYYYYYYYZ donde:
        - XXX: Código del municipio (001-999)
        - YYYYYYY: Número secuencial (0000001-9999999)
        - Z: Dígito verificador (0-9)
        """, example = "00112345671", requiredMode = Schema.RequiredMode.REQUIRED, pattern = "^\\d{3}-?\\d{7}-?\\d{1}$") @NotNull(message = "La cédula es obligatoria") @NotBlank(message = "La cédula no puede estar vacía") @Pattern(regexp = "^\\d{3}-?\\d{7}-?\\d{1}$", message = "La cédula debe tener el formato XXX-XXXXXXX-X o XXXXXXXXXXX (11 dígitos)") @Size(min = 11, max = 13, message = "La cédula debe tener entre 11 y 13 caracteres") @JsonProperty("cedula") String cedula,

    @Schema(description = """
        Indica si se debe incluir la URL de la foto de la cédula en la respuesta.
        Por defecto es true. Si es false, no se incluirá información de foto
        para optimizar el tiempo de respuesta.
        """, example = "true", defaultValue = "true") @JsonProperty("incluirFoto") Boolean incluirFoto,

    @Schema(description = """
        Formato de respuesta deseado:
        - 'completo': Incluye todos los campos disponibles (por defecto)
        - 'basico': Solo campos esenciales (nombre, cédula, estado)
        - 'personal': Datos personales básicos
        - 'familiar': Incluye información de cónyuge y padres
        """, example = "completo", defaultValue = "completo", allowableValues = {
        "completo", "basico", "personal",
        "familiar" }) @Pattern(regexp = "^(completo|basico|personal|familiar)$", message = "El formato debe ser: completo, basico, personal o familiar") @JsonProperty("formato") String formato){

  /**
   * Constructor con valores por defecto.
   * 
   * @param cedula número de cédula obligatorio
   */
  public ConsultaRequest(String cedula) {
    this(cedula, true, "completo");
  }

  /**
   * Constructor con cédula e incluir foto.
   * 
   * @param cedula      número de cédula obligatorio
   * @param incluirFoto si incluir información de foto
   */
  public ConsultaRequest(String cedula, Boolean incluirFoto) {
    this(cedula, incluirFoto, "completo");
  }

  /**
   * Obtiene la cédula limpia sin guiones.
   * 
   * @return cédula sin guiones ni espacios
   */
  public String getCedulaLimpia() {
    if (cedula == null) {
      return null;
    }
    return cedula.replaceAll("[^\\d]", "");
  }

  /**
   * Obtiene la cédula formateada con guiones.
   * 
   * @return cédula en formato XXX-XXXXXXX-X
   */
  public String getCedulaFormateada() {
    String cedulaLimpia = getCedulaLimpia();
    if (cedulaLimpia == null || cedulaLimpia.length() != 11) {
      return cedulaLimpia;
    }

    return String.format("%s-%s-%s",
        cedulaLimpia.substring(0, 3),
        cedulaLimpia.substring(3, 10),
        cedulaLimpia.substring(10, 11));
  }

  /**
   * Obtiene el código del municipio de la cédula.
   * 
   * @return código del municipio (primeros 3 dígitos)
   */
  public String getCodigoMunicipio() {
    String cedulaLimpia = getCedulaLimpia();
    if (cedulaLimpia == null || cedulaLimpia.length() < 3) {
      return null;
    }
    return cedulaLimpia.substring(0, 3);
  }

  /**
   * Obtiene el número secuencial de la cédula.
   * 
   * @return número secuencial (dígitos 4-10)
   */
  public String getNumeroSecuencial() {
    String cedulaLimpia = getCedulaLimpia();
    if (cedulaLimpia == null || cedulaLimpia.length() < 10) {
      return null;
    }
    return cedulaLimpia.substring(3, 10);
  }

  /**
   * Obtiene el dígito verificador de la cédula.
   * 
   * @return dígito verificador (último dígito)
   */
  public String getDigitoVerificador() {
    String cedulaLimpia = getCedulaLimpia();
    if (cedulaLimpia == null || cedulaLimpia.length() != 11) {
      return null;
    }
    return cedulaLimpia.substring(10, 11);
  }

  /**
   * Valida si la cédula tiene el formato correcto.
   * 
   * @return true si la cédula es válida
   */
  public boolean esCedulaValida() {
    String cedulaLimpia = getCedulaLimpia();
    return cedulaLimpia != null &&
        cedulaLimpia.length() == 11 &&
        cedulaLimpia.matches("\\d{11}");
  }

  /**
   * Obtiene el valor por defecto para incluir foto si es null.
   * 
   * @return true si incluirFoto es null, sino el valor actual
   */
  public boolean getIncluirFoto() {
    return incluirFoto != null ? incluirFoto : true;
  }

  /**
   * Obtiene el formato por defecto si es null o vacío.
   * 
   * @return "completo" si formato es null/vacío, sino el valor actual
   */
  public String getFormato() {
    return (formato == null || formato.isBlank()) ? "completo" : formato.toLowerCase();
  }
}