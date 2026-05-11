# Oritech 1.21.1 to 26.1 Update Primer

This is an Oritech-specific migration primer based on the official NeoForge primers/release posts and Fabric update posts for the versions between Minecraft `1.21.1` and `26.1`.

The blunt version: this is not a version bump. It is a staged port through multiple major Minecraft internals changes: registry-key-backed content setup, block entity serialization rewrites, recipe rewrites, item stack template changes, GUI/render submission changes, Fabric mapping/tooling changes, and NeoForge transfer API replacement.

## Sources Covered

- NeoForge primer index and version primers for `1.21.2`, `1.21.4`, `1.21.5`, `1.21.6`, `1.21.7`, `1.21.8`, `1.21.9`, `1.21.10`, `1.21.11`, and `26.1`.
- NeoForge release notes for the same migration window, especially `21.6`, `21.9`, `21.11`, and `26.1`.
- NeoForge transfer rework post for `21.9`.
- Fabric update posts for `1.21.2/1.21.3`, `1.21.4`, `1.21.5`, `1.21.6/1.21.7/1.21.8`, `1.21.9/1.21.10`, `1.21.11`, and `26.1`.

## Current Project Baseline

- Minecraft: `1.21.1`
- Java: `21`
- NeoForge: `21.1.216`
- Fabric Loader: `0.17.3`
- Fabric API: `0.116.7+1.21.1`
- Architectury API: `13.0.8`
- GeckoLib: `4.6.6`
- Modules: `common`, `fabric`, `neoforge`

The common module owns most gameplay. Fabric and NeoForge modules mostly adapt common APIs to loader-specific networking, transfer, events, attachments, and rendering.

## Recommended Port Order

Do not start by fixing random renderer errors. That will spiral. The port should be ordered around dependencies between systems.

1. Create a stable 1.21.1 branch/tag and keep it buildable.
2. Update Gradle/toolchain/dependencies enough to load the new Minecraft sources.
3. Make registrations compile: blocks, items, block entities, components, menus, recipes, particles, sounds.
4. Migrate core serialization for block entities and storage helpers.
5. Migrate item/fluid/energy transfer APIs and wrappers.
6. Migrate custom recipes and datagen.
7. Migrate networking payloads and sync helpers.
8. Migrate client screens/widgets.
9. Migrate renderers, overlays, outlines, GeckoLib integration, and mixins.
10. Restore compat integrations: EMI/REI/JEI/Jade/Tech Reborn/GrandPower/owo-lib.
11. Run gameplay tests around machines, pipes, reactors, addons, augments, storage, jetpacks, world rendering, and recipe viewers.

## Version-by-Version Impact

### 1.21.2 / 1.21.3

Major themes: registry-key-backed settings, recipes, server-side API signatures, ActionResult unification, entity render states.

Oritech impact:

- Every registered block/item/entity type path needs review. Block and item settings now need registry keys so default components/models/names/loot tables can be precomputed.
- Block entity type construction changes. Oritech's `BlockEntitiesContent` and supported-block setup need audit.
- Recipes change from simple identifier access to registry-key/server-side recipe behavior. Oritech's machine recipe code and datagen builders need early attention.
- `Ingredient` is no longer item-stack-centered in the same way. Oritech's `SizedIngredient`, `FluidIngredient`, and custom recipe codecs need review.
- Many server-only operations require `ServerLevel`/`ServerWorld` explicitly. Do not cast just to compile; server-only logic should be guarded properly.
- Entity rendering begins using render states. Any custom entity/layer rendering or mixins need review.
- Fabric: use Loom 1.8 for this era; Fabric API networking contexts gained Minecraft client/server instances; Loot API v3 replaces v2; `FabricBlockSettings` is removed.

### 1.21.4

Major themes: item model definition JSONs, block model behavior, client datagen, model loading changes.

Oritech impact:

- Item model definitions move to `assets/<namespace>/items`. Oritech has many custom items/tools/block items, so asset generation must change.
- `ItemColors` style tinting is removed in favor of item model definition tints. Any Oritech item tint logic must be moved.
- Block entities now always render their block model. Blocks that previously relied on `ENTITYBLOCK_ANIMATED` behavior must have sane block models.
- Equipment assets move from `assets/<namespace>/models/equipment` to `assets/<namespace>/equipment`.
- Fabric: Loom 1.9, client datagen support, pick-block events moved server-side, `BuiltinItemRenderer`/`BuiltinItemRendererRegistry` removed, model loading callbacks split.

### 1.21.5

Major themes: NBT API cleanup, dynamic registry paths, block replacement behavior, Fabric tag/registry aliases.

Oritech impact:

- `CompoundTag`/NBT access starts becoming stricter and more optional. This is a warning shot before 1.21.6's full value-view rewrite.
- `AbstractBlock#onStateReplaced` changed; block entities should use block-entity-specific replacement hooks. Oritech storage/machine cleanup logic needs audit.
- Dynamic registry JSONs move into namespaced directories. Check any Oritech custom dynamic registries or generated data.
- Fabric: Loom 1.10 requires Gradle 8.12; `TradeOfferHelper` signatures change; tag and registry alias APIs can help preserve renamed Oritech IDs during porting.

### 1.21.6 / 1.21.7 / 1.21.8

Major themes: block entity serialization rewrite, GUI render-state extraction, tag provider rewrite, render pipeline changes.

Oritech impact:

- This is a major Oritech breakpoint. `BlockEntity#saveAdditional`/`loadAdditional` move from `CompoundTag` plus registry lookup to `ValueOutput`/`ValueInput` on NeoForge/Mojang names. Fabric describes the same shift as `WriteView`/`ReadView`.
- Oritech has many `saveAdditional`, `loadAdditional`, `writeNbt`, and `readNbt` paths in machines, tanks, reactors, multiblocks, addon controllers, color helpers, and storage containers. Build a compatibility/helper layer first rather than rewriting each class in isolation.
- GUI rendering changes require full alpha in colors. Calls using `0xffffff` can become invisible and need `0xffffffff` or `ARGB.opaque(...)` style handling.
- `GuiGraphics` methods submit render state instead of drawing directly. Tooltip APIs change from immediate render calls to next-frame/deferred element APIs.
- `AbstractContainerScreen` rendering is split into more explicit content/carried/snapback item stages.
- Tag providers are rewritten. Oritech datagen tag providers need audit.
- Fabric: old HUD callback paths are replaced by `HudElementRegistry`; `BlockRenderLayerMap` moves under rendering v1 and becomes static-style; Rendering API material support is removed; Model Loading API adds extra model registration.

### 1.21.9 / 1.21.10

Major themes: full render submission pipeline, NeoForge transfer API rework, keybinding category changes, FML/runtime classpath changes.

Oritech impact:

- Block entity renderers now use render-state/submission style APIs. Oritech's block entity renderers, GeckoLib-backed renders, machine previews, reactor screen renders, ore finder overlay, cable renderer, and block outline renderer must be migrated deliberately.
- NeoForge removes/changes rendering events such as `RenderHighlightEvent`; custom block outlines move to extraction/submission events and custom outline renderers.
- `Level#isClientSide` becomes private in the 1.21.9 path; code must use the accessor/method form rather than direct field access.
- Key mapping categories become structured category objects. Oritech's `key.oritech.hotkey_category` setup must move to the new category registration model.
- NeoForge transfer rework replaces old APIs:
  - `IItemHandler` -> `ResourceHandler<ItemResource>`
  - `IFluidHandler` -> `ResourceHandler<FluidResource>`
  - `IFluidHandlerItem` -> `ResourceHandler<FluidResource>` with `ItemAccess`
  - `IEnergyStorage` -> `EnergyHandler`
- Oritech's NeoForge transfer layer is high risk: `NeoforgeItemApiImpl`, `NeoforgeFluidApiImpl`, and `NeoforgeEnergyApiImpl` currently wrap old handlers/capabilities. Pipes, machine IO, item/fluid containers, tanks, and batteries depend on this.
- The new NeoForge transfer APIs are transactional. Oritech's common transfer API should either adopt transaction concepts or carefully bridge them at the platform layer.
- NeoForge FML/runtime changes mean dev classpath hacks should be removed; standard Gradle runtime classpath handling is expected.
- Fabric: world render events were removed for 1.21.9 then reintroduced around 1.21.10/1.21.11 with extraction/render separation. Oritech's Fabric world overlays need review.
- Fabric: `Entity#getWorld` is renamed to `Entity#getEntityWorld` in Yarn mappings, resource loader APIs are reworked, screen key events now use context objects, and Loader 0.17 bundles MixinExtras 5.0.0.

### 1.21.11

Major themes: official mapping rename shuffle, final obfuscated release on Fabric, Fabric recipe sync/large packet helpers, game rules and environment attributes.

Oritech impact:

- NeoForge/Mojang names rename `ResourceLocation` to `Identifier` broadly. This touches packet IDs, registry keys, recipe IDs, tags, assets, and any direct ID helpers.
- Utility packages and many entity/model packages are shuffled. Mixins and imports will break heavily.
- Fabric explicitly treats `1.21.11` as the last obfuscated release and recommends moving to Mojang mappings before 26.1. Oritech already appears to be in a Mojang-mapped workspace, which helps, but Fabric module build logic still needs the 26.1 Loom transition.
- Fabric adds `PayloadTypeRegistry#registerLarge` for oversized packets and `RecipeSynchronization.synchronizeRecipeSerializer(...)` for server-to-client recipe sync. Oritech custom recipe visibility in recipe viewers should be checked here.
- Game rules are registry-backed; Fabric provides `GameRuleBuilder` and `GameRuleEvents`.
- Environment attributes replace or absorb various biome/dimension behavior. Oritech should check any biome/worldgen/environment assumptions.

### 26.1

Major themes: Java 25, unobfuscated Minecraft, new Fabric Loom model, templates for stacks/fluids, recipe serializer simplification, rendering rewrite continues.

Oritech impact:

- Java moves from 21 to 25. Update Gradle toolchains, CI, IDE configuration, and local dev docs.
- Gradle must be modern enough: NeoForge release notes call out Gradle 9.1+; Fabric 26.1 recommends Loom 1.15 and Gradle 9.4.0 at the time of writing.
- NeoForge release notes list ModDevGradle around `2.0.141` and NeoForge `26.1.0.1-beta` at that time. Versions will need final verification when actually porting.
- NeoForm versioning changes to `<mc version>-<neoform build>`, for example `26.1-1`.
- Parchment can likely be removed because Mojang parameter names are now available, though keeping it for javadocs may remain possible.
- Fabric 26.1 uses the new `net.fabricmc.fabric-loom` plugin without remapping. Replace `modImplementation`/`modCompileOnly` style dependencies with standard Gradle `implementation`/`compileOnly` where required, and use `jar` instead of `remapJar`.
- Fabric API names are updated from Yarn-style to official-style. Example: `ItemGroupEvents` -> `CreativeModeTabEvents`. Expect more renames in Fabric-specific code.
- `ItemStack` cannot be freely created before registries/world are loaded. Data files and recipes use `ItemStackTemplate`; NeoForge also mentions `FluidStackTemplate`.
- Recipe serializers become records/objects containing `MapCodec` and `StreamCodec`, rather than custom inner serializer classes. Oritech's `OritechRecipeType`, `AugmentDataRecipeType`, and recipe builders must be migrated.
- Recipe result fields and vanilla builders use `ItemStackTemplate`. Oritech datagen currently creates many `new ItemStack(...)` results; those paths need conversion.
- Loot type registrations directly register `MapCodec`s rather than wrapper `*Type` records. Check any custom loot functions/conditions if present.
- Data component initializers and bound components are more important. Oritech's `ComponentContent` and custom item state need audit.
- `GuiGraphics` is renamed/shifted toward `GuiGraphicsExtractor`, and screen methods shift from `render*` to `extract*` style names.
- Old block/item render dispatcher paths are removed or heavily rewritten. `BlockRenderDispatcher`, `ItemRenderer`, `SpecialModelRenderer`, block models, item models, fluid models, and material/sprite APIs all change.
- Chunk render layers become more automatic based on sprite properties. Fabric notes that `BlockRenderLayerMap` style manual registration is no longer the normal path in 26.1.
- Fabric fluid rendering moves toward vanilla `FluidModel`; most `FluidRenderHandler` usage should go away.
- Villager trades are data-driven. This likely matters only if Oritech adds trades or compat trade injection.

## Oritech-Specific Workstreams

### 1. Toolchain and Dependencies

Files to start with:

- `gradle.properties`
- `build.gradle`
- `common/build.gradle`
- `fabric/build.gradle`
- `neoforge/build.gradle`
- `gradle/wrapper/gradle-wrapper.properties`

Tasks:

- Move Java toolchain from 21 to 25.
- Update Gradle wrapper to a 9.x version compatible with both Fabric and NeoForge tooling.
- Update ModDevGradle, Fabric Loom, Fabric Loader, Fabric API, NeoForge, NeoForm, Architectury, GeckoLib, owo-lib, Forge Config API Port, EMI/REI/JEI/Jade, GrandPower, and transfer/energy dependencies.
- Remove obsolete Fabric remap-oriented build assumptions for 26.1.
- Decide whether to keep Parchment only for docs or remove it entirely.

### 2. Registration and Content Setup

Primary targets:

- `ArchitecturyRegistryContainer`
- `BlockContent`
- `ItemContent`
- `BlockEntitiesContent`
- `ComponentContent`
- `RecipeContent`

Tasks:

- Ensure every block/item uses registry-key-aware settings.
- Rebuild block entity type construction around the new APIs.
- Update data component registrations and defaults.
- Audit creative tab APIs and Fabric API renames.
- Validate block loot table keys after `Optional` loot table changes.

### 3. Block Entity Persistence

Primary targets:

- Machine base classes and all block entities under `common/src/main/java/rearth_neosample/oritech/block/entity`
- `MachineAddonController`
- `MultiblockMachineController`
- `ColorableMachine`
- Energy/fluid/item storage containers with `readNbt`/`writeNbt`

Tasks:

- Replace direct `CompoundTag` persistence with `ValueInput`/`ValueOutput` or Fabric's matching `ReadView`/`WriteView` abstraction.
- Build small helper methods for common patterns: optional ints, booleans, positions, item/fluid stacks/templates, lists, and nested objects.
- Keep data migration compatibility for existing worlds where possible. This matters for tanks, inventories, energy, reactors, pipe networks, addons, and color/multiblock state.

### 4. Transfer APIs

Primary targets:

- `common` energy/fluid/item API packages
- `fabric/src/main/java/rearth_neosample/oritech/fabric/FabricItemApi.java` and related Fabric transfer adapters
- `neoforge/src/main/java/rearth_neosample/oritech/neoforge/NeoforgeItemApiImpl.java`
- `neoforge/src/main/java/rearth_neosample/oritech/neoforge/NeoforgeFluidApiImpl.java`
- `neoforge/src/main/java/rearth_neosample/oritech/neoforge/NeoforgeEnergyApiImpl.java`

Tasks:

- Decide whether Oritech common storage APIs should expose transactions or whether the platform layer should bridge transactions internally.
- Replace NeoForge capability registrations and lookups with `Capabilities.Item`, `Capabilities.Fluid`, and `Capabilities.Energy` style APIs.
- Use `ResourceHandler<ItemResource>`, `ResourceHandler<FluidResource>`, `EnergyHandler`, `ItemAccess`, and transaction-aware helpers.
- Re-test pipes and caches thoroughly. Stale storage/capability caches are a known risk area.

### 5. Recipes and Datagen

Primary targets:

- `common/src/main/java/rearth_neosample/oritech/init/recipes`
- `common/src/main/java/rearth_neosample/oritech/api/recipe`
- `fabric/src/data/java/rearth_neosample/oritech/generator`
- compat recipe generators under `fabric/src/data/java`

Tasks:

- Convert custom recipe serializers to `RecipeSerializer<>(MapCodec, StreamCodec)` style.
- Convert recipe outputs and generated results to `ItemStackTemplate` where required.
- Update recipe IDs/keys, recipe display/client sync behavior, and ingredient handling.
- Update Fabric datagen providers to the new provider/generator structure and tag provider APIs.
- Check EMI/REI/JEI custom recipe categories after the base recipes compile.

### 6. Networking and Sync

Primary targets:

- `OritechPlatform`
- `OritechPlatformFabric`
- `OritechPlatformNeoForge`
- `NetworkManager`
- `SyncField`
- `ReflectiveCodecBuilder`
- packet records in tools, particles, augments, and server zipline code

Tasks:

- Update ID types for the `ResourceLocation` -> `Identifier` rename where applicable.
- Replace removed direct buffer helpers with stream codecs such as `Vec3#STREAM_CODEC` where needed.
- Review `StreamCodec#composite` overload changes.
- Consider Fabric `registerLarge` only if any Oritech payloads exceed vanilla packet limits.
- Re-test server/client machine sync, augment install packets, particle payloads, jetpack/laser packets, and zipline packets.

### 7. Screens and GUI Framework

Primary targets:

- `common/src/main/java/rearth_neosample/oritech/api/screen`
- `common/src/main/java/rearth_neosample/oritech/client/ui`
- `EmiItemFilterDragDropHandler`

Tasks:

- Replace immediate `GuiGraphics` drawing assumptions with extraction/submission APIs.
- Fix all text colors to include alpha.
- Update tooltip rendering to next-frame/deferred element APIs.
- Update container screen render method names/stages.
- Re-test machine screens, upgrades/addons overlay, item filter, reactor screen, unstable container screen, and block preview widgets.

### 8. Rendering, Models, and Client Hooks

Primary targets:

- `OritechFabricModClient`
- `OritechClientNeoForge`
- `ActiveCableRenderer`
- `OreFinderRenderer`
- `BlockOutlineRenderer`
- GeckoLib item/block renderers
- block entity renderers and render-bound extensions
- client mixins for cape/elytra/render layers

Tasks:

- Move block entity renderers to render state creation/extraction/submission.
- Replace `RenderHighlightEvent` usage with the new NeoForge outline extraction/render flow.
- Replace Fabric world render hooks with the new world render events once available for the target version.
- Convert direct `MultiBufferSource`/`VertexConsumer` rendering to queue/submission APIs where required.
- Update item model definitions and special model renderers.
- Confirm GeckoLib supports 26.1 before spending time hand-migrating GeckoLib-backed render paths.

### 9. Items, Tools, Armor, and Components

Primary targets:

- powered tools and armor under `common/src/main/java/rearth_neosample/oritech/item/tools`
- `OritechEnergyItem`
- `JetpackItem` and `JetpackElytraItem`
- `PortableLaserItem`
- `PromethiumPickaxeItem`
- `UnstableContainerItem`
- `ComponentContent`

Tasks:

- Move behavior controlled by vanilla components into item properties/components where appropriate: glider, equipment, tool, attack range, damage type, swing/use behavior.
- Avoid pre-world `new ItemStack(...)` in static data-like contexts; use templates where required.
- Rework tooltip extension to component tooltip appender APIs where available.
- Re-test powered item storage, fluid/energy container items, jetpack flight, laser use, wrench interactions, AOE toggles, and target position components.

### 10. Mixins, Access Wideners, and Access Transformers

Primary targets:

- `common/src/main/java/rearth_neosample/oritech/mixin`
- `fabric/src/main/java/rearth_neosample/oritech/fabric/mixin`
- `neoforge/src/main/java/rearth_neosample/oritech/neoforge/mixin`
- Fabric access widener files
- NeoForge access transformers

Tasks:

- Expect render method descriptors to break.
- Retarget mixins only after the equivalent vanilla 26.1 code path is understood.
- Remove mixins made obsolete by new official/Fabric/NeoForge APIs.
- Prefer new public APIs over access hacks when they exist.

## Risk Table

| Area | Risk | Why |
| --- | --- | --- |
| NeoForge transfer | Very high | Old item/fluid/energy capability interfaces are replaced by transactional handlers. |
| Block entity persistence | Very high | Oritech has many `CompoundTag` save/load paths and persistent machine state. |
| Screens/widgets | High | Custom GUI framework depends on old `GuiGraphics` render flow. |
| Renderers/overlays | High | Direct render calls move toward extraction/submission queues and render states. |
| Recipes/datagen | High | Custom serializers, custom builders, and generated outputs need the 26.1 recipe/template model. |
| Toolchain/build | High | Java 25, Gradle 9+, Fabric unobfuscated Loom, NeoForm versioning. |
| Networking | Medium | Already modern, but ID/codecs/vector helpers and recipe sync need audit. |
| Registrations | Medium-high | Registry-key-backed settings affect almost every block/item. |
| Compat | Medium-high | Depends on when external mods publish 26.1-compatible APIs. |
| Mixins | Medium-high | Many signatures and package names change; some hooks may be obsolete. |

## Practical Success Criteria

A useful first milestone is not "everything renders." It is:

- Both loader modules import the 26.1 Minecraft sources.
- Common registration compiles.
- A dev client reaches title screen on both Fabric and NeoForge.
- A basic world can load with Oritech registered.
- One simple machine block entity can save/load state.
- One item transfer, one fluid transfer, and one energy transfer path work on each loader.
- One custom recipe is generated, loaded, matched, and shown to a recipe viewer or synchronized path.
- One basic machine screen opens.

Only after that should the more complex renderer, reactor, pipe, addon, and compat work happen. Otherwise the port will turn into a pile of unrelated compiler errors with no stable checkpoint.
