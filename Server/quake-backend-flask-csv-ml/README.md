
# QuakeScope â€” Backend CSV + ML (sin ingesta USGS)

**Rol del servidor:** *no* descarga datos. **Lee** tus CSV (`api_earthquakes.csv` y `earthquake_predictions.csv`),
**opcionalmente** aplica modelos ML (`.h5` + `.pkl`) si los proporcionas en `models/`, y expone endpoints
para que la app mÃ³vil dibuje el **mapa**, aplique **filtros**, limite el **nÃºmero de eventos** y **oculte** elementos.

## Estructura
```
app/
  app.py              # Flask + rutas
  config.py           # .env y paths
  services/
    csvio.py          # lectura/escritura CSV + ocultos.json
    filters.py        # filtros querystring
    ml.py             # carga de modelos y pipeline de predicciÃ³n (opcional)
data/
  api_earthquakes.csv       # detectados (input)
  earthquake_predictions.csv # predichos (input alternativo)
  hidden.json                # persistencia de ocultos
models/
  earthquake_latitude_model.h5
  earthquake_longitude_model.h5
  earthquake_depth_model.h5
  earthquake_magnitude_model.h5
  scaler_latitude.pkl
  scaler_longitude.pkl
  scaler_depth.pkl
  scaler_magnitude.pkl
```

## Correr
```bash
python -m venv .venv && source .venv/bin/activate  # Windows: .venv\Scripts\activate
pip install -r requirements.txt
cp .env.example .env
python -m app.app
```

## CSV de entrada (ejemplos de columnas)
- `api_earthquakes.csv`: `id,time,latitude,longitude,depth,magnitude` (como el adjunto).
- `earthquake_predictions.csv`: `earthquake_id, latitude, longitude, depth, predicted_latitude, predicted_longitude, predicted_depth, predicted_magnitude, predicted_time, prediction_timestamp, predicted_earthquake_id, prediction_correct` (como el adjunto).

## Endpoints
- `GET /api/earthquakes/detected` â†’ **rojo**. Filtros: `min_mag,max_mag,since_ms,until_ms,bbox,limit,hide`.
- `GET /api/earthquakes/expected` â†’ **azul**. Si hay modelos, predice *on the fly*; si no, lee `earthquake_predictions.csv`.
- `GET /api/earthquakes/pairs` -> detectado + esperado ya pareados. Filtros independientes `real_*` / `expected_*`, respeta `limit` y `hide`.
- `POST /api/earthquakes/expected/recompute` â†’ fuerza predicciÃ³n con modelos y guarda `predictions_out.csv`.
- `GET /api/earthquakes/hidden` / `POST /api/earthquakes/hide` / `DELETE /api/earthquakes/hide/{id}`.
- `GET /api/earthquakes/summary` â†’ conteos.

## IntegraciÃ³n en la app (UI)
- **Mapa**: circulares con `radius_km` (visual) y color por `source` (`detected`=rojo, `expected`=azul).
- **Filtros**: sliders para magnitud, *viewport* â†’ `bbox`, *limit* para cantidad.
- **Ocultar**: persiste las elecciones de usuario contra `/hide`.

## ML (opcional)
- Coloca los `.h5` y escaladores `.pkl` en `models/`. El servidor alinearÃ¡ caracterÃ­sticas con
  `feature_names_in_` del `StandardScaler` si existe, y calcularÃ¡ *features* bÃ¡sicas (`time_numeric`, `year`, `month`, `day`,
  `lat_lon_interaction`). Si no hay modelos, el endpoint `/expected` **no falla**: usa el CSV de predicciones.

> Los modelos y su *feature engineering* derivan de la metodologÃ­a del TIF (Coria Pelaez, 2025). Ajusta
> `ml._feature_engineering` si tu set exacto de *features* difiere.
