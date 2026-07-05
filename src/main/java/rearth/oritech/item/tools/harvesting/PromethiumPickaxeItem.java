package rearth.oritech.item.tools.harvesting;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.SingletonGeoAnimatable;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.util.GeckoLibUtil;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.NonNull;
import rearth.oritech.Oritech;
import rearth.oritech.client.renderers.PromethiumToolRenderer;
import rearth.oritech.init.ComponentContent;
import rearth.oritech.init.TagContent;
import rearth.oritech.init.ToolsContent;
import rearth.oritech.util.PermissionHelpers;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PromethiumPickaxeItem extends Item implements GeoItem {

    private static final RawAnimation AREA_ANIM = RawAnimation.begin().thenLoop("area");
    private static final RawAnimation SILK_ANIM = RawAnimation.begin().thenLoop("silk_touch");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public PromethiumPickaxeItem(Properties properties) {
        super(properties);
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {

        // this intangible projectile is used as marker if an artificial silk touch has been applied
        if (!level.isClientSide() && stack.has(DataComponents.INTANGIBLE_PROJECTILE)) {
            var enchantments = stack.getEnchantments();
            var builder = new ItemEnchantments.Mutable(enchantments);
            builder.removeIf(elem -> elem.is(Enchantments.SILK_TOUCH));
            stack.set(DataComponents.ENCHANTMENTS, builder.toImmutable());
            stack.remove(DataComponents.INTANGIBLE_PROJECTILE);
        }

        return true;
    }

    private static boolean isAreaEnabled(ItemStack stack) {
        return stack.getOrDefault(ComponentContent.IS_AOE_ACTIVE.get(), false);
    }

    private static void setAreaEnabled(ItemStack stack, boolean enabled) {
        stack.set(ComponentContent.IS_AOE_ACTIVE.get(), enabled);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {

        if (!level.isClientSide() && player.isShiftKeyDown()) {
            var stack = player.getItemInHand(hand);

            var wasArea = isAreaEnabled(stack);
            var isArea = !wasArea;
            setAreaEnabled(stack, isArea);

            player.sendSystemMessage(isArea ? Component.translatable("message.oritech.tool_mode.area_effect") : Component.translatable("message.oritech.tool_mode.silk_touch"));
        }

        return super.use(level, player, hand);
    }

    public static List<BlockPos> getOffsetBlocks(LevelAccessor level, Player player, BlockPos pos) {
        var handStack = player.getMainHandItem();
        if (!handStack.is(ToolsContent.PROMETHIUM_PICKAXE)) return List.of();

        if (isAreaEnabled(handStack) && !player.isShiftKeyDown()) {
            var breakBlocks = new ArrayList<BlockPos>();
            var playerHit = player.pick(player.blockInteractionRange(), 0.0F, false);
            if (playerHit instanceof BlockHitResult blockHit) {
                var blockSide = blockHit.getDirection();
                var perpA = Direction.EAST;
                var perpB = Direction.NORTH;

                if (blockSide.equals(Direction.NORTH) || blockSide.equals(Direction.SOUTH)) {
                    perpA = Direction.UP;
                    perpB = Direction.EAST;
                } else if (blockSide.equals(Direction.EAST) || blockSide.equals(Direction.WEST)) {
                    perpA = Direction.UP;
                    perpB = Direction.NORTH;
                }

                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x == 0 && z == 0) continue;
                        var neighborPos = pos.offset(perpA.getUnitVec3i().multiply(x)).offset(perpB.getUnitVec3i().multiply(z));
                        breakBlocks.add(neighborPos);
                    }
                }

                return ImmutableList.copyOf(Iterables.filter(breakBlocks, p -> level.getBlockState(p).is(TagContent.DRILL_MINEABLE)));
            }
        }

        return List.of();
    }

    // called as event in Oritech initializer
    // area mode: breaks 3x3 blocks unless player is sneaking
    // silk touch mode: adds a temporary silk touch, which is then removed in the after break event
    // this is separate from mineBlock so that enchantments can be applied beforehand
    public static void preMine(Level level, BlockPos pos, BlockState state, ServerPlayer player) {
        if (PermissionHelpers.CHECKING_OFFSET_BREAK_PERMISSION.get()) return;

        var handStack = player.getMainHandItem();
        if (!handStack.is(ToolsContent.PROMETHIUM_PICKAXE)) return;

        // break additional blocks in preMine (Block.onBreak) instead of postMine (Block.onBroken)
        // so that the block still exists when determining which face of the block the player was looking at
        if (isAreaEnabled(handStack)) {
            // break additional blocks
            for (var offsetPos : getOffsetBlocks(level, player, pos)) {
                // drop itemStacks before breaking additional block, because level.breakBlock doesn't apply item enchantments if drop is enabled
                // this will ONLY apply item enchantments that affect block drops, and will not apply enchants like vein mining
                var offsetState = level.getBlockState(offsetPos);

                var canInteract = PermissionHelpers.CanPlayerBreakBlock(level, offsetPos, offsetState, player);
                if (!canInteract) continue;

                var offsetEntity = level.getBlockEntity(offsetPos);
                Block.dropResources(offsetState, level, offsetPos, offsetEntity, player, handStack);
                offsetState.getBlock().playerWillDestroy(level, offsetPos, offsetState, player);
                level.destroyBlock(offsetPos, false, player);
            }
        } else {
            // do silk touch
            var registry = level.registryAccess().getOrThrow(Registries.ENCHANTMENT).value();
            var registryEntry = registry.wrapAsHolder(registry.get(Enchantments.SILK_TOUCH).get().value());
            var hasExistingSilkTouch = handStack.getTagEnchantments().getLevel(registryEntry) > 0;

            if (!hasExistingSilkTouch) {
                handStack.enchant(registryEntry, 1);
                handStack.set(DataComponents.INTANGIBLE_PROJECTILE, Unit.INSTANCE);
            }
        }
    }

    public static ItemAttributeModifiers getRangeModifier(float range) {
        return ItemAttributeModifiers.builder()
                .add(
                        Attributes.BLOCK_INTERACTION_RANGE,
                        new AttributeModifier(Oritech.id("pick_block_range"), range, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(
                        Attributes.ENTITY_INTERACTION_RANGE,
                        new AttributeModifier(Oritech.id("pick_entity_range"), range, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }

    @Override
    public void appendHoverText(ItemStack itemStack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag) {
        super.appendHoverText(itemStack, context, display, builder, tooltipFlag);

        var area = isAreaEnabled(itemStack);

        builder.accept((area ? Component.translatable("tooltip.oritech.tool_mode.area_range.area") : Component.translatable("tooltip.oritech.tool_mode.area_range.single")).withStyle(ChatFormatting.GOLD));
        builder.accept(Component.translatable("tooltip.oritech.promethium_pick").withStyle(ChatFormatting.DARK_GRAY));

    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private PromethiumToolRenderer<PromethiumPickaxeItem> renderer;

            @Override
            public @NonNull GeoItemRenderer<?> getGeoItemRenderer() {
                if (this.renderer == null)
                    this.renderer = new PromethiumToolRenderer<>("promethium_pickaxe");
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("Pickaxe", 5,
                state -> PlayState.CONTINUE)
                .triggerableAnim("silk", SILK_ANIM)
                .triggerableAnim("area", AREA_ANIM));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // client only
    public void onHeldTick(ItemStack stack, Player player, ClientLevel level) {

        if (level.getGameTime() % 20 != 0) return;

        var area = isAreaEnabled(stack);
        triggerAnim(player, GeoItem.getId(stack), "Pickaxe", area ? "area" : "silk");

    }
}
