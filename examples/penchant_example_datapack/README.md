# Example Penchant Definition Datapack

This datapack demonstrates how to customize enchantment leveling and table costs in Penchant for Minecraft 1.20.1.

## Structure

Definitions are placed in:
`data/<namespace>/penchant/definition/<enchantment_id>.json`

For example:
* `data/minecraft/penchant/definition/unbreaking.json`
* `data/minecraft/penchant/definition/aqua_affinity.json`

## JSON Schema

```json
{
  "progress_cost_factor": {
    "base": 100,
    "per_level": 50
  },
  "experience_cost": 5,
  "book_requirement": 25
}
```

* **`progress_cost_factor`**:
  * `base`: The progress cost factor for Level 1 -> 2.
  * `per_level`: How much the cost factor increases for each additional level (e.g. Level 2 -> 3 is `base + per_level`).
  * *(Can also be defined as a flat number like `"progress_cost_factor": 100`)*
* **`experience_cost`**: Number of XP levels required in the reworked enchanting table.
* **`book_requirement`**: Number of bookshelves required around the table.

*(All fields are optional; omitting any field will fall back to vanilla formulas or server config).*

## Datapack Priority & Locking

* Any property defined in a loaded datapack takes highest priority.
* Operator in-game commands (`/penchant ...`) cannot override values defined in a datapack (the command will display an error message explaining that the property is locked by a datapack).
