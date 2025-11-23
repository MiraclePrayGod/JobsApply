# 🎯 FLUJO PRINCIPAL DE LA APLICACIÓN - ServiFast

## 📋 **RESUMEN EJECUTIVO**

El flujo principal conecta **Clientes** que necesitan servicios con **Trabajadores** que los realizan, pasando por estados claramente definidos desde la creación del trabajo hasta el pago y calificación.

---

## 🔄 **FLUJO COMPLETO PASO A PASO**

### **FASE 1: INICIO Y REGISTRO** 🔐

```
┌─────────────────┐
│   LoginScreen   │
└────────┬────────┘
         │
         ├─→ Selecciona rol: "Soy Cliente" o "Soy Trabajador"
         │
         ├─→ [Si no tiene cuenta] → RegisterScreen
         │   ├─→ Registro Cliente → POST /api/auth/register
         │   └─→ Registro Trabajador → POST /api/auth/register
         │
         └─→ [Si tiene cuenta] → POST /api/auth/login
             └─→ Recibe JWT Token
             └─→ Guarda token en PreferencesManager
             └─→ Navega según rol:
                 ├─→ Cliente → ClientDashboard
                 └─→ Trabajador → Dashboard
```

**Pantallas:**
- `LoginScreen` - Login/Registro
- `RegisterClientScreen` - Registro de cliente
- `RegisterWorkerScreen` - Registro de trabajador (con perfil)

**Endpoints:**
- `POST /api/auth/login` - Login
- `POST /api/auth/register` - Registro

---

### **FASE 2: CLIENTE CREA TRABAJO** 📝

```
┌──────────────────┐
│ ClientDashboard  │
└────────┬─────────┘
         │
         └─→ [Botón "Crear Trabajo"] → CreateJobScreen
             │
             ├─→ Cliente llena formulario:
             │   - Título
             │   - Descripción
             │   - Tipo de servicio (Plomería, Electricidad, etc.)
             │   - Dirección
             │   - Método de pago (Yape o Efectivo)
             │   - Monto base
             │
             └─→ POST /api/jobs
                 └─→ Backend crea Job con estado: PENDING
                 └─→ Vuelve a ClientDashboard
                 └─→ Trabajo aparece en "Mis Trabajos"
```

**Pantallas:**
- `ClientDashboard` - Dashboard del cliente
- `CreateJobScreen` - Crear nuevo trabajo

**Endpoints:**
- `POST /api/jobs` - Crear trabajo
- `GET /api/jobs/my-jobs` - Ver mis trabajos (cliente)

**Estado del Job:** `PENDING` (sin trabajador asignado)

---

### **FASE 3: TRABAJADOR VE Y APLICA** 👷

```
┌─────────────┐
│  Dashboard   │ (Trabajador)
└──────┬──────┘
       │
       ├─→ GET /api/jobs/available
       │   └─→ Muestra trabajos con estado PENDING
       │   └─→ Trabajador puede ver:
       │       - Título
       │       - Descripción
       │       - Tipo de servicio
       │       - Dirección
       │       - Monto
       │
       └─→ [Trabajador selecciona trabajo] → SolicitudDetailScreen
           │
           └─→ [Botón "Aplicar"] → POST /api/jobs/{jobId}/apply
               └─→ Backend crea JobApplication
               └─→ Estado: is_accepted = false
               └─→ Trabajador puede ver su aplicación en:
                   └─→ WorkerRequestsScreen
```

**Pantallas:**
- `Dashboard` - Dashboard del trabajador (trabajos disponibles)
- `SolicitudDetailScreen` - Detalle del trabajo
- `WorkerRequestsScreen` - Mis aplicaciones

**Endpoints:**
- `GET /api/jobs/available` - Trabajos disponibles
- `POST /api/jobs/{jobId}/apply` - Aplicar a trabajo
- `GET /api/jobs/my-applications` - Ver mis aplicaciones

**Estado del Job:** Sigue `PENDING`, pero ahora tiene aplicaciones

---

### **FASE 4: CLIENTE ACEPTA TRABAJADOR** ✅

```
┌──────────────────┐
│ ClientDashboard  │
└────────┬─────────┘
         │
         └─→ [Cliente selecciona su trabajo] → SolicitudDetailScreen
             │
             ├─→ GET /api/jobs/{jobId}/applications
             │   └─→ Muestra lista de trabajadores que aplicaron
             │   └─→ Cliente ve:
             │       - Nombre del trabajador
             │       - Foto de perfil
             │       - Servicios que ofrece
             │       - Si está verificado
             │
             └─→ [Cliente acepta trabajador] → POST /api/jobs/{jobId}/accept-worker/{applicationId}
                 └─→ Backend actualiza:
                     ├─→ Job.worker_id = trabajador aceptado
                     ├─→ Job.status = ACCEPTED
                     ├─→ JobApplication.is_accepted = true
                     └─→ Crea Commission (10% del total)
                 └─→ Trabajador recibe notificación (en WorkerRequestsScreen)
```

**Pantallas:**
- `SolicitudDetailScreen` - Detalle con aplicaciones
- `ChatScreen` - Chat con trabajador (opcional en este punto)

**Endpoints:**
- `GET /api/jobs/{jobId}/applications` - Ver aplicaciones
- `POST /api/jobs/{jobId}/accept-worker/{applicationId}` - Aceptar trabajador

**Estado del Job:** `ACCEPTED` (trabajador asignado, esperando inicio)

---

### **FASE 5: TRABAJADOR INICIA SERVICIO** 🚗

```
┌─────────────────────┐
│ WorkerRequestsScreen│
└──────────┬──────────┘
           │
           └─→ [Trabajador ve trabajo aceptado] → SolicitudDetailScreen
               │
               └─→ [Botón "En Camino"] → OnRouteScreen
                   │
                   └─→ POST /api/jobs/{jobId}/start-route
                       └─→ Job.status = IN_ROUTE
                       └─→ Cliente ve actualización en tiempo real
                       │
                       └─→ [Trabajador llega] → OnSiteScreen
                           │
                           └─→ POST /api/jobs/{jobId}/confirm-arrival
                               └─→ Job.status = ON_SITE
                               │
                               └─→ [Botón "Iniciar Servicio"] → ServiceInProgressScreen
                                   │
                                   └─→ POST /api/jobs/{jobId}/start-service
                                       └─→ Job.status = IN_PROGRESS
                                       └─→ Job.started_at = ahora
```

**Pantallas:**
- `WorkerRequestsScreen` - Trabajos aceptados
- `OnRouteScreen` - Trabajador en camino
- `OnSiteScreen` - Trabajador llegó
- `ServiceInProgressScreen` - Servicio en progreso

**Endpoints:**
- `POST /api/jobs/{jobId}/start-route` - Iniciar ruta
- `POST /api/jobs/{jobId}/confirm-arrival` - Confirmar llegada
- `POST /api/jobs/{jobId}/start-service` - Iniciar servicio

**Estados del Job:**
- `IN_ROUTE` → Trabajador en camino
- `ON_SITE` → Trabajador llegó
- `IN_PROGRESS` → Servicio en progreso

---

### **FASE 6: TRABAJADOR COMPLETA SERVICIO** ✅

```
┌──────────────────────────┐
│ ServiceInProgressScreen  │
└────────────┬─────────────┘
             │
             ├─→ Trabajador realiza el servicio
             │
             ├─→ [Subir evidencias] (fotos antes/después)
             │   └─→ POST /api/jobs/{jobId}/evidence
             │
             ├─→ [Agregar notas] (descripción del trabajo)
             │   └─→ POST /api/jobs/{jobId}/notes
             │
             └─→ [Botón "Completar Servicio"] → POST /api/jobs/{jobId}/complete
                 └─→ Job.status = COMPLETED
                 └─→ Job.completed_at = ahora
                 └─→ Navega a → PaymentAndReviewScreen
```

**Pantallas:**
- `ServiceInProgressScreen` - Servicio en progreso
- `PaymentAndReviewScreen` - Pago y calificación

**Endpoints:**
- `POST /api/jobs/{jobId}/evidence` - Subir evidencias
- `POST /api/jobs/{jobId}/notes` - Agregar notas
- `POST /api/jobs/{jobId}/complete` - Completar trabajo

**Estado del Job:** `COMPLETED`

---

### **FASE 7: PAGO Y CALIFICACIÓN** 💰⭐

```
┌──────────────────────────┐
│ PaymentAndReviewScreen   │
└────────────┬─────────────┘
             │
             ├─→ Cliente ve:
             │   - Evidencias del trabajo
             │   - Notas del trabajador
             │   - Monto total (base + extras)
             │
             ├─→ [Cliente paga]
             │   ├─→ Si Yape: Comisión automática (10%)
             │   └─→ Si Efectivo: Comisión pendiente
             │
             ├─→ [Cliente califica] → POST /api/jobs/{jobId}/rate
             │   └─→ Rating.client_rating (1-5)
             │   └─→ Rating.client_comment
             │
             └─→ [Trabajador califica] → POST /api/jobs/{jobId}/rate
                 └─→ Rating.worker_rating (1-5)
                 └─→ Rating.worker_comment
                 └─→ Vuelve a Dashboard
```

**Pantallas:**
- `PaymentAndReviewScreen` - Pago y calificación

**Endpoints:**
- `POST /api/jobs/{jobId}/rate` - Calificar trabajo

**Estado del Job:** `COMPLETED` (con calificaciones)

---

### **FASE 8: COMISIONES** 💵

```
┌─────────────────────────┐
│ PendingCommissionsScreen│
└────────────┬────────────┘
             │
             └─→ GET /api/commissions/pending
                 └─→ Muestra comisiones pendientes (10% del total)
                 │
                 └─→ [Trabajador envía pago] → POST /api/commissions/{id}/submit-payment
                     ├─→ Sube código Yape
                     ├─→ Sube comprobante (screenshot)
                     └─→ Commission.status = PAYMENT_SUBMITTED
                         └─→ Manager revisa y aprueba/rechaza
```

**Pantallas:**
- `PendingCommissionsScreen` - Comisiones pendientes

**Endpoints:**
- `GET /api/commissions/pending` - Ver comisiones pendientes
- `POST /api/commissions/{id}/submit-payment` - Enviar pago de comisión

---

## 💬 **CHAT (Disponible en todo momento)**

El chat está disponible en diferentes momentos del flujo:

```
┌─────────────────────────────────────┐
│         Chat Disponible             │
├─────────────────────────────────────┤
│ 1. Durante aplicación (antes aceptar)│
│    - Chat de aplicación específica  │
│    - application_id presente         │
│                                      │
│ 2. Después de aceptar trabajador     │
│    - Chat general del trabajo       │
│    - application_id = null          │
│                                      │
│ 3. Durante servicio en progreso     │
│    - Coordinación en tiempo real     │
└─────────────────────────────────────┘
```

**Pantallas:**
- `ChatScreen` - Pantalla de chat

**Endpoints:**
- `GET /api/chat/{jobId}/messages?application_id={id}` - Obtener mensajes
- `POST /api/chat/{jobId}/send` - Enviar mensaje
- `WebSocket: ws://BASE_URL/api/chat/ws/{jobId}` - Chat en tiempo real

---

## 📊 **ESTADOS DEL TRABAJO (Job Status)**

```
PENDING
  ↓ (Trabajador aplica)
PENDING (con aplicaciones)
  ↓ (Cliente acepta trabajador)
ACCEPTED
  ↓ (Trabajador presiona "En camino")
IN_ROUTE
  ↓ (Trabajador presiona "Llegué")
ON_SITE
  ↓ (Trabajador presiona "Iniciar servicio")
IN_PROGRESS
  ↓ (Trabajador completa servicio)
COMPLETED
  ↓ (Cliente y trabajador califican)
COMPLETED (con ratings)
```

---

## 🔄 **FLUJO VISUAL COMPLETO**

```
CLIENTE                          TRABAJADOR
   │                                │
   ├─→ Login/Registro               ├─→ Login/Registro
   │                                │
   ├─→ Crear Trabajo                │
   │   (PENDING)                    │
   │                                │
   │                                ├─→ Ver Trabajos Disponibles
   │                                │
   │                                ├─→ Aplicar a Trabajo
   │                                │   (JobApplication creada)
   │                                │
   ├─→ Ver Aplicaciones             │
   │                                │
   ├─→ Aceptar Trabajador           │
   │   (ACCEPTED)                   │
   │                                │
   │                                ├─→ Ver Trabajo Aceptado
   │                                │
   │                                ├─→ En Camino (IN_ROUTE)
   │                                │
   │                                ├─→ Llegué (ON_SITE)
   │                                │
   │                                ├─→ Iniciar Servicio (IN_PROGRESS)
   │                                │
   │                                ├─→ Completar Servicio
   │                                │   (COMPLETED)
   │                                │
   ├─→ Ver Evidencias y Notas       │
   │                                │
   ├─→ Pagar                        │
   │                                │
   ├─→ Calificar                    ├─→ Calificar
   │                                │
   │                                ├─→ Ver Comisiones Pendientes
   │                                │
   │                                ├─→ Enviar Pago de Comisión
   └────────────────────────────────┴─────────────────────────
```

---

## ✅ **VERIFICACIÓN DEL FLUJO**

### **Checklist de Funcionalidades:**

- [x] Login/Registro de cliente
- [x] Login/Registro de trabajador
- [x] Cliente crea trabajo
- [x] Trabajador ve trabajos disponibles
- [x] Trabajador aplica a trabajo
- [x] Cliente ve aplicaciones
- [x] Cliente acepta trabajador
- [x] Trabajador inicia ruta
- [x] Trabajador confirma llegada
- [x] Trabajador inicia servicio
- [x] Trabajador completa servicio
- [x] Cliente paga
- [x] Ambos califican
- [x] Chat en tiempo real
- [x] Comisiones

---

## 🎯 **CONCLUSIÓN**

El flujo principal está **completamente implementado** y sigue una lógica clara:

1. **Cliente crea necesidad** → Trabajo PENDING
2. **Trabajadores aplican** → JobApplications creadas
3. **Cliente elige trabajador** → Trabajo ACCEPTED
4. **Trabajador ejecuta servicio** → Estados: IN_ROUTE → ON_SITE → IN_PROGRESS
5. **Trabajador completa** → Trabajo COMPLETED
6. **Pago y calificación** → Proceso finalizado
7. **Comisiones** → Trabajador paga comisión a la plataforma

**Todo el flujo está conectado y funcional.** ✅

