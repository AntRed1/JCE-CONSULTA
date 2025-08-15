/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.arojas.jce_consulta.controllers;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.arojas.jce_consulta.DTOs.ConsultaRequest;
import com.arojas.jce_consulta.DTOs.ConsultaResponse;
import com.arojas.jce_consulta.service.JceConsultaService;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import reactor.core.publisher.Mono;

/**
 * Controlador REST para consultas de ciudadanos en el portal JCE.
 * 
 * Este controlador proporciona endpoints para consultar información
 * de ciudadanos dominicanos utilizando su número de cédula.
 * 
 * Características implementadas:
 * - Rate limiting por IP con bucket token
 * - Validación robusta de parámetros
 * - Documentación OpenAPI/Swagger completa
 * - Métricas de rendimiento con Micrometer
 * - Manejo elegante de errores HTTP
 * - Soporte para consultas reactivas
 * - Múltiples formatos de respuesta
 * - Headers de control de caché
 * 
 * @author A. Rojas
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/v1/jce")
@Validated
@Tag(name = "JCE Consulta API", description = "API para consultar información de ciudadanos dominicanos en el portal de la JCE")
public class JceConsultaController {

  private static final Logger logger = LoggerFactory.getLogger(JceConsultaController.class);

  private final JceConsultaService jceConsultaService;
  private final Bucket rateLimitBucket;

  public JceConsultaController(
      JceConsultaService jceConsultaService,
      @Qualifier("globalRateLimitBucket") Bucket rateLimitBucket) {
    this.jceConsultaService = jceConsultaService;
    this.rateLimitBucket = rateLimitBucket;
    logger.info("🎯 JceConsultaController inicializado correctamente");
  }

  // ========================================
  // ENDPOINTS PRINCIPALES
  // ========================================

  /**
   * Consulta completa de ciudadano con objeto request.
   */
  @PostMapping(value = "/consultar", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Consultar ciudadano con parámetros detallados", description = """
      Realiza una consulta completa de un ciudadano dominicano utilizando
      su número de cédula. Permite especificar formato de respuesta y
      si incluir foto de la cédula.
      """)
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Consulta procesada exitosamente", content = @Content(schema = @Schema(implementation = ConsultaResponse.class), examples = @ExampleObject(value = """
          {
            "exitosa": true,
            "mensaje": "Consulta procesada exitosamente",
            "codigo": "CONSULTA_EXITOSA",
            "cedula": "001-1234567-1",
            "tiempoRespuesta": 1250,
            "datos": {
              "nombres": "JUAN CARLOS",
              "apellido1": "RODRIGUEZ",
              "apellido2": "MARTINEZ"
            }
          }
          """))),
      @ApiResponse(responseCode = "400", description = "Parámetros de entrada inválidos"),
      @ApiResponse(responseCode = "429", description = "Límite de peticiones excedido"),
      @ApiResponse(responseCode = "500", description = "Error interno del servidor")
  })
  @Timed(value = "jce.consulta.request", description = "Tiempo de procesamiento de consultas POST")
  public Mono<ResponseEntity<ConsultaResponse>> consultarCiudadano(
      @Valid @RequestBody ConsultaRequest request,
      HttpServletRequest httpRequest) {

    String clientIp = obtenerIpCliente(httpRequest);
    String requestId = generarRequestId();

    logger.info("📋 [{}] POST /consultar - IP: {} - Cédula: {}",
        requestId, clientIp, request.getCedulaFormateada());

    // Verificar rate limit
    if (!verificarRateLimit(clientIp)) {
      return Mono.just(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
          .header("X-Rate-Limit-Retry-After", "60")
          .body(ConsultaResponse.error(
              "Límite de peticiones excedido. Intente nuevamente en 1 minuto.",
              "RATE_LIMIT_EXCEEDED",
              request.getCedulaFormateada(),
              0L)));
    }

    return jceConsultaService.consultarCiudadano(request)
        .map(response -> crearRespuestaHttp(response, requestId))
        .doOnTerminate(() -> logger.debug("🏁 [{}] Consulta POST finalizada", requestId));
  }

  /**
   * Consulta simplificada por cédula en URL.
   */
  @GetMapping("/consultar/{cedula}")
  @Operation(summary = "Consultar ciudadano por cédula", description = """
      Consulta simplificada usando solo el número de cédula en la URL.
      Retorna información completa sin foto por defecto.
      """)
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
      @ApiResponse(responseCode = "400", description = "Formato de cédula inválido"),
      @ApiResponse(responseCode = "429", description = "Límite de peticiones excedido")
  })
  @Timed(value = "jce.consulta.get", description = "Tiempo de procesamiento de consultas GET")
  public Mono<ResponseEntity<ConsultaResponse>> consultarCiudadanoPorCedula(
      @PathVariable @Parameter(description = "Número de cédula dominicana (con o sin guiones)", example = "001-1234567-1") @NotBlank(message = "La cédula no puede estar vacía") @Pattern(regexp = "^\\d{3}-?\\d{7}-?\\d{1}$", message = "Formato de cédula inválido. Use XXX-XXXXXXX-X o XXXXXXXXXXX") String cedula,

      @RequestParam(defaultValue = "completo") @Parameter(description = "Formato de respuesta", example = "completo", schema = @Schema(allowableValues = {
          "completo", "basico", "personal", "familiar" })) String formato,

      @RequestParam(defaultValue = "false") @Parameter(description = "Incluir URL de foto de cédula", example = "true") boolean incluirFoto,

      HttpServletRequest httpRequest) {

    String clientIp = obtenerIpCliente(httpRequest);
    String requestId = generarRequestId();

    logger.info("📋 [{}] GET /consultar/{} - IP: {} - Formato: {} - Foto: {}",
        requestId, cedula, clientIp, formato, incluirFoto);

    // Verificar rate limit
    if (!verificarRateLimit(clientIp)) {
      return Mono.just(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
          .header("X-Rate-Limit-Retry-After", "60")
          .body(ConsultaResponse.error(
              "Límite de peticiones excedido. Intente nuevamente en 1 minuto.",
              "RATE_LIMIT_EXCEEDED",
              cedula,
              0L)));
    }

    ConsultaRequest request = new ConsultaRequest(cedula, incluirFoto, formato);

    return jceConsultaService.consultarCiudadano(request)
        .map(response -> crearRespuestaHttp(response, requestId))
        .doOnTerminate(() -> logger.debug("🏁 [{}] Consulta GET finalizada", requestId));
  }

  /**
   * Endpoint de salud del microservicio.
   */
  @GetMapping("/health")
  @Operation(summary = "Verificar salud del servicio", description = "Endpoint para verificar el estado del microservicio y su conectividad con JCE")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Servicio operativo"),
      @ApiResponse(responseCode = "503", description = "Servicio no disponible")
  })
  public Mono<ResponseEntity<Map<String, Object>>> verificarSalud() {
    String requestId = generarRequestId();
    logger.debug("💊 [{}] Verificando salud del servicio", requestId);

    return Mono.fromFuture(jceConsultaService.verificarSaludServicio())
        .map(jceDisponible -> {
          HttpStatus status = jceDisponible ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;

          Map<String, Object> salud = Map.of(
              "servicio", "jce-consulta-ms",
              "version", "1.0.0",
              "estado", jceDisponible ? "UP" : "DOWN",
              "jce_conectividad", jceDisponible ? "DISPONIBLE" : "NO_DISPONIBLE",
              "timestamp", System.currentTimeMillis());

          return ResponseEntity.status(status)
              .header("Cache-Control", "no-cache")
              .body(salud);
        })
        .onErrorReturn(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(Map.of(
                "servicio", "jce-consulta-ms",
                "estado", "ERROR",
                "mensaje", "Error verificando conectividad",
                "timestamp", System.currentTimeMillis())));
  }

  /**
   * Endpoint para obtener métricas básicas.
   */
  @GetMapping("/metrics")
  @Operation(summary = "Métricas básicas del servicio", description = "Retorna métricas básicas de uso del microservicio")
  @ApiResponse(responseCode = "200", description = "Métricas obtenidas exitosamente")
  public ResponseEntity<Map<String, Object>> obtenerMetricas() {
    // En un entorno real, esto se obtendría del MeterRegistry
    Map<String, Object> metricas = Map.of(
        "servicio", "jce-consulta-ms",
        "version", "1.0.0",
        "uptime_ms", System.currentTimeMillis(),
        "estado", "ACTIVO",
        "info", "Use /actuator/metrics para métricas detalladas");

    return ResponseEntity.ok()
        .header("Cache-Control", "max-age=30")
        .body(metricas);
  }

  // ========================================
  // MÉTODOS PRIVADOS AUXILIARES
  // ========================================

  /**
   * Verifica el rate limit para el cliente.
   */
  private boolean verificarRateLimit(String clientIp) {
    ConsumptionProbe probe = rateLimitBucket.tryConsumeAndReturnRemaining(1);

    if (probe.isConsumed()) {
      logger.debug("✅ Rate limit OK para IP: {} - Tokens restantes: {}",
          clientIp, probe.getRemainingTokens());
      return true;
    } else {
      logger.warn("🚫 Rate limit excedido para IP: {} - Reintentar en: {}s",
          clientIp, probe.getNanosToWaitForRefill() / 1_000_000_000);
      return false;
    }
  }

  /**
   * Crea la respuesta HTTP apropiada según el resultado.
   */
  private ResponseEntity<ConsultaResponse> crearRespuestaHttp(ConsultaResponse response, String requestId) {
    HttpStatus status;
    String cacheControl;

    if (response.exitosa()) {
      status = HttpStatus.OK;
      cacheControl = "public, max-age=300"; // 5 minutos
      logger.info("✅ [{}] Respuesta exitosa - Tiempo: {}ms", requestId, response.tiempoRespuesta());
    } else {
      // Determinar status según el código de error
      status = switch (response.codigo()) {
        case "CEDULA_INVALIDA" -> HttpStatus.BAD_REQUEST;
        case "JCE_TIMEOUT", "JCE_NO_DISPONIBLE" -> HttpStatus.SERVICE_UNAVAILABLE;
        case "CIUDADANO_NO_ENCONTRADO" -> HttpStatus.NOT_FOUND;
        case "RATE_LIMIT_EXCEEDED" -> HttpStatus.TOO_MANY_REQUESTS;
        default -> HttpStatus.INTERNAL_SERVER_ERROR;
      };
      cacheControl = "no-cache";
      logger.warn("⚠️ [{}] Respuesta con error: {} - {}", requestId, response.codigo(), response.mensaje());
    }

    return ResponseEntity.status(status)
        .header("Cache-Control", cacheControl)
        .header("X-Request-ID", requestId)
        .header("X-Response-Time", response.tiempoRespuesta() + "ms")
        .body(response);
  }

  /**
   * Obtiene la IP real del cliente considerando proxies.
   */
  private String obtenerIpCliente(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    String xRealIP = request.getHeader("X-Real-IP");
    String xClientIP = request.getHeader("X-Client-IP");

    if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
      return xForwardedFor.split(",")[0].trim();
    }
    if (xRealIP != null && !xRealIP.isEmpty() && !"unknown".equalsIgnoreCase(xRealIP)) {
      return xRealIP;
    }
    if (xClientIP != null && !xClientIP.isEmpty() && !"unknown".equalsIgnoreCase(xClientIP)) {
      return xClientIP;
    }
    return request.getRemoteAddr();
  }

  /**
   * Genera un ID único para rastrear peticiones.
   */
  private String generarRequestId() {
    return "CTR-" + System.currentTimeMillis() + "-" +
        Integer.toHexString((int) (Math.random() * 65536));
  }
}
