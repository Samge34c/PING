# Contexto y recursos de la demo SOFA

SOFA 2026 es el contexto de una **simulación académica**. Ping no fue usado oficialmente por SOFA. Promociones y recompensas presentadas son ejemplos demo.

Fuentes oficiales consultadas el 14 de octubre de 2026:

- Evento, Corferias Bogotá y fechas 8–12 de octubre: https://www.enelsofa.com/sofa2026/index.php?page=info
- Zona Gamer (18), Comunidades SOFA (3 nivel 2), Cosplay Parade (8 nivel 2), SOFA Stage (17): https://www.enelsofa.com/sofa2026/index.php?page=geografia
- Stage Cultura Gamer (17) y Meet & Greet (23): https://www.enelsofa.com/sofa2026/index.php?page=programacion
- Referencia visual de posición relativa de pabellones/entradas: https://www.enelsofa.com/sofa2026/assets/img/img-covers/plano_geografia_desktop.jpg

**No se incorpora ninguna fotografía, logo SOFA ni plano oficial al proyecto:** no se comprobó una licencia de redistribución. El plano oficial se consultó como referencia factual; la interfaz dibuja un esquema propio simplificado de los pabellones relevantes. Coordenadas aproximadas, sin escala, distancias o instrucciones de recorrido. Stage y Cultura Gamer comparten el marcador del pabellón 17. Música usa ese punto como afinidad demo; no afirma que sea un escenario musical.

Cartel: composición propia con diagonales, sin logo ni arte oficial SOFA. Mapa y minimapa comparten el SVG propio `templates/experience/venue.html`; mapa: `templates/experience/map.html`, `static/js/event-map.js`; contexto y zonas: `EventExperienceCatalog`. Todos los recursos se sirven localmente; las fuentes anteriores solo documentan procedencia, no se solicitan al ejecutar la app.

«Cómo reconocerme» es texto opcional escrito por la sesión demo seleccionada, persistido mediante V12. No hay pistas precargadas por alias ni se restablecen al borrar. Solo el otro participante de un match ACTIVE con ambas aceptaciones puede leerlo; no se publica en listado, admin, propuesta, historial ni JSON de estado. La palabra sigue siendo el mecanismo de reconocimiento. `PING-GAMER` es una promoción ficticia, sin descuento, canje ni consumo de puntos; se muestra tras completar una conexión. El snack mantiene su canje real de 20 PTS.

## Hub Festival Social (DEC-031)

El Hub usa la ilustración original generada durante la segunda composición aprobada por Samuel: dos personas ficticias en una convención. No representa asistentes, actividad real ni arte oficial de SOFA. Se conservó el recurso original; no se regeneró. La copia local servida es WebP de 1536×1024, optimizada sin añadir dependencias. Prompt y hashes original/servido: `HUB_ILLUSTRATION_PROMPT.txt` y `HUB_ART_PROVENANCE.json`. Los SVG pequeños para tecnología, música, cine y Gamer proceden de geometría original del prototipo; no de ilustraciones oficiales. Manrope conserva su licencia local existente. La composición del resto de pantallas no cambia.
