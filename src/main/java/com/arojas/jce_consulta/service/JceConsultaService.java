/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.arojas.jce_consulta.service;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.arojas.jce_consulta.DTOs.ConsultaRequest;
import com.arojas.jce_consulta.DTOs.ConsultaResponse;
import com.arojas.jce_consulta.DTOs.ConsultaResponse.DatosCiudadano;
import com.arojas.jce_consulta.DTOs.ConsultaResponse.InformacionFoto;
import com.arojas.jce_consulta.client.JceHttpClient;
import com.arojas.jce_consulta.exceptions.ApiException;
import com.arojas.jce_consulta.model.Individuo;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import reactor.core.publisher.Mono;

/**
 * Servicio principal de negocio para consultas JCE.
 * 
 * Esta clase contiene la lógica de negocio principal del microservicio,
 * incluyendo validaciones, transformaciones de datos, manejo de caché,
 * y orquestación de llamadas a servicios externos.
 * 
 * Características:
 * - Validación robusta de cédulas dominicanas
 * - Caché inteligente con TTL configurable
 * - Métricas detalladas con Micrometer
 * - Filtrado de respuestas por formato solicitado
 * - Manejo elegante de errores de negocio
 * - Logging estructurado para auditoría
 * 
 * @author A. Rojas
 * @version 1.0.0
 */
@Service
public class JceConsultaService {

  private static final Logger logger = LoggerFactory.getLogger(JceConsultaService.class);

  // ========================================
  // DEPENDENCIAS Y CONFIGURACIÓN
  // ========================================

  private final JceHttpClient jceHttpClient;
  private final MeterRegistry meterRegistry;

  // Métricas
  private final Counter consultasExitosasCounter;
  private final Counter consultasErrorCounter;
  private final Counter cedulasInvalidasCounter;
  private final Counter ciudadanosNoEncontradosCounter;
  private final Timer consultaTimer;

  // Configuración
  private final String baseUrlJce;

  // Formatos válidos
  private static final Set<String> FORMATOS_VALIDOS = Set.of("completo", "basico", "personal", "familiar");

  /**
   * Constructor con inyección de dependencias.
   */
  public JceConsultaService(
      JceHttpClient jceHttpClient,
      MeterRegistry meterRegistry,
      @Value("${jce.consulta.jce.base-url}") String baseUrlJce) {

    this.jceHttpClient = jceHttpClient;
    this.meterRegistry = meterRegistry;
    this.baseUrlJce = baseUrlJce;

    // Inicializar métricas
    this.consultasExitosasCounter = Counter.builder("jce.consultas.exitosas")
        .description("Número total de consultas exitosas a la JCE")
        .register(meterRegistry);

    this.consultasErrorCounter = Counter.builder("jce.consultas.error")
        .description("Número total de consultas fallidas")
        .register(meterRegistry);

    this.cedulasInvalidasCounter = Counter.builder("jce.cedulas.invalidas")
        .description("Número de cédulas con formato inválido")
        .register(meterRegistry);

    this.ciudadanosNoEncontradosCounter = Counter.builder("jce.ciudadanos.no_encontrados")
        .description("Número de ciudadanos no encontrados en JCE")
        .register(meterRegistry);

    this.consultaTimer = Timer.builder("jce.consulta.duracion")
        .description("Duración de las consultas a la JCE")
        .register(meterRegistry);

    logger.info("🚀 JceConsultaService inicializado con métricas habilitadas");
  }

  // ========================================
  // MÉTODOS PÚBLICOS
  // ========================================

  /**
   * Consulta principal para obtener datos de un ciudadano en la JCE.
   * 
   * @param request petición con datos de consulta
   * @return Mono con la respuesta completa
   */
  @Cacheable(value = "consultas-jce", key = "#request.cedulaLimpia", unless = "#result.exitosa == false")
  public Mono<ConsultaResponse> consultarCiudadano(ConsultaRequest request) {
    String requestId = generateRequestId();

    logger.info("🔍 [{}] Iniciando consulta para cédula: {}",
        requestId, request.getCedulaFormateada());

    // Validar la petición antes de entrar en el flujo reactivo
    validateRequest(request);

    return executeConsultaWithTiming(request, requestId)
        .doOnSuccess(response -> logConsultaResult(response, requestId))
        .doOnError(error -> {
          consultasErrorCounter.increment();
          logger.error("❌ [{}] Error en consulta: {}", requestId, error.getMessage());
        });
  }

  /**
   * Consulta simplificada con solo cédula.
   * 
   * @param cedula número de cédula (con o sin guiones)
   * @return Mono con la respuesta
   */
  public Mono<ConsultaResponse> consultarCiudadano(String cedula) {
    return consultarCiudadano(new ConsultaRequest(cedula));
  }

  /**
   * Consulta con formato específico.
   * 
   * @param cedula  número de cédula
   * @param formato formato de respuesta deseado
   * @return Mono con la respuesta filtrada
   */
  public Mono<ConsultaResponse> consultarCiudadano(String cedula, String formato) {
    return consultarCiudadano(new ConsultaRequest(cedula, true, formato));
  }

  /**
   * Verifica el estado de salud del servicio JCE.
   * 
   * @return CompletableFuture con el estado de conectividad
   */
  public CompletableFuture<Boolean> verificarSaludServicio() {
    return jceHttpClient.verificarConectividad().toFuture();
  }

  // ========================================
  // MÉTODOS PRIVADOS - VALIDACIÓN
  // ========================================

  /**
   * Valida la petición de consulta.
   */
  private ConsultaRequest validateRequest(ConsultaRequest request) {
    logger.debug("🔍 Validando petición de consulta");

    // Validar cédula
    if (!request.esCedulaValida()) {
      cedulasInvalidasCounter.increment();
      throw ApiException.cedulaInvalida(request.getCedulaFormateada());
    }

    // Validar formato
    String formato = request.getFormato();
    if (formato != null && !FORMATOS_VALIDOS.contains(formato.toLowerCase())) {
      throw ApiException.formatoNoSoportado(formato);
    }

    logger.debug("✅ Petición validada correctamente");
    return request;
  }

  // ========================================
  // MÉTODOS PRIVADOS - EJECUCIÓN
  // ========================================

  /**
   * Ejecuta la consulta con medición de tiempo.
   */
  private Mono<ConsultaResponse> executeConsultaWithTiming(ConsultaRequest request, String requestId) {
    Timer.Sample sample = Timer.start(meterRegistry);

    return executeConsulta(request, requestId)
        .doOnTerminate(() -> sample.stop(consultaTimer));
  }

  /**
   * Ejecuta la consulta principal.
   */
  private Mono<ConsultaResponse> executeConsulta(ConsultaRequest request, String requestId) {
    logger.debug("🔄 [{}] Ejecutando consulta JCE", requestId);

    long startTime = System.currentTimeMillis();
    String cedulaLimpia = request.getCedulaLimpia();

    return jceHttpClient.consultarCiudadano(cedulaLimpia)
        .map(individuo -> processIndividuoResponse(individuo, request, startTime))
        .onErrorResume(error -> handleConsultaError(error, request, startTime));
  }

  /**
   * Procesa la respuesta del individuo desde JCE.
   */
  private ConsultaResponse processIndividuoResponse(Individuo individuo, ConsultaRequest request, long startTime) {
    long tiempoRespuesta = System.currentTimeMillis() - startTime;

    if (individuo == null || !individuo.esConsultaExitosa()) {
      ciudadanosNoEncontradosCounter.increment();
      return ConsultaResponse.error(
          "No se encontraron datos para la cédula consultada",
          "CIUDADANO_NO_ENCONTRADO",
          request.getCedulaFormateada(),
          tiempoRespuesta);
    }

    // Convertir a DTO con filtrado por formato
    DatosCiudadano datos = convertirADatosCiudadano(individuo, request.getFormato());
    InformacionFoto foto = request.getIncluirFoto() ? procesarInformacionFoto(individuo) : null;

    consultasExitosasCounter.increment();

    return ConsultaResponse.exitosa(
        request.getCedulaFormateada(),
        datos,
        foto,
        tiempoRespuesta);
  }

  /**
   * Maneja errores durante la consulta.
   */
  private Mono<ConsultaResponse> handleConsultaError(Throwable error, ConsultaRequest request, long startTime) {
    long tiempoRespuesta = System.currentTimeMillis() - startTime;
    consultasErrorCounter.increment();

    String mensaje;
    String codigo;

    if (error instanceof ApiException) {
      // Re-lanzar excepciones de dominio
      return Mono.error(error);
    }

    String errorMessage = error.getMessage();
    if (errorMessage != null && (errorMessage.contains("timeout") || errorMessage.contains("Timeout"))) {
      mensaje = "Timeout consultando el portal JCE - El servicio tardó demasiado en responder";
      codigo = "JCE_TIMEOUT";
    } else if (errorMessage != null && (errorMessage.contains("conexión") || errorMessage.contains("connect"))) {
      mensaje = "Error de conexión con el portal JCE - Servicio temporalmente no disponible";
      codigo = "JCE_NO_DISPONIBLE";
    } else {
      mensaje = "Error procesando la consulta en el portal JCE";
      codigo = "ERROR_PROCESAMIENTO";
    }

    return Mono.just(ConsultaResponse.error(
        mensaje,
        codigo,
        request.getCedulaFormateada(),
        tiempoRespuesta));
  }

  // ========================================
  // MÉTODOS PRIVADOS - TRANSFORMACIÓN
  // ========================================

  /**
   * Convierte Individuo a DatosCiudadano con filtrado por formato.
   */
  private DatosCiudadano convertirADatosCiudadano(Individuo individuo, String formato) {
    if (formato == null) {
      formato = "completo";
    }

    return switch (formato.toLowerCase()) {
      case "basico" -> crearDatosBasicos(individuo);
      case "personal" -> crearDatosPersonales(individuo);
      case "familiar" -> crearDatosFamiliares(individuo);
      default -> crearDatosCompletos(individuo); // "completo"
    };
  }

  /**
   * Crea datos básicos (solo información esencial).
   */
  private DatosCiudadano crearDatosBasicos(Individuo individuo) {
    return new DatosCiudadano(
        limpiarCampo(individuo.getNombres()),
        limpiarCampo(individuo.getApellido1()),
        limpiarCampo(individuo.getApellido2()),
        null, // nombreCompleto se calcula automáticamente
        limpiarCampo(individuo.getFechaNacimiento()),
        null, // lugar nacimiento
        null, // fecha expiración
        limpiarCampo(individuo.getSexo()),
        limpiarCampo(individuo.getEstadoCivilDescripcion()),
        null, // edad
        null, // código nacionalidad
        limpiarCampo(individuo.getDescripcionNacionalidad()),
        null, // municipio cédula
        null, // secuencia cédula
        null, // ocupación
        null, // cónyuge
        null, // cédula cónyuge
        null, // padre
        null, // madre
        null, // cédula vieja
        null, // pasaporte
        null, // categoría
        null, // descripción categoría
        limpiarCampo(individuo.getEstatusDescripcion()),
        null, // código causa
        null, // descripción causa inhabilidad
        null // descripción tipo causa
    );
  }

  /**
   * Crea datos personales (información personal sin familia).
   */
  private DatosCiudadano crearDatosPersonales(Individuo individuo) {
    return new DatosCiudadano(
        limpiarCampo(individuo.getNombres()),
        limpiarCampo(individuo.getApellido1()),
        limpiarCampo(individuo.getApellido2()),
        null, // nombreCompleto se calcula automáticamente
        limpiarCampo(individuo.getFechaNacimiento()),
        limpiarCampo(individuo.getLugarNacimiento()),
        limpiarCampo(individuo.getFechaExpiracion()),
        limpiarCampo(individuo.getSexo()),
        limpiarCampo(individuo.getEstadoCivilDescripcion()),
        limpiarCampo(individuo.getEdad()),
        limpiarCampo(individuo.getCodigoNacionalidad()),
        limpiarCampo(individuo.getDescripcionNacionalidad()),
        limpiarCampo(individuo.getMunicipioCedula()),
        limpiarCampo(individuo.getSecuenciaCedula()),
        limpiarCampo(individuo.getOcupacion()),
        null, // cónyuge
        null, // cédula cónyuge
        null, // padre
        null, // madre
        limpiarCampo(individuo.getCedulaVieja()),
        limpiarCampo(individuo.getPasaporte()),
        limpiarCampo(individuo.getCategoria()),
        limpiarCampo(individuo.getDescripcionCategoria()),
        limpiarCampo(individuo.getEstatusDescripcion()),
        limpiarCampo(individuo.getCodigoCausa()),
        limpiarCampo(individuo.getDescripcionCausaInhabilidad()),
        limpiarCampo(individuo.getDescripcionTipoCausa()));
  }

  /**
   * Crea datos familiares (información personal + familia).
   */
  private DatosCiudadano crearDatosFamiliares(Individuo individuo) {
    return new DatosCiudadano(
        limpiarCampo(individuo.getNombres()),
        limpiarCampo(individuo.getApellido1()),
        limpiarCampo(individuo.getApellido2()),
        null, // nombreCompleto se calcula automáticamente
        limpiarCampo(individuo.getFechaNacimiento()),
        limpiarCampo(individuo.getLugarNacimiento()),
        limpiarCampo(individuo.getFechaExpiracion()),
        limpiarCampo(individuo.getSexo()),
        limpiarCampo(individuo.getEstadoCivilDescripcion()),
        limpiarCampo(individuo.getEdad()),
        limpiarCampo(individuo.getCodigoNacionalidad()),
        limpiarCampo(individuo.getDescripcionNacionalidad()),
        limpiarCampo(individuo.getMunicipioCedula()),
        limpiarCampo(individuo.getSecuenciaCedula()),
        limpiarCampo(individuo.getOcupacion()),
        limpiarCampo(individuo.getConyugue()),
        limpiarCampo(individuo.getCedulaConyugue()),
        limpiarCampo(individuo.getPadre()),
        limpiarCampo(individuo.getMadre()),
        limpiarCampo(individuo.getCedulaVieja()),
        limpiarCampo(individuo.getPasaporte()),
        limpiarCampo(individuo.getCategoria()),
        limpiarCampo(individuo.getDescripcionCategoria()),
        limpiarCampo(individuo.getEstatusDescripcion()),
        limpiarCampo(individuo.getCodigoCausa()),
        limpiarCampo(individuo.getDescripcionCausaInhabilidad()),
        limpiarCampo(individuo.getDescripcionTipoCausa()));
  }

  /**
   * Crea datos completos (toda la información disponible).
   */
  private DatosCiudadano crearDatosCompletos(Individuo individuo) {
    return new DatosCiudadano(
        limpiarCampo(individuo.getNombres()),
        limpiarCampo(individuo.getApellido1()),
        limpiarCampo(individuo.getApellido2()),
        null, // nombreCompleto se calcula automáticamente
        limpiarCampo(individuo.getFechaNacimiento()),
        limpiarCampo(individuo.getLugarNacimiento()),
        limpiarCampo(individuo.getFechaExpiracion()),
        limpiarCampo(individuo.getSexo()),
        limpiarCampo(individuo.getEstadoCivilDescripcion()),
        limpiarCampo(individuo.getEdad()),
        limpiarCampo(individuo.getCodigoNacionalidad()),
        limpiarCampo(individuo.getDescripcionNacionalidad()),
        limpiarCampo(individuo.getMunicipioCedula()),
        limpiarCampo(individuo.getSecuenciaCedula()),
        limpiarCampo(individuo.getOcupacion()),
        limpiarCampo(individuo.getConyugue()),
        limpiarCampo(individuo.getCedulaConyugue()),
        limpiarCampo(individuo.getPadre()),
        limpiarCampo(individuo.getMadre()),
        limpiarCampo(individuo.getCedulaVieja()),
        limpiarCampo(individuo.getPasaporte()),
        limpiarCampo(individuo.getCategoria()),
        limpiarCampo(individuo.getDescripcionCategoria()),
        limpiarCampo(individuo.getEstatusDescripcion()),
        limpiarCampo(individuo.getCodigoCausa()),
        limpiarCampo(individuo.getDescripcionCausaInhabilidad()),
        limpiarCampo(individuo.getDescripcionTipoCausa()));
  }

  /**
   * Procesa información de foto de la cédula.
   */
  private InformacionFoto procesarInformacionFoto(Individuo individuo) {
    if (individuo != null && individuo.tieneFotoDisponible()) {
      String urlCompleta = baseUrlJce + individuo.getFotoUrl();
      return InformacionFoto.disponible(urlCompleta);
    } else {
      return InformacionFoto.noDisponible("Foto no disponible para esta cédula");
    }
  }

  // ========================================
  // MÉTODOS UTILITARIOS
  // ========================================

  /**
   * Limpia y valida un campo, retornando null si está vacío o es inválido.
   */
  private String limpiarCampo(String campo) {
    if (campo == null || campo.trim().isEmpty() || "null".equalsIgnoreCase(campo.trim())) {
      return null;
    }
    return campo.trim();
  }

  /**
   * Registra el resultado de una consulta.
   */
  private void logConsultaResult(ConsultaResponse response, String requestId) {
    if (response.exitosa()) {
      String nombreCompleto = response.datos() != null ? response.datos().nombreCompleto() : "N/A";
      logger.info("✅ [{}] Consulta exitosa en {}ms - Ciudadano encontrado: {}",
          requestId, response.tiempoRespuesta(), nombreCompleto);
    } else {
      logger.warn("⚠️ [{}] Consulta fallida en {}ms - {}: {}",
          requestId, response.tiempoRespuesta(), response.codigo(), response.mensaje());
    }
  }

  /**
   * Genera un ID único para rastrear peticiones.
   */
  private String generateRequestId() {
    return "SVC-" + System.currentTimeMillis() + "-" +
        Integer.toHexString(Thread.currentThread().hashCode());
  }
}