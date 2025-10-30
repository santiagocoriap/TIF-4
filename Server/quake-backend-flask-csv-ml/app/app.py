
import os, math, time
from flask import Flask, jsonify, request
from flask_cors import CORS
import pandas as pd
from config import Config
from services import csvio
from services.filters import apply_filters
from services.ml import predict_from_models

def estimate_radius_km(magnitude: float) -> float:
    # Visual-only radius; scales with magnitude
    return max(5.0, 7.5 * (2 ** (float(magnitude) - 3)))

def create_app():
    csvio.ensure_storage()
    app = Flask(__name__)
    app.config.from_object(Config)
    CORS(app)

    @app.get("/api/health")
    def health():
        return jsonify({"ok": True})

    def load_detected_records():
        df = csvio.read_api_earthquakes().copy()
        df["time_ms"] = pd.to_datetime(df["time"], errors="coerce").astype("int64") // 10**6
        df["radius_km"] = df["magnitude"].apply(lambda m: estimate_radius_km(m or 0))
        records = df.to_dict(orient="records")
        for record in records:
            record["source"] = "detected"
            record["id"] = record.get("id") or record.get("earthquake_id")
            record["earthquake_id"] = record.get("earthquake_id") or record.get("id")
        return records

    def load_expected_records():
        api_df = csvio.read_api_earthquakes()
        pred_df = predict_from_models(api_df)
        if pred_df is None:
            pred_df = csvio.read_predictions().copy()
        if "predicted_time" in pred_df.columns:
            pred_df["predicted_time_ms"] = pd.to_datetime(pred_df["predicted_time"], errors="coerce").astype("int64") // 10**6
        else:
            pred_df["predicted_time_ms"] = 0
        pred_df["radius_km"] = pred_df.get("predicted_magnitude", 0).apply(lambda m: estimate_radius_km(m or 0))
        out = []
        for _, row in pred_df.iterrows():
            eq_id = row.get("earthquake_id") or row.get("id")
            row_id = row.get("id") or (f"exp-{eq_id}" if eq_id else None)
            payload = {
                "id": row_id,
                "earthquake_id": eq_id,
                "latitude": row.get("predicted_latitude", row.get("latitude")),
                "longitude": row.get("predicted_longitude", row.get("longitude")),
                "original_latitude": row.get("latitude"),
                "original_longitude": row.get("longitude"),
                "depth": row.get("predicted_depth", row.get("depth")),
                "magnitude": row.get("predicted_magnitude"),
                "time": row.get("predicted_time"),
                "time_ms": row.get("predicted_time_ms"),
                "source": "expected",
                "radius_km": row.get("radius_km", 10.0),
                "place": None
            }
            if payload["id"] is None and payload["earthquake_id"]:
                payload["id"] = f"exp-{payload['earthquake_id']}"
            out.append(payload)
        return out

    @app.get("/api/earthquakes/detected")
    def detected():
        records = load_detected_records()
        hidden = csvio.get_hidden_ids()
        records.sort(key=lambda r: r.get("time_ms", 0), reverse=True)
        records = apply_filters(records, request.args, hidden)
        return jsonify({"count": len(records), "items": records})

    @app.get("/api/earthquakes/expected")
    def expected():
        out = load_expected_records()
        hidden = csvio.get_hidden_ids()
        out.sort(key=lambda r: r.get("time_ms", 0), reverse=True)
        out = apply_filters(out, request.args, hidden)
        return jsonify({"count": len(out), "items": out})

    @app.get("/api/earthquakes/pairs")
    def earthquake_pairs():
        detected_records = load_detected_records()
        expected_records = load_expected_records()

        def _get_float(param_name):
            value = request.args.get(param_name)
            if value is None:
                return None
            try:
                return float(value)
            except (TypeError, ValueError):
                return None

        def _get_int(param_name):
            value = request.args.get(param_name)
            if value is None:
                return None
            try:
                return int(value)
            except (TypeError, ValueError):
                return None

        def _within_range(value, min_value, max_value):
            if value is None:
                return False
            val = float(value)
            if min_value is not None and val < min_value:
                return False
            if max_value is not None and val > max_value:
                return False
            return True

        real_min_mag = _get_float("real_min_mag") or _get_float("min_mag")
        real_max_mag = _get_float("real_max_mag") or _get_float("max_mag")
        real_min_depth = _get_float("real_min_depth")
        real_max_depth = _get_float("real_max_depth")

        expected_min_mag = _get_float("expected_min_mag") or _get_float("min_mag")
        expected_max_mag = _get_float("expected_max_mag") or _get_float("max_mag")
        expected_min_depth = _get_float("expected_min_depth")
        expected_max_depth = _get_float("expected_max_depth")

        limit = _get_int("limit")
        hide_requested = request.args.get("hide", "1") == "1"
        hidden_ids = csvio.get_hidden_ids()

        real_lookup = {}
        for record in detected_records:
            key = record.get("earthquake_id") or record.get("id")
            if key:
                record["earthquake_id"] = key
                real_lookup[key] = record

        expected_lookup = {}
        for record in expected_records:
            key = record.get("earthquake_id")
            if key:
                expected_lookup[key] = record

        pairs = []
        for eq_id, real_record in real_lookup.items():
            expected_record = expected_lookup.get(eq_id)
            if expected_record is None:
                continue

            if hide_requested and (
                real_record.get("id") in hidden_ids or expected_record.get("id") in hidden_ids
            ):
                continue

            if not _within_range(real_record.get("magnitude"), real_min_mag, real_max_mag):
                continue
            if real_min_depth is not None or real_max_depth is not None:
                if not _within_range(real_record.get("depth"), real_min_depth, real_max_depth):
                    continue

            if not _within_range(expected_record.get("magnitude"), expected_min_mag, expected_max_mag):
                continue
            if expected_min_depth is not None or expected_max_depth is not None:
                if not _within_range(expected_record.get("depth"), expected_min_depth, expected_max_depth):
                    continue

            pairs.append({
                "earthquake_id": eq_id,
                "real": real_record,
                "expected": expected_record
            })

        pairs.sort(
            key=lambda item: max(
                int(item["real"].get("time_ms") or 0),
                int(item["expected"].get("time_ms") or 0)
            ),
            reverse=True
        )

        if limit is not None:
            limit = max(1, limit)
            pairs = pairs[:limit]

        return jsonify({"count": len(pairs), "items": pairs})

    @app.post("/api/earthquakes/expected/recompute")
    def expected_recompute():
        api_df = csvio.read_api_earthquakes()
        pred_df = predict_from_models(api_df)
        if pred_df is None:
            return jsonify({"ok": False, "error": "Models or scalers not found. Provide .h5 and .pkl in models/ or use existing predictions CSV."}), 400
        # persist for auditability
        path = csvio.write_predictions_out(pred_df)
        return jsonify({"ok": True, "rows": len(pred_df), "path": path})

    @app.get("/api/earthquakes/summary")
    def summary():
        det = csvio.read_api_earthquakes()
        exp = csvio.read_predictions()
        return jsonify({
            "detected": int(det.shape[0]),
            "expected": int(exp.shape[0]),
            "hidden": len(csvio.get_hidden_ids())
        })

    @app.get("/api/earthquakes/hidden")
    def get_hidden():
        return jsonify({"ids": list(csvio.get_hidden_ids())})

    @app.post("/api/earthquakes/hide")
    def hide():
        payload = request.get_json(force=True, silent=True) or {}
        ids = set(payload.get("ids", []))
        hidden = csvio.get_hidden_ids()
        hidden |= ids
        csvio.set_hidden_ids(hidden)
        return jsonify({"ids": list(hidden)})

    @app.delete("/api/earthquakes/hide/<qid>")
    def unhide(qid):
        hidden = csvio.get_hidden_ids()
        hidden.discard(qid)
        csvio.set_hidden_ids(hidden)
        return jsonify({"ids": list(hidden)})

    return app

if __name__ == "__main__":
    app = create_app()
    app.run(host="0.0.0.0", port=Config.PORT, debug=Config.DEBUG)
