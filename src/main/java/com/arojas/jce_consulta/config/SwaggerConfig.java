/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.arojas.jce_consulta.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuración avanzada de OpenAPI 3 (Swagger) para el microservicio JCE
 * Consulta.
 * 
 * Esta configuración proporciona documentación automática completa de la API,
 * incluyendo esquemas de respuesta, ejemplos, códigos de error estándar,
 * y información detallada sobre los endpoints disponibles.
 * 
 * @author A. Rojas
 * @version 1.0.0
 */
@Configuration
public class SwaggerConfig {

  @Value("${spring.application.name}")
  private String applicationName;

  @Value("${info.app.version}")
  private String applicationVersion;

  @Value("${server.port}")
  private String serverPort;

  @Value("${server.servlet.context-path}")
  private String contextPath;

  /**
   * Configuración principal de OpenAPI.
   * 
   * @return configuración OpenAPI completa
   */
  @Bean
  public OpenAPI customOpenAPI() {
    return new OpenAPI()
        .info(createApiInfo())
        .servers(createServers())
        .components(createComponents())
        .externalDocs(createExternalDocumentation());
  }

  /**
   * Grupo de APIs públicas para consultas JCE.
   */
  @Bean
  public GroupedOpenApi publicApi() {
    return GroupedOpenApi.builder()
        .group("jce-consulta-public")
        .displayName("JCE Consulta - API Pública")
        .pathsToMatch("/consulta/**")
        .addOpenApiCustomizer(publicApiCustomizer())
        .build();
  }

  /**
   * Grupo de APIs de monitoreo y administración.
   */
  @Bean
  public GroupedOpenApi managementApi() {
    return GroupedOpenApi.builder()
        .group("management")
        .displayName("Monitoreo y Administración")
        .pathsToMatch("/actuator/**")
        .addOpenApiCustomizer(managementApiCustomizer())
        .build();
  }

  /**
   * Información detallada de la API.
   */
  private Info createApiInfo() {
    return new Info()
        .title("🏛️ JCE Consulta Microservice API")
        .version(applicationVersion)
        .description("""
            ## 🇩🇴 Microservicio Profesional para Consulta JCE República Dominicana

            Esta API REST moderna permite consultar información ciudadana registrada en la
            **Junta Central Electoral (JCE)** de República Dominicana usando el número de cédula.

            ### ✨ Características Principales

            | Característica | Descripción |
            |----------------|-------------|
            | 🔍 **Consulta Avanzada** | Validación robusta de cédulas dominicanas |
            | ⚡ **Rate Limiting** | Protección contra abuso (100 req/min por IP) |
            | 🗄️ **Caché Inteligente** | Optimización con Redis y TTL configurable |
            | 📊 **Métricas Completas** | Monitoreo con Prometheus y Actuator |
            | 🛡️ **Manejo de Errores** | Respuestas consistentes con códigos HTTP |
            | 📝 **Logs Estructurados** | Auditoría y debugging avanzado |
            | 🚀 **Alto Rendimiento** | Cliente HTTP reactivo con WebFlux |

            ### 📋 Formato de Cédula Dominicana

            La API acepta cédulas en estos formatos:
            - **Con guiones**: `001-1234567-1`
            - **Sin guiones**: `00112345671`

            **Estructura**: `XXX-XXXXXXX-X`
            - `XXX`: Código del municipio (001-999)
            - `XXXXXXX`: Número secuencial (0000001-9999999)
            - `X`: Dígito verificador (0-9)

            ### 🔄 Tipos de Consulta

            | Formato | Descripción | Campos Incluidos |
            |---------|-------------|------------------|
            | `completo` | Todos los datos disponibles | Todos los campos |
            | `basico` | Solo información esencial | Nombre, cédula, estado |
            | `personal` | Datos personales básicos | Info personal sin familia |
            | `familiar` | Incluye información familiar | Personal + cónyuge/padres |

            ### 📊 Códigos de Respuesta

            | Código | Significado | Descripción |
            |--------|-------------|-------------|
            | `200` | ✅ **Éxito** | Consulta exitosa con datos |
            | `400` | ❌ **Bad Request** | Cédula inválida o parámetros erróneos |
            | `404` | 🔍 **Not Found** | Ciudadano no encontrado en JCE |
            | `429` | ⏱️ **Rate Limited** | Límite de solicitudes excedido |
            | `500` | 🔧 **Server Error** | Error interno del servidor |
            | `503` | 🚫 **Service Unavailable** | JCE no disponible temporalmente |

            ### 🔐 Rate Limiting

            - **Límite**: 100 solicitudes por minuto por IP
            - **Headers informativos**:
              - `X-RateLimit-Remaining`: Solicitudes restantes
              - `X-RateLimit-Reset`: Tiempo hasta reset (epoch)
              - `X-RateLimit-Limit`: Límite total

            ### 📈 Monitoreo

            - **Health Check**: `/actuator/health`
            - **Métricas**: `/actuator/metrics`
            - **Prometheus**: `/actuator/prometheus`

            ### 🛠️ Tecnologías

            - **Java 21** con características modernas
            - **Spring Boot 3.5.0** última versión
            - **WebFlux** para cliente HTTP reactivo
            - **Redis** para caché distribuido
            - **Bucket4j** para rate limiting avanzado
            - **Micrometer** para métricas y observabilidad

            ---

            💡 **Consejo**: Use el formato `completo` para obtener toda la información disponible,
            o `basico` para consultas más rápidas con datos esenciales.
            """)
        .termsOfService("https://arojas.dev/terms")
        .contact(new Contact()
            .name("A. Rojas - Lead Developer")
            .email("contacto@arojas.dev")
            .url("https://arojas.dev"))
        .license(new License()
            .name("MIT License")
            .url("https://opensource.org/licenses/MIT"));
  }

  /**
   * Servidores disponibles para la API.
   */
  private List<Server> createServers() {
    return List.of(
        new Server()
            .url("http://localhost:" + serverPort + contextPath)
            .description("🔧 Servidor de Desarrollo Local"),
        new Server()
            .url("https://api.arojas.dev/jce-consulta/v1")
            .description("🚀 Servidor de Producción"),
        new Server()
            .url("https://staging-api.arojas.dev/jce-consulta/v1")
            .description("🧪 Servidor de Staging"));
  }

  /**
   * Componentes reutilizables de la API.
   */
  private Components createComponents() {
    return new Components()
        .addSecuritySchemes("ApiKey", createApiKeySecurityScheme())
        .addResponses("BadRequest", createBadRequestResponse())
        .addResponses("NotFound", createNotFoundResponse())
        .addResponses("RateLimited", createRateLimitedResponse())
        .addResponses("InternalError", createInternalErrorResponse())
        .addResponses("ServiceUnavailable", createServiceUnavailableResponse());
  }

  /**
   * Documentación externa relacionada.
   */
  private ExternalDocumentation createExternalDocumentation() {
    return new ExternalDocumentation()
        .description("🇩🇴 Portal Oficial JCE República Dominicana")
        .url("https://jce.gob.do");
  }

  /**
   * Esquema de seguridad API Key (para futuras versiones).
   */
  private SecurityScheme createApiKeySecurityScheme() {
    return new SecurityScheme()
        .type(SecurityScheme.Type.APIKEY)
        .in(SecurityScheme.In.HEADER)
        .name("X-API-Key")
        .description("API Key para autenticación (funcionalidad futura)");
  }

  /**
   * Customizador para APIs públicas.
   */
  private OpenApiCustomizer publicApiCustomizer() {
    return openApi -> {
      openApi.getPaths().values().stream()
          .flatMap(pathItem -> pathItem.readOperations().stream())
          .forEach(operation -> {
            operation.addTagsItem("JCE Consulta");

            // Agregar respuestas estándar
            operation.getResponses()
                .addApiResponse("400", new ApiResponse().$ref("#/components/responses/BadRequest"))
                .addApiResponse("429", new ApiResponse().$ref("#/components/responses/RateLimited"))
                .addApiResponse("500", new ApiResponse().$ref("#/components/responses/InternalError"));
          });
    };
  }

  /**
   * Customizador para APIs de administración.
   */
  private OpenApiCustomizer managementApiCustomizer() {
    return openApi -> {
      openApi.getPaths().values().stream()
          .flatMap(pathItem -> pathItem.readOperations().stream())
          .forEach(operation -> operation.addTagsItem("Health & Monitoring"));
    };
  }

  // ========================================
  // RESPUESTAS ESTÁNDAR
  // ========================================

  private ApiResponse createBadRequestResponse() {
    return new ApiResponse()
        .description("❌ Solicitud inválida - Cédula con formato incorrecto o parámetros erróneos")
        .content(new Content()
            .addMediaType("application/json", new MediaType()
                .schema(new Schema<>()
                    .example("""
                        {
                            "exitosa": false,
                            "mensaje": "La cédula debe tener el formato XXX-XXXXXXX-X o XXXXXXXXXXX",
                            "codigo": "CEDULA_INVALIDA",
                            "timestamp": "2024-12-15T14:30:45",
                            "cedulaConsultada": "12345"
                        }
                        """))));
  }

  private ApiResponse createNotFoundResponse() {
    return new ApiResponse()
        .description("🔍 Ciudadano no encontrado en los registros de la JCE")
        .content(new Content()
            .addMediaType("application/json", new MediaType()
                .schema(new Schema<>()
                    .example("""
                        {
                            "exitosa": false,
                            "mensaje": "No se encontraron datos para la cédula consultada",
                            "codigo": "CIUDADANO_NO_ENCONTRADO",
                            "timestamp": "2024-12-15T14:30:45",
                            "cedulaConsultada": "001-1234567-1"
                        }
                        """))));
  }

  private ApiResponse createRateLimitedResponse() {
    return new ApiResponse()
        .description("⏱️ Límite de solicitudes excedido - Demasiadas peticiones en poco tiempo")
        .content(new Content()
            .addMediaType("application/json", new MediaType()
                .schema(new Schema<>()
                    .example(
                        """
                            {
                                "error": "Límite de solicitudes excedido",
                                "message": "Has superado el límite de 100 solicitudes por minuto. Intenta nuevamente en 45 segundos.",
                                "code": "RATE_LIMIT_EXCEEDED",
                                "timestamp": "2024-12-15T14:30:45"
                            }
                            """))));
  }

  private ApiResponse createInternalErrorResponse() {
    return new ApiResponse()
        .description("🔧 Error interno del servidor")
        .content(new Content()
            .addMediaType("application/json", new MediaType()
                .schema(new Schema<>()
                    .example("""
                        {
                            "exitosa": false,
                            "mensaje": "Error interno procesando la solicitud",
                            "codigo": "ERROR_INTERNO",
                            "timestamp": "2024-12-15T14:30:45"
                        }
                        """))));
  }

  private ApiResponse createServiceUnavailableResponse() {
    return new ApiResponse()
        .description("🚫 Servicio JCE temporalmente no disponible")
        .content(new Content()
            .addMediaType("application/json", new MediaType()
                .schema(new Schema<>()
                    .example("""
                        {
                            "exitosa": false,
                            "mensaje": "El portal de la JCE está temporalmente no disponible",
                            "codigo": "JCE_NO_DISPONIBLE",
                            "timestamp": "2024-12-15T14:30:45"
                        }
                        """))));
  }
}
