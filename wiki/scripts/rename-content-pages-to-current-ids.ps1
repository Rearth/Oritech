[CmdletBinding(SupportsShouldProcess)]
param(
    [string]$WikiDirectory = (Join-Path $PSScriptRoot '..')
)

$pageRenames = [ordered]@{
    'accelerator_controller' = 'particle_accelerator'
    'advanced_augment_station' = 'quantum_research_station'
    'assembler_block' = 'assembler'
    'atomic_forge_block' = 'atomic_forge'
    'augment_application_block' = 'cybernetic_augmentation_center'
    'basic_generator_block' = 'basic_generator'
    'big_solar_panel_block' = 'big_solar_panel'
    'bio_generator_block' = 'bio_generator'
    'capacitor_addon_extender' = 'power_bank_addon_extender'
    'ceiling_light' = 'industrial_light'
    'centrifuge_block' = 'centrifuge'
    'charger_block' = 'equipment_charger'
    'cooler_block' = 'industrial_chiller'
    'creative_storage_block' = 'creative_storage'
    'creative_tank_block' = 'creative_tank'
    'deep_drill_block' = 'bedrock_extractor'
    'destroyer_block' = 'destroyer'
    'drone_port_block' = 'drone_port'
    'enchanter_block' = 'stabilized_enchanter'
    'enchantment_catalyst_block' = 'arcane_catalyst'
    'fertilizer_block' = 'fertilizer'
    'foundry_block' = 'foundry'
    'fragment_forge_block' = 'fragment_forge'
    'fuel_generator_block' = 'fuel_generator'
    'industrial_glass_block' = 'industrial_glass'
    'item_filter_block' = 'item_filter'
    'large_storage_block' = 'large_storage'
    'laser_arm_block' = 'enderic_laser'
    'lava_generator_block' = 'lava_generator'
    'low_yield_nuke' = 'low_yield_nuclear_explosion_device'
    'machine_combi_addon' = 'heart_of_the_machine_addon'
    'machine_frame_block' = 'machine_frame'
    'machine_plating_block' = 'copper_reinforced_plating'
    'machine_processing_addon' = 'auxiliary_processing_chamber_addon'
    'machine_redstone_addon' = 'control_unit_addon'
    'machine_ultimate_addon' = 'synergy_matrix_addon'
    'metal_beam_block' = 'industrial_support_beam'
    'metal_girder_block' = 'industrial_support_girder'
    'nuke' = 'manhattan_module'
    'particle_collector_block' = 'tachyon_absorber'
    'pipe_booster_block' = 'pipe_booster'
    'placer_block' = 'placer'
    'portable_laser' = 'enderic_railgun'
    'power_pole_block' = 'energy_transmission_pole'
    'powered_furnace_block' = 'powered_furnace'
    'pulverizer_block' = 'pulverizer'
    'pump_block' = 'pump'
    'reactor_absorber_port' = 'reactor_coolant_absorber_port'
    'reactor_condenser' = 'reactor_heat_absorber'
    'reactor_controller' = 'nuclear_reactor_controller'
    'reactor_reflector' = 'reactor_neutron_reflector'
    'reactor_vent' = 'reactor_heat_vent'
    'refinery_block' = 'refinery'
    'refinery_module_block' = 'refinery_chamber_module'
    'resource_node_redstone' = 'redstone_resource_node'
    'shrinker_block' = 'addon_splicer'
    'simple_augment_station' = 'cybernetic_research_station'
    'small_storage_block' = 'portable_energy_storage'
    'small_tank_block' = 'portable_tank'
    'spawner_cage_block' = 'spawner_cage'
    'spawner_controller_block' = 'spawner_controller'
    'steam_engine_block' = 'steam_engine'
    'tech_button' = 'industrial_button'
    'tech_door' = 'industrial_door'
    'tech_lever' = 'industrial_lever'
    'treefeller_block' = 'tree_cutter'
    'unstable_container' = 'schrodingers_safe'
    'wither_crop_block' = 'soul_flowers'
}

$wikiRoot = (Resolve-Path -LiteralPath $WikiDirectory).Path
$moves = @(
    Get-ChildItem -LiteralPath $wikiRoot -Recurse -File -Filter '*.mdx' |
        Where-Object { $pageRenames.Contains($_.BaseName) } |
        ForEach-Object {
            [pscustomobject]@{
                Source = $_.FullName
                Destination = Join-Path $_.DirectoryName ($pageRenames[$_.BaseName] + '.mdx')
            }
        }
)

foreach ($move in $moves) {
    if (Test-Path -LiteralPath $move.Destination) {
        throw "Cannot rename '$($move.Source)': destination already exists: $($move.Destination)"
    }
}

foreach ($move in $moves) {
    if ($PSCmdlet.ShouldProcess($move.Source, "Rename to '$([IO.Path]::GetFileName($move.Destination))'")) {
        Move-Item -LiteralPath $move.Source -Destination $move.Destination
    }
}

$updatedMetadata = 0
foreach ($metadataFile in Get-ChildItem -LiteralPath $wikiRoot -Recurse -File -Filter '_meta.json') {
    $source = [IO.File]::ReadAllText($metadataFile.FullName)
    $updated = $source
    foreach ($rename in $pageRenames.GetEnumerator()) {
        $updated = $updated.Replace('"' + $rename.Key + '.mdx"', '"' + $rename.Value + '.mdx"')
    }

    if ($updated -ne $source -and $PSCmdlet.ShouldProcess($metadataFile.FullName, 'Update renamed MDX links')) {
        [IO.File]::WriteAllText($metadataFile.FullName, $updated, [Text.UTF8Encoding]::new($false))
        $updatedMetadata++
    }
}

Write-Host "Renamed $($moves.Count) MDX files and updated $updatedMetadata metadata files."
