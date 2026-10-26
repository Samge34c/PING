# Rendimiento backend · continuación de PR #53

## Alcance autorizado antes de cambiar código

Samuel solicita esta continuación sobre `perf/render-response@2346585d0c047ed113686aa7fb15060ffe52f198`; rama propia `perf/backend-roundtrips`, worktree `C:\Users\river\PING-perf-backend`. No se atribuye aprobación a Julián. La rama remota se obtuvo y coincide con ese SHA. El clon habitual permanece en `deploy/render-demo@40f2247`; frontend tiene su worktree independiente.

Problema del Issue: reducir trabajo y consultas SQL en navegación, exclusivamente con Render Free y Aiven Free, sin modificar funciones. Primero reproducir y contar; elegir solo 2–3 oportunidades justificadas. Candidatos: /connect construye dashboard antes de redirigir; Beneficios filtra catálogo global y carga encuentros completos para preguntar si hay Saga pendiente; ACTIVE vuelve a autorizar/leer la misma pista para texto y revisión. Son hipótesis hasta medir.

Escritura limitada a backend `src/main/java`, pruebas Java pertinentes y esta carpeta. Reservados a Claude templates, CSS/JS, imágenes/fuentes, `PublicResourceTests.java`, nuevas pruebas frontend y `docs/performance/frontend`. Se conservan POM, Dockerfile, despliegue, workflows, AGENTS, migraciones y documentación central. Sin caché global/de sesión, cambios de TLS, índices, reglas o datos reales. No se accede a Aiven/Render de producción ni se despliega.

Los 323 tests Java y 43 Python de PR #53 son resultados históricos de la base, no evidencia de este trabajo. Las mediciones nuevas distinguirán llamadas Java, SELECT SQL, filas/materialización y tiempo HTTP; no se equipararán con viajes de red ni con latencia Render/Aiven. Registros completos fuera de Git; solo resumen sanitizado aquí.

## Verificación y coordinación

Skills seleccionadas: ping-efficient-work, systematic-debugging y verification-before-completion; sin instalaciones ni skills visuales. No hay Tandem disponible en las herramientas de esta sesión; los límites de archivos y la base común permiten avanzar sin el puente. Antes de cada entrega se revisará el diff contra la base y se informarán los cambios de contrato relevantes a frontend, si los hubiera.

Estado: diagnóstico inicial; todavía sin optimización de producción. Commit/push y PR en borrador autorizados; no merge ni despliegue.

Issue asociado: https://github.com/Samge34c/PING/issues/54 (alcance leído y autorizado por esta petición).

Reproducción válida antes de producción: 6 pruebas HTTP/JDBC, 5 fallos de aserciones de trabajo evitable y 0 errores. /connect OFFERED: 7 SELECT/49 filas de encuentros; ACTIVE: 8/49; inactivo: 7/49. Beneficios SOFA: 9 SELECT, 98 filas de encuentros y 26 beneficios (solo 2 visibles). ACTIVE: 16 SELECT y dos llamadas a hints.other. Datos: 48 conexiones terminales extra y 24 beneficios de otro evento; H2 aislado. Log privado before-sql.log. Hub/Mi Ping/historial requieren sus datos visibles; el servicio de planos ya separa metadata de image_data, así que no se introduce una optimización artificial ahí.

## Unidad 1 · /connect acotado

Consulta paginada del identificador OFFERED/ACTIVE más reciente, con el mismo orden/predicado que el historial anterior. Si existe, redirección inmediata; sin encuentro se construyen solo participante, evento, held=false y firma que consume la plantilla. No se cambian templates ni controles de pertenencia del interceptor/destino.

Después: OFFERED 3 SELECT y 1 fila de encuentro; ACTIVE 3/1; inactivo 5 SELECT y 0 filas de encuentros. Antes: 7/49, 8/49 y 7/49 respectivamente. Se evitan historiales y formularios que esa ruta no usa. 22 pruebas dirigidas aprobadas (ConnectReadWorkTests, MatchLiveStateTests, AccountSecurityTests), 0 fallos/errores/omitidas. Registros privados: connect-after.log. El tiempo HTTP de una ejecución local no es una comparación estadística ni latencia de Render.

## Unidad 2 · Beneficios y predicados

El catálogo se filtra en SQL a activos del evento y genéricos; conserva locales primero y orden por ID. Los indicadores de completada/promoción y finalización pendiente se agrupan en una proyección agregada fresca de una sola operación para GET. El POST no reutiliza esa proyección: mantiene su consulta fresca de pendiente después de adquirir el bloqueo del evento, así como débito, requestKey y snapshots existentes.

Mismo escenario SOFA: 9→7 SELECT, 98→1 filas de encuentros consumidas por JDBC y 26→2 beneficios materializados. El predicado pendingCompletion independiente mantiene 1 SELECT, con 49→1 filas de resultado. Una primera versión intermedia usaba dos predicados separados (8 SELECT/2 filas); la medición justificó agruparlos antes de guardar esta unidad. Esto no cuenta filas que MySQL examina internamente ni paquetes de red.

Verificación final de esta unidad: 83 pruebas dirigidas, 0 fallos/errores/omitidas; BenefitsReadWorkTests, EventExperienceTests, RewardTests, EventBenefitTests, VerificationTests, VariableRewardTests y MatchCompletionSagaTests. Ambas orientaciones, datos ajenos y cambios de bandera entre operaciones comprobados. Registros privados benefits-final.log. Primera unidad respaldada en remoto: 4f6d66a.
