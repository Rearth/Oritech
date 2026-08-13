package rearth.oritech.item;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.storage.SchrodingersSafeBlockEntity;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.client.renderers.models.OritechBlockGeoModel;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.datamap.DataMapContent;

import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public class SchrodingersSafeItem extends Item implements GeoItem {
    private static final float ITEM_RENDER_SCALE = 0.25f;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public SchrodingersSafeItem(Properties settings) {
        super(settings);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {

    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private GeoItemRenderer<SchrodingersSafeItem> renderer;

            @Override
            public GeoItemRenderer<?> getGeoItemRenderer() {
                if (this.renderer == null)
                    this.renderer = new GeoItemRenderer<>(new OritechBlockGeoModel<>(Oritech.id("models/schrodingers_safe")));

                this.renderer.withScale(ITEM_RENDER_SCALE);

                return this.renderer;
            }
        });
    }

    @Override
    public void appendHoverText(ItemStack itemStack, TooltipContext context, TooltipDisplay display, Consumer<Component> consumer, TooltipFlag tooltipFlag) {
        super.appendHoverText(itemStack, context, display, consumer, tooltipFlag);

        var shiftPressed = Minecraft.getInstance().hasShiftDown();
        var ctrlPressed = Minecraft.getInstance().hasControlDown();

        if (shiftPressed) {
            consumer.accept(Component.translatable("tooltip.oritech.schrodingers_safe.sources").withStyle(ChatFormatting.DARK_PURPLE));
            getSourceBlocks().forEach(consumer);
        } else {
            consumer.accept(Component.translatable("tooltip.oritech.schrodingers_safe_extra_info").withStyle(ChatFormatting.DARK_PURPLE).withStyle(ChatFormatting.ITALIC));
        }

        if (ctrlPressed) {
            consumer.accept(Component.translatable("tooltip.oritech.schrodingers_safe.1").withStyle(ChatFormatting.GRAY));
            consumer.accept(Component.translatable("tooltip.oritech.schrodingers_safe.2").withStyle(ChatFormatting.GRAY));
            consumer.accept(Component.translatable("tooltip.oritech.schrodingers_safe.3").withStyle(ChatFormatting.GRAY));
            consumer.accept(Component.translatable("tooltip.oritech.schrodingers_safe.4").withStyle(ChatFormatting.GRAY));
            consumer.accept(Component.translatable("tooltip.oritech.schrodingers_safe.5").withStyle(ChatFormatting.GRAY));
        } else {
            consumer.accept(Component.translatable("tooltip.oritech.item_extra_info").withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.ITALIC));
        }

    }

    @Override
    public InteractionResult useOn(UseOnContext context) {

        var targetBlockPos = context.getClickedPos();
        var targetBlockState = context.getLevel().getBlockState(targetBlockPos);

        var sourceData = BuiltInRegistries.BLOCK.wrapAsHolder(targetBlockState.getBlock()).getData(DataMapContent.SCHRODINGERS_SAFE_SOURCE);
        var targetMultiplier = sourceData != null ? sourceData.quality() : -1f;

        for (var offset : SchrodingersSafeBlockEntity.getCoreOffsets()) {
            // the block is symetrical, so directions don't matter here
            var worldPos = targetBlockPos.offset(offset);
            var candidateState = context.getLevel().getBlockState(worldPos);
            if (!candidateState.canBeReplaced() && !offset.equals(new Vec3i(0, -1, 0))) {   // ignore below block for dragon egg support blocks
                context.getPlayer().sendSystemMessage(Component.translatable("text.oritech.schrodingers_safe_blocked"));
                ParticleContent.HighlightBlock(context.getLevel(), Vec3.atLowerCornerOf(worldPos));
                return InteractionResult.FAIL;
            }
        }

        if (targetMultiplier > 0) {
            context.getLevel().setBlockAndUpdate(targetBlockPos, BlockContent.SCHRODINGERS_SAFE.get().defaultBlockState());
            var createdBlockState = context.getLevel().getBlockState(targetBlockPos);
            createdBlockState.getBlock().setPlacedBy(context.getLevel(), targetBlockPos, createdBlockState, context.getPlayer(), context.getItemInHand());
            var createdEntity = context.getLevel().getBlockEntity(targetBlockPos, BlockEntitiesContent.SCHRODINGERS_SAFE_BLOCK.get()).get();
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

    public static List<MutableComponent> getSourceBlocks() {
        var dataMap = DataMapContent.SCHRODINGERS_SAFE_SOURCE;
        return BuiltInRegistries.BLOCK.listElements()
                .filter(holder -> holder.getData(dataMap) != null)
                .sorted(Comparator.comparingDouble(holder -> holder.getData(dataMap).quality()))
                .map(holder -> holder.value().getName()
                        .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)
                        .append(Component.literal(" (x" + holder.getData(dataMap).quality() + ")").withStyle(ChatFormatting.DARK_GRAY)))
                .toList();
    }
}
