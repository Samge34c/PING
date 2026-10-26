# Rendimiento backend · continuación de PR #53

## Alcance autorizado antes de cambiar código

Samuel solicita esta continuación sobre `perf/render-response@2346585d0c047ed113686aa7fb15060ffe52f198`; rama propia `perf/backend-roundtrips`, worktree `C:\Users\river\PING-perf-backend`. No se atribuye aprobación a Julián. La rama remota se obtuvo y coincide con ese SHA. El clon habitual permanece en `deploy/render-demo@40f2247`; frontend tiene su worktree independiente.

Problema del Issue: reducir trabajo y consultas SQL en navegación, exclusivamente con Render Free y Aiven Free, sin modificar funciones. Primero reproducir y contar; elegir solo 2–3 oportunidades justificadas. Candidatos: /connect construye dashboard antes de redirigir; Beneficios filtra catálogo global y carga encuentros completos para preguntar si hay Saga pendiente; ACTIVE vuelve a autorizar/leer la misma pista para texto y revisión. Son hipótesis hasta medir.

Escritura limitada a backend `src/main/java`, pruebas Java pertinentes y esta carpeta. Reservados a Claude templates, CSS/JS, imágenes/fuentes, `PublicResourceTests.java`, nuevas pruebas frontend y `docs/performance/frontend`. Se conservan POM, Dockerfile, despliegue, workflows, AGENTS, migraciones y documentación central. Sin caché global/de sesión, cambios de TLS, índices, reglas o datos reales. No se accede a Aiven/Render de producción ni se despliega.

Los 323 tests Java y 43 Python de PR #53 son resultados históricos de la base, no evidencia de este trabajo. Las mediciones nuevas distinguirán llamadas Java, SELECT SQL, filas/materialización y tiempo HTTP; no se equipararán con viajes de red ni con latencia Render/Aiven. Registros completos fuera de Git; solo resumen sanitizado aquí.

## Verificación y coordinación

Skills seleccionadas: ping-efficient-work, systematic-debugging y verification-before-completion; sin instalaciones ni skills visuales. No hay Tandem disponible en las herramientas de esta sesión; los límites de archivos y la base común permiten avanzar sin el puente. Antes de cada entrega se revisará el diff contra la base y se informarán los cambios de contrato relevantes a frontend, si los hubiera.

Estado: tres oportunidades medidas y verificadas con pruebas dirigidas; pendiente el control completo de cierre. Commit/push y PR en borrador autorizados; no merge ni despliegue.

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