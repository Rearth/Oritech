package rearth.oritech.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import rearth.oritech.item.tools.harvesting.ChainsawItem;
import rearth.oritech.item.tools.harvesting.PromethiumAxeItem;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {

    // Treat chainsaw/prometheum axe like sword when attacking
    @ModifyVariable(method = "attack(Lnet/minecraft/entity/Entity;)V", at = @At(value = "STORE"), ordinal = 1)
    private ItemStack getHandStack(ItemStack handStack, @Local(ordinal = 3) LocalBooleanRef isSword) {
        // also set "bl4" variable when the "itemStack2" variable is set
        isSword.set(isSword.get() || handStack.getItem() instanceof ChainsawItem || handStack.getItem() instanceof PromethiumAxeItem);
        return handStack;
    }
}
