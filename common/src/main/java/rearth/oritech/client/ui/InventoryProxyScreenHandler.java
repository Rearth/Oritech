package rearth.oritech.client.ui;

import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.owo.serialization.CodecUtils;
import io.wispforest.owo.serialization.endec.MinecraftEndecs;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.block.entity.addons.InventoryProxyAddonBlockEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.util.ScreenProvider;

import java.util.Objects;

public class InventoryProxyScreenHandler extends ScreenHandler {
    
    @NotNull
    protected final BlockPos blockPos;
    @NotNull
    protected final InventoryProxyAddonBlockEntity addonEntity;
    
    protected int selectedSlot = 0;

    @NotNull
    protected final ScreenProvider controllerScreen;
    
    public record InvProxyData(BlockPos ownPos, BlockPos controllerPos, int slot) {
        public static final Endec<InvProxyData> PACKET_ENDEC = StructEndecBuilder.of(MinecraftEndecs.BLOCK_POS.fieldOf("ownPos", InvProxyData::ownPos), MinecraftEndecs.BLOCK_POS.fieldOf("controllerPos", InvProxyData::controllerPos), Endec.INT.fieldOf("slot", InvProxyData::slot), InvProxyData::new);
        public static final PacketCodec<PacketByteBuf, InvProxyData> PACKET_CODEC = CodecUtils.toPacketCodec(PACKET_ENDEC);
    }
    
    public InventoryProxyScreenHandler(int syncId, PlayerInventory inventory, PacketByteBuf buf) {
        this(syncId, inventory, InvProxyData.PACKET_CODEC.decode(buf));
    }
    
    public InventoryProxyScreenHandler(int syncId, PlayerInventory inventory, InvProxyData data) {
        this(syncId, inventory, Objects.requireNonNull(inventory.player.getWorld().getBlockEntity(data.ownPos())), (ScreenProvider) inventory.player.getWorld().getBlockEntity(data.controllerPos), data.slot);
    }

    // on server, also called from client constructor
    public InventoryProxyScreenHandler(int syncId, PlayerInventory playerInventory, BlockEntity blockEntity, @NotNull ScreenProvider controllerScreen, int selectedSlot) {
        super(ModScreens.INVENTORY_PROXY_SCREEN, syncId);
        
        this.blockPos = blockEntity.getPos();
        this.controllerScreen = controllerScreen;
        this.selectedSlot = selectedSlot;
        this.addonEntity = (InventoryProxyAddonBlockEntity) blockEntity;
    }

    public @NotNull BlockPos getBlockPos() {
        return blockPos;
    }
    
    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }
    
    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }
}
