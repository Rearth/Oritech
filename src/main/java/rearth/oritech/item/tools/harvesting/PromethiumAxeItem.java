package rearth.oritech.item.tools.harvesting;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animation.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.component.Tool.Rule;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.entity.interaction.TreefellerBlockEntity;
import rearth.oritech.client.renderers.PromethiumToolRenderer;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;

public class PromethiumAxeItem extends AxeItem implements GeoItem {

    public record PendingBlock(Level level, BlockPos pos, ItemStack tool) {
    }

    public static final Deque<PendingBlock> pendingBlocks = new ArrayDeque<>();

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public PromethiumAxeItem(ToolMaterial toolMaterial, Properties settings) {
        super(toolMaterial, settings);
        // a bit of a hack, but set tool components again after super()
        // this lets PromethiumAxeItem extend AxeItem (for the right-click actions) and still ignore
        // the default tool components set up by AxeItem
        var toolComponent = new Tool(List.of(
                Rule.deniesDrops(toolMaterial.incorrectBlocksForDrops()),
                Rule.minesAndDrops(BlockTags.MINEABLE_WITH_AXE, toolMaterial.speed()),
                Rule.overrideSpeed(BlockTags.SWORD_EFFICIENT, 1.5F),
                Rule.minesAndDrops(List.of(Blocks.COBWEB), 15.0F)),
                1.0F, 1);
        this.components = settings.component(DataComponents.TOOL, toolComponent).buildAndValidateComponents();
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {

        if (!level.isClientSide() && miner.isShiftKeyDown()) {
            var startPos = pos.above();
            var startState = level.getBlockState(startPos);
            if (startState.is(BlockTags.LOGS)) {
                var treeBlocks = TreefellerBlockEntity.getTreeBlocks(startPos, level);
                pendingBlocks.addAll(treeBlocks.stream().map(elem -> new PendingBlock(level, elem, stack)).toList());
            }
        }

        return true;
    }

    public static void processPendingBlocks(Level level) {
        if (pendingBlocks.isEmpty()) return;

        var topWorld = pendingBlocks.getFirst().level();
        if (topWorld != level) return;

        for (int i = 0; i < 8 && !pendingBlocks.isEmpty(); i++) {
            var candidate = pendingBlocks.pollFirst();
            var candidatePos = candidate.pos();
            var candidateState = level.getBlockState(candidatePos);
            if (!candidateState.is(BlockTags.LOGS) && !candidateState.is(BlockTags.LEAVES)) return;

            var dropped = Block.getDrops(candidateState, (ServerLevel) level, candidatePos, null, null, candidate.tool());
            level.setBlockAndUpdate(candidatePos, Blocks.AIR.defaultBlockState());

            dropped.forEach(elem -> level.addFreshEntity(new ItemEntity(level, candidatePos.getX(), candidatePos.getY(), candidatePos.getZ(), elem)));

            level.playSound(null, candidatePos, candidateState.getSoundType().getBreakSound(), SoundSource.BLOCKS, 0.5f, 1f);
            level.addDestroyBlockEffect(candidatePos, candidateState);

            if (level instanceof ServerLevel sl)
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, candidatePos.getX() + 0.5, candidatePos.getY() + 0.5, candidatePos.getZ() + 0.5, 4, 0.6, 0.6, 0.6, 0);

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
        consumer.accept(Component.translatable("tooltip.oritech.promethium_axe").withStyle(ChatFormatting.DARK_GRAY));
    }
}
