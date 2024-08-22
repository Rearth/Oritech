```json
{
  "title": "Bedrock Extractor",
  "icon": "oritech:deep_drill_block",
  "category": "oritech:interaction",
  "associated_items": [
    "oritech:deep_drill_block"
  ],
  "ordinal": 7
}
```

![bedrock_extractor](oritech:textures/book/deep_drill.png,fit)

;;;;;

The bedrock extractor can mine ores from below bedrock at places where [ore resource nodes](^oritech:resources/resource_nodes) are found. The [multiblock](^oritech:processing/multiblocks) machine can only operate when placed on resource nodes, and
must be powered using [enderic lasers](^oritech:interaction/enderic_laser).

;;;;;

When in operation, the bedrock extractor will mine the ores directly below it (the whole 3x3 area). Each operation will randomly select one of the blocks below. Not all blocks have to be
ore nodes.