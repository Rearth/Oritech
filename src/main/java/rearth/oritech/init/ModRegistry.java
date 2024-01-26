package rearth.oritech.init;

import rearth.oritech.block.entity.PulverizerBlockEntity;
import team.reborn.energy.api.EnergyStorage;

public class ModRegistry {

    public static void register() {

        EnergyStorage.SIDED.registerForBlockEntity((blockEntity, direction) -> blockEntity.getStorage(), BlockEntitiesContent.PULVERIZER_ENTITY);

    }

}
