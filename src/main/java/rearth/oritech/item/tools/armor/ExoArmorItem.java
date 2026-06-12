package rearth.oritech.item.tools.armor;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.object.PlayState;
import com.geckolib.renderer.GeoArmorRenderer;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import org.jspecify.annotations.NonNull;
import rearth.oritech.Oritech;
import rearth.oritech.client.renderers.ExosuitArmorRenderer;
import rearth.oritech.init.SoundContent;
import rearth.oritech.init.ToolsContent;
import rearth.oritech.item.tools.util.ArmorEventHandler;

import java.util.function.Consumer;

public class ExoArmorItem extends Item implements GeoItem, ArmorEventHandler {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ArmorType type;

    public ExoArmorItem(ArmorMaterial material, ArmorType type, Properties settings) {
        super(settings);
        this.type = type;
    }

    public EquipmentSlot getEquipmentSlot() {
        return this.type.getSlot();
    }

    @Override
    public int getDefaultMaxStackSize() {
        return 1;
    }


    @Override
    public boolean isBarVisible(ItemStack stack) {
        return false;
    }

    @Override
    public void onEquipped(Player playerEntity, ItemStack stack) {

        if (this.getEquipmentSlot() == EquipmentSlot.HEAD)
            playerEntity.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false, false));
    }

    @Override
    public void onUnequipped(Player playerEntity, ItemStack stack) {

        if (this.getEquipmentSlot() == EquipmentSlot.HEAD)
            playerEntity.removeEffect(MobEffects.NIGHT_VISION);
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private ExosuitArmorRenderer renderer;

            @Override
            public @NonNull GeoArmorRenderer<?, ?> getGeoArmorRenderer(ItemStack itemStack, EquipmentSlot equipmentSlot) {
                if (this.renderer == null)
                    this.renderer = new ExosuitArmorRenderer(getModel(), Oritech.id("armor/exo_armor"));

                return this.renderer;
            }
        });
    }

    public Identifier getModel() {
        return Oritech.id("armor/exo_armor");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("base_controller", 20, state -> PlayState.STOP));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void appendHoverText(ItemStack itemStack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag) {
        super.appendHoverText(itemStack, context, display, builder, tooltipFlag);
        builder.accept(Component.translatable("tooltip.oritech." + BuiltInRegistries.ITEM.getKey(itemStack.getItem()).getPath()).withStyle(ChatFormatting.GRAY));
    }

    public static boolean CancelFallDamage(DamageSource source, DamageContainer container, LivingEntity entity) {
        if (source.is(DamageTypes.FALL) && entity.getItemBySlot(EquipmentSlot.FEET).is(ToolsContent.EXO_BOOTS)) {
            entity.level().playSound(null, entity.blockPosition(), SoundContent.SHORT_SERVO.value(), SoundSource.PLAYERS, 0.2f, 1.0f);
            return true;
        }

        return false;
    }
}
