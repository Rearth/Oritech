```json
{
  "title": "Pump",
  "icon": "oritech:pump_block",
  "category": "oritech:interaction",
  "associated_items": [
    "oritech:pump_block"
  ],
  "ordinal": 4
}
```

<block;oritech:pump_block>

The pump is used to pump liquids from below. When placed, it will extend a trunk down until a liquid or obstruction is found. Once a liquid has been found, it will
scan the liquid body and store all positions to

;;;;;

pump from. The pump can drain liquid bodies of size 100 000 or smaller. Note that the initialization may take a few
seconds for large targets.

Drained fluids are put into the internal storage. Up to 4 buckets per second can be pumped, at an energy cost of {gold}512 RF{} per block.