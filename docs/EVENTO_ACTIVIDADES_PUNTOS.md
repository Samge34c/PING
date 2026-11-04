# Evento · Actividades y puntos (DEC-064)

Solicitud de Samuel del 4 de noviembre de 2026. Rama `fix/evento-actividades-puntos`, creada desde el commit que está publicado en Render (`5b11db1`). No se atribuye aprobación a Julián. Integrado después sobre el SHA Live `191522f` en `fix/evento-actividades-live` (renumerado de DEC-063 a DEC-064, porque DEC-063 ya existe en esa base).

## Qué abre la pestaña Evento

`/me/event` redirige según el evento de la sesión:

- **Evento con guía SOFA** (`EventExperienceCatalog`, solo «SOFA 2026») → `/events/{id}/hub`.
- **Cualquier otro evento** → `/events/{id}/plan`.
  - **Sin encuentro activo:** plano del evento con el selector de puntos.
  - **Con encuentro ACTIVE:** «Tu punto de encuentro», con el plano y el punto guardados al activar (snapshot) y el botón «Volver al encuentro».

## Origen de los datos

| Dato | Fuente |
|---|---|
| Catálogo (evento estructurado) | Filas `event_activities` del evento, con `active` y sin `archived`: título, instrucciones y `reward_points` (0–100). Son las mismas filas que usa `EncounterService.assign`. |
| Catálogo (evento con lista anterior) | Líneas únicas de `events.encounter_activities`. Todas comparten la recompensa común `EncounterService.LEGACY_REWARD_POINTS` (10), que es la misma constante que usa la asignación. |
| Actividad asignada | Snapshot del encuentro ACTIVE (`matches.activity_title`, `activity`, `reward_points`). Muestra el importe que se acreditará, aunque la organización edite después la actividad. |

No se usan los textos fijos de SOFA (`ActivityCatalog`) en ningún caso. Si un evento con lista anterior no tiene líneas, aparece el estado vacío.

## Comportamiento

- La sección «Actividades y puntos» va antes del plano. Arriba se añade el acceso «Ir al plano del evento».
- Durante un encuentro, el enlace «Ver todas las actividades» queda bajo el título. Las indicaciones de llegada y el plano se mantienen en su sitio, y el catálogo aparece después, antes de «Volver al encuentro».
- En el Hub SOFA, la sección se coloca entre la portada y «Encuentra tu mundo».
- **Tarjetas:** título, descripción (solo si existe y no repite el título) y la etiqueta «+N puntos», o «Sin puntos» si vale 0.
- **«Tu actividad asignada»:** se destaca primero y se retira del resto de la lista si coincide con una actividad del catálogo, para no duplicarla.
- **Regla de acreditación:** una sola vez, a cada persona.
  - Política 2: cuando ambos registran su llegada, verifican sus códigos y confirman la actividad.
  - Política 1 (histórica): cuando ambos confirman la actividad.
- **Comprobación:** «La actividad se verifica y confirma desde tu encuentro», con enlace a `/matches/{id}`. No hay carga de fotos ni una sección de pruebas.
- El catálogo es de solo lectura: no tiene formularios, no cambia la asignación ni permite reclamar puntos.
- El acceso a la página no cambia (`AccessPolicy`: quien gestiona el evento, el dueño de la participación o la demo).

Sin cambios de reglas, Saga, códigos, privacidad, snapshots de planos, migraciones ni dependencias.

## Verificación

Pruebas dirigidas en `EventActivityGuideTests` (H2, datos aislados):

- **Evento con tres actividades:** 15, 25 y 40 puntos, más una actividad pausada y otra archivada.
- **Evento de control:** una actividad de 70 puntos, que no debe aparecer en el primero.
- **Sin encuentro:** el catálogo aparece antes del plano, sin la pausada, la archivada ni la del otro evento, y sin formularios.
- **Encuentro activo:**
  - La asignada aparece una sola vez y destacada.
  - «Ver todas las actividades» queda antes de las indicaciones de llegada.
  - Se conservan las indicaciones y «Volver al encuentro».
  - `/matches/{id}/plan` muestra lo mismo.
- **Evento sin actividades:** estado vacío, con el plano accesible.
- **Lista anterior:** la regla común muestra el valor efectivo de la constante, sin textos de SOFA.

Regresiones ejecutadas: `FloorplanViewTests`, `FloorplanTests`, `FloorplanWebTests`, `EventExperienceTests`, `StructuredEncounterTests`, `VariableRewardTests`, `EncounterConfigurationTests`, `AccountSecurityTests` y `WorkIntegrationTests`.

## Capturas reales

Chrome headless por CDP, con datos aislados en H2 y política 2. Las capturas se guardan en [`screenshots/evento-actividades-puntos/`](screenshots/evento-actividades-puntos/), y `results.json` registra lo medido en cada una: título, tarjetas, puntos, desborde horizontal y errores JS.

| Captura | Qué muestra |
|---|---|
| `01-evento-390-oscuro` | Evento sin encuentro a 390 px (referencia principal). |
| `03-encuentro-390-oscuro` | Durante el encuentro: título, «Ver todas las actividades» e indicaciones. |
| `04-encuentro-390-oscuro-actividades` | Actividad asignada, otras actividades y «Volver al encuentro». |
| `06-evento-360-claro-completo` | 360 px, tema claro. |
| `07-encuentro-360-claro-actividades` | Asignada a 360 px, tema claro. |
| `08-vacio-390-oscuro-completo` | Estado vacío con el plano accesible. |
| `09-evento-escritorio-oscuro` | Escritorio (1366 px). |
| `10-encuentro-escritorio-claro` | Escritorio (1366 px). |
| `11-hub-sofa-390-oscuro-actividades` | Hub SOFA con actividades configuradas. |

En las capturas de página completa, la barra inferior fija (en móvil) y la barra lateral (en escritorio) quedan a la altura del viewport inicial. Es un efecto de la captura, no de la página. Ninguna captura tiene desborde horizontal ni errores JS. El arnés de captura y las cookies quedaron fuera de Git.

**Límites:** Chromium emulado no sustituye a un iPhone físico (Safari, áreas seguras, desplazamiento real).
