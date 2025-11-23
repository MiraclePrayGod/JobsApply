# 🚀 Guía de Configuración con ngrok

## Pasos para usar ngrok con tu backend

### 1. Levantar tu backend FastAPI

En una terminal, ve a la carpeta `backend` y ejecuta:

```bash
cd backend
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

Deberías ver algo como:
```
INFO:     Uvicorn running on http://0.0.0.0:8000 (Press CTRL+C to quit)
```

### 2. Crear túnel con ngrok

En otra terminal (nueva), ejecuta:

```bash
ngrok http 8000
```

Verás algo como:
```
Session Status                online
Account                       Tu Cuenta (Plan: Free)
Version                       3.x.x
Region                        United States (us)
Latency                       -
Web Interface                 http://127.0.0.1:4040
Forwarding                    https://abc123.ngrok-free.app -> http://localhost:8000

Connections                   ttl     opn     rt1     rt5     p50     p90
                              0       0       0.00    0.00    0.00    0.00
```

### 3. Copiar la URL de ngrok

Copia la URL que aparece en `Forwarding`:
- **URL HTTPS**: `https://abc123.ngrok-free.app` (esta es la que necesitas)

### 4. Actualizar NetworkConfig en Android

Abre el archivo:
```
appmovil/app/src/main/java/com/example/getjob/utils/NetworkConfig.kt
```

Reemplaza `TU_URL_NGROK.ngrok-free.app` con tu URL real de ngrok:

```kotlin
const val BASE_URL = "https://abc123.ngrok-free.app"  // Tu URL real
```

### 5. Recompilar y probar

1. Sincroniza el proyecto en Android Studio
2. Recompila la app
3. Ejecuta en tu dispositivo/emulador
4. Prueba el login/registro

## 🔍 Verificar que funciona

### Opción 1: Desde el navegador
Abre en tu navegador:
```
https://TU_URL_NGROK.ngrok-free.app/docs
```

Deberías ver la documentación de FastAPI (Swagger UI).

### Opción 2: Desde la app Android
1. Abre la app
2. Intenta hacer login o registro
3. Revisa los logs en Android Studio para ver las peticiones HTTP

## ⚠️ Notas importantes

1. **URL temporal**: Con el plan gratuito de ngrok, la URL cambia cada vez que reinicias ngrok
2. **Actualizar cada vez**: Si reinicias ngrok, debes actualizar `NetworkConfig.kt` con la nueva URL
3. **Solo desarrollo**: ngrok es para desarrollo/testing, no para producción
4. **Límite de conexiones**: El plan gratuito tiene límites de conexiones concurrentes

## 🎯 Alternativa: URL fija

Si quieres una URL que no cambie:
- **ngrok con cuenta de pago**: Puedes configurar un dominio fijo
- **Railway/Render**: Despliega tu backend y obtén una URL permanente

## 📱 Probar desde dispositivo físico

1. Asegúrate de que tu dispositivo esté conectado a internet (no necesariamente la misma WiFi)
2. Actualiza `NetworkConfig.kt` con la URL de ngrok
3. Compila e instala la app
4. Prueba las funcionalidades

¡Listo! Tu backend local ahora es accesible desde cualquier dispositivo.

