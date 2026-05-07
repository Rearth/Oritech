# Oritech Cross-Platform Transfer API Design Options

This is a design plan for a 26.1-era Oritech transfer API. It focuses on three approaches:

- Option 1: Hybrid transaction core with thin platform adapters.
- Option 2: Platform-native exposure through loader-specific mixins/adapters.
- Option 4: Generic resource API for item/fluid/energy transfer.

The goal is to support Fabric and NeoForge transactions without creating wrapper classes for every special machine, while still letting individual containers override insertion, extraction, slot visibility, side rules, and commit behavior.

## Current Oritech Baseline

The current common API is simulation-based:

- Items: `ItemApi.InventoryStorage#insert/extract(..., boolean simulate)`.
- Fluids: `FluidApi.FluidStorage#insert/extract(..., boolean simulate)`.
- Energy: `EnergyApi.EnergyStorage#insert/extract(..., boolean simulate)`.
- Mutations call `update()` manually or from wrappers after non-simulated changes.

Fabric already has transactional platform APIs. Current Fabric wrappers use `SnapshotParticipant` and translate a Fabric transaction into Oritech simulation-style mutations.

NeoForge 1.21.1 currently wraps old handler APIs such as `IItemHandler`, `IFluidHandler`, and GrandPower energy. In the 26.1 path, NeoForge moves toward transactional `ResourceHandler<ItemResource>`, `ResourceHandler<FluidResource>`, `EnergyHandler`, `ItemAccess`, and `Transaction`.

Important constraint from prior pipe/cache bugs: do not cache live cross-block storage wrapper objects long-term. Cache lookup handles or target positions/directions, and resolve storage fresh for each transfer tick or operation.

## Recommendation

Option 1 is the best default architecture.

No, the mixin-first idea is not the right main design. It sounds like it reduces wrappers, but it mostly moves wrapper complexity into harder-to-debug loader-specific bytecode behavior. Use mixins only as a tactical compatibility bridge for classes that cannot otherwise implement a new interface cleanly.

Option 4 is attractive if you are willing to redesign deeply, but it risks over-abstracting energy into an item/fluid-shaped API. It is worth borrowing concepts from it, especially resource/slot views, but I would not make a single fully generic item/fluid/energy API the first port milestone unless you intentionally want a transfer-system rewrite before most gameplay compiles.

## Shared Concepts

The useful common ideas across all options are:

- Storages should expose views by side/context, not one global container for all interactions.
- Mutations should happen inside a transaction context.
- Updates should fire once on commit, not once per simulated/internal operation.
- Special containers should override rules through small methods or slot policies, not through new platform wrappers.
- Platform adapters should be few and generic: one item adapter, one fluid adapter, one energy adapter per platform.
- External storages from other mods still need adapters. Avoiding all wrappers is not realistic because Oritech and external platform APIs have different types.

## Option 1: Hybrid Transaction Core

### Concept

Make Oritech's common storage types transaction-aware, while preserving simple machine-facing methods where possible. Platform adapters become thin translators from Fabric/NeoForge transactions into Oritech transactions.

The key is: Oritech storage owns the state and snapshot behavior. Fabric and NeoForge wrappers no longer have to know how to snapshot every storage shape.

### Common API Sketch

```java
public interface TransferTransaction extends AutoCloseable {
    void commit();
    boolean isCommitted();
    <S> void snapshot(TransactionalParticipant<S> participant);
}

public interface TransactionalParticipant<S> {
    S createSnapshot();
    void restoreSnapshot(S snapshot);
    default void onCommitted() {}
}

public interface OritechTransferStorage<R> {
    long insert(R resource, long amount, TransferTransaction transaction);
    long extract(R resource, long amount, TransferTransaction transaction);
    boolean supportsInsertion();
    boolean supportsExtraction();
    void onFinalCommit();
}
```

Keep compatibility methods during migration:

```java
default long insert(R resource, long amount, boolean simulate) {
    try (var tx = OritechTransactions.open()) {
        long inserted = insert(resource, amount, tx);
        if (!simulate) tx.commit();
        return inserted;
    }
}
```

### Example Item Storage

```java
public class OritechItemStorage implements OritechTransferStorage<ItemStack>, TransactionalParticipant<List<ItemStack>> {
    private final NonNullList<ItemStack> stacks;
    private final Runnable onCommit;
    private final List<ItemSlotRule> rules;

    @Override
    public long insert(ItemStack stack, long amount, TransferTransaction tx) {
        if (stack.isEmpty() || amount <= 0) return 0;
        tx.snapshot(this);

        long remaining = amount;
        for (int slot = 0; slot < stacks.size() && remaining > 0; slot++) {
            if (!rules.get(slot).canInsert(stack)) continue;
            remaining -= insertIntoSlot(slot, stack, remaining);
        }
        return amount - remaining;
    }

    @Override
    public long extract(ItemStack stack, long amount, TransferTransaction tx) {
        if (stack.isEmpty() || amount <= 0) return 0;
        tx.snapshot(this);

        long remaining = amount;
        for (int slot = 0; slot < stacks.size() && remaining > 0; slot++) {
            if (!rules.get(slot).canExtract(stack)) continue;
            remaining -= extractFromSlot(slot, stack, remaining);
        }
        return amount - remaining;
    }

    @Override
    public List<ItemStack> createSnapshot() {
        return stacks.stream().map(ItemStack::copy).toList();
    }

    @Override
    public void restoreSnapshot(List<ItemStack> snapshot) {
        for (int i = 0; i < snapshot.size(); i++) {
            stacks.set(i, snapshot.get(i));
        }
    }

    @Override
    public void onCommitted() {
        onCommit.run();
    }
}
```

`InOutInventoryStorage` becomes a rule configuration, not necessarily a subclass:

```java
public record ItemSlotRule(boolean input, boolean output, int limit) {
    boolean canInsert(ItemStack stack) { return input; }
    boolean canExtract(ItemStack stack) { return output; }
}
```

Special machines can still override behavior:

```java
public final class FilteredItemStorage extends OritechItemStorage {
    @Override
    protected boolean canInsertIntoSlot(int slot, ItemStack stack) {
        return super.canInsertIntoSlot(slot, stack) && filterMatches(slot, stack);
    }
}
```

### Fabric Adapter

Fabric wrapper becomes generic and does not need storage-specific snapshot logic:

```java
public final class FabricItemStorageAdapter implements Storage<ItemVariant>, SlottedStorage<ItemVariant> {
    private final OritechItemStorage storage;

    @Override
    public long insert(ItemVariant variant, long maxAmount, TransactionContext fabricTx) {
        var tx = FabricOritechTransaction.wrap(fabricTx);
        return storage.insert(variant.toStack(), maxAmount, tx);
    }

    @Override
    public long extract(ItemVariant variant, long maxAmount, TransactionContext fabricTx) {
        var tx = FabricOritechTransaction.wrap(fabricTx);
        return storage.extract(variant.toStack(), maxAmount, tx);
    }
}
```

`FabricOritechTransaction.wrap(...)` registers one close callback with Fabric. On Fabric commit, it commits the Oritech transaction. On abort, it restores snapshots.

For external Fabric storages, use the inverse adapter:

```java
public final class FabricExternalItemStorage implements OritechTransferStorage<ItemStack> {
    private final Storage<ItemVariant> fabricStorage;

    @Override
    public long insert(ItemStack stack, long amount, TransferTransaction tx) {
        var fabricTx = tx.getOrCreatePlatformTransaction(FabricTransactions.KEY);
        return fabricStorage.insert(ItemVariant.of(stack), amount, fabricTx);
    }
}
```

### NeoForge Adapter

For 26.1 NeoForge, expose the same Oritech storage as a NeoForge resource handler:

```java
public final class NeoForgeItemStorageAdapter implements ResourceHandler<ItemResource> {
    private final OritechItemStorage storage;

    @Override
    public long insert(ItemResource resource, long amount, TransactionContext neoTx) {
        var tx = NeoForgeOritechTransaction.wrap(neoTx);
        return storage.insert(resource.toStack((int) amount), amount, tx);
    }

    @Override
    public long extract(ItemResource resource, long amount, TransactionContext neoTx) {
        var tx = NeoForgeOritechTransaction.wrap(neoTx);
        return storage.extract(resource.toStack((int) amount), amount, tx);
    }
}
```

For NeoForge item containers, item context matters. Item-backed storages should take an item access/context object, not only an `ItemStack`, because the stack may need to be exchanged back into a slot/cursor/player inventory on commit.

```java
public interface OritechItemAccess {
    ItemStack getStack();
    void setStack(ItemStack stack);
    void syncOnCommit(TransferTransaction tx);
}
```

### Pros

- Best balance of correctness and maintainability.
- Common storages own transaction semantics.
- Platform wrappers become thin and generic.
- Special containers override common rules without platform-specific classes.
- Fabric and NeoForge both map naturally to the transaction model.
- Existing machine code can migrate gradually through compatibility methods.

### Cons

- Medium-to-high common API churn.
- Requires careful transaction implementation and tests.
- External storage adapters still exist.
- Energy may need a specialized transaction path instead of pretending to be a resource stack.

### Best Use

Use this if the goal is a robust 26.1 API that can survive future platform churn.

## Option 2: Platform-Native Exposure via Mixins/Adapters

### Concept

Keep Oritech common storage classes mostly as-is, but make loader-specific source sets expose them directly as Fabric/NeoForge storage interfaces. This can be done with platform subclasses, adapter interfaces, or mixins into Oritech's own classes.

Example idea:

- On Fabric, inject `Storage<ItemVariant>` / `SlottedStorage<ItemVariant>` behavior into `SimpleInventoryStorage`.
- On NeoForge, inject `ResourceHandler<ItemResource>` behavior into `SimpleInventoryStorage`.
- Machine block entities still return `SimpleInventoryStorage`; platform lookup sees that it also implements the platform-native interface.

### Mixin Sketch

Fabric-side mixin:

```java
@Mixin(SimpleInventoryStorage.class)
public abstract class SimpleInventoryStorageFabricMixin implements SlottedStorage<ItemVariant> {
    @Override
    public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
        var self = (SimpleInventoryStorage) (Object) this;
        // Snapshot through Fabric's transaction system.
        // Delegate to self.insert(..., false).
    }
}
```

NeoForge-side mixin:

```java
@Mixin(SimpleInventoryStorage.class)
public abstract class SimpleInventoryStorageNeoForgeMixin implements ResourceHandler<ItemResource> {
    @Override
    public long insert(ItemResource resource, long amount, TransactionContext transaction) {
        var self = (SimpleInventoryStorage) (Object) this;
        // Snapshot through NeoForge transaction system.
        // Delegate to self.insert(..., false).
    }
}
```

### Cleaner Variant: Platform Extension Interfaces

Instead of mixins, use platform-only interfaces and small bridge objects:

```java
public interface FabricExposedItemStorage extends SlottedStorage<ItemVariant> {
    SimpleInventoryStorage oritech$self();
}
```

But common classes cannot directly implement Fabric/NeoForge interfaces because common code must compile without loader-specific types. That means either mixins, platform subclasses, or adapters are still required.

### Fabric Details

Fabric exposure still needs snapshot handling. If the mixin directly calls `self.insert(..., false)` inside a Fabric transaction, it must also restore state when the transaction aborts. That means each mixin needs logic similar to the current `SnapshotParticipant` wrappers.

You can avoid repeating that by adding common snapshot methods:

```java
public interface SnapshotBackedStorage<S> {
    S createSnapshot();
    void restoreSnapshot(S snapshot);
    void onFinalCommit();
}
```

Then the Fabric mixin can be thin.

### NeoForge Details

NeoForge 26.1 will also need transaction snapshot/commit behavior. Item-backed storage additionally needs `ItemAccess` handling. A mixin into `SimpleItemFluidStorage` or `SimpleEnergyItemStorage` does not automatically know whether the item is in a player slot, cursor, handler index, or standalone stack. That context still has to come from platform lookup.

### Pros

- Fewer visible wrapper objects for Oritech-owned storages.
- Platform APIs can see Oritech storages directly.
- Low machine call-site churn if common APIs stay mostly unchanged.

### Cons

- Brittle across loader/plugin/mixin changes.
- Harder to debug because behavior appears on classes only at runtime.
- Common classes still cannot type-check against platform APIs.
- You still need wrappers for external storages from other mods.
- You still need context adapters for item-backed storages.
- Transaction code may be duplicated across mixins unless common snapshot interfaces are added.

### Best Use

Use this tactically, not as the main architecture. It is reasonable for a short-term compile bridge or for one very hot path where wrapper allocation is proven expensive. It is not the cleanest foundation for the 26.1 port.

## Option 4: Generic Resource API

### Concept

Build one common transfer model for item, fluid, and energy. Treat everything as a resource plus amount inside views/slots.

```java
public interface OritechResource {
    boolean isBlank();
}

public record ItemTransferResource(ItemStack prototype) implements OritechResource {}
public record FluidTransferResource(Fluid fluid, DataComponentPatch components) implements OritechResource {}
public enum EnergyTransferResource implements OritechResource { INSTANCE }

public interface ResourceStorage<R extends OritechResource> {
    long insert(R resource, long amount, TransferTransaction tx);
    long extract(R resource, long amount, TransferTransaction tx);
    Iterable<ResourceView<R>> views();
}

public interface ResourceView<R extends OritechResource> {
    R resource();
    long amount();
    long capacity();
    boolean canInsert(R resource);
    boolean canExtract(R resource);
}
```

### Example Generic Item Storage

```java
public final class GenericItemStorage implements ResourceStorage<ItemTransferResource>, TransactionalParticipant<List<ItemStack>> {
    private final NonNullList<ItemStack> stacks;
    private final List<ResourceSlotPolicy<ItemTransferResource>> policies;

    @Override
    public long insert(ItemTransferResource resource, long amount, TransferTransaction tx) {
        tx.snapshot(this);
        ItemStack stack = resource.prototype().copyWithCount((int) amount);
        return insertStackByPolicy(stack, policies);
    }

    @Override
    public long extract(ItemTransferResource resource, long amount, TransferTransaction tx) {
        tx.snapshot(this);
        ItemStack stack = resource.prototype().copyWithCount((int) amount);
        return extractStackByPolicy(stack, policies);
    }
}
```

### Example Generic Fluid Storage

```java
public final class GenericFluidStorage implements ResourceStorage<FluidTransferResource>, TransactionalParticipant<List<FluidStack>> {
    @Override
    public long insert(FluidTransferResource resource, long amount, TransferTransaction tx) {
        tx.snapshot(this);
        var stack = FluidStack.create(resource.fluid(), amount, resource.components());
        return insertFluidByPolicy(stack);
    }
}
```

### Example Generic Energy Storage

```java
public final class GenericEnergyStorage implements ResourceStorage<EnergyTransferResource>, TransactionalParticipant<Long> {
    private long amount;
    private long capacity;

    @Override
    public long insert(EnergyTransferResource resource, long maxAmount, TransferTransaction tx) {
        tx.snapshot(this);
        long inserted = Math.min(maxAmount, capacity - amount);
        amount += inserted;
        return inserted;
    }
}
```

### Fabric Details

Fabric adapters become resource-specific translators:

```java
public final class FabricItemResourceAdapter implements Storage<ItemVariant> {
    private final ResourceStorage<ItemTransferResource> storage;

    @Override
    public long insert(ItemVariant variant, long amount, TransactionContext tx) {
        return storage.insert(new ItemTransferResource(variant.toStack()), amount, FabricOritechTransaction.wrap(tx));
    }
}
```

Energy still maps to Team Reborn Energy, not Fabric Transfer item/fluid resources:

```java
public final class FabricEnergyResourceAdapter implements EnergyStorage {
    private final ResourceStorage<EnergyTransferResource> storage;

    @Override
    public long insert(long maxAmount, TransactionContext tx) {
        return storage.insert(EnergyTransferResource.INSTANCE, maxAmount, FabricOritechTransaction.wrap(tx));
    }
}
```

### NeoForge Details

NeoForge item/fluid resources map naturally:

```java
public final class NeoForgeItemResourceAdapter implements ResourceHandler<ItemResource> {
    private final ResourceStorage<ItemTransferResource> storage;

    @Override
    public long insert(ItemResource resource, long amount, TransactionContext tx) {
        var oritechResource = new ItemTransferResource(resource.toStack(1));
        return storage.insert(oritechResource, amount, NeoForgeOritechTransaction.wrap(tx));
    }
}
```

Energy gets a separate adapter even in the generic model:

```java
public final class NeoForgeEnergyResourceAdapter implements EnergyHandler {
    private final ResourceStorage<EnergyTransferResource> storage;
}
```

### Pros

- Most unified long-term model.
- One rule/view/transaction framework can serve items, fluids, and energy.
- Good match for NeoForge's resource-oriented transfer direction.
- Special containers can be expressed through resource views and policies.

### Cons

- Highest churn.
- Easy to over-engineer.
- Energy is semantically different and may feel awkward as a resource.
- Item stacks and fluid stacks have component/template concerns that do not map perfectly to one abstraction.
- Many existing call sites will need migration or compatibility shims.

### Best Use

Use this only if you want to redesign transfer as a core Oritech subsystem, not just port to 26.1.

## How Special Containers Override Behavior

Whichever option wins, special behavior should live in common policies/views, not in platform wrappers.

Examples:

```java
public interface TransferSideView<S> {
    S storageForSide(@Nullable Direction side);
}

public interface SlotPolicy<R> {
    boolean canInsert(R resource);
    boolean canExtract(R resource);
    long maxAmount(R resource);
}
```

Use cases:

- Machine input/output slots: input slots deny extraction; output slots deny insertion.
- Filters: insertion checks an item/fluid predicate.
- Tanks: side-specific views expose only input/output tank.
- Creative storage: insertion/extraction changes behavior but still shares the API.
- Reactor ports: delegate to controller state but snapshot the port/controller mutation consistently.
- Pipe interfaces: resolve target storage fresh each tick and perform one transaction per transfer operation.

## Wrapper Count Strategy

The realistic target is not zero wrappers. The realistic target is a small fixed set:

- Fabric internal item adapter: Oritech -> Fabric.
- Fabric external item adapter: Fabric -> Oritech.
- Fabric internal fluid adapter.
- Fabric external fluid adapter.
- Fabric internal energy adapter.
- Fabric external energy adapter.
- NeoForge equivalents for item/fluid/energy.

That sounds like several classes, but it is stable. What you want to avoid is wrappers per machine type, per tank type, per pipe type, or per special container.

## Migration Plan If Choosing Option 1

1. Add transaction interfaces in common.
2. Add snapshot participant support to existing simple item/fluid/energy storages.
3. Add compatibility methods so existing `simulate` call sites still compile.
4. Move `update()` to commit callbacks where possible.
5. Replace Fabric wrappers with transaction-bridge wrappers that do not own storage-specific snapshot logic.
6. Port NeoForge wrappers to 26.1 `ResourceHandler`/`EnergyHandler` using the same transaction bridge.
7. Convert `InOutInventoryStorage`, `SimpleInOutFluidStorage`, and delegating storages to side/slot policy views.
8. Migrate pipe transfer code to use one explicit transaction per move.
9. Only then remove old simulation-only APIs if they are no longer needed.

## Main Pitfalls

- Do not call `update()` during simulation or aborted transaction paths.
- Do not commit source extraction before destination insertion is guaranteed.
- Do not cache live external storage wrappers across chunk unload/reload.
- Do not make common classes reference Fabric or NeoForge types.
- Do not use mixins as a substitute for a clear common transaction model.
- Do not force energy into the same API shape if it makes every energy call harder to read.

## Short Decision

Pick Option 1 as the main architecture.

Borrow from Option 4 for slot/resource views and policies.

Use Option 2 only as a tactical bridge when direct platform exposure is truly worth the runtime complexity.
