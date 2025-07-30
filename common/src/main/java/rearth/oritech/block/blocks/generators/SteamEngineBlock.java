package rearth.oritech.block.blocks.generators;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.base.block.MultiblockMachine;
import rearth.oritech.block.entity.generators.SteamEngineEntity;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.init.BlockEntitiesContent;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class SteamEngineBlock extends MultiblockMachine {
    public SteamEngineBlock(Properties settings) {
        super(settings);
    }
    
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return Objects.requireNonNull(super.getStateForPlacement(ctx)).setValue(BlockStateProperties.HORIZONTAL_FACING, ctx.getHorizontalDirection().getOpposite());
    }
    
    @Override
    public @NotNull Class<? extends BlockEntity> getBlockEntityType() {
        return SteamEngineEntity.class;
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag options) {
        
        if (Screen.hasControlDown()) {
            tooltip.add(Component.translatable("tooltip.oritech.steam_engine").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.oritech.steam_engine.1").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.oritech.steam_engine.2").withStyle(ChatFormatting.GRAY));
        }
        
        super.appendHoverText(stack, context, tooltip, options);
    }
    
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        
        if (!world.isClientSide) {
            
            var entity = world.getBlockEntity(pos, BlockEntitiesContent.STEAM_ENGINE_ENTITY);
            if (entity.isPresent() && entity.get().inSlaveMode()) {
                // working in slave mode. Don't open UI, just highlight controller
                player.sendSystemMessage(Component.translatable("message.oritech.steamengine.controller_link"));
                ParticleContent.HIGHLIGHT_BLOCK.spawn(world, Vec3.atLowerCornerOf(entity.get().master.getBlockPos()));
                return InteractionResult.SUCCESS;
            }
            
            
            
        }
        
        return super.useWithoutItem(state, world, pos, player, hit);
    }
}
