package rearth.oritech.item.tools.harvesting;

import net.fabricmc.fabric.api.item.v1.EnchantingContext;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MiningToolItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.Unit;
import net.minecraft.util.math.BlockPos;
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
    
    public PromethiumPickaxeItem(ToolMaterial material, TagKey<Block> effectiveBlocks, Settings settings) {
        super(material, effectiveBlocks, settings);
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
    public boolean canBeEnchantedWith(ItemStack stack, RegistryEntry<Enchantment> enchantment, EnchantingContext context) {
        return super.canBeEnchantedWith(stack, enchantment, context) && !enchantment.getKey().get().equals(Enchantments.SILK_TOUCH);
    }
    
    @Override
    public boolean postMine(ItemStack stack, World world, BlockState state, BlockPos pos, LivingEntity miner) {
        
        if (!world.isClient && miner instanceof PlayerEntity player) {
            if (isAreaEnabled(stack)) {
                var breakPositions = List.of(new Vec3i(0, 1, 0), new Vec3i(0, -1, 0));
                for (var offset : breakPositions) {
                    var worldPos = pos.add(offset);
                    var worldState = world.getBlockState(worldPos);
                    if (canMine(worldState, world, worldPos, player) && worldState.isIn(TagContent.DRILL_MINEABLE)) {
                        world.breakBlock(worldPos, true, player);
                    }
                }
            } else if (stack.contains(DataComponentTypes.INTANGIBLE_PROJECTILE)) {
                var enchantments = stack.getEnchantments();
                var builder = new ItemEnchantmentsComponent.Builder(enchantments);
                builder.remove(elem -> elem.matchesKey(Enchantments.SILK_TOUCH));
                stack.set(DataComponentTypes.ENCHANTMENTS, builder.build());
            }
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
            
            user.sendMessage(Text.literal(isArea ? "Area Effect" : "Silk Touch"));
        }
        
        return super.use(world, user, hand);
    }
    
    // called as event in Oritech initializer
    // adds a temporary silk touch, which is then removed in the after break event
    public static boolean preMine(World world, PlayerEntity player, BlockPos pos, BlockState blockState, BlockEntity blockEntity) {
        
        var stack = player.getStackInHand(Hand.MAIN_HAND);
        if (stack != null && stack.getItem().equals(ToolsContent.PROMETHIUM_PICKAXE) && !isAreaEnabled(stack)) {
            
            // do silk touch
            var hasExistingSilkTouch = EnchantmentHelper.getEnchantments(stack).getEnchantments().stream().anyMatch(elem -> elem.matchesKey(Enchantments.SILK_TOUCH));
            
            if (!hasExistingSilkTouch) {
                var registryEntry = world.getRegistryManager().get(RegistryKeys.ENCHANTMENT).getEntry(Enchantments.SILK_TOUCH).get();
                stack.addEnchantment(registryEntry, 1);
                stack.set(DataComponentTypes.INTANGIBLE_PROJECTILE, Unit.INSTANCE);
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
        
        tooltip.add(Text.literal("Mode: " + (area ? "Area" : "Single")).formatted(Formatting.GOLD));
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
