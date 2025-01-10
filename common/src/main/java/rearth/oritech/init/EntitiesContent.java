package rearth.oritech.init;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import rearth.oritech.util.ArchitecturyRegistryContainer;
import rearth.oritech.util.PortalEntity;

public class EntitiesContent implements ArchitecturyRegistryContainer<EntityType<?>> {
    
    public static final EntityType<PortalEntity> PORTAL_ENTITY = EntityType.Builder.create(PortalEntity::new, SpawnGroup.MISC)
                                                                   .dimensions(1, 2)
                                                                   .build();
    
    @Override
    public RegistryKey<Registry<EntityType<?>>> getRegistryType() {
        return RegistryKeys.ENTITY_TYPE;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public Class<EntityType<?>> getTargetFieldType() {
        return (Class<EntityType<?>>) (Object) EntityType.class;
    }
}
