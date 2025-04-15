package rearth.oritech.block.blocks.generators;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.base.block.MultiblockMachine;
import rearth.oritech.block.entity.generators.SteamEngineEntity;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.init.BlockEntitiesContent;

import java.util.List;
import java.util.Objects;

public class SteamEngineBlock extends MultiblockMachine {
    public SteamEngineBlock(Settings settings) {
        super(settings);
    }
    
    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return Objects.requireNonNull(super.getPlacementState(ctx)).with(Properties.HORIZONTAL_FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }
    
    @Override
    public @NotNull Class<? extends BlockEntity> getBlockEntityType() {
        return SteamEngineEntity.class;
    }
    
    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType options) {
        
        if (Screen.hasControlDown()) {
            tooltip.add(Text.translatable("tooltip.oritech.steam_engine").formatted(Formatting.GRAY));
            tooltip.add(Text.translatable("tooltip.oritech.steam_engine.1").formatted(Formatting.GRAY));
            tooltip.add(Text.translatable("tooltip.oritech.steam_engine.2").formatted(Formatting.GRAY));
        }
        
        super.appendTooltip(stack, context, tooltip, options);
    }
    
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        
        if (!world.isClient) {
            
            var entity = world.getBlockEntity(pos, BlockEntitiesContent.STEAM_ENGINE_ENTITY);
            if (entity.isPresent() && entity.get().inSlaveMode()) {
                // working in slave mode. Don't open UI, just highlight controller
                player.sendMessage(Text.translatable("message.oritech.steamengine.controller_link"));
                ParticleContent.HIGHLIGHT_BLOCK.spawn(world, Vec3d.of(entity.get().master.getPos()));
                return ActionResult.SUCCESS;
            }
            
            
            
        }
        
        return super.onUse(state, world, pos, player, hit);
    }
}
