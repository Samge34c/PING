# Handoff a integración (frontend)

**Rama y base:**
- Rama: `perf/frontend-mobile-load`.
- Base: `perf/render-response@2346585` (PR #53).
- Backend paralelo: PR #55 `perf/backend-roundtrips@66420ac`, leído sin integrar.

**Archivos de esta rama:**
- `templates/fragments/layout.html`: Bootstrap subconjunto y precarga de la fuente núcleo.
- `templates/home.html`: `srcset`.
- `templates/interests/selector.html`: fila compacta.
- `static/css/brand.css`: dos `@font-face`.
- Recursos nuevos en `static/fonts`, `static/img/sofa` y `static/vendor/bootstrap`.
- `PublicResourceTests.java` y `FrontendBundleTests.java`.
- `docs/performance/frontend/`.

**Sin intersección con la PR #55:** esa PR solo cambia `src/main/java`, sus pruebas y `docs/performance/backend/`.

**Contrato con backend:**
- `/connect` solo usa `participant`, `event`, `held`, `liveSignature` y `csrfToken`, más los atributos globales del layout. Es compatible con la retirada de historial, badges, promo y `recognitionHintForm` en esa ruta.
- Estos cambios no añaden atributos de modelo ni peticiones nuevas.
- No cambian `/me/benefits` ni la pista de ACTIVE.

**Al integrar:**
1. Unir ambas ramas desde la base común; no debería haber conflictos de archivos.
2. Ejecutar `clean verify` una vez sobre el resultado.
3. Comprobar que `python docs/performance/frontend/tools/bootstrap_subset.py --check` sigue al día. Si alguna rama añadió clases de Bootstrap a templates, JS o Java, regenerarlo. `FrontendBundleTests` lo detecta.
4. Actualizar `docs/BRAND_GUIDE.md`: se sirve un subconjunto generado de Bootstrap 5.3.8. Es documentación central; no se editó aquí.

**Decisiones abiertas** (ver README): compresión de HTML de páginas sin secretos, fragmento del editor de intereses para `/interests` y WOFF2 opcional.

**Pendiente:** medir en el iPhone la versión integrada y desplegada, con el servicio despierto, separando DNS/TLS y la espera del servidor.
