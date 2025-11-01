import copy
import json
import os
import threading
import time
from typing import Any, Dict, Iterable, List, Optional, Tuple

import requests
from google.auth.transport.requests import Request as GoogleRequest
from google.oauth2 import service_account

from config import Config


SERVICE_ACCOUNT_SCOPE = "https://www.googleapis.com/auth/firebase.messaging"

_credentials_lock = threading.Lock()
_entries_lock = threading.Lock()
_service_account_credentials: Optional[service_account.Credentials] = None
_service_account_project_id: Optional[str] = None


class NotificationSendError(RuntimeError):
    """Raised when sending the notification to FCM fails."""


def ensure_storage() -> None:
    """Ensure the token storage file exists."""
    path = Config.DEVICE_TOKENS_JSON
    os.makedirs(os.path.dirname(path), exist_ok=True)
    if not os.path.exists(path):
        with open(path, "w", encoding="utf-8") as handle:
            json.dump([], handle)


def _root_dir() -> str:
    return os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))


def _resolve_service_account_path() -> Optional[str]:
    raw_path = Config.FCM_SERVICE_ACCOUNT_JSON
    if not raw_path:
        return None

    candidate_paths: List[str] = []
    if os.path.isabs(raw_path):
        candidate_paths.append(raw_path)
    else:
        root = _root_dir()
        candidate_paths.append(os.path.join(root, raw_path))
        app_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
        candidate_paths.append(os.path.join(app_dir, raw_path))
        candidate_paths.append(os.path.abspath(raw_path))

    for path in candidate_paths:
        if os.path.exists(path):
            return path
    # Fall back to the first candidate (will raise later when opened)
    return candidate_paths[0]


def _load_service_account_credentials() -> Tuple[service_account.Credentials, str]:
    global _service_account_credentials, _service_account_project_id
    with _credentials_lock:
        if _service_account_credentials is not None and _service_account_project_id:
            return _service_account_credentials, _service_account_project_id

        path = _resolve_service_account_path()
        if not path:
            raise NotificationSendError(
                "FCM_SERVICE_ACCOUNT_JSON is not configured on the server"
            )
        try:
            credentials = service_account.Credentials.from_service_account_file(
                path,
                scopes=[SERVICE_ACCOUNT_SCOPE],
            )
        except (OSError, ValueError) as exc:
            raise NotificationSendError(
                f"Unable to load service account credentials: {exc}"
            ) from exc

        project_id = Config.FCM_PROJECT_ID or getattr(credentials, "project_id", None)
        if not project_id:
            try:
                with open(path, "r", encoding="utf-8") as handle:
                    info = json.load(handle)
                    project_id = info.get("project_id")
            except (OSError, ValueError):
                project_id = None
        if not project_id:
            raise NotificationSendError(
                "FCM project ID could not be determined. Set FCM_PROJECT_ID."
            )

        _service_account_credentials = credentials
        _service_account_project_id = project_id
        return credentials, project_id


def _load_entries() -> List[Dict[str, Any]]:
    ensure_storage()
    path = Config.DEVICE_TOKENS_JSON
    try:
        with open(path, "r", encoding="utf-8") as handle:
            data = json.load(handle)
            if isinstance(data, list):
                return data
    except (json.JSONDecodeError, OSError):
        pass
    return []


def _write_entries(entries: List[Dict[str, Any]]) -> None:
    path = Config.DEVICE_TOKENS_JSON
    with open(path, "w", encoding="utf-8") as handle:
        json.dump(entries, handle, ensure_ascii=False, indent=2)


def _load_entries_map_unlocked() -> Dict[str, Dict[str, Any]]:
    return {
        entry.get("token"): entry
        for entry in _load_entries()
        if entry.get("token")
    }


def _write_entries_map_unlocked(entries_map: Dict[str, Dict[str, Any]]) -> None:
    _write_entries(list(entries_map.values()))


def register_token(token: str, metadata: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
    token = (token or "").strip()
    if not token:
        raise ValueError("Token must be a non-empty string")

    now = int(time.time())
    with _entries_lock:
        entries = _load_entries_map_unlocked()
        entry = entries.get(token) or {"token": token, "created_at": now}
        entry["token"] = token
        entry.setdefault("created_at", now)
        entry["updated_at"] = now
        if metadata:
            existing_metadata = entry.get("metadata") or {}
            existing_metadata.update(metadata)
            entry["metadata"] = existing_metadata
        entries[token] = entry
        _write_entries_map_unlocked(entries)
        return copy.deepcopy(entry)


def list_tokens() -> List[str]:
    with _entries_lock:
        return list(_load_entries_map_unlocked().keys())


def _ensure_credentials_ready() -> Tuple[service_account.Credentials, str, GoogleRequest]:
    credentials, project_id = _load_service_account_credentials()
    auth_request = GoogleRequest()
    with _credentials_lock:
        if not credentials.valid:
            credentials.refresh(auth_request)
    return credentials, project_id, auth_request


def _normalize_data(data: Optional[Dict[str, Any]]) -> Dict[str, str]:
    if not isinstance(data, dict):
        return {}
    normalized: Dict[str, str] = {}
    for key, value in data.items():
        if key is None:
            continue
        normalized[str(key)] = "" if value is None else str(value)
    return normalized


def load_entries_snapshot() -> Dict[str, Dict[str, Any]]:
    """Return a deep copy of all token entries for read-only operations."""
    with _entries_lock:
        return {
            token: copy.deepcopy(entry)
            for token, entry in _load_entries_map_unlocked().items()
        }


def update_preferences(
    token: str,
    latitude: Optional[float],
    longitude: Optional[float],
    radius_km: Optional[float],
    minimum_magnitude: Optional[float],
) -> Dict[str, Any]:
    token = (token or "").strip()
    if not token:
        raise ValueError("Token must be a non-empty string")

    now = int(time.time())
    with _entries_lock:
        entries = _load_entries_map_unlocked()
        entry = entries.get(token) or {"token": token, "created_at": now}
        entry.setdefault("created_at", now)
        entry["updated_at"] = now
        entry["preferences"] = {
            "latitude": latitude,
            "longitude": longitude,
            "radius_km": radius_km,
            "minimum_magnitude": minimum_magnitude,
            "updated_at": now,
        }
        entries[token] = entry
        _write_entries_map_unlocked(entries)
        return copy.deepcopy(entry)


def record_delivery(token: str, earthquake_id: str, max_history: int = 100) -> None:
    if not earthquake_id:
        return
    token = (token or "").strip()
    if not token:
        return

    now = int(time.time())
    with _entries_lock:
        entries = _load_entries_map_unlocked()
        entry = entries.get(token) or {"token": token, "created_at": now}
        delivered = entry.get("delivered_ids") or []
        if earthquake_id in delivered:
            return
        delivered.append(earthquake_id)
        if len(delivered) > max_history:
            delivered = delivered[-max_history:]
        entry["delivered_ids"] = delivered
        entry["updated_at"] = now
        entries[token] = entry
        _write_entries_map_unlocked(entries)


def has_received_alert(entry: Dict[str, Any], earthquake_id: Optional[str]) -> bool:
    if not earthquake_id:
        return False
    delivered = entry.get("delivered_ids") or []
    return earthquake_id in delivered


def send_notification(
    tokens: Iterable[str],
    title: str,
    body: str,
    data: Optional[Dict[str, Any]] = None,
    dry_run: bool = False,
) -> Dict[str, Any]:
    seen = set()
    token_list: List[str] = []
    for token in tokens:
        if not token:
            continue
        if token in seen:
            continue
        seen.add(token)
        token_list.append(token)
    if not token_list:
        raise ValueError("At least one token is required")

    credentials, project_id, auth_request = _ensure_credentials_ready()
    endpoint_base = Config.FCM_API_URL.rstrip("/")
    url = f"{endpoint_base}/projects/{project_id}/messages:send"

    total_success = 0
    total_failure = 0
    responses: List[Dict[str, Any]] = []
    payload_data = _normalize_data(data)

    for token in token_list:
        with _credentials_lock:
            if not credentials.valid:
                credentials.refresh(auth_request)
            bearer_token = credentials.token

        message_payload: Dict[str, Any] = {
            "message": {
                "token": token,
                "notification": {
                    "title": title,
                    "body": body,
                },
                "data": payload_data.copy() if payload_data else {},
            }
        }
        if dry_run:
            message_payload["validate_only"] = True

        headers = {
            "Authorization": f"Bearer {bearer_token}",
            "Content-Type": "application/json; charset=UTF-8",
        }

        try:
            response = requests.post(
                url,
                headers=headers,
                json=message_payload,
                timeout=Config.FCM_TIMEOUT_SECONDS,
            )
        except requests.RequestException as exc:
            raise NotificationSendError(f"Failed to reach FCM: {exc}") from exc

        try:
            response_json = response.json()
        except ValueError:
            response_json = {"raw": response.text}

        is_success = response.status_code == 200
        if is_success:
            total_success += 1
        else:
            total_failure += 1

        responses.append(
            {
                "status_code": response.status_code,
                "success": 1 if is_success else 0,
                "failure": 0 if is_success else 1,
                "token": token,
                "response": response_json,
            }
        )

    return {
        "requested_tokens": token_list,
        "success": total_success,
        "failure": total_failure,
        "responses": responses,
        "dry_run": dry_run,
    }
