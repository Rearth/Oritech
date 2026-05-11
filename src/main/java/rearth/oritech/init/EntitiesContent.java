package rearth.oritech.init;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import rearth.oritech.util.PortalEntity;
import rearth.oritech.util.registry.OritechDeferredRegistry;

public class EntitiesContent {

    public static final OritechDeferredRegistry<EntityType<?>> ENTITIES = OritechDeferredRegistry.create(Registries.ENTITY_TYPE);
    
    public static final RegistrySupplier<EntityType<?>> PORTAL_ENTITY = ENTITIES.register("portal_entity", () -> EntityType.Builder.of(PortalEntity::new, MobCategory.MISC)
                                                                                                            .sized(1, 2)
                                                                                                            .build("portal_entity"));

    public static void register() {
        ENTITIES.register();
    }
}
