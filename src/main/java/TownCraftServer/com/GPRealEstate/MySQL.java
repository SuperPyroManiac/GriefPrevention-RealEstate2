package TownCraftServer.com.GPRealEstate;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
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

    static public String getStringLocation(final Location l) {
        if (l == null) {
            return "";
        }
        return l.getWorld().getName() + ":" + l.getBlockX() + ":" + l.getBlockY() + ":" + l.getBlockZ();
    }

    static public Location getLocationString(final String s) {
        if (s == null || s.trim().equals("")) {
            return null;
        }
        final String[] parts = s.split(":");
        if (parts.length == 4) {
            World w = Bukkit.getServer().getWorld(parts[0]);
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);
            return new Location(w, x, y, z);
        }
        return null;
    }

    public void createTable(){
        try{
            PreparedStatement ps = plugin.dataStore.getConnection().prepareStatement("CREATE TABLE IF NOT EXISTS gpre_userdata " + "(USERNAME VARCHAR(16), UUID VARCHAR(62),PRIMARY KEY (USERNAME))");
            ps.executeUpdate();
            PreparedStatement pss = plugin.dataStore.getConnection().prepareStatement("CREATE TABLE IF NOT EXISTS gpre_claimdata " + "(LOCATION VARCHAR(50), UUID VARCHAR(62),PRIMARY KEY (LOCATION))");
            pss.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void createPlayer(Player player){
        try{
            String name = player.getName();
            UUID uuid = player.getUniqueId();
            if (!exists(uuid)){
                PreparedStatement ps = plugin.dataStore.getConnection().prepareStatement("INSERT IGNORE INTO gpre_userdata" + " (USERNAME,UUID) VALUES (?,?)");
                ps.setString(1, name);
                ps.setString(2, uuid.toString());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void createClaim(Location location, UUID uuid){
        try{
            //if (!existsClaim(location)) {
                PreparedStatement ps = plugin.dataStore.getConnection().prepareStatement("INSERT IGNORE INTO gpre_claimdata" + " (LOCATION,UUID) VALUES (?,?)");
                ps.setString(1, getStringLocation(location));
                ps.setString(2, uuid.toString());
                ps.executeUpdate();
            //}
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean exists(UUID uuid){
        try{
            PreparedStatement ps = plugin.dataStore.getConnection().prepareStatement("SELECT * FROM gpre_userdata WHERE UUID=?");
            ps.setString(1, uuid.toString());
            ResultSet resultSet = ps.executeQuery();
            return resultSet.next();
        }catch(SQLException e){
            e.printStackTrace();
        }
        return false;
    }

    public boolean existsClaim(Location location){
        try{
            PreparedStatement ps = plugin.dataStore.getConnection().prepareStatement("SELECT * FROM gpre_claimdata WHERE LOCATION=?");
            ps.setString(1, getStringLocation(location));
            ResultSet resultSet = ps.executeQuery();
            return resultSet.next();
        }catch(SQLException e){
            e.printStackTrace();
        }
        return false;
    }

    public void deleteClaim(Location location){
        try{
            if(existsClaim(location)) {
                PreparedStatement ps = plugin.dataStore.getConnection().prepareStatement("DELETE FROM gpre_claimdata WHERE LOCATION=?");
                ps.setString(1, getStringLocation(location));
                ps.executeUpdate();
            }
        }catch(SQLException e){
            e.printStackTrace();
        }
    }

    public String getUsername(UUID uuid){
        try{
            PreparedStatement ps = plugin.dataStore.getConnection().prepareStatement("SELECT USERNAME FROM gpre_userdata WHERE UUID=?");
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

    public UUID getUUID(String name){
        try{
            PreparedStatement ps = plugin.dataStore.getConnection().prepareStatement("SELECT UUID FROM gpre_userdata WHERE USERNAME=?");
            ps.setString(1, name);
            ResultSet resultSet = ps.executeQuery();
            if (resultSet.next()){
                return UUID.fromString(resultSet.getString("UUID"));
            }
        }catch(SQLException e){
            e.printStackTrace();
        }
        return null;
    }

    public void updateUsername(Player player){
        try{
            String name = player.getName();
            UUID uuid = player.getUniqueId();
            PreparedStatement ps = plugin.dataStore.getConnection().prepareStatement("UPDATE gpre_userdata SET USERNAME=? WHERE UUID=?");
            ps.setString(1, name);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        }catch(SQLException e){
            e.printStackTrace();
        }
    }

    public void updateSignUsername(Player player){
        try{
            String name = player.getName();
            UUID uuid = player.getUniqueId();
            PreparedStatement ps = plugin.dataStore.getConnection().prepareStatement("SELECT LOCATION FROM gpre_claimdata WHERE UUID=?");
            ps.setString(1, uuid.toString());
            ResultSet resultSet = ps.executeQuery();
            while (resultSet.next()){
                Block block = getLocationString(resultSet.getString("LOCATION")).getBlock();
                if (block.getState() instanceof Sign){
                    Sign sign = (Sign) block.getState();
                    if ((sign.getLine(0).equalsIgnoreCase(ChatColor.BLUE + plugin.dataStore.cfgSignShort)) || (sign.getLine(0).equalsIgnoreCase(ChatColor.BLUE + plugin.dataStore.cfgSignLong))){
                        sign.setLine(2, name);
                    } else {
                        deleteClaim(getLocationString(resultSet.getString("LOCATION")));
                    }
                }else{
                    deleteClaim(getLocationString(resultSet.getString("LOCATION")));
                }
            }
        }catch(SQLException e){
            e.printStackTrace();
        }
    }

    public void registerEvents() {
        PluginManager pm = this.plugin.getServer().getPluginManager();
        pm.registerEvents(this, this.plugin);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){
        Player player = event.getPlayer();
        createPlayer(player);
        updateUsername(player);
        updateSignUsername(player);
    }
}
