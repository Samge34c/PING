# Handoff a frontend / integración

Base común: `perf/render-response@2346585d0c047ed113686aa7fb15060ffe52f198` (PR #53). Backend: `perf/backend-roundtrips`, Issue #54. Worktree: `C:\Users\river\PING-perf-backend`. Sin merge ni despliegue.

## Qué integrar

Unidades verificadas y respaldadas, en orden:

1. `4f6d66a`: Conectar consulta solo el ID OFFERED/ACTIVE más reciente antes de redirigir; sin encuentro no construye el historial ni formularios del dashboard.
2. `84d2e50`: catálogo de Beneficios filtrado en SQL, indicadores de completada/pendiente agrupados para GET y predicado fresco de pendiente para POST.
3. `977e625`: ACTIVE reutiliza una sola lectura autorizada de pista para texto y revisión dentro de la petición.

Las pruebas y evidencia quedan en esta rama. Al integrar con el trabajo de Claude, conservar ambos conjuntos desde la base común; estos commits no editan recursos frontend ni configuración compartida. La integración requiere revisión, no está realizada aquí.

## Contrato que necesita conocer Claude

- `/connect` conserva `participant`, `event`, `held`, `experience` y `liveSignature` cuando presenta la pantalla. Redirige si hay OFFERED/ACTIVE. La plantilla actual y sus fragmentos no usan historial, badges, promo ni recognitionHintForm en esta ruta; ya no se preparan allí. Si frontend empieza a necesitarlos, pedir el dato concreto sin volver a llamar al dashboard completo.
- `/me/benefits` mantiene `benefits`, `claims`, `benefitEvent`, `completionPending`, `promoUnlocked` para experiencias, `requestKey` y participante. Mismo orden: beneficios locales antes de genéricos, luego ID. No cambiar el POST para confiar en indicadores del HTML: su validación fresca bajo bloqueo sigue siendo obligatoria.
- Encuentro mantiene `recognitionHint` y `recognitionRevision` con el mismo servicio de autorización. No almacenar pistas, estados o permisos en sesión/JS para evitar consultas; la petición siguiente debe detectar cambios y revocación.
- No hay migraciones, dependencias, variables ni ajustes de despliegue nuevos. No se tocaron avatares, planos, matching, llegadas, verificación, Saga, saldo o entrega.

## Evidencia y límites

Comparación local sobre H2 aislado: Conectar con encuentro 7/8→3 SELECT; Beneficios SOFA 9→7; ACTIVE 16→14. Son SQL ejecutados en el backend, no solicitudes de recursos frontend ni tiempos de red Render/Aiven. Detalles, procedencia y validación final en README.md y evidence.json de esta carpeta.

No se ha usado Aiven de producción, modificado datos reales ni desplegado Render. La comparación autenticada en el iPhone queda para la versión integrada y efectivamente desplegada: mismas cuentas/datos, servicio despierto y varias navegaciones; separar DNS/TLS de espera del servidor. La suspensión de Render Free permanece.