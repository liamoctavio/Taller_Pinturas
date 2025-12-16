# 🎨 Taller de Pinturas - Sistema de Gestión Distribuido (Cloud Hybrid Architecture)

> Plataforma web moderna con arquitectura híbrida **AWS + Azure** para la gestión de eventos y galerías de arte. Enfocada en **seguridad**, **escalabilidad serverless** y **alto rendimiento**.

![Status](https://img.shields.io/badge/Status-Completado-success)
![Architecture](https://img.shields.io/badge/Architecture-Distributed%20Hybrid-blue)
![Java](https://img.shields.io/badge/Backend-Java%2017-orange)
![Angular](https://img.shields.io/badge/Frontend-Angular%2017-red)
![Azure](https://img.shields.io/badge/Cloud-Azure%20Serverless-0078D4)
![AWS](https://img.shields.io/badge/Cloud-AWS%20EC2-232F3E)

---

## 📖 Descripción del Proyecto

Este sistema es una solución integral para administrar un **Taller de Pinturas**, permitiendo la gestión de **Eventos** (talleres, clases) y una **Galería de Obras** multimedia.

El proyecto destaca por su **Arquitectura Distribuida Híbrida**, combinando la potencia de **AWS** para el orquestador (**BFF – Backend for Frontend**) y la flexibilidad **Serverless de Microsoft Azure** para la lógica de negocio, logrando un sistema **desacoplado**, **seguro** y **eficiente en costos**.

---

## 🏗️ Arquitectura del Sistema

El flujo de datos sigue un patrón estricto de seguridad y desacoplamiento, integrando múltiples nubes:

```mermaid
graph LR
  Client[Angular Client] -- HTTPS --> BFF[Spring Boot BFF (AWS EC2)]
  BFF -- HTTPS + Token --> AzureFunc[Azure Functions (Serverless)]
  AzureFunc -- JDBC --> DB[(Azure SQL Database)]
  AzureFunc -- Publish --> EventGrid[Azure Event Grid]

  style BFF fill:#FF9900,stroke:#232F3E,stroke-width:2px,color:white
  style AzureFunc fill:#0078D4,stroke:#333,stroke-width:2px,color:white
```

### 🧱 Stack Tecnológico

| Capa          | Tecnología                         | Plataforma                    |
| ------------- | ---------------------------------- | ----------------------------- |
| Frontend      | Angular 17 (Standalone Components) | Local / Web                   |
| BFF           | Spring Boot WebFlux (Java 17)      | AWS EC2 (Docker)              |
| Backend       | Azure Functions (Java 17)          | Azure Serverless              |
| Base de Datos | Azure SQL Database                 | Azure                         |
| Seguridad     | OAuth2 / OpenID Connect            | Microsoft Entra ID (Azure AD) |


---

## 🚀 Funcionalidades Clave

### 🔐 1. Seguridad Avanzada (Identity & Access)

* **Modelo Lectura Pública / Escritura Privada**

  * Visitantes pueden explorar galería y calendario sin autenticación (GET público).
  * Solo usuarios autenticados pueden crear, editar o eliminar contenido.

* **Protección Anti-IDOR**

  * Validación profunda en backend que asegura que un usuario solo pueda modificar recursos propios.
  * Verificación de `id_azure` vs `owner_id` en base de datos.

* **BFF Gateway**

  * Oculta la infraestructura interna.
  * Centraliza autenticación, manejo de tokens y CORS.

---

### ⚡ 2. Rendimiento y UX (Frontend)

* **Carga Diferida (Lazy Loading)**

  * Estrategia en dos fases: *Metadatos → Multimedia*.
  * First Contentful Paint < **300ms**.

* **Gestión de Imágenes**

  * Procesamiento y almacenamiento optimizado en **Base64**.
  * Transferencia asíncrona eficiente.

* **UI Reactiva**

  * Uso de `ChangeDetectorRef` para actualizaciones visuales inmediatas sin bloquear el hilo principal.

---

### 📡 3. Arquitectura Orientada a Eventos (EDA)

* **Telemetría en Tiempo Real** mediante **Azure Event Grid**.
* **Baja Latencia**: eventos asíncronos con entrega < **350ms**.
* **Escalabilidad Automática** gracias a Azure Functions (pago por uso).

---

## 🛠️ Instalación y Despliegue

### 📋 Prerrequisitos

* Java JDK 17
* Maven 3.8+
* Node.js + Angular CLI
* Azure CLI
* AWS CLI

---

### ☁️ 1. Despliegue Backend (Azure Functions)

El backend se despliega directamente a Azure usando Maven:

```bash
cd backend/tallerpinturas
mvn clean package azure-functions:deploy -DskipTests
```

> 💡 *Los tests se omiten para agilizar el despliegue en entorno de desarrollo.*

---

### 🔁 2. Ejecución del BFF (AWS / Local)

Ejecución local (puerto **8080**):

```bash
cd backend/bff-spring
# Configurar application.yml con las URLs de Azure Functions
mvn spring-boot:run
```

En producción, el BFF se empaqueta en **Docker** y se despliega en una instancia **EC2**.

---

### 🌐 3. Ejecución del Frontend (Angular)

```bash
cd frontend
npm install
ng serve
```

Acceso local: 👉 [http://localhost:4200](http://localhost:4200)

---

## 🧪 Calidad de Software

* **Unit Testing**

  * JUnit 5 + Mockito.
  * Tests de lógica de negocio y seguridad (`src/test`).

* **Performance**

  * Puntuación **>90** en Google Lighthouse.

* **Clean Code**

  * Separación clara: Controller / Service / Repository.
  * Uso de DTOs para transferencia de datos.

---

## ✒️ Autor

**Octavio Molina**
**Felipe Salgado**


* 💼 LinkedIn: *[agregar enlace]*
* 💻 GitHub: *[agregar enlace]*

---

⭐ Si este proyecto te resulta interesante, ¡no olvides dejar una estrella en el repositorio!
