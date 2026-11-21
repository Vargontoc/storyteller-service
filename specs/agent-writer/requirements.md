# Requisitos: agent-writer

Estado: aprobado
Revisión: 5

## Contexto

Con el guion aprobado, el agente escritor genera la escena de portada y después las páginas del cuento, una a una y a partir del beat de cada página. El adulto revisa cada resultado, pide cambios con un hint, elige entre las versiones generadas y acepta antes de seguir. El texto es para un niño de 3 a 4 años que lo escucha. Las imágenes de portada y de página se crean fuera de la aplicación; el sistema solo muestra la imagen importada cuando existe.

## Fuera de alcance

- Generación de audio de las páginas: es la etapa siguiente y no se especifica aquí.
- Generación de la imagen de portada y de la ilustración de página: se hacen fuera de la aplicación y no entran en esta feature ni en futuras.
- Importación de la imagen de portada o de página creada fuera de la aplicación.
- Generación de un título nuevo para la portada: el título es el del guion aprobado.
- Edición manual del texto de una página o de la descripción de escena.
- Regeneración de una página o de la portada sin hint, salvo el caso de REQ-029.
- Límite de palabras por frase: solo se limitan el número de frases y el total de palabras del texto narrativo.
- Regeneración automática de las páginas descartadas por un cambio.
- Conservar o recuperar las versiones no aceptadas de un histórico después de aceptar o de descartar.
- Cambios en el comportamiento de los cuentos antiguos sin escaleta.
- Mecanismo técnico de aprovisionamiento del Modelfile del escritor en el servidor.

## Requisitos funcionales

### REQ-001 Generar la escena de portada

- **Requisito:** Cuando el usuario solicita generar la portada de un cuento con guion aprobado, el sistema deberá pedir al escritor una descripción de escena de portada que use el título del guion aprobado sin modificarlo.
- **Criterios de aceptación:**
  - Dado un cuento con guion aprobado y sin portada, cuando el usuario solicita generar la portada, entonces el sistema muestra una descripción de escena de portada en estado pendiente de aceptación.
  - Dada una portada generada, cuando el usuario la consulta, entonces el título mostrado es idéntico al título del guion aprobado.
  - Dada una portada generada, cuando el usuario la consulta, entonces no tiene imagen asociada.

### REQ-002 Portada aceptada antes de la primera página

- **Requisito:** El sistema deberá impedir generar la página 1 mientras la portada no esté aceptada.
- **Criterios de aceptación:**
  - Dado un cuento con la portada pendiente de aceptación, cuando se solicita generar la página 1, entonces el sistema rechaza la operación e indica que primero hay que aceptar la portada.
  - Dado un cuento con la portada aceptada, cuando el usuario solicita generar la página 1, entonces el sistema inicia la generación.

### REQ-003 Generar una página desde su beat

- **Requisito:** Cuando el usuario solicita generar la página N, el sistema deberá pedir al escritor esa única página a partir del beat N de la escaleta aprobada.
- **Criterios de aceptación:**
  - Dado un cuento con la página N-1 aceptada, cuando el usuario solicita generar la página N, entonces el sistema genera solo la página N.
  - Dada una página N generada, cuando el usuario la consulta, entonces sabe a qué beat de la escaleta corresponde.

### REQ-004 Contexto que recibe el escritor

- **Requisito:** Cuando el sistema pide al escritor la página N, deberá entregarle el beat N aprobado, el título, la sinopsis y el reparto episódico del guion aprobado, la instantánea del protagonista del cuento y el texto de las páginas 1 a N-1 aceptadas.
- **Criterios de aceptación:**
  - Dado un cuento con las páginas 1 y 2 aceptadas, cuando se genera la página 3, entonces el escritor recibe el beat 3, el título, la sinopsis, el reparto episódico, la instantánea del protagonista y el texto aceptado de las páginas 1 y 2.
  - Dado un cuento cuyo protagonista se ha editado en el catálogo después de crear el cuento, cuando se genera una página, entonces el escritor recibe la instantánea guardada en el cuento y no la ficha actual del catálogo.

### REQ-005 Contenido de una página

- **Requisito:** El sistema deberá exigir que cada página generada contenga un texto narrativo y una descripción de escena, sin imagen.
- **Criterios de aceptación:**
  - Dada una página recién generada, cuando el usuario la consulta, entonces ve su texto narrativo y su descripción de escena.
  - Dada una página recién generada, cuando el usuario la consulta, entonces no tiene imagen asociada.

### REQ-006 Fidelidad al beat

- **Requisito:** El sistema deberá exigir que el texto narrativo y la descripción de escena de la página N narren el hecho del beat N sin alterar su función narrativa, los personajes que aparecen, la emoción o tono ni la nota visual.
- **Criterios de aceptación:**
  - Dado un beat cuyo campo "quiénes aparecen" lista al protagonista y a un conejo blanco, cuando se genera su página, entonces el texto y la escena no incluyen personajes con nombre que no figuren en ese campo.
  - Dado un beat con un hecho concreto, cuando se genera su página, entonces el texto narra ese hecho y no lo sustituye por otro.
  - Dado un beat con una nota visual, cuando se genera su página, entonces la descripción de escena recoge esa nota visual.

### REQ-007 Límite de longitud del texto narrativo

- **Requisito:** El sistema deberá considerar que el texto narrativo de una página cumple el límite de longitud cuando tiene de 1 a 4 frases y 66 palabras en total como máximo (límite de 60 palabras con una tolerancia del 10 %); no se limita el número de palabras de cada frase y la descripción de escena no está sujeta a este límite.
- **Criterios de aceptación:**
  - Dado un texto narrativo de 3 frases y 64 palabras en total, cuando se valida la página, entonces cumple el límite y se presenta al usuario para su revisión sin aviso.
  - Dado un texto narrativo de 2 frases y 67 palabras en total, cuando se valida la página, entonces incumple el límite.
  - Dado un texto narrativo de 5 frases y 40 palabras en total, cuando se valida la página, entonces incumple el límite.
  - Dado un texto narrativo de 1 frase de 25 palabras, cuando se valida la página, entonces cumple el límite.
  - Dada una descripción de escena de más de 66 palabras, cuando se valida la página, entonces la longitud de la escena no hace que incumpla el límite.

### REQ-008 Frases sin subordinadas encadenadas

- **Requisito:** El sistema deberá exigir que ninguna frase del texto narrativo contenga subordinadas encadenadas, es decir, una oración subordinada que dependa de otra subordinada.
- **Criterios de aceptación:**
  - Dada una página generada, cuando un revisor lee cada frase del texto narrativo, entonces ninguna contiene una subordinada dentro de otra subordinada (por ejemplo, "vio al pájaro que buscaba la rama donde estaba su nido").

### REQ-009 Lenguaje para niños de 3 a 4 años

- **Requisito:** El sistema deberá exigir que el texto narrativo use vocabulario y frases que entienda un niño de 3 a 4 años al escucharlo, sin imitar el habla infantil.
- **Criterios de aceptación:**
  - Dada una página generada, cuando un revisor la lee, entonces no contiene palabras que un niño de 3 a 4 años no conozca, salvo los nombres propios.
  - Dada una página generada con diálogos, cuando un revisor la lee, entonces los diálogos no imitan errores de pronunciación o de gramática propios del habla infantil.

### REQ-010 Generación secuencial con aceptación

- **Requisito:** El sistema deberá impedir generar la página N+1 mientras la página N no esté aceptada.
- **Criterios de aceptación:**
  - Dado un cuento con la página 2 pendiente de aceptación, cuando se solicita generar la página 3, entonces el sistema rechaza la operación e indica que primero hay que aceptar la página 2.
  - Dado un cuento con la página 2 aceptada y sin página 3, cuando el usuario solicita generar la página 3, entonces el sistema inicia la generación.

### REQ-011 Aceptar la portada o una página

- **Requisito:** Cuando el usuario acepta la portada o una página pendiente de aceptación, el sistema deberá marcarla como aceptada con su versión vigente (REQ-037) sin iniciar la generación de la página siguiente; una versión vigente con aviso de número de frases no se puede aceptar (REQ-028).
- **Criterios de aceptación:**
  - Dada la página 1 pendiente de aceptación, cuando el usuario la acepta, entonces la página 1 queda aceptada y se habilita la acción de generar la página 2.
  - Dada la portada pendiente de aceptación, cuando el usuario la acepta, entonces la portada queda aceptada y se habilita la acción de generar la página 1.
  - Dada la página 1 recién aceptada, cuando el usuario no solicita generar la página 2, entonces no se inicia ninguna generación.

### REQ-012 Pedir un cambio con hint

- **Requisito:** Cuando el usuario envía un hint sobre la portada o una página pendiente de aceptación, el sistema deberá pedir al escritor una nueva versión completa de esa portada o página a partir de su versión vigente, aplicando el hint.
- **Criterios de aceptación:**
  - Dada la página 3 pendiente de aceptación, cuando el usuario envía el hint "que el conejo esté contento", entonces el sistema muestra una nueva versión de la página 3 con texto narrativo y descripción de escena, pendiente de aceptación.
  - Dada la página 3 con las versiones 1 y 2 en su histórico y la versión 1 vigente, cuando el usuario envía un hint, entonces el escritor recibe la versión 1 como base del cambio.
  - Dada una nueva versión de la página 3 pendiente de aceptación, cuando el usuario envía otro hint, entonces el sistema genera otra versión completa de la página 3.

### REQ-013 Hint obligatorio

- **Requisito:** Si el usuario solicita un cambio de la portada o de una página sin escribir un hint, entonces el sistema deberá rechazar la solicitud sin pedir nada al escritor.
- **Criterios de aceptación:**
  - Dada una página pendiente de aceptación, cuando el usuario envía un hint vacío o formado solo por espacios, entonces el sistema rechaza la solicitud, indica que el hint es obligatorio y la página no cambia.

### REQ-014 Rechazo motivado del hint

- **Requisito:** Si el hint pide algo que se sale del beat de la página o incumple las reglas de edad del producto, entonces el sistema deberá rechazarlo, mostrar el motivo que da el escritor y conservar la versión vigente.
- **Criterios de aceptación:**
  - Dada la página 3, cuyo beat no incluye al lobo, cuando el usuario envía el hint "que aparezca el lobo", entonces el sistema indica que el hint no se acepta, muestra el motivo y la página 3 conserva su versión vigente.
  - Dada una página, cuando el usuario envía un hint con contenido no apto para 3 a 4 años, entonces el sistema indica que el hint no se acepta y muestra el motivo.

### REQ-015 Reabrir una página aceptada

- **Requisito:** Cuando el usuario envía un hint sobre una página ya aceptada, el sistema deberá pedir al escritor una nueva versión de esa página y presentarla como propuesta pendiente de aceptación en el histórico de propuestas de esa reapertura (REQ-038).
- **Criterios de aceptación:**
  - Dado un cuento con las páginas 1 a 4 aceptadas, cuando el usuario envía un hint sobre la página 2, entonces el sistema muestra una nueva versión de la página 2 pendiente de aceptación.
  - Dada una propuesta pendiente para la página 2 reabierta, cuando el usuario consulta el cuento antes de aceptarla, entonces las páginas 3 y 4 siguen presentes.
  - Dada una propuesta pendiente para la página 2 reabierta, cuando el usuario envía otro hint, entonces el escritor recibe como base la propuesta vigente.
  - Dada la página 2 aceptada con imagen importada, cuando el usuario envía un hint sobre ella, entonces la versión aceptada sigue mostrando su imagen y la propuesta se muestra sin imagen.

### REQ-016 Descarte en cascada al aceptar una página reabierta

- **Requisito:** Cuando el usuario acepta la propuesta vigente de una página reabierta K, el sistema deberá eliminar del cuento todas las páginas posteriores a K, junto con la imagen importada que tuvieran.
- **Criterios de aceptación:**
  - Dado un cuento con las páginas 1 a 4 aceptadas y una propuesta pendiente para la página 2, cuando el usuario acepta la propuesta, entonces la página 2 queda aceptada con la nueva versión y las páginas 3 y 4 dejan de existir en el cuento.
  - Dado el descarte anterior, cuando el usuario consulta el cuento, entonces la siguiente página que puede generar es la 3.
  - Dado un descarte en cascada que ha eliminado la página 3, que tenía imagen importada, cuando el usuario vuelve a generar la página 3, entonces la nueva página 3 se muestra sin imagen y solo con su descripción de escena.

### REQ-017 Aviso antes del descarte en cascada

- **Requisito:** Si la página reabierta tiene páginas posteriores, entonces el sistema deberá avisar de cuántas páginas se eliminarán y pedir confirmación antes de aceptar la nueva versión.
- **Criterios de aceptación:**
  - Dado un cuento con las páginas 1 a 4 y una propuesta pendiente para la página 2, cuando el usuario pulsa aceptar, entonces el sistema avisa de que se eliminarán 2 páginas y pide confirmación.
  - Dado el aviso anterior, cuando el usuario cancela, entonces la propuesta sigue pendiente y las páginas 3 y 4 permanecen.

### REQ-018 Sin regeneración automática de las páginas descartadas

- **Requisito:** El sistema no deberá regenerar automáticamente las páginas eliminadas por un descarte en cascada; cada una se generará cuando el usuario la solicite, siguiendo REQ-010.
- **Criterios de aceptación:**
  - Dado un descarte en cascada que ha eliminado las páginas 3 y 4, cuando termina el descarte, entonces no se inicia ninguna generación de página.
  - Dado el cuento anterior, cuando el usuario solicita generar la página 3, entonces se genera solo la página 3.

### REQ-019 Sin edición manual del texto

- **Requisito:** El sistema no deberá permitir editar a mano el texto narrativo ni la descripción de escena de la portada o de las páginas.
- **Criterios de aceptación:**
  - Dada una página pendiente o aceptada, cuando el usuario la visualiza, entonces no dispone de ninguna acción para escribir directamente en su texto narrativo o en su descripción de escena.

### REQ-020 Fallo de generación

- **Requisito:** Si la generación de la portada o de una página no produce un resultado por un error, entonces el sistema deberá informar del fallo, conservar el estado anterior del cuento y permitir volver a solicitar la misma operación; una página que incumple el límite de longitud tras agotar los intentos de REQ-025 no se trata como fallo, sino según REQ-026.
- **Criterios de aceptación:**
  - Dado un cuento con la página 2 aceptada, cuando falla la generación de la página 3, entonces el sistema muestra un mensaje de error, la página 3 no existe y el usuario puede volver a solicitar su generación.
  - Dada la página 3 pendiente de aceptación, cuando falla la generación de la versión pedida con un hint, entonces el sistema muestra un mensaje de error y la página 3 conserva su histórico y su versión vigente sin cambios.
  - Dada una página cuyas 3 generaciones incumplen el límite de longitud, cuando terminan, entonces el sistema no muestra un mensaje de fallo de generación y presenta la página según REQ-026.

### REQ-021 Cuento con todas las páginas aceptadas

- **Requisito:** Cuando el usuario acepta la última página de la escaleta, el sistema deberá dejar el cuento con todas sus páginas aceptadas y listo para la etapa de audio.
- **Criterios de aceptación:**
  - Dado un guion de 8 páginas con las páginas 1 a 7 aceptadas, cuando el usuario acepta la página 8, entonces el cuento indica que todas sus páginas están aceptadas y que está listo para la etapa de audio.
  - Dado un cuento con todas sus páginas aceptadas, cuando el usuario lo consulta, entonces no se ofrece generar más páginas.

### REQ-022 Vista de la portada y las páginas

- **Requisito:** Cuando el usuario abre las páginas de un cuento con escaleta, el sistema deberá mostrar la portada y cada página generada con el texto narrativo, la descripción de escena y el aviso de longitud de su versión vigente, la imagen importada de su versión aceptada si existe (REQ-040), su estado (pendiente de aceptación o aceptada), su histórico de versiones si está pendiente y las acciones disponibles en ese estado.
- **Criterios de aceptación:**
  - Dada una página aceptada con imagen importada, cuando el usuario la visualiza, entonces ve la imagen además de su texto narrativo y su descripción de escena.
  - Dada una portada aceptada con imagen importada, cuando el usuario la visualiza, entonces ve la imagen además de su título y su descripción de escena.
  - Dada una página aceptada sin imagen importada, cuando el usuario la visualiza, entonces ve su texto narrativo y su descripción de escena, sin imagen.
  - Dada una página pendiente de aceptación cuya versión vigente no tiene aviso, cuando el usuario la visualiza, entonces ve su texto, su escena, el estado pendiente y las acciones de aceptar y de pedir un cambio con hint.
  - Dada una página pendiente con varias versiones en su histórico, cuando el usuario la visualiza, entonces ve todas las versiones con el aviso de cada una, distingue cuál es la vigente y dispone de la acción de marcar como vigente cualquier otra.
  - Dada una página pendiente cuya versión vigente tiene aviso por exceso de palabras, cuando el usuario la visualiza, entonces ve el aviso y las acciones de aceptar y de pedir un cambio con hint.
  - Dada una página pendiente cuya versión vigente tiene aviso por número de frases, cuando el usuario la visualiza, entonces ve el aviso, la acción de pedir un cambio con hint y la de regenerar sin hint, y no ve la acción de aceptar.
  - Dada una página aceptada sin propuestas pendientes, cuando el usuario la visualiza, entonces ve su texto, su escena, el estado aceptada y la acción de pedir un cambio con hint, y no ve ningún histórico de versiones.
  - Dada una propuesta pendiente sobre una página reabierta, cuando el usuario la visualiza, entonces ve la versión aceptada separada del histórico de propuestas y dispone además de la acción de descartar las propuestas.
  - Dado un cuento cuya última página existente está aceptada y no es la última de la escaleta, cuando el usuario visualiza las páginas, entonces dispone de la acción de generar la página siguiente.

### REQ-023 Cuentos antiguos sin escaleta

- **Requisito:** El sistema deberá conservar el comportamiento actual de generación de páginas en los cuentos creados sin escaleta.
- **Criterios de aceptación:**
  - Dado un cuento creado con el formato anterior, cuando el usuario trabaja con sus páginas, entonces el sistema se comporta como antes de esta feature.
  - Dado un cuento creado con escaleta, cuando el usuario trabaja con sus páginas, entonces aplican los requisitos de esta feature.

### REQ-024 Modelfile del escritor en el servidor

- **Requisito:** El sistema deberá mantener el Modelfile del agente escritor desde el backend, dejando de usar la carpeta `agents/` como ubicación de mantenimiento, con el mismo patrón que el director.
- **Criterios de aceptación:**
  - Dado un despliegue del backend, cuando se consulta la configuración del agente escritor, entonces su Modelfile se carga o aprovisiona desde recursos gestionados por el backend.
  - Dado el repositorio tras esta feature, cuando se revisa la ubicación del Modelfile del escritor, entonces ya no reside en `agents/`.

### REQ-025 Regeneración automática por incumplimiento de longitud

- **Requisito:** Si el texto narrativo generado para una página incumple el límite de longitud de REQ-007, entonces el sistema deberá pedir otra generación al escritor sin intervención del usuario, hasta un máximo de 3 generaciones en total para esa solicitud, contando la primera; si una de ellas cumple el límite, solo esa entra en el histórico.
- **Criterios de aceptación:**
  - Dada una solicitud de generar la página 3, cuando la primera generación tiene 70 palabras y la segunda tiene 55 palabras y 3 frases, entonces el sistema presenta la segunda al usuario para su revisión sin aviso, no hay tercera generación y el histórico de la página 3 contiene solo la segunda.
  - Dada una solicitud de generar la página 3, cuando las 3 generaciones incumplen el límite, entonces el sistema no pide una cuarta generación.
  - Dada una solicitud de cambio con hint, de regeneración sin hint o de reapertura de una página, cuando la versión generada incumple el límite, entonces el sistema aplica las mismas regeneraciones automáticas.
  - Dada una regeneración automática en curso, cuando el usuario consulta la página, entonces no se le presenta ninguna de las versiones de esa solicitud hasta que terminan las regeneraciones.

### REQ-026 Versiones con aviso tras agotar las generaciones

- **Requisito:** Si tras las 3 generaciones de REQ-025 el texto narrativo sigue incumpliendo el límite de longitud, entonces el sistema deberá añadir las 3 versiones al histórico, cada una con un aviso que indique qué límite incumple (exceso de palabras, número de frases fuera de 1 a 4, o ambos), y dejar vigente la última generada.
- **Criterios de aceptación:**
  - Dada una página cuyas 3 generaciones superan 66 palabras con 1 a 4 frases, cuando terminan, entonces el histórico contiene las 3 versiones con aviso de exceso de palabras y la vigente es la tercera.
  - Dada una página cuyas 3 generaciones tienen 5 frases, cuando terminan, entonces la página se muestra pendiente de aceptación con las 3 versiones con aviso de número de frases.
  - Dada una página cuya primera generación tiene 5 frases y la segunda y tercera superan 66 palabras con 1 a 4 frases, cuando terminan, entonces la primera muestra aviso de número de frases, la segunda y la tercera muestran aviso de exceso de palabras y la vigente es la tercera.
  - Dadas las 3 versiones con aviso anteriores, cuando el usuario marca como vigente la primera, entonces la vigente pasa a ser la primera.

### REQ-027 Aceptar una versión con aviso de exceso de palabras

- **Requisito:** Cuando el usuario acepta una página pendiente cuya versión vigente tiene como único aviso el de exceso de palabras, el sistema deberá aceptarla tal cual, igual que una versión sin aviso.
- **Criterios de aceptación:**
  - Dada la página 3 pendiente cuya versión vigente tiene aviso de exceso de palabras, cuando el usuario la acepta, entonces la página 3 queda aceptada con el texto de esa versión sin cambios y se habilita la acción de generar la página 4.
  - Dada la página 3 pendiente cuya versión vigente tiene aviso de exceso de palabras, cuando el usuario envía un hint, entonces el sistema genera una nueva versión según REQ-012.

### REQ-028 Aceptación bloqueada por número de frases

- **Requisito:** Si la versión vigente de una página pendiente tiene aviso de número de frases fuera de 1 a 4, entonces el sistema deberá rechazar su aceptación e indicar que hay que elegir otra versión válida, pedir un cambio con hint o regenerarla sin hint.
- **Criterios de aceptación:**
  - Dada la página 3 pendiente cuya versión vigente tiene aviso de número de frases, cuando se solicita aceptarla, entonces el sistema rechaza la operación, indica el motivo y la página 3 sigue pendiente.
  - Dada una página pendiente cuya versión vigente tiene avisos de número de frases y de exceso de palabras, cuando se solicita aceptarla, entonces el sistema rechaza la operación.
  - Dada la página 3 pendiente cuya versión vigente tiene aviso de número de frases y otra versión del histórico sin aviso de número de frases, cuando el usuario marca esa otra versión como vigente, entonces se habilita la acción de aceptar.
  - Dada la página 3 pendiente cuya versión vigente tiene aviso de número de frases, cuando se solicita generar la página 4, entonces el sistema rechaza la operación según REQ-010.

### REQ-029 Regenerar sin hint una página bloqueada

- **Requisito:** Cuando el usuario solicita regenerar sin hint una página pendiente cuya versión vigente tiene aviso de número de frases, el sistema deberá pedir al escritor una nueva versión completa de esa página a partir de su beat, aplicando REQ-025 y REQ-026.
- **Criterios de aceptación:**
  - Dada la página 3 pendiente cuya versión vigente tiene aviso de número de frases, cuando el usuario solicita regenerarla sin hint, entonces el sistema añade al histórico una nueva versión de la página 3 pendiente de aceptación.
  - Dada una página pendiente cuya versión vigente no tiene aviso o tiene aviso solo de exceso de palabras, cuando el usuario la visualiza, entonces no dispone de la acción de regenerar sin hint, aunque otra versión de su histórico tenga aviso de número de frases.

### REQ-030 Descartar las propuestas de una reapertura

- **Requisito:** Cuando el usuario descarta las propuestas de una portada o página reabierta, el sistema deberá eliminar todo el histórico de propuestas de esa reapertura y conservar la versión aceptada y todas las páginas posteriores.
- **Criterios de aceptación:**
  - Dado un cuento con las páginas 1 a 4 aceptadas y tres propuestas pendientes para la página 2, cuando el usuario descarta las propuestas, entonces la página 2 muestra su versión aceptada anterior, no tiene histórico de propuestas y las páginas 3 y 4 permanecen aceptadas.
  - Dada una página reabierta cuya propuesta vigente tiene aviso de número de frases, cuando el usuario descarta las propuestas, entonces la página conserva su versión aceptada.
  - Dada la página 2 aceptada con imagen importada y con propuestas pendientes, cuando el usuario descarta las propuestas, entonces la página 2 muestra su versión aceptada con su imagen.

### REQ-031 Reabrir la portada aceptada

- **Requisito:** Cuando el usuario envía un hint sobre la portada aceptada y después acepta la propuesta vigente, el sistema deberá sustituir la portada aceptada por esa propuesta sin eliminar ninguna página.
- **Criterios de aceptación:**
  - Dado un cuento con la portada y las páginas 1 a 3 aceptadas, cuando el usuario envía un hint sobre la portada, entonces el sistema muestra una nueva versión de la portada pendiente de aceptación.
  - Dada la propuesta anterior, cuando el usuario la acepta, entonces la portada queda aceptada con la nueva versión, las páginas 1 a 3 siguen aceptadas y no se muestra aviso de descarte.
  - Dada la propuesta anterior, cuando el usuario descarta las propuestas, entonces la portada conserva su versión aceptada.
  - Dada la portada aceptada con imagen importada y una propuesta pendiente, cuando el usuario acepta la propuesta, entonces la portada queda aceptada con la nueva versión y sin imagen.
  - Dada la portada aceptada con imagen importada y una propuesta pendiente, cuando el usuario descarta las propuestas, entonces la portada muestra su versión aceptada con su imagen.

### REQ-032 Una sola generación a la vez en toda la aplicación

- **Requisito:** Mientras haya una generación en curso de cualquier tipo en la aplicación (portada o página del escritor, incluidas sus regeneraciones automáticas; guion o revisión del director; agente de personaje; retratos de ComfyUI; temas), el sistema deberá rechazar cualquier otra solicitud de generación e indicar que hay una generación en curso.
- **Criterios de aceptación:**
  - Dada la generación de la página 3 de un cuento en curso, cuando el usuario solicita generar la portada de otro cuento, entonces el sistema rechaza la solicitud e indica que hay una generación en curso.
  - Dada la generación de retratos de un protagonista en curso, cuando el usuario solicita generar una página, entonces el sistema rechaza la solicitud e indica que hay una generación en curso.
  - Dada la generación de un guion en curso, cuando el usuario solicita al agente de personaje un cambio de ficha o pide temas, entonces el sistema rechaza la solicitud e indica que hay una generación en curso.
  - Dada una página en su segunda generación automática según REQ-025, cuando el usuario solicita cualquier otra generación, entonces el sistema la rechaza, porque las regeneraciones automáticas forman parte de la misma generación.
  - Dada una generación que acaba de terminar con éxito o con fallo, cuando el usuario solicita otra generación, entonces el sistema la inicia.

### REQ-033 Sin aceptar, descartar ni elegir versión durante una generación del cuento

- **Requisito:** Mientras se genera la portada o una página de un cuento, el sistema deberá rechazar en ese cuento las solicitudes de aceptar, de descartar propuestas y de marcar una versión como vigente.
- **Criterios de aceptación:**
  - Dada la página 3 pendiente de aceptación y una generación con hint de la página 3 en curso, cuando el usuario solicita aceptar la página 3, entonces el sistema rechaza la operación e indica que hay una generación en curso.
  - Dada una propuesta pendiente sobre la página 2 reabierta y una generación con hint de esa propuesta en curso, cuando el usuario solicita descartarla, entonces el sistema rechaza la operación.
  - Dada la página 3 con dos versiones en su histórico, la segunda vigente y una generación con hint de la página 3 en curso, cuando el usuario solicita marcar como vigente la primera, entonces el sistema rechaza la operación, indica que hay una generación en curso y la vigente sigue siendo la segunda.
  - Dada una generación de un cuento que acaba de terminar, cuando el usuario solicita marcar como vigente otra versión del histórico, entonces el sistema la marca como vigente según REQ-036.

### REQ-034 Histórico de versiones de una portada o página pendiente

- **Requisito:** Cuando se presenta al usuario una versión de la portada o de una página pendiente de aceptación (generación inicial, cambio con hint o regeneración sin hint), el sistema deberá añadirla al histórico de versiones de esa portada o página, con su aviso de longitud si lo tiene, y marcarla como vigente.
- **Criterios de aceptación:**
  - Dada una solicitud de generar la página 3 cuya primera generación cumple el límite, cuando termina, entonces el histórico de la página 3 contiene una versión y es la vigente.
  - Dada la página 3 con una versión en su histórico, cuando el usuario envía un hint y la nueva versión cumple el límite, entonces el histórico contiene dos versiones y la vigente es la nueva.
  - Dada la página 3 pendiente cuya versión vigente tiene aviso de número de frases, cuando el usuario la regenera sin hint y la nueva versión cumple el límite, entonces el histórico gana una versión sin aviso, que pasa a ser la vigente.
  - Dada la portada pendiente con una versión, cuando el usuario envía un hint, entonces el histórico de la portada contiene dos versiones.

### REQ-035 Hint rechazado o fallo sin nueva versión

- **Requisito:** Si un hint es rechazado (REQ-014) o una generación falla (REQ-020), entonces el sistema no deberá añadir ninguna versión al histórico ni cambiar la versión vigente.
- **Criterios de aceptación:**
  - Dada la página 3 con dos versiones en su histórico y la segunda vigente, cuando el escritor rechaza un hint, entonces el histórico sigue con dos versiones y la vigente sigue siendo la segunda.
  - Dada la página 3 con dos versiones en su histórico, cuando falla la generación pedida con un hint, entonces el histórico sigue con dos versiones.

### REQ-036 Elegir la versión vigente

- **Requisito:** Cuando el usuario marca como vigente una versión del histórico de la portada o de una página pendiente, el sistema deberá hacerla vigente y mostrar su texto narrativo, su descripción de escena y su aviso como los de esa portada o página.
- **Criterios de aceptación:**
  - Dada la página 3 con tres versiones y la tercera vigente, cuando el usuario marca como vigente la primera, entonces la página 3 muestra el texto, la escena y el aviso de la primera, y el histórico conserva las tres versiones.
  - Dada la página 3 con la primera versión marcada como vigente, cuando el usuario la acepta, entonces la página 3 queda aceptada con el texto de la primera versión.

### REQ-037 Aceptar la versión vigente y eliminar el histórico

- **Requisito:** Cuando el usuario acepta la portada o una página pendiente, el sistema deberá aceptar su versión vigente y eliminar el resto de versiones de su histórico.
- **Criterios de aceptación:**
  - Dada la página 3 con tres versiones y la segunda vigente, cuando el usuario la acepta, entonces la página 3 queda aceptada con la segunda versión y ya no se muestra ningún histórico de versiones.
  - Dada la página 3 aceptada tras tener tres versiones, cuando se genera la página 4, entonces el escritor recibe como texto de la página 3 el de la versión aceptada.
  - Dada la página 3 pendiente de aceptación, cuando el usuario la acepta, entonces la página 3 queda aceptada sin imagen.

### REQ-038 Histórico de propuestas de una reapertura

- **Requisito:** Mientras la portada o una página aceptada tiene propuestas pendientes por una reapertura, el sistema deberá mantener esas propuestas en un histórico propio y conservar la versión aceptada fuera de él, sin cambios, hasta que se acepte una propuesta o se descarten.
- **Criterios de aceptación:**
  - Dada la página 2 aceptada y reabierta con dos hints, cuando el usuario la visualiza, entonces ve dos propuestas en el histórico y la versión aceptada aparte, con su texto original.
  - Dada la página 2 reabierta con dos propuestas, cuando el usuario quiere marcar como vigente una versión, entonces solo puede elegir entre las dos propuestas y no la versión aceptada.
  - Dada la página 2 reabierta con dos propuestas, cuando el usuario acepta la propuesta vigente, entonces la página 2 queda aceptada con esa propuesta y no se muestra ningún histórico.
  - Dada la página 2 aceptada con imagen importada y reabierta con dos propuestas, cuando el usuario la visualiza, entonces la versión aceptada se muestra con su imagen y las dos propuestas se muestran sin imagen.

### REQ-039 Sin límite de versiones en el histórico

- **Requisito:** El sistema no deberá limitar el número de versiones que puede contener el histórico de una portada o página pendiente ni el de las propuestas de una reapertura.
- **Criterios de aceptación:**
  - Dada la página 3 pendiente con una versión, cuando el usuario envía 10 hints aceptados por el escritor y generados sin fallo, entonces el histórico contiene 11 versiones y todas se pueden marcar como vigentes.

### REQ-040 Imagen importada solo en la versión aceptada

- **Requisito:** El sistema deberá mostrar imagen importada únicamente en la versión aceptada de la portada o de una página de un cuento con escaleta; las versiones pendientes de aceptación y las propuestas de una reapertura se mostrarán siempre sin imagen.
- **Criterios de aceptación:**
  - Dada la página 3 pendiente con varias versiones en su histórico, cuando el usuario la visualiza, entonces ninguna de sus versiones muestra imagen.
  - Dada la portada aceptada con imagen importada y reabierta con una propuesta, cuando el usuario la visualiza, entonces solo la versión aceptada muestra imagen.

### REQ-041 Versión recién aceptada sin imagen

- **Requisito:** Cuando el usuario acepta una versión de la portada o de una página, ya sea pendiente de aceptación o propuesta de una reapertura, el sistema deberá dejar la nueva versión aceptada sin imagen y dejar de mostrar la imagen de la versión aceptada anterior.
- **Criterios de aceptación:**
  - Dada la página 2 aceptada con imagen importada y una propuesta pendiente, cuando el usuario acepta la propuesta, entonces la página 2 muestra la nueva versión sin imagen y la imagen anterior deja de mostrarse.
  - Dada la portada pendiente de aceptación, cuando el usuario la acepta, entonces la portada queda aceptada sin imagen.

## Requisitos no funcionales

Ninguno.

## Supuestos

### SUP-001 Generación de la página siguiente bajo demanda

- **Tipo:** decisional
- **Descripción:** Aceptar la portada o una página no lanza la generación de la siguiente: el usuario la solicita con una acción explícita, igual que la regeneración de páginas descartadas.
- **Impacto:** REQ-003, REQ-010, REQ-011, REQ-022.
- **Estado:** validado

### SUP-002 Validación automática del límite de longitud

- **Tipo:** decisional
- **Descripción:** El sistema cuenta las frases y el total de palabras del texto narrativo. El número de frases (1 a 4) es estricto; el total de palabras tiene un límite de 60 con tolerancia del 10 % (hasta 66) y no hay límite por frase. Si el texto cumple, se presenta para revisión; si no, se regenera automáticamente hasta 3 generaciones en total contando la primera, y si sigue incumpliendo se presenta la página pendiente con aviso, sin descartarla. La ausencia de subordinadas encadenadas (REQ-008) y el vocabulario (REQ-009) no se validan automáticamente: los garantizan las instrucciones del escritor y la revisión del adulto.
- **Impacto:** REQ-007, REQ-008, REQ-009, REQ-020, REQ-025, REQ-026, REQ-027, REQ-028.
- **Estado:** validado

### SUP-003 Descartar la propuesta de una página reabierta

- **Tipo:** decisional
- **Descripción:** Mientras hay una propuesta pendiente sobre una página reabierta, el usuario puede descartarla; la página conserva la versión aceptada y las páginas posteriores no se tocan. Recogido en REQ-030.
- **Impacto:** REQ-015, REQ-016, REQ-017, REQ-030.
- **Estado:** validado

### SUP-004 Reabrir la portada aceptada

- **Tipo:** decisional
- **Descripción:** La portada aceptada también se puede reabrir con hint. Aceptar su nueva versión no elimina ninguna página, porque el escritor no usa la portada como contexto de las páginas (REQ-004). Recogido en REQ-031.
- **Impacto:** REQ-001, REQ-011, REQ-012, REQ-016, REQ-031.
- **Estado:** validado

### SUP-005 Una sola generación a la vez en toda la aplicación

- **Tipo:** decisional
- **Descripción:** Mientras hay una generación en curso de cualquier tipo (escritor, director, agente de personaje, retratos de ComfyUI o temas), el sistema rechaza cualquier otra generación e indica que hay una en curso; además, mientras se genera la portada o una página de un cuento, se rechazan en ese cuento la aceptación, el descarte de propuestas y la elección de versión vigente (SUP-011). Motivo: los modelos locales comparten una única GPU y el rendimiento con procesos en paralelo es incierto. Recogido en REQ-032 y REQ-033.
- **Impacto:** REQ-003, REQ-011, REQ-012, REQ-015, REQ-032, REQ-033; también funcionalidades de agent-director, del agente de personaje, de los retratos y de los temas.
- **Estado:** validado

### SUP-006 Notificación en tiempo real existente

- **Tipo:** contextual
- **Descripción:** El resultado de cada generación llega al usuario con el mecanismo de notificación en tiempo real que el producto ya usa, sin recargar la vista.
- **Impacto:** REQ-022.
- **Estado:** pendiente

### SUP-007 Política de assets de las páginas descartadas

- **Tipo:** contextual
- **Descripción:** Las páginas eliminadas por un descarte en cascada siguen las reglas de gestión de assets del producto, igual que las páginas invalidadas en agent-director (SUP-005 de esa feature); esta feature no define la eliminación física. Descartado por el usuario: la gestión de assets de escena sale del alcance; la imagen importada desaparece del cuento con su página (REQ-016).
- **Impacto:** REQ-016.
- **Estado:** descartado

### SUP-008 Interacción con la edición del guion

- **Tipo:** contextual
- **Descripción:** Si el guion vuelve a pendiente de aprobación por una edición directa de beat, se aplican REQ-011 y REQ-012 de agent-director. Cuando se vuelve a aprobar, la generación se retoma en la primera página sin versión aceptada, de acuerdo con REQ-002 y REQ-010.
- **Impacto:** REQ-002, REQ-010.
- **Estado:** pendiente

### SUP-009 Versión presentada tras agotar las generaciones

- **Tipo:** decisional
- **Descripción:** Cuando las 3 generaciones incumplen el límite de longitud, se presenta la última versión generada, no la que más se acerque al límite. Descartado por el usuario: se sustituye por el histórico de versiones, en el que entran las 3 versiones con su aviso, queda vigente la última y el usuario elige (REQ-026, REQ-034, REQ-036).
- **Impacto:** REQ-026.
- **Estado:** descartado

### SUP-010 Regeneración sin hint limitada a la página bloqueada

- **Tipo:** decisional
- **Descripción:** Cuando la aceptación está bloqueada por número de frases en la versión vigente, además del cambio con hint y de elegir otra versión válida, el usuario puede regenerar la página sin hint. Es la única excepción a la exclusión de la regeneración sin hint.
- **Impacto:** REQ-022, REQ-028, REQ-029, Fuera de alcance.
- **Estado:** validado

### SUP-011 Elegir versión vigente durante una generación del cuento

- **Tipo:** decisional
- **Descripción:** Mientras se genera la portada o una página de un cuento, el usuario no puede marcar una versión como vigente en ese cuento, del mismo modo que no puede aceptar ni descartar (REQ-033), para que un hint en curso no quede aplicado sobre una versión que ya no es la vigente. Recogido en REQ-033.
- **Impacto:** REQ-012, REQ-033, REQ-036.
- **Estado:** validado

## Preguntas abiertas

- ¿Qué indicación concreta muestra el cuento al quedar listo para la etapa de audio (REQ-021)? Se precisará al especificar la etapa de audio.
