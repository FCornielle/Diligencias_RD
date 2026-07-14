# DiligenciaRD

Aplicación Android de navegación y planificación de diligencias para República Dominicana, estilo Waze. No recomienda el establecimiento más cercano, sino el que permite **terminar la diligencia en el menor tiempo total**:

```
T_total = T_trayecto + T_estacionamiento + T_espera + T_servicio
```

> Encuentra dónde, cuándo y por qué ruta completarás más rápido tu diligencia.

## Estructura

- `android/` — App Kotlin + Jetpack Compose (Maps SDK, Places API New, Routes API, Navigation SDK)
- `docs/` — Especificación funcional, plan de desarrollo y datos semilla

## Estado

Fase demo: APK instalable con búsqueda, comparador de sucursales por tiempo total, comparador de rutas (rápida / menos congestionada / corta legal) y navegación giro a giro. Todo corre en el dispositivo; sin backend en esta etapa.

## Documentación

- [Especificación funcional y técnica](docs/especificacion.md)
- [Plan de desarrollo de la demo](docs/plan-demo.md)
- [Configuración de Google Cloud a costo cero](docs/gcp-setup.md)
