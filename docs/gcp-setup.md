# Nota sobre Google Cloud y proveedores externos

## Decisión actual

La demo de DiligenciaRD no requiere cuenta de Google Cloud Platform.

El proyecto debe poder compilar y ejecutarse con:

```properties
MAPS_API_KEY=PLACEHOLDER_API_KEY
```

Ese valor vive en `android/local.defaults.properties` y permite usar el modo local:

- Búsqueda con datos semilla empaquetados en assets.
- Rutas simuladas dentro del dispositivo.
- Mapa dibujado en Jetpack Compose Canvas.
- Navegación simulada con ETA y finalización estimada.

## Qué queda desactivado sin GCP

Cuando no hay una clave real, la app no inicializa ni debe llamar:

- Places API.
- Routes API.
- Maps SDK.
- Navigation SDK.

## Integración futura opcional

Si más adelante se decide usar Google Maps Platform, la clave real debe ir en `android/local.properties`, archivo ignorado por git:

```properties
MAPS_API_KEY=AIza...
```

En ese caso habría que revisar nuevamente:

- Proyecto GCP.
- Restricción de clave por paquete y SHA-1.
- APIs habilitadas.
- Cuotas.
- Atribuciones.
- Costos y límites.

Esa integración ya no forma parte del plan vigente de demo.
