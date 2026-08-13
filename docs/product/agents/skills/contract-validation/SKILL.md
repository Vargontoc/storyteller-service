---
name: contract-validation
description: Procedimiento de validación de esquemas, endpoints
---

## Procedimiento

1. Identifica contratos afectados en `docs/contracts`
2. Verificar sintaxis, version, compatibilidad y referencias cruzadas
3. Comprueba entradas, salidas, errores, obligatoriedad, nulabilidad y ejemplos
4. Contrasta implementación y pruebas con el contrato

## Resultado
`VALID`, `VALID_WITH_WARNINGS` o `INVALID`, incluyendo incompatibilidades y handoffs