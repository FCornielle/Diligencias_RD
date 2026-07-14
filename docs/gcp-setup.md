# Google Cloud a costo cero — guía de configuración

Objetivo: habilitar Maps/Places/Routes/Navigation para DiligenciaRD **sin posibilidad de cobro**.

## Por qué es US$0

1. **Prueba gratuita de GCP**: al crear la cuenta de facturación nueva recibes US$300 de crédito por 90 días. Mientras la cuenta esté en modo prueba, Google **no puede cobrarte**: si el crédito se agota, los servicios se detienen; el cobro solo existe si haces "Activate full account" manualmente. No lo hagas.
2. **Nivel gratuito permanente por SKU** (precios de marzo 2025): cada API tiene llamadas gratis mensuales (~10,000 en SKUs Essentials, ~5,000 en Pro, ~1,000 en Enterprise; Navigation SDK ~1,000 destinos/mes). El desarrollo de la demo consume una fracción de eso.
3. **Topes de cuota diarios**: el candado real. Al alcanzar el tope, las llamadas **fallan** en vez de facturarse. Las alertas de presupuesto solo avisan; las cuotas sí bloquean.

## Pasos (una sola vez, ~20 minutos)

1. Entra a <https://console.cloud.google.com> con tu cuenta de Google.
2. Crea el proyecto `diligenciard`.
3. Activa la facturación aceptando la **prueba gratuita** (pide tarjeta solo para verificar identidad; no cobra en modo prueba).
4. En **APIs & Services → Library**, habilita:
   - Maps SDK for Android
   - Places API (New)
   - Routes API
   - Navigation SDK
5. En **APIs & Services → Credentials → Create credentials → API key**:
   - Application restrictions: **Android apps** → añade el paquete `com.diligenciard.app` + la huella **SHA-1 del keystore de debug** (te la genero yo con `gradlew signingReport` cuando el proyecto exista).
   - API restrictions: limita la clave a las 4 APIs de arriba.
6. En **APIs & Services → Quotas**, fija topes diarios:
   - Routes API (ComputeRoutes y ComputeRouteMatrix): ~300 solicitudes/día
   - Places API (New): ~300 solicitudes/día
   - Navigation SDK (destinos): ~30/día
   (Se pueden subir al momento si un día de pruebas lo necesita.)
7. En **Billing → Budgets & alerts**, crea un presupuesto de US$1 con alertas al 50/90/100% — solo como aviso extra.

## Dónde va la clave

En `android/local.properties` (archivo **ignorado por git**):

```properties
MAPS_API_KEY=AIza...
```

Nunca se commitea. El build la inyecta vía el plugin de secretos de Gradle.

## Cuando la app pase a producción pública

- Mover Routes/Places al backend con una clave de servidor (spec §22).
- Añadir App Check.
- Revisar SKUs y cuotas con el volumen real.
