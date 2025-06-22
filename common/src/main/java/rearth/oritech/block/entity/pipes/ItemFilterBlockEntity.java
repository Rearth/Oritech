package rearth.oritech.block.entity.pipes;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
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
    
    public final FilterBlockInventory inventory = new FilterBlockInventory(1, this::markDirty);
    
    @SyncField(SyncType.GUI_OPEN)
    protected FilterData filterSettings = new FilterData(false, true, false, new HashMap<>());
    
    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        Inventories.writeNbt(nbt, inventory.heldStacks, false, registryLookup);
        nbt.putBoolean("whitelist", filterSettings.useWhitelist);
        nbt.putBoolean("useNbt", filterSettings.useNbt);
        nbt.putBoolean("useComponents", filterSettings.useComponents);
        
        var filterItems = filterSettings.items.values();
        var itemsNbtList = new NbtList();
        
        for (var item : filterItems) {
            var data = item.encode(registryLookup);
            itemsNbtList.add(data);
        }
        
        nbt.put("filterItems", itemsNbtList);
    }
    
    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        Inventories.readNbt(nbt, inventory.heldStacks, registryLookup);
        
        var whiteList = nbt.getBoolean("whitelist");
        var useNbt = nbt.getBoolean("useNbt");
        var useComponents = nbt.getBoolean("useComponents");
        
        var list = nbt.getList("filterItems", NbtElement.COMPOUND_TYPE);
        var itemsList = new HashMap<Integer, ItemStack>();
        for (int i = 0; i < list.size(); i++) {
            var data = list.get(i);
            var stack = ItemStack.fromNbt(registryLookup, data).orElse(ItemStack.EMPTY);
            
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
    public void saveExtraData(PacketByteBuf buf) {
        sendUpdate(SyncType.GUI_OPEN);
        buf.writeBlockPos(pos);
    }
    
    @Override
    public Text getDisplayName() {
        return Text.of("");
    }
    
    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new ItemFilterScreenHandler(syncId, playerInventory, this);
    }
    
    @Override
    public void serverTick(World world, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {
        
        // if non-empty and inventory in target, move it
        if (inventory.isEmpty()) return;
        
        var targetDirection = getCachedState().get(ItemFilterBlock.TARGET_DIR);
        var targetPos = pos.add(targetDirection.getVector());
        
        // todo caching
        var targetInv = ItemApi.BLOCK.find(world, targetPos, targetDirection);
        if (targetInv == null) return;
        
        var firstItem = inventory.heldStacks.getFirst();
        var inserted = targetInv.insert(firstItem.copy(), false);
        firstItem.decrement(inserted);
        
    }
    
    public FilterData getFilterSettings() {
        return filterSettings;
    }
    
    public void setFilterSettings(FilterData filterSettings) {
        this.filterSettings = filterSettings;
        this.markDirty();
    }
    
    @Override
    public void markDirty() {
        if (this.world != null)
            world.markDirty(pos);
    }
    
    public static void handleClientUpdate(ItemFilterPayload message, World world, DynamicRegistryManager registryAccess) {
        var blockEntity = world.getBlockEntity(message.pos(), BlockEntitiesContent.ITEM_FILTER_ENTITY);
        if (blockEntity.isPresent()) {
            blockEntity.get().setFilterSettings(message.data);
        }
    
    }
    
    // items is a map of position index (in the filter GUI) to filtered item stack
    public record FilterData(boolean useNbt, boolean useWhitelist, boolean useComponents, Map<Integer, ItemStack> items) {
        
        public static PacketCodec<RegistryByteBuf, FilterData> PACKET_CODEC = PacketCodec.tuple(
          PacketCodecs.BOOL, FilterData::useNbt,
          PacketCodecs.BOOL, FilterData::useWhitelist,
          PacketCodecs.BOOL, FilterData::useComponents,
          PacketCodecs.map(HashMap::new, PacketCodecs.INTEGER, ItemStack.PACKET_CODEC), FilterData::items,
          FilterData::new
        );
        
    }
    
    // used to send data to server
    public record ItemFilterPayload(BlockPos pos, FilterData data) implements CustomPayload {
        @Override
        public Id<? extends CustomPayload> getId() {
            return FILTER_PACKET_ID;
        }
        
        public static final CustomPayload.Id<ItemFilterPayload> FILTER_PACKET_ID = new CustomPayload.Id<>(Oritech.id("filter"));
        
        public static final PacketCodec<RegistryByteBuf, ItemFilterPayload> PACKET_CODEC = PacketCodec.tuple(
          BlockPos.PACKET_CODEC, ItemFilterPayload::pos,
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
                var matchesType = stack.getItem().equals(filterItem.getItem());
                if (!matchesType) continue;
                
                if (checkComponents) {
                    var componentsMatch = stack.getComponentChanges().equals(filterItem.getComponentChanges());
                    if (!componentsMatch) {
                        break;
                    }
                }
                
                if (checkNbt) {
                    // check if both have nbt, if so compare them
                    // if not both check if neither has nbt, and type matches
                    if (stack.contains(DataComponentTypes.CUSTOM_DATA) && filterItem.contains(DataComponentTypes.CUSTOM_DATA)) {
                        var match = stack.get(DataComponentTypes.CUSTOM_DATA).equals(filterItem.get(DataComponentTypes.CUSTOM_DATA));
                        if (match) {
                            matchesFilterItems = true;
                            break;
                        }
                    } else if (!stack.contains(DataComponentTypes.CUSTOM_DATA) && !filterItem.contains(DataComponentTypes.CUSTOM_DATA)) {
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
