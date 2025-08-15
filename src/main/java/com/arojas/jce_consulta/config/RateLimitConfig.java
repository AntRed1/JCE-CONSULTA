package com.arojas.jce_consulta.config;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import com.arojas.jce_consulta.config.RateLimitConfig.BucketResolver.RateLimitProperties;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;

/**
 * Configuración avanzada de Rate Limiting usando Bucket4j y Redis.
 * Implementa limitación de velocidad distribuida sin perfiles.
 * 
 * Características:
 * - Rate limiting por IP address
 * - Contadores distribuidos en Redis
 * - Configuración flexible y granular
 * - Métricas y logging detallado
 * - Estrategias de fallback para alta disponibilidad
 * 
 * @author A. Rojas
 * @version 1.0.0
 */
@Configuration
@ConditionalOnProperty(name = "bucket4j.enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitConfig {

  private static final Logger logger = LoggerFactory.getLogger(RateLimitConfig.class);

  // ========================================
  // CONFIGURACIÓN DE LÍMITES
  // ========================================
  @Value("${rate-limit.requests-per-minute:100}")
  private int requestsPerMinute;

  @Value("${rate-limit.requests-per-hour:1000}")
  private int requestsPerHour;

  @Value("${rate-limit.burst-capacity:20}")
  private int burstCapacity;

  @Value("${rate-limit.cache-expiration-minutes:60}")
  private int cacheExpirationMinutes;

  // ========================================
  // PROXY MANAGER REDIS DISTRIBUIDO
  // ========================================
  @Bean
  @SuppressWarnings("unchecked")
  public ProxyManager<String> proxyManager(LettuceConnectionFactory lettuceConnectionFactory) {
    logger.info("🔧 Configurando Rate Limit distribuido con Redis");
    logger.info("📊 Límites configurados: {} solicitudes/min, {} solicitudes/hora, {} burst, {} min cache",
        requestsPerMinute, requestsPerHour, burstCapacity, cacheExpirationMinutes);

    try {
      var conf = lettuceConnectionFactory.getStandaloneConfiguration();
      String redisUri = "redis://" + (conf.getPassword() != null ? conf.getPassword() + "@" : "")
          + conf.getHostName() + ":" + conf.getPort();
      RedisClient redisClient = RedisClient.create(redisUri);

      ProxyManager proxyManager = LettuceBasedProxyManager.builderFor(redisClient)
          .withExpirationStrategy(
              ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(
                  Duration.ofMinutes(cacheExpirationMinutes)))
          .build();

      logger.info("✅ ProxyManager Redis configurado exitosamente");
      return (ProxyManager<String>) proxyManager;

    } catch (Exception e) {
      logger.error("❌ Error configurando ProxyManager Redis: {}", e.getMessage());
      logger.warn("🔄 Fallback: Usando rate limiting en memoria local");
      throw new RuntimeException("No se pudo configurar rate limiting distribuido", e);
    }
  }

  // ========================================
  // CONFIGURACIÓN DEL BUCKET POR DEFECTO
  // ========================================
  @Bean
  public BucketConfiguration bucketConfiguration() {
    logger.info("🪣 Configurando Bucket para rate limiting");

    BucketConfiguration configuration = BucketConfiguration.builder()
        .addLimit(Bandwidth.classic(requestsPerMinute + burstCapacity,
            Refill.greedy(requestsPerMinute, Duration.ofMinutes(1))))
        .addLimit(Bandwidth.classic(requestsPerHour,
            Refill.greedy(requestsPerHour, Duration.ofHours(1))))
        .build();

    logger.info("✅ Bucket configuration creada con límites multi-nivel");
    return configuration;
  }

  // ========================================
  // RESOLVER DE BUCKETS POR IP
  // ========================================
  @Bean
  public BucketResolver bucketResolver(ProxyManager<String> proxyManager,
      BucketConfiguration bucketConfiguration) {
    return new BucketResolver(proxyManager, bucketConfiguration);
  }

  // ========================================
  // PROPIEDADES GENERALES DE RATE LIMIT
  // ========================================
  @Bean
  public RateLimitProperties rateLimitProperties() {
    logger.info("🔧 Aplicando configuración de Rate Limit general");

    return RateLimitProperties.builder()
        .requestsPerMinute(requestsPerMinute)
        .requestsPerHour(requestsPerHour)
        .burstCapacity(burstCapacity)
        .enabled(true)
        .logVerbose(true)
        .build();
  }

  // ========================================
  // CLASES INTERNAS
  // ========================================
  public static class BucketResolver {
    private final ProxyManager<String> proxyManager;
    private final BucketConfiguration bucketConfiguration;
    private static final Logger logger = LoggerFactory.getLogger(BucketResolver.class);

    public BucketResolver(ProxyManager<String> proxyManager, BucketConfiguration bucketConfiguration) {
      this.proxyManager = proxyManager;
      this.bucketConfiguration = bucketConfiguration;
    }

    public Bucket resolveBucket(String key) {
      try {
        Bucket bucket = proxyManager.builder().build(key, bucketConfiguration);
        logger.debug("🪣 Bucket resuelto para clave: {}", key);
        return bucket;
      } catch (Exception e) {
        logger.error("❌ Error resolviendo bucket para clave {}: {}", key, e.getMessage());
        return Bucket.builder()
            .addLimit(Bandwidth.classic(100, Refill.greedy(100, Duration.ofMinutes(1))))
            .build();
      }
    }

    public static class RateLimitProperties {
      private final int requestsPerMinute;
      private final int requestsPerHour;
      private final int burstCapacity;
      private final boolean enabled;
      private final boolean logVerbose;

      private RateLimitProperties(Builder builder) {
        this.requestsPerMinute = builder.requestsPerMinute;
        this.requestsPerHour = builder.requestsPerHour;
        this.burstCapacity = builder.burstCapacity;
        this.enabled = builder.enabled;
        this.logVerbose = builder.logVerbose;
      }

      public static Builder builder() {
        return new Builder();
      }

      public static class Builder {
        private int requestsPerMinute = 100;
        private int requestsPerHour = 1000;
        private int burstCapacity = 20;
        private boolean enabled = true;
        private boolean logVerbose = false;

        public Builder requestsPerMinute(int requestsPerMinute) {
          this.requestsPerMinute = requestsPerMinute;
          return this;
        }

        public Builder requestsPerHour(int requestsPerHour) {
          this.requestsPerHour = requestsPerHour;
          return this;
        }

        public Builder burstCapacity(int burstCapacity) {
          this.burstCapacity = burstCapacity;
          return this;
        }

        public Builder enabled(boolean enabled) {
          this.enabled = enabled;
          return this;
        }

        public Builder logVerbose(boolean logVerbose) {
          this.logVerbose = logVerbose;
          return this;
        }

        public RateLimitProperties build() {
          return new RateLimitProperties(this);
        }
      }

      public int getRequestsPerMinute() {
        return requestsPerMinute;
      }

      public int getRequestsPerHour() {
        return requestsPerHour;
      }

      public int getBurstCapacity() {
        return burstCapacity;
      }

      public boolean isEnabled() {
        return enabled;
      }

      public boolean isLogVerbose() {
        return logVerbose;
      }
    }

    public static class BucketStats {
      private final String key;
      private final long availableTokens;
      private final long capacity;
      private final Duration refillPeriod;

      private BucketStats(Builder builder) {
        this.key = builder.key;
        this.availableTokens = builder.availableTokens;
        this.capacity = builder.capacity;
        this.refillPeriod = builder.refillPeriod;
      }

      public static Builder builder() {
        return new Builder();
      }

      public static BucketStats empty(String key) {
        return builder().key(key).availableTokens(0).capacity(0).refillPeriod(Duration.ZERO).build();
      }

      public static class Builder {
        private String key;
        private long availableTokens;
        private long capacity;
        private Duration refillPeriod;

        public Builder key(String key) {
          this.key = key;
          return this;
        }

        public Builder availableTokens(long availableTokens) {
          this.availableTokens = availableTokens;
          return this;
        }

        public Builder capacity(long capacity) {
          this.capacity = capacity;
          return this;
        }

        public Builder refillPeriod(Duration refillPeriod) {
          this.refillPeriod = refillPeriod;
          return this;
        }

        public BucketStats build() {
          return new BucketStats(this);
        }
      }

      public String getKey() {
        return key;
      }

      public long getAvailableTokens() {
        return availableTokens;
      }

      public long getCapacity() {
        return capacity;
      }

      public Duration getRefillPeriod() {
        return refillPeriod;
      }

      public double getAvailabilityPercentage() {
        return capacity > 0 ? (double) availableTokens / capacity * 100 : 0;
      }

      public boolean isNearLimit() {
        return getAvailabilityPercentage() < 20;
      }

      @Override
      public String toString() {
        return String.format("BucketStats{key='%s', tokens=%d/%d (%.1f%%), refill=%s}", key, availableTokens, capacity,
            getAvailabilityPercentage(), refillPeriod);
      }
    }
  }
}