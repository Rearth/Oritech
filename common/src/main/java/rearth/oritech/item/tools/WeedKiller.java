package rearth.oritech.item.tools;

import rearth.oritech.client.init.ParticleContent;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public class WeedKiller extends Item {
    public WeedKiller(Properties settings) {
        super(settings);
    }
    
    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide())
            return super.useOn(context);
        
        var startPos = context.getClickedPos();
        
        new Thread(() -> doWeedKilling(context.getLevel(), startPos)).start();
        
        context.getItemInHand().consume(1, context.getPlayer());
        
        return InteractionResult.SUCCESS;
    }
    
    private void doWeedKilling(Level world, BlockPos startPos) {
        
        var maxRange = 20;
        var spreadRange = 3;
        var visited = new HashSet<BlockPos>();
        var open = new ArrayDeque<BlockPos>();
        open.add(startPos);
        
        while (!open.isEmpty()) {
            var candidate = open.pop();
            
            for (int x = -spreadRange; x <= spreadRange; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -spreadRange; z <= spreadRange; z++) {
                        
                        var target = new BlockPos(candidate.offset(x,y,z));
                        
                        if (visited.contains(target)) continue;
                        var distance = target.distManhattan(startPos);
                        
                        if (isWeedBlock(target, world) && distance < maxRange) {
                            open.add(target);
                            world.setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());
                            
                            ParticleContent.WEED_KILLER.spawn(world, target.getCenter(), new ParticleContent.LineData(candidate.getCenter(), target.getCenter()));
                            
                            try {
                                Thread.sleep(50);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            
                        }
                        
                        visited.add(target);
                        
                    }
                }
            }
            
        }
        
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag type) {
        super.appendHoverText(stack, context, tooltip, type);
        tooltip.add(Component.translatable("tooltip.oritech.weed_killer").withStyle(ChatFormatting.GRAY));
    }
    
    private boolean isWeedBlock(BlockPos pos, Level world) {
        var state = world.getBlockState(pos);
        if (state.isAir() || state.getFluidState().isSource()) return false;
        return state.canBeReplaced() || state.is(BlockTags.FLOWERS);
    }
}
