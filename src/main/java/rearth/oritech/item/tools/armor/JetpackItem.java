package rearth.oritech.item.tools.armor;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.client.renderers.ExosuitArmorRenderer;
import rearth.oritech.client.renderers.LaserArmRenderer;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

public class JetpackItem extends ArmorItem implements GeoItem {
    
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    
    public JetpackItem(RegistryEntry<ArmorMaterial> material, Type type, Settings settings) {
        super(material, type, settings);
    }
    
    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        
        if (!world.isClient) return;
        
        if (!(entity instanceof PlayerEntity player)) return;
        
        var isEquipped = player.getEquippedStack(EquipmentSlot.CHEST).equals(stack);
        if (!isEquipped) return;
        
        var client = MinecraftClient.getInstance();
        
        var up = client.options.jumpKey.isPressed();
        var forward = client.options.forwardKey.isPressed();
        var backward = client.options.backKey.isPressed();
        var left = client.options.leftKey.isPressed();
        var right = client.options.rightKey.isPressed();
        
        var upOnly = up && !(forward || backward || left || right);
        
        var powerMultiplier = 2f;
        
        // TODO powered elytra?
        if (up) {
            var velocity = player.getMovement();
            
            var verticalMultiplier = LaserArmRenderer.lerp(powerMultiplier, 1, 0.3f);
            var power = 0.17f * verticalMultiplier;
            var dampeningFactor = 1.6f;
            
            if (!upOnly) power *= 0.7f;
            
            var speed = Math.max(velocity.y, 0.85);
            var addedVelocity = power / Math.pow(speed, dampeningFactor);
            
            player.setVelocity(velocity.add(0, addedVelocity, 0));
        }
        
        if ((forward || backward) && !player.isOnGround() && up) {
            var modifier = forward ? 1f : -0.4;  // either go full speed ahead, or slowly backwards
            var power = 0.1f * powerMultiplier;
            
            // get existing movement
            var movement = player.getMovement();
            var horizontalMovement = new Vec3d(movement.x, 0, movement.z);
            
            // get player facing
            var playerForward = player.getRotationVecClient();
            playerForward = new Vec3d(playerForward.x, 0, playerForward.z).normalize();
            
            // apply forward / back
            horizontalMovement = horizontalMovement.add(playerForward.multiply(modifier * power));
            
            player.setVelocity(horizontalMovement.x, movement.y, horizontalMovement.z);
        }
        
        if ((left || right) && !player.isOnGround() && up) {
            var modifier = right ? 1 : -1;  // either go full speed ahead, or slowly backwards
            var power = 0.07f * powerMultiplier;
            
            // get existing movement
            var movement = player.getMovement();
            var horizontalMovement = new Vec3d(movement.x, 0, movement.z);
            
            // get player facing
            var playerForward = player.getRotationVecClient();
            playerForward = new Vec3d(playerForward.x, 0, playerForward.z).normalize();
            var playerRight = playerForward.rotateY(-90);
            
            // apply forward / back
            horizontalMovement = horizontalMovement.add(playerRight.multiply(modifier * power));
            
            player.setVelocity(horizontalMovement.x, movement.y, horizontalMovement.z);
        }
        
    }
    
    @Override
    public boolean canRepair(ItemStack stack, ItemStack ingredient) {
        return false;
    }
    
    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        return false;
    }
    
    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private GeoArmorRenderer<?> renderer;
            
            @Override
            public @Nullable <T extends LivingEntity> BipedEntityModel<?> getGeoArmorRenderer(@Nullable T livingEntity, ItemStack itemStack, @Nullable EquipmentSlot equipmentSlot, @Nullable BipedEntityModel<T> original) {
                
                if (this.renderer == null)
                    this.renderer = new ExosuitArmorRenderer();
                
                return this.renderer;
            }
        });
    }
    
    // Let's add our animation controller
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, 20, state -> PlayState.STOP));
    }
    
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
