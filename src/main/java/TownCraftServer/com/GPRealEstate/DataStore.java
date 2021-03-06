package TownCraftServer.com.GPRealEstate;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.plugin.PluginDescriptionFile;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public class DataStore {
    // Plugin File Paths
    public final String pluginDirPath = "plugins" + File.separator + "GPRealEstate" + File.separator;
    public final String configFilePath = pluginDirPath + "config.yml";
    //Config options
    public final String chatPrefix = ChatColor.translateAlternateColorCodes('&', "&8[&6GPRealEstate&8] &f");
    // Plugin Description File (plugin.yml) access.
    public PluginDescriptionFile pdf;
    public String cfgSignShort;
    public String cfgSignLong;
    public List<String> cfgSellKeywords;
    public String cfgReplaceSell;
    public int cfgReplaceValue;
    public boolean cfgIgnoreClaimSize;
    public boolean cfgAllowSellingParentAC;
    //MySQL Access
    public String cfgHost;
    public String cfgPort;
    public String cfgDatabase;
    public String cfgUsername;
    public String cfgPassword;
    public String cfgSSL;
    public Connection connection;
    Main plugin;

    public DataStore(Main plugin) {
        this.plugin = plugin;
        this.pdf = this.plugin.getDescription();
    }

    public List<String> stringToList(String input) {
        String[] array = input.matches("([;+])") ? input.split(";") : new String[]{input};
        return Arrays.asList(array);
    }

    public String listToString(List<String> input) {
        String string = "";
        int count = 1;
        for (Object str : input.toArray()) {
            if (count != 1) {
                count++;
                string += ";";
            }
            string += str.toString();
        }
        return string;
    }

    public Boolean isConnected(){
        return(connection == null ? false : true);
    }

    public void connect() throws ClassNotFoundException, SQLException {
        if (!isConnected()){
            connection = DriverManager.getConnection("jdbc:mysql://" + cfgHost + ":" + cfgPort + "/" + cfgDatabase + "?useSSL=" + cfgSSL, cfgUsername, cfgPassword);
        }
    }

    public void disconnect(){
        if (isConnected()){
            try{
                connection.close();
            } catch(SQLException e){
                e.printStackTrace();
            }
        }
    }

    public Connection getConnection(){
        return connection;
    }
}
