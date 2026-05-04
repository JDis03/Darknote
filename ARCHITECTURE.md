# DarkNote - Snippet Manager

## 1. Visión del Producto

**DarkNote** es un **Snippet Manager** Local-First multiplataforma (Desktop + Móvil) diseñado específicamente para almacenar **código, comandos de terminal y configuraciones**.

### Propósito Principal
**Evitar errores al copiar y pegar código en la terminal.** 
- Sin caracteres especiales de formato (\r, metadatos HTML)
- Sin markdown que interfiera con el pegado
- Sanitización automática del portapapeles
- Texto plano puro y limpio

### Filosofía Local-First
- Archivos de texto plano (.txt) en sistema de archivos local
- Opcional: .md solo para documentación explicativa
- Sincronización asíncrona con Dropbox
- Sin dependencia de servidor propio

---

## 2. Stack Tecnológico

### Core (Multiplataforma)
```
Lenguaje: Kotlin 2.0+
Arquitectura: KMP (Kotlin Multiplatform)
Persistencia: SQLDelight (SQLite multiplataforma)
Sync: Dropbox Core SDK (desktop) / API REST (móvil)
Serialización: kotlinx.serialization
DI: Koin
```

### Desktop (JVM)
```
UI Framework: Jetpack Compose Desktop
Editor: TextEditor nativo Compose (texto plano puro)
File Watching: Java NIO WatchService (existente)
Clipboard: Sanitización automática de portapapeles
System Tray: compose-system-tray
```

### Móvil (Android)
```
UI: Jetpack Compose
Navigation: Compose Navigation
Editor: TextEdit plano (sin formato)
Sync: WorkManager para background sync
```

---

## 3. Arquitectura de Módulos

### Módulos Compartidos (shared/)
```
shared/
├── core/                    # Modelos, dominio, casos de uso
│   ├── src/commonMain/
│   │   ├── model/          # Snippet, Folder, Tag, SyncStatus
│   │   ├── repository/     # Contratos de repositorios
│   │   ├── usecase/        # Casos de uso del dominio
│   │   └── clipboard/      # Lógica de sanitización
│   └── src/commonTest/
├── persistence/            # Implementación de persistencia
│   └── src/commonMain/
│       ├── database/       # SQLDelight schemas
│       └── repository/     # Implementaciones
└── sync/                   # Lógica de sync con Dropbox
    └── src/commonMain/
        ├── client/         # DropboxClient KMP
        └── engine/         # SyncEngine
```

### Aplicaciones
```
apps/
├── desktop/                # Aplicación Desktop
│   ├── src/jvmMain/
│   │   ├── ui/            # Componentes Compose Desktop
│   │   ├── editor/        # Editor de texto plano
│   │   ├── tree/          # Árbol de snippets/carpetas
│   │   ├── clipboard/     # Gestión de portapapeles
│   │   └── main/          # MainWindow, ViewManager
│   └── src/jvmTest/
└── android/               # Aplicación Android
    └── src/androidMain/
        ├── ui/            # Screens Compose
        ├── components/    # Componentes reutilizables
        └── main/          # MainActivity
```

---

## 4. Arquitectura Desktop (Patrón Kate-Inspired)

### Separación Documento-Vista (como Kate)
```
Desktop Architecture:
├── MainWindow             # Ventana principal
│   ├── Sidebar (Left)     # Árbol de carpetas/snippets
│   ├── EditorPanel        # Panel central de edición
│   │   ├── ViewManager    # Gestiona splits/tabs
│   │   ├── ViewSpace      # Grupo de pestañas
│   │   └── SnippetView    # Editor individual de snippet
│   └── Sidebar (Right)    # Opcional: tags, metadata, preview
├── SnippetManager         # Gestor central de snippets
├── ClipboardManager       # Sanitización de portapapeles
└── SyncManager           # Lógica de sincronización
```

### Componentes Clave

#### 1. SnippetManager (como KateDocManager)
```kotlin
class SnippetManager {
    // Lista de snippets abiertos
    val snippets: StateFlow<List<Snippet>>
    
    // Crear/cargar snippet
    fun openSnippet(path: Path): Snippet
    
    // Guardar cambios
    fun saveSnippet(snippet: Snippet)
    
    // Detectar cambios externos
    fun checkExternalChanges(snippet: Snippet)
    
    // Copiar al portapapeles con sanitización
    fun copyToClipboard(snippet: Snippet, sanitize: Boolean = true)
}
```

#### 2. ClipboardManager (Sanitización)
```kotlin
class ClipboardManager {
    // Sanitizar texto para terminal
    fun sanitizeForTerminal(text: String): String {
        return text
            .replace("\r\n", "\n")      // Windows -> Unix
            .replace("\r", "\n")         // Mac antiguo -> Unix
            .replace(Regex("<[^>]*>"), "") // Eliminar tags HTML
            .trim()                       // Eliminar espacios extra
    }
    
    // Copiar al portapapeles del sistema
    fun copy(text: String, sanitize: Boolean = true)
    
    // Pegar desde portapapeles (para referencia)
    fun paste(): String
}
```

#### 3. ViewManager (como KateViewManager)
```kotlin
class ViewManager {
    // Split views (horizontal/vertical)
    val rootView: MutableState<ViewContainer>
    
    // Crear split
    fun split(view: ViewSpace, orientation: Orientation)
    
    // Historial de navegación
    val navigationHistory: Stack<Location>
}

sealed class ViewContainer {
    data class Split(
        val orientation: Orientation,
        val first: ViewContainer,
        val second: ViewContainer
    ) : ViewContainer()
    
    data class Leaf(val viewSpace: ViewSpace) : ViewContainer()
}
```

#### 4. SnippetView (Editor de Texto Plano)
```kotlin
class SnippetView(
    val snippet: Snippet,
    val viewSpace: ViewSpace
) {
    // Estado del editor
    val content: MutableState<String>
    val cursorPosition: MutableState<Position>
    val selection: MutableState<Selection?>
    
    // Comandos
    fun insertText(text: String)
    fun deleteSelection()
    fun find(query: String)
    fun selectAll(): String
    
    // Copiar selección con sanitización
    fun copySelection(sanitize: Boolean = true)
    
    // Copiar todo el snippet
    fun copyAll(sanitize: Boolean = true)
}
```

### Árbol de Snippets (Sidebar Left)
```kotlin
class SnippetTreeViewModel {
    val rootNodes: StateFlow<List<TreeNode>>
    val selectedNode: MutableState<TreeNode?>
    
    fun createSnippet(parent: Folder?)
    fun createFolder(parent: Folder?)
    fun moveNode(node: TreeNode, newParent: Folder?)
    fun deleteNode(node: TreeNode)
    fun search(query: String): List<TreeNode>
    
    // Copiar snippet rápido desde el árbol
    fun quickCopy(snippet: Snippet)
}

sealed class TreeNode {
    data class FolderNode(
        val folder: Folder,
        val children: List<TreeNode>,
        val isExpanded: Boolean
    ) : TreeNode()
    
    data class SnippetNode(
        val snippet: Snippet,
        val syncStatus: SyncStatus,
        val language: String?  // "bash", "python", "config", etc.
    ) : TreeNode()
}
```

---

## 5. Arquitectura Móvil (Obsidian-Inspired)

### Navegación Principal
```
Bottom Navigation:
├── Snippets     # Lista de snippets recientes + search
├── Quick Copy   # Snippets más usados / favoritos
├── Capture      # Quick snippet creation
├── Tags         # Exploración por etiquetas
└── Settings     # Configuración y sync
```

### Screens
```kotlin
// Lista de snippets
@Composable
fun SnippetsScreen(
    snippets: List<Snippet>,
    onSnippetClick: (Snippet) -> Unit,
    onQuickCopy: (Snippet) -> Unit,
    onCreateSnippet: () -> Unit
)

// Editor móvil simplificado (texto plano)
@Composable
fun MobileSnippetEditor(
    snippet: Snippet,
    onBack: () -> Unit,
    onSave: (String) -> Unit,
    onCopy: (String, Boolean) -> Unit  // text, sanitize
)

// Quick copy view (snippets favoritos)
@Composable
fun QuickCopyScreen(
    favorites: List<Snippet>,
    onCopy: (Snippet) -> Unit
)
```

---

## 6. Modelo de Datos

```kotlin
// Entidades principales
@Entity
data class Snippet(
    val id: String,              // UUID
    val title: String,
    val content: String,         // Texto plano puro
    val folderId: String?,
    val tags: List<String>,
    val language: String?,       // "bash", "python", "kotlin", "config", etc.
    val isFavorite: Boolean,
    val createdAt: Long,
    val modifiedAt: Long,
    val syncStatus: SyncStatus,
    val localPath: String,       // Ruta al archivo .txt
    val docPath: String?         // Ruta opcional a documentación .md
)

@Entity
data class Folder(
    val id: String,
    val name: String,
    val parentId: String?,
    val sortOrder: Int,
    val createdAt: Long
)

@Entity
data class SnippetMetadata(
    val snippetId: String,
    val usageCount: Int,         // Para "más usados"
    val lastCopiedAt: Long?,     // Última vez copiado
    val dropboxRev: String,      // Revision de Dropbox
    val localHash: String,       // Hash del contenido local
    val lastSyncAt: Long,
    val conflictStatus: ConflictStatus?
)

enum class SyncStatus { SYNCED, PENDING_UPLOAD, PENDING_DOWNLOAD, CONFLICT }
enum class ConflictStatus { LOCAL_WINS, REMOTE_WINS, MERGE_NEEDED }

// Configuración de sanitización
@Entity
data class ClipboardSettings(
    val autoSanitize: Boolean,   // Sanitizar automáticamente al copiar
    val removeHtml: Boolean,      // Eliminar tags HTML
    val normalizeNewlines: Boolean, // Convertir a \n
    val trimWhitespace: Boolean   // Eliminar espacios al inicio/final
)
```

---

## 7. Sincronización (Sync Engine)

### Arquitectura
```kotlin
class SyncEngine(
    val dropboxClient: DropboxClient,
    val localRepository: SnippetRepository,
    val syncRepository: SyncRepository
) {
    // Sync bidireccional
    suspend fun sync(): SyncResult {
        val localChanges = detectLocalChanges()
        val remoteChanges = dropboxClient.listChanges()
        
        return reconcile(localChanges, remoteChanges)
    }
    
    // Detección de cambios locales (WatchService)
    fun watchLocalChanges(): Flow<FileChange>
    
    // Resolución de conflictos
    fun resolveConflict(snippet: Snippet, strategy: ResolutionStrategy)
}

// Conflictos
sealed class SyncResult {
    data class Success(val synced: Int, val conflicts: Int) : SyncResult()
    data class Error(val exception: Exception) : SyncResult()
}
```

---

## 8. Features Clave por Plataforma

### Desktop
- ✅ **Editor de texto plano puro** (sin markdown, sin preview)
- ✅ **Árbol de carpetas/snippets** con organización jerárquica
- ✅ **Splits** (ver múltiples snippets lado a lado)
- ✅ **Sanitización automática** del portapapeles
- ✅ **Atajos de teclado** para copiar rápido (Ctrl+C sanitizado)
- ✅ **Búsqueda global** de snippets
- ✅ **Syntax highlighting básico** opcional (para legibilidad, no para el output)
- ✅ **System tray** con quick access

### Mobile
- ✅ **Quick Copy** - Snippets favoritos accesibles en 1 tap
- ✅ **Editor simple** - pegar y guardar rápido
- ✅ **Share intent** - guardar snippets desde otras apps
- ✅ **Offline-first** - trabajar sin conexión
- ✅ **Tags** - organización flexible

---

## 9. Plan de Implementación por Fases

### Fase 1: Core y Persistencia (Semanas 1-2)
- [ ] Setup KMP project structure
- [ ] Definir modelos de datos (Snippet, Folder, Tag)
- [ ] Implementar SQLDelight schemas
- [ ] Repositorios y casos de uso básicos
- [ ] Implementar ClipboardManager con sanitización
- [ ] Tests de unidad

### Fase 2: Desktop - Navegación y Árbol (Semanas 3-4)
- [ ] MainWindow con Compose Desktop
- [ ] Sidebar de árbol de carpetas/snippets
- [ ] Operaciones CRUD básicas
- [ ] File watching integrado
- [ ] Persistencia de estado de UI

### Fase 3: Desktop - Editor y Clipboard (Semanas 5-7)
- [ ] Editor de texto plano (TextField nativo)
- [ ] ViewManager con splits
- [ ] Tabs y navegación
- [ ] Integrar ClipboardManager con sanitización
- [ ] Atajos de teclado para copiar sanitizado
- [ ] Historial de posiciones

### Fase 4: Sincronización (Semanas 8-9)
- [ ] Portar DropboxClient a KMP
- [ ] Implementar SyncEngine
- [ ] Detección y resolución de conflictos
- [ ] Background sync
- [ ] Indicadores de sync status

### Fase 5: Polish Desktop (Semana 10)
- [ ] Atajos de teclado (inspirados en Kate)
- [ ] Temas (dark/light)
- [ ] System tray integration
- [ ] Configuración persistente
- [ ] Export/import de snippets

### Fase 6: Móvil - Base (Semanas 11-13)
- [ ] Setup proyecto Android
- [ ] Pantalla de lista de snippets
- [ ] Editor móvil básico
- [ ] Navigation
- [ ] Share extension para quick capture

### Fase 7: Móvil - Features (Semanas 14-16)
- [ ] Quick Copy / Favoritos
- [ ] Búsqueda avanzada
- [ ] Widgets
- [ ] Offline-first completo
- [ ] Biometric auth

### Fase 8: Integración y Release (Semanas 17-18)
- [ ] Tests E2E
- [ ] Documentación
- [ ] Release Desktop (Linux/AppImage)
- [ ] Release Android (Play Store/F-Droid)

---

## 10. Estructura de Carpetas Final

```
darknote/
├── build.gradle.kts          # Root build config
├── settings.gradle.kts
├── gradle/
├── .gitignore
├── README.md
├── ARCHITECTURE.md           # Este documento
├── apps/
│   ├── desktop/
│   │   ├── build.gradle.kts
│   │   └── src/
│   │       └── jvmMain/
│   │           └── kotlin/
│   │               └── com/darknote/desktop/
│   │                   ├── Main.kt
│   │                   ├── di/
│   │                   ├── ui/
│   │                   │   ├── components/
│   │                   │   ├── tree/
│   │                   │   ├── editor/
│   │                   │   └── theme/
│   │                   ├── clipboard/
│   │                   └── viewmodel/
│   └── android/
│       ├── build.gradle.kts
│       └── src/
│           └── androidMain/
│               └── kotlin/
│                   └── com/darknote/android/
│                       ├── MainActivity.kt
│                       ├── di/
│                       ├── ui/
│                       │   ├── screens/
│                       │   ├── components/
│                       │   └── theme/
│                       └── viewmodel/
├── shared/
│   ├── core/
│   │   ├── build.gradle.kts
│   │   └── src/
│   │       └── commonMain/
│   │           └── kotlin/
│   │               └── com/darknote/core/
│   │                   ├── model/
│   │                   ├── repository/
│   │                   ├── usecase/
│   │                   └── clipboard/
│   ├── persistence/
│   │   ├── build.gradle.kts
│   │   └── src/
│   │       └── commonMain/
│   │           └── kotlin/
│   │               └── com/darknote/persistence/
│   │                   ├── database/
│   │                   └── repository/
│   └── sync/
│       ├── build.gradle.kts
│       └── src/
│           └── commonMain/
│               └── kotlin/
│                   └── com/darknote/sync/
│                       ├── client/
│                       └── engine/
├── docs/
│   ├── setup.md
│   ├── contributing.md
│   └── api/
└── kate/                     # Referencia (en .gitignore)
```

---

## 11. Decisiones Técnicas Clave

### Tipo de Editor
**Texto Plano Puro**
- Sin markdown
- Sin preview
- Sin formato rico
- Solo texto limpio que se puede pegar en terminal SIN ERRORES

**Opcional:** Syntax highlighting visual (solo para legibilidad, no afecta el output)

### Clipboard Strategy
```kotlin
// Sanitización automática
val sanitized = text
    .replace("\r\n", "\n")      // Windows newlines -> Unix
    .replace("\r", "\n")         // Mac old -> Unix  
    .replace(Regex("<[^>]*>"), "") // Remove HTML tags
    .replace("\\u00A0", " ")      // Non-breaking space
    .trim()                       // Trim whitespace
```

**Comportamiento por defecto:**
- Copiar desde editor → Siempre sanitizado
- Botón "Copiar original" → Opción para casos especiales

### Formatos de Archivo
- **Principal:** `.txt` - Código/comandos limpios
- **Opcional:** `.md` - Documentación explicativa del snippet
- **Metadatos:** SQLite - sync status, tags, usage count

### Sync Strategy
- **Estrategia:** Last-write-wins con detección de conflictos
- **Conflictos:** UI para elegir local/remote/merge
- **Frecuencia:** On-save + timer cada 5 min + manual

### Almacenamiento Local
- **Base:** SQLite con SQLDelight
- **Archivos:** .txt en ~/.config/darknote/snippets/
- **DB:** Metadatos, índices de búsqueda, sync status, usage stats

---

## Referencias

- **Kate:** https://github.com/KDE/kate (en ./kate/)
- **Joplin:** https://github.com/laurent22/joplin
- **Obsidian:** https://obsidian.md/
- **KMP:** https://kotlinlang.org/docs/multiplatform.html
- **Compose Desktop:** https://www.jetbrains.com/lp/compose/
