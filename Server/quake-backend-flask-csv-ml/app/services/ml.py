
import os
import numpy as np
import pandas as pd
from typing import Dict, Optional
from .custom_activation import clip_depth_activation
from config import Config

# Optional heavy imports guarded to avoid failures when models are absent
try:
    import joblib
except Exception:  # pragma: no cover
    joblib = None

try:
    import tensorflow as tf
    from tensorflow import keras
except Exception:  # pragma: no cover
    tf = None
    keras = None

def _custom_objects():
    # Custom functions used in training (depth model per metodología)
    def clip_depth_activation(x):
        import tensorflow as tf
        return tf.clip_by_value(x, -10.0, 7.0)  # example bounds
    def weighted_mse(y_true, y_pred):
        import tensorflow as tf
        # simple safe MSE
        return tf.reduce_mean(tf.square(y_true - y_pred))
    return {"clip_depth_activation": clip_depth_activation, "weighted_mse": weighted_mse}

def load_model_and_scaler(target: str):
    """Loads Keras .h5 and scaler .pkl for a target among: latitude, longitude, depth, magnitude.
    Returns (model, scaler) or (None, None) if not available.
    """
    models_dir = Config.MODELS_DIR
    h5_name = {
        "latitude": "earthquake_latitude_model.h5",
        "longitude": "earthquake_longitude_model.h5",
        "depth": "earthquake_depth_model.h5",
        "magnitude": "earthquake_magnitude_model.h5",
    }[target]
    pkl_name = f"scaler_{target}.pkl"
    model_path = os.path.join(models_dir, h5_name)
    scaler_path = os.path.join(models_dir, pkl_name)

    model = None
    scaler = None
    if keras and os.path.exists(model_path):
        model = keras.models.load_model(
            model_path,
            custom_objects={
                "Custom>clip_depth_activation": clip_depth_activation,
                "clip_depth_activation": clip_depth_activation,
            },
            compile=False
        )
    if joblib and os.path.exists(scaler_path):
        scaler = joblib.load(scaler_path)
    return model, scaler

def _feature_engineering(df: pd.DataFrame) -> pd.DataFrame:
    out = df.copy()
    # time features
    if "time" in out.columns:
        t = pd.to_datetime(out["time"], errors="coerce", utc=True).dt.tz_convert(None)
        out["year"] = t.dt.year.fillna(1970).astype(int)
        out["month"] = t.dt.month.fillna(1).astype(int)
        out["day"] = t.dt.day.fillna(1).astype(int)
        out["time_numeric"] = (t - pd.Timestamp("1970-01-01")).dt.total_seconds().fillna(0) / (24*3600.0)
    else:
        out["year"] = 1970; out["month"] = 1; out["day"] = 1; out["time_numeric"] = 0.0
    # interactions
    out["lat_lon_interaction"] = out.get("latitude", 0) * out.get("longitude", 0)
    # safe numeric casts
    for col in ["latitude","longitude","depth","magnitude","time_numeric","lat_lon_interaction"]:
        if col in out.columns:
            out[col] = pd.to_numeric(out[col], errors="coerce").fillna(0.0)
    return out

def _align_to_scaler(df: pd.DataFrame, scaler) -> pd.DataFrame:
    if scaler is None:
        # Fallback to reasonable default feature set
        cols = ["latitude","longitude","depth","magnitude","time_numeric","year","month","day","lat_lon_interaction"]
        for c in cols:
            if c not in df.columns:
                df[c] = 0.0
        return df[cols]
    # If scaler exposes feature names, reindex; else pass-through numeric columns
    feature_names = getattr(scaler, "feature_names_in_", None)
    if feature_names is not None:
        aligned = pd.DataFrame(index=df.index)
        for name in feature_names:
            aligned[name] = df[name] if name in df.columns else 0.0
        return aligned
    # generic
    return df.select_dtypes(include=["number"]).fillna(0.0)

def predict_from_models(api_df: pd.DataFrame) -> Optional[pd.DataFrame]:
    """Produce predictions for each target using available models/scalers.
    If some model is missing, returns None (caller may fallback to existing predictions CSV).
    """
    targets = ["latitude","longitude","depth","magnitude"]
    models = {}
    for t in targets:
        model, scaler = load_model_and_scaler(t)
        models[t] = (model, scaler)
    if any(m[0] is None for m in models.values()):
        return None

    df = _feature_engineering(api_df)
    preds = {}
    for t in targets:
        model, scaler = models[t]
        X = _align_to_scaler(df, scaler)
        Xs = scaler.transform(X) if scaler is not None else X.values
        y = model.predict(Xs, verbose=0).reshape(-1)
        preds[t] = y

    out = pd.DataFrame({
        "earthquake_id": api_df["id"].values,
        "latitude": api_df["latitude"].values,
        "longitude": api_df["longitude"].values,
        "depth": api_df["depth"].values,
        "predicted_latitude": preds["latitude"],
        "predicted_longitude": preds["longitude"],
        "predicted_depth": preds["depth"],
        "predicted_magnitude": preds["magnitude"],
    }, index=api_df.index)

    # predicted_time can be approximated as original time + delta (if there is a time model; here we mirror input time)
    if "time" in api_df.columns:
        out["predicted_time"] = pd.to_datetime(api_df["time"], errors="coerce")
    else:
        out["predicted_time"] = pd.NaT
    out["prediction_timestamp"] = pd.Timestamp.utcnow().tz_localize(None)
    out["predicted_earthquake_id"] = None
    out["prediction_correct"] = False
    out["source"] = "expected"
    return out
