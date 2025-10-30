
import os, json
import pandas as pd
from typing import Dict, List
from config import Config

def ensure_storage():
    os.makedirs(Config.DATA_DIR, exist_ok=True)
    if not os.path.exists(Config.HIDDEN_JSON):
        with open(Config.HIDDEN_JSON, "w", encoding="utf-8") as f:
            json.dump({"ids": []}, f)

def get_hidden_ids():
    ensure_storage()
    with open(Config.HIDDEN_JSON, "r", encoding="utf-8") as f:
        data = json.load(f)
    return set(data.get("ids", []))

def set_hidden_ids(ids):
    ensure_storage()
    with open(Config.HIDDEN_JSON, "w", encoding="utf-8") as f:
        json.dump({"ids": list(ids)}, f, ensure_ascii=False, indent=2)

def read_api_earthquakes():
    """Reads CSV with schema like: id,time,latitude,longitude,depth,magnitude"""
    path = Config.API_EARTHQUAKES_CSV
    if not os.path.exists(path):
        return pd.DataFrame(columns=["id","time","latitude","longitude","depth","magnitude"])
    df = pd.read_csv(path)
    # Normalize columns
    cols = {c.lower(): c for c in df.columns}
    # Force expected names
    rename = {}
    for expected in ["id","time","latitude","longitude","depth","magnitude"]:
        if expected not in df.columns:
            # case-insensitive fallback
            for k,v in cols.items():
                if k == expected:
                    rename[v] = expected
    if rename:
        df = df.rename(columns=rename)
    # Parse time to datetime (UTC naive for simplicity)
    if "time" in df.columns:
        df["time"] = pd.to_datetime(df["time"], errors="coerce", utc=True).dt.tz_convert(None)
    df["source"] = "detected"
    return df

def read_predictions():
    """Reads CSV with schema like earthquake_predictions.csv provided by user."""
    path = Config.PREDICTIONS_CSV
    if not os.path.exists(path):
        return pd.DataFrame(columns=[
            "earthquake_id","latitude","longitude","depth",
            "predicted_latitude","predicted_longitude","predicted_depth",
            "predicted_magnitude","predicted_time","prediction_timestamp",
            "predicted_earthquake_id","prediction_correct"
        ])
    df = pd.read_csv(path)
    # Normalize datetimes if present
    for col in ["predicted_time", "prediction_timestamp"]:
        if col in df.columns:
            df[col] = pd.to_datetime(df[col], errors="coerce")
    df["source"] = "expected"
    return df

def write_predictions_out(df):
    path = Config.OUTPUT_PREDICTIONS_CSV
    df.to_csv(path, index=False)
    return path
