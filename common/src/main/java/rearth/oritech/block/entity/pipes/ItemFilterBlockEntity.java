package rearth.oritech.block.entity.pipes;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import io.wispforest.endec.Endec;
import io.wispforest.owo.serialization.endec.MinecraftEndecs;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.api.item.ItemApi;
import rearth.oritech.api.item.containers.SimpleInventoryStorage;
import rearth.oritech.block.blocks.pipes.item.ItemFilterBlock;
import rearth.oritech.client.ui.ItemFilterScreenHandler;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.network.NetworkContent;

import java.util.HashMap;
import java.util.Map;

public class ItemFilterBlockEntity extends BlockEntity implements ItemApi.BlockProvider, ExtendedMenuProvider, BlockEntityTicker<ItemFilterBlockEntity> {
    
    public final FilterBlockInventory inventory = new FilterBlockInventory(1, this::markDirty);
    
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
        NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new NetworkContent.ItemFilterSyncPacket(pos, filterSettings));
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
    public void tick(World world, BlockPos pos, BlockState state, ItemFilterBlockEntity blockEntity) {
        
        // if non-empty and inventory in target, move it
        if (world.isClient || inventory.isEmpty()) return;
        
        var targetDirection = getCachedState().get(ItemFilterBlock.TARGET_DIR);
        var targetPos = pos.add(targetDirection.getVector());
        
        // todo caching
        var targetInv = ItemApi.BLOCK.find(world, targetPos, targetDirection);
        if (targetInv == null) return;
        
        var firstItem = inventory.heldStacks.getFirst();
        var inserted = targetInv.insert(firstItem, false);
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
    
    // items is a map of position index (in the filter GUI) to filtered item stack
    public record FilterData(boolean useNbt, boolean useWhitelist, boolean useComponents,
                             Map<Integer, ItemStack> items) {
    }
    
    public class FilterBlockInventory extends SimpleInventoryStorage {
        
        public FilterBlockInventory(int size, Runnable onUpdate) {
            super(size, onUpdate);
        }
        
        @Override
        public boolean supportsExtraction() {
            return false;
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
    
    public static Endec<Map<Integer, ItemStack>> FILTER_ITEMS_ENDEC = Endec.map(Object::toString, Integer::valueOf, MinecraftEndecs.ITEM_STACK);
}
