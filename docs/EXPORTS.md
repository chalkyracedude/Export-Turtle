# Export files

Files stay in `.runelite/plugin-data/export-turtle/`. JSON uses
`schemaVersion: 1` and character-name UUID filenames; see the snapshot model
classes for individual fields.

| Folder | Contents |
| --- | --- |
| `snapshots/` | Skills, quests, diaries, combat achievements, unlocks and optional clan details. |
| `items/` | Observed bank, seed-vault, inventory, equipment and item charges. |
| `collections/` | Collection Log entries. |
| `events/` | Observed milestones, kills, collection unlocks and valuable drops. |
| `diagnostics/` | Export status and last-success times. |
| `selfies/` | Portraits, headshots, animation atlases and three saved pose slots. |

Missing data means unknown. Open the bank, seed vault and Collection Log to
capture them. Events start with observations made while enabled; older history
is not reconstructed. Bundled task names and mappings can lag game updates.

Charge `quantity` and `observedAt` record confirmed checks. `trackedQuantity`
and `trackedAt` are estimates for supported items; re-check to reconcile.
Estimates can drift and require a fresh check after a session change.

Portraits are 600 × 740 PNGs; headshots are 240 × 240. Animation atlases
(`.360.png`) have 72 frames in 12 columns and 6 rows, each 240 × 296,
with 75 ms playback per frame. Saved slots use `.poseN` (1–3).
`selfies/poses/` holds internal pose archives for reloading in the plugin.

Exports contain private account data. Nothing is uploaded or encrypted.
Disabling a feature stops future writes and keeps saved files. Event retention
is configurable (0 means unlimited). To remove all exports and poses, close the
client and delete the plugin's data folder.
