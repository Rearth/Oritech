# Smart Splitter Plan

## Goal

Add a single-block item splitter with one internal stack. Items can be inserted normally, while any item transport or automation system can pull items from its horizontal faces according to a selectable distribution mode.

Output faces are configured on the splitter itself by right-clicking a horizontal face with an empty hand. Sneak-right-clicking cycles the distribution mode. This avoids inspecting neighboring blocks or depending on any particular pipe implementation. Each configured horizontal output exposes a side-aware NeoForge `ResourceHandler<ItemResource>` that enforces the selected mode entirely through the transaction API.

Insertion remains available from every face, including configured output faces. Extraction rules are always based on the face through which the capability was requested.

A capability request without a side may expose insertion but must not permit extraction, because there is no output face to charge. This prevents unsided automation from bypassing the distribution policy.

## Distribution modes

### Strict

Newly inserted items are reserved evenly between all configured outputs. Each side can extract only its own reservation. If an output is blocked or unused, its items remain stored and cannot be taken by another output. This works regardless of which mod owns the extracting transport.

### Overflow

Items initially receive the same even reservations as in Strict mode. If an output does not complete an extraction within a short grace period, its remaining reservation is redistributed between the other active outputs.

A committed extraction refreshes that output's activity timestamp. Simulated or rolled-back extractions must not do so. A blocked extractor therefore leaves its reservation untouched until the grace period expires.

### Round Robin

Only the output at the current cursor may extract. After one successful extraction operation, the cursor advances to the next active output. The entire amount accepted during that operation may move, so this mode balances operations rather than individual items and retains boosted-pipe throughput.

A blocked current output waits by default. It may be skipped after the same grace period used by Overflow mode.

## State and transaction handling

The block entity needs:

- One single-stack inventory.
- The selected distribution mode.
- The splitter's configured horizontal output faces.
- Per-output-face reservation counts for Strict and Overflow modes.
- Per-side last-successful-extraction timestamps.
- A persistent Round Robin cursor.
- A rotating remainder cursor for fair division of non-divisible insertion amounts.

Inventory changes, reservations, timestamps, and the Round Robin cursor must participate in the caller's transaction, using `SnapshotJournal` or an equivalent transactional state holder. Extractors may simulate or nest transactions before performing a real extraction. Only a committed extraction may consume a reservation or advance the cursor.

When an output face is enabled or disabled, rebalance the currently stored items among all configured outputs. When no outputs exist, retain all items. Neighbor changes alone do not alter the configured output set, so replacing one mod's pipe with another does not disturb reservations.

## Mod compatibility

The splitter must use only NeoForge's sided `Capabilities.Item.BLOCK`, `ResourceHandler<ItemResource>`, and transaction APIs. It must not inspect Oritech pipe blocks, pipe network data, extraction settings, or implementation classes, and it must not require an Oritech-specific callback.

Each face receives its own stable handler wrapper so the splitter knows which face is requesting extraction. The wrapper delegates insertion and inventory queries to the single backing stack, while its extraction methods apply the selected distribution policy. This works with Oritech pipes, other mods' pipes, hoppers, or automation adapters that use the same NeoForge capability contract.

The handlers must support callers that simulate by opening and rolling back a transaction, callers that use nested transactions, and callers that extract less than requested. Reservations, activity timestamps, and the Round Robin cursor therefore change through transaction-aware journals alongside the backing inventory. No state may advance merely because a capability was queried or an extraction was attempted.

Immediate knowledge of a blocked downstream inventory is not part of the item capability contract. Overflow and Round Robin skipping therefore use the absence of committed extraction over an initial 20-tick grace period. This is transport-agnostic and behaves consistently for every compatible mod.
