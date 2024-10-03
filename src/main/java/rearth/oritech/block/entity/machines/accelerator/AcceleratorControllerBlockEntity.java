package rearth.oritech.block.entity.machines.accelerator;

import io.wispforest.owo.util.VectorRandomUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.network.NetworkContent;
import rearth.oritech.util.Geometry;

import java.util.List;

public class AcceleratorControllerBlockEntity extends BlockEntity implements BlockEntityTicker<AcceleratorControllerBlockEntity> {
    
    private AcceleratorParticleLogic.ActiveParticle particle;
    private AcceleratorParticleLogic particleLogic;
    
    // client data
    public List<Vec3d> displayTrail;
    
    public AcceleratorControllerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.ACCELERATOR_CONTROLLER_BLOCK_ENTITY, pos, state);
    }
    
    @Override
    public void tick(World world, BlockPos pos, BlockState state, AcceleratorControllerBlockEntity blockEntity) {
        if (world.isClient) return;
        initParticleLogic();
        
        if (particle != null)
            particleLogic.update(particle);
    }
    
    private void initParticleLogic() {
        if (particleLogic == null) particleLogic = new AcceleratorParticleLogic(pos, (ServerWorld) world, this);
    }
    
    public void injectParticle() {
        
        var facing = getCachedState().get(Properties.HORIZONTAL_FACING);
        var posBehind = Geometry.offsetToWorldPosition(facing, new Vec3i(1, 0, 0), pos);
        var directionRight = Geometry.getRight(facing);
        
        var candidateBlock = world.getBlockState(new BlockPos(posBehind));
        if (candidateBlock.getBlock().equals(BlockContent.ACCELERATOR_RING)) {
            var startPosition = (BlockPos) posBehind;
            var nextGate = particleLogic.findNextGate(startPosition, directionRight, 1);
            particle = new AcceleratorParticleLogic.ActiveParticle(startPosition.toCenterPos(), 1, nextGate, startPosition);
        }
    }
    
    public void removeParticleDueToCollision() {
        this.particle = null;
    }
    
    public void onParticleExited(Vec3d from, Vec3d to, BlockPos lastGate, Vec3d exitDirection) {
        this.particle = null;
        
        var renderedTrail = List.of(from, to);
        NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new NetworkContent.AcceleratorParticleRenderPacket(pos, renderedTrail));
    }
    
    public void onParticleCollided(float relativeSpeed, Vec3d collision, BlockPos secondController, AcceleratorControllerBlockEntity secondControllerEntity) {
        
        particle = null;
        secondControllerEntity.removeParticleDueToCollision();
        
        var particleCount = Math.max(Math.sqrt(Math.sqrt(relativeSpeed)), 3);
        for (int i = 0; i < particleCount + 5; i++) {
            var offset = VectorRandomUtils.getRandomOffset(world, collision, particleCount * 1.5);
            ParticleContent.WEED_KILLER.spawn(world, collision, new ParticleContent.LineData(collision, offset));
        }
        
        ParticleContent.PARTICLE_COLLIDE.spawn(world, collision);
    }
    
    public void onParticleMoved(List<Vec3d> positions) {
        if (positions.size() > 1) {
            NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new NetworkContent.AcceleratorParticleRenderPacket(pos, positions));
        }
    }
    
    public AcceleratorParticleLogic.ActiveParticle getParticle() {
        return particle;
    }
    
    public void setDisplayTrail(List<Vec3d> displayTrail) {
        this.displayTrail = displayTrail;
    }
    
    // returns the amount of moment used
    public float handleParticleEntityCollision(BlockPos checkPos, AcceleratorParticleLogic.ActiveParticle particle, float remainingMomentum, LivingEntity mob) {
        
        var maxApplicableDamage = mob.getHealth();
        var inflictedDamage = Math.min(remainingMomentum, maxApplicableDamage);
        mob.damage(world.getDamageSources().magic(), remainingMomentum);
        var position = mob.getBoundingBox().getCenter();
        position = new Vec3d(position.x, particle.position.y, position.z);
        ParticleContent.BIG_HIT.spawn(world, position);
        
        return inflictedDamage;
    }
    
    public float handleParticleBlockCollision(BlockPos checkPos, AcceleratorParticleLogic.ActiveParticle particle, float remainingMomentum, BlockState hitState) {
        
        var blockHardness = hitState.getHardness(world, checkPos);
        if (blockHardness < 0)  // unbreakable block
            return remainingMomentum;
        
        if (remainingMomentum > blockHardness) {
            world.addBlockBreakParticles(checkPos, hitState);
            world.playSound(null, checkPos, hitState.getSoundGroup().getBreakSound(), SoundCategory.BLOCKS, 1f, 1f);
            world.breakBlock(checkPos, true);
        }
        
        return blockHardness;
    }
}
