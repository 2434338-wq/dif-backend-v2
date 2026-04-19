# DIF Metepec — Backend Ktor

Backend compartido para **App Web** y **App Móvil Android**. Una sola base de datos SQLite para ambas apps.

## Requisitos
- JDK 17+
- Gradle 8+

## Ejecutar

```bash
./gradlew run
```

El servidor corre en `http://0.0.0.0:8080`

## Endpoints principales

### Públicos
| Método | Ruta | Descripción |
|--------|------|-------------|
| POST | `/register` | Registro de usuario (JSON o form) |
| POST | `/login` | Login (JSON o form) → devuelve JWT |
| GET | `/health` | Health check |

### Autenticados (requiere `Authorization: Bearer <token>`)
| Método | Ruta | Descripción |
|--------|------|-------------|
| POST | `/donaciones` | Crear donación (auto-rellena datos del usuario) |
| GET | `/mis-donaciones` | Historial de donaciones del usuario |
| POST | `/solicitudes` | Crear solicitud (multipart con imagen opcional) |
| GET | `/mis-solicitudes` | Historial de solicitudes del usuario |
| GET | `/notificaciones` | Listar notificaciones del usuario |
| POST | `/notificaciones/{id}/leer` | Marcar notificación como leída |
| POST | `/notificaciones/leer-todas` | Marcar todas como leídas |
| GET | `/perfil` | Datos del usuario autenticado |

### Admin (App Web)
| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/admin/donaciones` | Todas las donaciones |
| POST | `/admin/donaciones/{id}/estado` | Cambiar estado |
| GET | `/admin/solicitudes` | Todas las solicitudes |
| POST | `/admin/solicitudes/{id}/estado` | Cambiar estatus |
| GET | `/admin/inventario` | Inventario completo |
| POST | `/admin/inventario` | Agregar artículo |
| POST | `/admin/inventario/{id}/ajuste` | Ajustar stock |
| GET | `/admin/entregas` | Todas las entregas |
| POST | `/admin/entregas` | Registrar entrega |
| GET | `/admin/usuarios` | Lista de usuarios |

## Configuración IP
Edita `src/main/resources/application.conf` para cambiar el puerto.
En la app web (`app.js`) y en la app Android (`NetworkModule.kt`), cambia la IP al valor de tu máquina.

## Base de datos
Se crea automáticamente como `dif_database.db` en la raíz del proyecto.
**Admin por defecto:** `admin@dif.gob.mx` / `admin123`
