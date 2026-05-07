# Oritech Java Architecture Overview

This is a compact map of the current Oritech codebase as it exists on Minecraft 1.21.1. The important point: Oritech is not a small content-only mod. It is a cross-loader tech mod with its own machine framework, transfer abstractions, custom recipes, custom screens, custom renderers, data components, networking, and several compat layers.

## Project Shape

- `common`: the main mod implementation. Most blocks, items, machines, recipes, screens, renderers, APIs, config, networking payloads, and compatibility code live here.
- `fabric`: Fabric entrypoints and platform glue for Fabric networking, transfer API, attachments, client hooks, access wideners, datagen, and Fabric-only dependencies.
- `neoforge`: NeoForge entrypoints and platform glue for NeoForge networking, capabilities, attachments, client events, access transformers, and NeoForge-only dependencies.
- `docs`: user-facing wiki/docs content, separate from the Java migration work.

The build is a Java 21 multi-project Gradle setup using Architectury-style common/platform separation. Current core versions are Minecraft `1.21.1`, NeoForge `21.1.216`, Fabric Loader `0.17.3`, Fabric API `0.116.7+1.21.1`, Architectury API `13.0.8`, GeckoLib `4.6.6`, and NeoForge ModDev/Fabric Loom in the loader modules.

## Common/Platform Boundary

`OritechPlatform` is the main cross-loader service surface. Common code calls it for platform-sensitive operations such as packet registration/sending, event hooks, attachments/player augment data, fake players, and block interaction helpers. Fabric and NeoForge each implement that contract in their loader modules.

Important platform files:

- `common/src/main/java/rearth/oritech/OritechPlatform.java`
- `fabric/src/main/java/rearth/oritech/fabric/OritechPlatformFabric.java`
- `neoforge/src/main/java/rearth/oritech/neoforge/OritechPlatformNeoForge.java`
- `fabric/src/main/java/rearth/oritech/fabric/OritechFabricMod.java`
- `fabric/src/main/java/rearth/oritech/fabric/client/OritechFabricModClient.java`
- `neoforge/src/main/java/rearth/oritech/neoforge/OritechModNeoForge.java`
- `neoforge/src/main/java/rearth/oritech/neoforge/client/OritechClientNeoForge.java`

The platform split is real and important. Migration work should keep common behavior in `common` and isolate loader API churn in the Fabric/NeoForge modules where possible.

## Minecraft Systems Oritech Touches

Oritech touches a broad set of Minecraft systems:

- Blocks and block states: many machine, pipe, storage, reactor, accelerator, multiblock, decorative, and infrastructure blocks.
- Block entities: persistent machine state, inventories, fluids, energy, progress, multiblock state, addons, reactor state, pipe/network state, and custom sync.
- Items: tools, powered tools, armor/jetpacks, fluid/energy containers, wrench interactions, targeting tools, unstable containers, and GeckoLib items.
- Energy/fluid/item transfer: common Oritech APIs with Fabric and NeoForge adapters.
- Recipes: custom machine recipes, custom serializers/codecs, recipe matching, and extensive datagen builders.
- Data components: custom item state such as AOE toggles, target positions, stored fluids, and addon data.
- GUI/screens: custom screen framework and widgets for machines, storage, reactors, addons, filters, and previews.
- Rendering: GeckoLib items/blocks, block entity renderers, world overlays, ore finder/cable renderers, block outlines, reactor previews, and custom item rendering.
- Networking: `CustomPacketPayload` and `StreamCodec` based packet registration through the platform layer.
- Player state/attachments: augment installation and player augment synchronization.
- Mixins/access changes: client render layers, elytra/cape behavior, render bounds, and platform-specific access widening/transforming.
- Compatibility: EMI, REI, JEI, Jade, Tech Reborn, GrandPower, owo-lib, Forge Config API Port, and loader-specific transfer/energy APIs.

## Main Java Areas

### Registration and Initialization

Central init classes under `common/src/main/java/rearth/oritech/init` register the mod content: blocks, items, block entities, components, menu types, particles, recipes, tags, sounds, and compat hooks. `ArchitecturyRegistryContainer` and the `*Content` classes are the main places to audit for registration API changes.

High-risk files include `BlockContent`, `ItemContent`, `BlockEntitiesContent`, `ComponentContent`, and `RecipeContent`.

### Machines, Storage, Pipes, and Multiblocks

The `block` and `block/entity` packages contain the core gameplay. Machine block entities usually own energy, inventory, fluid, progress, recipe matching, upgrades, addons, and sync fields. Pipe and storage classes bridge into the transfer APIs. Reactor and accelerator systems add multiblock/controller state and custom rendering/UI.

This is the largest behavioral surface of the mod and should be migrated before cosmetic compat polish.

### Transfer Abstractions

Oritech defines common energy, fluid, and item APIs, then adapts to Fabric Transfer API and NeoForge capabilities. Fabric code uses `Storage<ItemVariant>` and related transfer concepts. NeoForge code currently wraps old capability interfaces such as `IItemHandler`, `IFluidHandler`, `IFluidHandlerItem`, and GrandPower/energy storage.

This layer is strategically valuable: if it is updated cleanly, most machines and pipes can keep using Oritech's own common interfaces.

### Recipes and Datagen

Oritech has a custom machine recipe model and many datagen builders. The important files are under `common/src/main/java/rearth/oritech/api/recipe`, `common/src/main/java/rearth/oritech/init/recipes`, and `fabric/src/data/java/rearth/oritech/generator`.

The recipe stack is migration-sensitive because later 1.21.x and 26.1 rework recipe IDs, recipe displays, ingredient structure, recipe serializers, and recipe result stack representation.

### Screens and Widgets

Oritech has a custom screen/widget layer under `common/src/main/java/rearth/oritech/api/screen` and `common/src/main/java/rearth/oritech/client/ui`. Machine screens are not just vanilla container screens; they compose custom widgets, previews, labels, item widgets, overlays, scroll areas, progress bars, and reactor-specific views.

That makes the GUI rendering changes in 1.21.6+ and 26.1 one of the largest client-side migration risks.

### Rendering

Rendering includes GeckoLib renderers, custom world render hooks, block outline rendering, cable surfing rendering, ore finder overlays, reactor previews, item renderers, and block entity render bounds. Both Fabric and NeoForge client modules register or hook some of this rendering.

The direct use of `PoseStack`, `MultiBufferSource`, `VertexConsumer`, world render callbacks/events, and old block entity renderer signatures means the 1.21.9+ and 26.1 render submission changes will require focused work.

### Networking and Sync

Oritech already uses modern `CustomPacketPayload` and `StreamCodec` for packets, with registration delegated through `OritechPlatform`. This is a good starting point, but packet codec signatures, vector codecs, large packet handling, and recipe sync behavior still need review during the port.

`NetworkManager`, `SyncField`, `ReflectiveCodecBuilder`, item packets, particle payloads, augment packets, and platform packet registration should be audited together.

### Compat

Compatibility code is spread under `init/compat` and loader modules. The main integrations are EMI, REI, JEI, Jade, Tech Reborn, GrandPower, owo-lib, and loader-specific APIs. These should be treated as a later migration phase after core registration, machines, transfer, recipes, and rendering compile again.

## Migration Hotspots

For the 1.21.1 -> 26.1 port, the most sensitive Oritech areas are:

- Gradle/toolchain/dependency setup: Java 25, Gradle 9+, new Fabric Loom behavior, updated ModDevGradle/NeoForge/Fabric API/Architectury/GeckoLib.
- Registry keys and content setup: blocks/items/entity types/block entity types need the newer registry-key-driven setup.
- Block entity persistence: many classes still use `CompoundTag` in `saveAdditional`/`loadAdditional`; later versions replace that with value/read/write view APIs.
- Transfer layer: NeoForge 21.9 replaces old item/fluid/energy capability interfaces with resource/energy handlers and transactions.
- Recipes/datagen: custom recipes and generated outputs need updates through the recipe serializer and `ItemStackTemplate` changes.
- GUI framework: Oritech's custom widgets and machine screens need the GUI extraction/render-state changes.
- Rendering: block entity renderers, world overlays, block outlines, item renderers, and GeckoLib integration need the render submission pipeline changes.
- Mixins/access wideners/access transformers: render signatures and class/package names change enough that these will need deliberate retargeting.
- Compat: recipe viewers and Jade should be updated after the core gameplay path works.
