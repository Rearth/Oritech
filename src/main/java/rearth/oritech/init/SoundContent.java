package rearth.oritech.init;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import rearth.oritech.util.registry.OritechDeferredRegistry;

public class SoundContent {
    
    public static final OritechDeferredRegistry<SoundEvent> SOUNDS = OritechDeferredRegistry.create(Registries.SOUND_EVENT);
    
    public static final RegistrySupplier<SoundEvent> CABLE_MOVING = SOUNDS.register("cable_moving", () -> SoundEvent.createVariableRangeEvent(SOUNDS.id("cable_moving")));
    public static final RegistrySupplier<SoundEvent> SERVO_NOISES = SOUNDS.register("servo_noises", () -> SoundEvent.createVariableRangeEvent(SOUNDS.id("servo_noises")));
    public static final RegistrySupplier<SoundEvent> MECHANICAL_CLICK = SOUNDS.register("mechanical_click", () -> SoundEvent.createVariableRangeEvent(SOUNDS.id("mechanical_click")));
    public static final RegistrySupplier<SoundEvent> WELDING1 = SOUNDS.register("welding1", () -> SoundEvent.createVariableRangeEvent(SOUNDS.id("welding1")));
    public static final RegistrySupplier<SoundEvent> WELDING2 = SOUNDS.register("welding2", () -> SoundEvent.createVariableRangeEvent(SOUNDS.id("welding2")));
    public static final RegistrySupplier<SoundEvent> SAW_WORKING = SOUNDS.register("saw_working", () -> SoundEvent.createVariableRangeEvent(SOUNDS.id("saw_working")));
    public static final RegistrySupplier<SoundEvent> DYNAMO = SOUNDS.register("dynamo", () -> SoundEvent.createVariableRangeEvent(SOUNDS.id("dynamo")));
    public static final RegistrySupplier<SoundEvent> FURNACE_BURN = SOUNDS.register("furnace_burn", () -> SoundEvent.createVariableRangeEvent(SOUNDS.id("furnace_burn")));
    public static final RegistrySupplier<SoundEvent> GRINDER_WORKING = SOUNDS.register("grinder_working", () -> SoundEvent.createVariableRangeEvent(SOUNDS.id("grinder_working")));
    public static final RegistrySupplier<SoundEvent> LAVA_BUBBLES = SOUNDS.register("lava_bubbles", () -> SoundEvent.createVariableRangeEvent(SOUNDS.id("lava_bubbles")));
    public static final RegistrySupplier<SoundEvent> LIQUID_FLOW = SOUNDS.register("liquid_flow", () -> SoundEvent.createVariableRangeEvent(SOUNDS.id("liquid_flow")));
    public static final RegistrySupplier<SoundEvent> QUADCOPTER_FLYING = SOUNDS.register("quadcopter_flying", () -> SoundEvent.createVariableRangeEvent(SOUNDS.id("quadcopter_flying")));
    public static final RegistrySupplier<SoundEvent> QUADCOPTER_TAKEOFF = SOUNDS.register("quadcopter_takeoff", () -> SoundEvent.createVariableRangeEvent(SOUNDS.id("quadcopter_takeoff")));
    public static final RegistrySupplier<SoundEvent> SIZZLING_SOUND = SOUNDS.register("sizzling_sound", () -> SoundEvent.createVariableRangeEvent(SOUNDS.id("sizzling_sound")));
    public static final RegistrySupplier<SoundEvent> SQUISH = SOUNDS.register("squish", () -> SoundEvent.createVariableRangeEvent(SOUNDS.id("squish")));
    public static final RegistrySupplier<SoundEvent> PRESS = SOUNDS.register("press", () -> SoundEvent.createVariableRangeEvent(SOUNDS.id("press")));
    public static final RegistrySupplier<SoundEvent> PARTICLE_MOVING = SOUNDS.register("particle_moving", () -> SoundEvent.createVariableRangeEvent(SOUNDS.id("particle_moving")));
    public static final RegistrySupplier<SoundEvent> SHORT_SERVO = SOUNDS.register("short_servo", () -> SoundEvent.createVariableRangeEvent(SOUNDS.id("short_servo")));
    public static final RegistrySupplier<SoundEvent> WRENCH_TURN = SOUNDS.register("wrench_turn", () -> SoundEvent.createVariableRangeEvent(SOUNDS.id("wrench_turn")));
    public static final RegistrySupplier<SoundEvent> REACTOR = SOUNDS.register("reactor", () -> SoundEvent.createVariableRangeEvent(SOUNDS.id("reactor")));
    public static final RegistrySupplier<SoundEvent> REACTOR_LOADING = SOUNDS.register("reactor_loading", () -> SoundEvent.createVariableRangeEvent(SOUNDS.id("reactor_loading")));
    public static final RegistrySupplier<SoundEvent> REACTOR_WARNING = SOUNDS.register("reactor_warning", () -> SoundEvent.createVariableRangeEvent(SOUNDS.id("reactor_warning")));
    public static final RegistrySupplier<SoundEvent> NUKE_EXPLOSION = SOUNDS.register("nuke_explosion", () -> SoundEvent.createVariableRangeEvent(SOUNDS.id("nuke_explosion")));
    public static final RegistrySupplier<SoundEvent> ELECTRIC_SHOCK = SOUNDS.register("electric_shock", () -> SoundEvent.createVariableRangeEvent(SOUNDS.id("electric_shock")));
    public static final RegistrySupplier<SoundEvent> BEDROCK_EXTRACTOR = SOUNDS.register("bedrock_extractor", () -> SoundEvent.createVariableRangeEvent(SOUNDS.id("bedrock_extractor")));
    
    public static void register() {
        SOUNDS.register();
    }
}
