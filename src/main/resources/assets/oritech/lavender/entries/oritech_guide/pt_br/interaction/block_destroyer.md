```json
{
  "title": "Destruidor de Blocos",
  "icon": "oritech:destroyer_block",
  "category": "oritech:interaction",
  "associated_items": [
    "oritech:destroyer_block"
  ],
  "ordinal": 3
}
```

<block;oritech:destroyer_block[machine_assembled=true]>

O destruidor de blocos é usado para, como você já deve ter adivinhado, destruir blocos. É um [multi-bloco](^oritech:processing/multiblocks) que opera em uma [estrutura de máquina](^oritech:interaction:machine_frames), e mira diretamente na camada logo abaixo da estrutura.

;;;;;

O tempo e a energia necessários para quebrar um bloco são baseados na dureza dos blocos. O destruidor de blocos tenta destruir todos os blocos abaixo. Para permitir o uso na agricultura, o addon de filtro de plantações pode ser instalado.
Isso fará com que o destruidor de blocos ignore todas as plantaçoes não finalizadas.


<block;oritech:crop_filter_addon>

;;;;;

Ao adicionar addons de pedreira, o destruidor de blocos também pode ser usado como uma pedreira. Cada addon de pedreira multiplica o alcance por 8.

Isso significa que um addon dá 8 de alcance, 2 addons dão 64 de alcance, e 3 addons dão 512 de alcance.

<block;oritech:quarry_addon>