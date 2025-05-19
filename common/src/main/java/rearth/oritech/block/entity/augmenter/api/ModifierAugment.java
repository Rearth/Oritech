package rearth.oritech.block.entity.augmenter.api;

import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

public class ModifierAugment extends Augment {
    
    private final RegistryEntry<EntityAttribute> targetAttribute;
    private final float amount;
    private final EntityAttributeModifier.Operation operation;
    
    public ModifierAugment(Identifier id, RegistryEntry<EntityAttribute> targetAttribute, EntityAttributeModifier.Operation operation, float amount, boolean toggleable) {
        super(id, toggleable);
        this.targetAttribute = targetAttribute;
        this.amount = amount;
        this.operation = operation;
    }
    
    
    @Override
    public void activate(PlayerEntity player) {
        var instance = player.getAttributeInstance(targetAttribute);
        if (instance == null) return;
        instance.overwritePersistentModifier(new EntityAttributeModifier(id, amount, operation));
    }
    
    @Override
    public void deactivate(PlayerEntity player) {
        var instance = player.getAttributeInstance(targetAttribute);
        if (instance == null) return;
        instance.removeModifier(id);
    }
    
    @Override
    public void refreshServer(PlayerEntity player) {
        var instance = player.getAttributeInstance(targetAttribute);
        if (instance == null) return;
        instance.overwritePersistentModifier(new EntityAttributeModifier(id, amount, operation));
    }
    
    @Override
    public int refreshInterval() {
        return 6000; // doesn't need to happen often, as these effects are applied permanently
    }
}
