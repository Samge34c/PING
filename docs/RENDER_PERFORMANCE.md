# Rendimiento de la demostración gratuita

Base: `deploy/render-demo@40f22473d35fbe8164e9ba7ef5e025051858426e`. Rama de trabajo: `perf/render-response` (DEC-060).

## Issue propuesto y alcance autorizado

**Problema:** la navegación pública se percibe lenta en el iPhone. Samuel solicita reducir la espera manteniendo Render Free, Aiven Free y todas las funciones.

**Criterios:** evitar consultas repetidas por petición y trabajo innecesario al cargar recursos públicos, conservar la revocación en la siguiente petición privada, no cambiar puntos/canjes/roles ni usar caché compartida de contenido personal. Comprobar firmas del estado en vivo y el flujo existente con dos identidades. Publicación y despliegue se distinguen de la verificación local.

## Diagnóstico inicial

Las mediciones iniciales de `urllib` (3,5–5 s para el HTML) incluían el establecimiento de HTTPS desde el entorno de trabajo. Un desglose posterior de `curl` dio Inicio: TLS 3,024 s, primer byte 3,333 s, total 3,382 s; Login: TLS 3,051 s, primer byte 3,291 s, total 3,291 s. La diferencia posterior a TLS incluye red y servidor; **no mide exclusivamente el tiempo de PING**. No atribuir aquellos 3 segundos al procesamiento de la aplicación ni extrapolarlos al iPhone.

Lectura estática: `AuthSupport.current()` consulta `AccountService.user()` en cada llamada, incluidos varios consejos/controladores dentro de una sola petición; `AccessPolicy` también se ejecuta para los recursos estáticos públicos. En `DemoController`, algunas vistas vuelven a cargar el evento y todo el historial para obtener datos del encuentro ya leído. Las comprobaciones antes/después determinarán qué cambios se conservan.

## Estado

Implementación verificada localmente. No se ha cambiado el servicio público ni se han usado sus credenciales o datos privados. Los resultados históricos no se atribuyen a este incremento.

## Cambios y comparación reproducible

La identidad se vuelve a consultar en cada petición privada, pero se reutiliza dentro de esa petición para la misma autenticación. Un cambio de autenticación dentro de la petición descarta la lectura anterior; fuera de una petición no se conserva. No hay caché en sesión ni global. Los controladores privados siguen comprobando pertenencia, roles y `auth_epoch`.

`AccessPolicy` omite el acceso a cuentas solamente cuando Spring ha resuelto un `ResourceHttpRequestHandler`: los recursos públicos del classpath. La cadena de Spring Security sigue ejecutándose. Planos privados, códigos personales y endpoints de aplicación siguen siendo controladores y no utilizan esa salida.

La propuesta/ACTIVE reutiliza el encuentro ya autorizado y el evento ya leído para su firma de estado. No necesita consultar y transformar todo el historial. Mi Ping sigue mostrando el historial y los detalles anteriores; solo se elimina la segunda lectura idéntica de su evento. No se modifican Saga, matching, disponibilidad, importes, confirmaciones, QR, recibos, planos ni diseños D01–D14.

Pruebas HTTP sobre la misma aplicación, con H2 aislado y una cuenta con sesión (sin modo demo heredado):

| Trabajo contado por petición | Antes | Después |
|---|---:|---:|
| Lecturas frescas de cuenta en Inicio | 2 | 1 |
| Lecturas frescas de cuenta en Mi cuenta | 3 | 1 |
| Lecturas frescas de cuenta en `/me/state` | 1 | 1 |
| Lecturas de cuenta por CSS, JS o favicon público | 1 | 0 |
| Carga de historial completo en propuesta/ACTIVE | 1 | 0 |
| Lecturas del evento en propuesta/ACTIVE | 2 | 1 |

Las lecturas de cuenta llaman al método que ejecuta su SELECT; no son el total de consultas ni de viajes de red de una página. Los dos últimos renglones se comprobaron en ambos estados. La firma HTML coincide con el estado consultado por el polling para ambas orientaciones, aceptación parcial/bilateral, llegadas, verificaciones, cambio de evento, código propio y política histórica.

Se reprodujeron primero los fallos de rendimiento con las nuevas aserciones sobre el código anterior: 9 pruebas dirigidas, 7 fallidas y ninguna con error de infraestructura en la ejecución final de referencia. Después: 25 pruebas dirigidas aprobadas y **`clean verify`: 323 pruebas, 0 fallos, 0 errores, 0 omitidas; JAR generado**, sobre las fuentes finales de esta rama. Incluye revocación entre peticiones, restablecimiento de contraseña, dos cuentas concurrentes, autorización por evento, CSRF, QR, confirmaciones bilaterales, Saga y no duplicación del débito/entrega.

Entorno local: Temurin 21.0.13, Maven Wrapper 3.9.16. Aquí Mockito necesita cargar su agente al inicio de la JVM de pruebas porque no puede adjuntarlo después; se pasó por `-DargLine=-javaagent:<ruta-local-de-mockito-core-5.23.0.jar>`, sin cambiar POM ni dependencias de producción. Maven usa el proxy y almacén de confianza del entorno, fuera del repositorio. La suite incluye H2, no constituye un ensayo nuevo sobre Aiven ni sobre MySQL real.

Resumen de evidencia sanitizada: [render-performance-evidence.json](render-performance-evidence.json). Logs completos de este trabajo fuera del código en `ping-perf-evidence/`; no contienen credenciales de Aiven o Render porque no se solicitaron ni usaron.

## Activación y comprobación en Render

1. Mantener Free, la base de Aiven, el certificado y las variables existentes. No crear otro servicio ni modificar MySQL.
2. En el servicio existente, Settings → Build & Deploy → Branch: `perf/render-response`. Guardar y usar Manual Deploy → Deploy latest commit si no empieza automáticamente. El SHA de entrega debe coincidir con la rama remota. Alternativamente, integrar la PR mediante revisión/autorización y desplegar la rama de destino. No usar `main`, que no es la versión actual.
3. Esperar a Live y comprobar inicio de sesión, Mi Ping/Conectar, propuesta/ACTIVE, mapa y recibo en el iPhone. El reinicio puede cerrar sesiones; volver a entrar conserva cuentas, avatares y saldos en la misma base.
4. Comparar varias navegaciones con el servicio ya despierto, mismo teléfono, red, cuenta y datos. No usar solo el primer acceso ni declarar un porcentaje por reducir llamadas Java.
5. Reversión de código: desplegar `deploy/render-demo@40f22473d35fbe8164e9ba7ef5e025051858426e`. No hay migración ni transformación de datos que revertir.

**Límites:** no se ha medido todavía una mejora de segundos en la versión pública autenticada ni probado Safari físico. Una comprobación pública adicional con conexión HTTPS reutilizada dio Login 0,396 s e Inicio 0,204 s desde este entorno, antes del despliegue del parche. No demuestra el rendimiento de las rutas privadas. Render Free conserva su suspensión por inactividad, CPU/RAM limitadas y la latencia hacia Aiven; este trabajo no promete eliminarlas.
