package rearth.oritech.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;
import rearth.oritech.Oritech;
import rearth.oritech.util.PortalEntity;

import java.util.function.Supplier;

public class EntitiesContent {

    public static final DeferredRegister.Entities ENTITY_TYPES = DeferredRegister.createEntities(Oritech.MOD_ID);
    
    public static final Supplier<EntityType<?>> PORTAL_ENTITY = ENTITY_TYPES.register("portal_entity", () -> EntityType.Builder.of(PortalEntity::new, MobCategory.MISC)
                                                                                                            .sized(1, 2)
                                                                                                               .noSave()
                                                                                                               .noSummon()
                                                                                                            .build(ResourceKey.create(Registries.ENTITY_TYPE, Oritech.id("portal_entity"))));

}
