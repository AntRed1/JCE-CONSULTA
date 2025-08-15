/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.arojas.jce_consulta.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import com.arojas.jce_consulta.config.AppProperties.Cache;
import com.arojas.jce_consulta.config.AppProperties.Jce;
import com.arojas.jce_consulta.config.AppProperties.Metrics;
import com.arojas.jce_consulta.config.AppProperties.RateLimit;
import com.arojas.jce_consulta.config.AppProperties.Resilience;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Propiedades personalizadas de configuración para el microservicio JCE.
 * 
 * Esta clase centraliza todas las configuraciones específicas del
 * microservicio,
 * permitiendo una gestión más ordenada y validación automática de los valores
 * de configuración.
 * 
 * Características:
 * - Validación automática de propiedades con Bean Validation
 * - Agrupación lógica de configuraciones relacionadas
 * - Valores por defecto sensatos para todos los entornos
 * - Documentación integrada de cada propiedad
 * - Soporte para configuración por perfiles (dev, prod, test)
 * 
 * @author A. Rojas
 * @version 1.0.0
 */
@Component
@ConfigurationProperties(prefix = "jce.consulta")
@Validated
public class AppProperties {

  /**
   * Configuración del portal JCE.
   */
  private Jce jce = new Jce();

  /**
   * Configuración de rate limiting.
   */
  private RateLimit rateLimit = new RateLimit();

  /**
   * Configuración de caché.
   */
  private Cache cache = new Cache();

  /**
   * Configuración de timeouts y reintentos.
   */
  private Resilience resilience = new Resilience();

  /**
   * Configuración de métricas.
   */
  private Metrics metrics = new Metrics();

  // ========================================
  // GETTERS Y SETTERS PRINCIPALES
  // ========================================

  public Jce getJce() {
    return jce;
  }

  public void setJce(Jce jce) {
    this.jce = jce;
  }

  public RateLimit getRateLimit() {
    return rateLimit;
  }

  public void setRateLimit(RateLimit rateLimit) {
    this.rateLimit = rateLimit;
  }

  public Cache getCache() {
    return cache;
  }

  public void setCache(Cache cache) {
    this.cache = cache;
  }

  public Resilience getResilience() {
    return resilience;
  }

  public void setResilience(Resilience resilience) {
    this.resilience = resilience;
  }

  public Metrics getMetrics() {
    return metrics;
  }

  public void setMetrics(Metrics metrics) {
    this.metrics = metrics;
  }

  // ========================================
  // CLASES ANIDADAS DE CONFIGURACIÓN
  // ========================================

  /**
   * Configuración específica del portal JCE.
   */
  public static class Jce {

    /**
     * URL base del portal JCE.
     */
    @NotBlank(message = "La URL base del portal JCE es requerida")
    private String baseUrl = "https://dataportal.jce.gob.do";

    /**
     * ServiceID por defecto para las consultas.
     */
    @NotBlank(message = "El ServiceID es requerido")
    private String defaultServiceId = "ca8e51d3-8ddf-47a9-b385-0f0ffa08c65a";

    /**
     * Timeout para conexiones HTTP al portal JCE (en segundos).
     */
    @Min(value = 5, message = "El timeout de conexión debe ser al menos 5 segundos")
    @Max(value = 60, message = "El timeout de conexión no debe exceder 60 segundos")
    private int connectionTimeout = 15;

    /**
     * Timeout para lectura de respuesta del portal JCE (en segundos).
     */
    @Min(value = 10, message = "El timeout de lectura debe ser al menos 10 segundos")
    @Max(value = 120, message = "El timeout de lectura no debe exceder 120 segundos")
    private int readTimeout = 30;

    /**
     * User-Agent para usar en las peticiones al portal JCE.
     */
    @NotBlank(message = "El User-Agent es requerido")
    private String userAgent = "JCE-Consulta-Microservice/1.0.0";

    /**
     * Número máximo de reintentos para peticiones fallidas.
     */
    @Min(value = 0, message = "El número de reintentos no puede ser negativo")
    @Max(value = 5, message = "El número de reintentos no debe exceder 5")
    private int maxRetries = 2;

    /**
     * Habilitar compresión GZIP en las peticiones.
     */
    private boolean gzipEnabled = true;

    // Getters y Setters
    public String getBaseUrl() {
      return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
    }

    public String getDefaultServiceId() {
      return defaultServiceId;
    }

    public void setDefaultServiceId(String defaultServiceId) {
      this.defaultServiceId = defaultServiceId;
    }

    public int getConnectionTimeout() {
      return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
      this.connectionTimeout = connectionTimeout;
    }

    public int getReadTimeout() {
      return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
      this.readTimeout = readTimeout;
    }

    public String getUserAgent() {
      return userAgent;
    }

    public void setUserAgent(String userAgent) {
      this.userAgent = userAgent;
    }

    public int getMaxRetries() {
      return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
      this.maxRetries = maxRetries;
    }

    public boolean isGzipEnabled() {
      return gzipEnabled;
    }

    public void setGzipEnabled(boolean gzipEnabled) {
      this.gzipEnabled = gzipEnabled;
    }
  }

  /**
   * Configuración de rate limiting.
   */
  public static class RateLimit {

    /**
     * Número máximo de peticiones por ventana de tiempo.
     */
    @Min(value = 1, message = "El límite de peticiones debe ser al menos 1")
    @Max(value = 10000, message = "El límite de peticiones no debe exceder 10000")
    private int requestsPerWindow = 100;

    /**
     * Duración de la ventana de tiempo en minutos.
     */
    @Min(value = 1, message = "La ventana de tiempo debe ser al menos 1 minuto")
    @Max(value = 60, message = "La ventana de tiempo no debe exceder 60 minutos")
    private int windowSizeMinutes = 1;

    /**
     * Habilitar rate limiting distribuido con Redis.
     */
    private boolean distributedEnabled = true;

    /**
     * Prefijo para las keys de rate limit en Redis.
     */
    @NotBlank(message = "El prefijo de rate limit es requerido")
    private String redisKeyPrefix = "jce:ratelimit";

    /**
     * Habilitar rate limiting por IP.
     */
    private boolean perIpEnabled = true;

    /**
     * Habilitar rate limiting global.
     */
    private boolean globalEnabled = true;

    // Getters y Setters
    public int getRequestsPerWindow() {
      return requestsPerWindow;
    }

    public void setRequestsPerWindow(int requestsPerWindow) {
      this.requestsPerWindow = requestsPerWindow;
    }

    public int getWindowSizeMinutes() {
      return windowSizeMinutes;
    }

    public void setWindowSizeMinutes(int windowSizeMinutes) {
      this.windowSizeMinutes = windowSizeMinutes;
    }

    public boolean isDistributedEnabled() {
      return distributedEnabled;
    }

    public void setDistributedEnabled(boolean distributedEnabled) {
      this.distributedEnabled = distributedEnabled;
    }

    public String getRedisKeyPrefix() {
      return redisKeyPrefix;
    }

    public void setRedisKeyPrefix(String redisKeyPrefix) {
      this.redisKeyPrefix = redisKeyPrefix;
    }

    public boolean isPerIpEnabled() {
      return perIpEnabled;
    }

    public void setPerIpEnabled(boolean perIpEnabled) {
      this.perIpEnabled = perIpEnabled;
    }

    public boolean isGlobalEnabled() {
      return globalEnabled;
    }

    public void setGlobalEnabled(boolean globalEnabled) {
      this.globalEnabled = globalEnabled;
    }
  }

  /**
   * Configuración de caché.
   */
  public static class Cache {

    /**
     * TTL por defecto para caché de consultas (en minutos).
     */
    @Min(value = 1, message = "El TTL del caché debe ser al menos 1 minuto")
    @Max(value = 1440, message = "El TTL del caché no debe exceder 24 horas")
    private int defaultTtlMinutes = 60;

    /**
     * Tamaño máximo del caché en memoria (número de entradas).
     */
    @Min(value = 100, message = "El tamaño del caché debe ser al menos 100")
    @Max(value = 100000, message = "El tamaño del caché no debe exceder 100000")
    private int maxSize = 10000;

    /**
     * Habilitar caché distribuido con Redis.
     */
    private boolean distributedEnabled = true;

    /**
     * Prefijo para las keys de caché en Redis.
     */
    @NotBlank(message = "El prefijo de caché es requerido")
    private String redisKeyPrefix = "jce:cache";

    /**
     * Habilitar caché en memoria local.
     */
    private boolean localEnabled = true;

    /**
     * Habilitar estadísticas de caché.
     */
    private boolean statsEnabled = true;

    // Getters y Setters
    public int getDefaultTtlMinutes() {
      return defaultTtlMinutes;
    }

    public void setDefaultTtlMinutes(int defaultTtlMinutes) {
      this.defaultTtlMinutes = defaultTtlMinutes;
    }

    public int getMaxSize() {
      return maxSize;
    }

    public void setMaxSize(int maxSize) {
      this.maxSize = maxSize;
    }

    public boolean isDistributedEnabled() {
      return distributedEnabled;
    }

    public void setDistributedEnabled(boolean distributedEnabled) {
      this.distributedEnabled = distributedEnabled;
    }

    public String getRedisKeyPrefix() {
      return redisKeyPrefix;
    }

    public void setRedisKeyPrefix(String redisKeyPrefix) {
      this.redisKeyPrefix = redisKeyPrefix;
    }

    public boolean isLocalEnabled() {
      return localEnabled;
    }

    public void setLocalEnabled(boolean localEnabled) {
      this.localEnabled = localEnabled;
    }

    public boolean isStatsEnabled() {
      return statsEnabled;
    }

    public void setStatsEnabled(boolean statsEnabled) {
      this.statsEnabled = statsEnabled;
    }
  }

  /**
   * Configuración de resilencia (timeouts, circuit breakers, etc.).
   */
  public static class Resilience {

    /**
     * Timeout general para operaciones del microservicio (en segundos).
     */
    @Min(value = 5, message = "El timeout general debe ser al menos 5 segundos")
    @Max(value = 300, message = "El timeout general no debe exceder 300 segundos")
    private int operationTimeoutSeconds = 45;

    /**
     * Habilitar circuit breaker para llamadas al portal JCE.
     */
    private boolean circuitBreakerEnabled = true;

    /**
     * Número de fallos consecutivos para abrir el circuit breaker.
     */
    @Min(value = 3, message = "El umbral de fallos debe ser al menos 3")
    @Max(value = 20, message = "El umbral de fallos no debe exceder 20")
    private int circuitBreakerFailureThreshold = 5;

    /**
     * Tiempo de espera antes de intentar cerrar el circuit breaker (en segundos).
     */
    @Min(value = 10, message = "El tiempo de espera debe ser al menos 10 segundos")
    @Max(value = 300, message = "El tiempo de espera no debe exceder 300 segundos")
    private int circuitBreakerWaitDurationSeconds = 60;

    /**
     * Porcentaje de peticiones permitidas en estado half-open.
     */
    @Min(value = 10, message = "El porcentaje debe ser al menos 10%")
    @Max(value = 100, message = "El porcentaje no debe exceder 100%")
    private int circuitBreakerSlowCallRateThreshold = 50;

    /**
     * Habilitar retry automático para peticiones fallidas.
     */
    private boolean retryEnabled = true;

    /**
     * Número máximo de reintentos.
     */
    @Min(value = 1, message = "El número de reintentos debe ser al menos 1")
    @Max(value = 5, message = "El número de reintentos no debe exceder 5")
    private int maxRetryAttempts = 3;

    /**
     * Delay inicial entre reintentos (en milisegundos).
     */
    @Min(value = 500, message = "El delay debe ser al menos 500ms")
    @Max(value = 10000, message = "El delay no debe exceder 10000ms")
    private long retryDelayMillis = 1000;

    /**
     * Multiplicador exponencial para el delay de reintentos.
     */
    @Min(value = 1, message = "El multiplicador debe ser al menos 1")
    @Max(value = 5, message = "El multiplicador no debe exceder 5")
    private double retryMultiplier = 2.0;

    // Getters y Setters
    public int getOperationTimeoutSeconds() {
      return operationTimeoutSeconds;
    }

    public void setOperationTimeoutSeconds(int operationTimeoutSeconds) {
      this.operationTimeoutSeconds = operationTimeoutSeconds;
    }

    public boolean isCircuitBreakerEnabled() {
      return circuitBreakerEnabled;
    }

    public void setCircuitBreakerEnabled(boolean circuitBreakerEnabled) {
      this.circuitBreakerEnabled = circuitBreakerEnabled;
    }

    public int getCircuitBreakerFailureThreshold() {
      return circuitBreakerFailureThreshold;
    }

    public void setCircuitBreakerFailureThreshold(int circuitBreakerFailureThreshold) {
      this.circuitBreakerFailureThreshold = circuitBreakerFailureThreshold;
    }

    public int getCircuitBreakerWaitDurationSeconds() {
      return circuitBreakerWaitDurationSeconds;
    }

    public void setCircuitBreakerWaitDurationSeconds(int circuitBreakerWaitDurationSeconds) {
      this.circuitBreakerWaitDurationSeconds = circuitBreakerWaitDurationSeconds;
    }

    public int getCircuitBreakerSlowCallRateThreshold() {
      return circuitBreakerSlowCallRateThreshold;
    }

    public void setCircuitBreakerSlowCallRateThreshold(int circuitBreakerSlowCallRateThreshold) {
      this.circuitBreakerSlowCallRateThreshold = circuitBreakerSlowCallRateThreshold;
    }

    public boolean isRetryEnabled() {
      return retryEnabled;
    }

    public void setRetryEnabled(boolean retryEnabled) {
      this.retryEnabled = retryEnabled;
    }

    public int getMaxRetryAttempts() {
      return maxRetryAttempts;
    }

    public void setMaxRetryAttempts(int maxRetryAttempts) {
      this.maxRetryAttempts = maxRetryAttempts;
    }

    public long getRetryDelayMillis() {
      return retryDelayMillis;
    }

    public void setRetryDelayMillis(long retryDelayMillis) {
      this.retryDelayMillis = retryDelayMillis;
    }

    public double getRetryMultiplier() {
      return retryMultiplier;
    }

    public void setRetryMultiplier(double retryMultiplier) {
      this.retryMultiplier = retryMultiplier;
    }
  }

  /**
   * Configuración de métricas y monitoreo.
   */
  public static class Metrics {

    /**
     * Habilitar métricas personalizadas del microservicio.
     */
    private boolean customEnabled = true;

    /**
     * Habilitar métricas detalladas de JVM.
     */
    private boolean jvmEnabled = true;

    /**
     * Habilitar métricas de sistema (CPU, memoria, disco).
     */
    private boolean systemEnabled = true;

    /**
     * Habilitar métricas de HTTP (peticiones, respuestas, latencia).
     */
    private boolean httpEnabled = true;

    /**
     * Habilitar métricas de caché.
     */
    private boolean cacheEnabled = true;

    /**
     * Habilitar métricas de circuit breaker.
     */
    private boolean circuitBreakerEnabled = true;

    /**
     * Prefijo para nombres de métricas.
     */
    @NotBlank(message = "El prefijo de métricas es requerido")
    private String namePrefix = "jce.consulta";

    /**
     * Tags comunes para todas las métricas.
     */
    private String commonTags = "service=jce-consulta,version=1.0.0";

    /**
     * Intervalo de publicación de métricas (en segundos).
     */
    @Min(value = 10, message = "El intervalo debe ser al menos 10 segundos")
    @Max(value = 300, message = "El intervalo no debe exceder 300 segundos")
    private int publishIntervalSeconds = 60;

    /**
     * Habilitar export de métricas a sistemas externos (Prometheus, etc.).
     */
    private boolean exportEnabled = true;

    // Getters y Setters
    public boolean isCustomEnabled() {
      return customEnabled;
    }

    public void setCustomEnabled(boolean customEnabled) {
      this.customEnabled = customEnabled;
    }

    public boolean isJvmEnabled() {
      return jvmEnabled;
    }

    public void setJvmEnabled(boolean jvmEnabled) {
      this.jvmEnabled = jvmEnabled;
    }

    public boolean isSystemEnabled() {
      return systemEnabled;
    }

    public void setSystemEnabled(boolean systemEnabled) {
      this.systemEnabled = systemEnabled;
    }

    public boolean isHttpEnabled() {
      return httpEnabled;
    }

    public void setHttpEnabled(boolean httpEnabled) {
      this.httpEnabled = httpEnabled;
    }

    public boolean isCacheEnabled() {
      return cacheEnabled;
    }

    public void setCacheEnabled(boolean cacheEnabled) {
      this.cacheEnabled = cacheEnabled;
    }

    public boolean isCircuitBreakerEnabled() {
      return circuitBreakerEnabled;
    }

    public void setCircuitBreakerEnabled(boolean circuitBreakerEnabled) {
      this.circuitBreakerEnabled = circuitBreakerEnabled;
    }

    public String getNamePrefix() {
      return namePrefix;
    }

    public void setNamePrefix(String namePrefix) {
      this.namePrefix = namePrefix;
    }

    public String getCommonTags() {
      return commonTags;
    }

    public void setCommonTags(String commonTags) {
      this.commonTags = commonTags;
    }

    public int getPublishIntervalSeconds() {
      return publishIntervalSeconds;
    }

    public void setPublishIntervalSeconds(int publishIntervalSeconds) {
      this.publishIntervalSeconds = publishIntervalSeconds;
    }

    public boolean isExportEnabled() {
      return exportEnabled;
    }

    public void setExportEnabled(boolean exportEnabled) {
      this.exportEnabled = exportEnabled;
    }
  }

  // ========================================
  // MÉTODOS UTILITARIOS
  // ========================================

  /**
   * Obtiene el timeout de conexión como Duration.
   */
  public Duration getConnectionTimeoutDuration() {
    return Duration.ofSeconds(jce.getConnectionTimeout());
  }

  /**
   * Obtiene el timeout de lectura como Duration.
   */
  public Duration getReadTimeoutDuration() {
    return Duration.ofSeconds(jce.getReadTimeout());
  }

  /**
   * Obtiene el TTL de caché como Duration.
   */
  public Duration getCacheTtlDuration() {
    return Duration.ofMinutes(cache.getDefaultTtlMinutes());
  }

  /**
   * Obtiene la ventana de rate limit como Duration.
   */
  public Duration getRateLimitWindowDuration() {
    return Duration.ofMinutes(rateLimit.getWindowSizeMinutes());
  }

  /**
   * Obtiene el timeout de operación como Duration.
   */
  public Duration getOperationTimeoutDuration() {
    return Duration.ofSeconds(resilience.getOperationTimeoutSeconds());
  }

  /**
   * Verifica si el caché distribuido está habilitado.
   */
  public boolean isDistributedCacheEnabled() {
    return cache.isDistributedEnabled();
  }

  /**
   * Verifica si el rate limiting distribuido está habilitado.
   */
  public boolean isDistributedRateLimitEnabled() {
    return rateLimit.isDistributedEnabled();
  }

  /**
   * Obtiene la configuración completa como String para logging.
   */
  public String getConfigurationSummary() {
    return String.format("""
        JCE Consulta Configuration:
        - JCE Base URL: %s
        - Connection Timeout: %ds
        - Read Timeout: %ds
        - Rate Limit: %d requests per %d min(s)
        - Cache TTL: %d min(s)
        - Circuit Breaker: %s
        - Retry: %s (max %d attempts)
        - Distributed Cache: %s
        - Distributed Rate Limit: %s
        """,
        jce.getBaseUrl(),
        jce.getConnectionTimeout(),
        jce.getReadTimeout(),
        rateLimit.getRequestsPerWindow(),
        rateLimit.getWindowSizeMinutes(),
        cache.getDefaultTtlMinutes(),
        resilience.isCircuitBreakerEnabled() ? "enabled" : "disabled",
        resilience.isRetryEnabled() ? "enabled" : "disabled",
        resilience.getMaxRetryAttempts(),
        cache.isDistributedEnabled() ? "enabled" : "disabled",
        rateLimit.isDistributedEnabled() ? "enabled" : "disabled");
  }
}
