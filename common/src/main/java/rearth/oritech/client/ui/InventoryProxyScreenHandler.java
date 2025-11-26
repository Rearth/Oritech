package rearth.oritech.client.ui;

import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.owo.serialization.CodecUtils;
import io.wispforest.owo.serialization.endec.MinecraftEndecs;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.block.entity.addons.InventoryProxyAddonBlockEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.util.ScreenProvider;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class InventoryProxyScreenHandler extends AbstractContainerMenu {
    
    @NotNull
    protected final BlockPos blockPos;
    @NotNull
    protected final InventoryProxyAddonBlockEntity addonEntity;
    
    protected int selectedSlot = 0;

    @NotNull
    protected final ScreenProvider controllerScreen;
    
    public record InvProxyData(BlockPos ownPos, BlockPos controllerPos, int slot) {
        public static final Endec<InvProxyData> PACKET_ENDEC = StructEndecBuilder.of(MinecraftEndecs.BLOCK_POS.fieldOf("ownPos", InvProxyData::ownPos), MinecraftEndecs.BLOCK_POS.fieldOf("controllerPos", InvProxyData::controllerPos), Endec.INT.fieldOf("slot", InvProxyData::slot), InvProxyData::new);
        public static final StreamCodec<FriendlyByteBuf, InvProxyData> PACKET_CODEC = CodecUtils.toPacketCodec(PACKET_ENDEC);
    }
    
    public InventoryProxyScreenHandler(int syncId, Inventory inventory, FriendlyByteBuf buf) {
        this(syncId, inventory, InvProxyData.PACKET_CODEC.decode(buf));
    }
    
    public InventoryProxyScreenHandler(int syncId, Inventory inventory, InvProxyData data) {
        this(syncId, inventory, Objects.requireNonNull(inventory.player.level().getBlockEntity(data.ownPos())), (ScreenProvider) inventory.player.level().getBlockEntity(data.controllerPos), data.slot);
    }

    // on server, also called from client constructor
    public InventoryProxyScreenHandler(int syncId, Inventory playerInventory, BlockEntity blockEntity, @NotNull ScreenProvider controllerScreen, int selectedSlot) {
        super(ModScreens.INVENTORY_PROXY_SCREEN, syncId);
        
        this.blockPos = blockEntity.getBlockPos();
        this.controllerScreen = controllerScreen;
        this.selectedSlot = selectedSlot;
        this.addonEntity = (InventoryProxyAddonBlockEntity) blockEntity;
    }

    public @NotNull BlockPos getBlockPos() {
        return blockPos;
    }
    
    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }
    
    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
