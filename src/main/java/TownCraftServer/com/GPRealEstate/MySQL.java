package TownCraftServer.com.GPRealEstate;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.PluginManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

@SuppressWarnings("SqlNoDataSourceInspection")
public class MySQL implements Listener {
    private Main plugin;
    public MySQL(Main plugin){
        this.plugin = plugin;
    }


    public void createTable(){
        PreparedStatement ps;
        try{
            ps = plugin.dataStore.getConnection().prepareStatement("CREATE TABLE IF NOT EXISTS userdata " + "(USERNAME VARCHAR(100), UUID VARCHAR(100),PRIMARY KEY (USERNAME))");
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void createPlayer(Player player){
        try{
            String name = player.getName();
            UUID uuid = player.getUniqueId();
            if (!exists(uuid)){
                PreparedStatement ps2 = plugin.dataStore.getConnection().prepareStatement("INSERT IGNORE INTO userdata" + " (USERNAME,UUID) VALUES (?,?)");
                ps2.setString(1, name);
                ps2.setString(2, uuid.toString());
                ps2.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean exists(UUID uuid){
        try{
            PreparedStatement ps = plugin.dataStore.getConnection().prepareStatement("SELECT * FROM userdata WHERE UUID=?");
            ps.setString(1, uuid.toString());
            ResultSet resultSet = ps.executeQuery();
            return resultSet.next();
        }catch(SQLException e){
            e.printStackTrace();
        }
        return false;
    }

    public String getUsername(UUID uuid){
        try{
            PreparedStatement ps = plugin.dataStore.getConnection().prepareStatement("SELECT USERNAME FROM userdata WHERE UUID=?");
            ps.setString(1, uuid.toString());
            ResultSet resultSet = ps.executeQuery();
            if (resultSet.next()){
                return resultSet.getString("USERNAME");
            }
        }catch(SQLException e){
            e.printStackTrace();
        }
        return "NULL";
    }

    public void registerEvents() {
        PluginManager pm = this.plugin.getServer().getPluginManager();
        pm.registerEvents(this, this.plugin);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){
        Player player = event.getPlayer();
        String name = player.getName();
        UUID uuid = player.getUniqueId();
        createPlayer(player);
        try{
            PreparedStatement ps = plugin.dataStore.getConnection().prepareStatement("UPDATE userdata SET USERNAME=? WHERE UUID=?");
            ps.setString(1, name);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        }catch(SQLException e){
            e.printStackTrace();
        }
    }
}
