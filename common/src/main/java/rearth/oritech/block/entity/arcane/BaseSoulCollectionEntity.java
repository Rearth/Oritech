package rearth.oritech.block.entity.arcane;

import java.util.HashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.phys.Vec3;

public abstract class BaseSoulCollectionEntity extends BlockEntity implements GameEventListener.Provider<BaseSoulCollectionEntity.DeathListener> {
    
    private final DeathListener deathListener;
    
    public BaseSoulCollectionEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        deathListener = new DeathListener(pos);
    }
    
    @Override
    public DeathListener getListener() {
        return deathListener;
    }
    
    public static float getSoulTravelDuration(float distance) {
        return (float) (Math.sqrt(distance * 20) * 3);
    }
    
    public abstract boolean canAcceptSoul();
    public abstract void onSoulIncoming(Vec3 emitter);
    
    public class DeathListener implements GameEventListener {
        
        private final PositionSource position;
        
        private static final HashSet<Vec3> consumedEvents = new HashSet<>();
        
        public static void resetEvents() {
            consumedEvents.clear();
        }
        
        public DeathListener(BlockPos pos) {
            this.position = new BlockPositionSource(pos);
        }
        
        @Override
        public PositionSource getListenerSource() {
            return position;
        }
        
        @Override
        public int getListenerRadius() {
            return 23;
        }
        
        @Override
        public DeliveryMode getDeliveryMode() {
            return DeliveryMode.BY_DISTANCE;
        }
        
        @Override
        public boolean handleGameEvent(ServerLevel world, Holder<GameEvent> event, GameEvent.Context emitter, Vec3 emitterPos) {
            if (event.is(GameEvent.ENTITY_DIE.key()) && canAcceptSoul() && !consumedEvents.contains(emitterPos)) {
                onSoulIncoming(emitterPos);
                consumedEvents.add(emitterPos);
                return true;
            }
            
            return false;
        }
    }
}
