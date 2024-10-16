package rearth.oritech.item.tools.harvesting;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MiningToolItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.Unit;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import rearth.oritech.Oritech;
import rearth.oritech.client.renderers.PromethiumToolRenderer;
import rearth.oritech.init.ComponentContent;
import rearth.oritech.init.ToolsContent;
import rearth.oritech.init.datagen.data.TagContent;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;

public class PromethiumPickaxeItem extends MiningToolItem implements GeoItem {
    
    private static final RawAnimation AREA_ANIM = RawAnimation.begin().thenLoop("area");
    private static final RawAnimation SILK_ANIM = RawAnimation.begin().thenLoop("silk_touch");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    
    public PromethiumPickaxeItem(ToolMaterial toolMaterial, TagKey<Block> effectiveBlocks, Settings settings) {
        super(toolMaterial, effectiveBlocks, settings);
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
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
    public boolean postMine(ItemStack stack, World world, BlockState state, BlockPos pos, LivingEntity miner) {
        if (!world.isClient && stack.contains(DataComponentTypes.INTANGIBLE_PROJECTILE)) {
            var enchantments = stack.getEnchantments();
            var builder = new ItemEnchantmentsComponent.Builder(enchantments);
            builder.remove(elem -> elem.matchesKey(Enchantments.SILK_TOUCH));
            stack.set(DataComponentTypes.ENCHANTMENTS, builder.build());
        }
        
        return true;
    }
    
    private static boolean isAreaEnabled(ItemStack stack) {
        return stack.getOrDefault(ComponentContent.IS_AOE_ACTIVE, false);
    }
    
    private static void setAreaEnabled(ItemStack stack, boolean enabled) {
        stack.set(ComponentContent.IS_AOE_ACTIVE, enabled);
    }
    
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        
        if (!world.isClient && user.isSneaking()) {
            var stack = user.getStackInHand(hand);
            
            var wasArea = isAreaEnabled(stack);
            var isArea = !wasArea;
            setAreaEnabled(stack, isArea);
            
            user.sendMessage(isArea ? Text.translatable("message.oritech.tool_mode.area_effect") : Text.translatable("message.oritech.tool_mode.silk_touch"));
        }
        
        return super.use(world, user, hand);
    }
    
    // called as event in Oritech initializer
    // area mode: 
    // adds a temporary silk touch, which is then removed in the after break event
    public static boolean preMine(World world, PlayerEntity player, BlockPos pos, BlockState blockState, BlockEntity blockEntity) {
        
        var handStack = player.getMainHandStack();
        if (handStack == null || !handStack.isOf(ToolsContent.PROMETHIUM_PICKAXE)) return true;

        // break additional blocks in preMine (Block.onBreak) instead of postMine (Block.onBroken)
        // so that the block still exists when determining which face of the block the player was looking at
        if (isAreaEnabled(handStack) && !player.isInPose(EntityPose.CROUCHING)) {

            // use the block face and player look direction to determine which additional blocks to break
            List<Vec3i> breakPositions;
            var playerHit = player.raycast(player.getBlockInteractionRange(), 0.0F, false);
            if (playerHit instanceof BlockHitResult blockHit) {
                var blockSide = blockHit.getSide();

                if (blockSide == Direction.UP || blockSide == Direction.DOWN) {
                    var direction = player.getHorizontalFacing();
                    if (direction == Direction.NORTH || direction == Direction.SOUTH) {
                        breakPositions = List.of(new Vec3i(0, 0, 1), new Vec3i(0, 0, -1));
                    } else {
                        breakPositions = List.of(new Vec3i(1, 0, 0), new Vec3i(-1, 0, 0));
                    }
                } else {
                    breakPositions = List.of(new Vec3i(0, 1, 0), new Vec3i(0, -1, 0));
                }

                // break additional blocks
                for (var offset : breakPositions) {
                    var offsetPos = pos.add(offset);
                    var offsetState = world.getBlockState(offsetPos);
                    if (offsetState.isIn(TagContent.DRILL_MINEABLE)) {
                        var offsetEntity = world.getBlockEntity(offsetPos);
                        // drop stacks before breaking additional block, because world.breakBlock doesn't apply item enchantments if drop is enabled
                        // this will ONLY apply item enchantments that affect block drops, and will not apply enchants like vein mining
                        Block.dropStacks(offsetState, world, offsetPos, offsetEntity, player, handStack);
                        world.breakBlock(offsetPos, false, player);
                    }
                }
            }
        }
        
        if (!isAreaEnabled(handStack)) {
            
            // do silk touch
            var hasExistingSilkTouch = EnchantmentHelper.getEnchantments(handStack).getEnchantments().stream().anyMatch(elem -> elem.matchesKey(Enchantments.SILK_TOUCH));
            
            if (!hasExistingSilkTouch) {
                var registryEntry = world.getRegistryManager().get(RegistryKeys.ENCHANTMENT).getEntry(Enchantments.SILK_TOUCH).get();
                handStack.addEnchantment(registryEntry, 1);
                handStack.set(DataComponentTypes.INTANGIBLE_PROJECTILE, Unit.INSTANCE);
            }
        }
        
        
        return true;
    }
    
    @Override
    public AttributeModifiersComponent getAttributeModifiers() {
        return super.getAttributeModifiers()
                 .with(EntityAttributes.PLAYER_BLOCK_INTERACTION_RANGE, new EntityAttributeModifier(Oritech.id("pick_block_range"), 2, EntityAttributeModifier.Operation.ADD_VALUE), AttributeModifierSlot.MAINHAND)
                 .with(EntityAttributes.PLAYER_ENTITY_INTERACTION_RANGE, new EntityAttributeModifier(Oritech.id("pick_entity_range"), 2, EntityAttributeModifier.Operation.ADD_VALUE), AttributeModifierSlot.MAINHAND);
    }
    
    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        
        var area = isAreaEnabled(stack);
        
        tooltip.add((area ? Text.translatable("tooltip.oritech.tool_mode.area_range.area") :  Text.translatable("tooltip.oritech.tool_mode.area_range.single")).formatted(Formatting.GOLD));
        tooltip.add(Text.translatable("tooltip.oritech.promethium_pick").formatted(Formatting.DARK_GRAY));
        
    }
    
    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private PromethiumToolRenderer renderer;
            
            @Override
            public @Nullable BuiltinModelItemRenderer getGeoItemRenderer() {
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
    public void onHeldTick(ItemStack stack, PlayerEntity player, ClientWorld world) {
        
        if (world.getTime() % 20 != 0) return;
        
        var area = isAreaEnabled(stack);
        triggerAnim(player, GeoItem.getId(stack), "Pickaxe", area ? "area" : "silk");
        
    }
}
