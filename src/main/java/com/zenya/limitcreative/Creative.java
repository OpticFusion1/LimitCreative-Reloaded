package com.zenya.limitcreative;

import com.zenya.limitcreative.Updater.UpdateResult;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class Creative extends JavaPlugin implements Listener {

  private InteractionListener listener;

  @Override
  public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
    if (cmd.getName().equalsIgnoreCase("clearlore")) {
      if (sender.getName().equals("CONSOLE")) {
        sender.sendMessage(ChatColor.RED + "Shove off console");
        return true;
      }
      if (sender.hasPermission("limitcreative.clearlore")) {
        String creativeMessage = ChatColor.translateAlternateColorCodes('&', getConfig().getString("ItemMessage"))
                .replace("%Name%", "");
        ItemStack item = ((Player) sender).getItemInHand();
        if (item != null && item.getType() != Material.AIR) {
          boolean removed = false;
          if (item.hasItemMeta() && item.getItemMeta().hasLore()) {
            ItemMeta meta = item.getItemMeta();
            Iterator<String> itel = meta.getLore().iterator();
            List<String> lore = new ArrayList<>();
            while (itel.hasNext()) {
              String s = itel.next();
              if (s.startsWith(creativeMessage)) {
                removed = true;
              } else {
                lore.add(s);
              }
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
          }
          if (!removed) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', this.getConfig().getString("NoCreativeLoreOnItem")));
          } else {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', this.getConfig().getString("LoreRemoveSuccess")));
          }
        } else {
          sender.sendMessage(ChatColor.translateAlternateColorCodes('&', this.getConfig().getString("NotHoldingItem")));
        }
      } else {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', this.getConfig().getString("NoPermissionMessage")));
      }
    } else if (cmd.getName().equalsIgnoreCase("limitcreativeconvert")) {
      if (sender.hasPermission("limitcreative.convert")) {
        StorageApi.loadBlocksFromFlatfile();
        StorageApi.saveBlocksToMysql();
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', this.getConfig().getString("ConvertSQLiteToMySQL")));
      }
    }
    return true;
  }

  @Override
  public void onEnable() {

    new MetricsLite(this, 13819);
    checkForUpdate();

    File configFile = new File(this.getDataFolder(), "config.yml");
    if (!configFile.exists()) {
      saveDefaultConfig();
    }

    if (!getConfig().contains("config-version") || getConfig().getInt("config-version") != 5) {
      configFile.delete();
      saveDefaultConfig();
    }

    listener = new InteractionListener(this);
    Bukkit.getPluginManager().registerEvents(listener, this);
    StorageApi.setMainPlugin(this);
    if (getConfig().getBoolean("SaveBlocks")) {
      Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
        if (getConfig().getBoolean("UseMysql")) {
          StorageApi.setMysqlDetails(getConfig().getString("MysqlUsername"),
                  getConfig().getString("MysqlPassword"), getConfig().getString("MysqlHost"), getConfig()
                  .getString("MysqlDatabase"));
          StorageApi.loadBlocksFromMysql();
        } else {
          StorageApi.loadBlocksFromFlatfile();
        }
      }, 10);
    }
  }

  private void checkForUpdate() {
    Logger logger = getLogger();
    FileConfiguration pluginConfig = getConfig();
    Updater updater = new Updater(this, 98914, false);
    Updater.UpdateResult result = updater.getResult();
    if (result != UpdateResult.UPDATE_AVAILABLE) {
      return;
    }
    if (!pluginConfig.getBoolean("download-update")) {
      logger.info("===== UPDATE AVAILABLE ====");
      logger.info("https://www.spigotmc.org/resources/98914");
      logger.log(Level.INFO, "Installed Version: {0} New Version:{1}", new Object[]{updater.getOldVersion(), updater.getVersion()});
      logger.info("===== UPDATE AVAILABLE ====");
      return;
    }
    logger.info("==== UPDATE AVAILABLE ====");
    logger.info("====    DOWNLOADING   ====");
    updater.downloadUpdate();
  }
}
