# 🌿 EcoRegApp

> **Sistema de Gestión de Residuos Industriales**  
> Desarrollado para **ECOLIM S.A.C.** | Android nativo en Java | Versión 1.1 | Marzo 2026

---

## 📋 Descripción General

EcoRegApp es una aplicación Android nativa que permite a los operarios industriales **registrar, gestionar, analizar y exportar datos de residuos** de manera eficiente y segura. Diseñada para funcionar en entornos industriales con conectividad limitada, almacena los datos localmente y sincroniza cuando hay conexión disponible.

| Característica | Detalle |
|---|---|
| **Plataforma** | Android (Java) |
| **Arquitectura** | MVVM (Model-View-ViewModel) |
| **Base de datos** | Room (SQLite local) |
| **Navegación** | Navigation Component + BottomNavigationView |
| **Min SDK** | API 24 (Android 7.0 Nougat) |
| **Target SDK** | API 34 (Android 14) |

---

## 🔐 Credenciales de Acceso Demo

| Usuario / ID | Contraseña | Rol | Turno | Planta |
|---|---|---|---|---|
| `OP-01` | `ecolim2026` | Operario | Mañana | Planta A |
| `OP-42` | `ecolim2026` | Operario | Tarde | Planta A |
| `OP-10` | `ecolim2026` | Operario | Noche | Planta B |
| `ADMIN` | `ecolim2026` | Administrador | Mañana | Planta C |

---

## 📁 Estructura del Proyecto

```
app/src/main/java/com/ecolim/ecoregapp/
├── ui/
│   ├── activities/         # LoginActivity, MainActivity
│   ├── fragments/          # HomeFragment, RegistroFragment, HistorialFragment,
│   │                       # ReportesFragment, ImportarFragment, ProfileFragment,
│   │                       # SuccessFragment
│   └── adapters/           # ResiduoAdapter
├── data/
│   └── local/
│       ├── entity/         # Residuo.java
│       ├── dao/            # ResiduoDao.java
│       └── database/       # AppDatabase.java
├── utils/                  # SessionManager.java, FileManager.java, SafeNav.java
└── viewmodel/              # ResiduoViewModel.java

app/src/main/res/
├── layout/                 # 10+ XMLs de pantallas y diálogos
├── drawable/               # 30+ XMLs de fondos, íconos, badges
├── navigation/             # nav_graph.xml
├── menu/                   # bottom_nav_menu.xml
├── xml/                    # file_provider_paths.xml
└── values/                 # colors.xml, strings.xml, themes.xml
```

---

## 📱 Pantallas y Módulos

### 🏠 Home (HomeFragment)
- Saludo dinámico según hora del día
- Nombre del operario sincronizado desde Perfil via `onResume`
- Card con peso total del día y barra de progreso (meta: 63 kg)
- Stats: Registros hoy, Por Sync, Peligrosos
- Accesos rápidos: Nuevo Registro, Importar, Reportes, Historial
- Lista de últimos 5 registros con RecyclerView
- Cálculo de estadísticas en el hilo principal con `isAdded()` como guarda

### ➕ Nuevo Registro (RegistroFragment)
- Selección de tipo via ChipGroup: Plástico, Orgánico, Papel, Metal, Peligroso, Vidrio, Pendiente
- Campos: Peso (kg), Ubicación, Zona, Observaciones
- Validación EPP obligatoria para residuos peligrosos (guantes, mascarilla, lentes)
- Cámara para foto evidencia con FileProvider (`getCacheDir`)
- Procesamiento de imagen en hilo secundario
- Navegación protegida con `SafeNav` para evitar crashes por doble click
- Botones: **Guardar** (navega a éxito) y **Guardar + Nuevo** (limpia formulario)

### 📋 Historial (HistorialFragment)
- Lista completa de registros ordenados por fecha descendente
- RecyclerView con ResiduoAdapter
- Chips de filtro por tipo de residuo

### 📊 Reportes (ReportesFragment)
- Filtros: Hoy, 7 días, Este mes, Todo
- Calendario con `DatePickerDialog` para fecha específica
- **Gráfico de barras** (MPAndroidChart): kg por día
- **Gráfico de dona** (MPAndroidChart): distribución por tipo con leyenda de colores
- Chips de periodo con fondo blanco y texto verde para máxima legibilidad
- Exportar PDF e Exportar CSV con datos filtrados

### 📥 Importar (ImportarFragment)
- Importación de archivos CSV, PDF y TXT compatibles
- Inserción masiva en Room

### 👤 Perfil (ProfileFragment)
- Foto de perfil: cámara o galería, guardada en `filesDir`
- Procesamiento con muestreo (max 512px) para no saturar memoria
- Cambio de nombre, teléfono y email
- Cambio de contraseña con verificación
- Switch: Notificaciones, Sync solo con WiFi (**Modo oscuro eliminado** — no implementado)
- Cerrar sesión con confirmación
- Padding inferior de 80dp para que "Cerrar sesión" no quede tapado

---

## 🗄️ Modelo de Datos (Room)

```java
@Entity(tableName = "residuos")
public class Residuo {
    @PrimaryKey(autoGenerate = true) public int id;
    public String tipo;           // plastico, organico, papel, metal, peligroso, vidrio, pendiente
    public double pesoKg;
    public String fecha;          // yyyy-MM-dd
    public String planta;         // Planta A / B / C
    public String zona;           // Zona A-E
    public String operarioId;     // OP-01, OP-42, etc.
    public String operarioNombre;
    public String turno;          // Mañana / Tarde / Noche
    public String observaciones;
    public boolean eppConfirmado;
    public boolean sincronizado;
}
```

---

## 🏭 Categorías de Residuos

| Categoría | Peso típico | EPP requerido | Color en gráficos |
|---|---|---|---|
| Plástico | 0.5 – 15 kg | No | `#00D97E` 🟢 |
| Orgánico | 2 – 50 kg | No | `#4CAF50` 🟢 |
| Papel | 1 – 20 kg | No | `#2196F3` 🔵 |
| Metal | 0.5 – 30 kg | No | `#9E9E9E` ⚫ |
| Vidrio | 0.5 – 10 kg | No | `#00BCD4` 🔵 |
| Peligroso | 0.1 – 5 kg | ✅ Guantes + Mascarilla + Lentes | `#FF5722` 🔴 |
| Pendiente | 0.5 – 10 kg | No | Según clasificación final |

---

## 📦 Dependencias (build.gradle)

```gradle
// UI & Layout
implementation 'androidx.appcompat:appcompat:1.6.1'
implementation 'com.google.android.material:material:1.11.0'
implementation 'androidx.recyclerview:recyclerview:1.3.2'
implementation 'androidx.constraintlayout:constraintlayout:2.1.4'

// Navigation
implementation 'androidx.navigation:navigation-fragment:2.7.6'
implementation 'androidx.navigation:navigation-ui:2.7.6'

// Room (Base de datos local)
implementation 'androidx.room:room-runtime:2.6.1'
annotationProcessor 'androidx.room:room-compiler:2.6.1'

// Lifecycle - MVVM
implementation 'androidx.lifecycle:lifecycle-viewmodel:2.7.0'
implementation 'androidx.lifecycle:lifecycle-livedata:2.7.0'

// WorkManager
implementation 'androidx.work:work-runtime:2.9.0'

// Network
implementation 'com.squareup.retrofit2:retrofit:2.9.0'

// PDF & CSV
implementation 'com.itextpdf:itext7-core:7.2.5'
implementation 'com.opencsv:opencsv:5.8'

// Gráficos
implementation 'com.github.PhilJay:MPAndroidChart:v3.1.0'

// Imágenes
implementation 'de.hdodenhof:circleimageview:3.1.0'

// Animaciones
implementation 'com.airbnb.android:lottie:6.3.0'
```

> ⚠️ **Agregar en `settings.gradle`** el repositorio de JitPack:
> ```gradle
> maven { url 'https://jitpack.io' }
> ```

> ⚠️ **Compatibilidad Java** — cambiar en `build.gradle (app)`:
> ```gradle
> compileOptions {
>     sourceCompatibility JavaVersion.VERSION_11
>     targetCompatibility JavaVersion.VERSION_11
> }
> ```

---

## 🔧 Configuración AndroidManifest

### Permisos requeridos
```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```

### FileProvider (obligatorio para cámara)
```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="com.ecolim.ecoregapp.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_provider_paths"/>
</provider>
```

### res/xml/file_provider_paths.xml
```xml
<paths>
    <external-files-path name="my_images" path="Pictures/"/>
    <cache-path name="cache_images" path="."/>
    <files-path name="internal_files" path="."/>
</paths>
```

---

## 🧭 Navegación

| Fragment ID | Clase | Acceso |
|---|---|---|
| `homeFragment` | HomeFragment | Tab Inicio |
| `historialFragment` | HistorialFragment | Tab Historial |
| `importarFragment` | ImportarFragment | Tab Importar |
| `reportesFragment` | ReportesFragment | Tab Reportes |
| `profileFragment` | ProfileFragment | Tab Perfil |
| `registroFragment` | RegistroFragment | Card "Nuevo Registro" en Home |
| `successFragment` | SuccessFragment | Después de guardar registro |

> ℹ️ El FloatingActionButton fue eliminado. El acceso a Nuevo Registro se realiza únicamente desde la card en Home.

---

## 🛡️ SafeNav — Navegación Segura

`SafeNav.java` en `utils/` previene crashes por doble click o navegación rápida:

```java
// En lugar de:
Navigation.findNavController(v).navigate(R.id.destino);

// Usar:
SafeNav.go(Navigation.findNavController(v), R.id.destino);

// O directamente en setOnClickListener:
view.setOnClickListener(SafeNav.to(nav, R.id.destino));
```

Bloquea cualquier navegación que ocurra dentro de los **600ms** de la anterior. Si el destino ya no existe, atrapa la excepción silenciosamente.

---

## 📷 Implementación de Cámara

### Foto de Perfil
1. Permiso solicitado en runtime con `ActivityResultContracts.RequestPermission`
2. Archivo temporal en `getCacheDir()` — evita errores de permisos en Android 10+
3. URI compartida via FileProvider con `FLAG_GRANT_WRITE_URI_PERMISSION`
4. Decodificación con `BitmapFactory.Options.inSampleSize` para reducir memoria
5. Redimensionado automático a **máx 512px**
6. Guardado final en `getFilesDir()/profile_photo.jpg`
7. Ruta persistida en `SharedPreferences` con clave `foto_path_interna`

### Foto Evidencia en Registro
- Mismo flujo que Perfil, redimensionado a máx 800px
- Procesamiento en `Thread` secundario para no bloquear la UI
- Referencia guardada en campo `observaciones`

---

## 🔄 Sincronización Perfil → Home

Los cambios de nombre en Perfil se reflejan automáticamente en Home:

1. `ProfileFragment` guarda el nombre en `SharedPreferences` (`profile_prefs`, clave `nombre_usuario`)
2. `HomeFragment` implementa `onResume()`
3. Cada vez que se navega de vuelta a Home, `onResume` lee las preferencias y actualiza el nombre

---

## 🐛 Bugs Resueltos

| Problema | Causa | Solución |
|---|---|---|
| `android:imageTintList` not found | Incompatible con minSdk 24 | Eliminar atributo del XML |
| `ClassNotFoundException` ProfileFragment | Archivo Java no copiado | Copiar a `ui/fragments/` |
| `onBackPressed` crash en Fragment | `onClick` en XML no funciona en Fragments | Mover a Java: `btn_back.setOnClickListener` |
| Cámara crashea la app | `getExternalFilesDir` requiere permisos extra en Android 10+ | Cambiar a `getCacheDir()` |
| Foto no aparece después de tomarla | Bitmap en hilo principal, URI externa | Thread secundario + guardar copia en `filesDir` |
| FAB tapa contenido | `marginBottom` insuficiente | FAB eliminado completamente |
| Perfil crashea al abrir | `switch_modo_oscuro` en XML pero no en Java | Eliminar de ambos archivos |
| `NullPointerException` switch modo oscuro | Switch declarado en Java pero no existe en XML | Eliminar declaración, binding y listeners |
| Chips no se ven en Reportes | Texto blanco sobre fondo blanco | Fondo blanco opaco + texto verde |
| Scroll no llega a "Cerrar sesión" | Sin `paddingBottom` en ScrollView | `paddingBottom="80dp"` + `clipToPadding="false"` |
| `cannot find symbol iv_avatar_home` | ID inexistente en layout | Eliminar referencia del Java |
| `cannot find symbol NavController` | Import faltante en HomeFragment | Agregar `import androidx.navigation.NavController` |
| Colores cambian en el celular | Tema `DayNight` adaptaba colores al modo oscuro del sistema | Cambiar a `Theme.MaterialComponents.Light.NoActionBar` |
| App se sale al navegar rápido | Doble click lanzaba navegación duplicada | `SafeNav.java` — bloquea navegaciones en menos de 600ms |
| `RejectedExecutionException` en HomeFragment | `executor.shutdown()` en `onDestroyView` pero LiveData seguía disparando | Eliminar executor, calcular stats directamente en el hilo principal |
| Login lento / UI congelada al arrancar | `isLoggedIn()` y validación corrían en hilo principal | Mover a `ExecutorService` con `Handler` para volver al main thread |

---

## ⚡ Optimizaciones de Rendimiento

| Componente | Optimización |
|---|---|
| **LoginActivity** | Verificación de sesión y validación de credenciales en hilo secundario con `ExecutorService` |
| **LoginActivity** | `FLAG_ACTIVITY_CLEAR_TASK` para limpiar back stack — evita volver al login con el botón atrás |
| **LoginActivity** | Transición suave `fade_in / fade_out` en lugar de corte brusco |
| **HomeFragment** | `isAdded()` antes de actualizar la UI para evitar crashes si el fragment ya fue destruido |
| **HomeFragment** | `setHasFixedSize(false)` en RecyclerView para mediciones eficientes |
| **RegistroFragment / ProfileFragment** | Imagen procesada en `Thread` secundario con `inSampleSize` |
| **Tema** | `Light.NoActionBar` fuerza modo claro — sin cambios de colores por modo oscuro del sistema |
| **Navegación** | `SafeNav` evita crashes por navegación duplicada |

---

## 📂 Archivos de Datos de Prueba

| Archivo | Formato | Registros | Periodo | Uso |
|---|---|---|---|---|
| `residuos_ecolim_100.csv` | CSV | 100 | 2025–2026 | Prueba básica de importación |
| `residuos_ecolim_400.csv` | CSV | 400 | 2025–2026 | Prueba completa con todos los filtros |
| `residuos_ecolim_100.pdf` | PDF | 100 | 2025–2026 | Reporte con tabla coloreada por tipo |
| `residuos_ecolim_100.txt` | TXT | 100 | 2025–2026 | Reporte en texto plano con resumen |

### Estructura del CSV para importación
```
id,tipo,pesoKg,fecha,planta,zona,operarioId,turno,observaciones
1,Plastico,5.20,2026-02-22,Planta A,Zona B,OP-42,Mañana,Sin novedad
```

### Distribución del CSV de 400 registros
| Filtro | Registros |
|---|---|
| Hoy (2026-03-01) | 8 |
| Últimos 7 días | ~51 |
| Año 2026 | 113 |
| Año 2025 | 287 |

---

## 🎨 Paleta de Colores

| Nombre | Hex | Uso |
|---|---|---|
| `primary` | `#1B6F4A` | Color principal, textos y acciones |
| `primary_dark` | `#145238` | Header toolbar de Perfil |
| `accent` | `#00D97E` | Verde brillante para highlights |
| `bg_screen` | `#F0F4F2` | Fondo de todas las pantallas |
| `surface` | `#FFFFFF` | Cards y Bottom Navigation |
| `text_primary` | `#1A1A1A` | Texto principal |
| `text_hint` | `#888888` | Texto secundario |
| `danger` | `#FF5A5A` | Cerrar sesión y peligrosos |
| `divider` | `#E8E8E8` | Líneas divisoras |

---

## 🚀 Instalación y Compilación

### Requisitos
- Android Studio Hedgehog o superior
- JDK 11 o superior
- Android SDK API 24 – 34
- Gradle 8.x

### Pasos
```bash
# 1. Abrir el proyecto en Android Studio

# 2. Verificar gradle.properties
android.useAndroidX=true
android.enableJetifier=true

# 3. Verificar settings.gradle incluye:
maven { url 'https://jitpack.io' }

# 4. Verificar compileOptions en build.gradle (app):
compileOptions {
    sourceCompatibility JavaVersion.VERSION_11
    targetCompatibility JavaVersion.VERSION_11
}

# 5. Sync y compilar
Build > Rebuild Project
Run > Run 'app'
```

> 💡 **Tip de rendimiento:** La app puede verse lenta al correr desde Android Studio en modo Debug. Para probar el rendimiento real:
> `Build > Build Bundle(s) / APK(s) > Build APK(s)`

---

## 📌 Git — Flujo de trabajo

```bash
# Guardar cambios
git add .
git commit -m "fix: descripción del cambio"
git push

# Convenciones de commits
feat:     # Funcionalidad nueva
fix:      # Corrección de bug
refactor: # Mejora de código sin cambiar funcionalidad
perf:     # Mejora de rendimiento
docs:     # Documentación
```

---

## 🔮 Próximos Pasos

- [ ] Implementar modo oscuro real con `AppCompatDelegate.setDefaultNightMode()`
- [ ] Conectar con API REST para sincronización real
- [ ] Stats reales en Perfil (leer de Room en lugar de valores hardcoded)
- [ ] Galería de fotos de evidencia en Historial
- [ ] Filtros de búsqueda en Historial (fecha, tipo, operario)
- [ ] Dashboard de administrador con métricas globales
- [ ] Exportar gráficos como imagen dentro del PDF
- [ ] Notificaciones push para alertas de sincronización
- [ ] ML Kit para clasificación automática de residuos por foto

---

## 👷 Desarrollado para

**ECOLIM S.A.C.** — Gestión de Residuos Industriales  
EcoRegApp v1.1 — Marzo 2026

---

*Última actualización: 01/03/2026*
