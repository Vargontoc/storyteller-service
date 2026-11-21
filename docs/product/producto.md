# Producto: Storyteller Agent

## Visión

- Para quién: adultos (padres, educadores) que crean cuentos ilustrados y narrados para niños de 3 a 4 años.
- Problema que resuelve: crear un cuento completo (guión, personajes consistentes, imágenes y audio) sin saber escribir, dibujar ni editar audio, en local y con modelos propios.
- Propuesta de valor (1 o 2 frases): un adulto elige un tema del catálogo y un protagonista y revisa por etapas un cuento listo para leer o escuchar, con un protagonista que mantiene su identidad visual entre cuentos.
- Restricciones (legales, presupuesto, plataforma): hardware local (RTX 4070 SUPER / 12 GB VRAM), servicios en contenedores (Ollama, ComfyUI, Chatterbox), contenido apto para 3-4 años. Pendiente de confirmar.

## Funcionalidades

| ID | Funcionalidad | Valor | Prioridad | Estado | Notas | Especificacion SDD |
|---|---|---|---|---|---|---|
| FUN-001 | Generación de cuentos secuencial en tiempo real | Core del producto | imprescindible | acordada | Ya implementado (director, writer, narrator) | |
| FUN-002 | Gestión de cuentos con revisión y aceptación por etapas | Control del adulto sobre el resultado | imprescindible | acordada | Ya implementado | |
| FUN-003 | Generación de imágenes por ComfyUI (retratos de personaje) | Cuento ilustrado | imprescindible | acordada | Ya implementado (SDXL + IP-Adapter + LoRA). La ilustración de página ya no se genera en ComfyUI: ver PDEC-017 | |
| FUN-004 | Generación de audio por Chatterbox | Cuento narrado | imprescindible | acordada | Ya implementado | |
| FUN-005 | Catálogo de personajes reutilizable entre cuentos | Continuidad del protagonista y menos retrabajo | imprescindible | acordada | Biblioteca persistente con identidad visual fija; el catálogo es el reparto de protagonistas, no un elenco completo | |
| FUN-006 | Retrato del personaje con propuestas antes de usarlo | Fija la identidad visual del personaje | imprescindible | acordada | 2-4 candidatos generados por ComfyUI; el elegido queda como referencia de las escenas | |
| FUN-007 | Agente de personaje (Ollama) | Descripción narrativa y visual coherente y revisable | imprescindible | acordada | Absorbe el MODO 3 del director: genera descripción, traducción/atributos visuales y revisa los cambios pedidos por el adulto | |
| FUN-008 | Protagonista elegido o creado al iniciar el cuento | Reparto explícito y estable | imprescindible | acordada | Tema y protagonista en la misma pantalla; catálogo vacío es un estado inicial válido | |
| FUN-009 | Edición de un personaje usado en cuentos | Evitar romper cuentos ya generados | imprescindible | acordada | Cada cuento guarda la instantánea del personaje y su retrato; editar el catálogo solo afecta a cuentos nuevos | |
| FUN-010 | Pack inicial de personajes | Evitar catálogo vacío en el primer uso | deseable | descartada | Descartada: el catálogo arranca vacío y el primer cuento exige crear el protagonista | |
| FUN-011 | Reparto secundario sin ficha visual | Libertad narrativa sin inflar el catálogo | imprescindible | acordada | Los personajes y elementos episódicos (ej. "un conejo blanco" en la página 3) los nombra el director y se dibujan desde la descripción de escena; sin retrato ni identidad persistente | |
| FUN-012 | Creación del protagonista en dos modos | Reducir la fricción de un catálogo vacío | imprescindible | acordada | Automático: el agente inventa nombre, personalidad y descripción visual completa. Manual: el adulto escribe una frase libre y el agente completa la ficha. En ambos casos el adulto elige el retrato | |
| FUN-013 | Escaleta del guión página a página (director-guionista) | Arco narrativo cerrado en N páginas y control del adulto antes de escribir | imprescindible | acordada | El director recibe tema, nº de páginas y protagonista del catálogo, y devuelve título, sinopsis, reparto episódico y un beat estructurado por página. Se aprueba antes de generar páginas y el escritor la sigue de forma contractual | |
| FUN-014 | Importación de las ilustraciones de página generadas fuera | Mantener el cuento ilustrado con la herramienta externa | imprescindible | acordada | Cada página expone su descripción de escena; la imagen que devuelve la herramienta externa se importa y se muestra en la página. ComfyUI no interviene | |
| FUN-015 | Generación de páginas una a una tras el guion aprobado | Revisar y corregir sin rehacer el cuento entero | imprescindible | acordada | El escritor redacta cada página desde su beat, incluida la descripción de escena (sin imagen). Cada página se acepta antes de generar la siguiente (PDEC-022). Cualquier cambio descarta las páginas hijas y se regeneran una a una, sin propagación automática (PDEC-020). El cambio se pide con hint, con aceptación o rechazo motivado, sin edición manual (PDEC-021). Vocabulario y frases simples, para que un niño de 3-4 años lo entienda (PDEC-023) | |
| FUN-016 | Catálogo interno de temas para el cuento | Evitar temas inventados fuera de tono y simplificar la creación | imprescindible | acordada | Catálogo fijo cargado por migración: aventuras, rutinas, animales, naturaleza, familia y amigos y juegos, pensados para 3-4 años y sin moraleja ni carga moral. El tema se elige del catálogo; ya no hay agente generador de temas (PDEC-024) | |

## Opciones de tecnología

### Agente de personaje en Ollama

| Opción | Ventajas | Costes y riesgos | Fuente y fecha |
|---|---|---|---|
| Mismo modelo base que el director (qwen2.5:14b-instruct) con Modelfile propio | Ya descargado, sin coste de VRAM adicional si no conviven; estilo de prompt conocido | Compite por GPU con el director si se ejecutan a la vez | 2026-11-15 (estado del repo) |
| Modelo más pequeño para el agente de personaje | Respuesta más rápida y menos VRAM | Peor calidad en descripción visual y en rechazo de hints | 2026-11-15 |

Recomendación: Modelfile propio sobre el mismo modelo base; el agente de personaje no necesita convivir con el director en el mismo turno.

## Decisiones

| ID | Decisión | Motivo | Alternativas descartadas |
|---|---|---|---|
| PDEC-001 | El catálogo es una biblioteca reutilizable entre cuentos, con identidad visual fija | Continuidad del personaje y reutilización de imágenes | Inventario solo por cuento; biblioteca con colecciones y edición global (fase posterior) |
| PDEC-002 | El personaje se aprueba con un retrato elegido entre 2-4 propuestas, y ese retrato es la referencia de las escenas | La identidad visual deja de depender del prompt | Regenerar retrato solo desde la ficha; hoja de personaje con poses |
| PDEC-003 | El nuevo agente de personaje absorbe el MODO 3 del director y recibe contexto del cuento para rechazar hints incoherentes | Un solo dueño por personaje | Agente propone y director valida; comportamiento distinto según haya cuento o no |
| PDEC-004 | El personaje nace solo en el catálogo; el cuento elige del catálogo | Reparto explícito y estable | Nacer en el cuento; ambos caminos |
| PDEC-005 | El catálogo cubre solo al protagonista; el reparto secundario no tiene ficha visual | Mantiene el catálogo pequeño y la narración libre | Ficha ligera para secundarios |
| PDEC-006 | Tema y protagonista se eligen en la misma pantalla al crear el cuento | Evita sinopsis que no encajen con el protagonista y no bloquea el primer uso | Protagonista primero; tema primero |
| PDEC-007 | Cada cuento guarda la instantánea del protagonista y su retrato; editar el catálogo no altera cuentos anteriores | Coherencia con imágenes ya generadas y con cuentos aceptados | Referencia viva; instantánea con propagación opcional |
| PDEC-008 | El catálogo arranca vacío, sin pack inicial | Se prefiere que el adulto cree su primer protagonista antes que mantener un elenco precargado | Pack de 8-12; pack mínimo de 3-4 |
| PDEC-009 | Edición híbrida: campos de texto editables a mano y cambios de fondo pedidos al agente, que acepta o rechaza con motivo; lo visual siempre lo recalcula el agente | Control fino donde hace falta y conversación donde aporta | Solo conversación; solo edición manual |
| PDEC-010 | Un personaje sin retrato aprobado es borrador y no se puede elegir en un cuento | Todo cuento nace con identidad visual garantizada | Usable sin retrato; usable con aviso |
| PDEC-011 | El nombre del protagonista es editable en el catálogo; cada cuento conserva el nombre de su instantánea | El identificador estable es interno, no el nombre | Nombre inmutable; renombrar solo si no se ha usado |
| PDEC-012 | El protagonista se crea en modo automático (lo inventa el agente) o modo manual (frase libre que el agente completa) | Cubre tanto "sorpréndeme" como "quiero este personaje" | Solo frase libre; solo formulario guiado |
| PDEC-013 | El director-guionista produce un único guión completo (título, sinopsis, reparto episódico y escaleta) a partir de tema, nº de páginas y protagonista del catálogo | Hoy la sinopsis se escribe sin conocer al protagonista y no existe plan por página | Llamada aparte solo para la escaleta; mantener el director actual y que planifique el escritor |
| PDEC-014 | Cada beat de la escaleta es estructurado: qué ocurre, función narrativa, quiénes aparecen, emoción o tono y nota visual | Alimenta al escritor y a la herramienta externa de imagen sin invadir la redacción | Una frase por página; escena casi lista para imagen |
| PDEC-015 | La escaleta es contractual: el escritor narra el hecho del beat, no lo cambia | Es lo que da sentido a aprobar un plan | Guía orientativa; simple contexto |
| PDEC-016 | Aprobar el guión es requisito para generar la primera página; los cambios se piden con un hint y el agente devuelve la escaleta completa, con aceptación o motivo del rechazo | Sin puerta de aprobación, la escaleta no controla nada; mismo patrón que el agente de personaje | Revisión opcional (actual); edición manual página a página como único mecanismo |
| PDEC-017 | La descripción de escena de cada página se conserva, pero ComfyUI ya no la usa: la consume una herramienta externa | El guión debe seguir siendo utilizable fuera del producto | Eliminar la escena; conservarla solo como texto libre no exportable |
| PDEC-018 | La ilustración de página la genera una herramienta externa a partir de la escena; el producto importa la imagen resultante y la muestra en la página. ComfyUI queda solo para retratos | Mantiene el cuento ilustrado sin cargar la GPU local con la ilustración de página | Seguir generando la escena en ComfyUI; página solo con texto |
| PDEC-019 | Corrección híbrida de la escaleta: hint global al guión para cambios de fondo y edición directa del texto de un beat concreto; lo ya aprobado no se recalcula | Con 32 páginas no se puede regenerar todo por un cambio puntual | Solo hint global; solo edición manual |
| PDEC-020 | Cualquier cambio en una página descarta todas sus páginas hijas; no se regeneran automáticamente, se regeneran una a una bajo demanda | Es una herramienta de generación de contenido en desarrollo, sin usuario final: prima la coherencia y se evita gastar GPU en cascada | Distinguir retoque cosmético de cambio de contenido; propagar y regenerar automáticamente |
| PDEC-021 | El cambio de una página se pide con un hint al escritor, que regenera esa página completa y acepta o rechaza con motivo; no hay edición manual del texto | «Cambios ajustados» encaja con un hint y el agente es quien garantiza no salirse del beat; mismo patrón que el guion (PDEC-016) y la ficha (PDEC-009) | Edición directa del texto (exige validar contra el beat); híbrido como PDEC-019 |
| PDEC-022 | La generación se detiene en cada página: se acepta la p.N antes de generar la p.N+1 | Con la cascada estricta (PDEC-020), generar de golpe produce páginas que se tirarían | Generar todas y revisar después |
| PDEC-023 | El texto de las páginas usa vocabulario y frases simples para que un niño de 3-4 años lo entienda (criterio receptivo); no imita el habla infantil | Es lo que necesita el oyente de esa edad y evita empobrecer la narración | Que los diálogos imiten el habla de un niño de 3-4; criterio mixto |
| PDEC-024 | El tema se elige de un catálogo interno fijo y deja de generarse con un agente | Control del tono y del contenido para 3-4 años, sin temas inventados ni lecciones morales | Mantener el generador con filtros; catálogo editable desde la app |

## Preguntas abiertas

- [ ] Criterios de edad y de "especies fáciles de dibujar" que debe compartir el agente de personaje con el director, para que el modo automático no invente protagonistas difíciles de ilustrar.
- [ ] Confirmar la visión y las restricciones con el usuario.
- [ ] La importación de las ilustraciones de página se hará en su momento (etapa posterior); falta acordar el formato de intercambio con la herramienta externa.
- [ ] Ajustar la visión a herramienta de generación de contenido en desarrollo; padres y educadores son el horizonte, no el usuario actual.
