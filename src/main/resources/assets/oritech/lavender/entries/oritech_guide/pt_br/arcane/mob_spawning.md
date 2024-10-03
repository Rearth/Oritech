```json
{
  "title": "Spawning de Mobs",
  "icon": "oritech:spawner_controller_block",
  "category": "oritech:arcane",
  "associated_items": [
    "oritech:spawner_controller_block",
    "oritech:spawner_cage_block"
  ]
}
```

<block;oritech:spawner_controller_block>

Os mobs podem ser spawnados combinando um controlador de spawner com uma gaiola de spawner abaixo. Dependendo do tamanho da entidade spawnada, um tamanho diferente de gaiola será necessário.
O tipo do mob spawnado é determinado pelo

;;;;;

primeiro mob a pisar sobre o controlador e só pode ser alterado ao substituir o controlador. O controlador coletará almas
e as usará para spawnar o mob definido. O custo em almas depende dos HP do mob spawnado. Ele só spawnará mobs se uma superfície vazia estiver disponível nas proximidades.
Quando um tipo de mob está definido (ou quando clicado com o botão direito se inválido), o spawner destacará qual tamanho de gaiola de spawner é necessário abaixo. Você também pode clicar com o botão direito no spawner para

;;;;;

obter algumas informações sobre o estado operacional atual.
