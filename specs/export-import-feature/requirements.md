# Requisitos: export-import-feature

Estado: aprobado
Revisión: 7

## Contexto

El adulto necesita ilustrar la portada y las páginas del cuento con una herramienta externa e importar esas imágenes (FUN-014), y después llevar el cuento a dos aplicaciones externas sin trabajo manual: Nubi Game, con texto, imágenes y audio por página (FUN-018, PDEC-040), y el agente de recursos visuales, con el `story.json` del contrato `docs/contracts/agent-visual/input.md` v0.1 (FUN-019, PDEC-041).

## Fuera de alcance

- Generar las ilustraciones de página con ComfyUI o con cualquier agente de la aplicación; la ilustración se hace fuera (FUN-014).
- Validar en la importación la resolución, la proporción, la transparencia, el formato concreto o el peso de la imagen (PDEC-042).
- Completar los guiones de cuentos ya existentes con el lugar de escena de sus beats ni con el lugar y el tono propios de la portada; esos cuentos no son exportables ni para el agente visual ni para Nubi Game (SUP-021).
- Generar `setting` o `emotion` en el momento de exportar (PDEC-041).
- Paleta propia en la exportación para el agente visual; siempre se usa la paleta base.
- Incluir personajes secundarios en la exportación para el agente visual; solo se exporta el protagonista.
- Que el adulto suba la imagen del personaje con fondo transparente.
- Audio de la portada.
- Convertir de formato, redimensionar, reorientar, recomprimir o quitar metadatos a las imágenes de portada y de páginas al exportarlas para Nubi Game; el zip las incluye tal como se subieron y la aplicación consumidora se encarga del formato (SUP-007).
- Comprobar en la exportación para Nubi Game que Nubi Game admite el formato de cada imagen importada (SUP-007).
- Enviar los ficheros directamente a Nubi Game o al agente visual, o invocar su validador; la aplicación solo genera el fichero exportado.
- Quitar una imagen importada de la portada o de una página sin sustituirla por otra.
- Cambiar el comportamiento actual de la narración cuando una página cambia (SUP-020).

## Requisitos funcionales

### REQ-001 Descripción de escena visible en cada página interior

- **Requisito:** Mientras una página interior tenga contenido aceptado, la aplicación deberá mostrar al adulto su descripción de escena para que pueda usarla en la herramienta externa de ilustración.
- **Criterios de aceptación:**
  - Dado un cuento con la página 2 aceptada, cuando el adulto abre la página 2, entonces ve su descripción de escena completa.

### REQ-002 Subida de imagen en la portada

- **Requisito:** La aplicación deberá ofrecer en la portada una opción para subir una imagen.
- **Criterios de aceptación:**
  - Dado un cuento con la portada aceptada (SUP-005), cuando el adulto abre la portada, entonces dispone de la opción de subir imagen.
  - Dado un cuento con guion aprobado y la portada sin aceptar, cuando el adulto abre la portada, entonces no dispone de la opción de subir imagen.
  - Dado que el adulto sube una imagen en la portada, cuando la subida termina, entonces la portada muestra esa imagen.

### REQ-003 Subida de imagen en cada página interior

- **Requisito:** La aplicación deberá ofrecer en cada página interior una opción propia para subir una imagen.
- **Criterios de aceptación:**
  - Dado un cuento con tres páginas interiores aceptadas (SUP-005), cuando el adulto sube una imagen en la página 3, entonces solo la página 3 muestra esa imagen y las páginas 1 y 2 no cambian.

### REQ-004 Aceptación de cualquier imagen

- **Requisito:** Cuando el adulto sube un fichero que es una imagen, la aplicación deberá aceptarlo con independencia de su formato de imagen y de su tamaño.
- **Criterios de aceptación:**
  - Dado un fichero PNG de 300 × 200 px, cuando el adulto lo sube en una página, entonces la subida se acepta y la página muestra la imagen.
  - Dado un fichero JPEG de más de 20 MB, cuando el adulto lo sube en la portada, entonces la subida se acepta y la portada muestra la imagen.

### REQ-005 Rechazo de ficheros que no son imagen

- **Requisito:** Si el fichero subido no es una imagen, entonces la aplicación deberá rechazar la subida, indicar al adulto que el fichero no es una imagen y conservar la imagen anterior de la portada o página, si la había.
- **Criterios de aceptación:**
  - Dado una página con una imagen ya importada, cuando el adulto sube un fichero de texto, entonces ve un mensaje que indica que no es una imagen y la página sigue mostrando la imagen anterior.

### REQ-006 Sustitución de la imagen importada

- **Requisito:** Cuando el adulto sube una imagen en una portada o página que ya tiene imagen importada, la aplicación deberá sustituir la imagen anterior por la nueva.
- **Criterios de aceptación:**
  - Dado la página 1 con la imagen A, cuando el adulto sube la imagen B en la página 1, entonces la página 1 muestra B y A deja de estar asociada a la página.

### REQ-007 Conservación de la imagen con el contenido aceptado

- **Requisito:** La aplicación deberá conservar la imagen importada ligada al contenido aceptado de su portada o página mientras ese contenido no cambie.
- **Criterios de aceptación:**
  - Dado una página aceptada con imagen importada, cuando el adulto cierra la aplicación y vuelve a abrir el cuento, entonces la página sigue mostrando la misma imagen.
  - Dado una portada aceptada con imagen importada, cuando el adulto pide un cambio en la página 2 y acepta la nueva versión, entonces la portada sigue mostrando la misma imagen (SUP-006).

### REQ-008 Pérdida de la imagen al reaprobarse o descartarse la página

- **Requisito:** Cuando una página interior con imagen importada se reaprueba o se descarta por la cascada de PDEC-020, la aplicación deberá eliminar la asociación de esa imagen con la página.
- **Criterios de aceptación:**
  - Dado las páginas 2 y 3 con imagen importada, cuando el adulto pide un cambio en la página 2 y acepta la nueva versión, entonces ni la página 2 ni la página 3 muestran imagen importada.

### REQ-009 Disponibilidad de la exportación para Nubi Game

- **Requisito:** La aplicación deberá permitir exportar el cuento para Nubi Game solo cuando la portada esté aceptada, todas las páginas interiores estén aceptadas, la portada tenga imagen y cada página interior tenga imagen y audio.
- **Criterios de aceptación:**
  - Dado un cuento con la portada y todas las páginas interiores aceptadas, imagen en la portada y en cada página interior, y audio en cada página interior, cuando el adulto pide la exportación para Nubi Game, entonces obtiene el zip.
  - Dado un cuento completo salvo que la portada no tiene audio, cuando el adulto pide la exportación para Nubi Game, entonces obtiene el zip.

### REQ-010 Rechazo de la exportación para Nubi Game con el cuento incompleto

- **Requisito:** Si el adulto pide la exportación para Nubi Game con el cuento incompleto, entonces la aplicación deberá rechazarla sin generar el zip e indicar cada elemento que falta (portada sin aceptar, texto aceptado, imagen o audio) y en qué portada o página falta.
- **Criterios de aceptación:**
  - Dado un cuento al que le falta la imagen de la página 2 y el audio de la página 4, cuando el adulto pide la exportación para Nubi Game, entonces no se genera zip y el mensaje indica "imagen" en la página 2 y "audio" en la página 4.
  - Dado un cuento al que le falta la imagen de portada, cuando el adulto pide la exportación para Nubi Game, entonces no se genera zip y el mensaje indica que falta la imagen de la portada.
  - Dado un cuento cuya portada no está aceptada, cuando el adulto pide la exportación para Nubi Game, entonces no se genera zip y el mensaje indica "portada sin aceptar".

### REQ-011 Contenido del zip para Nubi Game

- **Requisito:** Cuando se genera la exportación para Nubi Game, la aplicación deberá incluir en el zip un `story.json`, la imagen de portada y, por cada página interior, su imagen y su audio, con cada imagen en un fichero cuya extensión corresponde a su formato real (SUP-022).
- **Criterios de aceptación:**
  - Dado un cuento completo de 5 páginas interiores, cuando el adulto abre el zip exportado, entonces encuentra un `story.json`, 1 imagen de portada, 5 imágenes de página y 5 audios, y ningún audio de portada.
  - Dado un cuento completo con la portada subida en PNG y la página 1 subida en JPEG, cuando el adulto abre el zip exportado, entonces la imagen de portada del zip está en PNG con extensión `.png` y la de la página 1 en JPEG con extensión `.jpg` (SUP-007, SUP-022).
  - Dado una página cuya imagen PNG se subió con un nombre de fichero terminado en `.jpg`, cuando el adulto abre el zip exportado, entonces el fichero de imagen de esa página tiene extensión `.png` (SUP-022).

### REQ-012 Cabecera del `story.json` para Nubi Game

- **Requisito:** El `story.json` de la exportación para Nubi Game deberá contener el título del cuento y la ruta de la imagen de portada (SUP-001).
- **Criterios de aceptación:**
  - Dado un cuento titulado "La luna dormida", cuando se inspecciona su `story.json` para Nubi Game, entonces contiene el título "La luna dormida" y una ruta que apunta a la imagen de portada incluida en el zip.

### REQ-013 Páginas del `story.json` para Nubi Game

- **Requisito:** El `story.json` de la exportación para Nubi Game deberá contener, por cada página interior y en orden de lectura, el texto aceptado de la página, la ruta de su imagen y la ruta de su audio.
- **Criterios de aceptación:**
  - Dado un cuento de 3 páginas interiores, cuando se inspecciona su `story.json` para Nubi Game, entonces contiene 3 entradas de página en el orden 1, 2, 3, cada una con su texto aceptado, la ruta de su imagen y la ruta de su audio.

### REQ-014 Referencias explícitas y resolubles

- **Requisito:** Cada ruta del `story.json` para Nubi Game deberá ser relativa a la raíz del zip y apuntar a un fichero incluido en el zip, de modo que el consumidor no necesite deducir ningún fichero por su nombre ni por su posición.
- **Criterios de aceptación:**
  - Dado un zip exportado para Nubi Game, cuando se resuelve cada ruta de su `story.json` desde la raíz del zip, entonces todas apuntan a un fichero existente dentro del zip.
  - Dado un zip exportado para Nubi Game, cuando se renombran todos los ficheros de imagen y audio y se actualizan sus rutas en el `story.json`, entonces cada página sigue asociada a su imagen y a su audio sin otra información.

### REQ-015 Imagen exportada para Nubi Game

- **Requisito:** La aplicación deberá exportar para Nubi Game, como imagen de la portada y de cada página interior, la imagen importada por el adulto en esa portada o página tal como se subió, sin convertir su formato ni modificar sus dimensiones (SUP-002, SUP-007).
- **Criterios de aceptación:**
  - Dado la página 2 con la imagen importada B, cuando se exporta el cuento para Nubi Game, entonces el fichero referenciado por la página 2 en el `story.json` es idéntico byte a byte a B.
  - Dado una página importada en BMP de 4096 × 3072 px, cuando se exporta el cuento para Nubi Game, entonces la imagen de esa página en el zip está en BMP y mide 4096 × 3072 px.

### REQ-016 Sustitución del zip anterior para Nubi Game

- **Requisito:** La aplicación deberá ofrecer como única exportación para Nubi Game el zip descrito en REQ-011 a REQ-015 (SUP-003).
- **Criterios de aceptación:**
  - Dado un cuento completo, cuando el adulto consulta las opciones de exportación, entonces solo encuentra una exportación para Nubi Game y su zip contiene un `story.json` con referencias a imágenes y audios.

### REQ-017 Lugar de la escena en cada beat y en la portada del guion

- **Requisito:** Cuando el director genera el guion de un cuento nuevo, la aplicación deberá incluir un lugar o ambiente de escena (`setting`) no vacío en cada beat de la escaleta y otro propio de la portada (SUP-011).
- **Criterios de aceptación:**
  - Dado un cuento nuevo de 4 páginas, cuando el director devuelve el guion, entonces los 4 beats tienen un lugar de escena no vacío.
  - Dado un cuento nuevo, cuando el director devuelve el guion, entonces la portada tiene un lugar de escena propio no vacío, registrado aparte del lugar del beat de la página 1.

### REQ-018 Disponibilidad de la exportación para el agente visual

- **Requisito:** La aplicación deberá permitir exportar el cuento para el agente visual solo cuando el guion esté aprobado, la portada esté aceptada, todas las páginas interiores estén aceptadas, todos sus beats tengan lugar de escena y la portada tenga lugar y tono propios, sin exigir imágenes ni audio.
- **Criterios de aceptación:**
  - Dado un cuento nuevo con guion aprobado, la portada y todas las páginas interiores aceptadas, sin imágenes importadas y sin audio, cuando el adulto pide la exportación para el agente visual, entonces obtiene el zip.

### REQ-019 Rechazo de la exportación para el agente visual sin las condiciones

- **Requisito:** Si el adulto pide la exportación para el agente visual y no se cumple alguna condición de REQ-018, entonces la aplicación deberá rechazarla sin generar el zip e indicar qué condición falta.
- **Criterios de aceptación:**
  - Dado un cuento existente cuyos beats no tienen lugar de escena, cuando el adulto pide la exportación para el agente visual, entonces no se genera zip y el mensaje indica que el guion no tiene el lugar de escena necesario.
  - Dado un cuento existente cuyos beats tienen lugar de escena pero cuya portada no tiene lugar ni tono propios, cuando el adulto pide la exportación para el agente visual, entonces no se genera zip y el mensaje indica que a la portada le faltan el lugar y el tono.
  - Dado un cuento con la página 3 sin aceptar, cuando el adulto pide la exportación para el agente visual, entonces no se genera zip y el mensaje indica que la página 3 no está aceptada.
  - Dado un cuento cuya portada no está aceptada, cuando el adulto pide la exportación para el agente visual, entonces no se genera zip y el mensaje indica "portada sin aceptar".

### REQ-020 Estructura del zip para el agente visual

- **Requisito:** Cuando se genera la exportación para el agente visual, la aplicación deberá producir un zip con `story.json` en la raíz y la imagen del protagonista en `characters/<id>/character.png`, donde `<id>` es el `id` del protagonista en el `story.json`.
- **Criterios de aceptación:**
  - Dado un cuento cuyo protagonista tiene `id` `nina`, cuando el adulto abre el zip exportado para el agente visual, entonces contiene `story.json` en la raíz y `characters/nina/character.png`.

### REQ-021 Campos fijos de la raíz del `story.json` visual

- **Requisito:** El `story.json` para el agente visual deberá tener `schemaVersion` `"0.1"`, `language` `"es"` y `ageRange` `"3-4"`.
- **Criterios de aceptación:**
  - Dado cualquier cuento exportado para el agente visual, cuando se inspecciona su `story.json`, entonces `schemaVersion` vale `"0.1"`, `language` vale `"es"` y `ageRange` vale `"3-4"`.

### REQ-022 Título e identificador del cuento

- **Requisito:** El `story.json` para el agente visual deberá tener como `title` el título del cuento y como `storyId` un identificador derivado del título que cumpla el patrón del contrato (minúsculas ASCII, dígitos y guiones; 3 a 40 caracteres) (SUP-009).
- **Criterios de aceptación:**
  - Dado un cuento titulado "La luna dormida", cuando se exporta para el agente visual, entonces `title` vale "La luna dormida" y `storyId` vale `la-luna-dormida`.
  - Dado el mismo cuento exportado dos veces sin cambios, cuando se comparan ambos `story.json`, entonces el `storyId` es idéntico.

### REQ-023 Paleta base

- **Requisito:** El `story.json` para el agente visual deberá declarar `style` con `paletteId` `"base"` y `paletteSource` `"base"`, sin el campo `colors`.
- **Criterios de aceptación:**
  - Dado cualquier cuento exportado para el agente visual, cuando se inspecciona `style`, entonces `paletteId` y `paletteSource` valen `"base"` y no existe `colors`.

### REQ-024 Protagonista como único personaje

- **Requisito:** El `story.json` para el agente visual deberá contener en `characters` un único personaje: el protagonista de la instantánea del cuento, con `role` `"protagonist"`, su nombre en `name`, `image` `characters/<id>/character.png` e `immutableTraits` con entre 1 y 5 rasgos tomados de la descripción visual de la instantánea (SUP-013).
- **Criterios de aceptación:**
  - Dado un cuento cuyo reparto episódico incluye un conejo blanco, cuando se exporta para el agente visual, entonces `characters` tiene un solo elemento, con `role` `"protagonist"` y el nombre del protagonista de la instantánea.
  - Dado cualquier cuento exportado para el agente visual, cuando se inspecciona `immutableTraits` del protagonista, entonces tiene entre 1 y 5 elementos no vacíos y cada uno aparece en la descripción visual de la instantánea.

### REQ-025 Imagen del protagonista con fondo transparente

- **Requisito:** Cuando se genera la exportación para el agente visual, la aplicación deberá obtener la imagen del protagonista a partir del retrato de la instantánea del cuento, eliminando su fondo de forma automática.
- **Criterios de aceptación:**
  - Dado un cuento cuyo protagonista tiene un retrato con fondo de color, cuando se exporta para el agente visual, entonces `character.png` muestra al protagonista del retrato con el fondo transparente.
  - Dado un protagonista editado en el catálogo después de crear el cuento, cuando se exporta ese cuento para el agente visual, entonces la imagen procede del retrato de la instantánea y no del retrato actual del catálogo.

### REQ-026 Rechazo por imagen del protagonista no conforme

- **Requisito:** Si la imagen del protagonista obtenida no cumple las reglas del contrato (PNG real, canal alfa con fondo transparente, al menos 1024 × 1024 px y proporción 1:1), entonces la aplicación deberá rechazar la exportación sin generar el zip e indicar cada regla incumplida.
- **Criterios de aceptación:**
  - Dado un retrato de 768 × 768 px en la instantánea, cuando el adulto pide la exportación para el agente visual, entonces no se genera zip y el mensaje indica que la imagen mide menos de 1024 × 1024 px.

### REQ-027 Escena de portada

- **Requisito:** El `story.json` para el agente visual deberá contener como primera escena una de `type` `"cover"` cuyo `text` es el título del cuento, cuya `visualDescription` es la descripción de escena aceptada de la portada (SUP-014, SUP-016), cuyo `setting` es el lugar de escena propio de la portada (REQ-017) y cuya `emotion` procede del tono propio de la portada (REQ-037), sin generar ninguno de los dos en el momento de exportar.
- **Criterios de aceptación:**
  - Dado un cuento titulado "La luna dormida" cuya portada tiene aceptada la descripción de escena "Nina mira la luna desde la ventana", lugar "habitación al anochecer" y tono de calma, cuando se exporta para el agente visual, entonces la primera escena tiene `type` `"cover"`, `text` "La luna dormida", `visualDescription` "Nina mira la luna desde la ventana", `setting` "habitación al anochecer" y `emotion` derivada de la calma.
  - Dado un cuento cuya portada y página 1 tienen descripciones de escena distintas, cuando se exporta para el agente visual, entonces la `visualDescription` de la escena `cover` es la de la portada y no la de la página 1.
  - Dado un cuento cuya portada tiene lugar "habitación al anochecer" y tono de calma y cuyo beat de la página 1 tiene lugar "jardín de noche" y tono de curiosidad, cuando se exporta para el agente visual, entonces la escena `cover` tiene `setting` "habitación al anochecer" y `emotion` derivada de la calma, no los de la página 1.

### REQ-028 Una escena por página interior

- **Requisito:** El `story.json` para el agente visual deberá contener, tras la portada y en orden de lectura, una escena de `type` `"scene"` por cada página interior, con el texto aceptado de la página en `text` y su descripción de escena en `visualDescription` (SUP-014).
- **Criterios de aceptación:**
  - Dado un cuento de 4 páginas interiores, cuando se exporta para el agente visual, entonces `scenes` tiene 5 elementos: la portada y 4 escenas `scene` en el orden 1 a 4, cada una con el texto y la descripción de escena aceptados de su página.

### REQ-029 Lugar y emoción de cada escena interior

- **Requisito:** La aplicación deberá tomar el `setting` de cada escena de `type` `"scene"` del lugar de escena de su beat y la `emotion` del tono de ese beat, sin generar ninguno de los dos en el momento de exportar.
- **Criterios de aceptación:**
  - Dado la página 2 con un beat de lugar "cocina de la abuela" y tono alegre, cuando se exporta para el agente visual, entonces la escena de la página 2 tiene `setting` "cocina de la abuela" y una `emotion` que corresponde a la alegría.

### REQ-030 Personajes de cada escena

- **Requisito:** Cada escena del `story.json` para el agente visual deberá referenciar en `characters` solo `id` existentes en `characters` de la raíz (SUP-008).
- **Criterios de aceptación:**
  - Dado cualquier cuento exportado para el agente visual, cuando se comparan los `characters` de cada escena con los `id` de la raíz, entonces no hay ninguna referencia a un `id` inexistente.

### REQ-031 Conformidad del `story.json` visual con el contrato v0.1

- **Requisito:** El `story.json` para el agente visual deberá estar codificado en UTF-8 sin BOM, ser JSON válido sin claves duplicadas y cumplir todas las reglas de validación de `docs/contracts/agent-visual/input.md` v0.1, con los `id` de personajes y escenas únicos y en formato slug.
- **Criterios de aceptación:**
  - Dado un zip exportado para el agente visual, cuando se valida su contenido con las reglas de la sección 5 del contrato, tomando la raíz del zip como `input/`, entonces la validación no devuelve ningún error.

### REQ-032 Rechazo si el `story.json` visual no cumple el contrato

- **Requisito:** Si el `story.json` resultante incumple alguna regla del contrato v0.1, entonces la aplicación deberá rechazar la exportación sin generar el zip e indicar todas las reglas incumplidas.
- **Criterios de aceptación:**
  - Dado un cuento cuyo título no permite derivar un `storyId` de al menos 3 caracteres, cuando el adulto pide la exportación para el agente visual, entonces no se genera zip y el mensaje indica que el identificador del cuento no cumple el patrón.
  - Dado un cuento con la portada aceptada cuya descripción de escena está vacía, cuando el adulto pide la exportación para el agente visual, entonces no se genera zip y el mensaje indica que la descripción visual de la escena de portada está vacía.

### REQ-033 Formato de imagen optimizado para móvil en la exportación para Nubi Game (retirado)

- **Retirado en la revisión 6.** El usuario decide que el zip para Nubi Game lleva las imágenes originales sin convertir (SUP-007 validado, SUP-015 descartado); lo sustituye la nueva redacción de REQ-011 y REQ-015. El ID no se reutiliza.

### REQ-034 Dimensiones de imagen limitadas en la exportación para Nubi Game (retirado)

- **Retirado en la revisión 6.** Las imágenes no se redimensionan al exportarlas para Nubi Game (SUP-007); lo sustituye la nueva redacción de REQ-015. El ID no se reutiliza.

### REQ-035 Rechazo de la exportación para Nubi Game por imagen no convertible (retirado)

- **Retirado en la revisión 6.** Al no haber conversión (SUP-007), no existe el caso de imagen no convertible; la exportación para Nubi Game solo se rechaza por las causas de REQ-010. El ID no se reutiliza.

### REQ-036 Descripción de escena visible en la portada

- **Requisito:** Mientras la portada tenga contenido aceptado, la aplicación deberá mostrar al adulto en la portada su propia descripción de escena para que pueda usarla en la herramienta externa de ilustración (SUP-016).
- **Criterios de aceptación:**
  - Dado un cuento con la portada aceptada cuya portada y página 1 tienen descripciones de escena distintas, cuando el adulto abre la portada, entonces ve la descripción de escena de la portada completa y no la de la página 1.

### REQ-037 Tono propio de la portada en el guion

- **Requisito:** Cuando el director genera el guion de un cuento nuevo, la aplicación deberá incluir un tono propio de la portada, no vacío, del que se derive la emoción dominante de la portada (SUP-011).
- **Criterios de aceptación:**
  - Dado un cuento nuevo, cuando el director devuelve el guion, entonces la portada tiene un tono propio no vacío, registrado aparte del tono del beat de la página 1.

### REQ-038 Redacción de la descripción de escena de la portada

- **Requisito:** Cuando el adulto aprueba el guion de un cuento, la aplicación deberá hacer que el escritor redacte la descripción de escena de la portada a partir del lugar y el tono propios de la portada (REQ-017, REQ-037), antes de generar la página 1.
- **Criterios de aceptación:**
  - Dado un cuento nuevo cuyo guion tiene para la portada el lugar "habitación al anochecer" y el tono de calma, cuando el adulto aprueba el guion, entonces la portada presenta al adulto una descripción de escena no vacía pendiente de revisión y la página 1 todavía no existe.

### REQ-039 Revisión de la descripción de escena de la portada

- **Requisito:** Cuando el escritor entrega una descripción de escena de la portada, la aplicación deberá permitir al adulto aceptarla o pedir un cambio con un hint al escritor, que la regenera completa, con el mismo procedimiento que las páginas interiores (PDEC-021).
- **Criterios de aceptación:**
  - Dado una descripción de escena de portada pendiente de revisión, cuando el adulto la acepta, entonces la portada queda aceptada y muestra esa descripción.
  - Dado una descripción de escena de portada pendiente de revisión, cuando el adulto pide un cambio con el hint "que se vea la luna llena", entonces el escritor entrega una nueva descripción completa de la portada pendiente de revisión.
  - Dado una portada aceptada, cuando el adulto pide un cambio con un hint, entonces el escritor entrega una nueva descripción de escena de la portada pendiente de revisión.

### REQ-040 Portada aceptada antes de la página 1

- **Requisito:** Mientras la portada de un cuento con guion aprobado no esté aceptada por primera vez, la aplicación deberá impedir la generación de la página 1 (PDEC-022, con la portada como página 0).
- **Criterios de aceptación:**
  - Dado un cuento con guion aprobado y la descripción de escena de la portada pendiente de revisión, cuando el adulto consulta el cuento, entonces la página 1 no se ha generado ni se puede generar.
  - Dado un cuento con guion aprobado, cuando el adulto acepta la portada, entonces la aplicación pasa a generar la página 1.

### REQ-041 El cambio de la portada no descarta las páginas interiores

- **Requisito:** Cuando el adulto acepta una nueva versión de la descripción de escena de la portada, la aplicación deberá conservar aceptadas las páginas interiores ya aceptadas, con su texto, su descripción de escena, su imagen importada y su audio.
- **Criterios de aceptación:**
  - Dado un cuento con la portada y las páginas 1 a 3 aceptadas y la página 2 con imagen importada, cuando el adulto pide un cambio en la portada y acepta la nueva versión, entonces las páginas 1 a 3 siguen aceptadas con el mismo texto y la página 2 sigue mostrando su imagen.

### REQ-042 Rehacer el guion descarta la descripción de la portada

- **Requisito:** Cuando el guion de un cuento se rehace, la aplicación deberá descartar la descripción de escena de la portada, de modo que la portada deje de estar aceptada hasta que se acepte una nueva según REQ-038 y REQ-039.
- **Criterios de aceptación:**
  - Dado un cuento con la portada aceptada, cuando se rehace el guion y el adulto aprueba el nuevo guion, entonces la portada muestra una nueva descripción de escena pendiente de revisión y no la anterior.

### REQ-043 Pérdida de la imagen de portada

- **Requisito:** Cuando la portada con imagen importada se reaprueba o el guion del cuento se rehace, la aplicación deberá eliminar la asociación de esa imagen con la portada (SUP-006).
- **Criterios de aceptación:**
  - Dado una portada aceptada con imagen importada, cuando el adulto pide un cambio en la portada y acepta la nueva versión, entonces la portada no muestra imagen importada.
  - Dado una portada aceptada con imagen importada, cuando se rehace el guion, entonces la portada no muestra imagen importada.

## Requisitos no funcionales

Ninguno.

## Supuestos

### SUP-001 El `story.json` para Nubi Game referencia la portada

- **Tipo:** decisional
- **Descripción:** Además de las páginas interiores, el `story.json` para Nubi Game incluye la ruta explícita de la imagen de portada, para que el consumidor tampoco la deduzca por nombre o posición.
- **Impacto:** REQ-012 y REQ-014.
- **Estado:** validado

### SUP-002 La imagen exportada es la importada por el adulto

- **Tipo:** decisional
- **Descripción:** La imagen de portada y de cada página que se exporta para Nubi Game es la que el adulto subió (FUN-014); no hay otra fuente de imagen de página, así que sin imagen subida la portada o página cuenta como incompleta.
- **Impacto:** REQ-009, REQ-010 y REQ-015.
- **Estado:** validado

### SUP-003 El nuevo zip para Nubi Game sustituye al actual

- **Tipo:** decisional
- **Descripción:** El zip con `story.json` y referencias explícitas sustituye a la primera versión del zip, que no referencia los assets; el formato antiguo deja de ofrecerse.
- **Impacto:** REQ-016.
- **Estado:** validado

### SUP-004 La portada muestra como referencia la descripción de escena de la primera página

- **Tipo:** decisional
- **Descripción:** La portada no tiene descripción de escena propia; para ilustrarla con la herramienta externa, la aplicación muestra en la portada la descripción de escena de la primera página interior, igual que hace la exportación visual con la portada (REQ-027).
- **Impacto:** Alcance de REQ-001 respecto a la portada; afecta a la pantalla de portada.
- **Estado:** descartado. El usuario afirma que la portada tiene descripción de escena propia; la pantalla de portada muestra esa descripción (REQ-036, SUP-016) y la escena `cover` de la exportación visual la usa como `visualDescription` (REQ-027, SUP-014).

### SUP-005 Momento en que se puede subir imagen

- **Tipo:** decisional
- **Descripción:** Como la imagen queda ligada al contenido aceptado, una página interior admite subida solo cuando está aceptada, y la portada admite subida solo cuando está aceptada (su descripción de escena aceptada, REQ-039). Revisado en la revisión 5 por decisión del usuario, que ha revisado lo ya validado: antes, la portada admitía subida desde que el guion estaba aprobado.
- **Impacto:** Precondiciones de REQ-002 y REQ-003.
- **Estado:** validado

### SUP-006 Vida de la imagen de portada

- **Tipo:** decisional
- **Descripción:** La imagen de portada se conserva mientras la portada aceptada no cambie; se pierde si la portada se reaprueba o si el guion se rehace. Los cambios en páginas interiores no la afectan. Revisado en la revisión 5 por decisión del usuario, que ha revisado lo ya validado: antes solo se perdía al rehacerse el guion.
- **Impacto:** REQ-007 y REQ-043.
- **Estado:** validado

### SUP-007 Las imágenes se exportan en su formato original

- **Tipo:** decisional
- **Descripción:** Como la importación acepta cualquier formato de imagen (PDEC-042), el zip para Nubi Game incluye cada imagen tal como se subió, sin convertirla de formato ni redimensionarla. La aplicación consumidora se encarga del formato.
- **Impacto:** REQ-011 y REQ-015; compatibilidad con Nubi Game, que queda a cargo de Nubi Game.
- **Estado:** validado. En la revisión 6 el usuario revierte la corrección anterior (que lo había descartado en favor de convertir las imágenes a un formato óptimo para móviles, con REQ-033 a REQ-035 y SUP-015): "el zip lleva las imágenes originales tal cual se subieron, sin convertir, y la aplicación consumidora se encarga del formato".

### SUP-008 Todas las escenas visuales referencian al protagonista

- **Tipo:** decisional
- **Descripción:** La portada y cada escena interior del `story.json` visual incluyen en `characters` el `id` del protagonista, al ser el único personaje exportado y el eje de cada beat.
- **Impacto:** REQ-030; contenido de `scenes[].characters`.
- **Estado:** validado

### SUP-009 Derivación del `storyId` desde el título

- **Tipo:** decisional
- **Descripción:** El `storyId` se obtiene del título pasando a minúsculas, transliterando tildes, diéresis y `ñ` a ASCII, sustituyendo cualquier secuencia de caracteres no alfanuméricos por un guion, quitando guiones iniciales y finales y recortando a 40 caracteres sin dejar guion final. Si el resultado tiene menos de 3 caracteres, la exportación se rechaza (REQ-032).
- **Impacto:** REQ-022 y REQ-032.
- **Estado:** validado

### SUP-010 El beat ya contiene un tono

- **Tipo:** contextual
- **Descripción:** Cada beat de la escaleta del director incluye ya un tono del que se puede derivar la emoción dominante de la escena.
- **Impacto:** REQ-029. La emoción de la portada sale de su tono propio (REQ-037), no de un beat.
- **Estado:** pendiente

### SUP-011 El lugar de escena y el tono de la portada forman parte de la escaleta que aprueba el adulto

- **Tipo:** decisional
- **Descripción:** El lugar de escena de cada beat se muestra al adulto en la escaleta junto al resto del beat y se aprueba con ella, para que el `story.json` refleje lo aprobado (motivo de PDEC-041). El lugar y el tono propios de la portada, que genera el director, se muestran y se aprueban del mismo modo junto con la escaleta. Ampliado en la revisión 4 por decisión del usuario (SUP-017).
- **Impacto:** REQ-017, REQ-037 y REQ-038; pantalla de revisión del guion.
- **Estado:** validado

### SUP-012 Calidad del retrato de la instantánea

- **Tipo:** contextual
- **Descripción:** Los retratos generados con ComfyUI suelen ser cuadrados y de al menos 1024 × 1024 px; si alguno no lo es, la exportación visual se rechaza según REQ-026.
- **Impacto:** Frecuencia de rechazos de REQ-026.
- **Estado:** pendiente

### SUP-013 Identificador del protagonista

- **Tipo:** decisional
- **Descripción:** El `id` del protagonista en el `story.json` visual se deriva de su nombre en la instantánea con las mismas reglas de slug que SUP-009.
- **Impacto:** REQ-020 y REQ-024; ruta `characters/<id>/character.png`.
- **Estado:** validado

### SUP-014 Origen de `visualDescription`

- **Tipo:** decisional
- **Descripción:** La `visualDescription` de cada escena interior es la descripción de escena aceptada de la página, redactada por el escritor (FUN-015); en la escena `cover` es la descripción de escena aceptada de la portada, redactada también por el escritor (REQ-038, SUP-016). Revisado en la revisión 3 por decisión del usuario: antes, la portada usaba la de la primera página interior.
- **Impacto:** REQ-027 y REQ-028.
- **Estado:** validado

### SUP-015 Formato y tamaño destino de las imágenes para Nubi Game

- **Tipo:** decisional
- **Descripción:** Las imágenes exportadas para Nubi Game se convierten a WebP con calidad 85, con la orientación ya aplicada y sin metadatos; el lado mayor se limita a 2048 px. No se define tratamiento de la transparencia, porque las imágenes no la tienen (SUP-019).
- **Impacto:** REQ-011, REQ-015, REQ-033 y REQ-034; formato, extensión y dimensiones de las imágenes del zip y rutas del `story.json`.
- **Estado:** descartado. En la revisión 6 el usuario decide exportar las imágenes originales sin conversión (SUP-007); se retiran REQ-033, REQ-034 y REQ-035 y se reescriben REQ-011 y REQ-015.

### SUP-016 La portada tiene descripción de escena propia

- **Tipo:** contextual
- **Descripción:** La portada (página 0) dispone de una descripción de escena propia, distinta de la de las páginas interiores, que el escritor redacta y el adulto acepta con el mismo procedimiento que las páginas interiores (REQ-038 a REQ-042). Lo afirma el usuario; `docs/product/producto.md` no lo recoge (FUN-014 y FUN-015 solo mencionan la descripción de escena de las páginas, y PDEC-026 indica que la portada no tiene texto).
- **Impacto:** REQ-027, REQ-036, REQ-038 a REQ-043 y criterio de portada de REQ-032.
- **Estado:** validado

### SUP-017 Lugar y emoción de la portada salen del primer beat

- **Tipo:** decisional
- **Descripción:** Propuesta: como el guion tiene un beat por página interior y la portada no tiene beat propio, el `setting` y la `emotion` de la escena `cover` se siguen tomando del lugar y el tono del beat de la página 1. La alternativa sería añadir a la portada un lugar y un tono propios en el guion, lo que ampliaría REQ-017 y la escaleta que aprueba el adulto.
- **Impacto:** REQ-027; posible coherencia entre la `visualDescription` de la portada y su `setting`.
- **Estado:** descartado. El usuario elige la alternativa: la portada tiene lugar y tono propios en el guion, que genera el director y el adulto aprueba con la escaleta (SUP-011). Sustituido por la ampliación de REQ-017, el nuevo REQ-037 y los cambios en REQ-018, REQ-019 y REQ-027.

### SUP-018 Transparencia de las imágenes exportadas para Nubi Game

- **Tipo:** decisional
- **Descripción:** Propuesta: como WebP admite transparencia, la conversión conserva el canal alfa de la imagen importada sin aplanarlo; una imagen sin transparencia se exporta opaca. La alternativa es aplanar la transparencia sobre fondo blanco, como se proponía con JPEG, si Nubi Game no compone las imágenes de página sobre un fondo propio.
- **Impacto:** REQ-033 (criterio de zonas transparentes); aspecto de las imágenes con transparencia en Nubi Game.
- **Estado:** descartado. El usuario indica que las imágenes de portada y de páginas no tienen transparencia, así que la decisión no es necesaria; sustituido por SUP-019 y se retira de REQ-033 el criterio de zonas transparentes.

### SUP-019 Las imágenes de portada y de páginas no tienen transparencia

- **Tipo:** contextual
- **Descripción:** Las imágenes que el adulto importa en la portada y en las páginas interiores no tienen transparencia. Lo afirma el usuario. Desde la revisión 6 las imágenes se exportan sin conversión (SUP-007), así que, si alguna tuviera transparencia, llegaría al zip tal como se subió.
- **Impacto:** Ninguno sobre requisitos vigentes desde la revisión 6; antes condicionaba REQ-033 y SUP-015, ya retirado y descartado.
- **Estado:** validado

### SUP-020 El audio de una página que cambia sigue el comportamiento actual de la narración

- **Tipo:** contextual
- **Descripción:** Si una página interior cambia por un hint o por la cascada de PDEC-020, su audio conserva o pierde la validez según el comportamiento actual de la narración, que esta feature no modifica. La exportación para Nubi Game cuenta como audio de la página el que la narración tenga asociado a ella en ese momento.
- **Impacto:** REQ-009 y REQ-010 (audio de cada página interior).
- **Estado:** validado

### SUP-021 Los cuentos existentes no son exportables

- **Tipo:** decisional
- **Descripción:** Los cuentos creados antes de esta feature no tienen lugar ni tono propios de la portada (fuera de alcance), así que el escritor no puede redactar su descripción de escena de portada (REQ-038) y su portada nunca queda aceptada. Por REQ-009 y REQ-018, esos cuentos no son exportables ni para Nubi Game ni para el agente visual; antes de esta revisión sí lo eran para Nubi Game si tenían imágenes y audio.
- **Impacto:** REQ-009, REQ-010, REQ-018 y REQ-019 aplicados a cuentos existentes.
- **Estado:** validado. El usuario acepta que los cuentos existentes dejen de ser exportables, también para Nubi Game.

### SUP-022 Cómo identifica Nubi Game el formato de cada imagen

- **Tipo:** decisional
- **Descripción:** Como las imágenes van al zip en su formato original (SUP-007), cada fichero de imagen del zip lleva la extensión que corresponde a su formato real (por ejemplo `.png`, `.jpg`, `.bmp`), con independencia del nombre con que se subió. El formato de cada imagen se identifica por la extensión de su fichero; el `story.json` para Nubi Game no lo declara en un campo aparte.
- **Impacto:** REQ-011; rutas de imagen de REQ-012, REQ-013 y REQ-014; contenido del `story.json` para Nubi Game.
- **Estado:** validado. En la revisión 7 el usuario acepta la propuesta y descarta la alternativa de declarar el tipo de imagen en un campo del `story.json`.

## Preguntas abiertas

Ninguna.
