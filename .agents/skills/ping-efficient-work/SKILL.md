---
name: ping-efficient-work
description: Usar en tareas de PING con exploración de código, múltiples comandos o varios pasos; no cargar para una pregunta sencilla sin investigación.
---

# Trabajo eficiente en PING

Skill propia del proyecto, versión 1.0.0. Autoría: proyecto PING, creada por solicitud del usuario. Licencia MIT, incluida en LICENSE. Sin dependencias de otras skills.

1. **Acota antes de leer.** Confirma copia, rama, petición y estado Git. Empieza con `rg` o `rg --files` sobre rutas pertinentes. Lee los archivos que explican la tarea y consulta el diff, incluidos los nuevos archivos relevantes. No vuelvas a leer contexto ya vigente sin una razón.
2. **Busca con intención.** En búsquedas normales excluye `target/`, `node_modules/`, cachés, binarios, skills vendorizadas y capturas/logs históricos. Usa rutas explícitas primero; no excluyas migraciones, tests ni archivos ocultos relevantes. Si buscas dentro de una skill o una evidencia, entra expresamente en esa ruta. La exclusión es del comando de búsqueda, no un borrado ni un cambio de Git.
3. **Selecciona instrucciones.** Consulta `docs/SKILLS_PING.md`; elige por el trabajo. Lee solo SKILL.md y las referencias que necesites. No encadenes subskills ni auditorías duplicadas. La identidad y los requisitos de PING prevalecen sobre recetas genéricas.
4. **Conserva evidencia.** Guarda logs completos de los comandos pertinentes en una ruta de trabajo fuera del código. Conserva comando, código de salida, fallos y ruta; resume lo útil sin ocultar errores ni perder el estado de un proceso todavía en ejecución. Si resumes con una tubería, conserva el código del comando original. Nunca registres secretos; conserva el fallo con valores sensibles omitidos.
5. **Reutiliza lo que funciona.** Comprueba y aprovecha servicios, herramientas, sesiones de demo y resultados vigentes. No reinicies MySQL, la aplicación o el entorno solo para empezar de cero. No repitas inspecciones ni pruebas sin cambios, fallos o incertidumbre nueva que lo justifiquen.
6. **Comprueba lo que afirmas.** Escoge evidencia proporcional a la afirmación. Un test Java no demuestra diseño; HTTP 200 no demuestra un flujo; un archivo local no prueba una descarga accesible. Identifica resultados históricos y su commit/estado. Investiga fallos antes de variar la solución.
7. **Deja continuidad.** Al pausar trabajo significativo, guarda una nota breve: copia/rama, cambios, servicios y estados activos, evidencia, bloqueos y siguiente paso. Mantén fuera del repositorio credenciales, sesiones y logs privados; no escribas grandes transcripciones.
8. **No simules ahorro.** No elimines requisitos, evidencia o errores para acortar el trabajo. No inicies subagentes, instales herramientas ni cambies configuración del agente automáticamente para ahorrar tokens. No atribuyas porcentajes de ahorro sin medición.
