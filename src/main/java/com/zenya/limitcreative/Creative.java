package com.zenya.limitcreative;

import com.zenya.limitcreative.Updater.UpdateResult;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import optic_fusion1.limitedcreative.command.LimitCreativeCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class Creative extends JavaPlugin implements Listener {

    private InteractionListener listener;

    @Override
    public void onEnable() {

        new MetricsLite(this, 13819);
        checkForUpdate();
        
        PluginCommand command = Bukkit.getPluginCommand("limitcreative");
        command.setExecutor(new LimitCreativeCommand(this));

        File configFile = new File(this.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            saveDefaultConfig();
        }

        if (!getConfig().contains("config-version") || getConfig().getInt("config-version") != 7) {
            configFile.delete();
            saveDefaultConfig();
        }

        listener = new InteractionListener(this);
        Bukkit.getPluginManager().registerEvents(listener, this);
        StorageApi.setMainPlugin(this);
        if (getConfig().getBoolean("saveBlocks")) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
                if (getConfig().getBoolean("useMysql")) {
                    StorageApi.setMysqlDetails(getConfig().getString("mysqlUsername"),
                            getConfig().getString("mysqlPassword"), getConfig().getString("mysqlHost"), getConfig()
                            .getString("mysqlDatabase"));
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
