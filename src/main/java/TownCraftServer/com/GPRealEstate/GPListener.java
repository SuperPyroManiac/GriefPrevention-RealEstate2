package TownCraftServer.com.GPRealEstate;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import net.md_5.bungee.api.ChatColor;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.PluginManager;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class GPListener implements Listener {
    private Main plugin;

    public final String pluginDirPath = "plugins" + File.separator + "GPRealEstate" + File.separator;
    public final String logFilePath = pluginDirPath + "GPRealEstate.log";

    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    Date date = new Date();

    public GPListener(Main plugin){
        this.plugin = plugin;
    }

    public void registerEvents(){
        PluginManager pm = this.plugin.getServer().getPluginManager();
        pm.registerEvents(this, this.plugin);
    }

    public void addLogEntry(String entry) {
        try {
            File logFile = new File(logFilePath);

            if (!logFile.exists()) {
                logFile.createNewFile();
            }

            FileWriter fw = new FileWriter(logFile, true);
            PrintWriter pw = new PrintWriter(fw);

            pw.println(entry);
            pw.flush();
            pw.close();
        }
        catch (IOException e) { e.printStackTrace(); }
    }

    private boolean makePayment(Player sender, OfflinePlayer reciever, Double price){
        if (!Main.econ.has(sender, price)) {
            sender.sendMessage(plugin.dataStore.chatPrefix + ChatColor.RED + "You do not have enough money!");
            return false;
        }

        EconomyResponse ecoresp = Main.econ.withdrawPlayer(sender, price);

        if (!ecoresp.transactionSuccess()) {
            sender.sendMessage(plugin.dataStore.chatPrefix + ChatColor.RED + "Could not withdraw the money!");
            return false;
        }

        if (!reciever.getName().equalsIgnoreCase("server")) {

            ecoresp = Main.econ.depositPlayer(reciever, price.doubleValue());

            if (!ecoresp.transactionSuccess()) {
                sender.sendMessage(plugin.dataStore.chatPrefix + ChatColor.RED + "Could not transfer money, refunding Player!");
                Main.econ.depositPlayer(sender, price.doubleValue());
                return false;
            }

        }

        return true;
    }

    @EventHandler 	// Player creates a sign
    public void onSignChange(SignChangeEvent event){

        // When a sign is being created..
        if((event.getLine(0).equalsIgnoreCase(plugin.dataStore.cfgSignShort)) || (event.getLine(0).equalsIgnoreCase(plugin.dataStore.cfgSignLong))){

            Player player = event.getPlayer();									// The Player
            Location location = event.getBlock().getLocation();					// The Sign Location

            GriefPrevention gp = GriefPrevention.instance;						// The GriefPrevention Instance
            Claim claim = gp.dataStore.getClaimAt(location, false, null);		// The Claim which contains the Sign.

            if (claim == null) {
                // The sign is not inside a claim.
                player.sendMessage(plugin.dataStore.chatPrefix + ChatColor.RED + "The sign you placed is not inside a claim!");
                event.setCancelled(true);
                return;
            }

            if (event.getLine(1).isEmpty()) {
                // The player did NOT enter a price on the second line.
                int newValue = plugin.dataStore.cfgReplaceValue;
                int claimValue = gp.dataStore.getClaimAt(event.getBlock().getLocation(), false, null).getArea();
                String thePrice = Integer.toString(newValue * claimValue);
                event.setLine(1, thePrice);
                addLogEntry(
                        "[" + this.dateFormat.format(this.date) + "] " + player.getName() + " has made a claim for sale at ["
                                + player.getLocation().getWorld() + ", "
                                + "X: " + player.getLocation().getBlockX() + ", "
                                + "Y: " + player.getLocation().getBlockY() + ", "
                                + "Z: " + player.getLocation().getBlockZ() + "] "
                                + "Price: " + thePrice + " " + Main.econ.currencyNamePlural()
                );
                // player.sendMessage(plugin.dataStore.chatPrefix + ChatColor.RED + "You need to enter the price on the second line!");
                // event.setCancelled(true);
                // return;
            }

            String price = event.getLine(1);

            try {
                Double.parseDouble(event.getLine(1));
            }
            catch (NumberFormatException e) {
                // Invalid input on second line, it has to be a NUMBER!
                player.sendMessage(plugin.dataStore.chatPrefix + ChatColor.RED + "The price you entered is not a valid number!");
                event.setCancelled(true);
                return;
            }

            if(claim.parent == null){
                // This is a "claim"

                if(player.getName().equalsIgnoreCase(claim.getOwnerName())){

                    if (!Main.perms.has(player, "gprealestate.claim.sell")) {
                        // The player does NOT have the correct permissions to sell claims
                        player.sendMessage(plugin.dataStore.chatPrefix + ChatColor.RED + "You do not have permission to sell claims!");
                        event.setCancelled(true);
                        return;
                    }

                    // Putting the claim up for sale!
                    event.setLine(0, plugin.dataStore.cfgSignLong);
                    event.setLine(1, ChatColor.DARK_GREEN + plugin.dataStore.cfgReplaceSell);
                    event.setLine(2, player.getName());
                    event.setLine(3, price + " " + Main.econ.currencyNamePlural());

                    player.sendMessage(plugin.dataStore.chatPrefix + ChatColor.AQUA + "You are now selling this claim for " + ChatColor.GREEN + price + " " + Main.econ.currencyNamePlural());

                    addLogEntry(
                            "[" + this.dateFormat.format(this.date) + "] " + player.getName() + " has made a claim for sale at ["
                                    + player.getLocation().getWorld() + ", "
                                    + "X: " + player.getLocation().getBlockX() + ", "
                                    + "Y: " + player.getLocation().getBlockY() + ", "
                                    + "Z: " + player.getLocation().getBlockZ() + "] "
                                    + "Price: " + price + " " + Main.econ.currencyNamePlural()
                    );

                }
                else if (claim.isAdminClaim()){
                    if (player.hasPermission("gprealestate.admin")) {
                        // This is an admin claim, we are checking if it's enabled in the config.
                        if(plugin.dataStore.cfgAllowSellingParentAC){
                            event.setLine(0, plugin.dataStore.cfgSignLong);
                            event.setLine(1, ChatColor.DARK_GREEN + plugin.dataStore.cfgReplaceSell);
                            event.setLine(2, player.getName());
                            event.setLine(3, price + " " + Main.econ.currencyNamePlural());

                            player.sendMessage(plugin.dataStore.chatPrefix + ChatColor.AQUA + "You are now selling this admin claim for " + ChatColor.GREEN + price + " " + Main.econ.currencyNamePlural());

                            addLogEntry(
                                    "[" + this.dateFormat.format(this.date) + "] " + player.getName() + " has made an admin claim for sale at "
                                            + "[" + player.getLocation().getWorld() + ", "
                                            + "X: " + player.getLocation().getBlockX() + ", "
                                            + "Y: " + player.getLocation().getBlockY() + ", "
                                            + "Z: " + player.getLocation().getBlockZ() + "] "
                                            + "Price: " + price + " " + Main.econ.currencyNamePlural());
                        }

                        else {
                            // This is a "Admin Claim" they cannot be sold unless enabled in config!
                            player.sendMessage(plugin.dataStore.chatPrefix + ChatColor.RED + "You cannot sell admin claims, they can only be leased!");
                            event.setCancelled(true);
                        }
                    } else {
                        player.sendMessage(plugin.dataStore.chatPrefix + ChatColor.RED + "You can only sell claims you own!");
                        event.setCancelled(true);
                    }
                }

                else {

                    player.sendMessage(plugin.dataStore.chatPrefix + ChatColor.RED + "You can only sell claims you own!");
                    event.setCancelled(true);


                }

            }
            else if (claim.parent.isAdminClaim()){
                if (Main.perms.has(player, "gprealestate.admin")) {
                    event.setLine(0, plugin.dataStore.cfgSignLong);
                    event.setLine(1, ChatColor.DARK_GREEN + plugin.dataStore.cfgReplaceSell);
                    event.setLine(2, player.getName());
                    event.setLine(3, price + " " + Main.econ.currencyNamePlural());

                    player.sendMessage(plugin.dataStore.chatPrefix + ChatColor.AQUA + "You are now selling access to this admin subclaim for " + ChatColor.GREEN + price + " " + Main.econ.currencyNamePlural());

                    addLogEntry(
                            "[" + this.dateFormat.format(this.date) + "] " + player.getName() + " has made an admin subclaim access for sale at "
                                    + "[" + player.getLocation().getWorld() + ", "
                                    + "X: " + player.getLocation().getBlockX() + ", "
                                    + "Y: " + player.getLocation().getBlockY() + ", "
                                    + "Z: " + player.getLocation().getBlockZ() + "] "
                                    + "Price: " + price + " " + Main.econ.currencyNamePlural());
                }
            }
            else if ((player.getName().equalsIgnoreCase(claim.parent.getOwnerName())) || (claim.managers.equals(player.getName()))) {
                // This is a "subclaim"

                if (Main.perms.has(player, "gprealestate.subclaim.sell")) {

                    String period = event.getLine(2);

                    if(period.isEmpty()){

                        // One time Leasing, player pays once for renting a claim.
                        event.setLine(0, plugin.dataStore.cfgSignLong);
                        event.setLine(1, ChatColor.DARK_GREEN + plugin.dataStore.cfgReplaceSell);
                        event.setLine(2, player.getName());
                        event.setLine(3, price + " " + Main.econ.currencyNamePlural());

                        player.sendMessage(plugin.dataStore.chatPrefix + ChatColor.AQUA + "You are now selling access to this subclaim for " + ChatColor.GREEN + price + " " + Main.econ.currencyNamePlural());

                        addLogEntry(
                                "[" + this.dateFormat.format(this.date) + "] " + player.getName() + " has made a subclaim access for sale at "
                                        + "[" + player.getLocation().getWorld() + ", "
                                        + "X: " + player.getLocation().getBlockX() + ", "
                                        + "Y: " + player.getLocation().getBlockY() + ", "
                                        + "Z: " + player.getLocation().getBlockZ() + "] "
                                        + "Price: " + price + " " + Main.econ.currencyNamePlural());

                    }
                }
                else {
                    // The player does NOT have the correct permissions to sell subclaims
                    player.sendMessage(plugin.dataStore.chatPrefix + ChatColor.RED + "You do not have permission to sell subclaims!");
                    event.setCancelled(true);
                }

            } // Second IF

        } // First IF

    }

    @SuppressWarnings("deprecation") //Future Self - If something doesn't work it's because of this! We are using usernames instead of UIID's. Which may be removed at any time.
    @EventHandler    // Player interacts with a block.
    public void onSignInteract(PlayerInteractEvent event) {

        if(event.getAction().equals(Action.RIGHT_CLICK_BLOCK)){

            Material type = event.getClickedBlock().getType();
            if ((type == Material.SPRUCE_SIGN) || (type == Material.SPRUCE_WALL_SIGN) || (type == Material.ACACIA_SIGN)
                    || (type == Material.ACACIA_WALL_SIGN) || (type == Material.BIRCH_SIGN) || (type == Material.BIRCH_WALL_SIGN)
                    || (type == Material.CRIMSON_SIGN) || (type == Material.CRIMSON_WALL_SIGN) || (type == Material.DARK_OAK_SIGN)
                    || (type == Material.DARK_OAK_WALL_SIGN) || (type == Material.JUNGLE_SIGN) || (type == Material.JUNGLE_WALL_SIGN)
                    || (type == Material.OAK_SIGN) || (type == Material.OAK_WALL_SIGN) || (type == Material.WARPED_SIGN)
                    || (type == Material.WARPED_WALL_SIGN)) {

                Sign sign = (Sign)event.getClickedBlock().getState();
                if ((sign.getLine(0).equalsIgnoreCase(plugin.dataStore.cfgSignShort)) || (sign.getLine(0).equalsIgnoreCase(plugin.dataStore.cfgSignLong))) {

                    Player player = event.getPlayer();

                    Location location = event.getClickedBlock().getLocation();

                    GriefPrevention gp = GriefPrevention.instance;
                    Claim claim = gp.dataStore.getClaimAt(location, false, null);

                    String[] delimit = sign.getLine(3).split(" ");
                    Double price = Double.valueOf(Double.valueOf(delimit[0].trim()).doubleValue());

                    String status = ChatColor.stripColor(sign.getLine(1));

                    if (claim == null){	// Sign is NOT inside a claim, breaks the sign.
                        player.sendMessage(plugin.dataStore.chatPrefix + ChatColor.RED + "This sign is no longer within a claim!");
                        event.getClickedBlock().setType(Material.AIR);
                        return;
                    }

                    if(event.getPlayer().isSneaking()){	// Player is sneaking, this is the info-tool

                        String message = "";

                        if(event.getPlayer().hasPermission("gprealestate.info")){

                            String claimType = claim.parent == null ? "claim" : "subclaim";

                            message += ChatColor.BLUE + "-----= " + ChatColor.WHITE + "[" + ChatColor.GOLD + "RealEstate Info" + ChatColor.WHITE + "]" + ChatColor.BLUE + " =-----\n";

                            if(status.equalsIgnoreCase(plugin.dataStore.cfgReplaceSell)){
                                message += ChatColor.AQUA + "This " + ChatColor.GREEN + claimType.toUpperCase() + ChatColor.AQUA + " is for sale, for " + ChatColor.GREEN + price + " " + Main.econ.currencyNamePlural() + "\n";
                                if(claimType.equalsIgnoreCase("claim")){
                                    message += ChatColor.AQUA + "The current owner is: " + ChatColor.GREEN + claim.getOwnerName();
                                }
                                else {
                                    message += ChatColor.AQUA + "The main claim owner is: " + ChatColor.GREEN + claim.getOwnerName() + "\n";
                                    message += ChatColor.LIGHT_PURPLE + "Note: " + ChatColor.AQUA + "You will only buy access to this subclaim!";
                                }
                            }
                            else {
                                message = ChatColor.RED + "Ouch! Something went wrong!";
                            }

                        }
                        else {
                            message = ChatColor.RED + "You do not have permissions to get RealEstate info!";
                        }

                        event.getPlayer().sendMessage(message);

                    }
                    else {

                        // Player is not sneaking, and wants to buy/lease the claim
                        if(claim.getOwnerName().equalsIgnoreCase(player.getName())) {
                            player.sendMessage(plugin.dataStore.chatPrefix + ChatColor.RED + "You already own this claim!");
                            return;
                        }

                        if((!sign.getLine(2).equalsIgnoreCase(claim.getOwnerName())) && (!claim.isAdminClaim())) {
                            player.sendMessage(plugin.dataStore.chatPrefix + ChatColor.RED + "The listed player does not have the rights to sell/lease this claim!");
                            event.getClickedBlock().setType(Material.AIR);
                            return;
                        }

                        if(claim.parent == null){

                            // This is a normal claim. This also includes admin claims if they are enabled in the config.
                            if (Main.perms.has(player, "gprealestate.claim.buy")) {

                                if((claim.getArea() <= gp.dataStore.getPlayerData(player.getUniqueId()).getAccruedClaimBlocks()) || player.hasPermission("Main.ignore.limit")){

                                    if(makePayment(player, Bukkit.getOfflinePlayer(sign.getLine(2)), price)){

                                        try {

                                            for (Claim child : claim.children) {
                                                child.clearPermissions();
                                                child.managers.remove(child.getOwnerName());
                                            }
                                            claim.clearPermissions();
                                            gp.dataStore.changeClaimOwner(claim, player.getUniqueId());
                                        }
                                        catch (Exception e) {
                                            e.printStackTrace();
                                            return;
                                        }
                                        if (claim.getOwnerName().equalsIgnoreCase(player.getName())) {
                                            player.sendMessage(plugin.dataStore.chatPrefix + ChatColor.AQUA + "You have successfully purchased this claim for " + ChatColor.GREEN + price + Main.econ.currencyNamePlural());
                                            addLogEntry(
                                                    "[" + this.dateFormat.format(this.date) + "] " + player.getName() + " Has purchased a claim at "
                                                            + "[" + player.getLocation().getWorld() + ", "
                                                            + "X: " + player.getLocation().getBlockX() + ", "
                                                            + "Y: " + player.getLocation().getBlockY() +", "
                                                            + "Z: " + player.getLocation().getBlockZ() + "] "
                                                            + "Price: " + price + " " + Main.econ.currencyNamePlural()
                                            );
                                        } else {
                                            player.sendMessage(plugin.dataStore.chatPrefix + ChatColor.RED + "Cannot purchase claim!");
                                        }
                                        //gp.dataStore.saveClaim(claim);
                                        event.getClickedBlock().breakNaturally();
                                    }
                                }
                                else {
                                    player.sendMessage(plugin.dataStore.chatPrefix + ChatColor.RED + "You do not have enough claim blocks available.");
                                }
                            }
                            else {
                                player.sendMessage(plugin.dataStore.chatPrefix + ChatColor.RED + "You do not have permission to buy claims!");
                            }
                        }
                        else {
                            // This is a subclaim.
                            if(status.equalsIgnoreCase(plugin.dataStore.cfgReplaceSell)){

                                if (Main.perms.has(player, "gprealestate.subclaim.buy")) {

                                    if(makePayment(player, Bukkit.getOfflinePlayer(sign.getLine(2)), price)){
                                        claim.clearPermissions();
                                        //This  is an admin subclaim
                                        if(claim.parent.isAdminClaim()) {
                                            if (player != Bukkit.getOfflinePlayer(sign.getLine(2))) {
                                                Main.econ.withdrawPlayer(Bukkit.getOfflinePlayer(sign.getLine(2)), price);
                                                claim.setPermission(player.getUniqueId().toString(), ClaimPermission.Build);	// Allowing the player to build in the subclaim!
                                                gp.dataStore.saveClaim(claim);
                                                event.getClickedBlock().breakNaturally();

                                                player.sendMessage(plugin.dataStore.chatPrefix + ChatColor.AQUA + "You have successfully purchased this admin subclaim for " + ChatColor.GREEN + price + Main.econ.currencyNamePlural());
                                                addLogEntry(
                                                        "[" + this.dateFormat.format(this.date) + "] " + player.getName() + " Has purchased an admin subclaim at "
                                                                + "[" + player.getLocation().getWorld() + ", "
                                                                + "X: " + player.getLocation().getBlockX() + ", "
                                                                + "Y: " + player.getLocation().getBlockY() + ", "
                                                                + "Z: " + player.getLocation().getBlockZ() + "] "
                                                                + "Price: " + price + " " + Main.econ.currencyNamePlural());

                                            }
                                            else {
                                                player.sendMessage(plugin.dataStore.chatPrefix + ChatColor.RED + "You can't buy the same claim you are selling!");
                                            }
                                        }
                                        else {
                                            if (!sign.getLine(2).equalsIgnoreCase("server")) {
                                                claim.managers.remove(sign.getLine(2));
                                            }

                                            claim.managers.add(player.getUniqueId().toString());							// Allowing the player to manage permissions.
                                            claim.setPermission(player.getUniqueId().toString(), ClaimPermission.Build);	// Allowing the player to build in the subclaim!
                                            gp.dataStore.saveClaim(claim);
                                            event.getClickedBlock().breakNaturally();

                                            player.sendMessage(plugin.dataStore.chatPrefix + ChatColor.AQUA + "You have successfully purchased this subclaim for " + ChatColor.GREEN + price + Main.econ.currencyNamePlural());

                                            addLogEntry(
                                                    "[" + this.dateFormat.format(this.date) + "] " + player.getName() + " Has purchased a subclaim at "
                                                            + "[" + player.getLocation().getWorld() + ", "
                                                            + "X: " + player.getLocation().getBlockX() + ", "
                                                            + "Y: " + player.getLocation().getBlockY() + ", "
                                                            + "Z: " + player.getLocation().getBlockZ() + "] "
                                                            + "Price: " + price + " " + Main.econ.currencyNamePlural()
                                            );

                                        }
                                    }

                                } else {
                                    player.sendMessage(plugin.dataStore.chatPrefix + ChatColor.RED + "You do not have permission to buy subclaims!");
                                }

                            }
                            else {
                                player.sendMessage(plugin.dataStore.chatPrefix + ChatColor.RED + "This sign was misplaced!");
                                event.getClickedBlock().setType(Material.AIR);
                            }

                        }
                    }
                }
            }
        }
    }
}
