package rearth.oritech.block.entity.augmenter.api;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;

public class ModifierAugment extends Augment {
    
    private final Holder<Attribute> targetAttribute;
    private final float amount;
    private final AttributeModifier.Operation operation;
    
    public ModifierAugment(ResourceLocation id, Holder<Attribute> targetAttribute, AttributeModifier.Operation operation, float amount, boolean toggleable) {
        super(id, toggleable);
        this.targetAttribute = targetAttribute;
        this.amount = amount;
        this.operation = operation;
    }
    
    
    @Override
    public void activate(Player player) {
        var instance = player.getAttribute(targetAttribute);
        if (instance == null) return;
        instance.addOrReplacePermanentModifier(new AttributeModifier(id, amount, operation));
    }
    
    @Override
    public void deactivate(Player player) {
        var instance = player.getAttribute(targetAttribute);
        if (instance == null) return;
        instance.removeModifier(id);
    }
    
    @Override
    public void refreshServer(Player player) {
        var instance = player.getAttribute(targetAttribute);
        if (instance == null) return;
        instance.addOrReplacePermanentModifier(new AttributeModifier(id, amount, operation));
    }
    
    @Override
    public int refreshInterval() {
        return 6000; // doesn't need to happen often, as these effects are applied permanently
    }
}
