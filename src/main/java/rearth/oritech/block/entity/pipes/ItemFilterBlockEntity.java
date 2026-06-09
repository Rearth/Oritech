package rearth.oritech.block.entity.pipes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.api.networking.NetworkedBlockEntity;
import rearth.oritech.api.networking.SyncField;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.api.transfer.item.DelegatingInventoryStorage;
import rearth.oritech.api.transfer.item.ItemProvider;
import rearth.oritech.client.ui.ItemFilterScreenHandler;
import rearth.oritech.init.BlockEntitiesContent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class ItemFilterBlockEntity extends NetworkedBlockEntity implements ItemProvider, MenuProvider {

    public final DelegatingFilterInventory inventory = new DelegatingFilterInventory(this::getBackingStorage, null);
    private BlockCapabilityCache<ResourceHandler<ItemResource>, Direction> lookupCache;

    @SyncField(SyncType.GUI_OPEN)
    protected FilterData filterSettings = new FilterData(false, true, false, new HashMap<>());

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("whitelist", filterSettings.useWhitelist);
        output.putBoolean("useNbt", filterSettings.useNbt);
        output.putBoolean("useComponents", filterSettings.useComponents);

        var filterItems = output.childrenList("filterItems");
        filterSettings.items.forEach((slot, stack) -> {
            var entry = filterItems.addChild();
            entry.putInt("slot", slot);
            entry.store("stack", ItemStack.OPTIONAL_CODEC, stack);
        });
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        var whiteList = input.getBooleanOr("whitelist", true);
        var useNbt = input.getBooleanOr("useNbt", false);
        var useComponents = input.getBooleanOr("useComponents", false);

        var itemsList = new HashMap<Integer, ItemStack>();
        for (var entry : input.childrenListOrEmpty("filterItems")) {
            var slot = entry.getIntOr("slot", itemsList.size());
            var stack = entry.read("stack", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
            itemsList.put(slot, stack);
        }

        var data = new FilterData(useNbt, whiteList, useComponents, itemsList);
        this.setFilterSettings(data);

    }

    public ItemFilterBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.ITEM_FILTER_ENTITY.get(), pos, state);
    }

    @Override
    public ResourceHandler<ItemResource> getItemLookup(@Nullable Direction direction) {
        return inventory;
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        sendUpdate(SyncType.GUI_OPEN);
    }

    @Override
    public Component getDisplayName() {
        return Component.nullToEmpty("");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new ItemFilterScreenHandler(syncId, playerInventory, this);
    }

    private @Nullable ResourceHandler<ItemResource> getBackingStorage() {

        if (lookupCache == null) {
            var targetDirection = getBlockState().getValue(BlockStateProperties.FACING);
            var targetPos = worldPosition.offset(targetDirection.getUnitVec3i());
            lookupCache = BlockCapabilityCache.create(Capabilities.Item.BLOCK, (ServerLevel) level, targetPos, targetDirection.getOpposite());
        }

        return lookupCache.getCapability();

    }

    @Override
    public void serverTick(ServerLevel serverLevel, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {
        // inventory is delegating now, so we don't really need this anymore
    }

    public FilterData getFilterSettings() {
        return filterSettings;
    }

    public void setFilterSettings(FilterData filterSettings) {
        this.filterSettings = filterSettings;
        this.setChanged();
    }

    @Override
    public void setChanged() {
        if (this.level != null)
            level.blockEntityChanged(worldPosition);
    }

    public static void handleClientUpdate(ItemFilterPayload message, IPayloadContext context) {
        var blockEntity = context.player().level().getBlockEntity(message.pos(), BlockEntitiesContent.ITEM_FILTER_ENTITY.get());
        if (blockEntity.isPresent()) {
            blockEntity.get().setFilterSettings(message.data);
        }

    }

    // items is a map of position index (in the filter GUI) to filtered item stack
    public record FilterData(boolean useNbt, boolean useWhitelist, boolean useComponents,
                             Map<Integer, ItemStack> items) {

        public static StreamCodec<RegistryFriendlyByteBuf, FilterData> PACKET_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, FilterData::useNbt,
                ByteBufCodecs.BOOL, FilterData::useWhitelist,
                ByteBufCodecs.BOOL, FilterData::useComponents,
                ByteBufCodecs.map(HashMap::new, ByteBufCodecs.INT, ItemStack.STREAM_CODEC), FilterData::items,
                FilterData::new
        );

    }

    // used to send data to server
    public record ItemFilterPayload(BlockPos pos, FilterData data) implements CustomPacketPayload {
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return FILTER_PACKET_ID;
        }

        public static final Type<ItemFilterPayload> FILTER_PACKET_ID = new Type<>(Oritech.id("filter"));

        public static final StreamCodec<RegistryFriendlyByteBuf, ItemFilterPayload> PACKET_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, ItemFilterPayload::pos,
                FilterData.PACKET_CODEC, ItemFilterPayload::data,
                ItemFilterPayload::new
        );
    }

    public class DelegatingFilterInventory extends DelegatingInventoryStorage {

        public DelegatingFilterInventory(Supplier<ResourceHandler<ItemResource>> backingStorage, @Nullable BooleanSupplier validPredicate) {
            super(backingStorage, validPredicate);
        }

        @Override
        public int insert(ItemResource resource, int amount, TransactionContext transaction) {
            if (!canInsert(resource, amount)) return 0;
            return super.insert(resource, amount, transaction);
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            if (!canInsert(resource, amount)) return 0;
            return super.insert(index, resource, amount, transaction);
        }

        public boolean canInsert(ItemResource resource, int amount) {

            var stack = resource.toStack(amount);

            // check filter settings
            var checkNbt = filterSettings.useNbt;
            var checkComponents = filterSettings.useComponents;
            var matchesFilterItems = false; // true if at least 1 item matches

            for (var filterItem : filterSettings.items.values()) {

                // todo
//                if (ModList.get().isLoaded("ftbfiltersystem")) {
//                    var filterApi = dev.ftb.mods.ftbfiltersystem.api.FTBFilterSystemAPI.api();
//                    if (filterApi.isFilterItem(filterItem)) {
//                        if (filterApi.doesFilterMatch(filterItem, stack, getLevel().registryAccess())) {
//                            matchesFilterItems = true;
//                            break;
//                        }
//                    }
//                }

                var matchesType = stack.getItem().equals(filterItem.getItem());
                if (!matchesType) continue;

                if (checkComponents) {
                    var componentsMatch = stack.getComponentsPatch().equals(filterItem.getComponentsPatch());
                    if (!componentsMatch) {
                        break;
                    }
                }

                if (checkNbt) {
                    // check if both have nbt, if so compare them
                    // if not both check if neither has nbt, and type matches
                    if (stack.has(DataComponents.CUSTOM_DATA) && filterItem.has(DataComponents.CUSTOM_DATA)) {
                        var match = stack.get(DataComponents.CUSTOM_DATA).equals(filterItem.get(DataComponents.CUSTOM_DATA));
                        if (match) {
                            matchesFilterItems = true;
                            break;
                        }
                    } else if (!stack.has(DataComponents.CUSTOM_DATA) && !filterItem.has(DataComponents.CUSTOM_DATA)) {
                        matchesFilterItems = true;
                        break;
                    }
                } else {
                    matchesFilterItems = true;
                    break;
                }

            }

            // matchesFilterItems is true when at least 1 item matches
            if (filterSettings.useWhitelist) {
                return matchesFilterItems;
            } else {
                // blacklist list, if we have a match we return false
                return !matchesFilterItems;
            }
        }
    }
}
