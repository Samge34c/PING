# Rendimiento backend · continuación de PR #53

## Alcance autorizado antes de cambiar código

Samuel solicita esta continuación sobre `perf/render-response@2346585d0c047ed113686aa7fb15060ffe52f198`; rama propia `perf/backend-roundtrips`, worktree `C:\Users\river\PING-perf-backend`. No se atribuye aprobación a Julián. La rama remota se obtuvo y coincide con ese SHA. El clon habitual permanece en `deploy/render-demo@40f2247`; frontend tiene su worktree independiente.

Problema del Issue: reducir trabajo y consultas SQL en navegación, exclusivamente con Render Free y Aiven Free, sin modificar funciones. Primero reproducir y contar; elegir solo 2–3 oportunidades justificadas. Candidatos: /connect construye dashboard antes de redirigir; Beneficios filtra catálogo global y carga encuentros completos para preguntar si hay Saga pendiente; ACTIVE vuelve a autorizar/leer la misma pista para texto y revisión. Son hipótesis hasta medir.

Escritura limitada a backend `src/main/java`, pruebas Java pertinentes y esta carpeta. Reservados a Claude templates, CSS/JS, imágenes/fuentes, `PublicResourceTests.java`, nuevas pruebas frontend y `docs/performance/frontend`. Se conservan POM, Dockerfile, despliegue, workflows, AGENTS, migraciones y documentación central. Sin caché global/de sesión, cambios de TLS, índices, reglas o datos reales. No se accede a Aiven/Render de producción ni se despliega.

Los 323 tests Java y 43 Python de PR #53 son resultados históricos de la base, no evidencia de este trabajo. Las mediciones nuevas distinguirán llamadas Java, SELECT SQL, filas/materialización y tiempo HTTP; no se equipararán con viajes de red ni con latencia Render/Aiven. Registros completos fuera de Git; solo resumen sanitizado aquí.

## Verificación y coordinación

Skills seleccionadas: ping-efficient-work, systematic-debugging y verification-before-completion; sin instalaciones ni skills visuales. No hay Tandem disponible en las herramientas de esta sesión; los límites de archivos y la base común permiten avanzar sin el puente. Antes de cada entrega se revisará el diff contra la base y se informarán los cambios de contrato relevantes a frontend, si los hubiera.

Estado: tres mejoras implementadas y verificadas; clean verify completo aprobado. Pendiente integración y despliegue por separado. Commit/push y PR en borrador autorizados; no merge ni despliegue.

Issue asociado: https://github.com/Samge34c/PING/issues/54 (alcance leído y autorizado por esta petición).

Referencia inicial antes de cambiar producción: 6 pruebas HTTP/JDBC, 5 fallos y 0 errores. Cuatro fallos demostraron trabajo evitable. El quinto era una expectativa incorrecta de la nueva prueba: la persona sí puede ver su propia pista en el formulario. Se corrigió esa expectativa y se repitió únicamente la reproducción de ACTIVE contra el controlador anterior: falló por las dos lecturas autorizadas en lugar de una (recognition-before-corrected.log). Las mediciones SQL originales siguen siendo válidas. /connect OFFERED: 7 SELECT/49 filas de encuentros; ACTIVE: 8/49; inactivo: 7/49. Beneficios SOFA: 9 SELECT, 98 filas de encuentros y 26 beneficios (solo 2 visibles). ACTIVE: 16 SELECT y dos llamadas a hints.other. Datos: 48 conexiones terminales extra y 24 beneficios de otro evento; H2 aislado. Log privado before-sql.log. Se inspeccionaron los accesos de Hub/Mi Ping/historial y planos para priorizar; no se afirma que estén completamente optimizados. FloorplanService ya separa metadata de image_data. Se acotó la implementación a las tres oportunidades reproducidas.

## Unidad 1 · /connect acotado

Consulta paginada del identificador OFFERED/ACTIVE más reciente, con el mismo orden/predicado que el historial anterior. Si existe, redirección inmediata; sin encuentro se construyen solo participante, evento, held=false y firma que consume la plantilla. No se cambian templates ni controles de pertenencia del interceptor/destino.

Después: OFFERED 3 SELECT y 1 fila de encuentro; ACTIVE 3/1; inactivo 5 SELECT y 0 filas de encuentros. Antes: 7/49, 8/49 y 7/49 respectivamente. Se evitan historiales y formularios que esa ruta no usa. 22 pruebas dirigidas aprobadas (ConnectReadWorkTests, MatchLiveStateTests, AccountSecurityTests), 0 fallos/errores/omitidas. Registros privados: connect-after.log. El tiempo HTTP de una ejecución local no es una comparación estadística ni latencia de Render.

## Unidad 2 · Beneficios y predicados

El catálogo se filtra en SQL a activos del evento y genéricos; conserva locales primero y orden por ID. Los indicadores de completada/promoción y finalización pendiente se agrupan en una proyección agregada fresca de una sola operación para GET. El POST no reutiliza esa proyección: mantiene su consulta fresca de pendiente después de adquirir el bloqueo del evento, así como débito, requestKey y snapshots existentes.

Mismo escenario SOFA: 9→7 SELECT, 98→1 filas de encuentros consumidas por JDBC y 26→2 beneficios materializados. El predicado pendingCompletion independiente mantiene 1 SELECT, con 49→1 filas de resultado. Una primera versión intermedia usaba dos predicados separados (8 SELECT/2 filas); la medición justificó agruparlos antes de guardar esta unidad. Esto no cuenta filas que MySQL examina internamente ni paquetes de red.

Verificación final de esta unidad: 83 pruebas dirigidas, 0 fallos/errores/omitidas; BenefitsReadWorkTests, EventExperienceTests, RewardTests, EventBenefitTests, VerificationTests, VariableRewardTests y MatchCompletionSagaTests. Ambas orientaciones, datos ajenos y cambios de bandera entre operaciones comprobados. Registros privados benefits-final.log. Primera unidad respaldada en remoto: 4f6d66a.

## Unidad 3 · pista autorizada una vez por petición

ACTIVE obtenía la misma pista con dos llamadas a RecognitionHintService.other: una para revisión y otra a través de DemoRecognitionHints para texto. Ahora usa un único resultado autorizado para ambos atributos. Se conserva la comprobación fresca de pertenencia, ACTIVE y aceptación bilateral; una nueva petición vuelve a leer la pista. El helper original permanece disponible, sin cambiar otros consumidores.

Antes/después: 2→1 llamadas Java a hints.other, 16→14 SELECT y 4→3 filas de encuentros consumidas. Misma solicitud, datos y perfil de seguridad. La reproducción corregida falla antes por 2 llamadas; después pasan 8 pruebas dirigidas (RecognitionReadWorkTests y RecognitionHintTests), 0 fallos/errores/omitidas. Comprueba dos cuentas, aceptación parcial/bilateral, cambio de pista entre peticiones, cancelación y una cuenta ajena. Registros privados recognition-before-corrected.log y recognition-after-corrected.log.
## Resultado y verificación final

| Petición / operación | SELECT antes→después | Filas de encuentros consumidas antes→después |
|---|---:|---:|
| Conectar con OFFERED | 7→3 | 49→1 |
| Conectar con ACTIVE | 8→3 | 49→1 |
| Conectar sin encuentro vigente | 7→5 | 49→0 |
| Beneficios SOFA | 9→7 | 98→1 |
| Pista en ACTIVE | 16→14 | 4→3 |
| Predicado de finalización pendiente | 1→1 | 49→1 |

Beneficios además evita materializar 24 filas ajenas: 26→2. En ACTIVE, hints.other pasa de 2→1 llamadas Java. Son tres unidades; el predicado forma parte de Beneficios. No son porcentajes ni segundos de mejora de la aplicación pública.

`clean verify` ejecutado una sola vez al cierre con las fuentes de `977e6258327a584eb7c6226b81d6a30a39773be8`, finalizado el 2026-10-25 a las 23:17:39 -05:00: **330 pruebas, 0 fallos, 0 errores, 0 omitidas; BUILD SUCCESS y JAR generado**. Coinciden resumen Maven y 42 informes XML de Surefire. Los siete tests nuevos se añaden a los 323 de la base; no se atribuye a esta ejecución el resultado histórico de Python. Las únicas modificaciones posteriores a la prueba completa son documentación exclusiva de esta carpeta.

Comando: `./mvnw.cmd -B --no-transfer-progress -Dspring.profiles.default=test -Dbackend.phase=final -DargLine=-javaagent:<mockito-core-5.23.0.jar local> clean verify`. Windows, Temurin 21.0.12 y Maven Wrapper; agente Mockito local sin modificar POM. H2 aislado, correo interceptado en memoria y HTTP real en loopback. No se ha arrancado ni detenido MySQL o la aplicación habitual.

Protecciones cubiertas por la suite: cuenta/roles/revocación, CSRF y pertenencia, matching/desempates, ambas aceptaciones/llegadas y verificación, confirmación/Saga, canjes/idempotencia, snapshots y planos. Las pruebas nuevas comprueban dos sesiones independientes, cuenta/evento ajenos y refresco entre peticiones. La densidad histórica y algunos estados se preparan con SQL sintético en H2; no se presentan como un recorrido manual completo ni como un ensayo sobre MySQL/Aiven.

El contador observa ejecuciones SELECT en JDBC y filas recorridas por ResultSet.next. No cuenta filas examinadas internamente, paquetes TCP o viajes físicos a MySQL. Los checkouts/commits del registro tampoco equivalen a conexiones de red. Se conservaron muestras HTTP individuales, pero no son un benchmark de latencia: no tienen DNS/TLS y no permiten extrapolar segundos en Render. La proyección agrupada reduce trabajo y operaciones; no se ha medido un plan de ejecución en MySQL.

Resultados completos fuera de Git: `%LOCALAPPDATA%/PING/performance-backend/`, incluidos full-final.log y full-final-surefire.zip. [evidence.json](evidence.json) contiene recuentos, comandos, procedencia, huellas SHA256 de logs y fuentes, además de los fallos de preparación/expectativa corregidos. No contiene cookies, tokens, correos de verificación ni valores privados.

Límites: no hay benchmark nuevo autenticado en Render, Aiven ni iPhone. La suspensión y recursos de Render Free permanecen. No se propone índice sin EXPLAIN, no se instala caché y no se cambian reglas funcionales. Se conserva el clon habitual en deploy/render-demo@40f2247, limpio; ningún archivo reservado a Claude o compartido se modificó. [Handoff de integración](HANDOFF.md).