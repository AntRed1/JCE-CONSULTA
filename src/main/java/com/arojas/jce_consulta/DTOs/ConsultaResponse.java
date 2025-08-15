/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.arojas.jce_consulta.DTOs;

import java.time.LocalDateTime;

import com.arojas.jce_consulta.DTOs.ConsultaResponse.DatosCiudadano;
import com.arojas.jce_consulta.DTOs.ConsultaResponse.InformacionFoto;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO para respuestas de consulta de datos en la JCE.
 * 
 * Este objeto encapsula la respuesta completa de una consulta exitosa
 * o fallida al portal de la Junta Central Electoral, incluyendo
 * metadatos de la operación y datos del ciudadano cuando están disponibles.
 * 
 * @author A. Rojas
 * @version 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Respuesta completa de una consulta en la JCE con metadatos y datos ciudadanos")
public record ConsultaResponse(

    // ========================================
    // METADATOS DE LA RESPUESTA
    // ========================================

    @Schema(description = "Indica si la consulta fue exitosa", example = "true") @JsonProperty("exitosa") Boolean exitosa,

    @Schema(description = "Mensaje descriptivo del resultado de la consulta", example = "Consulta realizada exitosamente") @JsonProperty("mensaje") String mensaje,

    @Schema(description = "Código de estado de la operación", example = "SUCCESS") @JsonProperty("codigo") String codigo,

    @Schema(description = "Timestamp de cuando se realizó la consulta", example = "2024-12-15T14:30:45") @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") @JsonProperty("timestamp") LocalDateTime timestamp,

    @Schema(description = "Tiempo de respuesta de la consulta en milisegundos", example = "1250") @JsonProperty("tiempoRespuesta") Long tiempoRespuesta,

    @Schema(description = "Cédula consultada (formateada con guiones)", example = "001-1234567-1") @JsonProperty("cedulaConsultada") String cedulaConsultada,

    // ========================================
    // DATOS CIUDADANOS
    // ========================================

    @Schema(description = "Datos personales del ciudadano (cuando la consulta es exitosa)") @JsonProperty("datos") DatosCiudadano datos,

    // ========================================
    // INFORMACIÓN DE FOTO
    // ========================================

    @Schema(description = "Información de la foto de cédula (opcional)") @JsonProperty("foto") InformacionFoto foto

) {

  /**
   * Constructor para respuesta exitosa completa.
   */
  public static ConsultaResponse exitosa(
      String cedulaConsultada,
      DatosCiudadano datos,
      InformacionFoto foto,
      Long tiempoRespuesta) {
    return new ConsultaResponse(
        true,
        "Consulta realizada exitosamente",
        "SUCCESS",
        LocalDateTime.now(),
        tiempoRespuesta,
        cedulaConsultada,
        datos,
        foto);
  }

  /**
   * Constructor para respuesta exitosa sin foto.
   */
  public static ConsultaResponse exitosa(
      String cedulaConsultada,
      DatosCiudadano datos,
      Long tiempoRespuesta) {
    return exitosa(cedulaConsultada, datos, null, tiempoRespuesta);
  }

  /**
   * Constructor para respuesta de error.
   */
  public static ConsultaResponse error(
      String mensaje,
      String codigo,
      String cedulaConsultada,
      Long tiempoRespuesta) {
    return new ConsultaResponse(
        false,
        mensaje,
        codigo,
        LocalDateTime.now(),
        tiempoRespuesta,
        cedulaConsultada,
        null,
        null);
  }

  /**
   * Datos personales completos del ciudadano.
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @Schema(description = "Información personal completa del ciudadano")
  public record DatosCiudadano(

      // Información básica
      @Schema(description = "Nombres completos", example = "JUAN CARLOS") @JsonProperty("nombres") String nombres,

      @Schema(description = "Primer apellido", example = "RODRIGUEZ") @JsonProperty("primerApellido") String primerApellido,

      @Schema(description = "Segundo apellido", example = "MARTINEZ") @JsonProperty("segundoApellido") String segundoApellido,

      @Schema(description = "Nombre completo concatenado", example = "JUAN CARLOS RODRIGUEZ MARTINEZ") @JsonProperty("nombreCompleto") String nombreCompleto,

      // Fechas importantes
      @Schema(description = "Fecha de nacimiento", example = "1985-03-15") @JsonProperty("fechaNacimiento") String fechaNacimiento,

      @Schema(description = "Lugar de nacimiento", example = "SANTO DOMINGO") @JsonProperty("lugarNacimiento") String lugarNacimiento,

      @Schema(description = "Fecha de expiración de la cédula", example = "2029-03-15") @JsonProperty("fechaExpiracion") String fechaExpiracion,

      // Información demográfica
      @Schema(description = "Sexo", example = "M", allowableValues = {
          "M", "F" }) @JsonProperty("sexo") String sexo,

      @Schema(description = "Estado civil", example = "SOLTERO") @JsonProperty("estadoCivil") String estadoCivil,

      @Schema(description = "Edad calculada", example = "39") @JsonProperty("edad") String edad,

      // Nacionalidad y ubicación
      @Schema(description = "Código de nacionalidad", example = "1") @JsonProperty("codigoNacionalidad") String codigoNacionalidad,

      @Schema(description = "Descripción de nacionalidad", example = "DOMINICANA") @JsonProperty("nacionalidad") String nacionalidad,

      @Schema(description = "Municipio donde se emitió la cédula", example = "DISTRITO NACIONAL") @JsonProperty("municipioCedula") String municipioCedula,

      @Schema(description = "Número de secuencia de la cédula", example = "1234567") @JsonProperty("secuenciaCedula") String secuenciaCedula,

      // Información laboral y personal
      @Schema(description = "Ocupación declarada", example = "INGENIERO") @JsonProperty("ocupacion") String ocupacion,

      // Información familiar
      @Schema(description = "Nombre del cónyuge", example = "MARIA FERNANDEZ") @JsonProperty("conyugue") String conyugue,

      @Schema(description = "Cédula del cónyuge", example = "001-7654321-9") @JsonProperty("cedulaConyugue") String cedulaConyugue,

      @Schema(description = "Nombre del padre", example = "CARLOS RODRIGUEZ") @JsonProperty("padre") String padre,

      @Schema(description = "Nombre de la madre", example = "ANA MARTINEZ") @JsonProperty("madre") String madre,

      // Documentos adicionales
      @Schema(description = "Cédula anterior (si aplica)", example = "001-0987654-3") @JsonProperty("cedulaVieja") String cedulaVieja,

      @Schema(description = "Número de pasaporte", example = "A12345678") @JsonProperty("pasaporte") String pasaporte,

      // Información de estatus
      @Schema(description = "Categoría de la cédula", example = "1") @JsonProperty("categoria") String categoria,

      @Schema(description = "Descripción de la categoría", example = "CEDULA PRIMERA VEZ") @JsonProperty("descripcionCategoria") String descripcionCategoria,

      @Schema(description = "Estatus actual", example = "TERMINADO") @JsonProperty("estatus") String estatus,

      @Schema(description = "Código de causa de inhabilidad (si aplica)", example = "") @JsonProperty("codigoCausa") String codigoCausa,

      @Schema(description = "Descripción de causa de inhabilidad (si aplica)", example = "") @JsonProperty("descripcionCausaInhabilidad") String descripcionCausaInhabilidad,

      @Schema(description = "Tipo de causa (si aplica)", example = "") @JsonProperty("descripcionTipoCausa") String descripcionTipoCausa

    ){

    /**
     * Constructor que concatena el nombre completo automáticamente.
     */
    public DatosCiudadano {
      if (nombreCompleto == null && nombres != null) {
        StringBuilder nombreBuilder = new StringBuilder(nombres);
        if (primerApellido != null && !primerApellido.isBlank()) {
          nombreBuilder.append(" ").append(primerApellido);
        }
        if (segundoApellido != null && !segundoApellido.isBlank()) {
          nombreBuilder.append(" ").append(segundoApellido);
        }
        nombreCompleto = nombreBuilder.toString().trim();
      }
    }

    /**
     * Verifica si el ciudadano tiene información familiar completa.
     */
    public boolean tieneInformacionFamiliar() {
      return (conyugue != null && !conyugue.isBlank()) ||
          (padre != null && !padre.isBlank()) ||
          (madre != null && !madre.isBlank());
    }

    /**
     * Verifica si tiene documentos adicionales.
     */
    public boolean tieneDocumentosAdicionales() {
      return (cedulaVieja != null && !cedulaVieja.isBlank()) ||
          (pasaporte != null && !pasaporte.isBlank());
    }
  }

  /**
   * Información de la foto de la cédula.
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @Schema(description = "Información de la foto de cédula")
  public record InformacionFoto(

      @Schema(description = "Indica si tiene foto disponible", example = "true") @JsonProperty("disponible") Boolean disponible,

      @Schema(description = "URL completa de la foto", example = "https://dataportal.jce.gob.do/photos/001/1234567.jpg") @JsonProperty("url") String url,

      @Schema(description = "Mensaje sobre la disponibilidad de la foto", example = "Foto encontrada y disponible") @JsonProperty("mensaje") String mensaje

  ) {

    /**
     * Constructor para foto disponible.
     */
    public static InformacionFoto disponible(String url) {
      return new InformacionFoto(true, url, "Foto encontrada y disponible");
    }

    /**
     * Constructor para foto no disponible.
     */
    public static InformacionFoto noDisponible(String razon) {
      return new InformacionFoto(false, null, razon != null ? razon : "Foto no disponible");
    }
  }
}
