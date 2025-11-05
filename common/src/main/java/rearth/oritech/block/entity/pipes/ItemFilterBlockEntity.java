package rearth.oritech.block.entity.pipes;

import dev.architectury.platform.Platform;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.api.item.ItemApi;
import rearth.oritech.api.item.containers.SimpleInventoryStorage;
import rearth.oritech.api.networking.NetworkedBlockEntity;
import rearth.oritech.api.networking.SyncField;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.block.blocks.pipes.item.ItemFilterBlock;
import rearth.oritech.client.ui.ItemFilterScreenHandler;
import rearth.oritech.init.BlockEntitiesContent;

import java.util.HashMap;
import java.util.Map;

public class ItemFilterBlockEntity extends NetworkedBlockEntity implements ItemApi.BlockProvider, ExtendedMenuProvider {
    
    public final FilterBlockInventory inventory = new FilterBlockInventory(1, this::setChanged);
    
    @SyncField(SyncType.GUI_OPEN)
    protected FilterData filterSettings = new FilterData(false, true, false, new HashMap<>());
    
    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.saveAdditional(nbt, registryLookup);
        ContainerHelper.saveAllItems(nbt, inventory.heldStacks, false, registryLookup);
        nbt.putBoolean("whitelist", filterSettings.useWhitelist);
        nbt.putBoolean("useNbt", filterSettings.useNbt);
        nbt.putBoolean("useComponents", filterSettings.useComponents);
        
        var filterItems = filterSettings.items.values();
        var itemsNbtList = new ListTag();
        
        for (var item : filterItems) {
            var data = item.save(registryLookup);
            itemsNbtList.add(data);
        }
        
        nbt.put("filterItems", itemsNbtList);
    }
    
    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.loadAdditional(nbt, registryLookup);
        ContainerHelper.loadAllItems(nbt, inventory.heldStacks, registryLookup);
        
        var whiteList = nbt.getBoolean("whitelist");
        var useNbt = nbt.getBoolean("useNbt");
        var useComponents = nbt.getBoolean("useComponents");
        
        var list = nbt.getList("filterItems", Tag.TAG_COMPOUND);
        var itemsList = new HashMap<Integer, ItemStack>();
        for (int i = 0; i < list.size(); i++) {
            var data = list.get(i);
            var stack = ItemStack.parse(registryLookup, data).orElse(ItemStack.EMPTY);
            
            itemsList.put(i, stack);
        }
        
        var data = new FilterData(useNbt, whiteList, useComponents, itemsList);
        this.setFilterSettings(data);
        
    }
    
    public ItemFilterBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.ITEM_FILTER_ENTITY, pos, state);
    }
    
    @Override
    public ItemApi.InventoryStorage getInventoryStorage(Direction direction) {
        return inventory;
    }
    
    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        sendUpdate(SyncType.GUI_OPEN);
        buf.writeBlockPos(worldPosition);
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
    
    @Override
    public void serverTick(Level world, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {
        
        // if non-empty and inventory in target, move it
        if (inventory.isEmpty()) return;
        
        var targetDirection = getBlockState().getValue(ItemFilterBlock.TARGET_DIR);
        var targetPos = pos.offset(targetDirection.getNormal());
        
        // todo caching
        var targetInv = ItemApi.BLOCK.find(world, targetPos, targetDirection);
        if (targetInv == null) return;
        
        var firstItem = inventory.heldStacks.getFirst();
        var inserted = targetInv.insert(firstItem.copy(), false);
        firstItem.shrink(inserted);
        
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
    
    public static void handleClientUpdate(ItemFilterPayload message, Player player, RegistryAccess registryAccess) {
        var blockEntity = player.level().getBlockEntity(message.pos(), BlockEntitiesContent.ITEM_FILTER_ENTITY);
        if (blockEntity.isPresent()) {
            blockEntity.get().setFilterSettings(message.data);
        }
    
    }
    
    // items is a map of position index (in the filter GUI) to filtered item stack
    public record FilterData(boolean useNbt, boolean useWhitelist, boolean useComponents, Map<Integer, ItemStack> items) {
        
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
        
        public static final CustomPacketPayload.Type<ItemFilterPayload> FILTER_PACKET_ID = new CustomPacketPayload.Type<>(Oritech.id("filter"));
        
        public static final StreamCodec<RegistryFriendlyByteBuf, ItemFilterPayload> PACKET_CODEC = StreamCodec.composite(
          BlockPos.STREAM_CODEC, ItemFilterPayload::pos,
          FilterData.PACKET_CODEC, ItemFilterPayload::data,
          ItemFilterPayload::new
        );
    }
    
    public class FilterBlockInventory extends SimpleInventoryStorage {
        
        public FilterBlockInventory(int size, Runnable onUpdate) {
            super(size, onUpdate);
        }
        
        public boolean canInsert(ItemStack stack) {
            
            // check filter settings
            var checkNbt = filterSettings.useNbt;
            var checkComponents = filterSettings.useComponents;
            var matchesFilterItems = false; // true if at least 1 item matches
            
            for (var filterItem : filterSettings.items.values()) {
                
                if (Platform.isModLoaded("ftbfiltersystem")) {
                    var filterApi = dev.ftb.mods.ftbfiltersystem.api.FTBFilterSystemAPI.api();
                    if (filterApi.isFilterItem(filterItem)) {
                        if (filterApi.doesFilterMatch(filterItem, stack)) {
                            matchesFilterItems = true;
                            break;
                        }
                    }
                }
                
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
        
        @Override
        public int insertToSlot(ItemStack addedStack, int slot, boolean simulate) {
            
            if (!canInsert(addedStack))
                return 0;
            
            return super.insertToSlot(addedStack, slot, simulate);
        }
    }
}
