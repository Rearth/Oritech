package rearth.oritech.item;

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
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class UnstableContainerItem extends Item implements GeoItem {
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private final float scale;
    private final String name;
    
    public UnstableContainerItem(Properties settings, float scale, String name) {
        super(settings);
        this.scale = scale;
        this.name = name;
    }
    
    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        
        consumer.accept(new GeoRenderProvider() {
            GeoItemRenderer<UnstableContainerItem> renderer = null;
            
            @Override
            public @Nullable BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
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
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag type) {
        
        var shiftPressed = Screen.hasShiftDown();
        var ctrlPressed = Screen.hasControlDown();
        
        if (shiftPressed) {
            tooltip.add(Component.translatable("tooltip.oritech.unstable_container.low").withStyle(ChatFormatting.DARK_PURPLE));
            tooltip.addAll(getBlocksFromTag(TagContent.UNSTABLE_CONTAINER_SOURCES_LOW));
            tooltip.add(Component.translatable("tooltip.oritech.unstable_container.medium").withStyle(ChatFormatting.DARK_PURPLE));
            tooltip.addAll(getBlocksFromTag(TagContent.UNSTABLE_CONTAINER_SOURCES_MEDIUM));
            tooltip.add(Component.translatable("tooltip.oritech.unstable_container.high").withStyle(ChatFormatting.DARK_PURPLE));
            tooltip.addAll(getBlocksFromTag(TagContent.UNSTABLE_CONTAINER_SOURCES_HIGH));
        } else {
            tooltip.add(Component.translatable("tooltip.oritech.unstable_container_extra_info").withStyle(ChatFormatting.DARK_PURPLE).withStyle(ChatFormatting.ITALIC));
        }
        
        if (ctrlPressed) {
            tooltip.add(Component.translatable("tooltip.oritech.unstable_container.1").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.oritech.unstable_container.2").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.oritech.unstable_container.3").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.oritech.unstable_container.4").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.oritech.unstable_container.5").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.oritech.item_extra_info").withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.ITALIC));
        }
        super.appendHoverText(stack, context, tooltip, type);
        
    }
    
    @Override
    public InteractionResult useOn(UseOnContext context) {
        
        var targetBlockPos = context.getClickedPos();
        var targetBlockState = context.getLevel().getBlockState(targetBlockPos);
        
        var targetMultiplier = -1f;
        if (targetBlockState.is(TagContent.UNSTABLE_CONTAINER_SOURCES_LOW)) {
            targetMultiplier = 0.3f;
        } else if (targetBlockState.is(TagContent.UNSTABLE_CONTAINER_SOURCES_MEDIUM)) {
            targetMultiplier = 1f;
        } else if (targetBlockState.is(TagContent.UNSTABLE_CONTAINER_SOURCES_HIGH)) {
            targetMultiplier = 5f;
        }
        
        for (var offset : UnstableContainerBlockEntity.getCoreOffsets()) {
            // the block is symetrical, so directions don't matter here
            var worldPos = targetBlockPos.offset(offset);
            var candidateState = context.getLevel().getBlockState(worldPos);
            if (!candidateState.canBeReplaced() && !offset.equals(new Vec3i(0, -1, 0))) {   // ignore below block for dragon egg support blocks
                context.getPlayer().sendSystemMessage(Component.translatable("text.oritech.unstable_container_blocked"));
                ParticleContent.HIGHLIGHT_BLOCK.spawn(context.getLevel(), Vec3.atLowerCornerOf(worldPos));
                return InteractionResult.FAIL;
            }
        }
        
        if (targetMultiplier > 0) {
            context.getLevel().setBlockAndUpdate(targetBlockPos, BlockContent.UNSTABLE_CONTAINER.defaultBlockState());
            var createdBlockState = context.getLevel().getBlockState(targetBlockPos);
            createdBlockState.getBlock().setPlacedBy(context.getLevel(), targetBlockPos, createdBlockState, context.getPlayer(), context.getItemInHand());
            var createdEntity = context.getLevel().getBlockEntity(targetBlockPos, BlockEntitiesContent.UNSTABLE_CONTAINER_BLOCK_ENTITY).get();
            createdEntity.setCapturedBlock(targetBlockState);
            createdEntity.qualityMultiplier = targetMultiplier;
            
            var player = context.getPlayer();
            if (!player.isCreative()) {
                var stack = context.getItemInHand();
                stack.shrink(1);
            }
            return InteractionResult.CONSUME;
        }
        
        return super.useOn(context);
    }
    
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
    
    public static List<MutableComponent> getBlocksFromTag(TagKey<Block> tagKey) {
        
        var candidate = BuiltInRegistries.BLOCK.getTag(tagKey);
        //noinspection OptionalIsPresent
        if (candidate.isEmpty()) return new ArrayList<>();
        
        return candidate.get().stream().map(blockRegistryEntry -> blockRegistryEntry.value().getName().withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)).toList();
    }
}
