# Levantamiento de oportunidades para DiligenciaRD

Fecha de revision: 22 de julio de 2026.

## Alcance y metodo

Este levantamiento combina documentacion de Google Maps Platform, datos abiertos dominicanos, tableros de OPSEVI, noticias y conversaciones publicas en redes. Las conversaciones sociales son senales cualitativas: ayudan a descubrir problemas y vocabulario, pero no representan por si solas a toda la poblacion.

El analisis de comentarios se plantea como scraping y social listening de texto publico. La etapa de extraccion directa de Instagram no se ejecuto porque la plataforma no expone esos comentarios publicos a este entorno y Meta prohibe la recoleccion automatizada sin permiso. El pipeline propuesto acepta una exportacion autorizada, un proveedor de escucha social o datos entregados por Accidentes_RD y procesa el contenido como un corpus extraido: deduplicacion, anonimizacion, clasificacion y agregacion.

## Hallazgo inmediato sobre rutas

La app mostraba una opcion de calles locales, pero esa preferencia solo reordenaba alternativas despues de recibirlas. No alteraba la peticion a Google. La correccion implementada separa dos decisiones:

- **Menor distancia:** solicita y prioriza `SHORTER_DISTANCE`. Esta ruta minimiza kilometros y puede usar calles locales, aunque tome mas tiempo.
- **Evitar autopistas:** envia `routeModifiers.avoidHighways=true`. Google favorece otras vias, pero no garantiza excluir todas las autopistas.
- **Menos congestionada:** compara duracion con trafico y la proporcion de tramos lentos o congestionados.

No existe una opcion responsable de "ignorar limites de velocidad". El calculo y la navegacion deben seguir reglas legales. Ademas, Google no ofrece datos de limites de velocidad en Republica Dominicana dentro del Navigation SDK, por lo que no debe prometerse esa funcion hasta contar con otra fuente legal y confiable.

## Problemas ciudadanos observados

### 1. La diligencia completa, no solo el trayecto

Las conversaciones publicas repiten que el costo real incluye tapones, filas, sistemas caidos, documentos faltantes y el riesgo de llegar cuando ya no atienden. DiligenciaRD puede diferenciarse calculando **tiempo total de diligencia**:

- Conduccion con trafico, estacionamiento, espera y duracion del servicio.
- Horario real, hora limite de recepcion y alertas de cierre.
- Requisitos y documentos necesarios antes de salir.
- Cita previa o alternativa digital cuando exista.
- Confirmacion comunitaria de fila, sistema activo y servicio disponible.
- Boton de llamada y pregunta sugerida para confirmar antes de viajar.

### 2. Transito impredecible y riesgo vial

Las fuentes sociales mencionan maniobras peligrosas, motocicletas, hoyos, poca disciplina vial y tapones que cambian rapidamente. Las estadisticas oficiales confirman que la seguridad vial merece prioridad: la ONE indica que el 70.48% de las muertes registradas en el lugar del accidente durante 2024 correspondio a motociclistas.

Funciones recomendadas:

- Incidentes recientes con direccion de la via, hora y nivel de confianza.
- Inundaciones, vias intransitables, obras, protestas y objetos en la calzada.
- Alertas discretas antes del punto de riesgo, sin distraer al conductor.
- Acceso rapido a 911 y Asistencia Vial, sin sustituir a las autoridades.
- Mapa historico de puntos criticos basado en OPSEVI y datos abiertos.

### 3. Informacion operativa del Gobierno

La app debe ayudar a saber no solo donde esta una oficina, sino si vale la pena ir ahora. Las integraciones de mayor valor serian:

- Directorio oficial de servicios, requisitos y canales digitales.
- Puntos GOB, oficinas regionales y servicios disponibles por sede.
- Turnos, citas, cierres extraordinarios y avisos de sistema fuera de servicio.
- Enlace a 311 para quejas formales, con seguimiento separado de los reportes comunitarios.

## Propuesta para Accidentes_RD

Si, es prudente mostrar accidentes reportados por Accidentes_RD con su hora, siempre que se implemente como **senal comunitaria temporal** y no como hecho oficial automatico.

### Modelo minimo del incidente

Cada evento debe contener:

- Fuente y enlace a la publicacion original.
- Hora de publicacion y, si se conoce, hora del accidente.
- Coordenadas, via, sentido y referencia cercana.
- Tipo: choque, atropello, vehiculo averiado, inundacion u obstruccion.
- Severidad estimada, estado y nivel de confianza.
- Numero de fuentes que lo corroboran.
- Hora de expiracion para retirarlo de rutas en vivo.

### Reglas de confianza y privacidad

- Mostrar `No confirmado` hasta corroborar ubicacion y vigencia.
- Usar una vida util de 30 a 120 minutos segun el tipo de incidente.
- No redirigir automaticamente por una publicacion antigua o ambigua.
- Ocultar nombres, rostros, placas y nombres de usuarios.
- No copiar comentarios de Instagram al mapa.
- Permitir corregir, descartar o marcar como resuelto.
- Deducir un incidente duplicado cuando varias paginas republican el mismo video.
- Conservar para analitica solo datos agregados y anonimizados.

### Arquitectura recomendada

1. Convenio con Accidentes_RD para recibir un feed curado o webhooks autorizados.
2. Geocodificacion asistida, con revision humana si la ubicacion es ambigua.
3. Corroboracion con Google, INTRANT/OPSEVI, 911, MOPC y reportes de usuarios.
4. Publicacion en el mapa con fuente, antiguedad y confianza visibles.
5. Expiracion automatica del incidente; el historial oficial queda separado.

Los comentarios pueden analizarse como scraping para descubrir problemas ciudadanos y corroborar eventos, pero no deben activar desvios por si solos. El extractor debe alimentarse de una fuente autorizada o de un export, porque una publicacion puede mostrar un accidente antiguo, un comentario puede ser un rumor y la ubicacion puede ser imprecisa.

El protocolo detallado esta en [social-listening-accidentes-rd.md](social-listening-accidentes-rd.md).

## Prioridades de producto

| Prioridad | Capacidad | Valor para RD | Fuente principal |
|---|---|---|---|
| P0 | Menor distancia y evitar autopistas reales | Eleccion de ruta comprensible | Google Routes API |
| P0 | Tiempo total de diligencia | Evita viajes que no resuelven | Places + datos propios |
| P0 | Horarios, requisitos, llamada y cierre | Reduce viajes perdidos | Institucion/Directorio oficial |
| P1 | Trafico e incidentes confirmados | Reduce demoras y exposicion | Google + socios oficiales |
| P1 | Fila y sistema activo por sede | Ataca una incomodidad repetida | Comunidad con reputacion |
| P1 | Inundaciones, hoyos y vias bloqueadas | Alto impacto local | MOPC/COE/comunidad |
| P2 | Piloto Accidentes_RD | Mayor rapidez de deteccion | Acuerdo y feed autorizado |
| P2 | Mapa historico de riesgo | Prevencion y planificacion | OPSEVI/datos abiertos |

## Plan de 90 dias

### Fase 1: confianza basica

- Estabilizar mapa, busqueda, tres alternativas de ruta y navegacion.
- Medir errores de Routes/Places/Navigation sin guardar trayectos personales.
- Mostrar claramente distancia, tiempo con trafico y retraso.
- Completar horarios, llamada, requisitos y fuentes de cada dato.

### Fase 2: diligencia inteligente

- Check-in anonimo de fila y sistema activo.
- Estimacion de riesgo de cierre y mejor hora para salir.
- Integrar directorios oficiales, Puntos GOB y enlaces a servicios digitales.
- Agregar capa historica agregada de OPSEVI, sin datos de victimas.

### Fase 3: incidentes dominicanos

- Piloto con un feed autorizado de Accidentes_RD.
- Moderacion, deduplicacion, expiracion y niveles de confianza.
- Integrar inundaciones y asistencia vial.
- Evaluar precision, falsos positivos y minutos ahorrados antes de ampliar.

## Fuentes consultadas

- [Google: rutas de menor distancia](https://developers.google.com/maps/documentation/routes/shorter-distance-routes)
- [Google: modificadores de ruta](https://developers.google.com/maps/documentation/routes/route-modifiers)
- [Google: cobertura de Navigation SDK](https://developers.google.com/maps/documentation/navigation/android-sdk/coverage-nav-sdk)
- [OPSEVI: tablero y mapa de seguridad vial](https://opsevi.intrant.gob.do/home)
- [ONE: infografia de seguridad vial 2025](https://www.one.gob.do/media/30ud1ei0/infograf%C3%ADa-seguridad-vial-2025.pdf)
- [Datos abiertos: fallecimientos por accidentes](https://datos.gob.do/dataset/estadistica-de-fallecimientos-por-accidentes-de-transito)
- [Datos abiertos: Asistencia y Proteccion Vial](https://datos.gob.do/dataset/asistenciavial)
- [El Pais: Accidentes_RD y seguridad vial dominicana](https://elpais.com/america-futura/2025-05-29/un-pacto-nacional-en-republica-dominicana-para-salvar-mas-de-3000-vidas-al-ano.html)
- [Instagram: restricciones contra scraping](https://www.facebook.com/help/instagram/740480200552298)
- [Meta: documentacion de Instagram API](https://www.postman.com/meta/instagram/documentation/6yqw8pt/instagram-api)
- [Conversacion publica: conducir en Santo Domingo](https://www.reddit.com/r/Dominicanos/comments/1usaovf/c%C3%B3mo_es_conducir_en_santo_domingo/)
- [Conversacion publica: trafico y diligencias pospuestas](https://www.reddit.com/r/Dominicanos/comments/1nh6k5k/)

## Criterio de exito

DiligenciaRD debe medir si ayuda a completar la gestion, no solo si abre una ruta: porcentaje de diligencias resueltas, minutos totales ahorrados, viajes evitados, precision de incidentes y reportes corregidos. La confianza del usuario depende de mostrar siempre fuente, hora y nivel de certeza.
