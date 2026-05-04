# Aprendizajes de Joplin - DarkNote Mejorado

## 📊 Análisis de Joplin

### Lo que hacen bien ✅

1. **Monorepo organizado**
   - `packages/lib` - Lógica compartida
   - `packages/app-desktop` - Electron
   - `packages/app-mobile` - React Native
   - **Aprendizaje:** Separar core de UI es correcto

2. **Modelo de datos flexible**
   - Notas con metadatos extensibles
   - Jerarquía de carpetas simple (parent_id)
   - Tags muchos-a-muchos
   - **Aprendizaje:** Base sólida, pero podemos simplificar para snippets

3. **Sync robusto**
   - Synchronizer como servicio separado
   - Soporte múltiples backends (Dropbox, OneDrive, etc.)
   - Reportes de sync detallados
   - **Aprendizaje:** Arquitectura de sync bien separada

4. **Editor modular**
   - Package `editor` separado
   - Soporte para TinyMCE, CodeMirror
   - **Aprendizaje:** Editor desacoplado es buena idea

---

### Lo que podemos mejorar 🚀

#### 1. Arquitectura más moderna

**Joplin:**
```typescript
// Clases estáticas, callbacks
class Note {
  static async load(noteId: string): Promise<NoteEntity>
  static async save(note: NoteEntity): Promise<void>
}
```

**DarkNote (Mejor):**
```kotlin
// Coroutines, Flows, Clean Architecture
interface SnippetRepository {
    suspend fun getById(id: String): Snippet?
    fun getAll(): Flow<List<Snippet>>  // Reactivo
    suspend fun save(snippet: Snippet): Result<Unit>
}

// Use cases explícitos
class CopySnippetUseCase(
    private val repository: SnippetRepository,
    private val clipboardManager: ClipboardManager
) {
    suspend operator fun invoke(snippetId: String, sanitize: Boolean = true): Result<Unit>
}
```

**Ventaja:** Código más testeable, reactivo, y con manejo de errores explícito.

#### 2. UI más moderna

**Joplin:** React + Redux + CSS tradicional

**DarkNote (Mejor):** Jetpack Compose
- Declarativa y type-safe
- Animaciones fluidas
- Single source of truth con StateFlow
- Previews en tiempo real

#### 3. Base de datos type-safe

**Joplin:** Queries SQL manuales

**DarkNote (Mejor):** SQLDelight
```sql
-- Schema type-safe
CREATE TABLE snippet (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    content TEXT NOT NULL
);

-- Queries verificadas en compile time
selectById:
SELECT * FROM snippet WHERE id = ?;
```

**Ventaja:** Seguridad de tipos en tiempo de compilación.

#### 4. Nuestro diferenciador: Clipboard Sanitizado

**Joplin:** No tienen esta función específica

**DarkNote (Único):**
```kotlin
class ClipboardSanitizer {
    fun sanitize(text: String): String {
        return text
            .replace("\r\n", "\n")           // Windows → Unix
            .replace("\u00A0", " ")           // Non-breaking space
            .replace(Regex("<[^>]*>"), "")   // HTML tags
            .trim()
    }
}

// Siempre sanitizado por defecto
clipboardManager.copy(snippet.content, sanitize = true)
```

**Ventaja:** Especializado en código/comandos que funcionan al 100% en terminal.

#### 5. Simplificación para snippets

**Joplin:** Feature-rich (markdown, encriptación, geolocalización, etc.)

**DarkNote (Mejor - enfocado):**
- Solo texto plano (no markdown para código)
- Sin encriptación (snippets no son sensibles típicamente)
- Sin geolocalización
- Sin recursos adjuntos
- **Foco:** Copiar-pegar código rápido y limpio

#### 6. Sincronización más simple

**Joplin:** Soporta múltiples backends complejos

**DarkNote (Mejor):**
- Solo Dropbox (para empezar)
- Archivos .txt planos (fácil de debuggear)
- Conflictos resolubles manualmente
- Sync on-demand + background

---

## 📋 Comparativa Arquitectura

| Aspecto | Joplin | DarkNote | Ganador |
|---------|--------|----------|---------|
| Lenguaje | TypeScript | Kotlin | 🤝 Empate |
| UI Framework | React + Redux | Compose + Coroutines | 🏆 DarkNote |
| Base de Datos | SQLite manual | SQLDelight | 🏆 DarkNote |
| Reactividad | Redux manual | StateFlow | 🏆 DarkNote |
| Type Safety | Runtime | Compile-time | 🏆 DarkNote |
| Multiplataforma | Electron/RN | KMP | 🏆 DarkNote |
| Clipboard | Básico | Sanitizado avanzado | 🏆 DarkNote |
| Features | 100+ | Esenciales | 🤝 Diferente foco |

---

## 🎯 Decisiónes Tomadas Basadas en Joplin

### ✅ Mantener (como Joplin)
1. **Separación core/UI** - Monorepo con shared y apps
2. **Jerarquía de carpetas** - parent_id simple y efectivo
3. **Sync como servicio** - Arquitectura desacoplada
4. **Tags** - Para categorización cruzada

### 🚀 Mejorar (vs Joplin)
1. **Coroutines + Flows** - Async moderno vs callbacks
2. **SQLDelight** - Type safety en BD
3. **Compose** - UI declarativa vs React
4. **KMP** - Código compartido real vs duplicación

### 💡 Innovar (único de DarkNote)
1. **Clipboard sanitizado** - Nuestro core feature
2. **Editor de splits** - Estilo Kate para comparar snippets
3. **Quick copy móvil** - Favoritos al estilo Obsidian
4. **Texto plano obligatorio** - No markdown para código

---

## 🛤️ Roadmap Inspirado

### Fase 1: Setup ✅ (Completado)
- KMP structure
- Modelos básicos
- Build configurado

### Fase 2: Core (Aprendiendo de Joplin/lib)
- Repositorios con Flows
- Casos de uso
- Servicios de clipboard

### Fase 3: Desktop UI (Mejor que Joplin/Electron)
- Árbol de carpetas optimizado
- Editor con splits (Kate-style)
- Atajos de teclado potentes

### Fase 4: Sync (Simplificado vs Joplin)
- Dropbox únicamente
- Archivos .txt planos
- Conflictos manuales

### Fase 5: Mobile (Estilo Obsidian)
- Quick copy desde favoritos
- Captura rápida
- Offline-first

---

## 📚 Referencias en Código

**Joplin:**
- Modelos: `/joplin/packages/lib/models/`
- Desktop UI: `/joplin/packages/app-desktop/gui/`
- Sync: `/joplin/packages/lib/Synchronizer/`

**DarkNote:**
- Modelos: `/shared/core/src/commonMain/kotlin/com/darknote/core/model/`
- Desktop UI: `/apps/desktop/src/jvmMain/kotlin/com/darknote/desktop/`
- Sync: `/shared/sync/src/commonMain/kotlin/com/darknote/sync/`

---

## Conclusión

**Joplin es excelente** como referencia de feature-set y estructura, pero **DarkNote debe ser mejor en arquitectura** usando Kotlin moderno, Compose, y KMP.

**Ventajas competitivas de DarkNote:**
1. Especialización en snippets de código
2. Clipboard sanitizado (único)
3. Editor de splits (Kate-inspired)
4. Stack tecnológico moderno
5. Simplicidad vs feature-bloat

**Mantenemos:** Estructura de carpetas, concepto de sync, jerarquía simple.
**Mejoramos:** Arquitectura, tecnologías, performance, type safety.
**Innovamos:** Clipboard sanitizado, flujo de trabajo optimizado para developers.
