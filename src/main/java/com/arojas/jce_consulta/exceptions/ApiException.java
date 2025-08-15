/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.arojas.jce_consulta.exceptions;

import org.springframework.http.HttpStatus;

import com.arojas.jce_consulta.exceptions.ApiException.TipoError;

/**
 * Excepción personalizada para errores de la API JCE.
 * 
 * Esta clase encapsula errores específicos del dominio de negocio
 * del microservicio, proporcionando información detallada sobre
 * el tipo de error, código de respuesta HTTP sugerido, y contexto
 * adicional para facilitar el debugging y logging.
 * 
 * Características:
 * - Códigos de error estructurados y consistentes
 * - Mapeo automático a status HTTP apropiados
 * - Contexto adicional para auditoría y debugging
 * - Soporte para anidación de excepciones (cause)
 * - Compatibilidad con Spring Boot error handling
 * 
 * @author A. Rojas
 * @version 1.0.0
 */
public class ApiException extends RuntimeException {

  /**
   * Tipos de errores específicos del dominio JCE.
   */
  public enum TipoError {
    // Errores de validación de entrada
    CEDULA_INVALIDA("La cédula proporcionada no tiene un formato válido", HttpStatus.BAD_REQUEST),
    PARAMETROS_INVALIDOS("Los parámetros de la petición son inválidos", HttpStatus.BAD_REQUEST),
    FORMATO_NO_SOPORTADO("El formato solicitado no está soportado", HttpStatus.BAD_REQUEST),

    // Errores de negocio
    CIUDADANO_NO_ENCONTRADO("No se encontraron datos para la cédula consultada", HttpStatus.NOT_FOUND),
    CEDULA_NO_ACTIVA("La cédula consultada no está activa en el sistema JCE", HttpStatus.UNPROCESSABLE_ENTITY),

    // Errores de integración externa
    JCE_NO_DISPONIBLE("El portal JCE no está disponible temporalmente", HttpStatus.SERVICE_UNAVAILABLE),
    JCE_TIMEOUT("El portal JCE tardó demasiado en responder", HttpStatus.GATEWAY_TIMEOUT),
    JCE_ERROR_COMUNICACION("Error de comunicación con el portal JCE", HttpStatus.BAD_GATEWAY),
    JCE_RESPUESTA_INVALIDA("El portal JCE retornó una respuesta inválida", HttpStatus.BAD_GATEWAY),

    // Errores de límites y cuotas
    RATE_LIMIT_EXCEDIDO("Se ha excedido el límite de peticiones permitidas", HttpStatus.TOO_MANY_REQUESTS),
    CUOTA_DIARIA_EXCEDIDA("Se ha excedido la cuota diaria de consultas", HttpStatus.TOO_MANY_REQUESTS),

    // Errores internos del sistema
    ERROR_CACHE("Error en el sistema de caché", HttpStatus.INTERNAL_SERVER_ERROR),
    ERROR_CONFIGURACION("Error de configuración del microservicio", HttpStatus.INTERNAL_SERVER_ERROR),
    ERROR_DESERIALIZACION("Error procesando la respuesta XML del portal JCE", HttpStatus.INTERNAL_SERVER_ERROR),
    ERROR_INTERNO("Error interno del microservicio", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String descripcion;
    private final HttpStatus statusHttp;

    TipoError(String descripcion, HttpStatus statusHttp) {
      this.descripcion = descripcion;
      this.statusHttp = statusHttp;
    }

    public String getDescripcion() {
      return descripcion;
    }

    public HttpStatus getStatusHttp() {
      return statusHttp;
    }

    public String getCodigo() {
      return this.name();
    }
  }

  // ========================================
  // ATRIBUTOS
  // ========================================

  private final TipoError tipoError;
  private final String codigoError;
  private final String contexto;
  private final Long timestamp;

  // ========================================
  // CONSTRUCTORES
  // ========================================

  /**
   * Constructor principal con tipo de error.
   */
  public ApiException(TipoError tipoError) {
    super(tipoError.getDescripcion());
    this.tipoError = tipoError;
    this.codigoError = tipoError.getCodigo();
    this.contexto = null;
    this.timestamp = System.currentTimeMillis();
  }

  /**
   * Constructor con tipo de error y mensaje personalizado.
   */
  public ApiException(TipoError tipoError, String mensajePersonalizado) {
    super(mensajePersonalizado);
    this.tipoError = tipoError;
    this.codigoError = tipoError.getCodigo();
    this.contexto = null;
    this.timestamp = System.currentTimeMillis();
  }

  /**
   * Constructor con tipo de error, mensaje y contexto.
   */
  public ApiException(TipoError tipoError, String mensajePersonalizado, String contexto) {
    super(mensajePersonalizado);
    this.tipoError = tipoError;
    this.codigoError = tipoError.getCodigo();
    this.contexto = contexto;
    this.timestamp = System.currentTimeMillis();
  }

  /**
   * Constructor con tipo de error, mensaje, contexto y causa.
   */
  public ApiException(TipoError tipoError, String mensajePersonalizado, String contexto, Throwable cause) {
    super(mensajePersonalizado, cause);
    this.tipoError = tipoError;
    this.codigoError = tipoError.getCodigo();
    this.contexto = contexto;
    this.timestamp = System.currentTimeMillis();
  }

  /**
   * Constructor con tipo de error y causa.
   */
  public ApiException(TipoError tipoError, Throwable cause) {
    super(tipoError.getDescripcion(), cause);
    this.tipoError = tipoError;
    this.codigoError = tipoError.getCodigo();
    this.contexto = null;
    this.timestamp = System.currentTimeMillis();
  }

  // ========================================
  // MÉTODOS ESTÁTICOS DE CONVENIENCIA
  // ========================================

  /**
   * Crea excepción para cédula inválida.
   */
  public static ApiException cedulaInvalida(String cedula) {
    return new ApiException(
        TipoError.CEDULA_INVALIDA,
        "La cédula '" + cedula + "' no tiene un formato válido. Use XXX-XXXXXXX-X o XXXXXXXXXXX",
        "cedula=" + cedula);
  }

  /**
   * Crea excepción para ciudadano no encontrado.
   */
  public static ApiException ciudadanoNoEncontrado(String cedula) {
    return new ApiException(
        TipoError.CIUDADANO_NO_ENCONTRADO,
        "No se encontraron datos para la cédula: " + cedula,
        "cedula=" + cedula);
  }

  /**
   * Crea excepción para JCE no disponible.
   */
  public static ApiException jceNoDisponible() {
    return new ApiException(
        TipoError.JCE_NO_DISPONIBLE,
        "El portal JCE no está disponible temporalmente. Intente nuevamente en unos minutos.");
  }

  /**
   * Crea excepción para timeout de JCE.
   */
  public static ApiException jceTimeout() {
    return new ApiException(
        TipoError.JCE_TIMEOUT,
        "El portal JCE tardó demasiado en responder. Intente nuevamente.");
  }

  /**
   * Crea excepción para rate limit excedido.
   */
  public static ApiException rateLimitExcedido() {
    return new ApiException(
        TipoError.RATE_LIMIT_EXCEDIDO,
        "Se ha excedido el límite de peticiones. Intente nuevamente en 1 minuto.");
  }

  /**
   * Crea excepción para formato no soportado.
   */
  public static ApiException formatoNoSoportado(String formato) {
    return new ApiException(
        TipoError.FORMATO_NO_SOPORTADO,
        "El formato '" + formato + "' no está soportado. Use: completo, basico, personal, familiar",
        "formato=" + formato);
  }

  /**
   * Crea excepción para error de deserialización XML.
   */
  public static ApiException errorDeserializacion(String detalles, Throwable cause) {
    return new ApiException(
        TipoError.ERROR_DESERIALIZACION,
        "Error procesando la respuesta XML del portal JCE: " + detalles,
        detalles,
        cause);
  }

  /**
   * Crea excepción para error de comunicación con JCE.
   */
  public static ApiException errorComunicacionJce(Throwable cause) {
    return new ApiException(
        TipoError.JCE_ERROR_COMUNICACION,
        "Error de comunicación con el portal JCE",
        cause != null ? cause.getMessage() : "No se proporcionó causa",
        cause);
  }

  // ========================================
  // GETTERS
  // ========================================

  public TipoError getTipoError() {
    return tipoError;
  }

  public String getCodigoError() {
    return codigoError;
  }

  public String getContexto() {
    return contexto;
  }

  public Long getTimestamp() {
    return timestamp;
  }

  public HttpStatus getStatusHttp() {
    return tipoError.getStatusHttp();
  }

  // ========================================
  // MÉTODOS DE UTILIDAD
  // ========================================

  /**
   * Retorna una representación completa del error para logging.
   */
  public String getDetalleCompleto() {
    StringBuilder sb = new StringBuilder();
    sb.append("ApiException{");
    sb.append("tipo=").append(tipoError.name());
    sb.append(", codigo='").append(codigoError).append('\'');
    sb.append(", mensaje='").append(getMessage()).append('\'');
    if (contexto != null) {
      sb.append(", contexto='").append(contexto).append('\'');
    }
    sb.append(", timestamp=").append(timestamp);
    if (getCause() != null) {
      sb.append(", causa=").append(getCause().getClass().getSimpleName());
      sb.append(":").append(getCause().getMessage());
    }
    sb.append('}');
    return sb.toString();
  }

  /**
   * Verifica si es un error de cliente (4xx).
   */
  public boolean esErrorCliente() {
    return tipoError.getStatusHttp().is4xxClientError();
  }

  /**
   * Verifica si es un error de servidor (5xx).
   */
  public boolean esErrorServidor() {
    return tipoError.getStatusHttp().is5xxServerError();
  }

  /**
   * Verifica si es un error de servicio externo.
   */
  public boolean esErrorServicioExterno() {
    return tipoError == TipoError.JCE_NO_DISPONIBLE ||
        tipoError == TipoError.JCE_TIMEOUT ||
        tipoError == TipoError.JCE_ERROR_COMUNICACION ||
        tipoError == TipoError.JCE_RESPUESTA_INVALIDA;
  }

  @Override
  public String toString() {
    return getDetalleCompleto();
  }
}