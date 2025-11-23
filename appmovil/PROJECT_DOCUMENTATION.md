# 📱 ServiFast - Documentación Completa del Proyecto

## 📋 Índice
1. [Resumen del Proyecto](#resumen-del-proyecto)
2. [Stack Tecnológico](#stack-tecnológico)
3. [Arquitectura del Sistema](#arquitectura-del-sistema)
4. [Análisis de UIs](#análisis-de-uis)
5. [Base de Datos (MySQL)](#base-de-datos-mysql)
6. [API Endpoints](#api-endpoints)
7. [Flujo de Comisiones](#flujo-de-comisiones)
8. [Estructura del Proyecto](#estructura-del-proyecto)
9. [Configuración de Despliegue](#configuración-de-despliegue)
10. [Flujos de Trabajo](#flujos-de-trabajo)

---

## 🎯 Resumen del Proyecto

**ServiFast** es una aplicación móvil que conecta trabajadores de servicios (plomería, electricidad, limpieza, etc.) con clientes que necesitan estos servicios. La plataforma gestiona solicitudes, seguimiento en tiempo real, pagos y un sistema de comisiones del 10%.

### Características Principales
- **Roles**: Cliente y Trabajador
- **Métodos de Pago**: Yape y Efectivo
- **Comisión**: 10% sobre servicios pagados
- **Seguimiento en Tiempo Real**: Geolocalización y actualizaciones en vivo
- **Sistema de Calificaciones**: Clientes y trabajadores se califican mutuamente

---

## 🛠️ Stack Tecnológico

### Backend
- **Framework**: FastAPI (Python)
- **Base de Datos**: MySQL
- **ORM**: SQLAlchemy
- **Autenticación**: JWT (JSON Web Tokens)
- **Despliegue**: Railway.app

### Frontend (Android)
- **Lenguaje**: Kotlin
- **UI Framework**: Jetpack Compose
- **Arquitectura**: MVVM (Model-View-ViewModel)
- **Navegación**: Navigation Compose
- **HTTP Client**: Retrofit
- **Base de Datos Local**: Room (opcional, para cache)
- **Autenticación**: Google Sign-In

### Servicios Adicionales
- **Mapas**: Google Maps SDK
- **Notificaciones**: Firebase Cloud Messaging (opcional)
- **Almacenamiento de Fotos**: Firebase Storage (opcional) o S3

---

## 🏗️ Arquitectura del Sistema

### Backend (FastAPI)
```
Cliente Android (Kotlin)
    ↓ HTTP/HTTPS
FastAPI Backend
    ↓
MySQL Database
```

### Flujo de Datos
1. **Cliente Android** → Realiza peticiones HTTP/HTTPS
2. **FastAPI Backend** → Procesa lógica de negocio
3. **MySQL Database** → Almacena datos persistentes
4. **Respuesta** → JSON → Cliente Android

---

## 📱 Análisis de UIs

### UI 1: Pantalla de Inicio de Sesión
- **Propósito**: Autenticación y selección de rol
- **Funcionalidades**:
  - Login con email/password
  - Registro de cuenta
  - Autenticación con Google
  - Selección de rol (Cliente/Trabajador)
- **Nota**: No muestra bottom bar (mejor práctica para auth)

### UI 2: Pantalla de Registro de Trabajador
- **Propósito**: Onboarding completo de trabajadores
- **Funcionalidades**:
  - Información básica (nombre, teléfono, foto)
  - Selección de servicios (Plomería, Electricidad, etc.)
  - Ubicación y disponibilidad
  - Configuración de pagos (Yape)
- **Scrollable**: Pantalla larga con scroll vertical

### UI 3: Dashboard de Inicio del Trabajador
- **Propósito**: Pantalla principal post-autenticación
- **Funcionalidades**:
  - Estado del perfil
  - Disponibilidad (toggle)
  - Ganancias estimadas
  - Lista de solicitudes cercanas
  - Búsqueda de trabajos
- **Bottom Bar**: Inicio, Solicitudes, Perfil, Comisiones

### UI 4: Pantalla "Trabajo Aceptado"
- **Propósito**: Detalles de un trabajo aceptado
- **Funcionalidades**:
  - Resumen del trabajo
  - Información del cliente
  - Ubicación y mapa
  - Detalles del servicio
  - Botones: Cancelar, Iniciar servicio

### UI 5: Pantalla "En ruta al cliente"
- **Propósito**: Seguimiento en tiempo real hacia el cliente
- **Funcionalidades**:
  - Mapa en tiempo real
  - ETA dinámico
  - Barra de progreso
  - Botones: Pausar, Confirmar llegada

### UI 6: Pantalla "En sitio con el cliente"
- **Propósito**: Gestión del servicio al llegar
- **Funcionalidades**:
  - Confirmación de llegada
  - Checklist de verificación
  - Programación del trabajo
  - Botones: Mensajes, Reprogramar, Iniciar trabajo

### UI 7: Pantalla "Trabajo en curso"
- **Propósito**: Gestión del servicio activo
- **Funcionalidades**:
  - Cronómetro en tiempo real
  - Resumen de costos (agregar extras)
  - Subir fotos de evidencia
  - Notas para el cliente
  - Botones: Mensajes, Pausar, Finalizar servicio

### UI 8: Pantalla "Confirmar pago y reseña"
- **Propósito**: Finalización del servicio
- **Funcionalidades**:
  - Confirmar pago recibido (Yape/Efectivo)
  - Calificar cliente
  - Botones: Contactar, Finalizar y enviar

---

## 🗄️ Base de Datos (MySQL)

### Esquema de Tablas

#### 1. `users`
```sql
CREATE TABLE users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('client', 'worker', 'manager') NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

#### 2. `workers`
```sql
CREATE TABLE workers (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    services JSON,  -- ['Plomería', 'Electricidad', 'Limpieza', etc.]
    description TEXT,
    district VARCHAR(100),
    is_available BOOLEAN DEFAULT FALSE,
    yape_number VARCHAR(20),
    profile_image_url VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

#### 3. `jobs`
```sql
CREATE TABLE jobs (
    id INT PRIMARY KEY AUTO_INCREMENT,
    client_id INT NOT NULL,
    worker_id INT,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    service_type VARCHAR(50) NOT NULL,  -- 'Plomería', 'Electricidad', etc.
    status ENUM(
        'pending',
        'accepted',
        'in_route',
        'on_site',
        'in_progress',
        'completed',
        'cancelled'
    ) DEFAULT 'pending',
    payment_method ENUM('yape', 'cash') NOT NULL,
    base_fee DECIMAL(10, 2) NOT NULL,
    extras DECIMAL(10, 2) DEFAULT 0.00,
    total_amount DECIMAL(10, 2) NOT NULL,
    address VARCHAR(500) NOT NULL,
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    scheduled_at DATETIME,
    started_at DATETIME,
    completed_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (client_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (worker_id) REFERENCES workers(id) ON DELETE SET NULL
);
```

#### 4. `commissions`
```sql
CREATE TABLE commissions (
    id INT PRIMARY KEY AUTO_INCREMENT,
    worker_id INT NOT NULL,
    job_id INT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,  -- 10% del total_amount
    status ENUM(
        'pending',
        'payment_submitted',
        'approved',
        'rejected'
    ) DEFAULT 'pending',
    payment_code VARCHAR(50),  -- Código Yape que adjunta trabajador
    payment_proof_url VARCHAR(500),  -- Screenshot/comprobante
    submitted_at DATETIME,
    reviewed_by INT,  -- Manager que validó
    reviewed_at DATETIME,
    notes TEXT,  -- Notas del manager si rechaza
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (worker_id) REFERENCES workers(id) ON DELETE CASCADE,
    FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE,
    FOREIGN KEY (reviewed_by) REFERENCES users(id) ON DELETE SET NULL
);
```

#### 5. `job_evidence`
```sql
CREATE TABLE job_evidence (
    id INT PRIMARY KEY AUTO_INCREMENT,
    job_id INT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    type ENUM('before', 'after') NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE
);
```

#### 6. `job_notes`
```sql
CREATE TABLE job_notes (
    id INT PRIMARY KEY AUTO_INCREMENT,
    job_id INT NOT NULL,
    description TEXT NOT NULL,
    materials_used TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE
);
```

#### 7. `ratings`
```sql
CREATE TABLE ratings (
    id INT PRIMARY KEY AUTO_INCREMENT,
    job_id INT NOT NULL,
    worker_rating INT CHECK (worker_rating BETWEEN 1 AND 5),
    worker_comment TEXT,
    client_rating INT CHECK (client_rating BETWEEN 1 AND 5),
    client_comment TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE
);
```

---

## 🔌 API Endpoints

### Autenticación
```
POST   /api/auth/register          # Registro de usuario
POST   /api/auth/login             # Login con email/password
POST   /api/auth/google            # Login con Google
POST   /api/auth/refresh         # Refresh token
```

### Trabajadores
```
GET    /api/workers/me             # Obtener perfil del trabajador actual
PUT    /api/workers/me             # Actualizar perfil
POST   /api/workers/register       # Registro completo de trabajador
GET    /api/workers/{id}           # Obtener trabajador por ID
GET    /api/workers/search         # Buscar trabajadores
```

### Trabajos
```
GET    /api/jobs                   # Lista de trabajos disponibles
GET    /api/jobs/{id}              # Detalles de trabajo
POST   /api/jobs                   # Crear nuevo trabajo (cliente)
POST   /api/jobs/{id}/accept       # Aceptar trabajo (trabajador)
POST   /api/jobs/{id}/start-route  # Iniciar ruta al cliente
POST   /api/jobs/{id}/confirm-arrival  # Confirmar llegada
POST   /api/jobs/{id}/start-service    # Iniciar servicio
POST   /api/jobs/{id}/add-extra       # Agregar extra al precio
POST   /api/jobs/{id}/upload-evidence # Subir fotos de evidencia
PUT    /api/jobs/{id}/notes        # Actualizar notas
POST   /api/jobs/{id}/complete     # Finalizar servicio
POST   /api/jobs/{id}/cancel       # Cancelar trabajo
POST   /api/jobs/{id}/rate         # Calificar (cliente o trabajador)
```

### Comisiones
```
GET    /api/commissions/pending         # Comisiones pendientes del trabajador
GET    /api/commissions/history        # Historial de comisiones
POST   /api/commissions/{id}/submit-payment  # Adjuntar código Yape
GET    /api/commissions/stats          # Estadísticas de comisiones
```

### Manager (Validación de Pagos)
```
GET    /api/manager/commissions/pending-review  # Comisiones en revisión
POST   /api/manager/commissions/{id}/approve    # Aprobar pago
POST   /api/manager/commissions/{id}/reject     # Rechazar pago
```

### Ubicación (Tiempo Real)
```
POST   /api/location/update            # Actualizar ubicación del trabajador
GET    /api/jobs/{id}/location        # Obtener ubicación del trabajo
```

---

## 💰 Flujo de Comisiones

### Sistema de Comisiones

1. **Trabajo Completado**
   - Trabajador recibe pago del cliente (Yape o Efectivo)
   - Sistema registra comisión pendiente del 10%

2. **Depósito de Comisión (Opcional Inmediato)**
   - Trabajador puede depositar la comisión a cuenta Yape de la plataforma
   - Si no lo hace, no hay bloqueo inmediato

3. **Registro de Deuda**
   - Se registra como "comisión pendiente" en el perfil del trabajador
   - Se acumula con otras comisiones pendientes

4. **Bloqueo Condicional**
   - Al intentar usar la app, se verifica si hay comisiones pendientes
   - Si hay deudas, se muestra pantalla/modal solicitando el pago
   - Trabajador debe pagar las comisiones pendientes para continuar

5. **Proceso de Pago**
   - Trabajador adjunta código de pago Yape
   - Estado cambia a "payment_submitted"
   - Manager valida desde panel/admin
   - Si aprueba: Estado "approved" → Desbloquea acceso
   - Si rechaza: Estado "rejected" → Trabajador debe corregir

### Estados de Comisión
- `pending`: Comisión generada, no pagada
- `payment_submitted`: Trabajador adjuntó código Yape, esperando validación
- `approved`: Manager aprobó el pago, comisión pagada
- `rejected`: Manager rechazó el pago, trabajador debe corregir

---

## 📂 Estructura del Proyecto

### Backend (FastAPI)
```
backend/
├── app/
│   ├── __init__.py
│   ├── main.py                 # Entry point FastAPI
│   ├── config.py               # Configuración (DB, JWT, etc.)
│   ├── database.py             # Conexión MySQL
│   │
│   ├── models/                 # Modelos SQLAlchemy
│   │   ├── __init__.py
│   │   ├── user.py
│   │   ├── worker.py
│   │   ├── job.py
│   │   ├── commission.py
│   │   ├── payment.py
│   │   └── rating.py
│   │
│   ├── schemas/                # Pydantic schemas (validación)
│   │   ├── __init__.py
│   │   ├── user.py
│   │   ├── worker.py
│   │   ├── job.py
│   │   ├── commission.py
│   │   └── rating.py
│   │
│   ├── api/                    # Endpoints
│   │   ├── __init__.py
│   │   ├── routes/
│   │   │   ├── __init__.py
│   │   │   ├── auth.py         # Login, registro
│   │   │   ├── workers.py      # CRUD trabajadores
│   │   │   ├── jobs.py         # Trabajos, solicitudes
│   │   │   ├── commissions.py  # Comisiones pendientes
│   │   │   ├── payments.py     # Validación de pagos
│   │   │   └── manager.py      # Endpoints de manager
│   │   │
│   │   └── dependencies.py     # Dependencias (auth, DB)
│   │
│   ├── services/               # Lógica de negocio
│   │   ├── __init__.py
│   │   ├── auth_service.py
│   │   ├── job_service.py
│   │   ├── commission_service.py
│   │   └── payment_service.py
│   │
│   └── utils/                  # Utilidades
│       ├── __init__.py
│       ├── security.py         # JWT, hash passwords
│       └── validators.py
│
├── requirements.txt
├── .env.example
├── .gitignore
└── README.md
```

### Frontend (Android)
```
app/
├── data/
│   ├── api/                    # Retrofit interfaces
│   │   ├── AuthApi.kt
│   │   ├── JobApi.kt
│   │   ├── WorkerApi.kt
│   │   └── CommissionApi.kt
│   │
│   ├── models/                 # Data models
│   │   ├── User.kt
│   │   ├── Worker.kt
│   │   ├── Job.kt
│   │   └── Commission.kt
│   │
│   └── repository/             # Repositories
│       ├── AuthRepository.kt
│       ├── JobRepository.kt
│       └── CommissionRepository.kt
│
├── domain/
│   └── usecases/               # Lógica de negocio
│       ├── LoginUseCase.kt
│       ├── AcceptJobUseCase.kt
│       └── SubmitPaymentUseCase.kt
│
├── presentation/
│   ├── ui/
│   │   ├── screens/            # Pantallas Compose
│   │   │   ├── login/
│   │   │   ├── register/
│   │   │   ├── dashboard/
│   │   │   ├── job/
│   │   │   └── commission/
│   │   │
│   │   ├── components/         # Componentes reutilizables
│   │   │   ├── JobCard.kt
│   │   │   ├── PaymentCard.kt
│   │   │   └── CommissionCard.kt
│   │   │
│   │   └── theme/              # Tema
│   │       ├── Color.kt
│   │       ├── Theme.kt
│   │       └── Type.kt
│   │
│   └── viewmodel/              # ViewModels
│       ├── LoginViewModel.kt
│       ├── DashboardViewModel.kt
│       └── JobViewModel.kt
│
└── utils/
    ├── network/
    │   └── NetworkConfig.kt
    └── constants/
        └── Constants.kt
```

---

## 🚀 Configuración de Despliegue

### Railway (Recomendado)

#### 1. Instalar Railway CLI
```bash
# Windows (PowerShell)
iwr https://railway.app/install.sh | iex

# Mac/Linux
curl -fsSL https://railway.app/install.sh | sh
```

#### 2. Login
```bash
railway login
```

#### 3. Crear Proyecto
```bash
cd backend
railway init
```

#### 4. Agregar MySQL
```bash
railway add mysql
```

#### 5. Variables de Entorno
Railway proporciona automáticamente:
- `MYSQL_HOST`
- `MYSQL_USER`
- `MYSQL_PASSWORD`
- `MYSQL_DATABASE`
- `MYSQL_PORT`

#### 6. Desplegar
```bash
railway up
```

#### 7. Obtener URL
```bash
railway domain
# Resultado: tu-app.railway.app
```

### Configuración en Backend

#### `config.py`
```python
import os

# Database
DATABASE_URL = os.getenv(
    "DATABASE_URL",
    f"mysql+pymysql://{os.getenv('MYSQL_USER')}:{os.getenv('MYSQL_PASSWORD')}@{os.getenv('MYSQL_HOST')}:{os.getenv('MYSQL_PORT')}/{os.getenv('MYSQL_DATABASE')}"
)

# JWT
SECRET_KEY = os.getenv("SECRET_KEY", "tu-secret-key-super-segura")
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = 30
```

### Configuración en Android

#### `NetworkConfig.kt`
```kotlin
object NetworkConfig {
    const val BASE_URL = if (BuildConfig.DEBUG) {
        "http://192.168.1.100:8000"  // Tu IP local para desarrollo
    } else {
        "https://tu-app.railway.app"  // Producción
    }
}
```

#### `AndroidManifest.xml`
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<application
    android:usesCleartextTraffic="true"  <!-- Solo para desarrollo -->
    ...>
```

---

## 🔄 Flujos de Trabajo

### Flujo Completo de un Trabajo

```
1. Cliente crea solicitud de trabajo
   ↓
2. Trabajador ve solicitud en Dashboard (UI 3)
   ↓
3. Trabajador acepta trabajo (UI 4)
   ↓
4. Trabajador inicia ruta al cliente (UI 5)
   ↓
5. Trabajador confirma llegada (UI 6)
   ↓
6. Trabajador inicia servicio (UI 7)
   ↓
7. Trabajador finaliza servicio (UI 8)
   ↓
8. Sistema registra comisión pendiente (10%)
   ↓
9. Trabajador puede usar la app normalmente
   ↓
10. Próxima vez que abre la app:
    - Si hay comisiones pendientes → Pantalla de bloqueo
    - Si no hay → Acceso normal
   ↓
11. Trabajador adjunta código Yape
   ↓
12. Manager valida pago
   ↓
13. Si aprueba → Desbloquea acceso
```

### Flujo de Autenticación

```
1. Usuario abre app
   ↓
2. Pantalla de Login (UI 1)
   ↓
3a. Login con Email/Password
   ↓
3b. Login con Google
   ↓
4. Backend valida credenciales
   ↓
5. Backend retorna JWT token
   ↓
6. App guarda token
   ↓
7. Usuario accede a Dashboard
```

---

## 📝 Notas Importantes

### Validación de Pagos
- El trabajador adjunta **código de pago Yape**
- Manager valida manualmente desde panel/admin
- Si aprueba: Estado "approved" → Desbloquea acceso
- Si rechaza: Estado "rejected" → Trabajador debe corregir

### Bottom Navigation Bar
- **NO se muestra** en pantallas de autenticación (Login/Registro)
- **SÍ se muestra** después de autenticarse en pantallas principales
- 4 elementos: Inicio, Solicitudes, Perfil, Comisiones

### Validación en Tiempo Real
- Todos los campos de formulario validan mientras el usuario escribe
- Feedback visual inmediato

### Geolocalización
- Permisos de ubicación en primer plano necesarios
- Actualización en tiempo real del trabajador
- Compartir ubicación con el cliente

---

## 🎨 Paleta de Colores

- **Primario**: Naranja (#FF...) - Acciones principales, botones CTA
- **Secundario**: Azul brillante - Información destacada, estado activo
- **Fondo**: Blanco - Tarjetas y contenido principal
- **Fondo Secundario**: Gris claro - Fondo de pantalla
- **Texto Principal**: Negro - Títulos y texto importante
- **Texto Secundario**: Gris - Descripciones e información secundaria
- **Estado Éxito**: Verde - Mensajes de éxito/confirmación
- **Estado Calificación**: Amarillo - Estrellas de calificación

---

## 📚 Próximos Pasos

1. ✅ **Análisis de UIs** - COMPLETADO
2. ✅ **Definición de Arquitectura** - COMPLETADO
3. ⏳ **Implementación del Backend** - EN PROGRESO
4. ⏳ **Configuración de Base de Datos**
5. ⏳ **Implementación de API Endpoints**
6. ⏳ **Implementación de Android App**
7. ⏳ **Integración de Google Sign-In**
8. ⏳ **Despliegue en Railway**

---

## 📞 Contacto y Soporte

Para preguntas o dudas sobre la implementación, consultar esta documentación o el chat del proyecto.

---

**Última actualización**: 2024
**Versión**: 1.0.0
**Estado**: Desarrollo

