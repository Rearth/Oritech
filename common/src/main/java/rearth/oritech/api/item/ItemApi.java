package rearth.oritech.api.item;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import rearth.oritech.api.fluid.ItemFluidApi;

import java.util.ServiceLoader;

public class ItemApi {
    
    public static BlockItemApi BLOCK = ServiceLoader.load(BlockItemApi.class)
                                         .findFirst()
                                         .orElseThrow(() -> new IllegalStateException("Failed to load block item api service."));
    
    public interface BlockProvider {
        InventoryStorage getInventoryStorage(Direction direction);
    }
    
    public interface InventoryStorage {
        
        default boolean supportsInsertion() {
            return true;
        }
        
        /**
         * Inserts the given ItemStack into the inventory.
         *
         * @param inserted the ItemStack to insert
         * @param simulate if true, the insertion is only simulated
         * @return the amount of items successfully inserted
         */
        int insert(ItemStack inserted, boolean simulate);
        
        /**
         * Inserts the given ItemStack into a specific slot in the inventory.
         *
         * @param inserted the ItemStack to insert
         * @param slot     the slot to insert into
         * @param simulate if true, the insertion is only simulated
         * @return the amount of items successfully inserted
         */
        int insertToSlot(ItemStack inserted, int slot, boolean simulate);
        
        /**
         * Checks if the inventory supports extraction.
         *
         * @return true if extraction is supported, false otherwise
         */
        default boolean supportsExtraction() {
            return true;
        }
        
        /**
         * Extracts the given ItemStack from the inventory.
         *
         * @param extracted the ItemStack to extract
         * @param simulate  if true, the extraction is only simulated
         * @return the amount of items successfully extracted
         */
        int extract(ItemStack extracted, boolean simulate);
        
        /**
         * Extracts the given ItemStack from a specific slot in the inventory.
         *
         * @param extracted the ItemStack to extract
         * @param slot      the slot to extract from
         * @param simulate  if true, the extraction is only simulated
         * @return the amount of items successfully extracted
         */
        int extractFromSlot(ItemStack extracted, int slot, boolean simulate);
        
        void setStackInSlot(int slot, ItemStack stack);
        
        ItemStack getStackInSlot(int slot);
        
        int getSlotCount();
        
        int getSlotLimit(int slot);
        
        void update();
        
    }
    
    
}
