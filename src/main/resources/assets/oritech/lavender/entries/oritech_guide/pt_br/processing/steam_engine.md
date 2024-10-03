```json
{
  "title": "Energia a Vapor",
  "icon": "oritech:steam_boiler_addon",
  "category": "oritech:processing",
  "associated_items": [
    "oritech:steam_engine_block",
    "oritech:steam_boiler_addon"
  ]
}
```

<block;oritech:steam_boiler_addon>


Todos os geradores (exceto o gerador básico) podem ser atualizados para produzir vapor. Para isso, adicione um addon de caldeira a vapor. Quando adicionado, a máquina não produzirá mais RF diretamente.

;;;;;

Em vez disso, ela produzirá vapor em uma taxa de 2:1. Para produzir vapor, a água será consumida. Líquidos de água e vapor podem ser bombeados para dentro e para fora diretamente do addon, mas não da própria máquina.

Para usar o vapor, uma máquina a vapor pode ser utilizada para produzir RF a partir dele. Ela utiliza vapor como entrada e fornece água como saída. No entanto, durante o processo, cerca de 20% da água será
perdida, portanto, uma fonte constante de água é necessária para os geradores.

;;;;;

Múltiplas máquinas a vapor podem ser encadeadas. Elas compartilharão o armazenamento de energia, o tanque de água e o tanque de vapor da primeira máquina na linha. Elas funcionarão de forma cooperativa.
A velocidade de uma máquina a vapor varia com base no vapor armazenado. Mais vapor resultará em mais pressão, fazendo-a operar mais rápido. 
A velocidade é escalonada linearmente com base na porcentagem de preenchimento dos tanques de vapor, com um multiplicador máximo de 10 quando o tanque está cheio.

;;;;;

No entanto, a eficiência da máquina varia com base na velocidade. Uma eficiência maior resulta em mais RF por unidade de vapor produzida. A eficiência da máquina é mais alta quando opera a cerca de 700% da velocidade.
Qualquer valor abaixo ou acima disso resultará em um rendimento menos ideal. A energia será fornecida pelas portas vermelhas da máquina. As portas de fluido são marcadas em azul.
