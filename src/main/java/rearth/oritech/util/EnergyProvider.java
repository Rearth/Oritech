package rearth.oritech.util;

import net.minecraft.util.math.Direction;
import team.reborn.energy.api.EnergyStorage;

public interface EnergyProvider {

    EnergyStorage getStorage(Direction direction);

}
