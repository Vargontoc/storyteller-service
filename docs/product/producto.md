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
| FUN-004 | Generación de audio por Chatterbox | Cuento narrado | imprescindible | acordada | Ya implementado: audio por página. La evolución con voz elegida está en FUN-017 | |
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
| FUN-015 | Generación de páginas una a una tras el guion aprobado | Revisar y corregir sin rehacer el cuento entero | imprescindible | acordada | El escritor redacta cada página desde su beat, incluida la descripción de escena (sin imagen). Cada página se acepta antes de generar la siguiente (PDEC-022). Cualquier cambio descarta las páginas hijas y se regeneran una a una, sin propagación automática (PDEC-020). El cambio se pide con hint, con aceptación o rechazo motivado, sin edición manual (PDEC-021). Vocabulario y frases simples, para que un niño de 3-4 años lo entienda (PDEC-023). El texto tiene 4 frases y 45-60 palabras, con una sola frase corta y las demás completas (PDEC-031); incluye al menos una línea de diálogo cuando hay dos o más personajes interactuando (PDEC-032); dramatiza el hecho del beat sin copiar su redacción y muestra la emoción con acciones y voces (PDEC-033, PDEC-034) | |
| FUN-016 | Catálogo estático de temas para el cuento | Evitar temas inventados fuera de tono y simplificar la creación | imprescindible | acordada | Catálogo fijo en la app: aventuras, rutinas, animales, naturaleza, familia y amigos y juegos, pensados para 3-4 años y sin moraleja ni carga moral. El tema elegido viaja al director al crear el cuento; no hay agente generador ni catálogo persistido, y cada cuento guarda como texto el tema con el que nació (PDEC-024) | |
| FUN-017 | Narración por página con voz elegida | Voz propia y expresividad controlada | imprescindible | acordada | Evoluciona FUN-004. El narrador lee el cuento entero y prepara el guion de audio (tono y énfasis), y Chatterbox Multilingual V3 sintetiza en español. Las onomatopeyas las escribe el escritor en el texto (PDEC-025), máximo una por página y solo con sonido natural en la escena (PDEC-030). La narración solo se habilita con todas las páginas interiores aceptadas (PDEC-026). El adulto sube voces y elige una al narrar; la voz se guarda con el cuento y renarrar con otra sustituye el audio (PDEC-029). La salida es audio por página: la app que integra el cuento no tiene narración automática y solo reproduce la página que se está viendo (PDEC-028). Motor: Multilingual V3, Turbo descartado (PDEC-027) | |

## Opciones de tecnología

### Agente de personaje en Ollama

| Opción | Ventajas | Costes y riesgos | Fuente y fecha |
|---|---|---|---|
| Mismo modelo base que el director (qwen2.5:14b-instruct) con Modelfile propio | Ya descargado, sin coste de VRAM adicional si no conviven; estilo de prompt conocido | Compite por GPU con el director si se ejecutan a la vez | 2026-11-15 (estado del repo) |
| Modelo más pequeño para el agente de personaje | Respuesta más rápida y menos VRAM | Peor calidad en descripción visual y en rechazo de hints | 2026-11-15 |

Recomendación: Modelfile propio sobre el mismo modelo base; el agente de personaje no necesita convivir con el director en el mismo turno.

### Motor TTS para español (Chatterbox)

| Opción | Ventajas | Costes y riesgos | Fuente y fecha |
|---|---|---|---|
| Chatterbox Multilingual V3 (contenedor actual, `USE_MULTILINGUAL_MODEL=true`) | Español entre 23 idiomas, clonación por referencia, mantiene exageración/cfg/temperatura y biblioteca de voces con idioma | El upstream está retirado como investigación desde ago-2026 (mantenimiento comunitario); el multilingüe general puede quedar por debajo del finetune es-ES en dicción | 2026-11-22 (Hugging Face y GitHub de ResembleAI; travisvn/chatterbox-tts-api) |
| Finetune Chatterbox Multilingual es-ES (o es-MX-LatAm) | Mejor dicción y calidad regional para España o Latinoamérica | Requiere cambiar el modelo y la carga del contenedor; más trabajo de integración | 2026-11-22 (Hugging Face ResembleAI/Chatterbox-Multilingual-es-es) |
| Chatterbox Turbo | 350M, menos VRAM y latencia; etiquetas paralingüísticas (`[laugh]`, `[sigh]`…) | Solo inglés; ignora exageración y `cfg_weight`, así que se pierden los presets de tono; el contenedor actual no lo soporta (PR abierto desde ene-2026) | 2026-11-22 (Hugging Face ResembleAI/chatterbox-turbo; docs NVIDIA; PR #74 de travisvn) |

Recomendación acordada: Multilingual V3, que es lo que ya usa el contenedor. Turbo queda descartado para español. El finetune es-ES se evalúa solo si la dicción no basta.

### Modelo del escritor

| Opción | Ventajas | Costes y riesgos | Fuente y fecha |
|---|---|---|---|
| qwen2.5:14b-instruct | Mismo modelo que el resto de agentes; ya descargado | Con las reglas nuevas se queda en 30-32 palabras y no cuenta; ignora el objetivo de longitud | 2026-11-23 (prueba local con los prompts del repo) |
| qwen3:14b solo para el escritor | Respeta mejor las reglas: 41-57 palabras, diálogo y escena coherente en las pruebas; ya descargado | Razonamiento interno (unos 2.000 caracteres) y más latencia; 9,3 GB; el resto de agentes sigue en qwen2.5 | 2026-11-23 (prueba local con los prompts del repo) |

Recomendación aplicada: qwen3:14b para el escritor y qwen2.5:14b para director, narrador y agente de personaje.

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
| PDEC-024 | El tema se elige de un catálogo estático en la app y deja de generarse con un agente. Solo se envía al director al crear el cuento, y el cuento lo conserva como texto | El tema es un dato de entrada, no una entidad persistida; se elimina la tabla, la FK y el endpoint sin perder la trazabilidad del cuento | Mantener el generador con filtros; catálogo editable desde la app; servir el catálogo desde el backend |
| PDEC-025 | Las onomatopeyas forman parte del texto de la página y las escribe el escritor; no se añaden solo en el audio | Texto y audio dicen lo mismo, y el TTS pronuncia lo que está escrito con intención | Añadirlas solo en audio; híbrido texto+audio |
| PDEC-026 | La narración solo se genera cuando todas las páginas interiores están generadas y aceptadas; la portada no tiene texto y no cuenta | Evita narrar texto que luego cambia por la cascada (PDEC-020) y gastar GPU dos veces | Narrar con páginas pendientes; narrar solo con el guion aprobado |
| PDEC-027 | Motor TTS: Chatterbox Multilingual V3. Turbo queda descartado para español; el finetune es-ES se evaluará solo si la dicción no basta | Turbo es solo inglés e ignora exageración y cfg_weight; V3 conserva los controles y ya está en el contenedor | Chatterbox Turbo; adoptar el finetune es-ES desde el inicio |
| PDEC-028 | La narración se entrega como audio por página, sin pista completa del cuento | La app que integra el cuento no tiene narración automática: reproduce solo la página que se está viendo | Pista completa; audio por página más pista completa |
| PDEC-029 | La voz se elige al narrar, con la voz por defecto preseleccionada, y se guarda con el cuento; renarrar con otra voz se permite avisando de que sustituye todo el audio | Cada protagonista puede tener su voz y queda constancia de con cuál se narró | Voz global fija; voz inmutable tras la primera narración |
| PDEC-030 | Onomatopeyas: máximo una por página y solo si la escena tiene un sonido natural; formas estándar y pronunciables; cuentan dentro de las 60 palabras; nunca en la portada | Enriquece la lectura en voz alta sin romper el límite de texto ni la pronunciación del TTS | Sin límite; varias por página |
| PDEC-031 | El texto de cada página tiene 4 frases y entre 45 y 60 palabras, con una sola frase corta y las demás completas; una sola regla de longitud en todo el escritor | El rango fija un objetivo, no solo un techo, y una única frase corta da ritmo sin convertir la página en frases telegráficas | Exactamente 15 palabras por frase; mantener el máximo de 60 sin mínimo |
| PDEC-032 | En páginas con dos o más personajes que interactúan, el texto incluye al menos una línea de diálogo con verbo dicendi simple; en páginas de un solo personaje no se fuerza | El diálogo da voz a los personajes y reduce el relato plano sin inventar interacción donde no la hay | Diálogo en todas las páginas; solo cuando el beat lo pida |
| PDEC-033 | El beat aporta el hecho y la función narrativa, no la redacción: el escritor no copia sus palabras y lo convierte en escena con acciones, gestos y voces, pudiendo añadir detalles sensoriales que no cambien el hecho, la función ni el tono | Evita el copy-paste del beat y mantiene el contrato narrativo (PDEC-015) | Permitir parafrasear el beat; exigir solo el hecho |
| PDEC-034 | Mostrar, no contar: la emoción se muestra en lo que los personajes hacen o dicen, no se nombra | Es lo que hace que el cuento se disfrute leído en voz alta | Nombrar la emoción; combinar ambas |
| PDEC-035 | Si el texto no cumple la longitud o el diálogo, se muestra igualmente y es el adulto quien pide el cambio con un hint; no hay reintento automático | El adulto mantiene el control y no se gasta GPU en reintentos que quizá no coincidan con su criterio | Reintento automático antes de mostrar; aviso bloqueante |
| PDEC-036 | El escritor usa qwen3:14b; director, narrador y agente de personaje siguen en qwen2.5:14b | Con las mismas reglas, qwen2.5 se queda en 30-32 palabras y qwen3 da 41-57 con diálogo; el coste es más latencia por el razonamiento interno | Mantener qwen2.5 y añadir una pasada de ampliación; cambiar todos los agentes a qwen3 |

## Preguntas abiertas

- [ ] Criterios de edad y de "especies fáciles de dibujar" que debe compartir el agente de personaje con el director, para que el modo automático no invente protagonistas difíciles de ilustrar.
- [ ] Confirmar la visión y las restricciones con el usuario.
- [ ] La importación de las ilustraciones de página se hará en su momento (etapa posterior); falta acordar el formato de intercambio con la herramienta externa.
- [ ] Ajustar la visión a herramienta de generación de contenido en desarrollo; padres y educadores son el horizonte, no el usuario actual.
- [ ] Al subir una voz hay que marcar idioma (es por defecto) y validar formato/duración; decidir si se pide declaración de permiso, porque clonar la voz de una persona real tiene implicaciones de derechos.

