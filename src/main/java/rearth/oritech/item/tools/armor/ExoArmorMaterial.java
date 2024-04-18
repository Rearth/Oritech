package rearth.oritech.item.tools.armor;

import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

public class ExoArmorMaterial implements ArmorMaterial {
    
    // order is boots, legs, chestplate, helmet
    private static final int[] BASE_DURABILITY = new int[] {13, 15, 16, 11};
    private static final int[] PROTECTION_VALUES = new int[] {2, 5, 6, 2};  // same values as iron
    
    @Override
    public int getDurability(ArmorItem.Type type) {
        return BASE_DURABILITY[type.getEquipmentSlot().getEntitySlotId()];
    }
    
    @Override
    public int getProtection(ArmorItem.Type type) {
        return PROTECTION_VALUES[type.getEquipmentSlot().getEntitySlotId()];
    }
    
    @Override
    public int getEnchantability() {
        return 15;
    }
    
    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ITEM_ARMOR_EQUIP_DIAMOND;
    }
    
    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.ofItems(Items.GOLD_INGOT);
    }
    
    @Override
    public String getName() {
        return "exosuit";
    }
    
    @Override
    public float getToughness() {
        return 2;
    }
    
    @Override
    public float getKnockbackResistance() {
        return 0.2f;
    }
}
