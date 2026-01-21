package rearth.oritech.init;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import rearth.oritech.util.PortalEntity;
import rearth.oritech.util.registry.ArchitecturyRegistryContainer;

public class EntitiesContent implements ArchitecturyRegistryContainer<EntityType<?>> {
    
    public static final EntityType<PortalEntity> PORTAL_ENTITY = EntityType.Builder.of(PortalEntity::new, MobCategory.MISC)
                                                                   .sized(1, 2)
                                                                   .build("portal_entity");
    
    @Override
    public ResourceKey<Registry<EntityType<?>>> getRegistryType() {
        return Registries.ENTITY_TYPE;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public Class<EntityType<?>> getTargetFieldType() {
        return (Class<EntityType<?>>) (Object) EntityType.class;
    }
}
