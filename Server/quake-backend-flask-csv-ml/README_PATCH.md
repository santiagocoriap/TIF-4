# Parche Keras 3 — activación custom + carga sin compilar

Este parche evita el 500 en `/api/earthquakes/expected` cuando el modelo H5
incluye la activación `Custom>clip_depth_activation` y Keras 3 no puede
deserializarla.

## Archivos incluidos
- `app/services/custom_activation.py`: registra la función en Keras.
- `requirements_patch.txt`: sugiere fijar `scikit-learn==1.6.1` para alinear con el scaler pickled.

## Cambios a aplicar en tu repo

1) **Importar el registro** en `app/services/ml.py` (arriba del archivo, antes de cargar modelos):
```python
from app.services.custom_activation import clip_depth_activation  # registra la activación
```

2) **Cargar el modelo con `compile=False` y pasar `custom_objects`:**
Busca la línea que hacía algo como:
```python
model = keras.models.load_model(model_path, custom_objects=_custom_objects())
```
y reemplázala por:
```python
model = keras.models.load_model(
    model_path,
    custom_objects={
        "Custom>clip_depth_activation": clip_depth_activation,
        "clip_depth_activation": clip_depth_activation,
    },
    compile=False
)
```

> Si ya cuentas con `_custom_objects()`, agrega las mismas claves allí y usa `compile=False`.

3) **(Opcional pero recomendado)** Cachear el modelo para no recargar en cada request:
```python
from functools import lru_cache

@lru_cache(maxsize=8)
def load_model_and_scaler(tag: str):
    # ... tu lógica ...
    return model, scaler
```

4) **Alinear scikit-learn** con el scaler pickled (evita InconsistentVersionWarning):
```
pip install --upgrade "scikit-learn==1.6.1"
```
o incluye el `requirements_patch.txt` en tu entorno/proceso de despliegue.

## Estructura esperada
Coloca `custom_activation.py` en `app/services/custom_activation.py` dentro de tu proyecto.