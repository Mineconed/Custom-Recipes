package dk.mineconed.customrecipes;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Made by Mineconed
 *
 * CustomRecipes: make your own crafting and furnace recipes in a GUI.
 */
public final class CustomRecipes extends JavaPlugin implements Listener {

    static final int[] GRID = {10, 11, 12, 19, 20, 21, 28, 29, 30};
    static final int PIL = 23;
    static final int RESULTAT = 25;
    static final int INFO = 4;
    static final int TILBAGE = 45;
    static final int TYPE = 47;
    static final int GEM = 49;
    static final int PERM = 51;
    static final int SLET = 53;

    private Tekster msg;
    private final Map<String, Opskrift> opskrifter = new LinkedHashMap<String, Opskrift>();
    private File fil;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        indlaesBeskeder();
        fil = new File(getDataFolder(), "recipes.yml");
        indlaes();
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("CustomRecipes is enabled with " + opskrifter.size() + " recipes. Made by Mineconed.");
    }

    @Override
    public void onDisable() {
        for (Opskrift o : opskrifter.values()) {
            o.afregistrer(this);
        }
    }

    // ═══════════════ HJÆLPERE ═══════════════

    private void indlaesBeskeder() {
        for (String n : new String[]{"messages_en.yml", "messages_da.yml"}) {
            if (!new File(getDataFolder(), n).exists()) {
                saveResource(n, false);
            }
        }
        String sprog = getConfig().getString("language", "en");
        File f = new File(getDataFolder(), "messages_" + sprog + ".yml");
        if (!f.exists()) {
            f = new File(getDataFolder(), "messages_en.yml");
            sprog = "en";
        }
        msg = Tekster.laes(f, getResource(sprog.equals("da") ? "messages_da.yml" : "messages_en.yml"));
    }

    private String c(String k, String... ph) {
        String s = msg.get(k, k);
        for (int i = 0; i + 1 < ph.length; i += 2) {
            s = s.replace(ph[i], ph[i + 1]);
        }
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    private void send(CommandSender s, String k, String... ph) {
        s.sendMessage(c("prefix") + c(k, ph));
    }

    private List<String> linjer(String k, String... ph) {
        return new ArrayList<String>(Arrays.asList(c(k, ph).split("\n")));
    }

    @SuppressWarnings("deprecation")
    private static ItemStack mat(String ny, String gammel, int data) {
        Material m = Material.matchMaterial(ny);
        if (m != null) {
            return new ItemStack(m, 1);
        }
        m = Material.matchMaterial(gammel);
        return new ItemStack(m == null ? Material.STONE : m, 1, (short) data);
    }

    private static ItemStack ikon(ItemStack i, String navn, List<String> lore) {
        ItemMeta m = i.getItemMeta();
        if (m != null) {
            m.setDisplayName(navn);
            if (lore != null) {
                m.setLore(lore);
            }
            try {
                m.addItemFlags(ItemFlag.values());
            } catch (Throwable ignore) {
                // ok
            }
            i.setItemMeta(m);
        }
        return i;
    }

    private ItemStack glas() {
        return ikon(mat("GRAY_STAINED_GLASS_PANE", "STAINED_GLASS_PANE", 7), " ", null);
    }

    private static String titel(String t) {
        return t.length() > 32 ? t.substring(0, 32) : t;
    }

    private float xp() {
        return (float) getConfig().getDouble("furnace.xp", 0.1);
    }

    private int kogetid() {
        return getConfig().getInt("furnace.cook_ticks", 200);
    }

    private String typeNavn(Opskrift.Type t) {
        return c(t == Opskrift.Type.SHAPED ? "type_shaped" : t == Opskrift.Type.SHAPELESS ? "type_shapeless" : "type_furnace");
    }

    private String nytId() {
        int n = 1;
        while (opskrifter.containsKey("recipe" + n)) {
            n++;
        }
        return "recipe" + n;
    }

    private void opdag(Player p, Opskrift o) {
        if (!getConfig().getBoolean("add_to_recipe_book", true)) {
            return;
        }
        Object k = o.noegle(this);
        if (k == null) {
            return;
        }
        try {
            p.getClass().getMethod("discoverRecipe", k.getClass()).invoke(p, k);
        } catch (Throwable ignore) {
            // 1.12 or older
        }
    }

    // ═══════════════ DATA ═══════════════

    private void indlaes() {
        for (Opskrift o : opskrifter.values()) {
            o.afregistrer(this);
        }
        opskrifter.clear();
        if (!fil.exists()) {
            return;
        }
        YamlConfiguration y = YamlConfiguration.loadConfiguration(fil);
        ConfigurationSection cs = y.getConfigurationSection("recipes");
        if (cs == null) {
            return;
        }
        for (String id : cs.getKeys(false)) {
            ConfigurationSection s = cs.getConfigurationSection(id);
            Opskrift o = new Opskrift(id);
            try {
                o.type = Opskrift.Type.valueOf(s.getString("type", "SHAPED"));
            } catch (IllegalArgumentException e) {
                o.type = Opskrift.Type.SHAPED;
            }
            o.kraeverPerm = s.getBoolean("permission", false);
            o.resultat = s.getItemStack("result");
            for (int i = 0; i < 9; i++) {
                o.grid[i] = s.getItemStack("grid." + i);
            }
            if (o.resultat == null || o.ingredienser().isEmpty()) {
                getLogger().warning("Recipe " + id + " is missing items and was skipped.");
                continue;
            }
            try {
                o.registrer(this, xp(), kogetid());
                opskrifter.put(id, o);
            } catch (Throwable e) {
                getLogger().warning("Could not add recipe " + id + ": " + e);
            }
        }
    }

    private void gem() {
        YamlConfiguration y = new YamlConfiguration();
        for (Opskrift o : opskrifter.values()) {
            String s = "recipes." + o.id + ".";
            y.set(s + "type", o.type.name());
            y.set(s + "permission", o.kraeverPerm);
            y.set(s + "result", o.resultat);
            for (int i = 0; i < 9; i++) {
                if (o.grid[i] != null) {
                    y.set(s + "grid." + i, o.grid[i]);
                }
            }
        }
        try {
            y.save(fil);
        } catch (IOException e) {
            getLogger().warning("Could not save recipes.yml: " + e.getMessage());
        }
    }

    // ═══════════════ MENUER ═══════════════

    void aabnListe(Player p, int side, boolean admin) {
        Meny m = new Meny(Meny.Slags.LISTE, admin);
        List<Opskrift> alle = new ArrayList<Opskrift>(opskrifter.values());
        int sider = Math.max(1, (alle.size() + 44) / 45);
        side = Math.max(0, Math.min(side, Math.subtractExact(sider, 1)));
        m.side = side;
        Inventory inv = Bukkit.createInventory(m, 54, titel(c(admin ? "title_editor" : "title_view",
                "{page}", String.valueOf(side + 1), "{pages}", String.valueOf(sider))));
        m.inv = inv;
        int start = side * 45;
        for (int i = 0; i < 45 && start + i < alle.size(); i++) {
            Opskrift o = alle.get(start + i);
            ItemStack vis = o.resultat.clone();
            ItemMeta im = vis.getItemMeta();
            if (im != null) {
                List<String> lore = im.hasLore() ? new ArrayList<String>(im.getLore()) : new ArrayList<String>();
                lore.add(" ");
                lore.addAll(linjer(admin ? "lore_recipe_admin" : "lore_recipe_view", "{id}", o.id, "{type}", typeNavn(o.type),
                        "{perm}", o.kraeverPerm ? "customrecipes.craft." + o.id : c("none")));
                im.setLore(lore);
                vis.setItemMeta(im);
            }
            inv.setItem(i, vis);
        }
        for (int s = 45; s < 54; s++) {
            inv.setItem(s, glas());
        }
        if (side > 0) {
            inv.setItem(45, ikon(new ItemStack(Material.ARROW), c("button_prev"), null));
        }
        if (side < Math.subtractExact(sider, 1)) {
            inv.setItem(53, ikon(new ItemStack(Material.ARROW), c("button_next"), null));
        }
        if (admin) {
            inv.setItem(49, ikon(mat("LIME_WOOL", "WOOL", 5), c("button_new"), linjer("lore_new")));
        } else {
            inv.setItem(49, ikon(new ItemStack(Material.BOOK), c("button_info"), linjer("lore_info", "{amount}", String.valueOf(alle.size()))));
        }
        p.openInventory(inv);
    }

    void aabnEditor(Player p, Opskrift o, boolean ny, Meny.Slags slags) {
        boolean admin = slags == Meny.Slags.REDIGER;
        Meny m = new Meny(slags, admin);
        m.kladde = o;
        m.gammeltId = ny ? null : o.id;
        Inventory inv = Bukkit.createInventory(m, 54, titel(c(admin ? (ny ? "title_new" : "title_edit") : "title_show", "{id}", o.id)));
        m.inv = inv;
        for (int s = 0; s < 54; s++) {
            inv.setItem(s, glas());
        }
        for (int i = 0; i < 9; i++) {
            inv.setItem(GRID[i], o.grid[i] == null ? null : o.grid[i].clone());
        }
        inv.setItem(RESULTAT, o.resultat == null ? null : o.resultat.clone());
        tegnKnapper(m);
        p.openInventory(inv);
    }

    private void tegnKnapper(Meny m) {
        Inventory inv = m.inv;
        Opskrift o = m.kladde;
        ItemStack pil = o.type == Opskrift.Type.FURNACE ? new ItemStack(Material.FURNACE) : mat("CRAFTING_TABLE", "WORKBENCH", 0);
        inv.setItem(PIL, ikon(pil, c("arrow_name", "{type}", typeNavn(o.type)), o.type == Opskrift.Type.FURNACE ? linjer("lore_furnace") : null));
        inv.setItem(TILBAGE, ikon(new ItemStack(Material.ARROW), c("button_back"), null));
        if (!m.admin) {
            return;
        }
        inv.setItem(INFO, ikon(new ItemStack(Material.BOOK), c("button_help"), linjer("lore_help")));
        inv.setItem(TYPE, ikon(o.type == Opskrift.Type.FURNACE ? new ItemStack(Material.FURNACE) : mat("CRAFTING_TABLE", "WORKBENCH", 0),
                c("button_type", "{type}", typeNavn(o.type)), linjer("lore_type")));
        inv.setItem(GEM, ikon(mat("LIME_WOOL", "WOOL", 5), c("button_save"), linjer("lore_save", "{id}", o.id)));
        inv.setItem(PERM, ikon(new ItemStack(Material.NAME_TAG), c("button_perm", "{state}", o.kraeverPerm ? c("on") : c("off")),
                linjer("lore_perm", "{perm}", "customrecipes.craft." + o.id)));
        if (m.gammeltId != null) {
            inv.setItem(SLET, ikon(mat("RED_WOOL", "WOOL", 14), c("button_delete"), linjer("lore_delete")));
        }
    }

    private static int gridIndex(int slot) {
        for (int i = 0; i < 9; i++) {
            if (GRID[i] == slot) {
                return i;
            }
        }
        return Math.negateExact(1);
    }

    private static boolean tom(ItemStack i) {
        return i == null || i.getType() == Material.AIR;
    }

    private void laesGrid(Meny m) {
        for (int i = 0; i < 9; i++) {
            ItemStack it = m.inv.getItem(GRID[i]);
            m.kladde.grid[i] = tom(it) ? null : it.clone();
        }
        ItemStack r = m.inv.getItem(RESULTAT);
        m.kladde.resultat = tom(r) ? null : r.clone();
    }

    // ═══════════════ KLIK ═══════════════

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof Meny) || !(e.getWhoClicked() instanceof Player)) {
            return;
        }
        Meny m = (Meny) e.getView().getTopInventory().getHolder();
        Player p = (Player) e.getWhoClicked();
        int raw = e.getRawSlot();
        int top = e.getView().getTopInventory().getSize();
        if (e.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            e.setCancelled(true);
            return;
        }
        if (raw >= top) {
            if (e.isShiftClick()) {
                e.setCancelled(true);
                if (m.slags == Meny.Slags.REDIGER && !tom(e.getCurrentItem())) {
                    tilfoejTilGrid(m, e.getCurrentItem());
                }
            }
            return;
        }
        e.setCancelled(true);
        if (raw < 0) {
            return;
        }
        if (m.slags == Meny.Slags.LISTE) {
            klikListe(p, m, raw, e.getClick());
        } else if (m.slags == Meny.Slags.VIS) {
            if (raw == TILBAGE) {
                aabnListe(p, 0, false);
            }
        } else {
            klikEditor(p, m, raw, e.getClick(), e.getCursor());
        }
    }

    private void tilfoejTilGrid(Meny m, ItemStack i) {
        ItemStack kopi = i.clone();
        kopi.setAmount(1);
        if (m.kladde.type == Opskrift.Type.FURNACE) {
            m.inv.setItem(GRID[4], kopi);
            return;
        }
        for (int s : GRID) {
            if (tom(m.inv.getItem(s))) {
                m.inv.setItem(s, kopi);
                return;
            }
        }
    }

    private void klikListe(Player p, Meny m, int raw, ClickType klik) {
        List<Opskrift> alle = new ArrayList<Opskrift>(opskrifter.values());
        if (raw == 45 && m.side > 0) {
            aabnListe(p, Math.subtractExact(m.side, 1), m.admin);
            return;
        }
        if (raw == 53) {
            aabnListe(p, m.side + 1, m.admin);
            return;
        }
        if (raw == 49 && m.admin) {
            Opskrift ny = new Opskrift(nytId());
            aabnEditor(p, ny, true, Meny.Slags.REDIGER);
            return;
        }
        int idx = m.side * 45 + raw;
        if (raw >= 45 || idx >= alle.size()) {
            return;
        }
        Opskrift o = alle.get(idx);
        if (m.admin && klik == ClickType.SHIFT_RIGHT) {
            slet(p, o);
            aabnListe(p, m.side, true);
            return;
        }
        if (m.admin) {
            aabnEditor(p, kopi(o), false, Meny.Slags.REDIGER);
        } else {
            aabnEditor(p, o, false, Meny.Slags.VIS);
        }
    }

    private static Opskrift kopi(Opskrift o) {
        Opskrift k = new Opskrift(o.id);
        k.type = o.type;
        k.kraeverPerm = o.kraeverPerm;
        k.resultat = o.resultat == null ? null : o.resultat.clone();
        for (int i = 0; i < 9; i++) {
            k.grid[i] = o.grid[i] == null ? null : o.grid[i].clone();
        }
        return k;
    }

    private void klikEditor(Player p, Meny m, int raw, ClickType klik, ItemStack cursor) {
        int g = gridIndex(raw);
        if (g >= 0) {
            if (m.kladde.type == Opskrift.Type.FURNACE && g != 4) {
                send(p, "furnace_middle");
                return;
            }
            if (tom(cursor)) {
                m.inv.setItem(raw, null);
            } else {
                ItemStack k = cursor.clone();
                k.setAmount(1);
                m.inv.setItem(raw, k);
            }
            return;
        }
        if (raw == RESULTAT) {
            ItemStack nu = m.inv.getItem(RESULTAT);
            if (!tom(cursor)) {
                m.inv.setItem(RESULTAT, cursor.clone());
            } else if (!tom(nu)) {
                if (klik.isShiftClick()) {
                    m.inv.setItem(RESULTAT, null);
                } else if (klik.isRightClick()) {
                    nu.setAmount(Math.max(1, Math.subtractExact(nu.getAmount(), 1)));
                    m.inv.setItem(RESULTAT, nu);
                } else {
                    nu.setAmount(Math.min(64, nu.getAmount() + 1));
                    m.inv.setItem(RESULTAT, nu);
                }
            }
            return;
        }
        switch (raw) {
            case TILBAGE:
                aabnListe(p, 0, true);
                return;
            case TYPE:
                laesGrid(m);
                Opskrift.Type[] t = Opskrift.Type.values();
                m.kladde.type = t[(m.kladde.type.ordinal() + 1) % t.length];
                if (m.kladde.type == Opskrift.Type.FURNACE) {
                    ItemStack in = m.kladde.ovnInput();
                    for (int s : GRID) {
                        m.inv.setItem(s, null);
                    }
                    m.inv.setItem(GRID[4], in);
                }
                tegnKnapper(m);
                return;
            case PERM:
                m.kladde.kraeverPerm = !m.kladde.kraeverPerm;
                tegnKnapper(m);
                return;
            case GEM:
                gemKladde(p, m);
                return;
            case SLET:
                if (m.gammeltId != null && klik.isShiftClick()) {
                    Opskrift o = opskrifter.get(m.gammeltId);
                    if (o != null) {
                        slet(p, o);
                    }
                    aabnListe(p, 0, true);
                } else if (m.gammeltId != null) {
                    send(p, "delete_confirm");
                }
                return;
            default:
                break;
        }
    }

    private void gemKladde(Player p, Meny m) {
        laesGrid(m);
        Opskrift o = m.kladde;
        if (o.resultat == null) {
            send(p, "need_result");
            return;
        }
        if (o.ingredienser().isEmpty()) {
            send(p, "need_ingredients");
            return;
        }
        if (o.type == Opskrift.Type.FURNACE) {
            ItemStack in = o.ovnInput();
            o.grid = new ItemStack[9];
            o.grid[4] = in;
        }
        Opskrift gammel = m.gammeltId == null ? null : opskrifter.get(m.gammeltId);
        if (gammel != null) {
            gammel.afregistrer(this);
        }
        try {
            o.registrer(this, xp(), kogetid());
        } catch (Throwable ex) {
            send(p, "save_failed", "{error}", String.valueOf(ex.getMessage()));
            getLogger().warning("Could not add recipe " + o.id + ": " + ex);
            if (gammel != null) {
                try {
                    gammel.registrer(this, xp(), kogetid());
                } catch (Throwable ignore) {
                    // ignore
                }
            }
            return;
        }
        opskrifter.put(o.id, o);
        gem();
        for (Player on : Bukkit.getOnlinePlayers()) {
            opdag(on, o);
        }
        send(p, "saved", "{id}", o.id);
        aabnListe(p, 0, true);
    }

    private void slet(Player p, Opskrift o) {
        o.afregistrer(this);
        opskrifter.remove(o.id);
        gem();
        send(p, "deleted", "{id}", o.id);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof Meny)) {
            return;
        }
        int top = e.getView().getTopInventory().getSize();
        for (int s : e.getRawSlots()) {
            if (s < top) {
                e.setCancelled(true);
                return;
            }
        }
    }

    // ═══════════════ CRAFTING ═══════════════

    private boolean maaLave(HumanEntity h, Opskrift o) {
        if (!o.kraeverPerm) {
            return true;
        }
        return h != null && (h.hasPermission("customrecipes.craft." + o.id) || h.hasPermission("customrecipes.craft.all"));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepare(PrepareItemCraftEvent e) {
        CraftingInventory ci = e.getInventory();
        ItemStack res = ci.getResult();
        if (tom(res)) {
            return;
        }
        ItemStack[] matrix = ci.getMatrix();
        HumanEntity h = e.getViewers().isEmpty() ? null : e.getViewers().get(0);
        for (Opskrift o : opskrifter.values()) {
            if (o.type == Opskrift.Type.FURNACE || !o.resultat.isSimilar(res) || !o.passer(matrix, false)) {
                continue;
            }
            if (!o.passer(matrix, true) || !maaLave(h, o)) {
                ci.setResult(null);
            }
            return;
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSmelt(FurnaceSmeltEvent e) {
        ItemStack kilde = e.getSource();
        if (tom(kilde)) {
            return;
        }
        for (Opskrift o : opskrifter.values()) {
            if (o.type != Opskrift.Type.FURNACE) {
                continue;
            }
            ItemStack in = o.ovnInput();
            if (in == null || in.getType() != kilde.getType() || !o.resultat.isSimilar(e.getResult())) {
                continue;
            }
            if (!Opskrift.lig(in, kilde, true)) {
                e.setCancelled(true);
            }
            return;
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        for (Opskrift o : opskrifter.values()) {
            if (maaLave(e.getPlayer(), o)) {
                opdag(e.getPlayer(), o);
            }
        }
    }

    // ═══════════════ KOMMANDO ═══════════════

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        String a1 = a.length > 0 ? a[0].toLowerCase() : "";
        boolean admin = s.hasPermission("customrecipes.admin");
        if (a1.equals("reload")) {
            if (!admin) {
                send(s, "no_permission");
                return true;
            }
            reloadConfig();
            indlaesBeskeder();
            indlaes();
            send(s, "reloaded", "{amount}", String.valueOf(opskrifter.size()));
            return true;
        }
        if (a1.equals("list")) {
            if (opskrifter.isEmpty()) {
                send(s, "no_recipes");
                return true;
            }
            for (Opskrift o : opskrifter.values()) {
                s.sendMessage(c("list_row", "{id}", o.id, "{type}", typeNavn(o.type), "{amount}", String.valueOf(o.resultat.getAmount()),
                        "{item}", o.resultat.getType().name().toLowerCase()));
            }
            return true;
        }
        if (a1.equals("delete") || a1.equals("give")) {
            if (!admin) {
                send(s, "no_permission");
                return true;
            }
            Opskrift o = a.length > 1 ? opskrifter.get(a[1]) : null;
            if (o == null) {
                send(s, "not_found");
                return true;
            }
            if (a1.equals("delete")) {
                o.afregistrer(this);
                opskrifter.remove(o.id);
                gem();
                send(s, "deleted", "{id}", o.id);
                return true;
            }
            Player maal = a.length > 2 ? Bukkit.getPlayerExact(a[2]) : (s instanceof Player ? (Player) s : null);
            if (maal == null) {
                send(s, "not_found");
                return true;
            }
            maal.getInventory().addItem(o.resultat.clone());
            send(s, "given", "{id}", o.id, "{player}", maal.getName());
            return true;
        }
        if (!(s instanceof Player)) {
            s.sendMessage("Use /" + label + " list|delete|give|reload");
            return true;
        }
        Player p = (Player) s;
        if (a1.equals("view") || !admin) {
            if (!p.hasPermission("customrecipes.view")) {
                send(p, "no_permission");
                return true;
            }
            aabnListe(p, 0, false);
            return true;
        }
        aabnListe(p, 0, true);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String label, String[] a) {
        List<String> ud = new ArrayList<String>();
        List<String> valg = new ArrayList<String>();
        if (a.length == 1) {
            valg.add("view");
            valg.add("list");
            if (s.hasPermission("customrecipes.admin")) {
                valg.addAll(Arrays.asList("delete", "give", "reload"));
            }
        } else if (a.length == 2 && (a[0].equalsIgnoreCase("delete") || a[0].equalsIgnoreCase("give"))) {
            valg.addAll(opskrifter.keySet());
        } else if (a.length == 3 && a[0].equalsIgnoreCase("give")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                valg.add(p.getName());
            }
        }
        String sidste = a.length == 0 ? "" : a[Math.subtractExact(a.length, 1)].toLowerCase();
        for (String v : valg) {
            if (v.toLowerCase().startsWith(sidste)) {
                ud.add(v);
            }
        }
        return ud;
    }
}
