package rearth.oritech.generator;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.advancements.critereon.EnchantmentPredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemEnchantmentsPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.ItemSubPredicates;
import net.minecraft.advancements.critereon.LocationPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds.Ints;
import net.minecraft.advancements.critereon.PlayerTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import rearth.oritech.Oritech;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.ItemContent;
import rearth.oritech.init.ToolsContent;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class AdvancementGenerator extends FabricAdvancementProvider {
    
    public AdvancementGenerator(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registryLookup) {
        super(output, registryLookup);
    }
    
    @Override
    public void generateAdvancement(HolderLookup.Provider registryLookup, Consumer<AdvancementHolder> consumer) {

        var background = ResourceLocation.parse("textures/gui/advancements/backgrounds/adventure.png");
        
        var rootAdvancement = Advancement.Builder.advancement()
                                .display(
                                  ItemContent.RAW_NICKEL, // The display icon
                                  Component.translatable("advancements.oritech.begin"), // The title
                                  Component.translatable("advancements.oritech.begin.description"), // The description
                                  background, // Background image used
                                  AdvancementType.TASK, // Options: TASK, CHALLENGE, GOAL
                                  true, // Show toast top right
                                  true, // Announce to chat
                                  false // Hidden in the advancement tab
                                )
                                // The first string used in criterion is the name referenced by other advancements when they want to have 'requirements'
                                .addCriterion("got_nickel", InventoryChangeTrigger.TriggerInstance.hasItems(ItemContent.RAW_NICKEL))
                                .save(consumer, Oritech.MOD_ID + "/root");
        
        
        var generatorAdvancement = Advancement.Builder.advancement().parent(rootAdvancement)
                                .display(
                                  BlockContent.BASIC_GENERATOR_BLOCK, // The display icon
                                  Component.translatable("advancements.oritech.generator"), // The title
                                  Component.translatable("advancements.oritech.generator.description"), // The description
                                  background, // Background image used
                                  AdvancementType.TASK, // Options: TASK, CHALLENGE, GOAL
                                  true, // Show toast top right
                                  true, // Announce to chat
                                  false // Hidden in the advancement tab
                                )
                                // The first string used in criterion is the name referenced by other advancements when they want to have 'requirements'
                                .addCriterion("got_generator", InventoryChangeTrigger.TriggerInstance.hasItems(BlockContent.BASIC_GENERATOR_BLOCK))
                                .save(consumer, Oritech.MOD_ID + "/generator");
        
        var furnaceAdvancement = Advancement.Builder.advancement().parent(generatorAdvancement)
                                .display(
                                  BlockContent.POWERED_FURNACE_BLOCK, // The display icon
                                  Component.translatable("advancements.oritech.furnace"), // The title
                                  Component.translatable("advancements.oritech.furnace.description"), // The description
                                  background, // Background image used
                                  AdvancementType.TASK, // Options: TASK, CHALLENGE, GOAL
                                  true, // Show toast top right
                                  true, // Announce to chat
                                  false // Hidden in the advancement tab
                                )
                                // The first string used in criterion is the name referenced by other advancements when they want to have 'requirements'
                                .addCriterion("got_furnace", InventoryChangeTrigger.TriggerInstance.hasItems(BlockContent.POWERED_FURNACE_BLOCK))
                                .save(consumer, Oritech.MOD_ID + "/furnace");

        var steamAdvancement = Advancement.Builder.advancement().parent(generatorAdvancement)
                                .display(
                                  BlockContent.STEAM_ENGINE_BLOCK, // The display icon
                                  Component.translatable("advancements.oritech.steam_engine"), // The title
                                  Component.translatable("advancements.oritech.steam_engine.description"), // The description
                                  background, // Background image used
                                  AdvancementType.TASK, // Options: TASK, CHALLENGE, GOAL
                                  true, // Show toast top right
                                  true, // Announce to chat
                                  false // Hidden in the advancement tab
                                )
                                // The first string used in criterion is the name referenced by other advancements when they want to have 'requirements'
                                .addCriterion("got_steam_engine", InventoryChangeTrigger.TriggerInstance.hasItems(BlockContent.STEAM_ENGINE_BLOCK))
                                .save(consumer, Oritech.MOD_ID + "/steam_engine");
        
        var pulverizerAdvancement = Advancement.Builder.advancement().parent(generatorAdvancement)
                                .display(
                                  BlockContent.PULVERIZER_BLOCK, // The display icon
                                  Component.translatable("advancements.oritech.pulverizer"), // The title
                                  Component.translatable("advancements.oritech.pulverizer.description"), // The description
                                  background, // Background image used
                                  AdvancementType.TASK, // Options: TASK, CHALLENGE, GOAL
                                  true, // Show toast top right
                                  true, // Announce to chat
                                  false // Hidden in the advancement tab
                                )
                                // The first string used in criterion is the name referenced by other advancements when they want to have 'requirements'
                                .addCriterion("got_pulverizer", InventoryChangeTrigger.TriggerInstance.hasItems(BlockContent.PULVERIZER_BLOCK))
                                .save(consumer, Oritech.MOD_ID + "/pulverizer");
        
        
        var foundryAdvancement = Advancement.Builder.advancement().parent(generatorAdvancement)
                                .display(
                                  BlockContent.FOUNDRY_BLOCK, // The display icon
                                  Component.translatable("advancements.oritech.foundry"), // The title
                                  Component.translatable("advancements.oritech.foundry.description"), // The description
                                  background, // Background image used
                                  AdvancementType.TASK, // Options: TASK, CHALLENGE, GOAL
                                  true, // Show toast top right
                                  true, // Announce to chat
                                  false // Hidden in the advancement tab
                                )
                                // The first string used in criterion is the name referenced by other advancements when they want to have 'requirements'
                                .addCriterion("got_foundry", InventoryChangeTrigger.TriggerInstance.hasItems(BlockContent.FOUNDRY_BLOCK))
                                .save(consumer, Oritech.MOD_ID + "/foundry");
        
        
        var assemblerAdvancement = Advancement.Builder.advancement().parent(generatorAdvancement)
                                .display(
                                  BlockContent.ASSEMBLER_BLOCK, // The display icon
                                  Component.translatable("advancements.oritech.assembler"), // The title
                                  Component.translatable("advancements.oritech.assembler.description"), // The description
                                  background, // Background image used
                                  AdvancementType.TASK, // Options: TASK, CHALLENGE, GOAL
                                  true, // Show toast top right
                                  true, // Announce to chat
                                  false // Hidden in the advancement tab
                                )
                                // The first string used in criterion is the name referenced by other advancements when they want to have 'requirements'
                                .addCriterion("got_assembler", InventoryChangeTrigger.TriggerInstance.hasItems(BlockContent.ASSEMBLER_BLOCK))
                                .save(consumer, Oritech.MOD_ID + "/assembler");
        
        var exoBootsAdvancement = Advancement.Builder.advancement().parent(assemblerAdvancement)
                                   .display(
                                     ToolsContent.EXO_BOOTS, // The display icon
                                     Component.translatable("advancements.oritech.exo_boots"), // The title
                                     Component.translatable("advancements.oritech.exo_boots.description"), // The description
                                     background, // Background image used
                                     AdvancementType.GOAL, // Options: TASK, CHALLENGE, GOAL
                                     true, // Show toast top right
                                     true, // Announce to chat
                                     false // Hidden in the advancement tab
                                   )
                                   // The first string used in criterion is the name referenced by other advancements when they want to have 'requirements'
                                   .addCriterion("got_exo_boots", InventoryChangeTrigger.TriggerInstance.hasItems(ToolsContent.EXO_BOOTS))
                                   .save(consumer, Oritech.MOD_ID + "/exo_boots");
        
        var exoLegsAdvancement = Advancement.Builder.advancement().parent(assemblerAdvancement)
                                   .display(
                                     ToolsContent.EXO_LEGGINGS, // The display icon
                                     Component.translatable("advancements.oritech.exo_legs"), // The title
                                     Component.translatable("advancements.oritech.exo_legs.description"), // The description
                                     background, // Background image used
                                     AdvancementType.GOAL, // Options: TASK, CHALLENGE, GOAL
                                     true, // Show toast top right
                                     true, // Announce to chat
                                     false // Hidden in the advancement tab
                                   )
                                   // The first string used in criterion is the name referenced by other advancements when they want to have 'requirements'
                                   .addCriterion("got_exo_legs", InventoryChangeTrigger.TriggerInstance.hasItems(ToolsContent.EXO_LEGGINGS))
                                   .save(consumer, Oritech.MOD_ID + "/exo_legs");
        
        var exoChestAdvancement = Advancement.Builder.advancement().parent(assemblerAdvancement)
                                   .display(
                                     ToolsContent.EXO_CHESTPLATE, // The display icon
                                     Component.translatable("advancements.oritech.exo_chest"), // The title
                                     Component.translatable("advancements.oritech.exo_chest.description"), // The description
                                     background, // Background image used
                                     AdvancementType.GOAL, // Options: TASK, CHALLENGE, GOAL
                                     true, // Show toast top right
                                     true, // Announce to chat
                                     false // Hidden in the advancement tab
                                   )
                                   // The first string used in criterion is the name referenced by other advancements when they want to have 'requirements'
                                   .addCriterion("got_exo_chest", InventoryChangeTrigger.TriggerInstance.hasItems(ToolsContent.EXO_CHESTPLATE))
                                   .save(consumer, Oritech.MOD_ID + "/exo_chest");
        
        var exoHelmetAdvancement = Advancement.Builder.advancement().parent(assemblerAdvancement)
                                   .display(
                                     ToolsContent.EXO_HELMET, // The display icon
                                     Component.translatable("advancements.oritech.exo_helmet"), // The title
                                     Component.translatable("advancements.oritech.exo_helmet.description"), // The description
                                     background, // Background image used
                                     AdvancementType.GOAL, // Options: TASK, CHALLENGE, GOAL
                                     true, // Show toast top right
                                     true, // Announce to chat
                                     false // Hidden in the advancement tab
                                   )
                                   // The first string used in criterion is the name referenced by other advancements when they want to have 'requirements'
                                   .addCriterion("got_exo_helmet", InventoryChangeTrigger.TriggerInstance.hasItems(ToolsContent.EXO_HELMET))
                                   .save(consumer, Oritech.MOD_ID + "/exo_helmet");
        
        var drillAdvancement = Advancement.Builder.advancement().parent(assemblerAdvancement)
                                   .display(
                                     ToolsContent.HAND_DRILL, // The display icon
                                     Component.translatable("advancements.oritech.drill"), // The title
                                     Component.translatable("advancements.oritech.drill.description"), // The description
                                     background, // Background image used
                                     AdvancementType.GOAL, // Options: TASK, CHALLENGE, GOAL
                                     true, // Show toast top right
                                     true, // Announce to chat
                                     false // Hidden in the advancement tab
                                   )
                                   // The first string used in criterion is the name referenced by other advancements when they want to have 'requirements'
                                   .addCriterion("got_drill", InventoryChangeTrigger.TriggerInstance.hasItems(ToolsContent.HAND_DRILL))
                                   .save(consumer, Oritech.MOD_ID + "/drill");

        var resourceNodeAdvancement = Advancement.Builder.advancement().parent(drillAdvancement)
                                   .display(
                                     BlockContent.RESOURCE_NODE_DIAMOND, // The display icon
                                     Component.translatable("advancements.oritech.resource_node"), // The title
                                     Component.translatable("advancements.oritech.resource_node.description"), // The description
                                     background, // Background image used
                                     AdvancementType.CHALLENGE, // Options: TASK, CHALLENGE, GOAL
                                     true, // Show toast top right
                                     true, // Announce to chat
                                     false // Hidden in the advancement tab
                                   )
                                   // The first string used in criterion is the name referenced by other advancements when they want to have 'requirements'
                                   .addCriterion("found_resource_node", PlayerTrigger.TriggerInstance.located(EntityPredicate.Builder.entity().steppingOn(LocationPredicate.Builder.location().setBlock(BlockPredicate.Builder.block().of(new Block[]{BlockContent.RESOURCE_NODE_COAL, BlockContent.RESOURCE_NODE_COPPER, BlockContent.RESOURCE_NODE_DIAMOND, BlockContent.RESOURCE_NODE_EMERALD, BlockContent.RESOURCE_NODE_GOLD, BlockContent.RESOURCE_NODE_IRON, BlockContent.RESOURCE_NODE_LAPIS, BlockContent.RESOURCE_NODE_NICKEL, BlockContent.RESOURCE_NODE_PLATINUM, BlockContent.RESOURCE_NODE_REDSTONE})))))
                                   .save(consumer, Oritech.MOD_ID + "/resource_node");        
        
        var centrifugeAdvancement = Advancement.Builder.advancement().parent(assemblerAdvancement)
                                .display(
                                  BlockContent.CENTRIFUGE_BLOCK, // The display icon
                                  Component.translatable("advancements.oritech.centrifuge"), // The title
                                  Component.translatable("advancements.oritech.centrifuge.description"), // The description
                                  background, // Background image used
                                  AdvancementType.TASK, // Options: TASK, CHALLENGE, GOAL
                                  true, // Show toast top right
                                  true, // Announce to chat
                                  false // Hidden in the advancement tab
                                )
                                // The first string used in criterion is the name referenced by other advancements when they want to have 'requirements'
                                .addCriterion("got_centrifuge", InventoryChangeTrigger.TriggerInstance.hasItems(BlockContent.CENTRIFUGE_BLOCK))
                                .save(consumer, Oritech.MOD_ID + "/centrifuge");
        
        
        var plasticAdvancement = Advancement.Builder.advancement().parent(centrifugeAdvancement)
                                .display(
                                  ItemContent.PLASTIC_SHEET, // The display icon
                                  Component.translatable("advancements.oritech.plastic"), // The title
                                  Component.translatable("advancements.oritech.plastic.description"), // The description
                                  background, // Background image used
                                  AdvancementType.GOAL, // Options: TASK, CHALLENGE, GOAL
                                  true, // Show toast top right
                                  true, // Announce to chat
                                  false // Hidden in the advancement tab
                                )
                                // The first string used in criterion is the name referenced by other advancements when they want to have 'requirements'
                                .addCriterion("got_plastic", InventoryChangeTrigger.TriggerInstance.hasItems(ItemContent.PLASTIC_SHEET))
                                .save(consumer, Oritech.MOD_ID + "/plastic");
        
        
        var augmenterAdvancement = Advancement.Builder.advancement().parent(centrifugeAdvancement)
                                .display(
                                  BlockContent.AUGMENT_APPLICATION_BLOCK, // The display icon
                                  Component.translatable("advancements.oritech.augmenter"), // The title
                                  Component.translatable("advancements.oritech.augmenter.description"), // The description
                                  background, // Background image used
                                  AdvancementType.GOAL, // Options: TASK, CHALLENGE, GOAL
                                  true, // Show toast top right
                                  true, // Announce to chat
                                  false // Hidden in the advancement tab
                                )
                                // The first string used in criterion is the name referenced by other advancements when they want to have 'requirements'
                                .addCriterion("got_augmenter", InventoryChangeTrigger.TriggerInstance.hasItems(BlockContent.AUGMENT_APPLICATION_BLOCK))
                                .save(consumer, Oritech.MOD_ID + "/augmenter");
        
        
        var arcaneAdvancement = Advancement.Builder.advancement().parent(plasticAdvancement)
                                .display(
                                  BlockContent.ENCHANTMENT_CATALYST_BLOCK, // The display icon
                                  Component.translatable("advancements.oritech.catalyst"), // The title
                                  Component.translatable("advancements.oritech.catalyst.description"), // The description
                                  background, // Background image used
                                  AdvancementType.TASK, // Options: TASK, CHALLENGE, GOAL
                                  true, // Show toast top right
                                  true, // Announce to chat
                                  false // Hidden in the advancement tab
                                )
                                // The first string used in criterion is the name referenced by other advancements when they want to have 'requirements'
                                .addCriterion("got_catalyst", InventoryChangeTrigger.TriggerInstance.hasItems(BlockContent.ENCHANTMENT_CATALYST_BLOCK))
                                .save(consumer, Oritech.MOD_ID + "/catalyst");

        var overEnchanted = Advancement.Builder.advancement().parent(arcaneAdvancement)
                                .display(
                                  BlockContent.ENCHANTER_BLOCK.asItem(), // The display icon
                                  Component.translatable("advancements.oritech.overenchanted"), // The title
                                  Component.translatable("advancements.oritech.overenchanted.description"), // The description
                                  background, // Background image used
                                  AdvancementType.CHALLENGE, // Options: TASK, CHALLENGE, GOAL
                                  true, // Show toast top right
                                  true, // Announce to chat
                                  false // Hidden in the advancement tab
                                )
                                // The first string used in criterion is the name referenced by other advancements when they want to have 'requirements'
                                .addCriterion("got_overenchanted", InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item().withSubPredicate(ItemSubPredicates.ENCHANTMENTS, ItemEnchantmentsPredicate.enchantments(List.of(new EnchantmentPredicate(Optional.empty(), Ints.atLeast(10)))))))
                                .save(consumer, Oritech.MOD_ID + "/overenchanted");
        
        
        var laserAdvancement = Advancement.Builder.advancement().parent(centrifugeAdvancement)
                                .display(
                                  BlockContent.LASER_ARM_BLOCK, // The display icon
                                  Component.translatable("advancements.oritech.laser"), // The title
                                  Component.translatable("advancements.oritech.laser.description"), // The description
                                  background, // Background image used
                                  AdvancementType.TASK, // Options: TASK, CHALLENGE, GOAL
                                  true, // Show toast top right
                                  true, // Announce to chat
                                  false // Hidden in the advancement tab
                                )
                                // The first string used in criterion is the name referenced by other advancements when they want to have 'requirements'
                                .addCriterion("got_laser", InventoryChangeTrigger.TriggerInstance.hasItems(BlockContent.LASER_ARM_BLOCK))
                                .save(consumer, Oritech.MOD_ID + "/laser");
        
        var reactorAdvancement = Advancement.Builder.advancement().parent(laserAdvancement)
                                .display(
                                  BlockContent.REACTOR_CONTROLLER, // The display icon
                                  Component.translatable("advancements.oritech.reactor"), // The title
                                  Component.translatable("advancements.oritech.reactor.description"), // The description
                                  background, // Background image used
                                  AdvancementType.TASK, // Options: TASK, CHALLENGE, GOAL
                                  true, // Show toast top right
                                  true, // Announce to chat
                                  false // Hidden in the advancement tab
                                )
                                // The first string used in criterion is the name referenced by other advancements when they want to have 'requirements'
                                .addCriterion("got_reactor", InventoryChangeTrigger.TriggerInstance.hasItems(BlockContent.REACTOR_CONTROLLER))
                                .save(consumer, Oritech.MOD_ID + "/reactor");
        
        
        var fluxiteAdvancement = Advancement.Builder.advancement().parent(laserAdvancement)
                                .display(
                                  ItemContent.FLUXITE, // The display icon
                                  Component.translatable("advancements.oritech.fluxite"), // The title
                                  Component.translatable("advancements.oritech.fluxite.description"), // The description
                                  background, // Background image used
                                  AdvancementType.GOAL, // Options: TASK, CHALLENGE, GOAL
                                  true, // Show toast top right
                                  true, // Announce to chat
                                  false // Hidden in the advancement tab
                                )
                                // The first string used in criterion is the name referenced by other advancements when they want to have 'requirements'
                                .addCriterion("got_fluxite", InventoryChangeTrigger.TriggerInstance.hasItems(ItemContent.FLUXITE))
                                .save(consumer, Oritech.MOD_ID + "/fluxite");
        
        
        var atomicForgeAdvancement = Advancement.Builder.advancement().parent(fluxiteAdvancement)
                                 .display(
                                   BlockContent.ATOMIC_FORGE_BLOCK, // The display icon
                                   Component.translatable("advancements.oritech.atomicforge"), // The title
                                   Component.translatable("advancements.oritech.atomicforge.description"), // The description
                                   background, // Background image used
                                   AdvancementType.TASK, // Options: TASK, CHALLENGE, GOAL
                                   true, // Show toast top right
                                   true, // Announce to chat
                                   false // Hidden in the advancement tab
                                 )
                                 // The first string used in criterion is the name referenced by other advancements when they want to have 'requirements'
                                 .addCriterion("got_atomic_forge", InventoryChangeTrigger.TriggerInstance.hasItems(BlockContent.ATOMIC_FORGE_BLOCK))
                                 .save(consumer, Oritech.MOD_ID + "/atomicforge");
        
        
        var promethiumAdvancement = Advancement.Builder.advancement().parent(atomicForgeAdvancement)
                                 .display(
                                   ItemContent.PROMETHEUM_INGOT, // The display icon
                                   Component.translatable("advancements.oritech.promethium"), // The title
                                   Component.translatable("advancements.oritech.promethium.description"), // The description
                                   background, // Background image used
                                   AdvancementType.CHALLENGE, // Options: TASK, CHALLENGE, GOAL
                                   true, // Show toast top right
                                   true, // Announce to chat
                                   false // Hidden in the advancement tab
                                 )
                                 // The first string used in criterion is the name referenced by other advancements when they want to have 'requirements'
                                 .addCriterion("got_promethium", InventoryChangeTrigger.TriggerInstance.hasItems(ItemContent.PROMETHEUM_INGOT))
                                 .save(consumer, Oritech.MOD_ID + "/promethium");
        
        var ultimateCoreAdvancement = Advancement.Builder.advancement().parent(promethiumAdvancement)
                                 .display(
                                   BlockContent.MACHINE_CORE_7, // The display icon
                                   Component.translatable("advancements.oritech.ultimate_core"), // The title
                                   Component.translatable("advancements.oritech.ultimate_core.description"), // The description
                                   background, // Background image used
                                   AdvancementType.GOAL, // Options: TASK, CHALLENGE, GOAL
                                   true, // Show toast top right
                                   true, // Announce to chat
                                   false // Hidden in the advancement tab
                                 )
                                 // The first string used in criterion is the name referenced by other advancements when they want to have 'requirements'
                                 .addCriterion("got_ultimate_core", InventoryChangeTrigger.TriggerInstance.hasItems(BlockContent.MACHINE_CORE_7.asItem()))
                                 .save(consumer, Oritech.MOD_ID + "/ultimate_core");
        
        var goldGemAdvancement = Advancement.Builder.advancement().parent(atomicForgeAdvancement)
                                  .display(
                                    ItemContent.GOLD_GEM, // The display icon
                                    Component.translatable("advancements.oritech.gold_gem"), // The title
                                    Component.translatable("advancements.oritech.gold_gem.description"), // The description
                                    background, // Background image used
                                    AdvancementType.GOAL, // Options: TASK, CHALLENGE, GOAL
                                    true, // Show toast top right
                                    true, // Announce to chat
                                    false // Hidden in the advancement tab
                                  )
                                  // The first string used in criterion is the name referenced by other advancements when they want to have 'requirements'
                                  .addCriterion("got_gold_gem", InventoryChangeTrigger.TriggerInstance.hasItems(ItemContent.GOLD_GEM))
                                  .save(consumer, Oritech.MOD_ID + "/gold_gem");
        
        
        var aiAdvancement = Advancement.Builder.advancement().parent(atomicForgeAdvancement)
                                 .display(
                                   ItemContent.SUPER_AI_CHIP, // The display icon
                                   Component.translatable("advancements.oritech.ai"), // The title
                                   Component.translatable("advancements.oritech.ai.description"), // The description
                                   background, // Background image used
                                   AdvancementType.GOAL, // Options: TASK, CHALLENGE, GOAL
                                   true, // Show toast top right
                                   true, // Announce to chat
                                   false // Hidden in the advancement tab
                                 )
                                 // The first string used in criterion is the name referenced by other advancements when they want to have 'requirements'
                                 .addCriterion("got_ai", InventoryChangeTrigger.TriggerInstance.hasItems(ItemContent.SUPER_AI_CHIP))
                                 .save(consumer, Oritech.MOD_ID + "/ai");
        
        
        var unholyAdvancement = Advancement.Builder.advancement().parent(assemblerAdvancement)
                                 .display(
                                   ItemContent.UNHOLY_INTELLIGENCE, // The display icon
                                   Component.translatable("advancements.oritech.unholy"), // The title
                                   Component.translatable("advancements.oritech.unholy.description"), // The description
                                   background, // Background image used
                                   AdvancementType.CHALLENGE, // Options: TASK, CHALLENGE, GOAL
                                   true, // Show toast top right
                                   true, // Announce to chat
                                   false // Hidden in the advancement tab
                                 )
                                 // The first string used in criterion is the name referenced by other advancements when they want to have 'requirements'
                                 .addCriterion("got_unholy", InventoryChangeTrigger.TriggerInstance.hasItems(ItemContent.UNHOLY_INTELLIGENCE))
                                 .save(consumer, Oritech.MOD_ID + "/unholy");
    }
}
