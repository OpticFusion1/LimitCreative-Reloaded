package com.zenya.limitcreative;

import com.google.common.base.Objects;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class InteractionListener implements Listener {

    private final String creativeMessage;
    private final ArrayList<Material> disallowedItems = new ArrayList<>();
    private final List<String> disallowedWorlds;
    private final List<String> blacklistedCommands;
    private final JavaPlugin plugin;

    public InteractionListener(JavaPlugin plugin) {
        this.plugin = plugin;
        creativeMessage = ChatColor.translateAlternateColorCodes('&', getConfig().getString("itemMessage"));
        disallowedWorlds = getConfig().getStringList("worldsDisabled");
        blacklistedCommands = getConfig().getStringList("blacklistedCommands");
        for (String disallowed : getConfig().getStringList("disabledItems")) {
            try {
                disallowedItems.add(Material.valueOf(disallowed.toUpperCase()));
            } catch (Exception ex) {

            }
        }
    }

    private boolean checkEntity(Entity entity) {
        return getConfig().getBoolean("preventUsage") && entity != null && entity instanceof Player
                && ((Player) entity).getGameMode() != GameMode.CREATIVE && isCreativeItem(((Player) entity).getItemInHand());
    }

    private FileConfiguration getConfig() {
        return plugin.getConfig();
    }

    private String getCreativeString(ItemStack item) {
        if (item != null && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta.hasLore()) {
                for (String s : meta.getLore()) {
                    if (s.startsWith(creativeMessage.replace("%Name%", ""))) {
                        return s;
                    }
                }
            }
        }
        return null;
    }

    private boolean isCreativeItem(ItemStack item) {
        if (item != null && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta.hasLore()) {
                for (String s : meta.getLore()) {
                    if (s.startsWith(creativeMessage.replace("%Name%", ""))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!player.getGameMode().equals(GameMode.CREATIVE)) {
            return;
        }
        if (player.hasPermission("limitcreative.bypasscmd")) {
            return;
        }
        String command = event.getMessage().replace("/", "");
        for (String cmd : blacklistedCommands) {
            if (command.startsWith(cmd) || cmd.equals("*")) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("disallowedCommandMessage")));
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        if (disallowedWorlds.contains(event.getEntity().getWorld().getName())) {
            return;
        }
        if (checkEntity(event.getDamager())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled() || disallowedWorlds.contains(event.getBlock().getWorld().getName())) {
            return;
        }
        if (getConfig().getBoolean("markBlocks")
                && (isCreativeItem(event.getItemInHand()) || event.getPlayer().getGameMode() == GameMode.CREATIVE)) {
            if (!event.getPlayer().hasPermission("limitcreative.nolore")) {
                StorageApi.markBlock(event.getBlockPlaced(), getCreativeString(event.getItemInHand()));
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBreak(BlockBreakEvent event) {
        if (event.isCancelled() || disallowedWorlds.contains(event.getBlock().getWorld().getName())) {
            return;
        }

        if (isCreativeItem(event.getPlayer().getItemInHand()) && event.getPlayer().getGameMode() == GameMode.SURVIVAL) {
            event.setCancelled(true);
            event.setExpToDrop(0);
        }

        if (StorageApi.isMarked(event.getBlock())) {
            String message = StorageApi.unmarkBlock(event.getBlock());
            if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
                event.setExpToDrop(0);
                Collection<ItemStack> drops = event.getBlock().getDrops(event.getPlayer().getItemInHand());
                for (ItemStack item : drops) {
                    ItemMeta meta = item.getItemMeta();
                    List<String> lore = new ArrayList<>();
                    if (meta.hasLore()) {
                        lore = meta.getLore();
                    }
                    lore.add(0, message);
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                    event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation().clone().add(0.5, 0, 0.5), item);
                }
                event.getBlock().setType(Material.AIR);
            }
        }
    }

    @EventHandler
    public void onBrew(BrewEvent event) {
        if (disallowedWorlds.contains(event.getBlock().getWorld().getName())) {
            return;
        }
        if (isCreativeItem(event.getContents().getIngredient())) {
            List<String> lore = event.getContents().getIngredient().getItemMeta().getLore();
            ItemStack[] items = event.getContents().getContents();
            for (ItemStack item : items) {
                if (item != null && item.getItemMeta() != null) {
                    ItemMeta meta = item.getItemMeta();
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                }
            }
            event.getContents().setContents(items);
        }
    }

    @EventHandler
    public void onCraft(PrepareItemCraftEvent event) {
        if (event.getViewers().isEmpty() || disallowedWorlds.contains(event.getViewers().get(0).getWorld().getName())) {
            return;
        }
        for (ItemStack item : event.getInventory().getMatrix()) {
            if (isCreativeItem(item)) {
                if (event.getViewers().get(0).getGameMode() != GameMode.CREATIVE && getConfig().getBoolean("PreventCrafting")) {
                    event.getInventory().setItem(0, new ItemStack(Material.AIR));
                } else if (getConfig().getBoolean("renameCrafting")) {
                    setCreativeItem(event.getViewers().get(0).getName(), event.getInventory().getItem(0));
                }
                break;
            }
        }
    }

    @EventHandler
    public void on(PlayerJoinEvent event) {
        if (getConfig().getBoolean("preventIllegalItems")) {
            for (ItemStack item : event.getPlayer().getInventory()) {
                if (isIllegalPlayerHead(item)) {
                    event.getPlayer().getInventory().remove(item);
                }
            }
            for (ItemStack item : event.getPlayer().getEnderChest()) {
                if (isIllegalPlayerHead(item)) {
                    event.getPlayer().getInventory().remove(item);
                }
            }
            for (ItemStack item : event.getPlayer().getEquipment().getArmorContents()) {
                if (isIllegalPlayerHead(item)) {
                    event.getPlayer().getInventory().remove(item);
                }
            }
        }
    }

    @EventHandler
    public void onCreativeClick(InventoryCreativeEvent event) {
        if (disallowedWorlds.contains(event.getWhoClicked().getWorld().getName())) {
            return;
        }

        event.setCursor(setCreativeItem(event.getWhoClicked().getName(), event.getCursor()));

        if (getConfig().getBoolean("preventIllegalItems") && isIllegalPlayerHead(event.getCursor())) {
            event.setCancelled(true);
            return;
        }

        if (disallowedItems.contains(event.getCursor().getType())) {
            if (!event.getWhoClicked().hasPermission("limitcreative.useblacklistitems")) {

                event.setCancelled(true);
                if (event.getWhoClicked() instanceof Player player) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("disabledItemMessage")));
                }
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (disallowedWorlds.contains(event.getEntity().getWorld().getName())) {
            return;
        }
        if (getConfig().getBoolean("preventArmor")) {
            if (event.getEntity() instanceof Player && ((Player) event.getEntity()).getGameMode() != GameMode.CREATIVE) {
                ItemStack[] items = ((Player) event.getEntity()).getInventory().getArmorContents();
                for (int i = 0; i < 4; i++) {
                    ItemStack item = items[i];
                    if (isCreativeItem(item)) {
                        items[i] = new ItemStack(Material.AIR);
                        HashMap<Integer, ItemStack> leftovers = ((Player) event.getEntity()).getInventory().addItem(item);
                        for (ItemStack leftoverItem : leftovers.values()) {
                            ((Player) event.getEntity()).getWorld().dropItem(((Player) event.getEntity()).getEyeLocation(),
                                    leftoverItem);
                        }
                    }
                }
                ((HumanEntity) event.getEntity()).getInventory().setArmorContents(items);
            }
        }
    }

    @EventHandler
    public void onEnchant(PrepareItemEnchantEvent event) {
        if (this.isCreativeItem(event.getItem()) && event.getEnchanter().getGameMode() != GameMode.CREATIVE) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (disallowedWorlds.contains(event.getLocation().getWorld().getName())) {
            return;
        }
        for (Block block : event.blockList()) {
            if (StorageApi.isMarked(block)) {
                String message = StorageApi.unmarkBlock(block);
                for (ItemStack item : block.getDrops()) {
                    ItemMeta meta = item.getItemMeta();
                    List<String> lore = new ArrayList<>();
                    if (meta.hasLore()) {
                        lore = meta.getLore();
                    }
                    lore.add(0, message);
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                    block.getWorld().dropItemNaturally(block.getLocation().clone().add(0.5, 0, 0.5), item);
                }
                block.setType(Material.AIR);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (disallowedWorlds.contains(event.getBlock().getLocation().getWorld().getName())) {
            return;
        }
        for (Block block : event.blockList()) {
            if (StorageApi.isMarked(block)) {
                String message = StorageApi.unmarkBlock(block);
                for (ItemStack item : block.getDrops()) {
                    ItemMeta meta = item.getItemMeta();
                    List<String> lore = new ArrayList<>();
                    if (meta.hasLore()) {
                        lore = meta.getLore();
                    }
                    lore.add(0, message);
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                    block.getWorld().dropItemNaturally(block.getLocation().clone().add(0.5, 0, 0.5), item);
                }
                block.setType(Material.AIR);
            }
        }
    }

    @EventHandler
    public void onGameModeSwitch(PlayerGameModeChangeEvent event) {
        if (disallowedWorlds.contains(event.getPlayer().getWorld().getName())) {
            return;
        }
        if (getConfig().getBoolean("preventArmor") && event.getPlayer().getGameMode() == GameMode.CREATIVE
                && event.getNewGameMode() != GameMode.CREATIVE) {
            ItemStack[] items = event.getPlayer().getInventory().getArmorContents();
            for (int i = 0; i < 4; i++) {
                ItemStack item = items[i];
                if (isCreativeItem(item)) {
                    items[i] = new ItemStack(Material.AIR);
                    HashMap<Integer, ItemStack> leftovers = event.getPlayer().getInventory().addItem(item);
                    for (ItemStack leftoverItem : leftovers.values()) {
                        event.getPlayer().getWorld().dropItem(event.getPlayer().getEyeLocation(), leftoverItem);
                    }
                }
            }
            event.getPlayer().getInventory().setArmorContents(items);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (disallowedWorlds.contains(event.getPlayer().getWorld().getName())) {
            return;
        }
        if (checkEntity(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (disallowedWorlds.contains(event.getPlayer().getWorld().getName())) {
            return;
        }
        if (checkEntity(event.getPlayer())) {
            event.setCancelled(true);
        }
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE && event.getRightClicked() instanceof ItemFrame) {
            ItemStack item = event.getPlayer().getItemInHand();
            if (item != null && item.getType() != Material.AIR && !isCreativeItem(item)) {
                ItemFrame frame = (ItemFrame) event.getRightClicked();
                if (frame.getItem() == null || frame.getItem().getType() == Material.AIR) {
                    event.getPlayer().setItemInHand(setCreativeItem(event.getPlayer().getName(), item));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClick(InventoryClickEvent event) {
        if (disallowedWorlds.contains(event.getWhoClicked().getWorld().getName())) {
            return;
        }
        if (event.getWhoClicked().getGameMode() != GameMode.CREATIVE && event.getInventory().getType() == InventoryType.ANVIL
                && isCreativeItem(event.getCurrentItem())
                || getConfig().getBoolean("preventIllegalItems") && isIllegalPlayerHead(event.getCurrentItem())) {
            if (getConfig().getBoolean("preventAnvil")) {
                event.setCancelled(true);
            }
        }
        if (event.getWhoClicked().getGameMode() == GameMode.CREATIVE && event.getAction() == InventoryAction.CLONE_STACK
                && !isCreativeItem(event.getCurrentItem())) {
            ItemStack item = event.getCurrentItem();
            if (item != null && item.getType() != Material.AIR) {
                item = setCreativeItem(event.getWhoClicked().getName(), event.getCurrentItem().clone());
                item.setAmount(item.getMaxStackSize());
                event.getWhoClicked().setItemOnCursor(item);
                event.setCancelled(true);
            }
        }

        /**
         * if(getConfig().getBoolean("PreventTransfer")) { Inventory top = event.getView().getTopInventory(); Inventory bottom = event.getView().getBottomInventory();
         *
         * if (!Objects.equal(top, bottom)) { if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY || event.getAction() == InventoryAction.DROP_ALL_CURSOR || event.getAction() == InventoryAction.DROP_ONE_CURSOR || event.getAction() == InventoryAction.DROP_ALL_SLOT || event.getAction() == InventoryAction.DROP_ONE_SLOT) { event.setCancelled(true); } } }*
         */
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryDragEvent(InventoryDragEvent event) {
        if (disallowedWorlds.contains(event.getWhoClicked().getWorld().getName())) {
            return;
        }

        if (getConfig().getBoolean("preventTransfer")) {
            Inventory top = event.getView().getTopInventory();
            Inventory bottom = event.getView().getBottomInventory();

            if (!Objects.equal(top, bottom)) {
                if (event.getOldCursor() != null && event.getOldCursor().getType() != Material.AIR) {
                    if (isCreativeItem(event.getOldCursor())
                            || getConfig().getBoolean("preventIllegalItems") && isIllegalPlayerHead(event.getOldCursor())) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        if (disallowedWorlds.contains(event.getPlayer().getWorld().getName())) {
            return;
        }
        if (getConfig().getBoolean("preventIllegalItems") && isIllegalPlayerHead(event.getItem().getItemStack())) {
            event.setCancelled(true);
            return;
        }
        if (!event.getPlayer().hasPermission("limitcreative.pickupitem")) {
            if (event.getPlayer().getGameMode() != GameMode.CREATIVE && isCreativeItem(event.getItem().getItemStack())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDrop(PlayerDropItemEvent event) {
        if (disallowedWorlds.contains(event.getPlayer().getWorld().getName())) {
            return;
        }

        if (getConfig().getBoolean("preventDrop")) {
            ItemStack itemStack = event.getItemDrop().getItemStack();
            if (isCreativeItem(itemStack) || getConfig().getBoolean("preventIllegalItems")
                    && isIllegalPlayerHead(itemStack)) {
                event.getItemDrop().remove();
            }
        }

    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPush(BlockPistonExtendEvent event) {
        if (disallowedWorlds.contains(event.getBlock().getWorld().getName())) {
        }
        for (Block block : event.getBlocks()) {
            if (StorageApi.isMarked(block)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRetract(BlockPistonRetractEvent event) {
        if (disallowedWorlds.contains(event.getBlock().getWorld().getName())) {
            return;
        }
        if (event.isSticky()) {
            Block block = event.getBlock().getRelative(event.getDirection()).getRelative(event.getDirection());
            if (StorageApi.isMarked(block)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onSmelt(FurnaceSmeltEvent event) {
        if (this.isCreativeItem(event.getSource())
                || isCreativeItem(((Furnace) event.getBlock().getState()).getInventory().getFuel())) {
            event.setCancelled(true);
        }
    }

    private ItemStack setCreativeItem(String who, ItemStack item) {
        if (item != null && item.getType() != Material.AIR && item.getType() != Material.WRITABLE_BOOK) {
            if (Bukkit.getServer().getPlayer(who) != null && Bukkit.getServer().getPlayer(who).hasPermission("limitcreative.nolore")) {
                return item;
            }
            if (!isCreativeItem(item)) {
                ItemMeta meta = item.getItemMeta();
                List<String> lore = new ArrayList<>();
                if (meta.hasLore()) {
                    lore = meta.getLore();
                }
                lore.add(0, creativeMessage.replace("%Name%", who));
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
        }
        return item;
    }

    private boolean isIllegalPlayerHead(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() != Material.PLAYER_HEAD) {
            return false;
        }
        SkullMeta meta = (SkullMeta) itemStack.getItemMeta();
        OfflinePlayer player = meta.getOwningPlayer();
        return player.getUniqueId().toString().equals("[]");
    }

}
