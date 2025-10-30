# MASTER PROMPT — Build **QuakeScope** (Android • Kotlin • Jetpack Compose)

## Goal

Create a **modular, clean, and testable** Android app named **QuakeScope** that visualizes pairs of earthquakes (real + estimated) on a Google Map and in a list. The app must have an exceptional **UI/UX** that keeps users engaged, supports **dark/light mode**, **English/Spanish**, and exposes rich **filters**. Code quality, performance, and accessibility are first-class.

---

## Tech Stack & Architecture (non-negotiable)

- **Language:** Kotlin (latest stable), Gradle Kotlin DSL
- **UI:** Jetpack Compose + Material 3, Google Maps Compose
- **Architecture:** Clean Architecture + MVVM
    - Layers: `app` (UI), `domain`, `data`
    - Unidirectional data flow with **State**/**Events**/**Effects**
- **DI:** Hilt
- **Async:** Kotlin Coroutines + Flows
- **Networking:** Retrofit + OkHttp (or Ktor client) with Moshi/Kotlinx Serialization
- **Persistence:** Room for offline cache
- **Preferences:** DataStore (Proto or Preferences)
- **List:** Paging 3 (for scalable lists)
- **Localization:** Android resources (EN, ES) + runtime language switch in Settings
- **Theming:** Material 3 dynamic color (when available), light/dark toggle
- **Testing:** JUnit5, Turbine (Flows), MockK, Robolectric, Compose UI tests
- **Static quality:** Detekt + Ktlint; baseline set and CI-friendly
- **Min SDK:** 26 or lower only if safe; Target SDK: latest stable

---

## Data Contract

**Source:** local dev server at **`localhost:8000`** returning **an array** of earthquake objects with this **exact element shape**:

```json
{
  "depth": 151.7339324951172,
  "earthquake_id": "us6000qhyd",
  "id": "exp-us6000qhyd",
  "latitude": -21.73385238647461,
  "longitude": -67.70409393310547,
  "magnitude": 4.288239002227783,
  "original_latitude": -20.9406,
  "original_longitude": -67.5152,
  "place": null,
  "radius_km": 18.31723193948689,
  "source": "expected",             // "usgs" (real) or "expected" (estimated)
  "time": "Thu, 05 Jun 2025 15:12:31 GMT",
  "time_ms": 1749136351000          // epoch millis (authoritative for sorting)
}

```

**Important emulator note:** From the Android emulator, **use `http://10.0.2.2:8000/`** (not `localhost`) as the base URL. Make this configurable:

- `BuildConfig.BASE_URL` with fallback to `10.0.2.2` for debug.
- Expose an override in **Settings → Developer options** (debug builds only).

**Endpoints (implement any one):**

- `GET /earthquakes` → returns **array** of objects above.
- Optionally `GET /health` for connectivity diagnostics.

---

## Domain Model & Pairing

- Define `Earthquake` domain entity mirroring fields above; parse `time_ms` to `Instant`.
- **Classification:** `source == "usgs"` → **Real**, `source == "expected"` → **Estimated**.
- **Pairing rule:** Pair items by shared **`earthquake_id`**. Each pair should ideally have one **Real** and one **Estimated**. Handle edge cases:
    - If a counterpart is missing, show the existing one; mark pair status as **Incomplete**.
- Define `EarthquakePair(real: Earthquake?, estimated: Earthquake?)`.
- Persist raw items and a derived table/index for fast pair lookup (Room views or precomputed mapping).

---

## App Navigation (Tabs)

Bottom tabs with icons (Material symbols):

1. **Map** (default) — `MapTab`
2. **List** — `ListTab`
3. **Wiki** — `WikiTab`
4. **Settings** — `SettingsTab`

---

## Map Tab — Google Maps Compose

**Purpose:** Visualize **all earthquake pairs** as circular overlays plus pair-link lines.

### Rendering

- For each earthquake:
    - Draw a **Circle** centered at `latitude/longitude` with radius `radius_km * 1000` meters.
    - **Color:**
        - Real = **Red** (high-contrast, accessible)
        - Estimated = **Blue** (high-contrast, accessible)
    - Add subtle stroke and a semi-transparent fill.
- For each **pair**, draw a **Polyline** between real and estimated epicenters.
    - Style with slight thickness, 30–40% transparency; use dashed pattern for clarity.

### Interaction

- **Tap a circle:**
    - Show an in-map info card pinned above the circle with:
        - Magnitude, Depth, Time (device locale & timezone aware), Lat/Lon, Source tag (Real/Estimated), Earthquake ID, optional Place (or “Unknown”).
    - **Highlight** both quakes of the pair on the map (increase stroke width + glow/alpha change).
    - **Sync selection with the List tab**: both quakes highlighted in the list and auto-scrolled into view if the user switches tabs.
- **Pair line visibility control (global):**
    - Setting with 3 modes:
        - **All On** (default)
        - **Selected Only**
        - **Off**
    - Quick-toggle in a collapsible map toolbar (FAB → sheet).
- **Hide controls:** When an item is selected, the info card offers:
    - **Hide this quake on map**
    - **Hide both in the pair on map**
    - Hides only on the map; in the list they remain visible but **muted** with a “Hidden on map” badge.
- **Camera behavior:**
    - On filter changes or initial load, **fit bounds** to all **visible (not hidden)** quakes.
    - On selecting a pair, animate camera to show both members optimally.
- **Legend & overlays:**
    - A small legend explaining **Red = Real**, **Blue = Estimated**.
    - Toggle for **Cluster density cap**: if too many circles, limit to “Amount to Display” (from filters).

### Performance

- Avoid re-composing map shapes excessively; maintain stable keys.
- Throttle map recomputations during rapid filter slider changes.

---

## List Tab — Pairs + Items

- **Paging 3** list of earthquake **pairs** (grouped by `earthquake_id`).
- Each row shows:
    - Pair header (ID, completeness, time range of the two events).
    - Two child items (Real & Estimated) with:
        - Magnitude, Lat, Lon, Depth, Time, a color pill (Red/Blue).
    - Row actions:
        - **Select** (syncs to map and highlights both)
        - **Hide this quake on map**
        - **Hide both in pair on map**
- **Sort by:** Time (new → old), Magnitude, Depth, Source priority (Real first), Distance (if location permission granted).
- **Selection & Highlighting:**
    - Selecting any item highlights both items and their circles on the map.
    - Hidden items appear with reduced opacity and a badge; never removed from the list.
- **Empty/edge states:** Clear empty, error, and no-results messages with recovery actions.

---

## Filters (Global, affect Map & List)

In a bottom sheet accessible from Map/List, store choices via DataStore:

- **Magnitude range** (continuous slider, e.g., 0–10, step 0.1)
- **Depth range** (0–700 km)
- **Date range** (start/end pickers; default last 30 days)
- **Amount to display** (cap, e.g., 50/100/250/500)
- **Source filter** (Real / Estimated / Both)
- **Completeness** (Only complete pairs / Include incomplete)
- **Sort by** (applies in List)
- **Distance from me** (if permission granted) with radius slider
- **Hidden items:** Toggle “Show hidden on map” (off by default)
- **Line visibility mode** (All On / Selected Only / Off)

---

## Wiki Tab — Visual, simple safety guide

- Ship offline content (Markdown + vector illustrations/Lottie).
- Sections:
    - What is an earthquake? Magnitude vs. intensity. Depth matters.
    - Before/During/After safety checklists (icons, bullet chips).
    - Visuals: Drop-Cover-Hold On, evacuation tips, emergency kit.
- Use a compact glossary (P-wave, S-wave, epicenter, aftershock).
- Make it **clear, visual, and scannable**; fully localized (EN/ES).

---

## Settings Tab

- **Appearance:** Theme (System/Light/Dark), dynamic color toggle.
- **Language:** English / Español (runtime switch, persists).
- **Map preferences:** Default zoom behavior, pair line mode, units (km/mi).
- **Data:** Amount to display default, auto-refresh interval (manual pull-to-refresh also).
- **Privacy:** Location permission explanation + soft opt-in.
- **Developer (debug builds):** Base URL override, mock latency toggle, map debug layers.

---

## UX Details (make it delightful)

- **Polish:** Micro-animations for selection, sheet transitions, and tab changes.
- **Fitts-law-friendly** touch targets (≥ 48dp).
- **Haptics** on key interactions (where supported).
- **Accessible colors** (WCAG AA), TalkBack labels, content descriptions, large-text support.
- **Offline-first:** Cache last successful fetch; show cached data instantly while refreshing.
- **Error handling:** Terse, human messages; retry buttons; exponential backoff.

---

## Networking & Caching

- Repository decides source of truth:
    - On app start / pull-to-refresh: fetch from API, upsert Room.
    - Expose `Flow<PagingData<EarthquakePair>>` and `Flow<List<MapRenderable>>`.
- **DTO ↔ Domain mappers** (pure functions, covered by unit tests).
- **Time zones**: Store times as UTC; render in device locale/time zone.

---

## Selection/Highlight Logic (single source of truth)

- Keep a `SelectionState` in a ViewModel accessible to both Map and List composables.
- Selecting any quake or pair:
    - Updates `SelectionState`
    - Emits UI effects: map focus, list scroll, highlight styles
- Hidden state is persisted (DataStore or Room join table) and respected by Map queries.

---

## Icons & Visual Language

- Use Material symbols for tabs and actions.
- Add a small **legend chip** on map explaining colors and line.

---

## Internationalization

- Provide **all** user-facing strings in `values/strings.xml` (EN) and `values-es/strings.xml` (ES).
- Format numbers and dates with locale-aware APIs.
- Settings language switch triggers a recomposition with the new Locale.

---

## Permissions

- **Location** is optional; if granted, enable:
    - “Sort by distance” and distance filter
    - “Center near me” shortcut
- Show rationale and graceful degradation if denied.

---

## File & Module Structure (example)

```
app/
  build.gradle.kts
  src/main/AndroidManifest.xml
  src/main/java/com/quakescope/app/...
    QuakeScopeApp.kt
    di/...
    ui/
      navigation/...
      map/...
      list/...
      wiki/...
      settings/...
      components/...
      theme/...
    util/...
domain/
  build.gradle.kts
  src/main/java/com/quakescope/domain/...
    model/Earthquake.kt
    model/EarthquakePair.kt
    repo/EarthquakeRepository.kt
    usecase/...
data/
  build.gradle.kts
  src/main/java/com/quakescope/data/...
    remote/dto/EarthquakeDto.kt
    remote/ApiService.kt
    local/db/QuakeDao.kt, entities/...
    repository/EarthquakeRepositoryImpl.kt
    mapper/DtoMappers.kt, EntityMappers.kt

```

---

## Key Implementations (must-haves)

- **Base URL switching**: default `http://10.0.2.2:8000/` in debug; configurable.
- **Retrofit interface**:
    - `@GET("earthquakes") suspend fun getEarthquakes(): List<EarthquakeDto>`
- **Room**:
    - Tables for quakes; view or computed mapping for pairs by `earthquake_id`.
- **Map rendering**:
    - Circles: `radius_km * 1000`
    - Colors: Red (Real), Blue (Estimated); semi-transparent fills.
    - Pair **Polyline** drawn by default (toggleable).
- **List actions**:
    - Buttons: hide single / hide pair (map-only), select/highlight.
- **Filters**:
    - Persisted, applied at DB query level when possible; otherwise in memory with diff-friendly updates.
- **State restoration**:
    - Preserve selection, filters, and map camera across process death.

---

## Accessibility & A11y Tests

- Content descriptions for map shapes (announce magnitude, depth, type, time).
- Ensure color is not the only cue; add badges/labels.
- UI tests verifying TalkBack labels and focus order.

---

## Telemetry (optional, privacy-first)

- Local analytics (no network) counting feature usage to improve UX; toggle in Settings.

---

## Testing Requirements

- **Unit tests:** Mappers, pairing logic (including edge cases), filters/sorting, repository.
- **UI tests:** Map/List selection sync, hiding behavior, filter sheet, line visibility modes.
- **Snapshot tests** for key screens (dark/light, EN/ES).
- CI-ready gradle tasks: `./gradlew detekt ktlintCheck test connectedCheck`

---

## Deliverables

- Complete Android Studio project with modules `app`, `domain`, `data`.
- **README**: setup, emulator base URL note (`10.0.2.2`), build flavors, feature list, screenshots/gifs.
- Sample **Wiki** content in EN/ES bundled offline.
- A small **seed script** (optional) to generate mock JSON.

---

## Acceptance Criteria (must pass)

1. Map shows circles for all visible quakes with correct color and radius; pair polylines enabled by default.
2. Tapping any quake shows its info overlay, highlights both quakes of the pair on map & list.
3. List rows show pairs; selecting any quake highlights the pair and syncs map focus.
4. Hide buttons: hide single or both on **map only**; list shows muted style + “Hidden on map” badge.
5. Filters and sorting work across Map and List; settings persist via DataStore.
6. App supports **EN/ES** with runtime switch; supports **Light/Dark**.
7. Works against **`http://10.0.2.2:8000/earthquakes`** on emulator.
8. Graceful offline behavior with cached data.
9. All tests green; lint and static checks pass.

---

## Stretch Ideas (implement if time permits)

- **Reverse geocoding** to populate `place` when null (cached).
- “Compare” view: side-by-side Real vs Estimated deltas (Δlat, Δlon, Δtime, Δmag, Δradius).
- Shareable screenshot of current map viewport with legend.

---

### Important UX Copy (EN/ES examples)

Provide localized strings for:

- Real / Estimated
- Hidden on map
- Show pair lines: All / Selected only / Off
- Filters, Magnitude, Depth, Date range, Amount to display, Sort by, Distance
- Safety tips, glossary terms

---

**Build this exactly as specified. Favor clarity, performance, and a calm, highly legible UI.**