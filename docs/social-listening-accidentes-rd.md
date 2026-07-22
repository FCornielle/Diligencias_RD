# Scraping y social listening de comentarios de Accidentes_RD

Fecha de revision: 22 de julio de 2026.

## Objetivo

Analizar comentarios publicos de publicaciones de Accidentes_RD para descubrir problemas recurrentes, obtener contexto sobre incidentes y priorizar funciones de DiligenciaRD. El corpus sirve como investigacion cualitativa y como fuente secundaria de corroboracion. No debe considerarse una muestra representativa de la poblacion ni una fuente oficial.

## Adquisicion del corpus

El procesamiento se disena como scraping, independientemente de como se obtenga el archivo de entrada. Las fuentes aceptables, en orden de preferencia, son:

1. Exportacion o feed entregado por Accidentes_RD mediante convenio.
2. Instagram API y webhooks con permisos concedidos por la cuenta profesional.
3. Exportacion de un proveedor de social listening con licencia para Instagram.
4. Muestra manual de publicaciones y comentarios publicos para investigacion puntual.

No se implementa un bot que evada login, limites, CAPTCHA o controles de Meta. Ese metodo es fragil, puede bloquear cuentas y no ofrece una procedencia confiable para una funcion de movilidad.

## Muestreo inicial

Para evitar que un solo video viral domine los resultados:

- Seleccionar 200 publicaciones recientes, cubriendo al menos 90 dias.
- Estratificar por choque, atropello, motocicleta, inundacion, averia, imprudencia y via bloqueada.
- Conservar un maximo uniforme de comentarios por publicacion.
- Registrar fecha del post, fecha del comentario y, si se conoce, fecha real del evento.
- Separar Gran Santo Domingo, Santiago y resto del pais.
- Medir cuantos comentarios tienen ubicacion, hora o estado verificable.

## Esquema de entrada

```json
{
  "source": "instagram",
  "account": "accidentes_rd",
  "post_id": "hash_estable",
  "post_url": "https://www.instagram.com/p/.../",
  "post_published_at": "2026-07-22T14:10:00-04:00",
  "comment_id": "hash_estable",
  "comment_published_at": "2026-07-22T14:24:00-04:00",
  "comment_text": "texto normalizado",
  "parent_comment_id": null
}
```

Los nombres de usuario, fotos, enlaces de perfil y otros identificadores personales se eliminan antes del analisis. Los identificadores tecnicos se convierten en hashes para deduplicar sin reconstruir identidades.

## Etiquetas de analisis

Cada comentario puede recibir cero o varias etiquetas:

- `LOCATION`: via, interseccion, sector, municipio o direccion.
- `TIME`: hora del incidente, antiguedad o expresiones como "ahora mismo".
- `DIRECTION`: sentido este/oeste/norte/sur o carril afectado.
- `ACTIVE_INCIDENT`: evidencia de que el evento sigue activo.
- `RESOLVED`: via despejada, grua o autoridad presente.
- `CONGESTION`: tapon, cierre, desvio o demora observada.
- `ROAD_CONDITION`: hoyo, agua, iluminacion, semaforo o senalizacion.
- `RISK_BEHAVIOR`: velocidad, alcohol, via contraria, casco o distraccion.
- `EMERGENCY`: heridos, fuego, persona atrapada o necesidad de 911.
- `SERVICE_COMPLAINT`: demora o ausencia de DIGESETT, MOPC, 911 u otra entidad.
- `RUMOR_OR_BLAME`: acusacion no verificada, especulacion o culpabilizacion.
- `ABUSE_OR_PII`: insultos, placa, telefono, nombre, rostro u otro dato sensible.

La clasificacion debe distinguir observacion directa, opinion, rumor y relato historico.

## De comentario a evento del mapa

Un comentario nunca crea por si solo un accidente confirmado. Puede elevar la confianza cuando contiene informacion util y coincide con otras fuentes.

Puntuacion inicial sugerida:

| Evidencia | Cambio |
|---|---:|
| Publicacion reciente de Accidentes_RD con ubicacion clara | +40 |
| Segundo medio o fuente oficial coincidente | +30 |
| Dos comentarios independientes con via y sentido | +15 |
| Comentario que confirma que sigue activo | +10 |
| Comentario sin hora o ubicacion | +0 |
| Contenido republicado o evento antiguo | -30 |
| Comentarios contradictorios | -20 |

- `0-39`: no mostrar en rutas; conservar solo para analisis.
- `40-69`: mostrar como `Reportado, no confirmado`.
- `70-100`: mostrar como `Corroborado`, siempre con fuente y hora.

El evento expira entre 30 y 120 minutos. Un comentario `RESOLVED` confirmado puede retirarlo antes.

## Temas preliminares encontrados

La exploracion de publicaciones republicadas, noticias y conversaciones publicas relacionadas sugiere estos grupos de necesidades:

1. Tapones impredecibles que hacen posponer diligencias y multiplican el tiempo total.
2. Conduccion agresiva, irrespeto de senales y maniobras en via contraria.
3. Alta exposicion de motociclistas y conflicto entre tipos de conductor.
4. Hoyos, agua, iluminacion deficiente y vias parcialmente bloqueadas.
5. Demora o falta de respuesta percibida de autoridades y asistencia vial.
6. Necesidad de saber si el incidente sigue activo, en que sentido y desde que hora.

Estos temas son hipotesis de producto. Deben cuantificarse con el corpus autorizado antes de convertirlos en porcentajes o afirmaciones nacionales.

## Salidas utiles para DiligenciaRD

- Ranking semanal de molestias por tema, provincia y via.
- Mapa de menciones agregadas, sin comentarios ni usuarios visibles.
- Lista de vias con reportes recurrentes de hoyos, inundacion o bloqueo.
- Eventos recientes corroborados para complementar el trafico de Google.
- Alertas de calidad cuando una ubicacion es ambigua o hay versiones contradictorias.
- Informe de solicitudes ciudadanas para posibles alianzas con INTRANT, DIGESETT, MOPC y 911.

## Control de calidad

- Revisar manualmente una muestra de al menos 10% del corpus etiquetado.
- Medir precision y falsos positivos de ubicacion, hora y estado activo.
- No inferir zonas peligrosas a partir de insultos, origen o perfil de comentaristas.
- No mostrar contenido grafico, victimas, placas ni acusaciones personales.
- Mantener enlace y hora de la fuente para auditoria interna.
- Publicar solo agregados cuando el objetivo sea investigacion de producto.

## Fuentes de referencia

- [Instagram: restricciones contra scraping no autorizado](https://www.facebook.com/help/instagram/740480200552298)
- [Meta: Instagram API y gestion de comentarios](https://www.postman.com/meta/instagram/documentation/6yqw8pt/instagram-api)
- [Accidentes_RD y seguridad vial dominicana](https://elpais.com/america-futura/2025-05-29/un-pacto-nacional-en-republica-dominicana-para-salvar-mas-de-3000-vidas-al-ano.html)
- [OPSEVI: tablero oficial de seguridad vial](https://opsevi.intrant.gob.do/home)
