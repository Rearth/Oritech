package rearth.oritech.item.tools.harvesting;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.ToolComponent;
import net.minecraft.component.type.ToolComponent.Rule;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
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
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.machines.interaction.TreefellerBlockEntity;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.client.renderers.PromethiumToolRenderer;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;

public class PromethiumAxeItem extends AxeItem implements GeoItem {
    
    private static final Deque<Pair<World, BlockPos>> pendingBlocks = new ArrayDeque<>();
    
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    
    public PromethiumAxeItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
        // a bit of a hack, but set tool components again after super()
        // this lets PromethiumAxeItem extend AxeItem (for the right-click actions) and still ignore
        // the default tool components set up by AxeItem
        var toolComponent = new ToolComponent(List.of(
            Rule.ofNeverDropping(toolMaterial.getInverseTag()),
            Rule.ofAlwaysDropping(BlockTags.AXE_MINEABLE, toolMaterial.getMiningSpeedMultiplier()),
            Rule.of(BlockTags.SWORD_EFFICIENT, 1.5F),
            Rule.ofAlwaysDropping(List.of(Blocks.COBWEB), 15.0F)),
            1.0F, 1);
        this.components = settings.component(DataComponentTypes.TOOL, toolComponent).getValidatedComponents();
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
        
        return true;
    }
    
    @Override
    public AttributeModifiersComponent getAttributeModifiers() {
        return super.getAttributeModifiers()
                 .with(EntityAttributes.PLAYER_BLOCK_INTERACTION_RANGE, new EntityAttributeModifier(Oritech.id("axe_block_range"), 2, EntityAttributeModifier.Operation.ADD_VALUE), AttributeModifierSlot.MAINHAND)
                 .with(EntityAttributes.PLAYER_ENTITY_INTERACTION_RANGE, new EntityAttributeModifier(Oritech.id("axe_entity_range"), 2, EntityAttributeModifier.Operation.ADD_VALUE), AttributeModifierSlot.MAINHAND);
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
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }
    
    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private PromethiumToolRenderer renderer;
            
            @Override
            public @Nullable BuiltinModelItemRenderer getGeoItemRenderer() {
                if (this.renderer == null)
                    this.renderer = new PromethiumToolRenderer("promethium_axe");
                return renderer;
            }
        });
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
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        tooltip.add(Text.translatable("tooltip.oritech.promethium_axe").formatted(Formatting.DARK_GRAY));
    }
}
