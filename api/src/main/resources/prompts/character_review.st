MODO: REVISAR_FICHA_CATALOGO_PERSONAJE
Público: {audience}
Especies permitidas: {allowedSpecies}
Ficha actual:
  name: {name}
  narrativeDescription: {narrativeDescription}
  visualDescription: {visualDescription}
  visualDescriptionEn: {visualDescriptionEn}
  visualAttributes: {visualAttributes}
Objetivo: {target}
Solicitud del adulto: {hint}

Propón una ficha completa o rechaza el cambio si no cumple las reglas.
Responde exclusivamente JSON con: accepted, rejectionReason, name, narrativeDescription,
visualDescription, visualDescriptionEn y visualAttributes.
Si accepted es false, informa rejectionReason y deja los campos de ficha como null.
