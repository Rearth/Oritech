package rearth.oritech.init.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementFrame;
import net.minecraft.advancement.criterion.InventoryChangedCriterion;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import rearth.oritech.Oritech;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.ItemContent;
import rearth.oritech.init.ToolsContent;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class AdvancementGenerator extends FabricAdvancementProvider {
    
    public AdvancementGenerator(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registryLookup) {
        super(output, registryLookup);
    }
    
    @Override
    public void generateAdvancement(RegistryWrapper.WrapperLookup registryLookup, Consumer<AdvancementEntry> consumer) {
        
        var rootAdvancement = Advancement.Builder.create()
                                .display(
                                  ItemContent.RAW_NICKEL, // The display icon
                                  Text.translatable("advancements.oritech.begin"), // The title
                                  Text.translatable("advancements.oritech.begin.description"), // The description
                                  Identifier.of("textures/gui/advancements/backgrounds/adventure.png"), // Background image used
                                  AdvancementFrame.TASK, // Options: TASK, CHALLENGE, GOAL
                                  true, // Show toast top right
                                  true, // Announce to chat
                                  false // Hidden in the advancement tab
                                )
                                // The first string used in criterion is the name referenced by other advancements when they want to have 'requirements'
                                .criterion("got_nickel", InventoryChangedCriterion.Conditions.items(ItemContent.RAW_NICKEL))
                                .build(consumer, Oritech.MOD_ID + "/root");
        
        
        var generatorAdvancement = Advancement.Builder.create().parent(rootAdvancement)
                                .display(
                                  BlockContent.BASIC_GENERATOR_BLOCK, // The display icon
                                  Text.translatable("advancements.oritech.generator"), // The title
                                  Text.translatable("advancements.oritech.generator.description"), // The description
                                  Identifier.of("textures/gui/advancements/backgrounds/adventure.png"), // Background image used
                                  AdvancementFrame.TASK, // Options: TASK, CHALLENGE, GOAL
                                  true, // Show toast top right
                                  true, // Announce to chat
                                  false // Hidden in the advancement tab
                                )
                                // The first string used in criterion is the name referenced by other advancements when they want to have 'requirements'
                                .criterion("got_generator", InventoryChangedCriterion.Conditions.items(BlockContent.BASIC_GENERATOR_BLOCK))
                                .build(consumer, Oritech.MOD_ID + "/generator");
        
        
        var furnaceAdvancement = Advancement.Builder.create().parent(generatorAdvancement)
                                .display(
                                  BlockContent.POWERED_FURNACE_BLOCK, // The display icon
                                  Text.translatable("advancements.oritech.furnace"), // The title
                                  Text.translatable("advancements.oritech.furnace.description"), // The description
                                  Identifier.of("textures/gui/advancements/backgrounds/adventure.png"), // Background image used
                                  AdvancementFrame.TASK, // Options: TASK, CHALLENGE, GOAL
                                  true, // Show toast top right
                                  true, // Announce to chat
                                  false // Hidden in the advancement tab
                                )
                                // The first string used in criterion is the name referenced by other advancements when they want to have 'requirements'
                                .criterion("got_furnace", InventoryChangedCriterion.Conditions.items(BlockContent.POWERED_FURNACE_BLOCK))
                                .build(consumer, Oritech.MOD_ID + "/furnace");
        
        
        var pulverizerAdvancement = Advancement.Builder.create().parent(generatorAdvancement)
                                .display(
                                  BlockContent.PULVERIZER_BLOCK, // The display icon
                                  Text.translatable("advancements.oritech.pulverizer"), // The title
                                  Text.translatable("advancements.oritech.pulverizer.description"), // The description
                                  Identifier.of("textures/gui/advancements/backgrounds/adventure.png"), // Background image used
                                  AdvancementFrame.TASK, // Options: TASK, CHALLENGE, GOAL
                                  true, // Show toast top right
                                  true, // Announce to chat
                                  false // Hidden in the advancement tab
                                )
                                // The first string used in criterion is the name referenced by other advancements when they want to have 'requirements'
                                .criterion("got_pulverizer", InventoryChangedCriterion.Conditions.items(BlockContent.PULVERIZER_BLOCK))
                                .build(consumer, Oritech.MOD_ID + "/pulverizer");
        
        
        var foundryAdvancement = Advancement.Builder.create().parent(generatorAdvancement)
                                .display(
                                  BlockContent.FOUNDRY_BLOCK, // The display icon
                                  Text.translatable("advancements.oritech.foundry"), // The title
                                  Text.translatable("advancements.oritech.foundry.description"), // The description
                                  Identifier.of("textures/gui/advancements/backgrounds/adventure.png"), // Background image used
                                  AdvancementFrame.TASK, // Options: TASK, CHALLENGE, GOAL
                                  true, // Show toast top right
                                  true, // Announce to chat
                                  false // Hidden in the advancement tab
                                )
                                // The first string used in criterion is the name referenced by other advancements when they want to have 'requirements'
                                .criterion("got_foundry", InventoryChangedCriterion.Conditions.items(BlockContent.FOUNDRY_BLOCK))
                                .build(consumer, Oritech.MOD_ID + "/foundry");
        
        
        var assemblerAdvancement = Advancement.Builder.create().parent(generatorAdvancement)
                                .display(
                                  BlockContent.ASSEMBLER_BLOCK, // The display icon
                                  Text.translatable("advancements.oritech.assembler"), // The title
                                  Text.translatable("advancements.oritech.assembler.description"), // The description
                                  Identifier.of("textures/gui/advancements/backgrounds/adventure.png"), // Background image used
                                  AdvancementFrame.TASK, // Options: TASK, CHALLENGE, GOAL
                                  true, // Show toast top right
                                  true, // Announce to chat
                                  false // Hidden in the advancement tab
                                )
                                // The first string used in criterion is the name referenced by other advancements when they want to have 'requirements'
                                .criterion("got_assembler", InventoryChangedCriterion.Conditions.items(BlockContent.ASSEMBLER_BLOCK))
                                .build(consumer, Oritech.MOD_ID + "/assembler");
        
        var exoBootsAdvancement = Advancement.Builder.create().parent(assemblerAdvancement)
                                   .display(
                                     ToolsContent.EXO_BOOTS, // The display icon
                                     Text.translatable("advancements.oritech.exo_boots"), // The title
                                     Text.translatable("advancements.oritech.exo_boots.description"), // The description
                                     Identifier.of("textures/gui/advancements/backgrounds/adventure.png"), // Background image used
                                     AdvancementFrame.GOAL, // Options: TASK, CHALLENGE, GOAL
                                     true, // Show toast top right
                                     true, // Announce to chat
                                     false // Hidden in the advancement tab
                                   )
                                   // The first string used in criterion is the name referenced by other advancements when they want to have 'requirements'
                                   .criterion("got_exo_boots", InventoryChangedCriterion.Conditions.items(ToolsContent.EXO_BOOTS))
                                   .build(consumer, Oritech.MOD_ID + "/exo_boots");
        
        var exoLegsAdvancement = Advancement.Builder.create().parent(assemblerAdvancement)
                                   .display(
                                     ToolsContent.EXO_LEGGINGS, // The display icon
                                     Text.translatable("advancements.oritech.exo_legs"), // The title
                                     Text.translatable("advancements.oritech.exo_legs.description"), // The description
                                     Identifier.of("textures/gui/advancements/backgrounds/adventure.png"), // Background image used
                                     AdvancementFrame.GOAL, // Options: TASK, CHALLENGE, GOAL
                                     true, // Show toast top right
                                     true, // Announce to chat
                                     false // Hidden in the advancement tab
                                   )
                                   // The first string used in criterion is the name referenced by other advancements when they want to have 'requirements'
                                   .criterion("got_exo_legs", InventoryChangedCriterion.Conditions.items(ToolsContent.EXO_LEGGINGS))
                                   .build(consumer, Oritech.MOD_ID + "/exo_legs");
        
        var exoChestAdvancement = Advancement.Builder.create().parent(assemblerAdvancement)
                                   .display(
                                     ToolsContent.EXO_CHESTPLATE, // The display icon
                                     Text.translatable("advancements.oritech.exo_chest"), // The title
                                     Text.translatable("advancements.oritech.exo_chest.description"), // The description
                                     Identifier.of("textures/gui/advancements/backgrounds/adventure.png"), // Background image used
                                     AdvancementFrame.GOAL, // Options: TASK, CHALLENGE, GOAL
                                     true, // Show toast top right
                                     true, // Announce to chat
                                     false // Hidden in the advancement tab
                                   )
                                   // The first string used in criterion is the name referenced by other advancements when they want to have 'requirements'
                                   .criterion("got_exo_chest", InventoryChangedCriterion.Conditions.items(ToolsContent.EXO_CHESTPLATE))
                                   .build(consumer, Oritech.MOD_ID + "/exo_chest");
        
        var exoHelmetAdvancement = Advancement.Builder.create().parent(assemblerAdvancement)
                                   .display(
                                     ToolsContent.EXO_HELMET, // The display icon
                                     Text.translatable("advancements.oritech.exo_helmet"), // The title
                                     Text.translatable("advancements.oritech.exo_helmet.description"), // The description
                                     Identifier.of("textures/gui/advancements/backgrounds/adventure.png"), // Background image used
                                     AdvancementFrame.GOAL, // Options: TASK, CHALLENGE, GOAL
                                     true, // Show toast top right
                                     true, // Announce to chat
                                     false // Hidden in the advancement tab
                                   )
                                   // The first string used in criterion is the name referenced by other advancements when they want to have 'requirements'
                                   .criterion("got_exo_helmet", InventoryChangedCriterion.Conditions.items(ToolsContent.EXO_HELMET))
                                   .build(consumer, Oritech.MOD_ID + "/exo_helmet");
        
        var drillAdvancement = Advancement.Builder.create().parent(assemblerAdvancement)
                                   .display(
                                     ToolsContent.HAND_DRILL, // The display icon
                                     Text.translatable("advancements.oritech.drill"), // The title
                                     Text.translatable("advancements.oritech.drill.description"), // The description
                                     Identifier.of("textures/gui/advancements/backgrounds/adventure.png"), // Background image used
                                     AdvancementFrame.GOAL, // Options: TASK, CHALLENGE, GOAL
                                     true, // Show toast top right
                                     true, // Announce to chat
                                     false // Hidden in the advancement tab
                                   )
                                   // The first string used in criterion is the name referenced by other advancements when they want to have 'requirements'
                                   .criterion("got_drill", InventoryChangedCriterion.Conditions.items(ToolsContent.HAND_DRILL))
                                   .build(consumer, Oritech.MOD_ID + "/drill");
        
        
        var centrifugeAdvancement = Advancement.Builder.create().parent(assemblerAdvancement)
                                .display(
                                  BlockContent.CENTRIFUGE_BLOCK, // The display icon
                                  Text.translatable("advancements.oritech.centrifuge"), // The title
                                  Text.translatable("advancements.oritech.centrifuge.description"), // The description
                                  Identifier.of("textures/gui/advancements/backgrounds/adventure.png"), // Background image used
                                  AdvancementFrame.TASK, // Options: TASK, CHALLENGE, GOAL
                                  true, // Show toast top right
                                  true, // Announce to chat
                                  false // Hidden in the advancement tab
                                )
                                // The first string used in criterion is the name referenced by other advancements when they want to have 'requirements'
                                .criterion("got_centrifuge", InventoryChangedCriterion.Conditions.items(BlockContent.CENTRIFUGE_BLOCK))
                                .build(consumer, Oritech.MOD_ID + "/centrifuge");
        
        
        var plasticAdvancement = Advancement.Builder.create().parent(centrifugeAdvancement)
                                .display(
                                  ItemContent.PLASTIC_SHEET, // The display icon
                                  Text.translatable("advancements.oritech.plastic"), // The title
                                  Text.translatable("advancements.oritech.plastic.description"), // The description
                                  Identifier.of("textures/gui/advancements/backgrounds/adventure.png"), // Background image used
                                  AdvancementFrame.GOAL, // Options: TASK, CHALLENGE, GOAL
                                  true, // Show toast top right
                                  true, // Announce to chat
                                  false // Hidden in the advancement tab
                                )
                                // The first string used in criterion is the name referenced by other advancements when they want to have 'requirements'
                                .criterion("got_plastic", InventoryChangedCriterion.Conditions.items(ItemContent.PLASTIC_SHEET))
                                .build(consumer, Oritech.MOD_ID + "/plastic");
        
        
        var laserAdvancement = Advancement.Builder.create().parent(centrifugeAdvancement)
                                .display(
                                  BlockContent.LASER_ARM_BLOCK, // The display icon
                                  Text.translatable("advancements.oritech.laser"), // The title
                                  Text.translatable("advancements.oritech.laser.description"), // The description
                                  Identifier.of("textures/gui/advancements/backgrounds/adventure.png"), // Background image used
                                  AdvancementFrame.TASK, // Options: TASK, CHALLENGE, GOAL
                                  true, // Show toast top right
                                  true, // Announce to chat
                                  false // Hidden in the advancement tab
                                )
                                // The first string used in criterion is the name referenced by other advancements when they want to have 'requirements'
                                .criterion("got_laser", InventoryChangedCriterion.Conditions.items(BlockContent.LASER_ARM_BLOCK))
                                .build(consumer, Oritech.MOD_ID + "/laser");
        
        
        var fluxiteAdvancement = Advancement.Builder.create().parent(laserAdvancement)
                                .display(
                                  ItemContent.FLUXITE, // The display icon
                                  Text.translatable("advancements.oritech.fluxite"), // The title
                                  Text.translatable("advancements.oritech.fluxite.description"), // The description
                                  Identifier.of("textures/gui/advancements/backgrounds/adventure.png"), // Background image used
                                  AdvancementFrame.GOAL, // Options: TASK, CHALLENGE, GOAL
                                  true, // Show toast top right
                                  true, // Announce to chat
                                  false // Hidden in the advancement tab
                                )
                                // The first string used in criterion is the name referenced by other advancements when they want to have 'requirements'
                                .criterion("got_fluxite", InventoryChangedCriterion.Conditions.items(ItemContent.FLUXITE))
                                .build(consumer, Oritech.MOD_ID + "/fluxite");
        
        
        var atomicForgeAdvancement = Advancement.Builder.create().parent(fluxiteAdvancement)
                                 .display(
                                   BlockContent.ATOMIC_FORGE_BLOCK, // The display icon
                                   Text.translatable("advancements.oritech.atomicforge"), // The title
                                   Text.translatable("advancements.oritech.atomicforge.description"), // The description
                                   Identifier.of("textures/gui/advancements/backgrounds/adventure.png"), // Background image used
                                   AdvancementFrame.TASK, // Options: TASK, CHALLENGE, GOAL
                                   true, // Show toast top right
                                   true, // Announce to chat
                                   false // Hidden in the advancement tab
                                 )
                                 // The first string used in criterion is the name referenced by other advancements when they want to have 'requirements'
                                 .criterion("got_atomic_forge", InventoryChangedCriterion.Conditions.items(BlockContent.ATOMIC_FORGE_BLOCK))
                                 .build(consumer, Oritech.MOD_ID + "/atomicforge");
        
        
        var promethiumAdvancement = Advancement.Builder.create().parent(atomicForgeAdvancement)
                                 .display(
                                   ItemContent.PROMETHEUM_INGOT, // The display icon
                                   Text.translatable("advancements.oritech.promethium"), // The title
                                   Text.translatable("advancements.oritech.promethium.description"), // The description
                                   Identifier.of("textures/gui/advancements/backgrounds/adventure.png"), // Background image used
                                   AdvancementFrame.CHALLENGE, // Options: TASK, CHALLENGE, GOAL
                                   true, // Show toast top right
                                   true, // Announce to chat
                                   false // Hidden in the advancement tab
                                 )
                                 // The first string used in criterion is the name referenced by other advancements when they want to have 'requirements'
                                 .criterion("got_promethium", InventoryChangedCriterion.Conditions.items(ItemContent.PROMETHEUM_INGOT))
                                 .build(consumer, Oritech.MOD_ID + "/promethium");
        
        
        var aiAdvancement = Advancement.Builder.create().parent(atomicForgeAdvancement)
                                 .display(
                                   ItemContent.SUPER_AI_CHIP, // The display icon
                                   Text.translatable("advancements.oritech.ai"), // The title
                                   Text.translatable("advancements.oritech.ai.description"), // The description
                                   Identifier.of("textures/gui/advancements/backgrounds/adventure.png"), // Background image used
                                   AdvancementFrame.GOAL, // Options: TASK, CHALLENGE, GOAL
                                   true, // Show toast top right
                                   true, // Announce to chat
                                   false // Hidden in the advancement tab
                                 )
                                 // The first string used in criterion is the name referenced by other advancements when they want to have 'requirements'
                                 .criterion("got_ai", InventoryChangedCriterion.Conditions.items(ItemContent.SUPER_AI_CHIP))
                                 .build(consumer, Oritech.MOD_ID + "/ai");
        
        
        var unholyAdvancement = Advancement.Builder.create().parent(assemblerAdvancement)
                                 .display(
                                   ItemContent.UNHOLY_INTELLIGENCE, // The display icon
                                   Text.translatable("advancements.oritech.unholy"), // The title
                                   Text.translatable("advancements.oritech.unholy.description"), // The description
                                   Identifier.of("textures/gui/advancements/backgrounds/adventure.png"), // Background image used
                                   AdvancementFrame.CHALLENGE, // Options: TASK, CHALLENGE, GOAL
                                   true, // Show toast top right
                                   true, // Announce to chat
                                   false // Hidden in the advancement tab
                                 )
                                 // The first string used in criterion is the name referenced by other advancements when they want to have 'requirements'
                                 .criterion("got_unholy", InventoryChangedCriterion.Conditions.items(ItemContent.UNHOLY_INTELLIGENCE))
                                 .build(consumer, Oritech.MOD_ID + "/unholy");
    }
}
