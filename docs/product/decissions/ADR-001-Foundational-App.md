# ADR-001 - Aplicación fundacional

## Estado

proposal
Fecha: 2026-08-11

## Contexto

Storyteller Agent genera cuentos infantiles para niños de 3 a 4 años mediante un modelo de lenguaje. No requiere registro de usuarios ni métricas avanzadas de rendimiento.

El sistema consta de una API Java/Spring Boot, una interfaz web ligera y servicios de infraestructura ejecutables localmente con Docker Compose. Debe poder usar Ollama como proveedor local de texto, ComfyUI como proveedor local de imagen, y Chatterbox como proveedor local de audio (ya operativo como servicio independiente en la infraestructura del usuario).

La generación de un cuento no es una única llamada al modelo: es un proceso interactivo y con estado, en el que el usuario valida y ajusta el contenido en varios puntos antes de darlo por terminado. Los cuentos generados, así como el progreso intermedio de generación, deben persistirse. La memoria conversacional se incorporará cuando exista un flujo de trabajo definido.

## Decisión

1. La aplicación se distribuirá y ejecutará mediante Docker Compose. El repositorio documentará requisitos, variables de entorno y el procedimiento de arranque local.

2. PostgreSQL será el almacén de datos obligatorio, tanto para los cuentos terminados como para el estado intermedio de generación (borradores en curso, historial de intentos por página). La persistencia de memoria conversacional se añadirá sobre la misma base de datos cuando se implemente el chat con estado.

3. Ollama será el proveedor de modelos de texto local predeterminado. Se ejecutará como servicio en Docker Compose, activable/desactivable por perfil según el entorno.

4. ComfyUI será el proveedor de generación de imágenes de los cuentos. Se ejecutará como servicio en Docker Compose, activable/desactivable por perfil.

5. Chatterbox será el proveedor de generación de audio de los cuentos. Ya opera como servicio independiente en la infraestructura local del usuario; la API se integrará contra él como servicio externo configurable por variables de entorno, sin gestionarlo dentro del propio Docker Compose del proyecto.

6. Debido a que Ollama y ComfyUI comparten la misma GPU local con VRAM limitada, no se garantiza su ejecución simultánea a plena carga. La generación de un cuento sigue un pipeline secuencial por fases: primero se completa y aprueba todo el texto del cuento (fase de texto, con Ollama), y solo después se generan las imágenes (fase de imagen, con ComfyUI), nunca de forma intercalada dentro de la misma pasada. La API es responsable de orquestar la carga/descarga de los modelos correspondientes entre fases.

7. El agente del dominio será responsable de:

   - Generar, a partir de la petición inicial del usuario, un guion (outline) coherente: tema, personajes con su descripción, y un reparto de la trama en beats narrativos distribuidos según el número de páginas configurado por el usuario.
   - Permitir la revisión y edición manual del guion por parte del usuario antes de iniciar la generación de páginas.
   - Generar cada página de forma individual (texto + prompt de imagen asociado) siguiendo el guion aprobado, en formato JSON.
   - Permitir al usuario, por cada página generada, aprobarla o solicitar su regeneración, opcionalmente con una pista (hint) de ajuste. El agente evaluará si la pista contradice el guion troncal; si detecta contradicción, avisará al usuario, que podrá igualmente forzar la generación con esa pista.
   - No limitar el número de reintentos de regeneración por página.
   - Replanificar los beats narrativos restantes cuando el usuario modifique el número de páginas a mitad de generación, tomando como contexto fijo las páginas ya aprobadas.
   - Una vez aprobado el texto completo del cuento, generar una imagen de referencia por cada personaje del guion (vía ComfyUI), y reutilizar dichas referencias para mantener consistencia visual en la generación de imagen de cada página, que se disparará bajo demanda del usuario.

8. La API será responsable de:

   - Validar las solicitudes de generación.
   - Construir y enviar prompts al modelo configurado.
   - Aplicar restricciones adecuadas a contenido para menores.
   - Persistir cuentos, sus metadatos mínimos, y el estado intermedio de generación (guion, páginas por estado, historial de intentos incluyendo pistas forzadas por el usuario).
   - Permitir reanudar un cuento en curso en una sesión posterior.
   - Devolver respuestas HTTP estables a la interfaz web.
   - Orquestar el pipeline secuencial texto → imagen descrito en el punto 6.
   - Integrarse con ComfyUI para generar imágenes.
   - Integrarse con Chatterbox para generar los audios de los distintos textos con distintas tonalidades.

9. La app será responsable de:

   - Mostrar una interfaz sencilla e intuitiva para el usuario, con un flujo tipo asistente (wizard) que refleje el proceso por fases: revisión del guion, generación y validación de páginas de texto, y generación de imágenes bajo demanda.

## Stack Tech

- Java + Spring Boot + Spring AI
- Ollama
- Vue3
- PostgreSQL
- ComfyUI
- Chatterbox

## Consecuencias

### Positivas

- El entorno local es reproducible y requiere pocos pasos.
- Los cuentos, y el progreso a medio terminar, sobreviven a reinicios de la aplicación y a cierres de sesión del usuario.
- El uso local no depende de una API externa para texto ni imagen.
- El proveedor de IA de texto puede cambiarse mediante configuración.
- La estrategia de imagen de referencia por personaje da consistencia visual entre páginas sin depender únicamente del prompt de texto.
- El historial de intentos por página deja trazabilidad de por qué cada cuento terminó como terminó, incluidas las pistas forzadas por el usuario pese a aviso de contradicción.

### Negativas

- Docker Compose tendrá al menos API y PostgreSQL, y opcionalmente Ollama y ComfyUI.
- Se deberán gestionar credenciales y endpoints de proveedores externos (incluido Chatterbox) exclusivamente mediante variables de entorno.
- La API necesitará migraciones de base de datos y pruebas de persistencia, incluyendo el modelo de estado intermedio (borradores, intentos por página).
- Ollama y ComfyUI pueden requerir más recursos locales y descarga previa de modelos.
- La restricción de VRAM compartida entre Ollama y ComfyUI obliga a un pipeline secuencial por fases, con la complejidad añadida de orquestar carga/descarga de modelos y sin generación de imagen en tiempo real durante la fase de texto.
- El número de intentos de regeneración sin límite, combinado con el historial completo por intento, puede hacer crecer significativamente el volumen de datos persistidos por cuento.