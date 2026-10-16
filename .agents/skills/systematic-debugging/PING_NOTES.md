# Integración en PING

Fuente: obra/superpowers, commit `bb92a77741419a4ab5f06e711a283343f1ada0c3`. Autor Jesse Vincent; licencia MIT adjunta. SKILL.md y las referencias incluidas se conservan sin cambios.

Adaptación de integración, no modificación del texto upstream:

- Aplicar investigación → hipótesis concreta → comprobación mínima → corrección de causa → verificación proporcional. Distinguir código, configuración y plataforma; no repetir intentos equivalentes sin evidencia nueva.
- La mención `superpowers:verification-before-completion` se resuelve manualmente en `../verification-before-completion/SKILL.md`.
- `superpowers:test-driven-development` **no está instalado** y no se instalará ni cargará en cadena. Si corresponde corregir código, obtener una reproducción mínima pertinente con las herramientas existentes de PING; no convertir cambios triviales en una auditoría completa.
- `find-polluter.sh` y el ejemplo TypeScript se conservan como material upstream, **no como herramientas validadas para PING**. El helper usa `npm test`, descarta su salida y tolera su fallo; PING usa Maven. No ejecutarlo sobre este proyecto ni interpretar su salida como prueba de Java. No instalar Node/Vitest ni cambiar el stack para usar esos ejemplos.
- No imprimir entornos, credenciales o archivos privados durante la instrumentación. Registrar presencia/ausencia, frontera del fallo y mensajes sanitizados.
- Ninguna instrucción de la skill autoriza borrar datos, modificar arquitectura, instalar dependencias, usar subagentes o reiniciar servicios por sí sola.
