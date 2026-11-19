package dk.mineconed.customrecipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Made by Mineconed
 *
 * One custom recipe: type, 3x3 grid and result. Registers itself on every version from 1.8 to 1.21.
 */
public final class Opskrift {

    public enum Type { SHAPED, SHAPELESS, FURNACE }

    final String id;
    Type type = Type.SHAPED;
    ItemStack[] grid = new ItemStack[9];
    ItemStack resultat;
    boolean kraeverPerm;

    Opskrift(String id) {
        this.id = id;
    }

    // ═══════════════ VERSION ═══════════════

    private static Class<?> nkKlasse() {
        try {
            return Class.forName("org.bukkit.NamespacedKey");
        } catch (Throwable e) {
            return null;
        }
    }

    Object noegle(Plugin pl) {
        Class<?> nk = nkKlasse();
        if (nk == null) {
            return null;
        }
        try {
            return nk.getConstructor(Plugin.class, String.class).newInstance(pl, id);
        } catch (Throwable e) {
            return null;
        }
    }

    private static Object praecisValg(ItemStack i) {
        if (i == null || !i.hasItemMeta()) {
            return null;
        }
        try {
            Class<?> ec = Class.forName("org.bukkit.inventory.RecipeChoice$ExactChoice");
            ItemStack en = i.clone();
            en.setAmount(1);
            return ec.getConstructor(ItemStack.class).newInstance(en);
        } catch (Throwable e) {
            return null;
        }
    }

    private static Class<?> valgKlasse() {
        try {
            return Class.forName("org.bukkit.inventory.RecipeChoice");
        } catch (Throwable e) {
            return null;
        }
    }

    /** Data value for 1.8 to 1.12. Short.MAX_VALUE means any data. */
    private static int data(ItemStack i) {
        return i.getType().getMaxDurability() > 0 ? Short.MAX_VALUE : i.getDurability();
    }

    // ═══════════════ GRID ═══════════════

    List<ItemStack> ingredienser() {
        List<ItemStack> l = new ArrayList<ItemStack>();
        for (ItemStack i : grid) {
            if (i != null) {
                l.add(i);
            }
        }
        return l;
    }

    ItemStack ovnInput() {
        if (grid[4] != null) {
            return grid[4];
        }
        for (ItemStack i : grid) {
            if (i != null) {
                return i;
            }
        }
        return null;
    }

    /** The grid cut down to the smallest box that holds all items. Returns rows of width w. */
    private static ItemStack[][] beskaer(ItemStack[] m, int bredde) {
        int hoejde = m.length / bredde;
        int minR = hoejde;
        int maxR = Math.negateExact(1);
        int minK = bredde;
        int maxK = Math.negateExact(1);
        for (int r = 0; r < hoejde; r++) {
            for (int k = 0; k < bredde; k++) {
                ItemStack i = m[Math.addExact(r * bredde, k)];
                if (i != null && i.getType() != Material.AIR) {
                    minR = Math.min(minR, r);
                    maxR = Math.max(maxR, r);
                    minK = Math.min(minK, k);
                    maxK = Math.max(maxK, k);
                }
            }
        }
        if (maxR < 0) {
            return new ItemStack[0][0];
        }
        int rh = Math.addExact(Math.subtractExact(maxR, minR), 1);
        int rb = Math.addExact(Math.subtractExact(maxK, minK), 1);
        ItemStack[][] ud = new ItemStack[rh][rb];
        for (int r = 0; r < rh; r++) {
            for (int k = 0; k < rb; k++) {
                ItemStack i = m[Math.addExact(Math.addExact(r, minR) * bredde, Math.addExact(k, minK))];
                ud[r][k] = i == null || i.getType() == Material.AIR ? null : i;
            }
        }
        return ud;
    }

    static boolean lig(ItemStack ingrediens, ItemStack i, boolean praecis) {
        if (ingrediens == null || i == null) {
            return ingrediens == null && (i == null || i.getType() == Material.AIR);
        }
        if (ingrediens.getType() != i.getType()) {
            return false;
        }
        if (!praecis) {
            return true;
        }
        if (ingrediens.getType().getMaxDurability() == 0 && ingrediens.getDurability() != i.getDurability()) {
            return false;
        }
        if (!ingrediens.hasItemMeta()) {
            return true;
        }
        return i.hasItemMeta() && Bukkit.getItemFactory().equals(ingrediens.getItemMeta(), i.getItemMeta());
    }

    /** Does the crafting grid match? praecis also checks names, lore and enchants. */
    boolean passer(ItemStack[] matrix, boolean praecis) {
        if (type == Type.FURNACE || matrix == null) {
            return false;
        }
        if (type == Type.SHAPELESS) {
            List<ItemStack> mangler = ingredienser();
            List<ItemStack> har = new ArrayList<ItemStack>();
            for (ItemStack i : matrix) {
                if (i != null && i.getType() != Material.AIR) {
                    har.add(i);
                }
            }
            if (har.size() != mangler.size()) {
                return false;
            }
            for (ItemStack ing : mangler) {
                Iterator<ItemStack> it = har.iterator();
                boolean fundet = false;
                while (it.hasNext()) {
                    if (lig(ing, it.next(), praecis)) {
                        it.remove();
                        fundet = true;
                        break;
                    }
                }
                if (!fundet) {
                    return false;
                }
            }
            return true;
        }
        int b = matrix.length == 4 ? 2 : 3;
        ItemStack[][] a = beskaer(grid, 3);
        ItemStack[][] m = beskaer(matrix, b);
        if (a.length != m.length || a.length == 0 || a[0].length != m[0].length) {
            return false;
        }
        return samme(a, m, praecis, false) || samme(a, m, praecis, true);
    }

    private static boolean samme(ItemStack[][] a, ItemStack[][] m, boolean praecis, boolean spejl) {
        int w = a[0].length;
        for (int r = 0; r < a.length; r++) {
            for (int k = 0; k < w; k++) {
                int mk = spejl ? Math.subtractExact(Math.subtractExact(w, 1), k) : k;
                if (!lig(a[r][k], m[r][mk], praecis)) {
                    return false;
                }
            }
        }
        return true;
    }

    // ═══════════════ REGISTRER ═══════════════

    private static void ingrediens(Object r, char tegn, ItemStack i, boolean legacy) throws Exception {
        Object valg = praecisValg(i);
        Class<?> vk = valgKlasse();
        if (valg != null && vk != null) {
            try {
                if (r instanceof ShapedRecipe) {
                    r.getClass().getMethod("setIngredient", char.class, vk).invoke(r, tegn, valg);
                } else {
                    r.getClass().getMethod("addIngredient", vk).invoke(r, valg);
                }
                return;
            } catch (Throwable ignore) {
                // no exact choice on this version, the craft check handles it
            }
        }
        if (r instanceof ShapedRecipe) {
            if (legacy) {
                ((ShapedRecipe) r).setIngredient(tegn, i.getType(), data(i));
            } else {
                ((ShapedRecipe) r).setIngredient(tegn, i.getType());
            }
        } else {
            if (legacy) {
                ((ShapelessRecipe) r).addIngredient(i.getType(), data(i));
            } else {
                ((ShapelessRecipe) r).addIngredient(i.getType());
            }
        }
    }

    Recipe lav(Plugin pl, float xp, int tid) throws Exception {
        Object key = noegle(pl);
        Class<?> nk = nkKlasse();
        boolean legacy = materialLegacy();
        ItemStack res = resultat.clone();
        if (type == Type.FURNACE) {
            ItemStack in = ovnInput();
            Object valg = praecisValg(in);
            Class<?> vk = valgKlasse();
            if (key != null && valg != null && vk != null) {
                return (Recipe) FurnaceRecipe.class.getConstructor(nk, ItemStack.class, vk, float.class, int.class)
                        .newInstance(key, res, valg, xp, tid);
            }
            if (key != null) {
                try {
                    return (Recipe) FurnaceRecipe.class.getConstructor(nk, ItemStack.class, Material.class, float.class, int.class)
                            .newInstance(key, res, in.getType(), xp, tid);
                } catch (NoSuchMethodException old) {
                    // 1.12: no key constructor for furnace
                }
            }
            return legacy ? new FurnaceRecipe(res, in.getType(), data(in)) : new FurnaceRecipe(res, in.getType());
        }
        Object r;
        if (type == Type.SHAPED) {
            r = key != null ? ShapedRecipe.class.getConstructor(nk, ItemStack.class).newInstance(key, res) : new ShapedRecipe(res);
            ItemStack[][] f = beskaer(grid, 3);
            Map<String, Character> tegn = new LinkedHashMap<String, Character>();
            Map<Character, ItemStack> brug = new LinkedHashMap<Character, ItemStack>();
            String[] form = new String[f.length];
            char naeste = 'a';
            for (int ra = 0; ra < f.length; ra++) {
                StringBuilder sb = new StringBuilder();
                for (ItemStack i : f[ra]) {
                    if (i == null) {
                        sb.append(' ');
                        continue;
                    }
                    String id = i.getType().name() + ":" + i.getDurability() + ":" + (i.hasItemMeta() ? i.getItemMeta().hashCode() : 0);
                    Character t = tegn.get(id);
                    if (t == null) {
                        t = naeste;
                        naeste++;
                        tegn.put(id, t);
                        brug.put(t, i);
                    }
                    sb.append(t.charValue());
                }
                form[ra] = sb.toString();
            }
            ((ShapedRecipe) r).shape(form);
            for (Map.Entry<Character, ItemStack> e : brug.entrySet()) {
                ingrediens(r, e.getKey(), e.getValue(), legacy);
            }
        } else {
            r = key != null ? ShapelessRecipe.class.getConstructor(nk, ItemStack.class).newInstance(key, res) : new ShapelessRecipe(res);
            for (ItemStack i : ingredienser()) {
                ingrediens(r, ' ', i, legacy);
            }
        }
        return (Recipe) r;
    }

    /** True on 1.8 to 1.12 where items use data values. */
    static boolean materialLegacy() {
        return Material.matchMaterial("LIME_WOOL") == null;
    }

    void registrer(Plugin pl, float xp, int tid) throws Exception {
        afregistrer(pl);
        Bukkit.addRecipe(lav(pl, xp, tid));
    }

    void afregistrer(Plugin pl) {
        Object key = noegle(pl);
        if (key != null) {
            try {
                Method m = Bukkit.class.getMethod("removeRecipe", key.getClass());
                m.invoke(null, key);
                return;
            } catch (Throwable ignore) {
                // older than 1.15.2
            }
        }
        if (resultat == null) {
            return;
        }
        Iterator<Recipe> it = Bukkit.recipeIterator();
        while (it.hasNext()) {
            Recipe r = it.next();
            if (r == null) {
                continue;
            }
            if (key != null) {
                try {
                    Object k = r.getClass().getMethod("getKey").invoke(r);
                    if (key.equals(k)) {
                        it.remove();
                        return;
                    }
                    continue;
                } catch (Throwable ignore) {
                    // not keyed
                }
            }
            boolean sammeType = (type == Type.SHAPED && r instanceof ShapedRecipe)
                    || (type == Type.SHAPELESS && r instanceof ShapelessRecipe)
                    || (type == Type.FURNACE && r instanceof FurnaceRecipe);
            ItemStack rr = r.getResult();
            if (sammeType && rr != null && rr.isSimilar(resultat) && rr.getAmount() == resultat.getAmount()) {
                it.remove();
                return;
            }
        }
    }
}
