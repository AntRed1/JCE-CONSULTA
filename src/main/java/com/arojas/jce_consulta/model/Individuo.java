/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.arojas.jce_consulta.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * Modelo interno que representa un individuo según la respuesta XML de la JCE.
 * 
 * Esta clase mapea directamente los campos XML devueltos por el portal
 * de la Junta Central Electoral y proporciona métodos utilitarios
 * para el procesamiento y transformación de datos.
 * 
 * @author A. Rojas
 * @version 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "root")
public class Individuo {

  // ========================================
  // CAMPOS BÁSICOS DE IDENTIDAD
  // ========================================

  @JacksonXmlProperty(localName = "nombres")
  @JsonProperty("nombres")
  private String nombres;

  @JacksonXmlProperty(localName = "apellido1")
  @JsonProperty("apellido1")
  private String apellido1;

  @JacksonXmlProperty(localName = "apellido2")
  @JsonProperty("apellido2")
  private String apellido2;

  // ========================================
  // INFORMACIÓN TEMPORAL Y UBICACIÓN
  // ========================================

  @JacksonXmlProperty(localName = "fecha_nac")
  @JsonProperty("fecha_nac")
  private String fechaNacimiento;

  @JacksonXmlProperty(localName = "lugar_nac")
  @JsonProperty("lugar_nac")
  private String lugarNacimiento;

  @JacksonXmlProperty(localName = "fecha_expiracion")
  @JsonProperty("fecha_expiracion")
  private String fechaExpiracion;

  // ========================================
  // CARACTERÍSTICAS DEMOGRÁFICAS
  // ========================================

  @JacksonXmlProperty(localName = "sexo")
  @JsonProperty("sexo")
  private String sexo;

  @JacksonXmlProperty(localName = "est_civil")
  @JsonProperty("est_civil")
  private String estadoCivil;

  @JacksonXmlProperty(localName = "edad")
  @JsonProperty("edad")
  private String edad;

  // ========================================
  // NACIONALIDAD E INFORMACIÓN CÍVICA
  // ========================================

  @JacksonXmlProperty(localName = "cod_nacion")
  @JsonProperty("cod_nacion")
  private String codigoNacionalidad;

  @JacksonXmlProperty(localName = "desc_nacionalidad")
  @JsonProperty("desc_nacionalidad")
  private String descripcionNacionalidad;

  @JacksonXmlProperty(localName = "mun_ced")
  @JsonProperty("mun_ced")
  private String municipioCedula;

  @JacksonXmlProperty(localName = "seq_ced")
  @JsonProperty("seq_ced")
  private String secuenciaCedula;

  // ========================================
  // INFORMACIÓN LABORAL Y PERSONAL
  // ========================================

  @JacksonXmlProperty(localName = "ocupacion")
  @JsonProperty("ocupacion")
  private String ocupacion;

  // ========================================
  // INFORMACIÓN FAMILIAR
  // ========================================

  @JacksonXmlProperty(localName = "conyugue")
  @JsonProperty("conyugue")
  private String conyugue;

  @JacksonXmlProperty(localName = "cedula_conyugue")
  @JsonProperty("cedula_conyugue")
  private String cedulaConyugue;

  @JacksonXmlProperty(localName = "padre")
  @JsonProperty("padre")
  private String padre;

  @JacksonXmlProperty(localName = "madre")
  @JsonProperty("madre")
  private String madre;

  // ========================================
  // DOCUMENTOS Y REGISTROS
  // ========================================

  @JacksonXmlProperty(localName = "cedula_vieja")
  @JsonProperty("cedula_vieja")
  private String cedulaVieja;

  @JacksonXmlProperty(localName = "pasaporte")
  @JsonProperty("pasaporte")
  private String pasaporte;

  @JacksonXmlProperty(localName = "fotourl")
  @JsonProperty("fotourl")
  private String fotoUrl;

  // ========================================
  // INFORMACIÓN DE CATEGORÍA Y ESTADO
  // ========================================

  @JacksonXmlProperty(localName = "categoria")
  @JsonProperty("categoria")
  private String categoria;

  @JacksonXmlProperty(localName = "desc_categoria")
  @JsonProperty("desc_categoria")
  private String descripcionCategoria;

  @JacksonXmlProperty(localName = "estatus")
  @JsonProperty("estatus")
  private String estatus;

  // ========================================
  // INFORMACIÓN DE CAUSAS (INHABILITACIÓN)
  // ========================================

  @JacksonXmlProperty(localName = "cod_causa")
  @JsonProperty("cod_causa")
  private String codigoCausa;

  @JacksonXmlProperty(localName = "desc_causa_inhabilidad")
  @JsonProperty("desc_causa_inhabilidad")
  private String descripcionCausaInhabilidad;

  @JacksonXmlProperty(localName = "desc_tipo_causa")
  @JsonProperty("desc_tipo_causa")
  private String descripcionTipoCausa;

  // ========================================
  // METADATOS DE RESPUESTA
  // ========================================

  @JacksonXmlProperty(localName = "success")
  @JsonProperty("success")
  private String success;

  @JacksonXmlProperty(localName = "message")
  @JsonProperty("message")
  private String message;

  @JacksonXmlProperty(localName = "responsetime")
  @JsonProperty("responsetime")
  private String responseTime;

  // ========================================
  // CONSTRUCTORES
  // ========================================

  /**
   * Constructor por defecto.
   */
  public Individuo() {
  }

  // ========================================
  // MÉTODOS UTILITARIOS
  // ========================================

  /**
   * Obtiene el nombre completo concatenado.
   * 
   * @return nombre completo con apellidos
   */
  public String getNombreCompleto() {
    StringBuilder nombreCompleto = new StringBuilder();

    if (nombres != null && !nombres.isBlank()) {
      nombreCompleto.append(nombres.trim());
    }

    if (apellido1 != null && !apellido1.isBlank()) {
      if (nombreCompleto.length() > 0)
        nombreCompleto.append(" ");
      nombreCompleto.append(apellido1.trim());
    }

    if (apellido2 != null && !apellido2.isBlank()) {
      if (nombreCompleto.length() > 0)
        nombreCompleto.append(" ");
      nombreCompleto.append(apellido2.trim());
    }

    return nombreCompleto.toString().trim();
  }

  /**
   * Traduce el código de estado civil a descripción legible.
   * 
   * @return descripción del estado civil
   */
  public String getEstadoCivilDescripcion() {
    if (estadoCivil == null || estadoCivil.isBlank()) {
      return "Dato no disponible";
    }

    return switch (estadoCivil.toUpperCase()) {
      case "C" -> "CASADO";
      case "D" -> "DIVORCIADO";
      case "S" -> "SOLTERO";
      case "V" -> "VIUDO";
      case "U" -> "UNION LIBRE";
      case "SE" -> "SEPARADO";
      default -> estadoCivil;
    };
  }

  /**
   * Traduce el código de estatus a descripción legible.
   * 
   * @return descripción del estatus
   */
  public String getEstatusDescripcion() {
    if (estatus == null || estatus.isBlank()) {
      return "Dato no disponible";
    }

    return switch (estatus.toUpperCase()) {
      case "N" -> "NO ATENDIDO";
      case "P" -> "EN PROCESO";
      case "T" -> "TERMINADO";
      case "A" -> "APROBADO";
      case "R" -> "RECHAZADO";
      default -> estatus;
    };
  }

  /**
   * Valida si el campo contiene datos válidos.
   * 
   * @param campo valor del campo a validar
   * @return valor limpio o "Dato no disponible"
   */
  public String validarCampo(String campo) {
    if (campo == null || campo.isBlank() || "null".equalsIgnoreCase(campo)) {
      return "Dato no disponible";
    }
    return campo.trim();
  }

  /**
   * Verifica si tiene información familiar completa.
   * 
   * @return true si tiene datos de cónyuge, padre o madre
   */
  public boolean tieneInformacionFamiliar() {
    return (conyugue != null && !conyugue.isBlank()) ||
        (padre != null && !padre.isBlank()) ||
        (madre != null && !madre.isBlank());
  }

  /**
   * Verifica si tiene documentos adicionales.
   * 
   * @return true si tiene cédula vieja o pasaporte
   */
  public boolean tieneDocumentosAdicionales() {
    return (cedulaVieja != null && !cedulaVieja.isBlank()) ||
        (pasaporte != null && !pasaporte.isBlank());
  }

  /**
   * Verifica si tiene foto disponible.
   * 
   * @return true si tiene URL de foto válida
   */
  public boolean tieneFotoDisponible() {
    return fotoUrl != null && !fotoUrl.isBlank() && !fotoUrl.equalsIgnoreCase("null");
  }

  /**
   * Obtiene el tiempo de respuesta como número.
   * 
   * @return tiempo de respuesta en milisegundos, 0 si no es válido
   */
  public Long getTiempoRespuestaNumerico() {
    if (responseTime == null || responseTime.isBlank()) {
      return 0L;
    }
    try {
      return Long.valueOf(responseTime);
    } catch (NumberFormatException e) {
      return 0L;
    }
  }

  /**
   * Verifica si la consulta fue exitosa según los metadatos.
   * 
   * @return true si la consulta fue exitosa
   */
  public boolean esConsultaExitosa() {
    return success != null &&
        ("true".equalsIgnoreCase(success) || "1".equals(success)) &&
        nombres != null && !nombres.isBlank() &&
        apellido1 != null && !apellido1.isBlank();
  }

  // ========================================
  // GETTERS Y SETTERS
  // ========================================

  public String getNombres() {
    return nombres;
  }

  public void setNombres(String nombres) {
    this.nombres = nombres;
  }

  public String getApellido1() {
    return apellido1;
  }

  public void setApellido1(String apellido1) {
    this.apellido1 = apellido1;
  }

  public String getApellido2() {
    return apellido2;
  }

  public void setApellido2(String apellido2) {
    this.apellido2 = apellido2;
  }

  public String getFechaNacimiento() {
    return fechaNacimiento;
  }

  public void setFechaNacimiento(String fechaNacimiento) {
    this.fechaNacimiento = fechaNacimiento;
  }

  public String getLugarNacimiento() {
    return lugarNacimiento;
  }

  public void setLugarNacimiento(String lugarNacimiento) {
    this.lugarNacimiento = lugarNacimiento;
  }

  public String getFechaExpiracion() {
    return fechaExpiracion;
  }

  public void setFechaExpiracion(String fechaExpiracion) {
    this.fechaExpiracion = fechaExpiracion;
  }

  public String getSexo() {
    return sexo;
  }

  public void setSexo(String sexo) {
    this.sexo = sexo;
  }

  public String getEstadoCivil() {
    return estadoCivil;
  }

  public void setEstadoCivil(String estadoCivil) {
    this.estadoCivil = estadoCivil;
  }

  public String getEdad() {
    return edad;
  }

  public void setEdad(String edad) {
    this.edad = edad;
  }

  public String getCodigoNacionalidad() {
    return codigoNacionalidad;
  }

  public void setCodigoNacionalidad(String codigoNacionalidad) {
    this.codigoNacionalidad = codigoNacionalidad;
  }

  public String getDescripcionNacionalidad() {
    return descripcionNacionalidad;
  }

  public void setDescripcionNacionalidad(String descripcionNacionalidad) {
    this.descripcionNacionalidad = descripcionNacionalidad;
  }

  public String getMunicipioCedula() {
    return municipioCedula;
  }

  public void setMunicipioCedula(String municipioCedula) {
    this.municipioCedula = municipioCedula;
  }

  public String getSecuenciaCedula() {
    return secuenciaCedula;
  }

  public void setSecuenciaCedula(String secuenciaCedula) {
    this.secuenciaCedula = secuenciaCedula;
  }

  public String getOcupacion() {
    return ocupacion;
  }

  public void setOcupacion(String ocupacion) {
    this.ocupacion = ocupacion;
  }

  public String getConyugue() {
    return conyugue;
  }

  public void setConyugue(String conyugue) {
    this.conyugue = conyugue;
  }

  public String getCedulaConyugue() {
    return cedulaConyugue;
  }

  public void setCedulaConyugue(String cedulaConyugue) {
    this.cedulaConyugue = cedulaConyugue;
  }

  public String getPadre() {
    return padre;
  }

  public void setPadre(String padre) {
    this.padre = padre;
  }

  public String getMadre() {
    return madre;
  }

  public void setMadre(String madre) {
    this.madre = madre;
  }

  public String getCedulaVieja() {
    return cedulaVieja;
  }

  public void setCedulaVieja(String cedulaVieja) {
    this.cedulaVieja = cedulaVieja;
  }

  public String getPasaporte() {
    return pasaporte;
  }

  public void setPasaporte(String pasaporte) {
    this.pasaporte = pasaporte;
  }

  public String getFotoUrl() {
    return fotoUrl;
  }

  public void setFotoUrl(String fotoUrl) {
    this.fotoUrl = fotoUrl;
  }

  public String getCategoria() {
    return categoria;
  }

  public void setCategoria(String categoria) {
    this.categoria = categoria;
  }

  public String getDescripcionCategoria() {
    return descripcionCategoria;
  }

  public void setDescripcionCategoria(String descripcionCategoria) {
    this.descripcionCategoria = descripcionCategoria;
  }

  public String getEstatus() {
    return estatus;
  }

  public void setEstatus(String estatus) {
    this.estatus = estatus;
  }

  public String getCodigoCausa() {
    return codigoCausa;
  }

  public void setCodigoCausa(String codigoCausa) {
    this.codigoCausa = codigoCausa;
  }

  public String getDescripcionCausaInhabilidad() {
    return descripcionCausaInhabilidad;
  }

  public void setDescripcionCausaInhabilidad(String descripcionCausaInhabilidad) {
    this.descripcionCausaInhabilidad = descripcionCausaInhabilidad;
  }

  public String getDescripcionTipoCausa() {
    return descripcionTipoCausa;
  }

  public void setDescripcionTipoCausa(String descripcionTipoCausa) {
    this.descripcionTipoCausa = descripcionTipoCausa;
  }

  public String getSuccess() {
    return success;
  }

  public void setSuccess(String success) {
    this.success = success;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getResponseTime() {
    return responseTime;
  }

  public void setResponseTime(String responseTime) {
    this.responseTime = responseTime;
  }

  // ========================================
  // MÉTODOS ESTÁNDAR
  // ========================================

  @Override
  public String toString() {
    return "Individuo{" +
        "nombres='" + nombres + '\'' +
        ", apellido1='" + apellido1 + '\'' +
        ", apellido2='" + apellido2 + '\'' +
        ", fechaNacimiento='" + fechaNacimiento + '\'' +
        ", sexo='" + sexo + '\'' +
        ", nacionalidad='" + descripcionNacionalidad + '\'' +
        ", success='" + success + '\'' +
        '}';
  }
}