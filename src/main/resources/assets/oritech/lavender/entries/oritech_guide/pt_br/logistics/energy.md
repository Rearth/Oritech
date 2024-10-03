```json
{
  "title": "Energia",
  "icon": "oritech:energy_pipe",
  "category": "oritech:logistics",
  "associated_items": [
    "oritech:energy_pipe",
    "oritech:small_storage_block",
    "oritech:large_storage_block"
  ]
}
```

O Oritech utiliza {gold}**RF**{} to power all of its machines. Ele usa a API de Energia Reborn para isso.  Isso significa que o Oritech é compatível com
todos os mods que usam o sistema de energia do Tech Reborn, ou seja, praticamente todos os mods que utilizam energia no Fabric.

Há apenas 1 nível de cabo disponível, capaz de transferir até {gold}10k RF/t{}.

;;;;;

Os geradores sempre produzirão energia, e todas as outras máquinas aceitam
energia de todos os lados (e não a voltarão a produzir). Os cabos em si armazenam até {gold}10k RF{} em cada conexão de máquina, se não conseguirem transmitir a energia.

;;;;;

Para armazenar e acumular energia, você pode usar blocos de armazenamento de energia. Eles estão disponíveis em 2 tamanhos e podem ser ampliados massivamente usando addons.
Os blocos de armazenamento de energia aceitam energia de todos os lados com uma {green}porta verde{}, e só podem transmitir para uma única {red}porta vermelha{}. Um sinal de redstone desativará toda a saída de energia.

<block;oritech:small_storage_block>

;;;;;

A energia também pode ser transferida sem fio usando um laser endérico. Veja [laser endérico](^oritech:interaction/enderic_laser)

![enderic laser](oritech:textures/book/enderic_laser.png,fit)
