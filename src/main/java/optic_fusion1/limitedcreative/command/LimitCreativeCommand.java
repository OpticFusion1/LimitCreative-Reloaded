package optic_fusion1.limitedcreative.command;

import com.zenya.limitcreative.Creative;
import com.zenya.limitcreative.StorageApi;
import java.util.ArrayList;
import java.util.List;
import optic_fusion1.limitedcreative.util.StringUtils;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class LimitCreativeCommand implements CommandExecutor {

    private FileConfiguration config;

    public LimitCreativeCommand(Creative creative) {
        config = creative.getConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args[0].equalsIgnoreCase("clearLore")) {
            return clearLore(sender, cmd, label, args);
        }
        if (args[0].equalsIgnoreCase("convert")) {
            return convert(sender);
        }
        return true;
    }

    private boolean convert(CommandSender sender) {
        if (!sender.hasPermission("limitcreative.convert")) {
            sender.sendMessage(StringUtils.colorize(config.getString("noPermissionMessage")));
            return true;
        }
        StorageApi.loadBlocksFromFlatfile();
        StorageApi.saveBlocksToMysql();
        sender.sendMessage(StringUtils.colorize(config.getString("convertSQLiteToMySQL")));
        return true;
    }

    private boolean clearLore(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender.getName().equals("CONSOLE")) {
            sender.sendMessage(StringUtils.colorize(config.getString("commandRanAsConsole")));
            return true;
        }
        if (!sender.hasPermission("limitcreative.clearlore")) {
            sender.sendMessage(StringUtils.colorize(config.getString("noPermissionMessage")));
            return true;
        }
        String creativeMessage = StringUtils.colorize(config.getString("itemMessage").replace("%Name%", ""));
        ItemStack itemStack = ((Player) sender).getInventory().getItemInMainHand();
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            sender.sendMessage(StringUtils.colorize(config.getString("notHoldingItem")));
            return true;
        }
        boolean removed = false;
        if (!itemStack.hasItemMeta() || !itemStack.getItemMeta().hasLore()) {
            sender.sendMessage(StringUtils.colorize(config.getString("noCreativeLoreOnItem")));
            return true;
        }
        ItemMeta itemMeta = itemStack.getItemMeta();
        List<String> newItemLore = new ArrayList<>();
        for (String line : itemMeta.getLore()) {
            if (line.startsWith(creativeMessage)) {
                removed = true;
            } else {
                newItemLore.add(line);
            }
        }
        itemMeta.setLore(newItemLore);
        itemStack.setItemMeta(itemMeta);
        if (!removed) {
            sender.sendMessage(StringUtils.colorize(config.getString("NoCreativeLoreOnItem")));
        } else {
            sender.sendMessage(StringUtils.colorize(config.getString("LoreRemoveSuccess")));
        }
        return true;
    }

}
