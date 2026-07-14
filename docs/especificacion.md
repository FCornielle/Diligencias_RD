# Especificación funcional y técnica: DiligenciaRD

## 1. Visión del producto

**DiligenciaRD** será una aplicación Android de navegación y planificación de diligencias para República Dominicana, con una experiencia visual similar a Waze.

La aplicación no recomendará simplemente el establecimiento más cercano. Debe encontrar la alternativa que permita al usuario **terminar su diligencia en el menor tiempo total**.

Ejemplo:

* Banco A: 18 minutos conduciendo + 55 minutos esperando.
* Banco B: 27 minutos conduciendo + 12 minutos esperando.
* Recomendación: Banco B, con un ahorro estimado de 34 minutos.

### Propuesta de valor

> Encuentra dónde, cuándo y por qué ruta completarás más rápido tu diligencia.

---

# 2. Principio principal de cálculo

La aplicación deberá calcular:

```
T_total = T_trayecto + T_estacionamiento + T_espera + T_servicio
```

Donde:

* `T_trayecto`: duración de conducción con tráfico.
* `T_estacionamiento`: tiempo estimado para estacionarse.
* `T_espera`: tiempo esperado antes de ser atendido.
* `T_servicio`: duración aproximada de la diligencia.

La recomendación principal debe minimizar `T_total`, no solamente la distancia o la duración del viaje.

---

# 3. Modos de recomendación

La aplicación mostrará cuatro modos:

## 3.1 Diligencia óptima

Selecciona automáticamente:

* La mejor sucursal.
* La mejor hora.
* La mejor ruta.
* El menor tiempo total para completar la diligencia.

## 3.2 Ruta más rápida

Prioriza exclusivamente el menor tiempo de conducción considerando el tráfico actual.

## 3.3 Ruta menos congestionada

Prioriza una ruta estable, con menor proporción de tramos clasificados como:

* `SLOW`
* `TRAFFIC_JAM`

Puede aceptar algunos minutos adicionales si reduce significativamente la exposición a congestionamientos.

Google Routes API puede entregar tráfico por intervalos de la ruta, clasificados como `NORMAL`, `SLOW` o `TRAFFIC_JAM`.

## 3.4 Ruta corta legal

Prioriza la menor distancia recorrida, aunque:

* Utilice calles locales.
* Tenga más giros.
* Pase por calles residenciales.
* Sea menos cómoda que una avenida principal.
* Tenga una velocidad teórica inferior.

Google ofrece la opción experimental `SHORTER_DISTANCE`, que prioriza la distancia por encima de la velocidad y la comodidad de conducción. Puede utilizar calles locales, caminos no convencionales o estacionamientos, pero no devuelve maniobras que Google conozca como ilegales.

### Restricción obligatoria

La aplicación nunca debe:

* Recomendar exceder límites de velocidad.
* Circular en sentido contrario.
* Ignorar restricciones vehiculares.
* Entrar en calles privadas.
* Utilizar caminos no aptos para automóviles.
* Presentar una ruta como segura cuando existan dudas.

No existe en Google una opción denominada `ignoreSpeedLimits`. La función correcta será minimizar la distancia sin usar la velocidad de la calle como objetivo principal.

---

# 4. Fuentes de datos de Google

## 4.1 Places API

Utilizar para obtener:

* Nombre del establecimiento.
* `placeId`.
* Dirección.
* Coordenadas.
* Categoría.
* Número telefónico.
* Sitio web.
* Horarios habituales.
* Horario correspondiente al día actual.
* Si está abierto.
* Estado del establecimiento.
* Calificación.
* Cantidad de reseñas.
* Fotografías.
* Accesibilidad, cuando esté disponible.

Places API permite buscar establecimientos cercanos y filtrar por tipo, ubicación y estado de apertura.

## 4.2 Compute Route Matrix

Utilizar para comparar simultáneamente varias sucursales.

Entrada:

* Ubicación actual del usuario.
* Hasta un número configurable de sucursales candidatas.
* Hora de salida.
* Modo `DRIVE`.
* Preferencia `TRAFFIC_AWARE` o `TRAFFIC_AWARE_OPTIMAL`.

Salida requerida:

* `distanceMeters`
* `duration`
* `staticDuration`
* estado de cada elemento

Google permite comparar distancias y duraciones entre múltiples orígenes y destinos mediante Compute Route Matrix.

## 4.3 Compute Routes

Utilizar después de seleccionar las mejores sucursales para obtener:

* Ruta predeterminada.
* Hasta tres alternativas.
* Ruta de menor distancia.
* Polilínea.
* Tiempo con tráfico.
* Duración de referencia.
* Distancia.
* Tráfico por tramos.
* Instrucciones.
* `routeToken`.

Con `TRAFFIC_AWARE_OPTIMAL`, `duration` refleja el tiempo estimado considerando tráfico actual. `staticDuration` sirve como referencia sin el efecto inmediato de la congestión.

## 4.4 Navigation SDK

Utilizar para proporcionar dentro de DiligenciaRD:

* Navegación giro a giro.
* Instrucciones de voz.
* Recalculo de ruta.
* Velocidad y progreso.
* Próxima maniobra.
* Hora estimada de llegada.
* Detección de llegada.
* Interfaz personalizada con la marca DiligenciaRD.

La ruta seleccionada debe transferirse al Navigation SDK mediante un `routeToken`.

---

# 5. Restricción sobre "horas populares"

Google Maps muestra públicamente:

* Horas populares.
* Concurrencia en vivo.
* Tiempo de espera.
* Duración de visitas.

Sin embargo, esos datos no forman parte de los campos disponibles públicamente en Places API. Google explica que los calcula con datos agregados de usuarios que han autorizado su historial de ubicación.

Por tanto, el agente no debe:

* Hacer scraping de Google Maps.
* Automatizar consultas para copiar "horas populares".
* Tomar capturas y analizarlas automáticamente.
* Crear una base de concurrencia derivada de Google.
* Mezclar tráfico de Google con OpenStreetMap o GraphHopper.

Los términos de Google impiden utilizar contenido de Routes API con mapas no pertenecientes a Google y limitan el almacenamiento de determinadas coordenadas.

---

# 6. Motor de espera de DiligenciaRD

DiligenciaRD deberá tener su propio sistema de estimación.

El usuario no estará obligado a reportar filas ni tiempos manualmente.

## 6.1 Niveles de calidad del dato

Cada estimación deberá mostrar una categoría visible:

### En vivo

Disponible cuando existe:

* Integración con sistema de turnos.
* API del comercio.
* Calendario de citas.
* Información proporcionada por una institución asociada.
* Sensores o contador autorizado por el establecimiento.

### Predicción DiligenciaRD

Predicción específica del establecimiento obtenida mediante:

* Históricos propios.
* Hora.
* Día de la semana.
* Categoría de establecimiento.
* Datos pasivos y anónimos de llegada y salida, con consentimiento.
* Patrones estacionales.
* Festivos.
* Quincena y fin de mes.
* Cercanía a la hora de apertura o cierre.
* Condiciones de tránsito alrededor del establecimiento.

### Estimación general

Utilizada cuando todavía no existen suficientes datos específicos.

Ejemplo:

> Espera estimada: 20–35 min
> Confianza: baja
> Basada en establecimientos similares.

Nunca presentar una predicción como información "en vivo" si no existe una fuente en tiempo real.

---

# 7. Obtención pasiva de tiempos

Con autorización explícita del usuario, la aplicación puede detectar:

1. Inicio del viaje.
2. Llegada al área del establecimiento.
3. Entrada aproximada mediante geocerca.
4. Permanencia.
5. Salida del establecimiento.
6. Finalización de la diligencia.

No debe pedir al usuario que complete formularios.

## 7.1 Separación del tiempo

La permanencia completa no siempre equivale a espera. Debe dividirse en:

```
T_permanencia = T_espera + T_servicio
```

Para separarlos se utilizarán:

* Categoría de la diligencia.
* Duración típica del servicio.
* Hora de llegada.
* Históricos de sesiones similares.
* Información de citas.
* Distribución estadística del tiempo total.

## 7.2 Privacidad

Guardar:

* Identificador anónimo rotativo.
* Lugar visitado.
* Hora redondeada.
* Duración.
* Tipo de diligencia.

No guardar como histórico permanente:

* Recorrido GPS completo del usuario.
* Dirección de residencia.
* Ubicaciones no relacionadas con una diligencia.
* Identidad vinculada a movimientos precisos.

Debe existir:

* Consentimiento explícito.
* Opción para desactivar datos pasivos.
* Eliminación de historial.
* Política de privacidad.
* Procesamiento agregado cuando sea posible.

---

# 8. Modelo predictivo de espera

## 8.1 Variables de entrada

```text
place_id
categoría del establecimiento
tipo de diligencia
día de la semana
hora de llegada estimada
mes
festivo
día anterior o posterior a festivo
quincena
fin de mes
hora desde la apertura
tiempo restante para el cierre
tráfico de llegada al área
cantidad reciente de sesiones detectadas
duración histórica de visitas
clima, si se integra posteriormente
citas disponibles
capacidad conocida del establecimiento
```

## 8.2 Salidas

El modelo devolverá:

```json
{
  "wait_minutes_p50": 18,
  "wait_minutes_p80": 32,
  "service_minutes_p50": 14,
  "confidence": 0.76,
  "source": "DILIGENCIARD_MODEL",
  "last_updated": "2026-07-13T14:30:00-04:00"
}
```

Utilizar intervalos, no solamente un valor exacto.

Ejemplo visual:

> Espera habitual: 15–25 min
> Posible espera alta: hasta 38 min
> Confianza: 76%

## 8.3 Modelo inicial

Primera versión:

* Medianas por categoría, día y hora.
* Corrección por establecimiento.
* Corrección por festivo y quincena.
* Suavizado para muestras pequeñas.

Versión posterior:

* CatBoost, LightGBM o modelo equivalente.
* Predicción por cuantiles.
* Entrenamiento periódico.
* Validación separada por establecimiento.

---

# 9. Algoritmo de selección de sucursal

## Paso 1: interpretar la intención

El usuario podrá buscar:

* "Renovar licencia".
* "Ir al banco".
* "Sacar acta de nacimiento".
* "Pagar impuestos".
* "Comprar medicamentos".
* "Ir al supermercado".
* "Comer".
* Nombre concreto de un establecimiento.

El buscador debe diferenciar entre:

* Lugar.
* Tipo de comercio.
* Diligencia.
* Servicio requerido.

## Paso 2: encontrar sucursales

Buscar establecimientos dentro de un radio inicial, por ejemplo:

* 5 km en zonas densas.
* 15 km si existen pocos resultados.
* Expandir progresivamente hasta encontrar suficientes opciones.

## Paso 3: filtrar

Excluir o advertir:

* Cerrados permanentemente.
* Cerrados temporalmente.
* Lugares que cerrarán antes de la llegada.
* Sucursales que no ofrecen el servicio buscado.
* Resultados duplicados.
* Lugares sin acceso vehicular conocido.

## Paso 4: comparar trayectos

Usar Route Matrix para calcular `T_llegada,i` para cada sucursal `i`.

## Paso 5: predecir la espera a la hora real de llegada

```
H_llegada,i = H_actual + T_llegada,i
```

La espera debe estimarse usando `H_llegada,i`, no la hora actual.

## Paso 6: calcular el tiempo total

```
T_total,i = T_llegada,i + T_parking,i + T_espera,i + T_servicio,i
```

## Paso 7: añadir incertidumbre

```
T_ajustado,i = T_total,i + λ·U_i
```

Donde:

* `U_i`: incertidumbre de la predicción.
* `λ`: penalización configurable.

La aplicación debe evitar recomendar agresivamente una sucursal con datos poco confiables frente a otra con información sólida.

## Paso 8: ordenar

Mostrar:

1. Menor tiempo total.
2. Menor tiempo de viaje.
3. Menor espera.
4. Menor distancia.

---

# 10. Algoritmo de selección de ruta

Para la sucursal elegida, solicitar:

* Ruta predeterminada con `TRAFFIC_AWARE_OPTIMAL`.
* Rutas alternativas.
* Ruta `SHORTER_DISTANCE`.

## 10.1 Variables por ruta

```text
duration
staticDuration
distanceMeters
trafficJamDistance
slowDistance
normalDistance
cantidad de giros
cantidad de tramos
routeToken
routeLabel
```

## 10.2 Indicadores calculados

### Retraso por tráfico

```
T_retraso = T_duration - T_staticDuration
```

### Proporción congestionada

```
C_jam = D_TRAFFIC_JAM / D_total
```

### Proporción lenta

```
C_slow = D_SLOW / D_total
```

### Puntuación general

```
S_r = w_t·T̂_r + w_d·D̂_r + w_j·C_jam,r + w_s·C_slow,r + w_u·U_r
```

Donde los valores con sombrero están normalizados entre las rutas candidatas.

## 10.3 Pesos por modo

### Más rápida

```text
w_t = 0.75
w_d = 0.10
w_j = 0.10
w_s = 0.05
```

### Menos congestionada

```text
w_t = 0.35
w_d = 0.10
w_j = 0.40
w_s = 0.15
```

### Ruta corta legal

```text
w_t = 0.10
w_d = 0.80
w_j = 0.07
w_s = 0.03
```

En este modo, la distancia domina la decisión. La velocidad límite de la calle no será una variable del algoritmo propio.

### Diligencia óptima

Primero selecciona la sucursal con menor tiempo total y luego la ruta con menor tiempo de llegada confiable.

---

# 11. Interfaz principal

## 11.1 Pantalla de inicio

Diseño semejante a una aplicación de navegación:

* Mapa ocupando toda la pantalla.
* Posición actual centrada.
* Tráfico visible.
* Barra de búsqueda superior.
* Botón de voz.
* Botón de ubicación.
* Panel inferior deslizable.
* Accesos rápidos.

Texto de la barra:

> ¿Qué diligencia necesitas hacer?

Accesos rápidos:

* Bancos
* Gobierno
* Supermercados
* Farmacias
* Restaurantes
* Clínicas
* Telecomunicaciones
* Couriers

## 11.2 Marcadores

Cada marcador debe mostrar un color asociado al tiempo total:

* Verde: recomendado.
* Amarillo: tiempo intermedio.
* Rojo: tiempo alto.
* Gris: cerrado o información insuficiente.

Dentro del marcador:

```text
1 h 04 min
```

Este valor representa el tiempo para completar la diligencia, no solo para llegar.

---

# 12. Tarjeta de establecimiento

La tarjeta inferior debe incluir:

```text
DGII – Administración Local Máximo Gómez

32 min conduciendo
12–18 min esperando
10–15 min de servicio

Tiempo total estimado: 54–65 min
Ahorro frente a Churchill: 28 min

Abierto hasta las 4:00 p. m.
Confianza de la espera: 81%
```

Botones:

* Ir ahora
* Ver rutas
* Programar
* Documentos
* Llamar
* Guardar

---

# 13. Comparador de sucursales

Mostrar las tres mejores opciones:

| Sucursal     | Trayecto | Espera | Servicio |  Total |
| ------------ | -------: | -----: | -------: | -----: |
| Máximo Gómez |   32 min | 14 min |   12 min | 58 min |
| Churchill    |   20 min | 47 min |   12 min | 79 min |
| San Isidro   |   36 min | 22 min |   12 min | 70 min |

Encabezado:

> Recomendamos Máximo Gómez: ahorrarías aproximadamente 21 minutos.

El usuario puede cambiar entre:

* Terminar más rápido.
* Conducir menos.
* Esperar menos.
* Ir al más cercano.

---

# 14. Comparador de rutas

Mostrar sobre el mapa:

### Ruta recomendada

> 26 min · 8.2 km
> Menor tiempo con tráfico.

### Ruta corta legal

> 29 min · 6.4 km
> 1.8 km menos, usando calles locales.

### Menos congestionada

> 31 min · 8.8 km
> 65% menos recorrido en congestionamiento fuerte.

La aplicación no debe presentar "ruta corta" como "ruta más rápida".

---

# 15. Navegación activa

Durante la navegación mostrar:

* Próxima maniobra.
* Distancia hasta la maniobra.
* Tiempo restante.
* Hora de llegada.
* Distancia restante.
* Barra de tráfico.
* Tiempo total de diligencia.
* Espera estimada al llegar.

Ejemplo:

```text
Llegas al establecimiento: 10:24 a. m.
Espera estimada al llegar: 15–22 min
Finalización estimada: 10:58–11:07 a. m.
```

Si cambia el tráfico o la espera:

> Encontramos una alternativa que podría ahorrarte 12 minutos en total.

No cambiar automáticamente de sucursal cuando el usuario está conduciendo. Mostrar una opción grande y segura:

* Cambiar
* Mantener destino

---

# 16. Estado de llegada

Cuando el Navigation SDK detecte llegada:

```text
Has llegado a DGII Máximo Gómez.

Espera estimada: 16–24 min
Tiempo de servicio: 10–15 min
Finalización estimada: 11:05 a. m.
```

Acciones:

* Ver documentos
* Abrir turno digital
* Llamar
* Finalizar navegación

La aplicación puede iniciar automáticamente una sesión anónima de visita únicamente si el usuario autorizó esta función.

---

# 17. Programar una diligencia

El usuario podrá elegir:

* Ir ahora.
* Salir a una hora específica.
* Llegar antes de una hora.
* Buscar la mejor hora del día.

Ejemplo:

> Mejor momento para ir hoy: 2:15 p. m.
> Duración total estimada: 43 min
> Ahora: 1 h 18 min.

Para salidas futuras, Google combina patrones históricos de tráfico con la hora solicitada. Cuanto más futura sea la salida, mayor peso reciben los patrones históricos.

---

# 18. Diseño visual

## Estilo

* Moderno.
* Minimalista.
* Amigable.
* Dominicano sin caer en estereotipos.
* Botones grandes.
* Uso con una sola mano.
* Legible mientras el teléfono está en soporte vehicular.

## Colores sugeridos

* Azul oscuro: navegación y confianza.
* Verde: mejor opción.
* Ámbar: advertencias.
* Rojo: congestión o cierre.
* Blanco y gris claro: superficies.
* Modo oscuro automático durante la noche.

## Tipografía

* Roboto o tipografía nativa Android.
* Números de tiempo grandes.
* Contraste accesible.
* Evitar textos largos durante navegación.

## Elementos semejantes a Waze

* Mapa dinámico.
* Paneles inferiores.
* ETA prominente.
* Alternativas por colores.
* Indicaciones simples.
* Recalculo visible.
* Interacciones mínimas mientras se conduce.

No copiar logotipos, personajes ni elementos protegidos de Waze.

---

# 19. Arquitectura técnica recomendada

## Aplicación Android

```text
Kotlin
Jetpack Compose
Google Maps SDK for Android
Google Navigation SDK
Google Places SDK
Fused Location Provider
Room para caché local permitida
WorkManager
Firebase Cloud Messaging
Firebase Crashlytics
```

El Navigation SDK permite una experiencia de navegación integrada y personalizable dentro de Android.

## Backend (fase futura)

```text
Python
FastAPI
PostgreSQL
PostGIS
Redis
Celery o sistema de trabajos equivalente
Google Cloud Run
Google Pub/Sub
BigQuery para analítica
Cloud Storage
```

## Motor predictivo (fase futura)

```text
Python
pandas
scikit-learn
CatBoost o LightGBM
MLflow opcional
```

## Panel administrativo (fase futura)

```text
Next.js o React
Mapa de establecimientos
Gestión de categorías
Gestión de servicios
Corrección de sucursales
Integración con instituciones
Monitoreo de modelos
```

---

# 20. Modelo de datos mínimo

## Place

```text
id
google_place_id
name
category
address
latitude
longitude
business_status
timezone
phone
website
created_at
updated_at
```

## Service

```text
id
name
category
description
default_service_minutes
required_documents
appointment_url
```

## PlaceService

```text
place_id
service_id
is_available
opening_rules
verification_source
verified_at
```

## WaitEstimate

```text
place_id
service_id
arrival_datetime
p50_minutes
p80_minutes
confidence
source
model_version
generated_at
```

## VisitSession

```text
anonymous_session_id
place_id
service_id
arrival_bucket
departure_bucket
duration_minutes
consent_version
```

## RouteCandidate

```text
origin_hash
destination_place_id
route_label
distance_meters
duration_seconds
static_duration_seconds
jam_ratio
slow_ratio
route_token
expires_at
```

No almacenar indefinidamente contenido restringido de Google. Los `placeId` sí pueden conservarse; determinadas coordenadas obtenidas mediante servicios de Google tienen restricciones de almacenamiento.

---

# 21. Endpoints del backend (fase futura; en la demo esta lógica vive en un módulo Kotlin dentro de la app)

## Buscar diligencia

```http
GET /v1/search?q=renovar+licencia&lat=...&lng=...
```

## Comparar sucursales

```http
POST /v1/branches/compare
```

```json
{
  "origin": {
    "latitude": 18.48,
    "longitude": -69.94
  },
  "service_id": "renew_driver_license",
  "departure_time": "now"
}
```

## Obtener estimación de espera

```http
GET /v1/places/{placeId}/wait-estimate?service_id=...&arrival_time=...
```

## Comparar rutas

```http
POST /v1/routes/compare
```

## Iniciar diligencia

```http
POST /v1/trips
```

## Registrar llegada pasiva

```http
POST /v1/trips/{tripId}/arrival
```

## Registrar finalización

```http
POST /v1/trips/{tripId}/complete
```

---

# 22. Seguridad

* No incluir claves secretas de Routes API en la aplicación (en la fase demo se usa una clave restringida por paquete + SHA-1 y topes de cuota; migrar a backend antes de un lanzamiento público).
* Las consultas de rutas deben pasar por el backend (producción).
* Restringir la clave Android por paquete y certificado.
* Aplicar cuotas por usuario e IP.
* Usar App Check o mecanismo equivalente.
* Detectar automatización y abuso.
* Solicitar solamente los campos necesarios mediante field masks.
* No guardar rutas completas sin necesidad.
* Mostrar atribución de Google Maps correctamente.

Google exige atribución visible y diferenciación del contenido proporcionado por Google Maps.

---

# 23. Manejo de errores

## Sin datos de espera

Mostrar:

> Todavía no tenemos suficiente información de espera para este lugar. El tiempo total no incluye la fila.

## Lugar cerrado al llegar

Mostrar:

> Este establecimiento podría cerrar antes de tu llegada.

## Ruta corta no disponible

Como `SHORTER_DISTANCE` es una función experimental, utilizar como respaldo:

* Ruta predeterminada.
* Alternativas.
* Ruta con menor `distanceMeters` entre las disponibles.

La función de menor distancia es actualmente experimental o pre-GA y puede cambiar.

## Sin conexión

* Mantener navegación activa si el SDK lo permite.
* Mostrar última información disponible.
* No presentar tráfico antiguo como tráfico actual.
* Indicar la hora de actualización.

---

# 24. MVP completo

La primera versión publicable deberá incluir:

1. Registro opcional.
2. Ubicación actual.
3. Búsqueda de lugares y diligencias.
4. Resultados cercanos.
5. Estado abierto/cerrado.
6. Comparación de sucursales.
7. Tiempo de trayecto con tráfico.
8. Modelo inicial de espera.
9. Tiempo total de diligencia.
10. Ruta rápida.
11. Ruta menos congestionada.
12. Ruta corta legal.
13. Navegación giro a giro.
14. Programación de diligencia.
15. Historial.
16. Favoritos.
17. Política de privacidad.
18. Panel administrativo básico.

---

# 25. Criterios de aceptación

La solución estará correctamente implementada cuando:

* Compare al menos cinco sucursales.
* Calcule la espera para la hora estimada de llegada.
* Muestre el tiempo de viaje separado de la espera.
* Muestre el tiempo total.
* Identifique claramente el origen de la estimación.
* Nunca llame "en vivo" a una predicción.
* Permita seleccionar ruta corta legal.
* Mantenga todas las maniobras legales.
* Muestre tráfico por colores.
* Pueda navegar usando el `routeToken` seleccionado.
* Recalcule ante cambios significativos.
* No dependa de reportes manuales.
* Solicite consentimiento antes de recopilar permanencia.
* Cumpla las atribuciones de Google Maps.
* No haga scraping de Google Maps.
* No mezcle contenido de Google con mapas no Google.
