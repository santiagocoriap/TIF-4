# app/services/custom_activation.py
# Registra la activación custom para Keras 3 al cargar modelos H5.
# Solo con importar este módulo, la función queda registrada.

try:
    import keras  # Keras 3 (standalone)
except Exception:
    from tensorflow import keras  # Fallback a tf.keras si no existe keras separado

import tensorflow as tf

@keras.saving.register_keras_serializable(package="Custom", name="clip_depth_activation")
def clip_depth_activation(x):
    """Activación de recorte. Ajusta los límites si tu modelo usa otros.
    Actualmente: [0.0, 1000.0].
    """
    try:
        # Keras 3 API
        return keras.ops.clip(x, 0.0, 1000.0)
    except Exception:
        # Fallback tf.* por compatibilidad
        return tf.clip_by_value(x, 0.0, 1000.0)