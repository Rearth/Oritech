package rearth.oritech.block.entity.pipes;

import io.wispforest.endec.Endec;
import io.wispforest.owo.serialization.endec.MinecraftEndecs;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.blocks.pipes.ItemFilterBlock;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.ui.ItemFilterScreenHandler;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.network.NetworkContent;
import rearth.oritech.util.InventoryProvider;

import java.util.HashMap;
import java.util.Map;

public class ItemFilterBlockEntity extends BlockEntity implements InventoryProvider, ExtendedScreenHandlerFactory, BlockEntityTicker<ItemFilterBlockEntity> {
    
    protected final FilterBlockInventory inventory = new FilterBlockInventory(1);
    
    protected FilterData filterSettings = new FilterData(false, true, new HashMap<>());
    protected BlockApiCache<Storage<ItemVariant>, Direction> lookupCache;
    
    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        Inventories.writeNbt(nbt, inventory.heldStacks, false, registryLookup);
        nbt.putBoolean("whitelist", filterSettings.useWhitelist);
        nbt.putBoolean("useNbt", filterSettings.useNbt);
        
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
        
        var list = nbt.getList("filterItems", NbtElement.COMPOUND_TYPE);
        var itemsList = new HashMap<Integer, ItemStack>();
        for (int i = 0; i < list.size(); i++) {
            var data = list.get(i);
            var stack = ItemStack.fromNbt(registryLookup, data).orElse(ItemStack.EMPTY);
            
            itemsList.put(i, stack);
        }
        
        var data = new FilterData(useNbt, whiteList, itemsList);
        this.setFilterSettings(data);
        
    }
    
    public ItemFilterBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.ITEM_FILTER_ENTITY, pos, state);
    }
    
    @Override
    public InventoryStorage getInventory(Direction direction) {
        return InventoryStorage.of(inventory, direction);
    }
    @Override
    public Object getScreenOpeningData(ServerPlayerEntity player) {
        NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new NetworkContent.ItemFilterSyncPacket(pos, filterSettings));
        return new ModScreens.BasicData(pos);
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
    public void tick(World world, BlockPos pos, BlockState state, ItemFilterBlockEntity blockEntity) {
        
        // if non-empty and inventory in target, move it
        if (world.isClient || inventory.isEmpty()) return;
        
        var targetInv = getCache().find(state.get(ItemFilterBlock.TARGET_DIR).getOpposite());
        if (targetInv == null) return;
        
        try (var tx = Transaction.openOuter()) {
            var firstItem = inventory.heldStacks.get(0);
            var inserted = targetInv.insert(ItemVariant.of(firstItem), firstItem.getCount(), tx);
            firstItem.decrement((int) inserted);
            tx.commit();
        }
        
    }
    
    private BlockApiCache<Storage<ItemVariant>, Direction> getCache() {
        if (lookupCache == null) {
            var targetDirection = getCachedState().get(ItemFilterBlock.TARGET_DIR);
            var targetPos = pos.add(targetDirection.getVector());
            lookupCache = BlockApiCache.create(ItemStorage.SIDED, (ServerWorld) world, targetPos);
        }
        
        return lookupCache;
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
    
    // items is a map of position index (in the filter GUI) to filtered item stack
    public record FilterData(boolean useNbt, boolean useWhitelist, Map<Integer, ItemStack> items) {
    }
    
    protected class FilterBlockInventory extends SimpleInventory implements SidedInventory {
        
        public FilterBlockInventory(int size) {
            super(size);
        }
        
        @Override
        public void markDirty() {
            ItemFilterBlockEntity.this.markDirty();
        }
        
        @Override
        public int[] getAvailableSlots(Direction side) {
            return new int[]{0};
        }
        
        @Override
        public boolean canInsert(int slot, ItemStack stack, @Nullable Direction side) {
            
            // check sides first
            var outputSide = getCachedState().get(ItemFilterBlock.TARGET_DIR);
            if (side.equals(outputSide)) return false;
            
            // then check filter settings
            // todo add option to compare components and/or nbt (possibly third button?)
            var checkNbt = filterSettings.useNbt;
            var matchesFilterItems = false; // check if at least 1 item matches
            
            for (var filterItem : filterSettings.items.values()) {
                var matchesType = stack.getItem().equals(filterItem.getItem());
                if (!matchesType) continue;
                
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
        public boolean canExtract(int slot, ItemStack stack, Direction side) {
            
            var outputSide = getCachedState().get(ItemFilterBlock.TARGET_DIR);
            return side.equals(outputSide);
        }
    }
    
    public static Endec<Map<Integer, ItemStack>> FILTER_ITEMS_ENDEC = Endec.map(Object::toString, Integer::valueOf, MinecraftEndecs.ITEM_STACK);
}
