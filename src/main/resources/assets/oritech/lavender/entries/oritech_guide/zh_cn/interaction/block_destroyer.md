```json
{
  "title": "Block Destroyer",
  "icon": "oritech:destroyer_block",
  "category": "oritech:interaction",
  "associated_items": [
    "oritech:destroyer_block"
  ],
  "ordinal": 3
}
```

<block;oritech:destroyer_block[machine_assembled=true]>

正如您所想的那样，方块破坏器是用于破坏由[机器框架](^oritech:interaction/machine_frames)搭建的龙门架下方方块的[多方块结构](^oritech:processing/multiblocks)。

;;;;;

破坏一个方块所需的时间和能量取决于被破坏方块的硬度。方块破坏器总是会试图破坏位于其工作范围内的所有方块。通过安装作物过滤插件可以使其自动跳过未成熟作物并收获成熟作物。

<block;oritech:crop_filter_addon>

;;;;;

通过安装采石场插件可以将方块破坏器作为采石场使用。每个采石场插件可以使方块破坏器的最大破坏距离乘以8。

这意味着安装一个采石场插件可以使方块破坏器破坏距离变为8格，两个采石场插件可以使其破坏距离变为64格，而三个采石场插件可以使其破坏距离达到512格。

<block;oritech:quarry_addon>
