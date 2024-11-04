```json
{
  "title": "Cadres de Machine",
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

Le *[placeur de blocs](^oritech:interaction/block_placer), le [destructeur de blocs](^oritech:interaction/block_destroyer) et le [fertiliseur](^oritech:interaction/fertilizer)* fonctionnent tous sur une structure construite avec des cadres de machine. Le cadre de machine délimite la zone dans laquelle les machines opèrent. Cette zone doit être rectangulaire et vide à l'intérieur.

Les machines ciblent toujours les blocs situés en **dessous** du cadre.

;;;;;

Plusieurs machines peut fonctionner sur le même cadre de machine.

Pour ce faire, placez simplement plusieurs machines sur le cadre. Les machines itèrent toujours à travers tous les blocs de la zone du cadre.

Toutes les machines fonctionnant sur des cadres de machine peuvent utiliser la plupart des modules. Le module de vitesse augmente à la fois le mouvement et l'opération.
