# PING · Propuesta V2 de marca y sistema (Figma)

Estado: **propuesta de diseño, no implementada.**

- La recomendación de marca es una opinión de diseño: no es una aprobación humana ni autoriza cambiar la aplicación.
- Integrarla exige una decisión en `DECISION_LOG.md` y la aceptación de Samuel y Julián (AGENTS.md).
- Rama local: `design/conectar-figma`. Sin cambios en el código de la app, los datos, los permisos ni `main`. Sin push ni merge.

## Enlaces (página «05 · Propuesta V2»)

La V1 (Festival editorial) se conserva intacta en las páginas 01–04.

| Qué | Frame |
|---|---|
| Página V2 | [38:262](https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/?node-id=38-262) |
| 1 · Marca: wordmark, conceptos A/B/C | [39:294](https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/?node-id=39-294) |
| 1b · Pruebas de marca (claro, oscuro, monocromo, uva, 16–48 px, icono, cabecera) | [39:3103](https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/?node-id=39-3103) |
| Logo recomendado C: símbolo · horizontal · wordmark | [39:305](https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/?node-id=39-305) · [39:311](https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/?node-id=39-311) · [39:295](https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/?node-id=39-295) |
| 2 · Componentes V2 | [40:382](https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/?node-id=40-382) |
| 3 · Pantallas: Identidad · Intereses · Encuentro activo · Recibo (390) | [43:922](https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/?node-id=43-922) · [43:1160](https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/?node-id=43-1160) · [44:1121](https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/?node-id=44-1121) · [44:1339](https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/?node-id=44-1339) |
| 360×740 (las cuatro) · texto ×1,3 con alias largo · escritorio 1280 | sección [43:921](https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/?node-id=43-921) · [46:2146](https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/?node-id=46-2146) · [46:2336](https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/?node-id=46-2336) |
| Tema claro (Recibo, Intereses) | [49:1969](https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/?node-id=49-1969) · [49:2227](https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/?node-id=49-2227) |
| 4 · Comparativa anterior/V2 · fundamentos | [49:2271](https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/?node-id=49-2271) · [49:3138](https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/?node-id=49-3138) |

**Capturas examinadas:** carpeta [`capturas/`](capturas/).

**Vectores:** carpeta [`marca/`](marca/), con los SVG maestros de C (claro, oscuro, negro, blanco, símbolo e icono de app) y los conceptos A y B.

## Marca: tres conceptos y recomendación

Los tres se construyeron como trazados propios (no es texto de una fuente). Tienen fustes de 32/128, panzas geométricas y terminales rectos. Se renderizaron y se miraron en cada ronda.

| Concepto | Idea en una frase | Resultado de las pruebas |
|---|---|---|
| A · Punto de encuentro (evolución) | El punto de «PING.» crece al grosor del trazo: el lugar donde se llega. | Conserva el valor actual. El punto se pierde sobre crema y el símbolo «P.» recuerda a una señal de aparcamiento. |
| B · Pase (emblema) | PING calado en una entrada con muescas. | Inequívocamente de evento, pero a 16 px la P se empasta, la caja repite el problema del aparcamiento y el contenedor ocupa mucho en una cabecera. |
| **C · P en común (recomendado)** | Una P hecha de dos etiquetas de interés que se cruzan; la lente amarilla es lo que comparten dos personas. | Se lee a 16 px, funciona en monocromo (la lente pasa a hueco), en oscuro y sobre uva. La lente se reutiliza en el sistema solo donde algo es «en común». |

- **Calidad de los SVG:** auditados con `svg_audit.py`, con 97/100 el símbolo, 89/100 el lockup y 98/100 el icono. El único aviso es el ángulo deliberado de la apertura de la G.
- **Sin despejar:** no se ha hecho búsqueda de marcas registradas; hace falta antes de adoptarla.

## Problemas de V1 comprobados en los componentes y resueltos

1. **Estado:** las cuatro variantes decían «Disponible para conectar» y no tenían icono. Ahora hay 9 estados reales, cada uno con su texto e icono.
2. **Campo de error:** repetía la ayuda neutra. Ahora tiene un mensaje propio que dice qué ocurrió y cómo corregirlo.
3. **Títulos superpuestos:** «Opción de gusto» y «Paso de misión» estaban en la misma posición (y = 2861). En V2 todo se coloca en flujo.
4. **Gafas invisibles en oscuro:** eran solo trazo oscuro. Ahora tienen cristal tintado y las opciones se previsualizan sobre el avatar.
5. **Avatar:** solo variaba el fondo. Ahora las variantes son tamaño (S/M/L) y marco (Simple/Anillo), y fondo, animal, sombrero y accesorio son piezas intercambiables. Se muestran 8 combinaciones distintas en claro y oscuro.
6. **Botones:**
   - Cuatro jerarquías con uso definido: Principal (avanza; una por pantalla), Secundario (alternativa), Texto (navegación, ≥ 44 px) y Peligro (cancelar o bloquear).
   - Estados: Normal, Pulsado, Cargando («Enviando…», sin doble envío) y Deshabilitado (con candado).
   - El texto se ajusta a varias líneas.
7. **Gusto, en común y categoría:** ahora tienen formas distintas:
   - Gusto: rectángulo con + o ✓.
   - En común: píldora con la lente.
   - Categoría: pestaña con contador y subrayado.

**Hallazgo nuevo corregido en esta ronda:** los indicadores amarillos (navegación activa, pestaña, progreso) no llegaban a 3:1 sobre crema. Ahora usan el rol `v2/indicador`: amarillo en oscuro, uva en claro.

## Sistema V2

- **Color:**
  - 30 variables `v2/*` con modos Oscuro/Claro y sintaxis `var(--ping-*)`.
  - Los tintes de estado son colores sólidos: la opacidad de un relleno ligado no se propaga a las instancias.
  - Todo el texto supera 4,5:1 y los bordes de control 3:1.
  - El amarillo de marca y el ámbar de atención solo tienen 1,4:1 entre sí, así que «Atención» se distingue por el triángulo, la palabra y la banda; el amarillo nunca indica estado.
- **Tipografía:** Manrope (OFL, ya local) en 11 estilos `V2/*`, con mínimo de 13 px. Se probó con texto ×1,3 y un alias de 34 caracteres.
- **Componentes:** botón, campo, aviso, insignia de estado, gusto, en común, categoría, avatar, navegación, cabecera con la marca y paso de misión.
- **Iconos:** se reutilizan los 44 existentes (trazo 1,7) y se añaden 5 con el mismo trazo: info, check-circle, x-circle, pause y loader.
- **Movimiento:** solo cuando explica un cambio:
  - pulsado, 80 ms;
  - elegir un gusto, 120 ms, cancelando la transición anterior;
  - paso completado, 160 ms;
  - lente al conectar, 240 ms, una vez;
  - con movimiento reducido, cambio instantáneo o fundido ≤ 100 ms.

## Pantallas (datos de ejemplo ficticios)

Los datos de ejemplo son: Luna y Mateo, SOFA 2026 en demo académica, gustos reales del catálogo, el código K7Q 4M2, la palabra COMETA y el recibo R-0003.

1. **Identidad y avatar:**
   - Cabecera uva con la vista pública.
   - Alias público con aviso de privacidad del correo.
   - Las piezas se previsualizan sobre el avatar actual.
   - Acción fija al alcance del pulgar.
2. **Selección de intereses:** búsqueda en 264 gustos, familias como pestañas con contador, gustos con +/✓ y bandeja fija con el total y la acción.
3. **Encuentro activo:**
   - Las dos personas con la lente y los gustos en común.
   - El punto y los pasos (hecho, tu turno, bloqueado).
   - La tarea actual (verificar a Mateo) va antes de lo demás.
   - El código personal es privado y se puede ocultar.
   - La palabra compartida va separada.
   - A 360×740, el paso completado se pliega para que la acción quede a la vista.
4. **Recibo:**
   - Documento con el código y el QR.
   - Coste cobrado una vez, saldo, lugar, validez y número.
   - Seguimiento Solicitado → Por entregar → Entregado (lo marca la persona encargada).
   - Aviso de caducidad y renovación sin nuevo cobro.

**Comprobaciones:** 390 a altura completa, 360×740 como viewport real, escritorio 1280 del encuentro y tema claro de Recibo e Intereses.

**Sin comprobar:** Safari, teclado y cámara físicos, lector de pantalla y rendimiento.

## Guía breve de implementación (cuando se apruebe)

1. **Marca:**
   - Lockup C inline en la cabecera, con `currentColor` para el trazo principal.
   - Clases para el anillo y la lente.
   - `c-icono-app.svg` como base del favicon y del manifest (`export_variants.py --web-icons`).
2. **Tokens:** copiar `recursos/tokens-v2.css` a `brand.css`, sustituyendo los valores, sin renombrar las clases existentes.
3. **Fragmentos Thymeleaf compartidos:** botón (jerarquía y estado), campo (con `aria-describedby` para la ayuda y el error), aviso, insignia, gusto, en común, categoría, avatar (composición SVG existente con marco), navegación y paso.
4. **Por grupos de pantallas:** entrada (identidad y gustos) → encuentro → recibo y beneficios.
5. **Verificación:** al final, recorrido integrado y la suite pertinente.

**Dependencias funcionales:**

- **Ninguna obligatoria.** Todo representa estados ya existentes: disponibilidad, propuesta bilateral, llegadas, verificación personal, confirmación, recibo de 24 h renovable sin cobro y entrega por encargado.
- **Solo en el cliente:** ocultar el código privado, plegar el paso completado y la bandeja «Revisar».
- **Cambia un recurso:** el tinte de los cristales de `avatars/accesorios/gafas.svg` modifica un recurso existente; es solo visual.

## Decisiones que necesitan respuesta

1. ¿Adoptar el concepto C como marca de PING? Es un cambio de identidad, con búsqueda de marcas registradas pendiente.
2. AGENTS.md pide conservar ACTIVE salvo defecto. El encuentro V2 cambia su composición: ¿se autoriza?
3. ¿Se acepta modificar el SVG de las gafas del catálogo de avatares?

## Skills utilizadas

- **`logo-design`** (kaankiziltug/logo-design-skill, commit `734090317a21`, v1.4.5, MIT):
  - Se instaló solo en este proyecto y **sin la biblioteca de 1 432 logotipos de terceros**.
  - Se leyó el `SKILL.md` completo y las referencias de rediseño, pruebas, tipos de marca, sistema de identidad, construcción SVG y tipografía.
  - Se usaron `render_png.py` (con Chrome) y `svg_audit.py`.
  - Se omitió la pausa tras los conceptos, por autorización explícita del encargo.
- **`figma-use`, `figma-generate-library` y `figma-generate-design`** (oficiales): variables, componentes con propiedades, booleanas y pantallas.
- **`impeccable`:** se aplicaron las reglas de `craft-floor` ya leídas, sin ejecutar sus scripts. No se cargaron otras skills.
