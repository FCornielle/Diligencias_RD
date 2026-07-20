# Plan de desarrollo: DiligenciaRD demo sin cuenta GCP

## Directriz vigente

El usuario no quiere crear ni usar una cuenta de Google Cloud Platform para continuar la demo.

Por tanto, el camino de desarrollo queda así:

- La demo debe correr con `MAPS_API_KEY=PLACEHOLDER_API_KEY`.
- No debe exigir proyecto GCP, facturación, cuota, APIs habilitadas ni credenciales.
- No debe llamar Google Places, Google Routes ni Google Navigation cuando no haya una clave real.
- La experiencia principal debe seguir demostrando búsqueda, comparación de sucursales, comparación de rutas y navegación.
- Google Maps Platform queda como proveedor opcional futuro, no como requisito de la demo.

## Estado actual

El repo contiene una app Android Kotlin + Jetpack Compose.

La app ahora tiene dos modos:

- **Modo local por defecto:** se activa con la clave placeholder. Usa datos semilla locales, rutas simuladas y una superficie de mapa dibujada en Compose Canvas.
- **Modo Google opcional:** se activa solo si `MAPS_API_KEY` parece una clave real de Google. Mantiene el camino anterior con Maps/Places/Routes/Navigation para una fase futura.

## Decisiones de producto para la demo

- El diferenciador se mantiene: recomendar por `T_total = trayecto + estacionamiento + espera + servicio`.
- Las esperas siguen siendo **Estimación general**, confianza baja, basada en establecimientos similares. Nunca se presentan como datos en vivo.
- Las rutas locales son aproximaciones para validar UX y lógica, no navegación legal real.
- La navegación sin GCP es una guía simulada con ETA y finalización estimada.
- El APK demo debe ser útil para enseñar el flujo, aunque no tenga tráfico real ni turn-by-turn real.

## Fase 1 - Demo local funcional

Implementado:

- Catálogo local de diligencias (`services_rd.json`).
- Heurísticas locales de espera (`wait_heuristics_rd.json`).
- Sucursales semilla locales en Santo Domingo (`demo_places_rd.json`).
- Búsqueda local por intención, texto o categoría.
- Comparador de sucursales con rutas locales aproximadas.
- Comparador de rutas con tres modos:
  - Ruta recomendada.
  - Menos congestionada.
  - Ruta corta legal simulada.
- Mapa Canvas local con marcadores de tiempo total.
- Navegación simulada cuando no hay GCP.

Criterio de salida:

- `./gradlew :app:compileDebugKotlin` compila.
- Abrir la app con `MAPS_API_KEY=PLACEHOLDER_API_KEY`.
- Tocar un chip como Bancos, Gobierno o Farmacias.
- Ver al menos cinco resultados cuando existan datos semilla.
- Ver desglose de trayecto, espera, servicio y total.
- Ver rutas y abrir la pantalla de navegación simulada.

## Fase 2 - Pulido de demo sin proveedores externos

Siguientes tareas recomendadas:

- Mejorar el Canvas para mostrar nombres cortos de vías y cluster de marcadores.
- Añadir más sucursales semilla por Santo Domingo, Santiago y principales provincias.
- Añadir filtros de cerrado/abierto basados en horarios locales semilla.
- Añadir programación simple: "ir ahora", "salir a una hora", "mejor hora de hoy".
- Persistir favoritos e historial localmente con Room.
- Preparar APK debug/release instalable sin credenciales.

## Fase 3 - Datos abiertos sin cuenta GCP

Opciones a evaluar sin exigir cuenta GCP:

- Dataset propio curado de establecimientos importantes.
- Importación manual/semiautomática de datos abiertos permitidos.
- OpenStreetMap solo si se respetan licencias, atribución y políticas de uso.
- Backend propio futuro para geocodificación/routing si el volumen crece.

Importante:

- No hacer scraping de Google Maps.
- No copiar "horas populares" de Google.
- No mezclar datos restringidos de Google con mapas no Google.
- No presentar rutas simuladas como rutas legales reales.

## Fase 4 - Integración real futura

Cuando el usuario decida usar un proveedor real, se puede elegir entre:

- Google Maps Platform, con cuenta GCP y restricciones.
- MapLibre + tiles propios o proveedor compatible.
- OSRM/Valhalla/GraphHopper autoalojado.
- Un proveedor comercial que no sea GCP.

La arquitectura debe mantener interfaces para:

- Búsqueda de lugares.
- Matriz de rutas.
- Rutas alternativas.
- Navegación.

Así la lógica de DiligenciaRD no queda casada con un proveedor.

## Git y despliegue

- Commits pequeños por hito.
- Autoría local del repo: `Fernando Cornielle <fernandocornielle@gmail.com>`.
- Sin coautoría automática.
- Push a `origin/main` cuando la compilación pase.

## Riesgos

- La demo local no tiene tráfico real.
- La ruta corta legal es simulada; sirve para UX, no para conducción real.
- Los datos semilla pueden quedar incompletos o desactualizados.
- Un proveedor externo futuro puede requerir cuenta, cuota, licencia o infraestructura propia.
