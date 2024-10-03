```json
{
  "title": "Porto de Drone",
  "icon": "oritech:drone_port_block",
  "category": "oritech:logistics"
}
```

<block;oritech:drone_port_block>

O porto de drone permite que você transporte itens por grandes distâncias usando drones voadores. É necessário ter um porto de drone na posição de decolagem
e aterrissagem, que deve ser construído e alimentado.

;;;;;

Uma vez construído, você precisa atribuir o porto alvo usando um item de designador de alvo. Vincule-o ao porto de drone alvo clicando com shift + botão direito no
porto alvo. Em seguida, no porto de onde você deseja enviar itens, abra a interface do usuário e coloque o designador no slot especial para itens.


O porto alvo precisa estar a pelo menos 50 blocos de distância. A área em que ele está também precisa estar carregada.

;;;;;

Um porto de drone só pode enviar itens para um porto alvo específico, mas um porto pode receber itens de vários portos. No entanto, cada drone
leva alguns segundos para aterrissar, então, se os itens estiverem chegando com muita frequência, um porto receptor pode ficar sobrecarregado quando é alvo de múltiplos portos.


O tempo que leva para entregar itens é constante, não importa quão longe o drone tenha que voar. No entanto, o custo de energia aumenta com a distância.

;;;;;

A raiz quadrada da distância é usada no cálculo do uso de energia.