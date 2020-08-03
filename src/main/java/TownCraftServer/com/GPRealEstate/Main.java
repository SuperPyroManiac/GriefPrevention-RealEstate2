package TownCraftServer.com.GPRealEstate;

import net.md_5.bungee.api.ChatColor;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

public class Main extends JavaPlugin {

    DataStore dataStore;
    Logger log;

    // Dependencies Variables
    public static boolean vaultPresent = false;
    public static Economy econ = null;
    public static Permission perms = null;

    public void onEnable(){

        this.log = getLogger();
        dataStore = new DataStore(this);

        new GPListener(this).registerEvents();

        if (checkVault()) {

            this.log.info("Vault has been detected and enabled.");

            if (setupEconomy()) {
                this.log.info("Vault is using " + econ.getName() + " as the economy plugin.");
            } else {
                this.log.warning("No compatible economy plugin detected [Vault].");
                this.log.warning("Disabling plugin.");
                getPluginLoader().disablePlugin(this);
                return;
            }

            if (setupPermissions()) {
                this.log.info("Vault is using " + perms.getName() + " for the permissions.");
            } else {
                this.log.warning("No compatible permissions plugin detected [Vault].");
                this.log.warning("Disabling plugin.");
                getPluginLoader().disablePlugin(this);
                return;
            }

        }

        loadConfig(false);

    }

    private boolean checkVault(){
        vaultPresent = getServer().getPluginManager().getPlugin("Vault") != null;
        return vaultPresent;
    }

    private boolean setupEconomy(){
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        econ = (Economy)rsp.getProvider();
        return econ != null;
    }

    private boolean setupPermissions(){
        RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
        perms = (Permission)rsp.getProvider();
        return perms != null;
    }

    public void loadConfig(boolean reload){

        FileConfiguration config = YamlConfiguration.loadConfiguration(new File(dataStore.configFilePath));
        FileConfiguration outConfig = new YamlConfiguration();


        // Loading the config file items that exist or setting the default values.
        dataStore.cfgSignShort = config.getString("GPRealEstate.Keywords.Signs.Short", "[RE]");
        dataStore.cfgSignLong = config.getString("GPRealEstate.Keywords.Signs.Long", "[RealEstate]");

        dataStore.cfgSellKeywords = dataStore.stringToList(config.getString("GPRealEstate.Keywords.Actions.Selling", "Sell;Selling;For Sale"));
        dataStore.cfgReplaceSell = config.getString("GPRealEstate.Keywords.Actions.ReplaceSell", "FOR SALE");
        dataStore.cfgReplaceValue = config.getInt("GPRealEstate.Keywords.Actions.BuyPrice", 5);

        dataStore.cfgAllowSellingParentAC = config.getBoolean("GPRealEstate.Rules.AllowSellingParentAC", false);
        dataStore.cfgIgnoreClaimSize = config.getBoolean("GPRealEstate.Rules.IgnoreSizeLimit", false);

        if(!reload) {
            // Letting the console know the "Keywords"
            this.log.info("Signs will be using the keywords \"" + dataStore.cfgSignShort + "\" or \"" + dataStore.cfgSignLong + "\"");
        }

        // Saving the config information into the file.
        outConfig.set("GPRealEstate.Keywords.Signs.Short", dataStore.cfgSignShort);
        outConfig.set("GPRealEstate.Keywords.Signs.Long", dataStore.cfgSignLong);
        outConfig.set("GPRealEstate.Keywords.Actions.Selling", dataStore.listToString(dataStore.cfgSellKeywords));
        outConfig.set("GPRealEstate.Keywords.Actions.ReplaceSell", dataStore.cfgReplaceSell);
        outConfig.set("GPRealEstate.Keywords.Actions.BuyPrice", dataStore.cfgReplaceValue);
        outConfig.set("GPRealEstate.Rules.IgnoreSizeLimit", dataStore.cfgIgnoreClaimSize);
        outConfig.set("GPRealEstate.Rules.AllowSellingParentAC", dataStore.cfgAllowSellingParentAC);

        try {
            outConfig.save(dataStore.configFilePath);
        }
        catch(IOException exception){
            this.log.info("Unable to write to the configuration file at \"" + dataStore.configFilePath + "\"");
        }

    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if(command.getName().equalsIgnoreCase("gpre") && sender.hasPermission("gprealestate.command")){

            if(args.length == 0){
                sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&8&m======== &6GP Real Estate &8&m========"));
                sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "                &4Arguments:"));
                sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&6- &2Version | Gets the plugins version."));
                sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&6- &2Reload  | Reloads the config file."));
                sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&6- &2Help    | Shows this message."));
                sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&8&m========= &6Permissions &8&m========="));
                //TODO
                return true;
            }
            else if(args.length == 1){

                if(args[0].equalsIgnoreCase("version") && sender.hasPermission("gprealestate.admin")){
                    sender.sendMessage(dataStore.chatPrefix + ChatColor.GREEN + "You are running " + ChatColor.RED + dataStore.pdf.getName() + ChatColor.GREEN + " version " + ChatColor.RED + dataStore.pdf.getVersion());
                    return true;
                }
                else if(args[0].equalsIgnoreCase("reload") && sender.hasPermission("gprealestate.admin")){
                    loadConfig(true);
                    sender.sendMessage(dataStore.chatPrefix + ChatColor.GREEN + "The config file was succesfully reloaded.");
                    return true;
                }
                else if(args[0].equalsIgnoreCase("help")){
                    sender.sendMessage(dataStore.chatPrefix + ChatColor.GREEN + "Commands: -Permission");
                    sender.sendMessage(dataStore.chatPrefix + ChatColor.GREEN + "gpre version | -gprealestate.admin");
                    sender.sendMessage(dataStore.chatPrefix + ChatColor.GREEN + "gpre reload: | -gprealestate.admin");
                    return true;
                }
                else {
                    sender.sendMessage(dataStore.chatPrefix + ChatColor.GREEN + "Unknown. Use 'gpre help' for info");
                    return true;
                }

            }
        }
        else {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8[&6Server&8] &4You do not have permission for this!"));
            return false;
        }

        return false;

    }
}
