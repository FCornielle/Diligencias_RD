# Arquitectura de ruteo hibrido

Fecha de revision: 22 de julio de 2026.

## Decision

DiligenciaRD prioriza la alternativa menos congestionada usando todas las rutas devueltas por Google. Si no existe detalle de trafico, usa el menor tiempo estimado como fallback. El usuario todavia puede elegir explicitamente menor distancia.

La aplicacion no calculara rutas suponiendo que se exceden los limites de velocidad. Los tiempos y la navegacion deben modelar conduccion legal. Una velocidad maxima configurada por un proveedor describe el vehiculo o limita el modelo; no elimina las restricciones legales de la via.

## Por que el hibrido no reemplaza la ruta de Google

Navigation SDK recibe un `routeToken` generado por Routes API, Routes Preferred API o Route Optimization API. Una polilinea producida por TomTom, Mapbox o GraphHopper no se puede convertir en un token valido de Google.

Por eso el hibrido recomendado mantiene:

- Google Routes API para candidatos, trafico por tramo y tokens.
- Google Navigation SDK para instrucciones giro a giro y recalculo.
- Fuentes externas para incidentes, cierres, confianza y penalizaciones.
- Accidentes_RD, MOPC, INTRANT/OPSEVI y 911 como senales locales cuando exista acceso autorizado.

## Proveedores evaluados

### Google Routes

- `TRAFFIC_AWARE_OPTIMAL` realiza la busqueda mas completa con trafico actual.
- `TRAFFIC_ON_POLYLINE` clasifica tramos como normal, lento o tapon fuerte.
- Es la unica fuente actual que entrega el token consumido por la navegacion integrada.

### TomTom Routing

- Admite trafico actual, alternativas, obras y cierres segun disponibilidad.
- Puede devolver secciones de trafico, zona urbana, autopista, via sin pavimentar y limite de velocidad.
- Permite evitar autopistas y vias sin pavimentar.
- Requiere cuenta, clave, validacion de cobertura local y revision de licencia/costo.

### Mapbox Directions

- El perfil `driving-traffic` devuelve congestion, congestion numerica y cierres.
- Puede devolver `maxspeed` como anotacion beta.
- Produce hasta dos alternativas y requiere token propio.
- Sus rutas no se pueden enviar directamente al Navigation SDK de Google.

### GraphHopper

- Sus modelos personalizados permiten cambiar prioridad, influencia de distancia y velocidades usadas por el modelo.
- Es util para experimentar con preferencia de calles, pero no sustituye una fuente robusta de trafico vivo.
- Configurar una velocidad de calculo no autoriza ni recomienda conducir a esa velocidad.

## Flujo propuesto

1. Solicitar a Google rutas alternativas con trafico y `routeToken`.
2. Calcular por ruta porcentaje de tapon fuerte, tramos lentos, demora y distancia.
3. Consultar incidentes externos que intersecten cada polilinea y sigan vigentes.
4. Aplicar penalizaciones por accidente, cierre, inundacion, obra o baja confianza.
5. Elegir el token con menor congestion total; usar duracion como desempate.
6. Entregar ese token al Navigation SDK.
7. Recalcular cuando cambie el trafico o aparezca un incidente corroborado.

## Puntuacion inicial

```text
congestion = 2.0 * proporcion_tapon_fuerte
           + 1.0 * proporcion_trafico_lento
           + penalizacion_incidentes
           + penalizacion_cierres
```

Una ruta sin telemetria no se considera automaticamente libre. Se compara por tiempo solo cuando ninguna alternativa contiene detalle de trafico.

## Piloto recomendado

Antes de contratar un segundo proveedor:

1. Medir durante dos semanas 20 trayectos frecuentes de Santo Domingo con Google.
2. Registrar ETA, congestion, cierre reportado y tiempo real, sin guardar identidad del conductor.
3. Probar TomTom y Mapbox sobre la misma muestra.
4. Comparar cobertura, falsos cierres, minutos de error y costo por mil viajes.
5. Elegir un proveedor secundario solo si mejora de forma medible a Google mas incidentes locales.

La primera integracion externa de mayor valor para RD probablemente sea un feed de incidentes locales corroborados, no otro calculador completo de rutas.

## Fuentes

- [Google: trafico en polilineas](https://developers.google.com/maps/documentation/routes/traffic_on_polylines)
- [Google: rutas con routeToken](https://developers.google.com/maps/documentation/navigation/android-sdk/customize-route)
- [TomTom: Calculate Route](https://developer.tomtom.com/routing-api/documentation/tomtom-maps/v1/calculate-route)
- [TomTom: parametros comunes](https://developer.tomtom.com/routing-api/documentation/tomtom-maps/v1/common-routing-parameters)
- [Mapbox: Directions API](https://docs.mapbox.com/api/navigation/directions/)
- [GraphHopper: Custom Model](https://docs.graphhopper.com/openapi/section/mcp-endpoint)
