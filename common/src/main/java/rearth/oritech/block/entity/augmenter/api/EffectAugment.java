package rearth.oritech.block.entity.augmenter.api;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;

public class EffectAugment extends Augment {
    
    public final Holder<MobEffect> effectType;
    public final int amplifier;
    
    public EffectAugment(ResourceLocation id, boolean toggleable, Holder<MobEffect> effectType, int amplifier) {
        super(id, toggleable);
        this.effectType = effectType;
        this.amplifier = amplifier;
    }
    
    @Override
    public void activate(Player player) {
        player.addEffect(new MobEffectInstance(effectType, -1, amplifier, true, false, false));
    }
    
    @Override
    public void deactivate(Player player) {
        player.removeEffect(effectType);
    }
    
    @Override
    public void refreshServer(Player player) {
        player.addEffect(new MobEffectInstance(effectType, -1, amplifier, true, false, false));
    }
    
    @Override
    public int refreshInterval() {
        return 80;  // called every few seconds because potions / other items / mods might be changing the same effect.
    }
}
