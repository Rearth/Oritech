package rearth.oritech.item;

import net.minecraft.block.Block;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.storage.UnstableContainerBlockEntity;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.TagContent;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class UnstableContainerItem extends Item implements GeoItem {
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private final float scale;
    private final String name;
    
    public UnstableContainerItem(Settings settings, float scale, String name) {
        super(settings);
        this.scale = scale;
        this.name = name;
    }
    
    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        
        consumer.accept(new GeoRenderProvider() {
            GeoItemRenderer<UnstableContainerItem> renderer = null;
            
            @Override
            public @Nullable BuiltinModelItemRenderer getGeoItemRenderer() {
                if (this.renderer == null)
                    this.renderer = new GeoItemRenderer<>(new DefaultedBlockGeoModel<>(Oritech.id("models/" + name)));
                
                this.renderer.withScale(scale);
                
                return this.renderer;
            }
        });
    }
    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    
    }
    
    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        
        var shiftPressed = Screen.hasShiftDown();
        var ctrlPressed = Screen.hasControlDown();
        
        if (shiftPressed) {
            tooltip.add(Text.translatable("tooltip.oritech.unstable_container.low").formatted(Formatting.DARK_PURPLE));
            tooltip.addAll(getBlocksFromTag(TagContent.UNSTABLE_CONTAINER_SOURCES_LOW));
            tooltip.add(Text.translatable("tooltip.oritech.unstable_container.medium").formatted(Formatting.DARK_PURPLE));
            tooltip.addAll(getBlocksFromTag(TagContent.UNSTABLE_CONTAINER_SOURCES_MEDIUM));
            tooltip.add(Text.translatable("tooltip.oritech.unstable_container.high").formatted(Formatting.DARK_PURPLE));
            tooltip.addAll(getBlocksFromTag(TagContent.UNSTABLE_CONTAINER_SOURCES_HIGH));
        } else {
            tooltip.add(Text.translatable("tooltip.oritech.unstable_container_extra_info").formatted(Formatting.DARK_PURPLE).formatted(Formatting.ITALIC));
        }
        
        if (ctrlPressed) {
            tooltip.add(Text.translatable("tooltip.oritech.unstable_container.1").formatted(Formatting.GRAY));
            tooltip.add(Text.translatable("tooltip.oritech.unstable_container.2").formatted(Formatting.GRAY));
            tooltip.add(Text.translatable("tooltip.oritech.unstable_container.3").formatted(Formatting.GRAY));
            tooltip.add(Text.translatable("tooltip.oritech.unstable_container.4").formatted(Formatting.GRAY));
            tooltip.add(Text.translatable("tooltip.oritech.unstable_container.5").formatted(Formatting.GRAY));
        } else {
            tooltip.add(Text.translatable("tooltip.oritech.item_extra_info").formatted(Formatting.GRAY).formatted(Formatting.ITALIC));
        }
        super.appendTooltip(stack, context, tooltip, type);
        
    }
    
    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        
        var targetBlockPos = context.getBlockPos();
        var targetBlockState = context.getWorld().getBlockState(targetBlockPos);
        
        var targetMultiplier = -1f;
        if (targetBlockState.isIn(TagContent.UNSTABLE_CONTAINER_SOURCES_LOW)) {
            targetMultiplier = 0.3f;
        } else if (targetBlockState.isIn(TagContent.UNSTABLE_CONTAINER_SOURCES_MEDIUM)) {
            targetMultiplier = 1f;
        } else if (targetBlockState.isIn(TagContent.UNSTABLE_CONTAINER_SOURCES_HIGH)) {
            targetMultiplier = 5f;
        }
        
        for (var offset : UnstableContainerBlockEntity.getCoreOffsets()) {
            // the block is symetrical, so directions don't matter here
            var worldPos = targetBlockPos.add(offset);
            var candidateState = context.getWorld().getBlockState(worldPos);
            if (!candidateState.isReplaceable() && !offset.equals(new Vec3i(0, -1, 0))) {   // ignore below block for dragon egg support blocks
                context.getPlayer().sendMessage(Text.translatable("text.oritech.unstable_container_blocked"));
                ParticleContent.HIGHLIGHT_BLOCK.spawn(context.getWorld(), Vec3d.of(worldPos));
                return ActionResult.FAIL;
            }
        }
        
        if (targetMultiplier > 0) {
            context.getWorld().setBlockState(targetBlockPos, BlockContent.UNSTABLE_CONTAINER.getDefaultState());
            var createdBlockState = context.getWorld().getBlockState(targetBlockPos);
            createdBlockState.getBlock().onPlaced(context.getWorld(), targetBlockPos, createdBlockState, context.getPlayer(), context.getStack());
            var createdEntity = context.getWorld().getBlockEntity(targetBlockPos, BlockEntitiesContent.UNSTABLE_CONTAINER_BLOCK_ENTITY).get();
            createdEntity.setCapturedBlock(targetBlockState);
            createdEntity.qualityMultiplier = targetMultiplier;
            
            var player = context.getPlayer();
            if (!player.isCreative()) {
                var stack = context.getStack();
                stack.decrement(1);
            }
            return ActionResult.CONSUME;
        }
        
        return super.useOnBlock(context);
    }
    
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
    
    public static List<MutableText> getBlocksFromTag(TagKey<Block> tagKey) {
        
        var candidate = Registries.BLOCK.getEntryList(tagKey);
        //noinspection OptionalIsPresent
        if (candidate.isEmpty()) return new ArrayList<>();
        
        return candidate.get().stream().map(blockRegistryEntry -> blockRegistryEntry.value().getName().formatted(Formatting.GRAY, Formatting.ITALIC)).toList();
    }
}
