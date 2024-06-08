package rearth.oritech.init;

import io.wispforest.owo.registration.reflect.AutoRegistryContainer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import rearth.oritech.Oritech;

public class SoundContent implements AutoRegistryContainer<SoundEvent> {
    
    public static final SoundEvent CABLE_MOVING = SoundEvent.of(new Identifier(Oritech.MOD_ID, "cable_moving"));
    public static final SoundEvent SERVO_NOISES = SoundEvent.of(new Identifier(Oritech.MOD_ID, "servo_noises"));
    public static final SoundEvent MECHANICAL_CLICK = SoundEvent.of(new Identifier(Oritech.MOD_ID, "mechanical_click"));
    public static final SoundEvent WELDING1 = SoundEvent.of(new Identifier(Oritech.MOD_ID, "welding1"));
    public static final SoundEvent WELDING2 = SoundEvent.of(new Identifier(Oritech.MOD_ID, "welding2"));
    public static final SoundEvent SAW_WORKING = SoundEvent.of(new Identifier(Oritech.MOD_ID, "saw_working"));
    public static final SoundEvent DYNAMO = SoundEvent.of(new Identifier(Oritech.MOD_ID, "dynamo"));
    public static final SoundEvent FURNACE_BURN = SoundEvent.of(new Identifier(Oritech.MOD_ID, "furnace_burn"));
    public static final SoundEvent GRINDER_WORKING = SoundEvent.of(new Identifier(Oritech.MOD_ID, "grinder_working"));
    public static final SoundEvent LAVA_BUBBLES = SoundEvent.of(new Identifier(Oritech.MOD_ID, "lava_bubbles"));
    public static final SoundEvent LIQUID_FLOW = SoundEvent.of(new Identifier(Oritech.MOD_ID, "liquid_flow"));
    public static final SoundEvent QUADCOPTER_FLYING = SoundEvent.of(new Identifier(Oritech.MOD_ID, "quadcopter_flying"));
    public static final SoundEvent QUADCOPTER_TAKEOFF = SoundEvent.of(new Identifier(Oritech.MOD_ID, "quadcopter_takeoff"));
    public static final SoundEvent SIZZLING_SOUND = SoundEvent.of(new Identifier(Oritech.MOD_ID, "sizzling_sound"));
    public static final SoundEvent SQUISH = SoundEvent.of(new Identifier(Oritech.MOD_ID, "squish"));
    
    @Override
    public Registry<SoundEvent> getRegistry() {
        return Registries.SOUND_EVENT;
    }
    
    @Override
    public Class<SoundEvent> getTargetFieldType() {
        return SoundEvent.class;
    }
}
