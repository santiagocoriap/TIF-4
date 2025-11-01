
import os

class Config:
    DEBUG = os.getenv("DEBUG", "true").lower() == "true"
    PORT = int(os.getenv("PORT", "8000"))
    DATA_DIR = os.getenv("DATA_DIR", os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "data")))

    # CSV sources
    API_EARTHQUAKES_CSV = os.getenv("API_EARTHQUAKES_CSV", os.path.join(DATA_DIR, "api_earthquakes.csv"))
    PREDICTIONS_CSV = os.getenv("PREDICTIONS_CSV", os.path.join(DATA_DIR, "earthquake_predictions.csv"))

    # Output CSV (predictions produced by ML if recomputed)
    OUTPUT_PREDICTIONS_CSV = os.getenv("OUTPUT_PREDICTIONS_CSV", os.path.join(DATA_DIR, "predictions_out.csv"))

    # Hidden IDs persistence
    HIDDEN_JSON  = os.getenv("HIDDEN_JSON", os.path.join(DATA_DIR, "hidden.json"))

    # Limits and defaults
    DEFAULT_WINDOW_DAYS = int(os.getenv("DEFAULT_WINDOW_DAYS", "7"))
    DEFAULT_MIN_MAG = float(os.getenv("DEFAULT_MIN_MAG", "2.5"))
    MAX_LIMIT = int(os.getenv("MAX_LIMIT", "2000"))

    # Models directory (h5 and pkl)
    MODELS_DIR = os.getenv("MODELS_DIR", os.path.join(os.path.dirname(__file__), "..", "models"))

    # Notification storage and delivery
    DEVICE_TOKENS_JSON = os.getenv(
        "DEVICE_TOKENS_JSON",
        os.path.join(DATA_DIR, "device_tokens.json")
    )
    FCM_SERVICE_ACCOUNT_JSON = os.getenv("FCM_SERVICE_ACCOUNT_JSON")
    FCM_PROJECT_ID = os.getenv("FCM_PROJECT_ID")
    FCM_API_URL = os.getenv("FCM_API_URL", "https://fcm.googleapis.com/v1")
    FCM_TIMEOUT_SECONDS = float(os.getenv("FCM_TIMEOUT_SECONDS", "10"))
