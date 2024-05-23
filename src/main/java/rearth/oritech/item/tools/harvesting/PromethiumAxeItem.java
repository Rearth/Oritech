package rearth.oritech.item.tools.harvesting;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ToolMaterial;
import rearth.oritech.client.renderers.PromethiumToolRenderer;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.RenderProvider;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class PromethiumAxeItem extends AxeItem implements GeoItem {
    
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Supplier<Object> renderProvider = GeoItem.makeRenderer(this);
    
    public PromethiumAxeItem(ToolMaterial material, float attackDamage, float attackSpeed) {
        super(material, attackDamage, attackSpeed, new Settings().maxDamage(-1).maxCount(1));
    }
    
    @Override
    public boolean isSuitableFor(BlockState state) {
        return Items.DIAMOND_AXE.isSuitableFor(state)
                 || Items.DIAMOND_SWORD.isSuitableFor(state)
                 || Items.SHEARS.isSuitableFor(state);
    }
    
    @Override
    public boolean canRepair(ItemStack stack, ItemStack ingredient) {
        return false;
    }
    
    @Override
    public boolean isDamageable() {
        return false;
    }
    
    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }
    
    @Override
    public void createRenderer(Consumer<Object> consumer) {
        consumer.accept(new RenderProvider() {
            private PromethiumToolRenderer renderer;
            
            @Override
            public BuiltinModelItemRenderer getCustomRenderer() {
                if (this.renderer == null)
                    this.renderer = new PromethiumToolRenderer("promethium_axe");
                return renderer;
            }
        });
    }
    
    @Override
    public Supplier<Object> getRenderProvider() {
        return renderProvider;
    }
    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    
    }
    
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
