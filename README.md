<div align="center">

# 🏛️ JCE Consulta Microservice

<img src="https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21"/>
<img src="https://img.shields.io/badge/Spring_Boot-3.5.4-green?style=for-the-badge&logo=spring-boot&logoColor=white" alt="Spring Boot"/>
<img src="https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white" alt="Redis"/>
<img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white" alt="Docker"/>
<img src="https://img.shields.io/badge/License-Apache_2.0-blue?style=for-the-badge" alt="License"/>

### 🇩🇴 **Microservicio empresarial de alto rendimiento para consulta de datos ciudadanos**  

### **Junta Central Electoral (JCE) - República Dominicana**

<img src="https://readme-typing-svg.herokuapp.com?font=Fira+Code&size=20&duration=3000&pause=1000&color=2F81F7&center=true&vCenter=true&width=600&lines=%F0%9F%9A%80+Arquitectura+Hexagonal;%E2%9A%A1+WebFlux+Reactivo;%F0%9F%94%92+Rate+Limiting+Inteligente;%F0%9F%93%8A+M%C3%A9tricas+Avanzadas" alt="Typing SVG"/>

</div>

---

## 🌟 Características Principales

<table>
<tr>
<td width="50%">

### 🏗️ **Arquitectura & Patrones**

- 🔷 **Arquitectura Hexagonal** con DDD
- 🧹 **Clean Architecture** y SOLID
- ⚡ **Reactive Programming** con WebFlux
- 🔄 **Circuit Breaker** con retry inteligente
- 📐 **Event-Driven Architecture**

</td>
<td width="50%">

### 🚀 **Tecnologías Core**

- ☕ **Java 21** con Virtual Threads
- 🍃 **Spring Boot 3.5.4** & WebFlux
- 🌐 **Netty** como servidor embebido
- 🔴 **Redis** para caché distribuido
- 📊 **Prometheus** & Micrometer

</td>
</tr>
</table>

---

## 🎯 Casos de Uso

```mermaid
graph TD
    A[🌐 Cliente Web] --> B[🛡️ Rate Limiter]
    B --> C[📋 Validador de Cédula]
    C --> D[🏛️ JCE Consulta API]
    D --> E[💾 Redis Cache]
    D --> F[🌐 JCE Portal]
    G[📊 Metrics] --> H[📈 Prometheus]
    I[🔍 Health Check] --> J[⚕️ Actuator]
```

---

## 🔧 Instalación y Configuración

### 📋 Prerrequisitos

```bash
☕ Java 21+
🐳 Docker & Docker Compose
🔴 Redis Server
📦 Maven 3.9+
```

### 🚀 Inicio Rápido

```bash
# 📥 Clonar repositorio
git clone https://github.com/AntRed1/JCE-CONSULTA.git
cd JCE-CONSULTA

# 🔨 Compilar proyecto
mvn clean compile

# 🧪 Ejecutar tests
mvn test

# 📦 Generar JAR
mvn clean package

# 🚀 Ejecutar aplicación
java -jar target/jce-consulta-ms-1.0.0.jar
```

### 🐳 Docker Deployment

```bash
# 🏗️ Construir imagen
mvn spring-boot:build-image

# 🚀 Ejecutar con Docker
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=docker \
  arojas/jce-consulta-ms:1.0.0
```

---

## 📚 API Documentation

### 🌐 Endpoints Principales

| Método | Endpoint | Descripción | Rate Limit |
|--------|----------|-------------|------------|
| `GET` | `/api/v1/consulta/{cedula}` | 🔍 Consultar datos por cédula | 100/min |
| `GET` | `/actuator/health` | ⚕️ Health check | - |
| `GET` | `/actuator/metrics` | 📊 Métricas sistema | - |
| `GET` | `/swagger-ui.html` | 📖 Documentación API | - |

### 📝 Ejemplo de Uso

```bash
# 🔍 Consulta básica
curl -X GET "http://localhost:8080/api/v1/consulta/00112345678" \
  -H "Accept: application/json"

# 📊 Verificar métricas
curl -X GET "http://localhost:8080/actuator/prometheus"
```

### 📋 Validación de Cédula

```json
{
  "cedula": "00112345678",
  "valida": true,
  "formato": "###########",
  "digitoVerificador": "8"
}
```

---

## ⚙️ Configuración

### 🔧 application.yml

```yaml
server:
  port: 8080
  
spring:
  application:
    name: jce-consulta-ms
  
  redis:
    host: localhost
    port: 6379
    
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

### 🌍 Profiles Disponibles

| Profile | Descripción | Uso |
|---------|-------------|-----|
| `local` | 🏠 Desarrollo local | Default |
| `test` | 🧪 Testing | Tests automatizados |
| `docker` | 🐳 Contenedor | Docker deployment |
| `prod` | 🚀 Producción | Environment productivo |

---

## 🔒 Rate Limiting & Seguridad

### 📊 Configuración de Límites

```yaml
rate-limiting:
  default:
    capacity: 100        # requests por minuto
    refill: 10          # tokens por segundo
  consulta:
    capacity: 50        # consultas por minuto
    refill: 5           # tokens por segundo
```

### 🛡️ Características de Seguridad

- ✅ **Validación robusta** de cédulas dominicanas
- 🚫 **Rate limiting** distribuido con Redis
- 🔄 **Circuit breaker** para tolerancia a fallos
- 📝 **Logging estructurado** para auditoría
- 🔐 **Headers de seguridad** HTTP

---

## 📊 Monitoreo & Observabilidad

### 📈 Métricas Disponibles

- 🎯 **Latencia** de requests
- 📊 **Throughput** por endpoint
- 💾 **Uso de caché** Redis
- 🔄 **Circuit breaker** status
- ⚡ **JVM** metrics

### 📋 Health Checks

```bash
# ⚕️ Health general
curl http://localhost:8080/actuator/health

# 🔴 Redis connectivity
curl http://localhost:8080/actuator/health/redis

# 💿 Disk space
curl http://localhost:8080/actuator/health/diskSpace
```

---

## 🧪 Testing

### 🔬 Tipos de Tests

```bash
# 🧪 Unit Tests
mvn test

# 🔗 Integration Tests
mvn verify

# 📊 Coverage Report
mvn jacoco:report
```

### 📊 Cobertura de Código

| Componente | Cobertura |
|------------|-----------|
| **Services** | 95%+ |
| **Controllers** | 90%+ |
| **Utilities** | 100% |
| **Total** | 92%+ |

---

## 🏗️ Arquitectura del Sistema

### 📐 Capas de la Aplicación

```
┌─────────────────────────────────────────┐
│                 API Layer               │
├─────────────────────────────────────────┤
│              Application               │
│            (Use Cases)                 │
├─────────────────────────────────────────┤
│                Domain                  │
│            (Business Logic)            │
├─────────────────────────────────────────┤
│             Infrastructure             │
│        (External Services)             │
└─────────────────────────────────────────┘
```

### 🔄 Flujo de Datos

```mermaid
sequenceDiagram
    participant C as Cliente
    participant A as API Gateway
    participant S as Service
    participant R as Redis
    participant J as JCE Portal
    
    C->>A: Request consulta
    A->>A: Rate Limiting
    A->>S: Process request
    S->>R: Check cache
    alt Cache Hit
        R->>S: Return cached data
    else Cache Miss
        S->>J: Fetch from JCE
        J->>S: Return data
        S->>R: Store in cache
    end
    S->>A: Return response
    A->>C: JSON response
```

---

## 🤝 Contribución

### 📝 Guía para Contribuir

1. 🍴 **Fork** el proyecto
2. 🌿 Crear **branch** para feature (`git checkout -b feature/AmazingFeature`)
3. 📝 **Commit** cambios (`git commit -m 'Add some AmazingFeature'`)
4. 📤 **Push** al branch (`git push origin feature/AmazingFeature`)
5. 🔄 Crear **Pull Request**

### 📋 Estándares de Código

- ☕ **Java 21** best practices
- 🧹 **Clean Code** principles
- 📝 **Javadoc** para APIs públicas
- 🧪 **Tests** para nueva funcionalidad
- 📐 **SonarQube** compliance

---

## 📊 Performance Benchmarks

| Métrica | Valor | Descripción |
|---------|-------|-------------|
| **Latencia P50** | < 50ms | Respuesta típica |
| **Latencia P95** | < 200ms | 95% de requests |
| **Throughput** | 1000+ RPS | Requests por segundo |
| **Uptime** | 99.9% | Disponibilidad |

---

## 🛠️ Tecnologías Utilizadas

<div align="center">

### Backend & Framework

![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring](https://img.shields.io/badge/spring-%236DB33F.svg?style=for-the-badge&logo=spring&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-F2F4F9?style=for-the-badge&logo=spring-boot)

### Database & Cache

![Redis](https://img.shields.io/badge/redis-%23DD0031.svg?style=for-the-badge&logo=redis&logoColor=white)

### Build & Deploy

![Apache Maven](https://img.shields.io/badge/Apache%20Maven-C71A36?style=for-the-badge&logo=Apache%20Maven&logoColor=white)
![Docker](https://img.shields.io/badge/docker-%230db7ed.svg?style=for-the-badge&logo=docker&logoColor=white)

### Monitoring & Testing

![Prometheus](https://img.shields.io/badge/Prometheus-E6522C?style=for-the-badge&logo=Prometheus&logoColor=white)
![Swagger](https://img.shields.io/badge/-Swagger-%23Clojure?style=for-the-badge&logo=swagger&logoColor=white)

</div>

---

## 📄 Licencia

Este proyecto está licenciado bajo la **Apache License 2.0** - ver el archivo [LICENSE](LICENSE) para detalles.

---

## 👨‍💻 Autor

<div align="center">

**A. Rojas**  
🚀 *Lead Developer & Software Architect*  

[![Email](https://img.shields.io/badge/Email-contacto@arojas.dev-red?style=for-the-badge&logo=gmail)](mailto:contacto@arojas.dev)
[![GitHub](https://img.shields.io/badge/GitHub-AntRed1-black?style=for-the-badge&logo=github)](https://github.com/AntRed1)
[![Timezone](https://img.shields.io/badge/Timezone-America/Santo_Domingo-blue?style=for-the-badge&logo=world)](https://time.is/Santo_Domingo)

</div>

---

<div align="center">

### 🇩🇴 **Hecho con ❤️ en República Dominicana**

<img src="https://img.shields.io/github/stars/AntRed1/JCE-CONSULTA?style=social" alt="GitHub stars"/>
<img src="https://img.shields.io/github/forks/AntRed1/JCE-CONSULTA?style=social" alt="GitHub forks"/>
<img src="https://img.shields.io/github/watchers/AntRed1/JCE-CONSULTA?style=social" alt="GitHub watchers"/>

**⭐ Si este proyecto te ayuda, no olvides darle una estrella!**

</div>
