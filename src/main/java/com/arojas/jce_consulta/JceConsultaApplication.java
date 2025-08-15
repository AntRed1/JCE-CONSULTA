package com.arojas.jce_consulta;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableAsync;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Aplicación principal del Microservicio JCE Consulta.
 * 
 * Este microservicio profesional proporciona una API REST moderna para
 * consultar
 * datos ciudadanos en el portal de la Junta Central Electoral (JCE) de
 * República Dominicana.
 * 
 * Características principales:
 * - API REST con Spring Boot 3.5.0 y Java 21
 * - Rate limiting avanzado con Bucket4j
 * - Documentación automática con OpenAPI 3
 * - Validación robusta de cédulas dominicanas
 * - Caché inteligente con Redis
 * - Métricas y monitoreo con Actuator
 * - Cliente HTTP reactivo con WebFlux
 * - Manejo elegante de errores
 * 
 * @author A. Rojas
 * @version 1.0.0
 * @since 2024
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
@ConfigurationPropertiesScan
@OpenAPIDefinition(info = @Info(title = "JCE Consulta Microservice API", version = "1.0.0", description = """
        **Microservicio profesional para consulta de datos ciudadanos en la JCE de República Dominicana**

        Esta API permite realizar consultas de información personal registrada en la
        Junta Central Electoral (JCE) mediante el número de cédula de identidad dominicana.

        ## Características principales:
        * ✅ **Validación robusta** de formato de cédula dominicana (11 dígitos)
        * ⚡ **Rate limiting** inteligente para prevenir abuso
        * 🔄 **Caché** con TTL configurable para optimizar rendimiento
        * 📊 **Métricas** y monitoreo integrado con Prometheus
        * 🛡️ **Manejo elegante de errores** con códigos HTTP estándar
        * 📝 **Logs estructurados** para auditoría y debugging
        * 🚀 **Cliente HTTP reactivo** para máximo rendimiento

        ## Datos retornados:
        La API retorna información completa del ciudadano incluyendo:
        datos personales, estado civil, nacionalidad, información familiar,
        fechas relevantes, estatus y más.

        ## Formato de respuesta:
        Todas las respuestas están en formato JSON moderno con estructura consistente,
        incluyendo metadatos de la consulta, timestamps y códigos de estado.

        ## Rate Limiting:
        - **100 solicitudes por minuto** por IP por defecto
        - Headers informativos sobre límites restantes
        - Respuestas HTTP 429 cuando se excede el límite

        ---

        🇩🇴 **Desarrollado para República Dominicana** - Cumple con estándares locales de cédulas
        """, contact = @Contact(name = "A. Rojas - Lead Developer", email = "contacto@arojas.dev", url = "https://arojas.dev"), license = @License(name = "MIT License", url = "https://opensource.org/licenses/MIT"), termsOfService = "https://arojas.dev/terms"), servers = {
        @Server(description = "Desarrollo Local", url = "http://localhost:8080/api/v1"),
        @Server(description = "Producción", url = "https://api.arojas.dev/jce-consulta/v1")
}, tags = {
        @Tag(name = "JCE Consulta", description = "Operaciones de consulta de datos ciudadanos en la JCE"),
        @Tag(name = "Health & Monitoring", description = "Endpoints de salud y monitoreo del microservicio")
})
public class JceConsultaApplication {

    private static final Logger logger = LoggerFactory.getLogger(JceConsultaApplication.class);

    /**
     * Método principal para iniciar la aplicación Spring Boot.
     * 
     * @param args argumentos de línea de comandos
     */
    public static void main(String[] args) {
        // Configurar propiedades del sistema para mejor rendimiento
        configureSystemProperties();

        try {
            ConfigurableApplicationContext context = SpringApplication.run(JceConsultaApplication.class, args);
            logApplicationStartup(context);
        } catch (Exception e) {
            logger.error("❌ Error crítico durante el inicio de la aplicación", e);
            System.exit(1);
        }
    }

    /**
     * Configura propiedades del sistema para optimizar el rendimiento.
     */
    private static void configureSystemProperties() {
        // Optimizaciones JVM para contenedores y microservicios
        System.setProperty("java.awt.headless", "true");
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("user.timezone", "America/Santo_Domingo");

        // Configuraciones de red optimizadas
        System.setProperty("networkaddress.cache.ttl", "60");
        System.setProperty("networkaddress.cache.negative.ttl", "10");

        logger.info("🔧 Propiedades del sistema configuradas para óptimo rendimiento");
    }

    /**
     * Registra información detallada sobre el inicio exitoso de la aplicación.
     * 
     * @param context contexto de la aplicación Spring
     */
    private static void logApplicationStartup(ConfigurableApplicationContext context) {
        Environment env = context.getEnvironment();

        try {
            String serverPort = env.getProperty("server.port", "8080");
            String contextPath = env.getProperty("server.servlet.context-path", "");
            String hostAddress = InetAddress.getLocalHost().getHostAddress();
            String hostName = InetAddress.getLocalHost().getHostName();
            String[] activeProfiles = env.getActiveProfiles();
            String profilesStr = activeProfiles.length == 0 ? "default" : Arrays.toString(activeProfiles);

            String startupTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            logger.info("""

                    \ud83d\ude80 ============================================
                       JCE CONSULTA MICROSERVICE INICIADO
                    ============================================
                    \ud83d\udcc5 Timestamp: {}
                    \ud83c\udff7\ufe0f  Aplicaci\u00f3n: {}
                    \ud83d\udce6 Versi\u00f3n: {}
                    \u2615 Java: {} ({})
                    \ud83c\udf31 Spring Boot: {}
                    \ud83d\udd27 Perfiles: {}
                    \ud83d\udda5\ufe0f  Host: {} ({})
                    \ud83c\udf10 URLs de acceso:
                       \u2022 Local:    http://localhost:{}{}
                       \u2022 Red:      http://{}:{}{}
                       \u2022 Swagger:  http://localhost:{}{}/swagger-ui.html
                       \u2022 API Docs: http://localhost:{}{}/api-docs
                       \u2022 Health:   http://localhost:{}{}/actuator/health
                       \u2022 Metrics:  http://localhost:{}{}/actuator/metrics
                    ============================================
                    """,
                    startupTime,
                    env.getProperty("spring.application.name", "jce-consulta-ms"),
                    env.getProperty("info.app.version", "1.0.0"),
                    System.getProperty("java.version"),
                    System.getProperty("java.vm.name"),
                    env.getProperty("spring.boot.version", "3.5.0"),
                    profilesStr,
                    hostName, hostAddress,
                    serverPort, contextPath,
                    hostAddress, serverPort, contextPath,
                    serverPort, contextPath,
                    serverPort, contextPath,
                    serverPort, contextPath,
                    serverPort, contextPath);

            // Log adicional para desarrollo
            if (Arrays.asList(activeProfiles).contains("dev")) {
                logger.info("🔍 Modo desarrollo activado - Logging detallado habilitado");
                logger.info("📝 Logs de la aplicación: {}",
                        env.getProperty("logging.file.path", "logs"));
            }

            // Información de configuración JCE
            logger.info("🏛️ Configuración JCE Portal:");
            logger.info("   • Base URL: {}", env.getProperty("jce.portal.base-url"));
            logger.info("   • Service ID: {}", env.getProperty("jce.portal.service-id"));
            logger.info("   • Timeout: {}ms", env.getProperty("jce.portal.timeout"));

            // Información de Rate Limiting
            if (env.getProperty("bucket4j.enabled", Boolean.class, false)) {
                logger.info("⚡ Rate Limiting activado - Límites aplicados por IP");
            }

            // Información de Cache
            String cacheType = env.getProperty("spring.cache.type", "none");
            logger.info("🗄️ Cache configurado: {}", cacheType.toUpperCase());

            logger.info("✅ Microservicio listo para recibir peticiones");

        } catch (UnknownHostException e) {
            logger.warn("⚠️ No se pudo determinar la dirección del host: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("❌ Error registrando información de startup", e);
        }
    }
}
