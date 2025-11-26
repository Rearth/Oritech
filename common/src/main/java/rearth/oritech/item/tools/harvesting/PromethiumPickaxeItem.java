package rearth.oritech.item.tools.harvesting;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import dev.architectury.event.EventResult;
import dev.architectury.utils.value.IntValue;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments.Mutable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.client.renderers.PromethiumToolRenderer;
import rearth.oritech.init.ComponentContent;
import rearth.oritech.init.TagContent;
import rearth.oritech.init.ToolsContent;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PromethiumPickaxeItem extends DiggerItem implements GeoItem {
    
    private static final RawAnimation AREA_ANIM = RawAnimation.begin().thenLoop("area");
    private static final RawAnimation SILK_ANIM = RawAnimation.begin().thenLoop("silk_touch");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    
    public PromethiumPickaxeItem(Tier toolMaterial, TagKey<Block> effectiveBlocks, Properties settings) {
        super(toolMaterial, effectiveBlocks, settings);
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
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
    public boolean mineBlock(ItemStack stack, Level world, BlockState state, BlockPos pos, LivingEntity miner) {
        if (!world.isClientSide && stack.has(DataComponents.INTANGIBLE_PROJECTILE)) {
            var enchantments = stack.getEnchantments();
            var builder = new ItemEnchantments.Mutable(enchantments);
            builder.removeIf(elem -> elem.is(Enchantments.SILK_TOUCH));
            stack.set(DataComponents.ENCHANTMENTS, builder.toImmutable());
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
    public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
        
        if (!world.isClientSide && user.isShiftKeyDown()) {
            var stack = user.getItemInHand(hand);
            
            var wasArea = isAreaEnabled(stack);
            var isArea = !wasArea;
            setAreaEnabled(stack, isArea);
            
            user.sendSystemMessage(isArea ? Component.translatable("message.oritech.tool_mode.area_effect") : Component.translatable("message.oritech.tool_mode.silk_touch"));
        }
        
        return super.use(world, user, hand);
    }
    
    public static List<BlockPos> getOffsetBlocks(Level world, Player player, BlockPos pos) {
        var handStack = player.getMainHandItem();
        if (handStack == null || !handStack.is(ToolsContent.PROMETHIUM_PICKAXE)) return List.of();
        
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
                        var neighborPos = pos.offset(perpA.getNormal().multiply(x)).offset(perpB.getNormal().multiply(z));
                        breakBlocks.add(neighborPos);
                    }
                }
                
                return ImmutableList.copyOf(Iterables.filter(breakBlocks, p -> world.getBlockState(p).is(TagContent.DRILL_MINEABLE)));
            }
        }
        
        return List.of();
    }
    
    // called as event in Oritech initializer
    // area mode: breaks 3x3 blocks unless player is sneaking
    // silk touch mode: adds a temporary silk touch, which is then removed in the after break event
    public static EventResult preMine(Level world, BlockPos pos, BlockState state, ServerPlayer player, @Nullable IntValue xp) {
        
        var handStack = player.getMainHandItem();
        if (handStack == null || !handStack.is(ToolsContent.PROMETHIUM_PICKAXE)) return EventResult.pass();
        
        // break additional blocks in preMine (Block.onBreak) instead of postMine (Block.onBroken)
        // so that the block still exists when determining which face of the block the player was looking at
        if (isAreaEnabled(handStack)) {
            // break additional blocks
            for (var offsetPos : getOffsetBlocks(world, player, pos)) {
                // drop stacks before breaking additional block, because world.breakBlock doesn't apply item enchantments if drop is enabled
                // this will ONLY apply item enchantments that affect block drops, and will not apply enchants like vein mining
                var offsetState = world.getBlockState(offsetPos);
                var offsetEntity = world.getBlockEntity(offsetPos);
                Block.dropResources(offsetState, world, offsetPos, offsetEntity, player, handStack);
                offsetState.getBlock().playerWillDestroy(world, offsetPos, offsetState, player);
                world.destroyBlock(offsetPos, false, player);
            }
        } else {
            // do silk touch
            var hasExistingSilkTouch = EnchantmentHelper.getEnchantmentsForCrafting(handStack).keySet().stream().anyMatch(elem -> elem.is(Enchantments.SILK_TOUCH));
            
            if (!hasExistingSilkTouch) {
                var registryEntry = world.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(Enchantments.SILK_TOUCH).get();
                handStack.enchant(registryEntry, 1);
                handStack.set(DataComponents.INTANGIBLE_PROJECTILE, Unit.INSTANCE);
            }
        }
        
        return EventResult.pass();
    }
    
    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers() {
        return super.getDefaultAttributeModifiers()
                 .withModifierAdded(Attributes.BLOCK_INTERACTION_RANGE, new AttributeModifier(Oritech.id("pick_block_range"), 2, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                 .withModifierAdded(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(Oritech.id("pick_entity_range"), 2, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag type) {
        super.appendHoverText(stack, context, tooltip, type);
        
        var area = isAreaEnabled(stack);
        
        tooltip.add((area ? Component.translatable("tooltip.oritech.tool_mode.area_range.area") : Component.translatable("tooltip.oritech.tool_mode.area_range.single")).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.oritech.promethium_pick").withStyle(ChatFormatting.DARK_GRAY));
        
    }
    
    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private PromethiumToolRenderer renderer;
            
            @Override
            public @Nullable BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (this.renderer == null)
                    this.renderer = new PromethiumToolRenderer("promethium_pickaxe");
                return renderer;
            }
        });
    }
    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "Pickaxe", 5, state -> PlayState.CONTINUE).triggerableAnim("silk", SILK_ANIM).triggerableAnim("area", AREA_ANIM));
    }
    
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
    
    // client only
    public void onHeldTick(ItemStack stack, Player player, ClientLevel world) {
        
        if (world.getGameTime() % 20 != 0) return;
        
        var area = isAreaEnabled(stack);
        triggerAnim(player, GeoItem.getId(stack), "Pickaxe", area ? "area" : "silk");
        
    }
}