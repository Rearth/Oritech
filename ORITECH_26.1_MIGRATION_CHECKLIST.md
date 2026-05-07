# Oritech 26.1 Migration Checklist

Use this as the working checklist for the 1.21.1 -> 26.1 port. Keep the primer as the explanation layer; this file is the execution layer.

## 0. Branch and Baseline

- [ ] Create a dedicated 26.1 migration branch.
- [ ] Keep a clean 1.21.1 branch/tag available for comparison.
- [ ] Record current successful build/datagen commands and expected outputs.
- [ ] Record a small test-world scenario: one basic machine, item pipe, fluid tank, energy transfer, reactor/multiblock, jetpack, and recipe viewer.
- [ ] Decide whether the first compile target is common-only, Fabric-first, NeoForge-first, or both loaders together.

## 1. Toolchain and Build Files

Status note:

- Initial 26.1.2 build-system migration is in place. Gradle config resolves and `:common:compileJava` reaches project source compilation before failing on expected 26.1 API migration errors.
- Architectury `19.0.2` is wired from local PR jars under `E:/Data/Downloads/[26.1.1] architectury-19.0.2`.
- GeckoLib `5.5.1` is restored through the new `com.geckolib` Fabric/NeoForge `26.1.2` artifacts. Direct POM probes resolve and Gradle reaches Java compilation with the dependency present.
- JEI is updated to `29.5.0.28`; direct POM probes and Gradle dependency insight resolve for the `26.1.2` common, Fabric, and NeoForge artifacts.
- NeoForge's `data` IDE run now uses the ModDevGradle 2 `serverData` run type; the IntelliJ sync task sequence including `:neoforge:prepareDataRun` passes.
- EMI, REI, Jade, Oracle Index, GrandPower, and old runtime-only extras are parked until 26.1-compatible artifacts or replacement APIs are available.
- Datagen dependencies are guarded behind `enable_datagen_deps=false` and intentionally skipped for this pass.

Files:

- `gradle.properties`
- `build.gradle`
- `settings.gradle`
- `common/build.gradle`
- `fabric/build.gradle`
- `neoforge/build.gradle`
- `gradle/wrapper/gradle-wrapper.properties`

Tasks:

- [x] Update Gradle wrapper to a 9.x version compatible with both loader toolchains.
- [x] Move Java toolchain from 21 to 25.
- [x] Update local/CI Java configuration to Java 25.
- [x] Update NeoForge/ModDevGradle/NeoForm versions for 26.1.
- [x] Update Fabric Loom to the 26.1 unobfuscated plugin model.
- [x] Replace Fabric remapping-oriented dependency/task usage where required.
- [x] Update Fabric Loader and Fabric API.
- [x] Update Architectury API or decide whether it still supports the desired 26.1 setup.
- [x] Update GeckoLib after confirming 26.1 support.
- [x] Update or park owo-lib, Forge Config API Port, EMI, REI, JEI, Jade, GrandPower, and other compat dependencies.
- [x] Decide whether Parchment remains useful for docs or should be removed.
- [x] Verify Gradle can resolve dependencies and generate/import sources.

## 2. Mapping and Naming Pass

Tasks:

- [ ] Confirm the workspace is using official/unobfuscated names consistently.
- [ ] Update imports and references affected by `ResourceLocation` -> `Identifier` where applicable.
- [ ] Update utility package moves under `net.minecraft.util`.
- [ ] Update renamed `criterion`/advancement/loot package references.
- [ ] Update Fabric API Yarn-style names to official-style names.
- [ ] Run a targeted mixin descriptor audit after imports compile.

## 3. Registration Compile Baseline

Files:

- `ArchitecturyRegistryContainer`
- `BlockContent`
- `ItemContent`
- `BlockEntitiesContent`
- `ComponentContent`
- `RecipeContent`
- menu/sound/particle/tag init classes

Tasks:

- [ ] Add registry keys to all item settings.
- [ ] Add registry keys to all block settings.
- [ ] Add registry keys to entity type builders, if Oritech entity types are present.
- [ ] Update block item translation-key behavior where needed.
- [ ] Update block entity type builders and valid-block wiring.
- [ ] Update creative tab/event APIs.
- [ ] Update data component registration and default component initialization.
- [ ] Update fuel registration events.
- [ ] Compile until common registration errors are resolved.

## 4. Block Entity Persistence

Files and areas:

- `common/src/main/java/rearth/oritech/block/entity`
- `MachineAddonController`
- `MultiblockMachineController`
- `ColorableMachine`
- energy/fluid/item storage containers
- reactor, refinery, tank, storage, pipe, accelerator, augmenter entities

Tasks:

- [ ] Create helper methods for the new read/write view API.
- [ ] Replace direct `CompoundTag` save/load in base machine classes first.
- [ ] Replace storage container `readNbt`/`writeNbt` APIs.
- [ ] Migrate item stack persistence to template/component-safe APIs where needed.
- [ ] Migrate fluid persistence to template/resource-safe APIs where needed.
- [ ] Preserve old world data compatibility where feasible.
- [ ] Re-test one simple machine save/load before migrating every specialized machine.
- [ ] Migrate reactor/multiblock/addon state after the base helpers are stable.

## 5. Transfer APIs

Files:

- common energy/fluid/item API packages
- `fabric/src/main/java/rearth/oritech/fabric/FabricItemApi.java`
- Fabric fluid/energy adapters
- `neoforge/src/main/java/rearth/oritech/neoforge/NeoforgeItemApiImpl.java`
- `neoforge/src/main/java/rearth/oritech/neoforge/NeoforgeFluidApiImpl.java`
- `neoforge/src/main/java/rearth/oritech/neoforge/NeoforgeEnergyApiImpl.java`

Tasks:

- [ ] Decide whether Oritech common APIs expose transactions or bridge them internally.
- [ ] Replace NeoForge item capabilities with `ResourceHandler<ItemResource>`.
- [ ] Replace NeoForge fluid capabilities with `ResourceHandler<FluidResource>`.
- [ ] Replace NeoForge item-fluid capability assumptions with `ItemAccess`.
- [ ] Replace NeoForge energy capabilities with `EnergyHandler`.
- [ ] Update capability registration keys and provider signatures.
- [ ] Update block capability cache creation.
- [ ] Rework wrapper classes before touching every machine implementation.
- [ ] Re-test item insertion/extraction with simulation and real operations.
- [ ] Re-test fluid fill/drain with tanks and fluid container items.
- [ ] Re-test energy insert/extract with batteries and machines.
- [ ] Re-test pipes with unloaded/reloaded chunks.

## 6. Recipes and Datagen

Files:

- `common/src/main/java/rearth/oritech/init/recipes`
- `common/src/main/java/rearth/oritech/api/recipe`
- `fabric/src/data/java/rearth/oritech/generator`
- compat recipe generators

Tasks:

- [ ] Convert custom recipe serializers to `MapCodec` plus `StreamCodec` records/objects.
- [ ] Convert recipe outputs to `ItemStackTemplate` where required.
- [ ] Update custom machine recipe builders.
- [ ] Update generated vanilla recipes.
- [ ] Update generated custom machine recipes.
- [ ] Update `SizedIngredient` and `FluidIngredient` codecs.
- [ ] Update recipe IDs/registry keys and client recipe display assumptions.
- [ ] Add Fabric recipe synchronization for custom recipe serializers if recipe viewers or clients need them.
- [ ] Update datagen provider APIs.
- [ ] Update tag provider APIs.
- [ ] Run datagen and inspect generated recipe JSON for one simple and one complex machine recipe.

## 7. Networking and Sync

Files:

- `OritechPlatform`
- `OritechPlatformFabric`
- `OritechPlatformNeoForge`
- `NetworkManager`
- `SyncField`
- `ReflectiveCodecBuilder`
- packet records in tools, particles, augments, and zipline code

Tasks:

- [ ] Update packet ID types and constructors.
- [ ] Update `StreamCodec` composition APIs.
- [ ] Replace removed buffer read/write helpers with codecs.
- [ ] Update `Vec3`, `BlockPos`, identifier, item/fluid/resource codecs.
- [ ] Review payload registration signatures on both loaders.
- [ ] Use Fabric large-payload registration only for payloads that need it.
- [ ] Re-test machine screen sync.
- [ ] Re-test augment install/sync.
- [ ] Re-test particle payloads.
- [ ] Re-test laser, jetpack, and zipline packets.

## 8. Screens and Widgets

Files:

- `common/src/main/java/rearth/oritech/api/screen`
- `common/src/main/java/rearth/oritech/client/ui`
- EMI drag/drop screen integration

Tasks:

- [ ] Convert screen rendering to extraction/submission APIs.
- [ ] Update `GuiGraphics`/`GuiGraphicsExtractor` method names and call flow.
- [ ] Fix colors to include alpha.
- [ ] Update tooltip rendering to deferred/next-frame APIs.
- [ ] Update container screen background/content/carried-item stages.
- [ ] Update custom widgets: labels, boxes, textures, item widgets, scroll widgets, overlays, block preview widgets.
- [ ] Re-test basic machine screen.
- [ ] Re-test upgrade/addon overlay.
- [ ] Re-test reactor screen preview rendering.
- [ ] Re-test item filter screen and drag/drop behavior.

## 9. Rendering and Client Hooks

Files and areas:

- `OritechFabricModClient`
- `OritechClientNeoForge`
- `ActiveCableRenderer`
- `OreFinderRenderer`
- `BlockOutlineRenderer`
- block entity renderers
- GeckoLib item/block renderers
- reactor preview rendering
- cape/elytra/render-layer mixins

Tasks:

- [ ] Confirm GeckoLib 26.1 renderer APIs before hand-migrating Oritech renderers.
- [ ] Convert block entity renderers to render state APIs.
- [ ] Convert world overlays to extraction/render event model.
- [ ] Replace NeoForge highlight event usage with the new outline renderer flow.
- [ ] Replace direct `MultiBufferSource`/`VertexConsumer` assumptions where the new queue API requires it.
- [ ] Update item model definition JSON generation/placement.
- [ ] Update special item model renderers.
- [ ] Update block render layer assumptions and remove obsolete manual layer registrations.
- [ ] Update fluid rendering to vanilla `FluidModel` where applicable.
- [ ] Re-test cable surfer rendering, ore finder overlay, block outlines, machine BERs, item renderers, jetpack/cape/elytra visuals.

## 10. Items, Tools, Armor, and Components

Files and areas:

- powered tools and armor packages
- `OritechEnergyItem`
- `JetpackItem`
- `JetpackElytraItem`
- `PortableLaserItem`
- `PromethiumPickaxeItem`
- `UnstableContainerItem`
- `ComponentContent`

Tasks:

- [ ] Move vanilla-supported behavior into data components where appropriate.
- [ ] Update glider/elytra-like behavior.
- [ ] Update tool, attack, use, and equipment components.
- [ ] Replace static/pre-world `ItemStack` creation where templates are required.
- [ ] Update item tooltip APIs.
- [ ] Re-test powered tool energy drain.
- [ ] Re-test jetpack movement and fuel/energy sync.
- [ ] Re-test laser use and targeting.
- [ ] Re-test unstable container behavior.
- [ ] Re-test wrench and machine interaction behavior.

## 11. Mixins and Access Changes

Files and areas:

- common mixins
- Fabric mixins and access wideners
- NeoForge mixins and access transformers

Tasks:

- [ ] Disable broken mixins only long enough to reach compile checkpoints.
- [ ] Retarget render method descriptors after equivalent vanilla methods are found.
- [ ] Remove mixins made obsolete by new public APIs.
- [ ] Update access wideners for renamed classes/methods/fields.
- [ ] Update access transformers for renamed classes/methods/fields.
- [ ] Verify both client and dedicated server startup after mixin retargeting.

## 12. Compat Restore Pass

Compat targets:

- EMI
- REI
- JEI
- Jade
- Tech Reborn
- GrandPower
- owo-lib
- Forge Config API Port

Tasks:

- [ ] Restore compat dependencies only after core gameplay compiles.
- [ ] Update recipe category APIs.
- [ ] Update recipe sync/client recipe display assumptions.
- [ ] Update Jade block/entity data providers.
- [ ] Update Tech Reborn compat recipes and tags.
- [ ] Update GrandPower/energy bridge or remove it if superseded by NeoForge 26.1 energy APIs.
- [ ] Test recipe viewers on both loaders.

## 13. Runtime Test Matrix

Fabric:

- [ ] Client reaches title screen.
- [ ] Dedicated server starts.
- [ ] New world loads.
- [ ] Existing migrated test world loads.
- [ ] Datagen completes.
- [ ] Basic machine works.
- [ ] Item/fluid/energy transfer works.
- [ ] Recipe viewer shows Oritech recipes.
- [ ] World overlays render.

NeoForge:

- [ ] Client reaches title screen.
- [ ] Dedicated server starts.
- [ ] New world loads.
- [ ] Existing migrated test world loads.
- [ ] Datagen completes.
- [ ] Basic machine works.
- [ ] Item/fluid/energy transfer works.
- [ ] Recipe viewer shows Oritech recipes.
- [ ] World overlays render.

Cross-loader behavior:

- [ ] Same recipes generated for both loaders where intended.
- [ ] Same machine IO behavior on both loaders.
- [ ] Same item/fluid/energy values on both loaders.
- [ ] Same config defaults on both loaders.
- [ ] Same networking behavior in singleplayer and dedicated server.

## First Real Milestone

Treat this as the first meaningful port milestone:

- [ ] 26.1 sources import in IDE.
- [ ] Common registrations compile.
- [ ] Fabric client reaches title screen.
- [ ] NeoForge client reaches title screen.
- [ ] One simple machine places, opens its screen, saves/loads state, and processes one recipe.
- [ ] One item transfer path works.
- [ ] One fluid transfer path works.
- [ ] One energy transfer path works.

Everything after that is still a lot of work, but at least the port has a spine.
