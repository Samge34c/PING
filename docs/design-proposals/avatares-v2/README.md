# Avatares V3 · propuesta en Figma

Propuesta de diseño, sin implementar en la app. Página **PING · Avatares V3** del archivo Figma `WCrjKj5eGOZQ8wUaxsEqfc`. Enlaces, nodos, decisiones y reparto con Codex: [docs/handoffs/FIGMA_CLAUDE.md](../../handoffs/FIGMA_CLAUDE.md).

## Contenido

- `PING_Avatares_V2/`: paquete original extraído del ZIP `PING_Avatares_Editables*` (SHA256SUMS 132/132). No se ha modificado ni se han ejecutado sus scripts.
- `fuentes-v3/preparar_v3.py`: convierte bases y accesorios del catálogo en plantillas con colores marcadores (`#A…EE` pelaje, `#B0…EE` acento) que Figma enlaza a variables. Aplica la corrección de las gafas cuadradas.
- `fuentes-v3/plantillas_v3.json`, `lote_bases.js`: plantillas y script usado para importar las cuatro bases. `lotes_figma.py` genera esos scripts.
- `fuentes-v3/svg-v3-accesorios/`: 80 SVG (`frontal|trasero__slot__animal__pieza.svg`) subidos a Figma como vectores editables; incluye las orejas del conejo sobre el sombrero bucket.
- `capturas/`: exportaciones 1× desde Figma (pantallas 390, 360×740, claro, texto ×1,3, Mi Ping, Conectar, lista, láminas y componentes).

## Sistema

- `Avatar V3`: Animal (Panda rojo, Nutria, Gato, Conejo) × Recorte (Editor ≥ 48 px, Mini 32–44 px, Primer plano para miniaturas). Capas: traseros → base → cuello → cara → cabeza → audio, expuestas como instancias intercambiables.
- Pelaje y color de pieza son modos de variables («PING Avatares V3 · Pelaje», «… · Acento»), no variantes.
- Una pieza por slot; auriculares no combinan con gorro, gorra, bucket ni vincha (reglas del catálogo).

## Estado

Samuel aprobó el 2026-10-25 composición, colores, tipografía y jerarquía de «V3 · Personalizar · Pelaje · 390». Las miniaturas de «Cabeza» quedan reservadas a una prueba de Codex. Integrarlo en la app requiere decisión en DECISION_LOG y aceptación de Samuel y Julián. Sin pruebas en teléfono real.
