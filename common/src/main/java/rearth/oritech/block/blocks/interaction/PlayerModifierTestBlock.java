package rearth.oritech.block.blocks.interaction;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import rearth.oritech.Oritech;

@SuppressWarnings("DataFlowIssue")
public class PlayerModifierTestBlock extends Block {
    public PlayerModifierTestBlock(Settings settings) {
        super(settings);
    }
    
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        
        if (world.isClient)
            return ActionResult.SUCCESS;
        
        System.out.println(player.getMaxHealth());
        
        player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).overwritePersistentModifier(new EntityAttributeModifier(Oritech.id("healthboost"), 10, EntityAttributeModifier.Operation.ADD_VALUE));
        player.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).overwritePersistentModifier(new EntityAttributeModifier(Oritech.id("damage"), 10, EntityAttributeModifier.Operation.ADD_VALUE));
        
        return ActionResult.SUCCESS;
    }
}
