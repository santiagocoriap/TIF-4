
from config import Config

def apply_filters(rows, args, hidden_ids):
    # rows: list of dicts
    out = rows
    if args.get("hide", "1") == "1":
        out = [r for r in out if r.get("id") not in hidden_ids]
    # magnitude filters
    if "min_mag" in args:
        mm = float(args.get("min_mag"))
        out = [r for r in out if float(r.get("magnitude", r.get("predicted_magnitude", 0)) or 0) >= mm]
    if "max_mag" in args:
        mx = float(args.get("max_mag"))
        out = [r for r in out if float(r.get("magnitude", r.get("predicted_magnitude", 0)) or 0) <= mx]
    # time filters (epoch ms or ISO handled by client; server only supports since_ms/until_ms if provided)
    if "since_ms" in args:
        since = int(args.get("since_ms"))
        out = [r for r in out if int(r.get("time_ms", r.get("predicted_time_ms", 0)) or 0) >= since]
    if "until_ms" in args:
        until = int(args.get("until_ms"))
        out = [r for r in out if int(r.get("time_ms", r.get("predicted_time_ms", 0)) or 0) <= until]
    # bbox filter
    if "bbox" in args:
        try:
            west, south, east, north = [float(x) for x in args.get("bbox").split(",")]
            def inside(lat, lon):
                return south <= float(lat) <= north and west <= float(lon) <= east
            out = [r for r in out if inside(r.get("latitude", r.get("predicted_latitude")), r.get("longitude", r.get("predicted_longitude")))]
        except Exception:
            pass
    # limit
    if "limit" in args:
        lim = max(1, min(int(args.get("limit")), Config.MAX_LIMIT))
        out = out[:lim]
    return out
