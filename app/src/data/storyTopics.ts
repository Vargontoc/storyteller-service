export interface StoryTopic {
  id: number
  type: string
  description: string
}

/**
 * Catálogo estático de temas para cuentos de 3-4 años.
 * Son aventuras y rutinas cotidianas, sin moraleja ni carga moral.
 * No se persiste en el servidor: el tema elegido viaja al director al crear el cuento.
 */
export const STORY_TOPICS: StoryTopic[] = [
  { id: 1, type: 'Aventuras', description: 'Un viaje en globo entre las nubes' },
  { id: 2, type: 'Aventuras', description: 'Una búsqueda del tesoro en el jardín' },
  { id: 3, type: 'Aventuras', description: 'Un paseo en tren por la montaña' },
  { id: 4, type: 'Aventuras', description: 'Una excursión a la cueva de los ecos' },
  { id: 5, type: 'Aventuras', description: 'Un día en el faro junto al mar' },
  { id: 6, type: 'Aventuras', description: 'Un cohete de cartón rumbo a la luna' },
  { id: 7, type: 'Aventuras', description: 'Una isla con flores gigantes' },
  { id: 8, type: 'Aventuras', description: 'Un barco de papel en el charco' },

  { id: 9, type: 'Rutinas', description: 'Levantarse y vestirse por la mañana' },
  { id: 10, type: 'Rutinas', description: 'El desayuno con tostadas y mermelada' },
  { id: 11, type: 'Rutinas', description: 'Lavarse los dientes antes de dormir' },
  { id: 12, type: 'Rutinas', description: 'La hora del baño con muchas burbujas' },
  { id: 13, type: 'Rutinas', description: 'Recoger los juguetes al terminar' },
  { id: 14, type: 'Rutinas', description: 'Preparar la mochila para ir al parque' },
  { id: 15, type: 'Rutinas', description: 'El cuento de antes de dormir' },

  { id: 16, type: 'Animales', description: 'Un gatito que persigue mariposas' },
  { id: 17, type: 'Animales', description: 'Un perrito que encuentra un balón' },
  { id: 18, type: 'Animales', description: 'Una pata y sus patitos en el estanque' },
  { id: 19, type: 'Animales', description: 'Un erizo que busca su gorrito' },
  { id: 20, type: 'Animales', description: 'Un búho que no puede dormir' },
  { id: 21, type: 'Animales', description: 'Una tortuga que pasea despacio al río' },
  { id: 22, type: 'Animales', description: 'Un conejo que salta entre zanahorias' },

  { id: 23, type: 'Naturaleza', description: 'Una tarde de lluvia y charcos' },
  { id: 24, type: 'Naturaleza', description: 'El arcoíris después de la tormenta' },
  { id: 25, type: 'Naturaleza', description: 'Un paseo por el bosque de pinos' },
  { id: 26, type: 'Naturaleza', description: 'Las estrellas desde la ventana' },
  { id: 27, type: 'Naturaleza', description: 'Las hojas de otoño que vuelan' },
  { id: 28, type: 'Naturaleza', description: 'Un día de nieve y muñecos' },
  { id: 29, type: 'Naturaleza', description: 'Las flores que abren por la mañana' },

  { id: 30, type: 'Familia y amigos', description: 'Una merienda con los abuelos' },
  { id: 31, type: 'Familia y amigos', description: 'Un cumpleaños con globos de colores' },
  { id: 32, type: 'Familia y amigos', description: 'Un picnic en el parque' },
  { id: 33, type: 'Familia y amigos', description: 'Jugar con un amigo nuevo' },
  { id: 34, type: 'Familia y amigos', description: 'Una tarde con la tía y sus cuentos' },
  { id: 35, type: 'Familia y amigos', description: 'Regar las plantas con papá' },
  { id: 36, type: 'Familia y amigos', description: 'Una siesta con la manta favorita' },

  { id: 37, type: 'Juegos', description: 'Una torre de bloques muy alta' },
  { id: 38, type: 'Juegos', description: 'Un tren de juguete por el pasillo' },
  { id: 39, type: 'Juegos', description: 'Pintar con las manos de colores' },
  { id: 40, type: 'Juegos', description: 'Una caja de cartón que es un castillo' },
  { id: 41, type: 'Juegos', description: 'Un puzzle de animales del bosque' },
  { id: 42, type: 'Juegos', description: 'Una pelota que rueda y rueda' },
  { id: 43, type: 'Juegos', description: 'Unas marionetas que cobran vida' },
]
