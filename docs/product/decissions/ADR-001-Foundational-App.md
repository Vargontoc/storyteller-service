# ADR-001 - Aplicación fundacional

## Estado

Aceptada
Fecha: 2026-08-01

## Contexto

Storyteller Agent genera cuentos infantiles para niños de 3 a 4 años mediante un modelo de lenguaje. No requiere registro de usuarios ni métricas avanzadas de rendimiento.

El sistema consta de una API Java/Spring Boot, una interfaz web ligera y servicios de infraestructura ejecutables localmente con Docker Compose. Debe poder usar Ollama como proveedor local por defecto y permitir un proveedor externo mediante configuración.

Los cuentos generados deben persistirse. La memoria conversacional se incorporará cuando exista un flujo de chat definido.

## Decisión

1. La aplicación se distribuirá y ejecutará mediante Docker Compose. El repositorio documentará requisitos, variables de entorno y el procedimiento de arranque local.

2. PostgreSQL será el almacén de datos obligatorio para los cuentos generados. La persistencia de memoria conversacional se añadirá sobre la misma base de datos cuando se implemente el chat con estado.

3. Ollama será el proveedor de modelos local predeterminado para desarrollo. Se ejecutará como servicio opcional en Docker Compose.

4. La API admitirá proveedores externos de modelos mediante variables de entorno, sin requerir cambios de código. Cuando se configure un proveedor externo, Ollama no será necesario.

5. La API será responsable de:
   - Validar las solicitudes de generación.
   - Construir y enviar prompts al modelo configurado.
   - Aplicar restricciones adecuadas a contenido para menores.
   - Persistir cuentos y sus metadatos mínimos.
   - Devolver respuestas HTTP estables a la interfaz web.

6. No se incluirán cuentas de usuario, autorización ni observabilidad avanzada en esta fase. Se conservará la posibilidad de incorporarlas sin cambiar el contrato principal de generación.

## Consecuencias

### Positivas

- El entorno local es reproducible y requiere pocos pasos.
- Los cuentos sobreviven a reinicios de la aplicación.
- El uso local no depende de una API externa.
- El proveedor de IA puede cambiarse mediante configuración.

### Negativas

- Docker Compose tendrá al menos API y PostgreSQL, y opcionalmente Ollama.
- Se deberán gestionar credenciales de proveedores externos exclusivamente mediante variables de entorno.
- La API necesitará migraciones de base de datos y pruebas de persistencia.
- Ollama puede requerir más recursos locales y descarga previa del modelo.

