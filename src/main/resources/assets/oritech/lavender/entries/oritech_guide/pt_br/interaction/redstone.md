```json
{
  "title": "Redstone",
  "icon": "oritech:energy_pipe",
  "category": "oritech:interaction",
  "associated_items": [
    "oritech:machine_redstone_addon",
    "minecraft:redstone"
  ]
}
```

Algumas máquinas do Oritech podem interagir diretamente com a redstone, como o tanque portátil e o armazenamento de energia portátil. O conteúdo dos tanques portáteis pode ser medido usando um comparador, 
e a saída do armazenamento de energia portátil pode ser desativada com um sinal de redstone. Para todos os outros blocos, é necessário um "Controlador de Addon de Redstone".

;;;;;

O controlador de addon de redstone pode ser anexado assim como qualquer outro addon e pode ser configurado pela interface do usuário. Quando configurado, os dados podem ser lidos usando um comparador. O sinal do comparador será
gerado pelo addon, não pela máquina em si. A máquina também pode ser desativada com um sinal de redstone para o addon.