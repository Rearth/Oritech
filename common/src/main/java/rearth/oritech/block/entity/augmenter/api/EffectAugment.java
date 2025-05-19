package rearth.oritech.block.entity.augmenter.api;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

public class EffectAugment extends Augment {
    
    public final RegistryEntry<StatusEffect> effectType;
    public final int amplifier;
    
    public EffectAugment(Identifier id, boolean toggleable, RegistryEntry<StatusEffect> effectType, int amplifier) {
        super(id, toggleable);
        this.effectType = effectType;
        this.amplifier = amplifier;
    }
    
    @Override
    public void activate(PlayerEntity player) {
        player.addStatusEffect(new StatusEffectInstance(effectType, -1, amplifier, true, false, false));
    }
    
    @Override
    public void deactivate(PlayerEntity player) {
        player.removeStatusEffect(effectType);
    }
    
    @Override
    public void refreshServer(PlayerEntity player) {
        player.addStatusEffect(new StatusEffectInstance(effectType, -1, amplifier, true, false, false));
    }
    
    @Override
    public int refreshInterval() {
        return 80;  // called every few seconds because potions / other items / mods might be changing the same effect.
    }
}
