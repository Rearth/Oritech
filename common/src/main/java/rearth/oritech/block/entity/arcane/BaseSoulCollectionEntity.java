package rearth.oritech.block.entity.arcane;

import java.util.HashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

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
            if (event.is(GameEvent.ENTITY_DIE.key()) && isValidEntity(emitter.sourceEntity()) && canAcceptSoul() && !consumedEvents.contains(emitterPos)) {
                onSoulIncoming(emitterPos);
                consumedEvents.add(emitterPos);
                return true;
            }
            
            return false;
        }

        private boolean isValidEntity(@Nullable Entity entity) {
            // We allow null entities as the Soul Flower triggers a ENTITY_DIE game event with a null entity
            return entity == null || entity instanceof LivingEntity;
        }
    }
}
