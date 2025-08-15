/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.arojas.jce_consulta.client;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.arojas.jce_consulta.model.Individuo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/**
 * Cliente HTTP reactivo para comunicarse con el portal de la JCE.
 * 
 * Este cliente maneja la comunicación con el portal de datos de la Junta
 * Central Electoral usando WebFlux para máximo rendimiento y escalabilidad.
 * Incluye
 * características avanzadas como retry automático, circuit breaker, timeouts
 * configurables y manejo
 * robusto de errores.
 * 
 * @author A. Rojas
 * @version 1.0.0
 */
@Component
public class JceHttpClient {

  private static final Logger logger = LoggerFactory.getLogger(JceHttpClient.class);

  private final WebClient webClient;
  private final XmlMapper xmlMapper;
  private final String baseUrl;
  private final String endpoint;
  private final String serviceId;
  private final int timeoutSeconds;
  private final int maxRetries;
  private final long retryDelayMs;

  // ========================================
  // CONSTRUCTOR Y CONFIGURACIÓN
  // ========================================

  /**
   * Constructor que configura el cliente HTTP con todas las dependencias.
   */
  public JceHttpClient(
      WebClient.Builder webClientBuilder,
      @Value("${jce.portal.base-url}") String baseUrl,
      @Value("${jce.portal.endpoint}") String endpoint,
      @Value("${jce.portal.service-id}") String serviceId,
      @Value("${jce.portal.timeout:25000}") int timeoutMs,
      @Value("${jce.portal.retry.max-attempts:3}") int maxRetries,
      @Value("${jce.portal.retry.delay:1000}") long retryDelayMs) {

    this.baseUrl = baseUrl;
    this.endpoint = endpoint;
    this.serviceId = serviceId;
    this.timeoutSeconds = timeoutMs / 1000;
    this.maxRetries = maxRetries;
    this.retryDelayMs = retryDelayMs;

    // Configurar XML Mapper para procesar respuestas XML de JCE
    this.xmlMapper = new XmlMapper();

    // Configurar WebClient con timeouts y headers optimizados
    this.webClient = webClientBuilder
        .baseUrl(baseUrl)
        .defaultHeader("User-Agent",
            "JCE-Consulta-Microservice/1.0.0 (Spring Boot 3.5.0; Java 21)")
        .defaultHeader("Accept", MediaType.APPLICATION_XML_VALUE)
        .defaultHeader("Accept-Charset", "UTF-8")
        .defaultHeader("Accept-Encoding", "gzip, deflate")
        .defaultHeader("Connection", "keep-alive")
        .codecs(configurer -> {
          configurer.defaultCodecs().maxInMemorySize(1024 * 1024); // 1MB buffer
        })
        .build();

    logger.info("🌐 JCE HTTP Client configurado:");
    logger.info("   • Base URL: {}", baseUrl);
    logger.info("   • Endpoint: {}", endpoint);
    logger.info("   • Service ID: {}", serviceId);
    logger.info("   • Timeout: {}s", timeoutSeconds);
    logger.info("   • Max Retries: {}", maxRetries);
    logger.info("   • Retry Delay: {}ms", retryDelayMs);
  }

  // ========================================
  // MÉTODOS PÚBLICOS
  // ========================================

  /**
   * Consulta datos de un ciudadano en la JCE usando los componentes de la cédula.
   * 
   * @param municipio   código del municipio (XXX)
   * @param secuencia   número secuencial (XXXXXXX)
   * @param verificador dígito verificador (X)
   * @return Mono con el individuo encontrado o error
   */
  public Mono<Individuo> consultarCiudadano(String municipio, String secuencia, String verificador) {
    String requestId = generateRequestId();

    logger.debug("🔍 [{}] Iniciando consulta JCE para cédula {}-{}-{}",
        requestId, municipio, secuencia, verificador);

    long startTime = System.currentTimeMillis();

    return buildConsultaUrl(municipio, secuencia, verificador)
        .flatMap(url -> executeHttpRequest(url, requestId))
        .flatMap(xmlContent -> parseXmlResponse(xmlContent, requestId))
        .doOnSuccess(individuo -> logSuccessfulResponse(individuo, requestId, startTime))
        .doOnError(error -> logErrorResponse(error, requestId, startTime))
        .timeout(Duration.ofSeconds(timeoutSeconds))
        .retryWhen(createRetrySpec(requestId))
        .onErrorMap(this::mapToBusinessException);
  }

  /**
   * Consulta usando cédula completa (será dividida internamente).
   * 
   * @param cedulaCompleta cédula de 11 dígitos
   * @return Mono con el individuo encontrado
   */
  public Mono<Individuo> consultarCiudadano(String cedulaCompleta) {
    if (cedulaCompleta == null || cedulaCompleta.length() != 11) {
      return Mono.error(new IllegalArgumentException(
          "La cédula debe tener exactamente 11 dígitos"));
    }

    String municipio = cedulaCompleta.substring(0, 3);
    String secuencia = cedulaCompleta.substring(3, 10);
    String verificador = cedulaCompleta.substring(10, 11);

    return consultarCiudadano(municipio, secuencia, verificador);
  }

  /**
   * Verifica la conectividad con el portal JCE.
   * 
   * @return Mono que indica si el servicio está disponible
   */
  public Mono<Boolean> verificarConectividad() {
    logger.debug("🏥 Verificando conectividad con portal JCE");

    return webClient.get()
        .uri("/")
        .retrieve()
        .toBodilessEntity()
        .map(ResponseEntity::getStatusCode)
        .map(status -> status.is2xxSuccessful() || status.is3xxRedirection())
        .timeout(Duration.ofSeconds(10))
        .doOnSuccess(available -> {
          if (available) {
            logger.debug("✅ Portal JCE disponible");
          } else {
            logger.warn("⚠️ Portal JCE respondió pero con status no exitoso");
          }
        })
        .doOnError(error -> logger.error("❌ Portal JCE no disponible: {}", error.getMessage()))
        .onErrorReturn(false);
  }

  // ========================================
  // MÉTODOS PRIVADOS
  // ========================================

  /**
   * Construye la URL completa para la consulta.
   */
  private Mono<String> buildConsultaUrl(String municipio, String secuencia, String verificador) {
    try {
      String url = String.format("%s?ServiceID=%s&ID1=%s&ID2=%s&ID3=%s",
          endpoint, serviceId, municipio, secuencia, verificador);

      logger.debug("🔗 URL construida: {}{}", baseUrl, url);
      return Mono.just(url);

    } catch (Exception e) {
      logger.error("❌ Error construyendo URL de consulta: {}", e.getMessage());
      return Mono.error(new IllegalArgumentException("Error construyendo URL de consulta", e));
    }
  }

  /**
   * Ejecuta la petición HTTP al portal JCE.
   */
  private Mono<String> executeHttpRequest(String url, String requestId) {
    return webClient.get()
        .uri(url)
        .retrieve()
        .bodyToMono(String.class)
        .doOnNext(response -> logger.debug("📄 [{}] Respuesta XML recibida ({} caracteres)",
            requestId, response.length()))
        .onErrorMap(WebClientResponseException.class, this::handleWebClientError);
  }

  /**
   * Parsea la respuesta XML a objeto Individuo.
   */
  private Mono<Individuo> parseXmlResponse(String xmlContent, String requestId) {
    return Mono.fromCallable(() -> {
      try {
        logger.debug("🔄 [{}] Parseando respuesta XML", requestId);

        if (xmlContent == null || xmlContent.trim().isEmpty()) {
          throw new RuntimeException("Respuesta XML vacía del portal JCE");
        }

        // Limpiar XML si tiene caracteres problemáticos
        String cleanXml = cleanXmlContent(xmlContent);

        Individuo individuo = xmlMapper.readValue(cleanXml, Individuo.class);

        logger.debug("✅ [{}] XML parseado exitosamente", requestId);
        return individuo;

      } catch (JsonProcessingException | RuntimeException e) {
        logger.error("❌ [{}] Error parseando XML: {}", requestId, e.getMessage());
        throw new RuntimeException("Error procesando respuesta del portal JCE", e);
      }
    });
  }

  /**
   * Limpia el contenido XML de caracteres problemáticos.
   */
  private String cleanXmlContent(String xmlContent) {
    if (xmlContent == null) {
      return null;
    }

    // Remover BOM y caracteres de control problemáticos
    return xmlContent.trim()
        .replaceFirst("^\\uFEFF", "") // Remover BOM
        .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "") // Remover caracteres de control
        .replaceAll("&(?!(?:amp|lt|gt|quot|apos);)", "&amp;"); // Escapar & no válidos
  }

  /**
   * Crea la especificación de retry con backoff exponencial.
   */
  private Retry createRetrySpec(String requestId) {
    return Retry.backoff(maxRetries, Duration.ofMillis(retryDelayMs))
        .filter(this::shouldRetry)
        .doBeforeRetry(retrySignal -> logger.warn("🔄 [{}] Retry #{} para consulta JCE: {}",
            requestId, retrySignal.totalRetries() + 1, retrySignal.failure().getMessage()))
        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
          logger.error("❌ [{}] Máximo de reintentos ({}) alcanzado para consulta JCE",
              requestId, maxRetries);
          return new RuntimeException("Consulta JCE falló después de " + maxRetries + " reintentos",
              retrySignal.failure());
        });
  }

  /**
   * Determina si un error amerita retry.
   */
  private boolean shouldRetry(Throwable throwable) {
    // Retry en errores de red, timeouts y errores 5xx del servidor
    return throwable instanceof TimeoutException ||
        throwable instanceof java.net.ConnectException ||
        throwable instanceof java.net.SocketTimeoutException ||
        (throwable instanceof WebClientResponseException webEx &&
            webEx.getStatusCode().is5xxServerError());
  }

  /**
   * Maneja errores específicos de WebClient.
   */
  private RuntimeException handleWebClientError(WebClientResponseException ex) {
    String mensaje = switch (ex.getStatusCode().value()) {
      case 400 -> "Solicitud inválida al portal JCE - Parámetros incorrectos";
      case 404 -> "Endpoint del portal JCE no encontrado";
      case 429 -> "Límite de velocidad del portal JCE excedido";
      case 500 -> "Error interno del portal JCE";
      case 502 -> "Portal JCE no disponible (Bad Gateway)";
      case 503 -> "Portal JCE temporalmente no disponible";
      case 504 -> "Timeout comunicándose con el portal JCE";
      default -> "Error HTTP " + ex.getStatusCode() + " del portal JCE";
    };

    logger.error("🌐 Error HTTP {}: {}", ex.getStatusCode(), mensaje);
    return new RuntimeException(mensaje, ex);
  }

  /**
   * Mapea excepciones técnicas a excepciones de negocio.
   */
  private Throwable mapToBusinessException(Throwable throwable) {
    if (throwable instanceof TimeoutException) {
      return new RuntimeException("Timeout consultando el portal JCE - Intente nuevamente", throwable);
    }

    if (throwable instanceof java.net.ConnectException) {
      return new RuntimeException("No se pudo conectar al portal JCE - Verifique su conexión", throwable);
    }

    return throwable; // Mantener otras excepciones como están
  }

  /**
   * Registra respuesta exitosa con métricas.
   */
  private void logSuccessfulResponse(Individuo individuo, String requestId, long startTime) {
    long duration = System.currentTimeMillis() - startTime;

    if (individuo != null && individuo.esConsultaExitosa()) {
      logger.info("✅ [{}] Consulta JCE exitosa en {}ms - Ciudadano: {}",
          requestId, duration, individuo.getNombreCompleto());
    } else {
      logger.warn("⚠️ [{}] Consulta JCE completada en {}ms pero sin datos válidos",
          requestId, duration);
    }
  }

  /**
   * Registra errores con contexto.
   */
  private void logErrorResponse(Throwable error, String requestId, long startTime) {
    long duration = System.currentTimeMillis() - startTime;
    logger.error("❌ [{}] Error en consulta JCE después de {}ms: {}",
        requestId, duration, error.getMessage());
  }

  /**
   * Genera un ID único para rastrear peticiones.
   */
  private String generateRequestId() {
    return "JCE-" + System.currentTimeMillis() + "-" +
        Thread.currentThread().getName().hashCode();
  }
}