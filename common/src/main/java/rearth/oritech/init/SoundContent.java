package rearth.oritech.init;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import rearth.oritech.Oritech;
import rearth.oritech.util.registry.ArchitecturyRegistryContainer;

public class SoundContent implements ArchitecturyRegistryContainer<SoundEvent> {
    
    public static final SoundEvent CABLE_MOVING = SoundEvent.createVariableRangeEvent(Oritech.id("cable_moving"));
    public static final SoundEvent SERVO_NOISES = SoundEvent.createVariableRangeEvent(Oritech.id("servo_noises"));
    public static final SoundEvent MECHANICAL_CLICK = SoundEvent.createVariableRangeEvent(Oritech.id("mechanical_click"));
    public static final SoundEvent WELDING1 = SoundEvent.createVariableRangeEvent(Oritech.id("welding1"));
    public static final SoundEvent WELDING2 = SoundEvent.createVariableRangeEvent(Oritech.id("welding2"));
    public static final SoundEvent SAW_WORKING = SoundEvent.createVariableRangeEvent(Oritech.id("saw_working"));
    public static final SoundEvent DYNAMO = SoundEvent.createVariableRangeEvent(Oritech.id("dynamo"));
    public static final SoundEvent FURNACE_BURN = SoundEvent.createVariableRangeEvent(Oritech.id("furnace_burn"));
    public static final SoundEvent GRINDER_WORKING = SoundEvent.createVariableRangeEvent(Oritech.id("grinder_working"));
    public static final SoundEvent LAVA_BUBBLES = SoundEvent.createVariableRangeEvent(Oritech.id("lava_bubbles"));
    public static final SoundEvent LIQUID_FLOW = SoundEvent.createVariableRangeEvent(Oritech.id("liquid_flow"));
    public static final SoundEvent QUADCOPTER_FLYING = SoundEvent.createVariableRangeEvent(Oritech.id("quadcopter_flying"));
    public static final SoundEvent QUADCOPTER_TAKEOFF = SoundEvent.createVariableRangeEvent(Oritech.id("quadcopter_takeoff"));
    public static final SoundEvent SIZZLING_SOUND = SoundEvent.createVariableRangeEvent(Oritech.id("sizzling_sound"));
    public static final SoundEvent SQUISH = SoundEvent.createVariableRangeEvent(Oritech.id("squish"));
    public static final SoundEvent PRESS = SoundEvent.createVariableRangeEvent(Oritech.id("press"));
    public static final SoundEvent PARTICLE_MOVING = SoundEvent.createVariableRangeEvent(Oritech.id("particle_moving"));
    public static final SoundEvent SHORT_SERVO = SoundEvent.createVariableRangeEvent(Oritech.id("short_servo"));
    public static final SoundEvent WRENCH_TURN = SoundEvent.createVariableRangeEvent(Oritech.id("wrench_turn"));
    public static final SoundEvent REACTOR = SoundEvent.createVariableRangeEvent(Oritech.id("reactor"));
    public static final SoundEvent REACTOR_LOADING = SoundEvent.createVariableRangeEvent(Oritech.id("reactor_loading"));
    public static final SoundEvent REACTOR_WARNING = SoundEvent.createVariableRangeEvent(Oritech.id("reactor_warning"));
    public static final SoundEvent NUKE_EXPLOSION = SoundEvent.createVariableRangeEvent(Oritech.id("nuke_explosion"));
    public static final SoundEvent ELECTRIC_SHOCK = SoundEvent.createVariableRangeEvent(Oritech.id("electric_shock"));
    public static final SoundEvent BEDROCK_EXTRACTOR = SoundEvent.createVariableRangeEvent(Oritech.id("bedrock_extractor"));
    
    @Override
    public Class<SoundEvent> getTargetFieldType() {
        return SoundEvent.class;
    }
    
    @Override
    public ResourceKey<Registry<SoundEvent>> getRegistryType() {
        return Registries.SOUND_EVENT;
    }
}
