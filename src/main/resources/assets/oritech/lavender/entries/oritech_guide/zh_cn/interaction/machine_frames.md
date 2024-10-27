```json
{
  "title": "机器框架",
  "icon": "oritech:machine_frame_block",
  "category": "oritech:interaction",
  "associated_items": [
    "oritech:machine_frame_block",
    "oritech:destroyer_block",
    "oritech:placer_block",
    "oritech:fertilizer_block"
  ],
  "ordinal": 0
}
```

*[方块放置器](^oritech:interaction/block_placer), [方块破坏器](^oritech:interaction/block_destroyer)和[施肥机](^oritech:interaction/fertilizer)*都需要在由机器框架搭建的龙门架上工作。龙门架的大小决定机器的工作范围，其形状只能是空心矩形。

机器的朝向总是对准龙门架的**下方**。

任意数量的不同机器可以在同一龙门架上运行。为此您只需直接在同一龙门架上放置机器即可。

;;;;;

机器工作时会遍历其工作范围内的所有方块。 

所有在龙门架上运行的机器可以安装大部分插件。

比如：速度升级插件可以提高机器遍历时的移动速度和处理速度。

<block;oritech:machine_speed_addon>
