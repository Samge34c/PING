# Integración continua de PING — DEC-053

Un solo workflow: [.github/workflows/bootstrap.yml](../.github/workflows/bootstrap.yml). Los filtros anteriores solo admitían bases históricas, por lo que las PR recientes no disparaban Actions. Ahora `pull_request` admite cualquier base y sus eventos normales, incluidas PR en borrador. Una actualización cancela la ejecución anterior de esa PR; no hay segunda ejecución por `push`. Las PR antiguas necesitan una actualización para disparar el workflow; esta PR no modifica sus ramas.

| Check | Ejecución | Alcance |
|---|---|---|
| Java 21 / Linux | Ubuntu24.04, Temurin21, Maven Wrapper, `clean verify` una vez | Perfiles declarados por cada prueba; `spring.profiles.default=test` solo como fallback. H2 efímero. No imponer `spring.profiles.active=test`, porque cambiaría pruebas de seguridad. Sin servicio MySQL/credenciales externas. |
| Tools / Windows | Windows2025, Python3.12 estándar, PowerShell5.1 y7 | Parser en ambos motores, una ejecución de unittest Python y dobles nativos en ambos motores. No repite Java ni la suite Python. |

La caché Maven de setup-java usa `pom.xml` y `.mvn/wrapper/maven-wrapper.properties` para su clave, además del entorno que incorpora la acción. No se cachean bases, sesiones o informes. Sin caché pip: no hay paquetes Python que instalar.

## Windows: tres evidencias distintas

1. **Parser:** Test-Scripts.ps1 analiza recursivamente los scripts, incluidos tests; no ejecuta operaciones.
2. **Dobles nativos:** Test-Native.ps1 compila una pequeña fixture C# con el compilador .NET Framework del runner. En una carpeta NUEVA temporal con espacios/á/ñ copia Common.ps1 y ping_ops.py. Prueba resolución de raíz, argv vacío/comillas/backslashes/metacaracteres/Unicode con Start-Process real, detección Java21 por stderr y rechazo de JRE, llamada PowerShell→Python→cliente nativo, código de error1045/redacción y restauración del entorno. El doble no abre sockets ni bases; comprueba que la contraseña sintética solo llega en MYSQL_PWD y nunca en argumentos/salida. Limpia únicamente su directorio temporal. No usa Java/MySQL/Wi-Fi personales ni Pester.
3. **Operación:** un check verde NO demuestra arranque real, backup/restore MySQL en Windows, firewall/Wi-Fi, selección de interfaz, cámara, certificado, Safari o acceso desde teléfono. Esas comprobaciones siguen en el PC/celular de Samuel con [WORK_PRUEBA_WINDOWS.md](WORK_PRUEBA_WINDOWS.md). Los ensayos MySQL anteriores fueron Linux y siguen documentados como tales.

Para ejecutar los controles nativos manualmente desde la raíz en Windows:

```powershell
.\tools\windows\Test-Scripts.ps1
python -m unittest discover -s tools/windows/tests -p "test_*.py" -v
.\tools\windows\tests\Test-Native.ps1
```

No ejecutar Test-Native.ps1 como una prueba operativa ni interpretar sus datos ficticios como una configuración personal.

## Seguridad y fuentes verificadas

Permiso único `contents: read`; checkout con `persist-credentials: false`, sin `pull_request_target`, secretos de producción, commits, push, deploy ni permisos administrativos. Cero artefactos publicados: los logs Maven completos permanecen en RUNNER_TEMP y solo se muestran recuentos/identificadores de pruebas, sin cuerpos, propiedades ni stdout privado. El código de salida de Maven conserva el fallo del check. Los tests de herramientas usan datos sintéticos y sus informes no imprimen credenciales. Las cachés de dependencias no son un respaldo ni una fuente confiable de datos de producto.

Acciones oficiales consultadas y tags resueltos mediante git ls-remote (20 octubre2026, Bogotá), fijadas a commit completo:

| Acción | Major / SHA verificado | Documentación oficial |
|---|---|---|
| checkout |v7 / `3d3c42e5aac5ba805825da76410c181273ba90b1`|[actions/checkout](https://github.com/actions/checkout)|
| setup-java |v6 / `b843ebf9b913146b38da2eaa7aeddf76074ae1ad`|[actions/setup-java](https://github.com/actions/setup-java)|
| setup-python |v7 / `5fda3b95a4ea91299a34e894583c3862153e4b97`|[actions/setup-python](https://github.com/actions/setup-python)|

Se leyó también action.yml de cada commit: Node24 e inputs utilizados confirmados. Sintaxis/eventos/concurrencia/permisos: [referencia oficial de workflows](https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax). No se eligen hashes de ejemplos sin comprobarlos. Las actualizaciones futuras requieren revisar la fuente y actualizar conscientemente el pin.

La prueba nativa reprodujo una diferencia real de PowerShell7.5+: la llamada .NET con null conserva un valor vacío. Restore-PingEnvironment ahora elimina explícitamente entradas originalmente ausentes mediante el proveedor Env:, manteniendo valores presentes/vacíos. [Semántica oficial de Microsoft](https://learn.microsoft.com/en-us/powershell/module/microsoft.powershell.core/about/about_environment_variables?view=powershell-7.5). Regresión de ausencia/valor en ambos motores y de vacío persistente en7.5+, donde está soportado; no se modifica entorno User/Machine.

No se cambian protecciones de ramas. Después de revisar la PR, un administrador puede elegir los checks `Java 21 / Linux` y `Tools / Windows` como obligatorios; esa configuración queda fuera de este bloque.

## Resultado de esta entrega

Ejecución REAL de GitHub Actions [39525525640](https://github.com/Samge34c/PING/actions/runs/39525525640), evento `pull_request`, intento1, commit `2da924425a63aaf56964e9b8cde231c62a8d3700`, resultado **success** en ambos trabajos:

- [Java / Linux](https://github.com/Samge34c/PING/actions/runs/39525525640/job/118979118910): `clean verify` correcto, **267 pruebas,0 fallos/errores/omitidas**; restauración de caché Maven y Wrapper comprobada.
- [Herramientas / Windows](https://github.com/Samge34c/PING/actions/runs/39525525640/job/118979119326): **18 pruebas Python correctas**, ambos parsers y todos los casos nativos correctos en Windows PowerShell **5.1.26100.33438** y PowerShell **7.6.6**, con Python3.12.10. Sin Java/MySQL reales ni operaciones contra bases.

Las ejecuciones previas conservaron fallos reales: [39524859886](https://github.com/Samge34c/PING/actions/runs/39524859886) detectó una fixture ROOT no normalizada frente a rutas Windows resueltas; [39525075732](https://github.com/Samge34c/PING/actions/runs/39525075732) detectó el código de salida residual del caso negativo del harness; [39525260200](https://github.com/Samge34c/PING/actions/runs/39525260200) reprodujo el defecto de restauración del entorno PowerShell7. Se corrigieron sus causas, conservando aserciones y controles. Los logs originales siguen en esas ejecuciones.

Localmente se ejecutaron18 pruebas Python y después una prueba dirigida de la fixture; no se repitió la suite Java. PowerShell se comprobó en el runner Windows, sin instalarlo aquí. El cierre documental posterior puede ser HEAD; su ejecución y SHA exactos se consultan en la [PR50 en borrador](https://github.com/Samge34c/PING/pull/50). Respaldos y siguiente acción exacta: [continuidad única](ENCUENTRO_CONFIGURABLE_LLEGADA.md). La operación real en PC/celular sigue pendiente según el alcance anterior.
