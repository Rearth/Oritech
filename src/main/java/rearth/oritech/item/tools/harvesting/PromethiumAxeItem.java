package rearth.oritech.item.tools.harvesting;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ToolMaterial;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.entity.machines.interaction.TreefellerBlockEntity;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.client.renderers.PromethiumToolRenderer;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.RenderProvider;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class PromethiumAxeItem extends AxeItem implements GeoItem {
    
    private static final Deque<Pair<World, BlockPos>> pendingBlocks = new ArrayDeque<>();
    
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Supplier<Object> renderProvider = GeoItem.makeRenderer(this);
    
    public PromethiumAxeItem(ToolMaterial material, float attackDamage, float attackSpeed) {
        super(material, attackDamage, attackSpeed, new Settings().maxDamage(-1).maxCount(1));
    }
    
    @Override
    public boolean isSuitableFor(BlockState state) {
        return Items.DIAMOND_AXE.isSuitableFor(state)
                 || Items.DIAMOND_SWORD.isSuitableFor(state)
                 || Items.SHEARS.isSuitableFor(state);
    }
    
    @Override
    public boolean postMine(ItemStack stack, World world, BlockState state, BlockPos pos, LivingEntity miner) {
        
        if (!world.isClient && miner.isSneaking()) {
            var startPos = pos.up();
            var startState = world.getBlockState(startPos);
            if (startState.isIn(BlockTags.LOGS)) {
                var treeBlocks = TreefellerBlockEntity.getTreeBlocks(startPos, world);
                pendingBlocks.addAll(treeBlocks.stream().map(elem -> new Pair<>(world, elem)).toList());
            }
        }
        
        return super.postMine(stack, world, state, pos, miner);
    }
    
    public static void processPendingBlocks(World world) {
        if (pendingBlocks.isEmpty()) return;
        
        var topWorld = pendingBlocks.getFirst().getLeft();
        if (topWorld != world) return;
        
        for (int i = 0; i < 8 && !pendingBlocks.isEmpty(); i++) {
            var candidate = pendingBlocks.pollFirst().getRight();
            var candidateState = world.getBlockState(candidate);
            if (!candidateState.isIn(BlockTags.LOGS) && !candidateState.isIn(BlockTags.LEAVES)) return;
            
            var dropped = Block.getDroppedStacks(candidateState, (ServerWorld) world, candidate, null);
            world.setBlockState(candidate, Blocks.AIR.getDefaultState());
            
            dropped.forEach(elem -> world.spawnEntity(new ItemEntity(world, candidate.getX(), candidate.getY(), candidate.getZ(), elem)));
            
            world.playSound(null, candidate, candidateState.getSoundGroup().getBreakSound(), SoundCategory.BLOCKS, 0.5f, 1f);
            world.addBlockBreakParticles(candidate, candidateState);
            
            ParticleContent.BLOCK_DESTROY_EFFECT.spawn(world, Vec3d.of(candidate), 4);
            
            if (candidateState.isIn(BlockTags.LOGS)) break;
        }
        
    }
    
    @Override
    public boolean canRepair(ItemStack stack, ItemStack ingredient) {
        return false;
    }
    
    @Override
    public boolean isDamageable() {
        return false;
    }
    
    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }
    
    @Override
    public void createRenderer(Consumer<Object> consumer) {
        consumer.accept(new RenderProvider() {
            private PromethiumToolRenderer renderer;
            
            @Override
            public BuiltinModelItemRenderer getCustomRenderer() {
                if (this.renderer == null)
                    this.renderer = new PromethiumToolRenderer("promethium_axe");
                return renderer;
            }
        });
    }
    
    @Override
    public Supplier<Object> getRenderProvider() {
        return renderProvider;
    }
    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    
    }
    
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
    
    public static void onTick(ServerWorld serverWorld) {
        processPendingBlocks(serverWorld);
    }
    
    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        tooltip.add(Text.translatable("tooltip.oritech.promethium_axe").formatted(Formatting.DARK_GRAY));
    }
}
