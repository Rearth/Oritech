package rearth.oritech.block.entity.arcane;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.event.BlockPositionSource;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.PositionSource;
import net.minecraft.world.event.listener.GameEventListener;

import java.util.HashSet;

public abstract class BaseSoulCollectionEntity extends BlockEntity implements GameEventListener.Holder<BaseSoulCollectionEntity.DeathListener> {
    
    private final DeathListener deathListener;
    
    public BaseSoulCollectionEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        deathListener = new DeathListener(pos);
    }
    
    @Override
    public DeathListener getEventListener() {
        return deathListener;
    }
    
    public static float getSoulTravelDuration(float distance) {
        return (float) (Math.sqrt(distance * 20) * 3);
    }
    
    public abstract boolean canAcceptSoul();
    public abstract void onSoulIncoming(Vec3d emitter);
    
    public class DeathListener implements GameEventListener {
        
        private final PositionSource position;
        
        private static final HashSet<Vec3d> consumedEvents = new HashSet<>();
        
        public static void resetEvents() {
            consumedEvents.clear();
        }
        
        public DeathListener(BlockPos pos) {
            this.position = new BlockPositionSource(pos);
        }
        
        @Override
        public PositionSource getPositionSource() {
            return position;
        }
        
        @Override
        public int getRange() {
            return 23;
        }
        
        @Override
        public TriggerOrder getTriggerOrder() {
            return TriggerOrder.BY_DISTANCE;
        }
        
        @Override
        public boolean listen(ServerWorld world, RegistryEntry<GameEvent> event, GameEvent.Emitter emitter, Vec3d emitterPos) {
            if (event.matchesKey(GameEvent.ENTITY_DIE.registryKey()) && canAcceptSoul() && !consumedEvents.contains(emitterPos)) {
                onSoulIncoming(emitterPos);
                consumedEvents.add(emitterPos);
                return true;
            }
            
            return false;
        }
    }
}
