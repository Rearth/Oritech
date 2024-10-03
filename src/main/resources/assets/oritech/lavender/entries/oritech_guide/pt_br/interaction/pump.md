```json
{
  "title": "Bomba",
  "icon": "oritech:pump_block",
  "category": "oritech:interaction",
  "associated_items": [
    "oritech:pump_block"
  ],
  "ordinal": 4
}
```

<block;oritech:pump_block>

A bomba é usada para bombear líquidos debaixo da terra. Quando colocada, ela estenderá um tronco para baixo até que um líquido ou obstrução seja encontrado. Assim que um líquido for encontrado, ela irá
escanear o corpo líquido e armazenar todas as posições para

;;;;;

bombear. A bomba pode drenar corpos líquidos de tamanho 100.000 ou menor. Note que a inicialização pode levar alguns
segundos para alvos grandes.

Os fluidos drenados são colocados no armazenamento interno. Até 4 baldes por segundo podem ser bombeados, a um custo de energia de {gold}512 RF{} por bloco.