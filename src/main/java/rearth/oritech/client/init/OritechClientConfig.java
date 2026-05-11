package rearth.oritech.client.init;

import net.neoforged.neoforge.common.ModConfigSpec;

public class OritechClientConfig {
    // Client config
    private static final ModConfigSpec.Builder CLIENT = new ModConfigSpec.Builder();
    
    public static final ModConfigSpec.BooleanValue showMachinePreview = OritechClientConfig.CLIENT
                                                                          .comment("Render multiblock placement preview")
                                                                          .define("showMachinePreview", true);
    
    public static final ModConfigSpec.BooleanValue enableHelpButton = OritechClientConfig.CLIENT
                                                                        .comment("Enable help button in machine UIs")
                                                                        .define("enableHelpButton", true);
    
    public static final ModConfigSpec.DoubleValue maxZiplineSpeed = OritechClientConfig.CLIENT
                                                                      .comment("Maximum zipline speed in blocks/second")
                                                                      .defineInRange("maxZiplineSpeed", 8.0, 0.0, 100.0);
    
    public static final ModConfigSpec.DoubleValue ziplineAcceleration = OritechClientConfig.CLIENT
                                                                          .comment("Zipline acceleration, higher = faster")
                                                                          .defineInRange("ziplineAcceleration", 0.1, 0.0, 10.0);
    
    public static final ModConfigSpec.BooleanValue ziplineAutoJump = OritechClientConfig.CLIENT
                                                                       .comment("Enable auto jump at zipline end")
                                                                       .define("ziplineAutoJump", true);
    
    public static final ModConfigSpec.BooleanValue ziplineCameraSwitch = OritechClientConfig.CLIENT
                                                                           .comment("Enable 3rd person camera while ziplining")
                                                                           .define("ziplineCameraSwitch", true);
    
    
    public static final ModConfigSpec CLIENT_SPEC = OritechClientConfig.CLIENT.build();
}
