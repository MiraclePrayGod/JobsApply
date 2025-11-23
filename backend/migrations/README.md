# Migraciones de Base de Datos

Este directorio contiene scripts de migración SQL para actualizar el esquema de la base de datos.

## Migración: Agregar campos Plus a Workers (2024-11-21)

### Problema
El modelo `Worker` tiene campos nuevos (`is_plus_active`, `plus_expires_at`) y existe un nuevo modelo `WorkerSubscription` que no existen en la base de datos MySQL, causando errores 500 cuando SQLAlchemy intenta hacer SELECT/JOIN.

### Solución

#### Opción 1: Ejecutar SQL directamente en MySQL (Recomendado)

1. Conéctate a tu base de datos MySQL:
   ```bash
   mysql -u root -p getjob_db
   ```
   (O usa tu cliente MySQL favorito: MySQL Workbench, phpMyAdmin, etc.)

2. Ejecuta el archivo SQL:
   ```sql
   source backend/migrations/migration_2024_11_21_add_worker_plus_fields.sql
   ```
   
   O copia y pega el contenido del archivo directamente en tu cliente MySQL.

#### Opción 2: Desde la línea de comandos

```bash
mysql -u root -p getjob_db < backend/migrations/migration_2024_11_21_add_worker_plus_fields.sql
```

#### Opción 3: Usar el script Python (solo para crear tabla WorkerSubscription)

```bash
cd backend
python migrations/apply_migration.py
```

### Qué hace esta migración

1. **Agrega columnas a `workers`:**
   - `is_plus_active` (TINYINT(1), NOT NULL, DEFAULT 0)
   - `plus_expires_at` (DATETIME, NULL)

2. **Crea tabla `worker_subscriptions`:**
   - Tabla completa con todos los campos del modelo `WorkerSubscription`
   - Foreign key a `workers.id` con CASCADE
   - Índices para optimizar consultas

### Verificación

Después de aplicar la migración, verifica que todo esté correcto:

```sql
-- Verificar columnas en workers
DESCRIBE workers;

-- Verificar que la tabla worker_subscriptions existe
SHOW TABLES LIKE 'worker_subscriptions';

-- Ver estructura de worker_subscriptions
DESCRIBE worker_subscriptions;
```

### Nota sobre MySQL y "IF NOT EXISTS"

Algunas versiones de MySQL no soportan `IF NOT EXISTS` en `ALTER TABLE`. Si obtienes un error, ejecuta los comandos manualmente:

```sql
-- Si la columna ya existe, MySQL te dirá "Duplicate column name"
ALTER TABLE workers ADD COLUMN is_plus_active TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE workers ADD COLUMN plus_expires_at DATETIME NULL;
```

### Próximos pasos

Después de aplicar esta migración:

1. **Reinicia el servidor FastAPI** para que los cambios tomen efecto
2. **Prueba el endpoint** `/api/jobs/available` desde la app o Postman
3. Debería funcionar sin errores 500

### Siguiente: Alembic (Opcional)

Para futuras migraciones, considera usar Alembic para gestionar cambios de esquema de forma automática:

```bash
pip install alembic
alembic init migrations
# Configurar alembic.ini con tu database_url
alembic revision --autogenerate -m "Add worker plus fields"
alembic upgrade head
```

Esto te permitirá versionar y aplicar migraciones de forma más profesional.

