# Probar esta versión desde el iPhone · 10 minutos

Rama `feature/auditoria-recorridos`, worktree `C:\Users\river\PING-cierre`. Entorno **solo de este PC y su Wi-Fi**: no se publica en Internet, no usa túneles, Render ni Aiven. Todos los datos son ficticios.

## 1. Qué ejecutar

Doble clic en:

```
C:\Users\river\AppData\Local\PING\prueba-esta-version\PROBAR-ESTA-VERSION.cmd
```

1. Inicia la **MySQL de ensayo** de esa carpeta (`127.0.0.1:3318`, solo accesible desde este PC, base `ping_prueba_iphone`).
2. Llama al arranque guiado de esta copia (`C:\Users\river\PING-cierre\INICIAR-PING.cmd`). Compila si hace falta, prepara HTTPS y abre el panel.

Para terminar: `DETENER-PRUEBA.cmd`, en la misma carpeta. Detiene el PING de esta copia y la MySQL de ensayo, y conserva los datos.

**No es tu PING habitual.** `PING-movil` usa su propia carpeta privada, la MySQL84 del puerto 3306 y el puerto 8081. Esta copia usa otra configuración cifrada (`%LOCALAPPDATA%\PING\launcher\5dfd85adee5a467f`), otra CA, otra base y el puerto **8095** (más el 8096 para preparar el iPhone). Pueden convivir, pero no hace falta abrir las dos.

## 2. Comprobar que se ejecuta esta versión

- El arranque guiado calcula el SHA-256 de `src/main`, `pom.xml` y el Maven Wrapper. Si no coincide con el JAR registrado, **recompila antes de abrir**; nunca sirve un JAR anterior. En la ventana verás «Compilación vigente comprobada por hashes» (reutiliza) o «Preparando la versión actual» (recompila).
- Comprobado el 08/11/2026 22:20 con el árbol limpio en `2a6f431`: huella de fuentes = huella del JAR en ejecución, hash del JAR correcto, `https://192.168.1.9:8095/login` 200 validado con la CA local y migración V25 aplicada.
- **En el teléfono** se distingue a simple vista: al entrar con `personal@ping.test` llegas a **Organización** (antes caía en «Únete a tu evento»), y en Beneficios el botón «Solicitar canje» aparece justo debajo de «Te alcanza».
- Si cambias de rama o editas código, el siguiente doble clic recompila. Para saber qué commit tienes: `git -C C:\Users\river\PING-cierre log -1 --oneline` (el árbol debe estar limpio).

## 3. Abrirlo desde el iPhone

1. iPhone y PC en el **mismo Wi-Fi**. El panel muestra la dirección actual (hoy `https://192.168.1.9:8095`); si cambia la IP, el panel se actualiza solo.
2. **Una sola vez, aunque ya prepararas tu PING habitual:** esta copia tiene su propia CA. En el panel, escanea **«Preparar el iPhone · una sola vez»**, descarga el certificado, instálalo y actívalo en *Ajustes → General → Información → Ajustes de confianza de certificados*. Los pasos completos están en [WORK_PRUEBA_WINDOWS.md](../../WORK_PRUEBA_WINDOWS.md#cámara-del-iphone-https-local-desde-el-25-de-octubre-de-2026). Para distinguir los dos perfiles, compara la huella que muestra el panel.
3. Escanea el QR de acceso HTTPS del panel. Safari debe abrir PING **sin aviso**. Sin la CA de confianza, la cámara de PING no funciona.
4. En el PC, para el papel de personal, abre la misma dirección. Si el navegador avisa del certificado, usa **«Confiar en este PC»** en el panel: afecta solo a tu usuario de Windows.

Si el teléfono no llega: el perfil de red de este PC es **Público**. Hay reglas de entrada previas que permiten Java (Adoptium) y Python 3.14 en ese perfil; no las creé yo. Si aun así no conecta, revisa el aislamiento de clientes del router o la VPN, o usa **«Permitir Wi-Fi privado»** del panel (pide UAC solo para esa regla).

## 4. Cuentas (exclusivamente de prueba, locales)

| Correo | Papel | Estado |
|---|---|---|
| `organizacion@ping.test` | Administración general | Creó «Feria de prueba PING» y «Ensayo de cobertura» |
| `personal@ping.test` | Personal del evento | Permiso **solo** en «Feria de prueba PING»: revisa tareas y entrega canjes |
| `participante-a@ping.test` | Participante (iPhone) | Verificada; todavía **no** está en ningún evento |
| `participante-b@ping.test` | Participante (segundo teléfono o ventana privada) | Igual que A |
| `cobertura-1@ping.test`, `cobertura-2@ping.test` | Pasada automatizada | Solo en «Ensayo de cobertura». No las uses |

**Contraseñas:** en `C:\Users\river\AppData\Local\PING\prueba-esta-version\cuentas.env`. Esa carpeta solo es accesible para tu usuario de Windows y está fuera de Git. No las copies en Issues ni en capturas.

**Evento preparado: «Feria de prueba PING»** (Abierto). Tiene dos lugares (Plazoleta de la fuente y Cafetería norte) y la actividad «Dos canciones», que da +15 puntos, requiere **revisión del personal** y pregunta «¿Qué canciones se recomendaron?». Recompensas: «Café o aromática» (10 puntos, con imagen) y «Sticker de PING» (5 puntos, sin imagen, con la imagen genérica). Entrega en «Mesa de PING». El código y el QR del evento aparecen en Organización → «Código y QR».

## 5. Guion de 10 minutos · dos participantes y personal

Tú facilitas. **Lee solo el objetivo, sin decir qué botón pulsar.** Si alguien pide ayuda, pregunta primero «¿Qué buscarías?» y anota la pista que des, tal cual.

| Tiempo | Quién | Objetivo (léelo tal cual) |
|---|---|---|
| 0:00–2:30 | A (iPhone) y B | «Entra en la Feria de prueba con este código o QR y deja tu perfil listo para conocer gente.» Dales correo y contraseña de su cuenta. |
| 2:30–6:30 | A y B | «Encontrad a alguien, quedad en el lugar que os toque y terminad lo necesario para que los dos recibáis puntos.» |
| 2:30–6:30 | Personal (PC) | «Cuando te llegue algo para revisar, la primera vez pide que lo corrijan; la segunda, apruébalo.» |
| 6:30–9:00 | A | «Usa tus puntos en algo que te alcance y enséñale al personal lo que mostrarías para recogerlo.» |
| 6:30–9:00 | Personal | «Entrega lo que te muestren.» |
| 9:00–10:00 | Todos | «¿Qué fue lo más confuso?» · «Explícame para qué sirve PING con tus palabras.» |

La versión larga, con la tarea de organización y la hoja de observación, está en [PRUEBA_APRENDIZAJE.md](PRUEBA_APRENDIZAJE.md).

## 6. Resultados esperados

1. **Entrar:** código o QR del evento → «Entra a tu cuenta» (con «Tu entrada a Feria de prueba PING») → «Tu identidad en el evento» (alias y avatar) → «¿Qué te gusta?» → Mi Ping del evento. La pista de reconocimiento es opcional.
2. **Encuentro:**
   - Conectar → activar disponibilidad → buscar → propuesta → aceptan los dos.
   - La tarjeta «Ahora» guía: ir al lugar → «Ya llegué» → códigos personales (escanear el QR de la otra persona o escribir su código) → hacer la actividad y enviar la respuesta.
   - Personal ve «Revisar tareas · 1 pendiente» y pide corrección con un comentario. La pareja ve el comentario y «Reenviar corregida».
   - Personal aprueba y los dos confirman. Resultado: «Conexión completada» y **+15 a cada persona, una sola vez**.
   - Al enviar a revisión no llega ningún punto; los puntos llegan tras la aprobación y las dos confirmaciones, en cualquier orden.
3. **Canje:**
   - Beneficios muestra 15 puntos; en el café, «Te alcanza · Quedarás con 5» y «Solicitar canje» visible sin bajar.
   - Al solicitar aparece el recibo con su código y QR, y el saldo baja a 5.
   - Personal: Organización → «Entregar un canje» → escanear o escribir → «Confirmar entrega» → «Entrega confirmada. No se cobraron puntos adicionales.»
   - El recibo de A pasa a «Entregado».
4. **Cámara del iPhone** (pendiente físico): la cámara pide permiso la primera vez y empieza con la trasera; «Usar cámara frontal» cambia de cámara; se lee el QR a 30–50 cm; al volver de otra app la cámara está cerrada; con el teclado abierto se siguen viendo la nota para el personal y el código manual.

## 7. Cómo registrar un fallo

Anota cada fallo en una fila, al momento. Una fila por problema, aunque se repita.

| # | Hora | Dispositivo · navegador | Rol (sin contraseña) | Tarea | Qué intentaba | Qué pasó (texto literal de la pantalla) | Qué esperaba | ¿Se recuperó? ¿Cómo? | Captura |
|---|---|---|---|---|---|---|---|---|---|
| 1 | | | | | | | | | |

- **Capturas:** recorta o tapa los códigos de recibo, los QR personales, los correos reales y la barra de direcciones si muestra un enlace con `token=`.
- **Registros:** no los copies en documentos. Anota la hora; el detalle está en el panel → Registros (`aplicacion.log`), en la carpeta privada.
- Clasifica cada fallo: fallo reproducido (puedes repetirlo con los mismos pasos), riesgo de comprensión (la persona dudó o tomó otro camino) o comprobación pendiente.
- Para repetir la prueba desde cero, crea cuentas nuevas desde «Crear cuenta» en el teléfono. El correo de verificación llega a «Abrir mensajes de ensayo» del panel del PC, no a Internet.
