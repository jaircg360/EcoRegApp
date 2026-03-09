# 📱 EcoRegApp — Gestión de Residuos ECOLIM S.A.C.

> Aplicación Android para el registro, seguimiento y reporte de residuos sólidos bajo la norma **NTP 900.058**.

---

## 🏢 Información del Proyecto

| Campo | Detalle |
|---|---|
| **Empresa** | ECOLIM S.A.C. |
| **Norma** | NTP 900.058 |
| **Plataforma** | Android (minSdk 26 / targetSdk 34) |
| **Arquitectura** | MVVM + Room Database + Navigation Component |
| **Lenguaje** | Java |

---

## 👤 Credenciales Demo

| ID | Contraseña | Rol |
|---|---|---|
| `OP-01` | `ecolim2026` | Operario |
| `OP-42` | `ecolim2026` | Operario |
| `OP-10` | `ecolim2026` | Operario |
| `ADMIN` | `ecolim2026` | Administrador |

---

## 🧱 Arquitectura

```
com.ecolim.ecoregapp/
├── data/
│   └── local/
│       ├── entity/       → Residuo.java
│       ├── dao/          → ResiduoDao.java
│       └── AppDatabase.java
├── ui/
│   ├── activities/       → SplashActivity, LoginActivity, MainActivity
│   ├── fragments/        → Home, Registro, Historial, Reportes, Importar, Perfil
│   ├── adapters/         → ResiduoAdapter.java
│   └── views/            → DonutChartView.java, BarChartView.java
├── viewmodel/            → ResiduoViewModel.java
└── utils/                → SessionManager, NotificationHelper, FileManager,
                            OperariosManager, BootReceiver, NotificationReceiver
```

---

## ✨ Funcionalidades

### 🔐 Login
- Fondo verde degradado (`#1B5E20` → `#43A047`)
- Card blanca con campos de ID y contraseña
- Validación por `OperariosManager` — soporta operarios y ADMIN
- Círculos decorativos y badge de credenciales demo

### 🏠 Inicio (Home)
- Saludo personalizado con nombre del operario o ID
- ADMIN ve: *"Buenas tardes, Administrador 👋"*
- Operario ve: *"Buenas tardes, Operario OP-01 👋"*

### 📝 Registro de Residuos
- Categorías: Plástico, Orgánico, Papel, Metal, Vidrio, Peligroso, Otros
- Selección de tipo con chips (tags corregidos para evitar NPE/crash)
- Peso, ubicación, zona, observaciones
- EPP con checkbox único para residuos peligrosos
- **Foto de evidencia** con cámara — URI guardada en `archivoOrigen` para visualizarla luego
- Fotos almacenadas en `getFilesDir()/evidencias/` (evita `SecurityException` en Android 12+)
- Notificación real al guardar (alerta prioritaria para peligrosos)
- Verificación de meta diaria (63 kg)

### 📋 Historial
- Lista de registros con emoji por tipo, peso, zona, fecha y operario
- **Sin flecha** — tarjetas limpias
- Toque abre dialog de detalle con **foto de evidencia** (si fue registrada)
- ADMIN puede editar y eliminar cualquier registro
- Operario solo puede editar/eliminar los propios

### 📊 Reportes
- **Header degradado verde** con título y tabs de período
- **Tabs visuales**: Hoy / Semana / Mes / Todo — tab activo en blanco con texto verde
- 3 tarjetas de estadísticas: peso total, registros, tipos
- **Gráfico de dona** con distribución por tipo (Canvas nativo, sin dependencias)
- **Gráfico de barras** con kg por tipo
- Leyenda al costado de la dona con % y kg por categoría
- Filtro por tipo de residuo con chips con emojis
- Exportar **PDF** (rojo degradado) y **CSV** (verde degradado) con compartir

### 📥 Importar Datos
- Importar desde **CSV**, **PDF** o **TXT**
- Formato CSV: `tipo,peso,fecha,ubicacion,zona,...`
- Formato TXT/PDF: `tipo,peso,fecha,ubicacion,zona` por línea
- Notificación al completar importación

### 👤 Perfil
- Foto de perfil — solo desde botón lápiz (sin click en el círculo)
- Cambio de nombre, contraseña
- Switch de notificaciones (bloquea todas excepto alertas peligrosas)
- Admin ve sección de gestión de operarios (crear / eliminar)
- Limpiar caché
- Iniciales generadas desde el nombre del usuario

---

## 🔔 Notificaciones

| Acción | Tipo |
|---|---|
| Nuevo registro normal | `enviar()` — respeta switch |
| Registro peligroso | `enviarAlerta()` — siempre llega |
| Exportar PDF / CSV | `enviar()` |
| Importar archivo | `enviar()` |
| Cambios en perfil | `enviar()` |
| Meta diaria alcanzada (63 kg) | `notificarMetaAlcanzada()` |
| Boot del dispositivo | Recrea canales via `BootReceiver` |

---

## 🎨 Diseño

### Colores principales
| Color | Hex | Uso |
|---|---|---|
| Primary | `#1B6F4A` | Botones, íconos |
| Primary Dark | `#0F4A30` | Header, status bar |
| Accent | `#00D97E` | Badges, sync |
| Danger | `#FF5A5A` | Peligroso |
| Warning | `#FFB547` | Orgánico, EPP |

### Drawables clave
| Archivo | Descripción |
|---|---|
| `bg_login_gradient.xml` | Degradado verde login |
| `bg_reportes_header.xml` | Degradado header reportes |
| `bg_tab_container.xml` | Cápsula tabs período |
| `bg_tab_active.xml` | Tab activo (blanco) |
| `bg_btn_exportar_pdf.xml` | Degradado rojo PDF |
| `bg_btn_exportar_csv.xml` | Degradado verde CSV |
| `bg_badge_demo.xml` | Badge glassmorphism login |
| `bg_circle_deco_top/bottom.xml` | Círculos decorativos login |

---

## 🛠️ Dependencias

```groovy
// UI
implementation 'com.google.android.material:material:1.11.0'
implementation 'androidx.navigation:navigation-fragment:2.7.6'
implementation 'de.hdodenhof:circleimageview:3.1.0'
implementation 'com.airbnb.android:lottie:6.3.0'

// Base de datos
implementation 'androidx.room:room-runtime:2.6.1'

// Red
implementation 'com.squareup.retrofit2:retrofit:2.9.0'

// Exportación
implementation 'com.itextpdf:itext7-core:7.2.5'
implementation 'com.opencsv:opencsv:5.8'

// Gráficos — Canvas nativo (sin dependencias externas)
// DonutChartView.java + BarChartView.java en ui/views/
```

---

## 📁 FileProvider

```xml
<!-- res/xml/file_paths.xml -->
<paths>
    <external-files-path name="exports" path="Documents/EcoRegApp/"/>
    <cache-path name="cache" path="."/>
    <files-path name="evidencias" path="evidencias/"/>
    <files-path name="perfil" path="perfil/"/>
</paths>
```

---

## 🔑 Permisos

```xml
INTERNET
READ/WRITE_EXTERNAL_STORAGE
READ_MEDIA_IMAGES, READ_MEDIA_DOCUMENTS
CAMERA
POST_NOTIFICATIONS
RECEIVE_BOOT_COMPLETED
SCHEDULE_EXACT_ALARM, USE_EXACT_ALARM
VIBRATE
```

---

## 📦 Datos de Prueba

El repositorio incluye 3 archivos con **100 registros cada uno** para probar la importación:

| Archivo | Formato | Uso |
|---|---|---|
| `residuos_ecolim.csv` | CSV con encabezado | Botón "Seleccionar CSV" |
| `residuos_ecolim.txt` | `tipo,peso,fecha,ubicacion,zona` | Botón "PDF / TXT" |
| `residuos_ecolim.pdf` | Tabla formateada | Botón "PDF / TXT" |

Fechas entre **octubre 2025 y marzo 2026**, 7 tipos de residuos distribuidos.

---

## 🐛 Bugs Corregidos

| Fix | Descripción |
|---|---|
| fix10 | Foto de perfil solo desde lápiz — eliminado overlay/click en círculo |
| fix11 | Notificaciones reales en todos los módulos |
| fix12 | AndroidManifest completo + SplashActivity como LAUNCHER |
| fix13 | `BootReceiver` y `NotificationReceiver` creados |
| fix14 | Cámara `SecurityException` — removido `FLAG_GRANT_WRITE_URI_PERMISSION` |
| fix15 | `LoginActivity` usa `OperariosManager` para validar todos los operarios |
| fix16 | `OperariosManager.validarLogin()` agregado |
| fix17 | Rol Operario vs Administrador en perfil e inicio corregido |
| fix18 | Foto de evidencia guardada en DB y visible en historial |
| fix19 | Chips de categoría sin `android:tag` → crash NPE corregido + gráficos |
| fix20 | `fragment_registro.xml` IDs sincronizados con el Java (9 errores de compilación) |

---

## 🚀 Cómo ejecutar

1. Clona el repositorio:
```bash
git clone https://github.com/AndriuCM25/EcoRegApp.git
```
2. Abre en **Android Studio Hedgehog** o superior
3. Sincroniza Gradle
4. Ejecuta en dispositivo físico o emulador (API 26+)
5. Ingresa con `OP-42` / `ecolim2026`

---

## 👨‍💻 Desarrollado para

**ECOLIM S.A.C.** — Gestión de Residuos Sólidos
Norma Técnica Peruana **NTP 900.058**

---

*EcoRegApp © 2026 — Todos los derechos reservados*
