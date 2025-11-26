package rearth.oritech.item.tools.harvesting;

import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.interaction.TreefellerBlockEntity;
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
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.component.Tool.Rule;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class PromethiumAxeItem extends AxeItem implements GeoItem {
    
    public static final Deque<Tuple<Level, BlockPos>> pendingBlocks = new ArrayDeque<>();
    
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    
    public PromethiumAxeItem(Tier toolMaterial, Properties settings) {
        super(toolMaterial, settings);
        // a bit of a hack, but set tool components again after super()
        // this lets PromethiumAxeItem extend AxeItem (for the right-click actions) and still ignore
        // the default tool components set up by AxeItem
        var toolComponent = new Tool(List.of(
            Rule.deniesDrops(toolMaterial.getIncorrectBlocksForDrops()),
            Rule.minesAndDrops(BlockTags.MINEABLE_WITH_AXE, toolMaterial.getSpeed()),
            Rule.overrideSpeed(BlockTags.SWORD_EFFICIENT, 1.5F),
            Rule.minesAndDrops(List.of(Blocks.COBWEB), 15.0F)),
            1.0F, 1);
        this.components = settings.component(DataComponents.TOOL, toolComponent).buildAndValidateComponents();
    }
    
    @Override
    public boolean mineBlock(ItemStack stack, Level world, BlockState state, BlockPos pos, LivingEntity miner) {
        
        if (!world.isClientSide && miner.isShiftKeyDown()) {
            var startPos = pos.above();
            var startState = world.getBlockState(startPos);
            if (startState.is(BlockTags.LOGS)) {
                var treeBlocks = TreefellerBlockEntity.getTreeBlocks(startPos, world);
                pendingBlocks.addAll(treeBlocks.stream().map(elem -> new Tuple<>(world, elem)).toList());
            }
        }
        
        return true;
    }
    
    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers() {
        return super.getDefaultAttributeModifiers()
                 .withModifierAdded(Attributes.BLOCK_INTERACTION_RANGE, new AttributeModifier(Oritech.id("axe_block_range"), 2, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                 .withModifierAdded(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(Oritech.id("axe_entity_range"), 2, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
    }
    
    public static void processPendingBlocks(Level world) {
        if (pendingBlocks.isEmpty()) return;
        
        var topWorld = pendingBlocks.getFirst().getA();
        if (topWorld != world) return;
        
        for (int i = 0; i < 8 && !pendingBlocks.isEmpty(); i++) {
            var candidate = pendingBlocks.pollFirst().getB();
            var candidateState = world.getBlockState(candidate);
            if (!candidateState.is(BlockTags.LOGS) && !candidateState.is(BlockTags.LEAVES)) return;
            
            var dropped = Block.getDrops(candidateState, (ServerLevel) world, candidate, null);
            world.setBlockAndUpdate(candidate, Blocks.AIR.defaultBlockState());
            
            dropped.forEach(elem -> world.addFreshEntity(new ItemEntity(world, candidate.getX(), candidate.getY(), candidate.getZ(), elem)));
            
            world.playSound(null, candidate, candidateState.getSoundType().getBreakSound(), SoundSource.BLOCKS, 0.5f, 1f);
            world.addDestroyBlockEffect(candidate, candidateState);
            
            ParticleContent.BLOCK_DESTROY_EFFECT.spawn(world, Vec3.atLowerCornerOf(candidate), 4);
            
            if (candidateState.is(BlockTags.LOGS)) break;
        }
        
    }
    
    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack ingredient) {
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
            public @Nullable BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
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
    
    public static void onTick(ServerLevel serverWorld) {
        processPendingBlocks(serverWorld);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag type) {
        super.appendHoverText(stack, context, tooltip, type);
        tooltip.add(Component.translatable("tooltip.oritech.promethium_axe").withStyle(ChatFormatting.DARK_GRAY));
    }
}
