package rearth.oritech.block.blocks.generators;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.base.block.PassiveGeneratorBlock;
import rearth.oritech.block.entity.generators.BigSolarPanelEntity;
import rearth.oritech.util.MultiblockMachineController;

import java.util.List;

import static rearth.oritech.block.base.block.MultiblockMachine.ASSEMBLED;
import static rearth.oritech.util.TooltipHelper.addMachineTooltip;


public class BigSolarPanelBlock extends PassiveGeneratorBlock {

    public BigSolarPanelBlock(Properties settings) {
        super(settings);
        registerDefaultState(defaultBlockState().setValue(ASSEMBLED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ASSEMBLED);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BigSolarPanelEntity(pos, state);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {

        if (!level.isClientSide()) {

            var entity = level.getBlockEntity(pos);
            if (!(entity instanceof BigSolarPanelEntity solarPanel)) {
                return InteractionResult.SUCCESS;
            }

            var wasAssembled = state.getValue(ASSEMBLED);

            if (!wasAssembled) {
                var corePlaced = solarPanel.tryPlaceNextCore(player);
                if (corePlaced) return InteractionResult.SUCCESS;
            }

            var isAssembled = solarPanel.initMultiblock(state);

            // first time created
            if (isAssembled && !wasAssembled) {
                solarPanel.triggerSetupAnimation();
                return InteractionResult.SUCCESS;
            }

            if (!isAssembled) {
                player.sendSystemMessage(Component.translatable("message.oritech.machine.missing_core"));
            } else {
                solarPanel.sendInfoMessageToPlayer(player);
            }
            return InteractionResult.SUCCESS;

        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {

        if (!level.isClientSide() && state.getValue(ASSEMBLED)) {

            var entity = level.getBlockEntity(pos);
            if (entity instanceof MultiblockMachineController machineEntity) {
                machineEntity.onControllerBroken();
            }
        }

        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public void addToTooltip(Item.TooltipContext tooltipContext, Consumer<Component> consumer, TooltipFlag tooltipFlag, DataComponentGetter dataComponentGetter) {
        super.appendHoverText(stack, context, tooltip, options);
        addMachineTooltip(tooltip, this, this);
        if (Minecraft.getInstance().hasControlDown())
            tooltip.add(Component.translatable("tooltip.oritech.solar_generation").withStyle(ChatFormatting.GRAY));
    }
}
