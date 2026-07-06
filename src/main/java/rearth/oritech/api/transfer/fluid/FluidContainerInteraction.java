package rearth.oritech.api.transfer.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.util.ScreenProvider;

import java.util.List;
import java.util.Objects;

public final class FluidContainerInteraction {

    private FluidContainerInteraction() {
    }

    public static boolean tryFluidBlockItemInteraction(Level level, BlockPos pos, Player player, InteractionHand hand) {
        return tryFluidBlockItemInteraction(level, pos, level.getBlockEntity(pos), player, hand);
    }

    public static boolean tryFluidBlockItemInteraction(Level level, BlockPos pos, @Nullable BlockEntity blockEntity, Player player, InteractionHand hand) {
        if (blockEntity == null || player.getItemInHand(hand).isEmpty()) return false;

        var tankStorages = getInteractableFluidStorages(blockEntity);
        if (tankStorages.isEmpty()) return false;

        var itemAccess = ItemAccess.forPlayerInteraction(player, hand).oneByOne();
        var itemFluidStorage = itemAccess.getCapability(Capabilities.Fluid.ITEM);
        if (itemFluidStorage == null) return false;

        var moveItemToBlock = hasFluid(itemFluidStorage);
        var moved = false;

        try (var transaction = Transaction.openRoot()) {
            for (var tankStorage : tankStorages) {
                var transferResult = moveItemToBlock
                        ? ResourceHandlerUtil.moveFirst(itemFluidStorage, tankStorage, resource -> true, Integer.MAX_VALUE, transaction)
                        : ResourceHandlerUtil.moveFirst(tankStorage, itemFluidStorage, resource -> true, Integer.MAX_VALUE, transaction);

                if (transferResult != null && !transferResult.isEmpty()) {
                    moved = true;
                    break;
                }
            }

            if (moved && !level.isClientSide()) {
                transaction.commit();
            }
        }

        if (moved && !level.isClientSide()) {
            level.playSound(null, pos, SoundEvents.AXOLOTL_SPLASH, SoundSource.PLAYERS, 0.8f, 1.4f);
        }

        return moved;
    }

    private static boolean hasFluid(ResourceHandler<FluidResource> handler) {
        for (int slot = 0; slot < handler.size(); slot++) {
            if (!handler.getResource(slot).isEmpty() && handler.getAmountAsLong(slot) > 0) {
                return true;
            }
        }

        return false;
    }

    private static List<ResourceHandler<FluidResource>> getInteractableFluidStorages(BlockEntity blockEntity) {
        if (blockEntity instanceof ScreenProvider screenProvider) {
            return screenProvider.getInteractableFluidStorages().stream()
                    .filter(Objects::nonNull)
                    .toList();
        }

        if (blockEntity instanceof FluidProvider fluidProvider) {
            var storage = fluidProvider.getFluidLookup(null);
            if (storage != null) {
                return List.of(storage);
            }
        }

        return List.of();
    }
}
