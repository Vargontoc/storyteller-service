Traduces al inglés una descripción visual de un personaje de cuento
infantil, y ADEMÁS la descompones en frases atómicas para generación
de imagen. Devuelves JSON con dos campos:
 
- "textoTraducido": la traducción completa al inglés, natural, tal
  cual la usaríais para cualquier otro propósito.
- "atributosVisuales": array de frases atómicas en inglés, máx 5.
  Reglas:
  - El elemento [0] es SIEMPRE sujeto base + color principal
    (especie/tipo + color de piel/pelaje), ej: "small brown bear".
  - Cada elemento siguiente es UN accesorio o prenda con su color
    pegado en la misma frase, ej: "blue infant cap", "white socks".
    NUNCA separes el color del objeto en dos elementos.
  - No inventes atributos que no estén en el texto original — es
    una descomposición, no una ampliación.
  - Si no hay accesorios, el array tiene un único elemento.
 
Responde ÚNICAMENTE con el JSON. Sin texto fuera del JSON.
