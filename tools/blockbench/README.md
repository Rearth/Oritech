# Oritech collision shapes for Blockbench

Install the official GeckoLib Blockbench plugin, then use **File → Plugins → Load Plugin from File** to load `oritech_collision.js`. No build step or additional npm packages are needed. This companion plugin works in GeckoLib Animated Model projects (Blockbench 4.12+).

## Authoring

1. Open the machine's editable `.bbmodel` project.
2. Choose **Edit → Add Oritech Collision Box**. This creates a root-level bone named exactly `collision`, if needed, and adds a one-block cube.
3. Move, resize, duplicate, and rename cubes normally. You can also create the root bone manually. Nested bones inside it are supported.
4. Collision cubes appear cyan and translucent, without textures. Depth testing and depth writing are disabled and they render after the normal model, so boxes remain visible through the machine. The Outliner visibility controls still work. Moving a cube out of the collision subtree restores normal rendering.
5. Choose **File → Export → Export Oritech Collision Shapes** and save `<machine>.collision.json` into `src/main/resources/assets/oritech/collision/block/models/`.
6. Save the `.bbmodel` to preserve your editable collision boxes. Normal GeckoLib geometry export automatically omits the root collision subtree while this plugin is loaded.

Keep exactly one root `collision` bone. A nested bone named `collision` elsewhere is ordinary visual geometry. All cubes under the root collision bone are exported, including hidden cubes and cubes with the normal Export flag disabled. Everything outside that subtree is ignored by the collision exporter.

Use **unrotated, axis-aligned boxes and bones**. Nonzero rotations, non-unit stretch, flat/inverted boxes, and non-cube elements inside the collision subtree produce an actionable export error. Inflation is included. Bone pivots do not translate unrotated boxes. Do not animate collision bones: the export uses authored dimensions, not the current animation frame. These boxes represent a stable assembled machine shape.

The overlay changes only runtime mesh materials/render order, not UVs, textures or saved project properties. Unloading the plugin restores them. Reload the plugin when updating its JavaScript file.

## Asset contract: version 1

This is a separate JSON-based file type, identified by the `.collision.json` suffix. It is not a GeckoLib geometry file and is intentionally outside GeckoLib's model loader directory.

```json
{
  "format_version": 1,
  "coordinate_system": "oritech:block_local_north",
  "boxes": [
    {"min": [0, 0, 0], "max": [1, 1, 1]}
  ]
}
```

- Units are Minecraft blocks (16 Blockbench units = 1 block).
- Coordinates are relative to the controller block's lower corner, with the machine facing north. X is east, Y is up, Z is south.
- Blockbench `[x, y, z]` converts to `[x / 16 + 0.5, y / 16, z / 16 + 0.5]`. Bedrock export and GeckoLib import each mirror X, cancelling one another. This follows the standard GeckoLib block renderer; custom renderer transforms would need an explicit adjustment in the future consumer.
- Coordinates may be negative or greater than 1 for multiblocks. Each box has three finite minimum coordinates strictly below its maximum coordinates.
- Boxes describe solid volumes regardless of texture alpha, UVs or disabled faces. Keep all faces enabled for an accurate viewport preview.
- No animation, texture, controller identity or world position is stored. The asset path supplies the machine identity.
- Oritech loads bundled files on both client and dedicated server, rotates each box around the controller center, splits it into block-local segments, and caches the shapes. Use the **registered block ID** as the filename: `oritech:assembler` uses `assembler.collision.json`. Missing files preserve the block's existing shapes.
- The shared `MachineBlock`, `FrameInteractionBlock` and `PassiveGeneratorBlock` classes, plus refinery chamber modules and machine cores, call `CollisionShapes.getShape` from their normal `getShape` override. Their subclasses inherit file-based collision and selection without a mixin. For an unrelated block or a subclass with its own shape override, use the same helper first and keep the existing shape as the fallback when it returns `null`. A separate collision override must also honor that result.
- Multiblocks use these shapes only while assembled. Used cores look up their segment from the controller's file; unused cores keep their normal shape. Every occupied shape cell must have a controller or linked core block. A cell with no authored boxes has an empty shape.
- Targeting the controller or any used core highlights the complete authored machine shape. The client joins the cached sections for this visual outline, removing seams at block boundaries. Collision and ray targeting still use individual sections. Machines without an asset keep the normal block outline; vanilla high-contrast outline settings are respected.
- Shapes are static and follow horizontal block facing (north for blocks without horizontal facing). Custom renderer transforms need matching authored coordinates. Assets are loaded from the mod, not resource packs; restart the game/server after changing files.

`collision.schema.json` documents the structural contract. Positive dimensions and finite coordinates are additionally checked by the exporter.

## Verification

Run `node --test tools/blockbench/oritech_collision.test.cjs` from the repository root. Tests cover filtering, coordinate conversion, inflation, invalid transforms, geometry export exclusion, preview material restoration, export actions and plugin cleanup using a mocked Blockbench API.

Manual smoke test in Blockbench: put a collision cube inside an opaque machine cube, confirm the cyan overlay is visible in textured view, rename/reparent it out of `collision`, undo, switch projects, export both file types, and unload the plugin. Confirm ordinary geometry and its textures remain unchanged. The automated tests do not replace this GPU/UI check.

API references: [Blockbench export API](https://blockbench.net/wiki/docs/blockbench/), [official GeckoLib plugin source](https://github.com/JannisX11/blockbench-plugins/blob/master/plugins/geckolib/geckolib.js), and [Blockbench cube preview implementation](https://github.com/JannisX11/blockbench/blob/v4.12.6/js/outliner/cube.js).
