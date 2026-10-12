package rearth.oritech.init.compat.jade;

import net.minecraft.resources.Identifier;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.MachineCoreEntity;
import snownee.jade.api.Accessor;
import snownee.jade.api.view.ClientViewGroup;
import snownee.jade.api.view.EnergyView;
import snownee.jade.api.view.IClientExtensionProvider;
import snownee.jade.api.view.IServerExtensionProvider;
import snownee.jade.api.view.ViewGroup;

import java.util.List;

public enum OritechCoreEnergyProvider implements IServerExtensionProvider<EnergyView.Data> {
    INSTANCE;

    private static final Identifier ID = Oritech.id("machine_core_energy");

    @Override
    public boolean shouldRequestData(Accessor<?> accessor) {
        return OritechJadePlugin.isConnectedCore(accessor);
    }

    @Override
    public List<ViewGroup<EnergyView.Data>> getGroups(Accessor<?> accessor) {
        if (!(accessor.getTarget() instanceof MachineCoreEntity core) || !core.isEnabled()) return List.of();
        var storage = core.getEnergyLookup(null);
        if (storage == null || storage.getCapacityAsLong() <= 0) return List.of();
        return List.of(new ViewGroup<>(List.of(new EnergyView.Data(storage.getAmountAsLong(), storage.getCapacityAsLong()))));
    }

    @Override
    public Identifier getUid() {
        return ID;
    }

    public enum Client implements IClientExtensionProvider<EnergyView.Data, EnergyView> {
        INSTANCE;

        @Override
        public List<ClientViewGroup<EnergyView>> getClientGroups(Accessor<?> accessor, List<ViewGroup<EnergyView.Data>> groups) {
            return ClientViewGroup.map(groups, data -> EnergyView.read(data, "FE"), null);
        }

        @Override
        public Identifier getUid() {
            return ID;
        }
    }
}
