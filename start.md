Especificaciones del Proyecto: NoteSync Kotlin (CLI/Desktop)
1. Visión General
Crear una aplicación de notas minimalista en Kotlin para Arch Linux. La aplicación debe funcionar bajo la filosofía "Local-First": los archivos se guardan como .md en el sistema de archivos local y se sincronizan de forma asíncrona con Dropbox.

Objetivo principal: Evitar errores de formato al copiar y pegar desde las notas hacia la terminal (Kitty).

2. Stack Tecnológico
Lenguaje: Kotlin 2.0+ (JVM).

Sistema Operativo: Arch Linux.

Entorno de ejecución: Kitty Terminal.

Librerías principales:

com.dropbox.core:dropbox-core-sdk: Para la integración con la nube.

org.jetbrains.kotlinx:kotlinx-coroutines: Para sincronización en segundo plano.

org.jetbrains.compose: (Opcional) Para la interfaz de escritorio.

java.nio.file: Para monitoreo de archivos en tiempo real.

3. Requerimientos Funcionales
A. Gestión Local (Eficiencia tipo Kate)
Directorio de trabajo: Las notas se almacenan en ~/.config/knotes/notes/.

Formato: Texto plano con extensión .md.

Sanitizado de Portapapeles: Implementar una función que, al copiar texto, elimine caracteres especiales de formato (\r, metadatos HTML) para que el pegado en la terminal sea 100% limpio.

B. Sincronización con Dropbox
Auth: Implementar OAuth2 con flujo PKCE para entornos de escritorio.

Sync Logic:

Detectar cambios en la carpeta local usando WatchService.

Comparar versiones mediante el hash del contenido.

Subir archivos nuevos o modificados automáticamente.

Descargar cambios remotos al iniciar la app.

4. Estructura de Código Sugerida
Kotlin
// 1. Monitor de archivos (File Watcher)
// 2. Cliente de Dropbox (Sync Engine)
// 3. Gestor de Portapapeles (Clipboard Sanitizer)
// 4. Interfaz de Usuario (Compose Desktop o CLI)
5. Tareas Iniciales para la IA
Generar el archivo build.gradle.kts con todas las dependencias necesarias.

Crear la clase DropboxClient que maneje el login y la subida de archivos.

Implementar la lógica de ClipboardManager para limpiar el texto antes de enviarlo al sistema.
