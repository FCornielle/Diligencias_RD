# Plan de desarrollo: DiligenciaRD (APK demo, costo US$0, sin nube)

## Contexto

DiligenciaRD es una app Android de navegación (estilo Waze) para República Dominicana que recomienda **dónde, cuándo y por qué ruta** completar una diligencia en el **menor tiempo total** (trayecto + estacionamiento + espera + servicio), no solo el destino más cercano. Existe una especificación funcional completa (26 secciones) aportada por el usuario.

**Decisiones tomadas con el usuario:**
- Primer entregable: **APK demo funcional** (mapa, búsqueda, comparador de sucursales con tiempo total heurístico, comparador de rutas y navegación). ML, datos pasivos, backend y panel admin vienen después.
- **Costo US$0 obligatorio**: se crea la cuenta GCP pero no se paga nada. Doble candado: (a) prueba gratuita de GCP (US$300 / 90 días, sin posibilidad de cobro hasta hacer upgrade manual) y (b) topes de cuota diarios por API para que el exceso falle en vez de facturarse.
- **Sin computación en la nube**: nada de Cloud Run ni backend en la demo. Todo corre en el celular; la app llama a las APIs de Google directamente con clave restringida al paquete + SHA-1.
- Pruebas: **teléfono Android físico** por USB.
- **Play Store pospuesto**: el APK instalable es suficiente por ahora.
- **GitHub**: repo remoto ya creado — `git@github.com:FCornielle/Diligencias_RD.git`. Los commits van **a nombre del usuario (Fernando Cornielle / fernandocornielle@gmail.com), sin línea de coautoría de Claude**, pusheados por SSH.

**Estado verificado de la máquina (Windows 11):**
- ✅ Node 22.16, Git 2.47
- ❌ Sin JDK, sin Android Studio, sin Android SDK, sin adb (se instalan en Fase 0)
- Python no se necesita en esta etapa (sin backend)

**Datos de APIs verificados (julio 2026):**
- Navigation SDK Android: GA, facturado por destino (~1,000 destinos gratis/mes). Exige `targetSdk` 33+; la versión 7.0+ del SDK exige `targetSdk` 36.
- `SHORTER_DISTANCE` en Routes API: **sigue experimental (pre-GA)** → obligatorio el fallback de la spec (alternativa con menor `distanceMeters`).
- Precios GMP (modelo marzo-2025): uso gratuito mensual por SKU (~10K llamadas Essentials, ~5K Pro, ~1K Enterprise). Con topes de cuota, en desarrollo el costo es US$0 garantizado.
- Routes API por REST acepta claves restringidas a apps Android enviando los encabezados `X-Android-Package` y `X-Android-Cert` — por eso la demo puede llamarla desde el celular sin backend y sin exponer una clave de servidor.

---

## Estructura del proyecto

En el directorio actual (`36 - Diligencia RD`), repo git conectado a `git@github.com:FCornielle/Diligencias_RD.git`:

```
android/            # App Kotlin + Jetpack Compose (todo el producto demo)
docs/               # Especificación, decisiones, semilla de heurísticas de espera
.gitignore          # Excluir local.properties, claves API, keystores, build/
```

Commits pequeños por hito, autoría `Fernando Cornielle <fernandocornielle@gmail.com>` (configurar `git config user.name/user.email` locales al repo), **sin coautoría de Claude**, push a `main` por SSH (verificar antes `ssh -T git@github.com`).

---

## Fase 0 — Preparación del entorno (días 1–2)

1. **JDK 17** (Temurin) — requerido por Android Gradle Plugin.
2. **Android Studio** (última estable) + Android SDK (API 36) + Platform Tools (adb).
3. **Teléfono físico**: activar opciones de desarrollador + depuración USB; verificar con `adb devices`.
4. **Cuenta Google Cloud a costo cero** (pasos guiados, los hace el usuario en el navegador):
   - Crear cuenta + proyecto `diligenciard`. Aceptar la **prueba gratuita** (US$300/90 días): mientras no se haga upgrade manual, Google no puede cobrar.
   - Habilitar APIs: Maps SDK for Android, Places API (New), Routes API, Navigation SDK.
   - **Una sola clave Android** restringida por paquete `com.diligenciard.app` + huella SHA-1 del keystore de debug, limitada a esas 4 APIs. No hay clave de servidor porque no hay servidor.
   - **Topes de cuota diarios** por API (p. ej. Routes ~300/día, Places ~300/día, Navigation ~30 destinos/día) para que sea imposible salir del nivel gratuito incluso después de la prueba. Alertas de presupuesto a US$1 como aviso extra.
5. **Git**: `git init`, configurar identidad del usuario, conectar remoto y primer push (docs + estructura).

**Criterio de salida:** `adb devices` ve el teléfono; proyecto Compose vacío corre en él; una llamada de prueba a Routes API (con los headers Android) devuelve una ruta en Santo Domingo; primer commit pusheado al repo del usuario.

## Fase 1 — Esqueleto de la app (semana 1)

App Kotlin + Compose, arquitectura MVVM sencilla (ViewModel + Repository; sin sobre-ingeniería):

- Mapa a pantalla completa (Maps SDK) con capa de tráfico activada.
- Ubicación actual (Fused Location Provider) + botón de centrar + flujo de permisos.
- Barra de búsqueda superior ("¿Qué diligencia necesitas hacer?") + chips de accesos rápidos (Bancos, Gobierno, Farmacias, etc.).
- Panel inferior deslizable (BottomSheet de Material 3).
- Tema claro/oscuro con la paleta de la spec (§18): azul oscuro, verde, ámbar, rojo.

**Criterio de salida:** la app corre en el teléfono y muestra el mapa con tráfico centrado en tu ubicación real en RD.

## Fase 2 — Búsqueda y establecimientos (semanas 1–2)

- Búsqueda por texto y categoría vía Places API (New) desde el dispositivo, con field masks mínimas (spec §22).
- Diccionario local de intenciones ES-RD → categorías/servicios ("renovar licencia" → INTRANT, "sacar acta" → JCE, "pagar impuestos" → DGII, banco, farmacia, supermercado…), como asset JSON — semilla de la futura tabla `Service` (spec §20).
- Radio expandible: 5 km → 15 km → más, hasta ≥5 candidatos (spec §9 paso 2).
- Filtros: cerrado permanente/temporal, cerrará antes de la llegada, duplicados.
- Marcadores con color por tiempo total (verde/amarillo/rojo/gris) y el tiempo dentro del pin.
- Tarjeta de establecimiento (spec §12): tiempos desglosados, horario, botones Ir ahora / Ver rutas / Llamar / Guardar.

## Fase 3 — Comparador de sucursales, todo en el celular (semanas 2–3)

Módulo Kotlin dentro de la app (sustituye al backend de la spec §21 en esta etapa; misma lógica, misma interfaz conceptual, portable a FastAPI después):

- Compute Route Matrix por REST (origen → hasta ~10 candidatos, `TRAFFIC_AWARE_OPTIMAL`) → hora estimada de llegada por sucursal → espera estimada **a esa hora** (spec §9 paso 5) → `T_total = trayecto + parking + espera + servicio` → penalización por incertidumbre `λ·U` → orden.
- **Motor de espera v0 = "Estimación general" (spec §6.3)**: tabla heurística empaquetada como asset (JSON) + Room para overrides: espera p50/p80 por categoría × día × franja horaria, con correcciones por quincena/fin de mes/festivos RD y cercanía a apertura/cierre. Siempre etiquetada "Confianza: baja · Basada en establecimientos similares". **Nunca se presenta como "en vivo".**
- Persistencia local con Room siguiendo el esquema de la spec §20 (`Place`, `Service`, `PlaceService`, `WaitEstimate`, `RouteCandidate`), respetando restricciones de Google (guardar `place_id`; no cachear rutas/coordenadas indefinidamente — usar `expires_at`).
- UI del comparador (spec §13): tabla de 3 mejores opciones + encabezado "Recomendamos X: ahorrarías ~N min" + conmutador de criterio (más rápido / conducir menos / esperar menos / más cercano).

## Fase 4 — Comparador de rutas (semana 3)

- Compute Routes por REST: `TRAFFIC_AWARE_OPTIMAL` + alternativas + petición separada con `SHORTER_DISTANCE` (fallback a la alternativa de menor `distanceMeters` si falla, por ser pre-GA).
- Indicadores por ruta (spec §10): retraso por tráfico (`duration − staticDuration`), proporciones jam/slow por tramos, puntuación `S_r` con los pesos por modo de la spec §10.3.
- UI: polilíneas por colores sobre el mapa + tarjetas de las 3 rutas (Recomendada / Corta legal / Menos congestionada) con descripción honesta (nunca vender "corta" como "más rápida").
- Restricción legal (spec §3.4): solo rutas devueltas por Google; ninguna lógica propia que sugiera maniobras.

## Fase 5 — Navegación giro a giro (semanas 3–4)

- Navigation SDK con el `routeToken` de la ruta elegida.
- Pantalla de navegación: próxima maniobra, ETA, distancia, y el diferenciador: **banda de "finalización de diligencia estimada"** (llegada + espera + servicio, spec §15).
- Detección de llegada → pantalla de llegada (spec §16, sin sesión pasiva todavía).
- Sugerencia de alternativa con botones grandes Cambiar / Mantener destino (sin cambio automático de sucursal en conducción).

## Fase 6 — Pulido y APK demo (semana 4)

- "Programar diligencia" simple: mejor hora del día vía Route Matrix con `departureTime` futuro + curva heurística de espera.
- Manejo de errores de la spec §23 (sin datos de espera, cierre antes de llegada, sin conexión).
- Atribuciones de Google Maps visibles (spec §22).
- Keystore de firma propio (fuera del repo, con respaldo) + **APK firmado instalable** (y AAB guardado para cuando llegue Play Store). Añadir la SHA-1 del keystore de release a la restricción de la clave API.

**Entregable: APK instalable en cualquier Android 8+ con el flujo completo: buscar → comparar sucursales → comparar rutas → navegar. Costo acumulado: US$0.**

---

## Fases futuras (cuando el usuario lo pida)

- **Play Store**: la cuenta personal exige prueba cerrada con 12 testers × 14 días; requerirá política de privacidad publicada, formulario Data Safety y ficha de tienda.
- **Backend FastAPI** (la lógica de Fase 3 ya quedará aislada en un módulo para portarla), consentimiento y datos pasivos (spec §7), motor predictivo v1 (spec §8.3), historial/favoritos con cuenta, panel admin.
- Nota: al mover las llamadas de Routes al backend se elimina la clave del APK, como pide la spec §22 para producción.

## Costos

- **US$0 garantizado en toda la fase demo**: prueba gratuita GCP (sin cobro posible hasta upgrade manual) + niveles gratuitos por SKU + topes de cuota diarios que rechazan el exceso en vez de facturarlo.
- Único gasto ya realizado por el usuario: cuenta de Play (US$25, pago único, no se usa aún).

## Riesgos principales

1. **`SHORTER_DISTANCE` pre-GA** puede cambiar/fallar → fallback ya diseñado (menor `distanceMeters`).
2. **Clave API dentro del APK**: aceptable en demo (restringida por paquete + SHA-1 y con topes de cuota); debe migrar a backend antes de un lanzamiento público real.
3. **Topes de cuota muy bajos** pueden cortar una sesión intensa de pruebas → ajustables al momento en la consola sin costo.
4. **Sin datos de espera reales**: la heurística v0 se muestra siempre con confianza baja; el valor pleno llega con datos pasivos + modelo (fase futura).
5. **ToS de Google**: nada de scraping de "horas populares", no mezclar contenido de Routes con mapas no-Google, atribuciones visibles, límites de almacenamiento — incorporados al diseño.
6. **Fin de la prueba gratuita (90 días)**: sin upgrade manual no hay cobro; los topes de cuota mantienen el uso dentro del nivel gratuito permanente después.

## Verificación (por fase)

- **F0:** `adb devices` lista el teléfono; app Compose vacía corre en él; llamada de prueba a Routes API devuelve una ruta en Santo Domingo; `git push` llega al repo `FCornielle/Diligencias_RD` con autoría del usuario.
- **F1–F2:** en el teléfono, buscar "farmacia" muestra ≥5 marcadores con colores y tarjeta con horario real.
- **F3:** el comparador devuelve ≥5 sucursales ordenadas por `T_total`, con la espera calculada a la hora de llegada y desglose visible trayecto/espera/servicio (criterios de aceptación §25).
- **F4:** las 3 rutas se pintan en el mapa con métricas distintas; forzar el fallback de `SHORTER_DISTANCE` y comprobar que degrada bien.
- **F5:** navegación real en el teléfono (trayecto corto real en RD) usando el `routeToken`, con recálculo y detección de llegada.
- **F6:** instalar el APK firmado en un teléfono limpio y completar el flujo entero sin depurador ni PC.
