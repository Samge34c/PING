# Integración en PING

Fuente: obra/superpowers, commit `bb92a77741419a4ab5f06e711a283343f1ada0c3`. Autor Jesse Vincent; licencia MIT adjunta. SKILL.md se conserva sin cambios.

Adaptación de integración, no modificación del texto upstream:

- Evidencia antes de afirmaciones: identificar la afirmación, elegir la comprobación adecuada, ejecutarla completa y revisar salida y código de salida.
- La comprobación debe ser proporcional al cambio y demostrar precisamente lo que se declara. No repetir toda la suite para verificar documentación o instalación de instrucciones.
- Un test Java no valida apariencia; HTTP 200 no valida aceptación bilateral; un archivo local no valida una descarga accesible. Revisar cada capa con evidencia pertinente.
- Resultados históricos pueden citarse como históricos, con su commit/estado y límites. No presentarlos como una nueva ejecución.
- No crear fallos en PING para probar esta skill. Las menciones upstream a PR, commits, delegación o red-green no autorizan esas acciones ni instalan otras skills.
