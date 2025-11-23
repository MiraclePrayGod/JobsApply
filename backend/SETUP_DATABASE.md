# 🗄️ Guía de Configuración de Base de Datos

Esta guía te ayudará a configurar tu base de datos MySQL local con Laragon para el proyecto GetJob.

## 📋 Requisitos Previos

1. ✅ Laragon instalado y ejecutándose
2. ✅ MySQL activo en Laragon
3. ✅ Python 3.8+ instalado
4. ✅ Dependencias instaladas (`pip install -r requirements.txt`)
5. ✅ Archivo `.env` configurado en `backend/`

## 🚀 Proceso Rápido (Recomendado)

### Paso 1: Crear la Base de Datos

1. Abre **HeidiSQL** o **phpMyAdmin** desde Laragon
2. Ejecuta este comando SQL:

```sql
CREATE DATABASE IF NOT EXISTS getjob_db 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;
```

O simplemente abre el archivo `migrations/create_database.sql` y ejecútalo.

### Paso 2: Ejecutar el Script Maestro

Desde la carpeta `backend/`, ejecuta:

```bash
python setup_database.py
```

Este script automáticamente:
- ✅ Verifica la conexión a la base de datos
- ✅ Crea todas las tablas necesarias
- ✅ Ejecuta todas las migraciones
- ✅ Te pregunta si quieres poblar con datos de prueba
- ✅ Muestra un resumen de lo creado

## 📝 Proceso Manual (Paso a Paso)

Si prefieres hacerlo manualmente:

### 1. Crear las Tablas Base

```bash
python init_db.py
```

Esto crea todas las tablas principales definidas en los modelos.

### 2. Ejecutar Migraciones

Ejecuta estas migraciones en orden (son idempotentes, puedes ejecutarlas varias veces):

```bash
# Agregar campos full_name y phone a users
python migrate_add_user_fields.py

# Agregar campos de verificación a workers
python migrate_add_worker_verification_fields.py

# Agregar sistema de aplicaciones de trabajadores
python migrate_add_job_applications.py
```

### 3. Poblar con Datos de Prueba (Opcional)

```bash
python seed_data.py
```

Esto crea:
- 3 clientes de prueba
- 3 trabajadores de prueba
- 1 manager
- Varios trabajos en diferentes estados
- Comisiones, ratings y mensajes de ejemplo

**Credenciales de prueba:**
- Cliente: `cliente1@test.com` / `password123`
- Trabajador: `trabajador1@test.com` / `password123`
- Manager: `manager@test.com` / `password123`

## 🔍 Verificar que Todo Funcionó

Puedes verificar en HeidiSQL o phpMyAdmin que se crearon estas tablas:

- `users`
- `workers`
- `jobs`
- `job_applications`
- `commissions`
- `job_evidence`
- `job_notes`
- `ratings`
- `messages`

## ⚠️ Solución de Problemas

### Error: "Can't connect to MySQL server"

- Verifica que Laragon esté ejecutándose
- Verifica que MySQL esté activo (botón verde en Laragon)
- Revisa que el archivo `.env` tenga la configuración correcta

### Error: "Unknown database 'getjob_db'"

- Asegúrate de haber creado la base de datos primero (Paso 1)
- Verifica que el nombre en `.env` coincida con el que creaste

### Error: "Access denied for user"

- Verifica el usuario y contraseña en `.env`
- Por defecto, Laragon usa `root` sin contraseña
- Si configuraste una contraseña, actualiza `MYSQL_PASSWORD` en `.env`

### Error al ejecutar migraciones

- Las migraciones son idempotentes, puedes ejecutarlas varias veces
- Si una migración dice "ya existe", es normal, puedes continuar

## 🎯 Siguiente Paso

Una vez configurada la base de datos, ejecuta el servidor:

```bash
uvicorn app.main:app --reload
```

El servidor estará disponible en: http://localhost:8000

Documentación API:
- Swagger UI: http://localhost:8000/docs
- ReDoc: http://localhost:8000/redoc

