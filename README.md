<p align="center"><img src="CustomRecipes_ikon_128.png" width="128"></p>
<h1 align="center">CustomRecipes</h1>
<p align="center">Make your own crafting recipes in a GUI.<br>Made by <b>Mineconed</b> • Minecraft 1.8 to 1.21</p>

## Features
- 🧰 **GUI editor**: put items in a 3x3 grid and pick the result
- 🔷 **Shaped**, **shapeless** and **furnace** recipes
- ✨ Works with **custom items**: names, lore and enchants must match
- 🔢 Choose how many items the result gives
- 🔑 Optional **permission per recipe**
- 📖 Recipes show up in the **recipe book** (1.13+)
- 👀 **/recipes** lets players see all custom recipes
- ⚡ Recipes work **right away**, no restart needed
- 🎁 **/customrecipes give** gives the result to a player
- 🛡️ Safe editor: your items are never taken or lost
- 🌐 English and Danish built in

## Setup
1. Put **CustomRecipes.jar** in your plugins folder and start the server
2. Type **/customrecipes** and click **New recipe**
3. Pick up an item and click a slot in the grid. Put the result on the right
4. Click **Save**. The recipe works right away!

## Commands
| Command | What it does |
|---|---|
| `/customrecipes` | open the editor (admin) or the recipe list (players). Also /recipes |
| `/customrecipes view` | see all recipes |
| `/customrecipes list` | list recipes in chat |
| `/customrecipes give <id> [player]` | give the result |
| `/customrecipes delete <id>` | delete a recipe |
| `/customrecipes reload` | reload |

## Permissions
| Permission | What it does |
|---|---|
| `customrecipes.admin` | make, edit and delete recipes (op) |
| `customrecipes.view` | see recipes (everyone) |
| `customrecipes.craft.<id>` | craft a recipe that needs permission |
| `customrecipes.craft.all` | craft every recipe (op) |

## Config
| Setting | What it does |
|---|---|
| `language` | en or da |
| `add_to_recipe_book` | show recipes in the recipe book (1.13+) |
| `furnace.xp` | XP from furnace recipes |
| `furnace.cook_ticks` | cook time, 200 = 10 seconds |

## Versions
1.8 to 1.21. Recipes are saved in recipes.yml.
