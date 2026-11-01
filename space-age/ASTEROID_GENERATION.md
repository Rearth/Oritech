# Asteroid generation datapacks

Finite asteroids use two synced datapack registries:

- `data/<namespace>/oritech_space_age/asteroid_types/<name>.json`
- `data/<namespace>/oritech_space_age/asteroid_fields/<name>.json`

Restart the world/server after editing these registry definitions. Existing generated asteroids remain saved snapshots: changing a definition does not respawn resources or move a mission target. New player systems use the current definitions.

## Asteroid types

Example: `asteroid_types/ferrous.json`.

```json
{
  "volume": { "min": 50000, "max": 250000 },
  "materials": [
    { "block": "oritech_space_age:superdense_iron_ore", "weight": 65 },
    { "block": "oritech_space_age:superdense_nickel_ore", "weight": 15 },
    { "block": "minecraft:stone", "weight": 20 }
  ],
  "surface_gravity": 0
}
```

Volume is the physical block count. Material weights are composition proportions, not selection chances: all listed materials share that exact volume, subject to integer rounding. The default types contain 80% resources and 20% stone/deepslate. Exotic types also contain real special blocks.

Registered vanilla/Oritech host-rock ore variants normalize to their superdense family. Explicit dense block IDs are recommended. Other registered blocks remain literal materials. Mining drops are controlled by block loot tables, not generation weights. Mass comes from the existing material densities, and size comes from the shared physical shape. There are no independent mass/radius settings.

## Asteroid fields

Fields select types using `asteroid_types`, a list of `{ "type": "namespace:id", "weight": 10 }`. These weights ARE selection chances. `asteroids_per_region` is an inclusive integer `{ "min": ..., "max": ... }` range. A zero count is allowed.

The `layout` forms are:

- `cloud`: `center: {x,y}` and `radius` define one circular survey region.
- `belt`: `center: {x:0,y:0}`, `radius`, `half_width`, and `survey_segments` define an annulus split into curved regions. Belts currently surround the Sun; nonzero belt centres are rejected.
- `scattered_clouds`: `center`, cloud `radius`, `region_count`, and `distance: {min,max}` distribute circular regions around the centre.

All coordinates and distances use the simulation's shared two-dimensional space plane. The default outer belt has radius 6,500,000, half-width 310,000, 16 survey segments, and 18 asteroids per segment. Six nearby fields and ten scattered clouds reproduce the previous population of 414 finite asteroids.

Optional grouping:

```json
"grouping": {
  "groups_per_region": { "min": 3, "max": 5 },
  "spread": 140000,
  "type_selection": "per_group"
}
```

Grouping divides the region's asteroid count across randomly placed subgroups. `spread` is each subgroup's scatter radius; positions are clamped to the parent survey region. `per_group` selects one composition type for the subgroup. `per_asteroid` selects independently for every member. Without grouping, positions are distributed across the whole region.

## Persistence and validation

Generation is deterministic from world seed, player UUID and field ID. Adding an unrelated field does not reroll other fields. Generated IDs and source field IDs are saved alongside resolved positions and compositions. Mining, fragmentation and normal reloads never regenerate the initial population.

Invalid ranges, weights, duplicate material families, missing type references, unknown/air blocks, and invalid layout dimensions are rejected. The server validates references before starting.

`data/oritech_space_age/asteroid_defaults.json` indexes bundled definitions for standalone simulations and tests. Update that index when adding a built-in default. Normal server datapacks use the registry contents directly; custom packs do not need to modify the index.
