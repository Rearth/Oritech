package rearth.oritech.init.compat.jade;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.MachineCoreEntity;
import snownee.jade.api.Accessor;
import snownee.jade.api.view.ClientViewGroup;
import snownee.jade.api.view.IClientExtensionProvider;
import snownee.jade.api.view.IServerExtensionProvider;
import snownee.jade.api.view.ItemView;
import snownee.jade.api.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public enum OritechCoreItemProvider implements IServerExtensionProvider<ItemStack> {
    INSTANCE;

    private static final Identifier ID = Oritech.id("machine_core_items");

    @Override
    public boolean shouldRequestData(Accessor<?> accessor) {
        return OritechJadePlugin.isConnectedCore(accessor);
    }

    @Override
    public List<ViewGroup<ItemStack>> getGroups(Accessor<?> accessor) {
        if (!(accessor.getTarget() instanceof MachineCoreEntity core) || !core.isEnabled()) return List.of();
        var storage = core.getItemLookup(null);
        if (storage == null) return List.of();
        var items = new ArrayList<ItemStack>();
        for (int slot = 0; slot < storage.size(); slot++) {
            var resource = storage.getResource(slot);
            var amount = storage.getAmountAsInt(slot);
            if (!resource.isEmpty() && amount > 0) items.add(resource.toStack(amount));
        }
        return items.isEmpty() ? List.of() : List.of(new ViewGroup<>(items));
    }

    @Override
    public Identifier getUid() {
        return ID;
    }

    public enum Client implements IClientExtensionProvider<ItemStack, ItemView> {
        INSTANCE;

        @Override
        public List<ClientViewGroup<ItemView>> getClientGroups(Accessor<?> accessor, List<ViewGroup<ItemStack>> groups) {
            return ClientViewGroup.map(groups, ItemView::new, null);
        }

        @Override
        public Identifier getUid() {
            return ID;
        }
    }
}
