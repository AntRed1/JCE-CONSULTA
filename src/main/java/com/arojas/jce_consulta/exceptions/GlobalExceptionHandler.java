/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.arojas.jce_consulta.exceptions;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import com.arojas.jce_consulta.DTOs.ConsultaResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

/**
 * Manejador global de excepciones para el microservicio JCE.
 * 
 * Esta clase centraliza el manejo de todas las excepciones que pueden
 * ocurrir en el microservicio, proporcionando respuestas consistentes
 * y estructuradas para diferentes tipos de errores.
 * 
 * Características implementadas:
 * - Manejo específico de excepciones de dominio (ApiException)
 * - Manejo de errores de validación de Spring Boot
 * - Manejo de errores HTTP estándar (404, 405, 415, etc.)
 * - Logging estructurado con niveles apropiados
 * - Métricas de errores para monitoreo
 * - Respuestas JSON consistentes
 * - Ocultación de detalles internos en producción
 * - Códigos de error estructurados
 * 
 * @author A. Rojas
 * @version 1.0.0
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final Counter erroresCounter;
    private final Counter erroresValidacionCounter;
    private final Counter erroresInternosCounter;

    public GlobalExceptionHandler(MeterRegistry meterRegistry) {
        this.erroresCounter = Counter.builder("jce.errores.total")
                .description("Número total de errores capturados")
                .register(meterRegistry);

        this.erroresValidacionCounter = Counter.builder("jce.errores.validacion")
                .description("Número de errores de validación")
                .register(meterRegistry);

        this.erroresInternosCounter = Counter.builder("jce.errores.internos")
                .description("Número de errores internos del servidor")
                .register(meterRegistry);

        logger.info("🛡️ GlobalExceptionHandler inicializado con métricas");
    }

    // ========================================
    // EXCEPCIONES DE DOMINIO
    // ========================================

    /**
     * Maneja excepciones específicas del dominio JCE.
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ConsultaResponse> handleApiException(
            ApiException ex,
            HttpServletRequest request) {

        String requestId = generarRequestId();
        erroresCounter.increment();

        // Log según el tipo de error
        String detalle = Objects.requireNonNullElse(ex.getDetalleCompleto(), ex.getMessage());
        if (ex.esErrorCliente()) {
            logger.warn("⚠️ [{}] Error de cliente: {} - URI: {}",
                    requestId, detalle, request.getRequestURI());
        } else if (ex.esErrorServicioExterno()) {
            logger.error("🌐 [{}] Error servicio externo: {} - URI: {}",
                    requestId, detalle, request.getRequestURI());
        } else {
            logger.error("💥 [{}] Error interno: {} - URI: {}",
                    requestId, detalle, request.getRequestURI());
            erroresInternosCounter.increment();
        }

        ConsultaResponse response = ConsultaResponse.error(
                ex.getMessage(),
                ex.getCodigoError(),
                extraerCedulaDeContexto(ex.getContexto()),
                0L);

        return ResponseEntity
                .status(ex.getStatusHttp())
                .header("X-Request-ID", requestId)
                .header("X-Error-Type", ex.getTipoError().name())
                .body(response);
    }

    // ========================================
    // ERRORES DE VALIDACIÓN
    // ========================================

    /**
     * Maneja errores de validación en el request body (@Valid).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ConsultaResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String requestId = generarRequestId();
        erroresValidacionCounter.increment();

        List<String> errores = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .collect(Collectors.toList());

        String mensaje = "Errores de validación: " + String.join("; ", errores);

        logger.warn("📋 [{}] Error validación request body: {} - URI: {}",
                requestId, mensaje, request.getRequestURI());

        ConsultaResponse response = ConsultaResponse.error(
                mensaje,
                "VALIDACION_FALLIDA",
                null,
                0L);

        return ResponseEntity
                .badRequest()
                .header("X-Request-ID", requestId)
                .header("X-Error-Type", "VALIDATION")
                .body(response);
    }

    /**
     * Maneja errores de validación en parámetros de URL
     * (@PathVariable, @RequestParam).
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ConsultaResponse> handleConstraintViolationException(
            ConstraintViolationException ex,
            HttpServletRequest request) {

        String requestId = generarRequestId();
        erroresValidacionCounter.increment();

        List<String> errores = ex.getConstraintViolations()
                .stream()
                .map(this::formatConstraintViolation)
                .collect(Collectors.toList());

        String mensaje = "Parámetros inválidos: " + String.join("; ", errores);

        logger.warn("🔗 [{}] Error validación parámetros: {} - URI: {}",
                requestId, mensaje, request.getRequestURI());

        ConsultaResponse response = ConsultaResponse.error(
                mensaje,
                "PARAMETROS_INVALIDOS",
                null,
                0L);

        return ResponseEntity
                .badRequest()
                .header("X-Request-ID", requestId)
                .header("X-Error-Type", "PARAMETER_VALIDATION")
                .body(response);
    }

    /**
     * Maneja errores de bind de formularios.
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ConsultaResponse> handleBindException(
            BindException ex,
            HttpServletRequest request) {

        String requestId = generarRequestId();
        erroresValidacionCounter.increment();

        List<String> errores = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .collect(Collectors.toList());

        String mensaje = "Errores en formulario: " + String.join("; ", errores);

        logger.warn("📝 [{}] Error bind formulario: {} - URI: {}",
                requestId, mensaje, request.getRequestURI());

        ConsultaResponse response = ConsultaResponse.error(
                mensaje,
                "FORMULARIO_INVALIDO",
                null,
                0L);

        return ResponseEntity
                .badRequest()
                .header("X-Request-ID", requestId)
                .body(response);
    }

    // ========================================
    // ERRORES HTTP ESTÁNDAR
    // ========================================

    /**
     * Maneja parámetros requeridos faltantes.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ConsultaResponse> handleMissingParameter(
            MissingServletRequestParameterException ex,
            HttpServletRequest request) {

        String requestId = generarRequestId();
        erroresValidacionCounter.increment();

        String mensaje = String.format("Parámetro requerido faltante: '%s' de tipo %s",
                ex.getParameterName(), ex.getParameterType());

        logger.warn("❓ [{}] Parámetro faltante: {} - URI: {}",
                requestId, mensaje, request.getRequestURI());

        ConsultaResponse response = ConsultaResponse.error(
                mensaje,
                "PARAMETRO_REQUERIDO_FALTANTE",
                null,
                0L);

        return ResponseEntity
                .badRequest()
                .header("X-Request-ID", requestId)
                .body(response);
    }

    /**
     * Maneja tipos de argumento incorrectos.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ConsultaResponse> handleTypeMismatchException(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        String requestId = generarRequestId();
        erroresValidacionCounter.increment();

        String tipoEsperado = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "desconocido";

        String mensaje = String.format("Tipo de parámetro incorrecto: '%s' debe ser de tipo %s",
                ex.getName(), tipoEsperado);

        logger.warn("🔢 [{}] Tipo parámetro incorrecto: {} - URI: {}",
                requestId, mensaje, request.getRequestURI());

        ConsultaResponse response = ConsultaResponse.error(
                mensaje,
                "TIPO_PARAMETRO_INCORRECTO",
                null,
                0L);

        return ResponseEntity
                .badRequest()
                .header("X-Request-ID", requestId)
                .body(response);
    }

    /**
     * Maneja JSON mal formateado.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ConsultaResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        String requestId = generarRequestId();
        erroresValidacionCounter.increment();

        String mensaje = "JSON mal formateado o contenido de petición inválido";

        // Proporcionar detalles específicos para algunos casos comunes
        if (ex.getCause() instanceof JsonProcessingException) {
            mensaje = "Error procesando JSON: formato inválido";
        } else if (ex.getCause() instanceof InvalidFormatException) {
            mensaje = "Error en formato JSON: valor inválido en el campo";
        }

        logger.warn("📄 [{}] JSON inválido: {} - URI: {}",
                requestId, ex.getMessage(), request.getRequestURI());

        ConsultaResponse response = ConsultaResponse.error(
                mensaje,
                "JSON_INVALIDO",
                null,
                0L);

        return ResponseEntity
                .badRequest()
                .header("X-Request-ID", requestId)
                .body(response);
    }

    /**
     * Maneja métodos HTTP no soportados.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ConsultaResponse> handleMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {

        String requestId = generarRequestId();
        erroresCounter.increment();

        String metodosPermitidos = "GET, POST";
        if (ex.getSupportedHttpMethods() != null) {
            metodosPermitidos = ex.getSupportedHttpMethods().stream()
                    .map(HttpMethod::name)
                    .collect(Collectors.joining(", "));
        }

        String mensaje = String.format("Método HTTP no soportado: %s. Métodos permitidos: %s",
                ex.getMethod(), metodosPermitidos);

        logger.warn("🚫 [{}] Método no soportado: {} - URI: {}",
                requestId, mensaje, request.getRequestURI());

        ConsultaResponse response = ConsultaResponse.error(
                mensaje,
                "METODO_NO_SOPORTADO",
                null,
                0L);

        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .header("Allow", metodosPermitidos)
                .header("X-Request-ID", requestId)
                .body(response);
    }

    /**
     * Maneja tipos de contenido no soportados.
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ConsultaResponse> handleMediaTypeNotSupportedException(
            HttpMediaTypeNotSupportedException ex,
            HttpServletRequest request) {

        String requestId = generarRequestId();
        erroresCounter.increment();

        String tiposPermitidos = ex.getSupportedMediaTypes()
                .stream()
                .map(Object::toString)
                .collect(Collectors.joining(", "));

        String mensaje = String.format("Tipo de contenido no soportado: %s. Tipos permitidos: %s",
                ex.getContentType(), tiposPermitidos);

        logger.warn("📎 [{}] Media type no soportado: {} - URI: {}",
                requestId, mensaje, request.getRequestURI());

        ConsultaResponse response = ConsultaResponse.error(
                mensaje,
                "MEDIA_TYPE_NO_SOPORTADO",
                null,
                0L);

        return ResponseEntity
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .header("X-Request-ID", requestId)
                .body(response);
    }

    /**
     * Maneja rutas no encontradas.
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ConsultaResponse> handleNoHandlerFoundException(
            NoHandlerFoundException ex,
            HttpServletRequest request) {

        String requestId = generarRequestId();
        erroresCounter.increment();

        String mensaje = String.format("Endpoint no encontrado: %s %s",
                ex.getHttpMethod(), ex.getRequestURL());

        logger.warn("🔍 [{}] Endpoint no encontrado: {} - URI: {}",
                requestId, mensaje, request.getRequestURI());

        ConsultaResponse response = ConsultaResponse.error(
                mensaje,
                "ENDPOINT_NO_ENCONTRADO",
                null,
                0L);

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .header("X-Request-ID", requestId)
                .body(response);
    }

    // ========================================
    // EXCEPCIONES GENERALES
    // ========================================

    /**
     * Maneja todas las demás excepciones no capturadas específicamente.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ConsultaResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        String requestId = generarRequestId();
        erroresCounter.increment();
        erroresInternosCounter.increment();

        // Log completo del error para debugging
        logger.error("💥 [{}] Error interno no manejado - URI: {} - Tipo: {} - Mensaje: {}",
                requestId, request.getRequestURI(),
                ex.getClass().getSimpleName(), ex.getMessage(), ex);

        String mensaje = "Error interno del servidor. Contacte al administrador si persiste.";

        ConsultaResponse response = ConsultaResponse.error(
                mensaje,
                "ERROR_INTERNO",
                null,
                0L);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("X-Request-ID", requestId)
                .header("X-Error-Type", "INTERNAL")
                .body(response);
    }

    // ========================================
    // MÉTODOS AUXILIARES
    // ========================================

    /**
     * Formatea errores de campo para una presentación clara.
     */
    private String formatFieldError(FieldError error) {
        return String.format("Campo '%s': %s (valor rechazado: %s)",
                error.getField(),
                error.getDefaultMessage(),
                error.getRejectedValue());
    }

    /**
     * Formatea violaciones de constraint para una presentación clara.
     */
    private String formatConstraintViolation(ConstraintViolation<?> violation) {
        String propertyPath = violation.getPropertyPath().toString();
        // Extraer solo el nombre del parámetro (última parte del path)
        String paramName = propertyPath.contains(".") ? propertyPath.substring(propertyPath.lastIndexOf('.') + 1)
                : propertyPath;

        return String.format("Parámetro '%s': %s (valor: %s)",
                paramName,
                violation.getMessage(),
                violation.getInvalidValue());
    }

    /**
     * Extrae la cédula del contexto si está disponible.
     */
    private String extraerCedulaDeContexto(String contexto) {
        if (contexto == null || !contexto.contains("cedula=")) {
            return null;
        }

        try {
            int inicio = contexto.indexOf("cedula=") + 7;
            int fin = contexto.indexOf(",", inicio);
            if (fin == -1) {
                fin = contexto.length();
            }
            return contexto.substring(inicio, fin);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Genera un ID único para rastrear errores.
     */
    private String generarRequestId() {
        return "ERR-" + System.currentTimeMillis() + "-" +
                Integer.toHexString((int) (Math.random() * 65536));
    }
}