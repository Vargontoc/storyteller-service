# Sprint 01: agent-writer - agents

Estado: aprobado

## Alcance

- Tareas: TASK-006 (de `specs/agent-writer/tasks.md`)
- Dependencias internas: Ninguna.
- Dependencias externas: TASK-006 (agents) depende de TASK-005 (server): el backend debe aprovisionar al arrancar el modelo del escritor desde `api/src/main/resources/prompts/writer.modelfile` mediante `WriterModelProvisioner`, sin alterar la compatibilidad del aprovisionamiento del resto de modelos. La retirada se ejecuta solo tras su entrega y validación.

## Origen SDD

- Requirements: 5
- Design: 2
- Tasks: 1

## Enfoque técnico

Tras confirmar la entrega de TASK-005 en `server`, retirar la fuente duplicada del Modelfile del escritor en `agents/`; el recurso del backend pasa a ser la única fuente de mantenimiento, conforme a DEC-005. Revisar `agents/load_model.ps1` antes de editarlo: en el estado actual es un cargador genérico por parámetros y no contiene una referencia fija al escritor, por lo que no debe alterarse sin una referencia concreta que eliminar. La retirada no debe cambiar los Modelfiles ni el mecanismo de carga de director o narrador.

## Cambios por tarea

### TASK-006 Retirar el Modelfile del escritor de agents/

- **Ficheros:** `agents/writer-story-agent/Modelfile` (eliminar): retirar la copia obsoleta del Modelfile del escritor después de comprobar que TASK-005 aprovisiona el recurso equivalente desde el backend.
- **Ficheros:** `agents/load_model.ps1` (verificar; modificar solo si al revalidar existe una referencia explícita al escritor): eliminar exclusivamente esa referencia si existiera. Mantener su interfaz genérica (`ModelName` y `ModelFolder`) y su comportamiento para los demás modelos.
- **Enfoque:** No trasladar ni reescribir el contenido del Modelfile en este módulo. La propiedad del recurso nuevo y del aprovisionamiento corresponde a TASK-005 de `server`; antes de la retirada, comprobar su entrega y las pruebas de esa tarea. Tras ello, verificar que `agents/` no contiene el Modelfile ni referencias textuales al escritor, sin afectar los recursos de director y narrador.
- **Pruebas automáticas:** No aplica según TASK-006. Como revisión estática, comprobar que no existe `agents/writer-story-agent/Modelfile` ni referencia al escritor bajo `agents/`, y que siguen presentes los Modelfiles de director y narrador. Ejecutar el comando de prueba disponible del proyecto para el módulo si se incorpora uno; en ausencia de pruebas del módulo, documentar la revisión como evidencia de la tarea.
- **Prueba manual:** Con TASK-005 validada, revisar que el recurso del backend `api/src/main/resources/prompts/writer.modelfile` y `WriterModelProvisioner` son los que aprovisionan el escritor. Ejecutar `agents/load_model.ps1` para un modelo conservado con sus argumentos habituales y comprobar que termina sin error y no registra el escritor; verificar en Ollama que el escritor sigue disponible por el aprovisionamiento del backend.

## Orden de implementación

1. TASK-006 (habilitada por: TASK-005 de `server` entregada y validada, con el recurso `api/src/main/resources/prompts/writer.modelfile` y `WriterModelProvisioner` operativos y compatibles con los demás modelos).

## Riesgos y dudas técnicas

- Retirar la copia antes de que el backend esté validado dejaría el escritor sin fuente operativa. Mitigación: no iniciar la retirada hasta la evidencia de TASK-005.
- `load_model.ps1` es actualmente genérico y no contiene una referencia específica al escritor; forzar una modificación introduciría una regresión en el cargador de director o narrador. Mitigación: limitar cualquier edición a una referencia explícita que exista al momento de ejecutar la tarea.
- La tarea no puede demostrar desde `agents/` el aprovisionamiento del backend. Mitigación: registrar la evidencia de entrega y validación de TASK-005 antes de marcar TASK-006 como hecha.

## Validación

- Suite del módulo: pendiente
- Integración: pendiente
- Validación manual: pendiente
