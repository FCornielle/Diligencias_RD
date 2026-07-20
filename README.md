# DiligenciaRD

Aplicación Android de navegación y planificación de diligencias para República Dominicana, estilo Waze. No recomienda el establecimiento más cercano, sino el que permite **terminar la diligencia en el menor tiempo total**:

```
T_total = T_trayecto + T_estacionamiento + T_espera + T_servicio
```

> Encuentra dónde, cuándo y por qué ruta completarás más rápido tu diligencia.

## Estructura

- `android/` — App Kotlin + Jetpack Compose. La demo corre sin cuenta GCP usando datos locales y rutas simuladas; Google Maps Platform queda como proveedor opcional futuro.
- `docs/` — Especificación funcional, plan de desarrollo y datos semilla

## Estado

Fase demo sin GCP: APK instalable con búsqueda local, comparador de sucursales por tiempo total, comparador de rutas (rápida / menos congestionada / corta legal) y navegación simulada. Todo corre en el dispositivo; sin backend, cuenta GCP ni claves API.

## Documentación

- [Especificación funcional y técnica](docs/especificacion.md)
- [Plan de desarrollo de la demo](docs/plan-demo.md)
- [Nota sobre Google Cloud y proveedores externos](docs/gcp-setup.md)
