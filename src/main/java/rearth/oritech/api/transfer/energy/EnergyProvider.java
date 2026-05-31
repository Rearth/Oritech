package rearth.oritech.api.transfer.energy;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import org.jetbrains.annotations.Nullable;

public interface EnergyProvider {

    EnergyHandler getEnergyLookup(@Nullable Direction direction);

}
