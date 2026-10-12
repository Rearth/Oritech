package rearth.oritech.init.compat.jade;

import net.minecraft.resources.Identifier;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.MachineCoreEntity;
import snownee.jade.api.Accessor;
import snownee.jade.api.fluid.JadeFluidObject;
import snownee.jade.api.view.ClientViewGroup;
import snownee.jade.api.view.FluidView;
import snownee.jade.api.view.IClientExtensionProvider;
import snownee.jade.api.view.IServerExtensionProvider;
import snownee.jade.api.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public enum OritechCoreFluidProvider implements IServerExtensionProvider<FluidView.Data> {
    INSTANCE;

    private static final Identifier ID = Oritech.id("machine_core_fluid");

    @Override
    public boolean shouldRequestData(Accessor<?> accessor) {
        return OritechJadePlugin.isConnectedCore(accessor);
    }

    @Override
    public List<ViewGroup<FluidView.Data>> getGroups(Accessor<?> accessor) {
        if (!(accessor.getTarget() instanceof MachineCoreEntity core) || !core.isEnabled()) return List.of();
        var storage = core.getFluidLookup(null);
        if (storage == null) return List.of();
        var tanks = new ArrayList<FluidView.Data>();
        for (int slot = 0; slot < storage.size(); slot++) {
            var resource = storage.getResource(slot);
            var capacity = storage.getCapacityAsLong(slot, resource);
            if (capacity <= 0) continue;
            var fluid = resource.isEmpty() ? JadeFluidObject.empty()
                    : JadeFluidObject.of(resource.getFluid(), storage.getAmountAsLong(slot), resource.getComponentsPatch());
            tanks.add(new FluidView.Data(fluid, capacity));
        }
        return tanks.isEmpty() ? List.of() : List.of(new ViewGroup<>(tanks));
    }

    @Override
    public Identifier getUid() {
        return ID;
    }

    public enum Client implements IClientExtensionProvider<FluidView.Data, FluidView> {
        INSTANCE;

        @Override
        public List<ClientViewGroup<FluidView>> getClientGroups(Accessor<?> accessor, List<ViewGroup<FluidView.Data>> groups) {
            return ClientViewGroup.map(groups, FluidView::readDefault, null);
        }

        @Override
        public Identifier getUid() {
            return ID;
        }
    }
}
