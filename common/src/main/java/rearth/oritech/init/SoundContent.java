package rearth.oritech.init;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.SoundEvent;
import rearth.oritech.Oritech;
import rearth.oritech.util.ArchitecturyRegistryContainer;

public class SoundContent implements ArchitecturyRegistryContainer<SoundEvent> {
    
    public static final SoundEvent CABLE_MOVING = SoundEvent.of(Oritech.id("cable_moving"));
    public static final SoundEvent SERVO_NOISES = SoundEvent.of(Oritech.id("servo_noises"));
    public static final SoundEvent MECHANICAL_CLICK = SoundEvent.of(Oritech.id("mechanical_click"));
    public static final SoundEvent WELDING1 = SoundEvent.of(Oritech.id("welding1"));
    public static final SoundEvent WELDING2 = SoundEvent.of(Oritech.id("welding2"));
    public static final SoundEvent SAW_WORKING = SoundEvent.of(Oritech.id("saw_working"));
    public static final SoundEvent DYNAMO = SoundEvent.of(Oritech.id("dynamo"));
    public static final SoundEvent FURNACE_BURN = SoundEvent.of(Oritech.id("furnace_burn"));
    public static final SoundEvent GRINDER_WORKING = SoundEvent.of(Oritech.id("grinder_working"));
    public static final SoundEvent LAVA_BUBBLES = SoundEvent.of(Oritech.id("lava_bubbles"));
    public static final SoundEvent LIQUID_FLOW = SoundEvent.of(Oritech.id("liquid_flow"));
    public static final SoundEvent QUADCOPTER_FLYING = SoundEvent.of(Oritech.id("quadcopter_flying"));
    public static final SoundEvent QUADCOPTER_TAKEOFF = SoundEvent.of(Oritech.id("quadcopter_takeoff"));
    public static final SoundEvent SIZZLING_SOUND = SoundEvent.of(Oritech.id("sizzling_sound"));
    public static final SoundEvent SQUISH = SoundEvent.of(Oritech.id("squish"));
    public static final SoundEvent PRESS = SoundEvent.of(Oritech.id("press"));
    public static final SoundEvent PARTICLE_MOVING = SoundEvent.of(Oritech.id("particle_moving"));
    public static final SoundEvent SHORT_SERVO = SoundEvent.of(Oritech.id("short_servo"));
    public static final SoundEvent WRENCH_TURN = SoundEvent.of(Oritech.id("wrench_turn"));
    
    @Override
    public Class<SoundEvent> getTargetFieldType() {
        return SoundEvent.class;
    }
    
    @Override
    public RegistryKey<Registry<SoundEvent>> getRegistryType() {
        return RegistryKeys.SOUND_EVENT;
    }
}
