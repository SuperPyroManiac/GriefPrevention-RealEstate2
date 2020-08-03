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

    public final String pluginDirPath = "plugins" + File.separator + "GPRealEstate" + File.separator;
    public final String logFilePath = pluginDirPath + "GPRealEstate.log";
    private final Main plugin;
    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    Date date = new Date();

    public GPListener(Main plugin) {
        this.plugin = plugin;
    }

    public void registerEvents() {
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean makePayment(Player sender, OfflinePlayer reciever, Double price) {
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
                Main.econ.depositPlayer(sender, price);
                return false;
            }
        }
        return true;
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        if ((event.getLine(0).equalsIgnoreCase(plugin.dataStore.cfgSignShort)) || (event.getLine(0).equalsIgnoreCase(plugin.dataStore.cfgSignLong))) {

            Player player = event.getPlayer();
            Location location = event.getBlock().getLocation();
            GriefPrevention gp = GriefPrevention.instance;
            Claim claim = gp.dataStore.getClaimAt(location, false, null);

            if (claim == null) {
                player.sendMessage(plugin.dataStore.chatPrefix + ChatColor.RED + "The sign you placed is not inside a claim!");
                event.setCancelled(true);
                return;
            }

            if (event.getLine(1).isEmpty()) {
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
            }
            String price = event.getLine(1);
            try {
                Double.parseDouble(event.getLine(1));
            } catch (NumberFormatException e) {
                player.sendMessage(plugin.dataStore.chatPrefix + ChatColor.RED + "The price you entered is not a valid number!");
                event.setCancelled(true);
                return;
            }
            if (claim.parent == null) {
                if (player.getName().equalsIgnoreCase(claim.getOwnerName())) {
                    if (!Main.perms.has(player, "gprealestate.claim.sell")) {
                        player.sendMessage(plugin.dataStore.chatPrefix + ChatColor.RED + "You do not have permission to sell claims!");
                        event.setCancelled(true);
                        return;
                    }
                    event.setLine(0, ChatColor.BLUE + plugin.dataStore.cfgSignLong);
                    event.setLine(1, ChatColor.DARK_GREEN + plugin.dataStore.cfgReplaceSell);
                    event.setLine(2, player.getName());
                    event.setLine(3, price + " " + Main.econ.currencyNamePlural());
                    player.sendMessage(plugin.dataStore.chatPrefix + ChatColor.BLUE + "You are now selling this claim for " + ChatColor.GREEN + price + " " + Main.econ.currencyNamePlural());
                    addLogEntry(
                            "[" + this.dateFormat.format(this.date) + "] " + player.getName() + " has made a claim for sale at ["
                                    + player.getLocation().getWorld() + ", "
                                    + "X: " + player.getLocation().getBlockX() + ", "
                                    + "Y: " + player.getLocation().getBlockY() + ", "
                                    + "Z: " + player.getLocation().getBlockZ() + "] "
                                    + "Price: " + price + " " + Main.econ.currencyNamePlural()
                    );
                } else if (claim.isAdminClaim()) {
                    if (player.hasPermission("gprealestate.admin")) {
                        if (plugin.dataStore.cfgAllowSellingParentAC) {
                            event.setLine(0, ChatColor.BLUE + plugin.dataStore.cfgSignLong);
                            event.setLine(1, ChatColor.DARK_GREEN + plugin.dataStore.cfgReplaceSell);
                            event.setLine(2, player.getName());
                            event.setLine(3, price + " " + Main.econ.currencyNamePlural());
                            player.sendMessage(plugin.dataStore.chatPrefix + ChatColor.BLUE + "You are now selling this admin claim for " + ChatColor.GREEN + price + " " + Main.econ.currencyNamePlural());
                            addLogEntry(
                                    "[" + this.dateFormat.format(this.date) + "] " + player.getName() + " has made an admin claim for sale at "
                                            + "[" + player.getLocation().getWorld() + ", "
                                            + "X: " + player.getLocation().getBlockX() + ", "
                                            + "Y: " + player.getLocation().getBlockY() + ", "
                                            + "Z: " + player.getLocation().getBlockZ() + "] "
                                            + "Price: " + price + " " + Main.econ.currencyNamePlural());
                        } else {
                            player.sendMessage(plugin.dataStore.chatPrefix + ChatColor.RED + "You cannot sell admin claims!");
                            event.setCancelled(true);
                        }
                    } else {
                        player.sendMessage(plugin.dataStore.chatPrefix + ChatColor.RED + "You can only sell claims you own!");
                        event.setCancelled(true);
                    }
                } else {
                    player.sendMessage(plugin.dataStore.chatPrefix + ChatColor.RED + "You can only sell claims you own!");
                    event.setCancelled(true);
                }
            } else if (claim.parent.isAdminClaim()) {
                if (Main.perms.has(player, "gprealestate.admin")) {
                    event.setLine(0, ChatColor.BLUE + plugin.dataStore.cfgSignLong);
                    event.setLine(1, ChatColor.DARK_GREEN + plugin.dataStore.cfgReplaceSell);
                    event.setLine(2, player.getName());
                    event.setLine(3, price + " " + Main.econ.currencyNamePlural());
                    player.sendMessage(plugin.dataStore.chatPrefix + ChatColor.BLUE + "You are now selling access to this admin subclaim for " + ChatColor.GREEN + price + " " + Main.econ.currencyNamePlural());
                    addLogEntry(
                            "[" + this.dateFormat.format(this.date) + "] " + player.getName() + " has made an admin subclaim access for sale at "
                                    + "[" + player.getLocation().getWorld() + ", "
                                    + "X: " + player.getLocation().getBlockX() + ", "
                                    + "Y: " + player.getLocation().getBlockY() + ", "
                                    + "Z: " + player.getLocation().getBlockZ() + "] "
                                    + "Price: " + price + " " + Main.econ.currencyNamePlural());
                }
            } else if ((player.getName().equalsIgnoreCase(claim.parent.getOwnerName())) || (claim.managers.equals(player.getName()))) {
                if (Main.perms.has(player, "gprealestate.subclaim.sell")) {
                    String period = event.getLine(2);
                    if (period.isEmpty()) {
                        event.setLine(0, ChatColor.BLUE + plugin.dataStore.cfgSignLong);
                        event.setLine(1, ChatColor.DARK_GREEN + plugin.dataStore.cfgReplaceSell);
                        event.setLine(2, player.getName());
                        event.setLine(3, price + " " + Main.econ.currencyNamePlural());
                        player.sendMessage(plugin.dataStore.chatPrefix + ChatColor.BLUE + "You are now selling access to this subclaim for " + ChatColor.GREEN + price + " " + Main.econ.currencyNamePlural());
                        addLogEntry(
                                "[" + this.dateFormat.format(this.date) + "] " + player.getName() + " has made a subclaim access for sale at "
                                        + "[" + player.getLocation().getWorld() + ", "
                                        + "X: " + player.getLocation().getBlockX() + ", "
                                        + "Y: " + player.getLocation().getBlockY() + ", "
                                        + "Z: " + player.getLocation().getBlockZ() + "] "
                                        + "Price: " + price + " " + Main.econ.currencyNamePlural());
                    }
                } else {
                    // The player does NOT have the correct permissions to sell subclaims
                    player.sendMessage(plugin.dataStore.chatPrefix + ChatColor.RED + "You do not have permission to sell subclaims!");
                    event.setCancelled(true);
                }
            }
        }
    }
    //TODO: - If something doesn't work it's because of this! We are using usernames instead of UIID's. Which may be removed at any time.
    @EventHandler
    public void onSignInteract(PlayerInteractEvent event) {
        if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            Material type = event.getClickedBlock().getType();
            if ((type == Material.SPRUCE_SIGN) || (type == Material.SPRUCE_WALL_SIGN) || (type == Material.ACACIA_SIGN)
                    || (type == Material.ACACIA_WALL_SIGN) || (type == Material.BIRCH_SIGN) || (type == Material.BIRCH_WALL_SIGN)
                    || (type == Material.CRIMSON_SIGN) || (type == Material.CRIMSON_WALL_SIGN) || (type == Material.DARK_OAK_SIGN)
                    || (type == Material.DARK_OAK_WALL_SIGN) || (type == Material.JUNGLE_SIGN) || (type == Material.JUNGLE_WALL_SIGN)
                    || (type == Material.OAK_SIGN) || (type == Material.OAK_WALL_SIGN) || (type == Material.WARPED_SIGN)
                    || (type == Material.WARPED_WALL_SIGN)) {
                Sign sign = (Sign) event.getClickedBlock().getState();
                if ((sign.getLine(0).equalsIgnoreCase(plugin.dataStore.cfgSignShort)) || (sign.getLine(0).equalsIgnoreCase(plugin.dataStore.cfgSignLong))) {
                    Player player = event.getPlayer();
                    Location location = event.getClickedBlock().getLocation();
                    GriefPrevention gp = GriefPrevention.instance;
                    Claim claim = gp.dataStore.getClaimAt(location, false, null);
                    String[] delimit = sign.getLine(3).split(" ");
                    Double price = Double.valueOf(Double.valueOf(delimit[0].trim()).doubleValue());
                    String status = ChatColor.stripColor(sign.getLine(1));
                    if (claim == null) {
                        player.sendMessage(plugin.dataStore.chatPrefix + ChatColor.RED + "This sign is no longer within a claim!");
                        event.getClickedBlock().setType(Material.AIR);
                        return;
                    }
                    if (event.getPlayer().isSneaking()) {
                        String message = "";
                        if (event.getPlayer().hasPermission("gprealestate.info")) {
                            String claimType = claim.parent == null ? "claim" : "subclaim";
                            message += ChatColor.translateAlternateColorCodes('&', "&8&m======== &6GP Real Estate Info &8&m========\n");
                            if (status.equalsIgnoreCase(plugin.dataStore.cfgReplaceSell)) {
                                message += ChatColor.BLUE + "This " + ChatColor.GREEN + claimType.toUpperCase() + ChatColor.BLUE + " is for sale, for " + ChatColor.GREEN + price + " " + Main.econ.currencyNamePlural() + "\n";
                                if (claimType.equalsIgnoreCase("claim")) {
                                    message += ChatColor.BLUE + "The current owner is: " + ChatColor.GREEN + claim.getOwnerName();
                                } else {
                                    message += ChatColor.BLUE + "The main claim owner is: " + ChatColor.GREEN + claim.getOwnerName() + "\n";
                                    message += ChatColor.LIGHT_PURPLE + "Note: " + ChatColor.BLUE + "You will only buy access to this subclaim!";
                                }
                            } else {
                                message = plugin.dataStore.chatPrefix + ChatColor.RED + "Ouch! Something went wrong!";
                            }
                        } else {
                            message = plugin.dataStore.chatPrefix + ChatColor.RED + "You do not have permissions to get RealEstate info!";
                        }
                        event.getPlayer().sendMessage(message);
                    } else {
                        if (claim.getOwnerName().equalsIgnoreCase(player.getName())) {
                            player.sendMessage(plugin.dataStore.chatPrefix + ChatColor.RED + "You already own this claim!");
                            return;
                        }
                        if ((!sign.getLine(2).equalsIgnoreCase(claim.getOwnerName())) && (!claim.isAdminClaim())) {
                            player.sendMessage(plugin.dataStore.chatPrefix + ChatColor.RED + "The listed player does not have the rights to sell this claim!");
                            event.getClickedBlock().setType(Material.AIR);
                            return;
                        }
                        if (claim.parent == null) {
                            if (Main.perms.has(player, "gprealestate.claim.buy")) {
                                if ((claim.getArea() <= gp.dataStore.getPlayerData(player.getUniqueId()).getAccruedClaimBlocks()) || player.hasPermission("Main.ignore.limit")) {
                                    if (makePayment(player, Bukkit.getOfflinePlayer(sign.getLine(2)), price)) {
                                        try {
                                            for (Claim child : claim.children) {
                                                child.clearPermissions();
                                                child.managers.remove(child.getOwnerName());
                                            }
                                            claim.clearPermissions();
                                            gp.dataStore.changeClaimOwner(claim, player.getUniqueId());
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            return;
                                        }
                                        if (claim.getOwnerName().equalsIgnoreCase(player.getName())) {
                                            player.sendMessage(plugin.dataStore.chatPrefix + ChatColor.BLUE + "You have successfully purchased this claim for " + ChatColor.GREEN + price + Main.econ.currencyNamePlural());
                                            addLogEntry(
                                                    "[" + this.dateFormat.format(this.date) + "] " + player.getName() + " Has purchased a claim at "
                                                            + "[" + player.getLocation().getWorld() + ", "
                                                            + "X: " + player.getLocation().getBlockX() + ", "
                                                            + "Y: " + player.getLocation().getBlockY() + ", "
                                                            + "Z: " + player.getLocation().getBlockZ() + "] "
                                                            + "Price: " + price + " " + Main.econ.currencyNamePlural()
                                            );
                                        } else {
                                            player.sendMessage(plugin.dataStore.chatPrefix + ChatColor.RED + "Cannot purchase claim!");
                                        }
                                        event.getClickedBlock().breakNaturally();
                                    }
                                } else {
                                    player.sendMessage(plugin.dataStore.chatPrefix + ChatColor.RED + "You do not have enough claim blocks available.");
                                }
                            } else {
                                player.sendMessage(plugin.dataStore.chatPrefix + ChatColor.RED + "You do not have permission to buy claims!");
                            }
                        } else {
                            if (status.equalsIgnoreCase(plugin.dataStore.cfgReplaceSell)) {
                                if (Main.perms.has(player, "gprealestate.subclaim.buy")) {
                                    if (makePayment(player, Bukkit.getOfflinePlayer(sign.getLine(2)), price)) {
                                        claim.clearPermissions();
                                        //This  is an admin subclaim
                                        if (claim.parent.isAdminClaim()) {
                                            if (player != Bukkit.getOfflinePlayer(sign.getLine(2))) {
                                                Main.econ.withdrawPlayer(Bukkit.getOfflinePlayer(sign.getLine(2)), price);
                                                claim.setPermission(player.getUniqueId().toString(), ClaimPermission.Build);
                                                gp.dataStore.saveClaim(claim);
                                                event.getClickedBlock().breakNaturally();
                                                player.sendMessage(plugin.dataStore.chatPrefix + ChatColor.BLUE + "You have successfully purchased this admin subclaim for " + ChatColor.GREEN + price + Main.econ.currencyNamePlural());
                                                addLogEntry(
                                                        "[" + this.dateFormat.format(this.date) + "] " + player.getName() + " Has purchased an admin subclaim at "
                                                                + "[" + player.getLocation().getWorld() + ", "
                                                                + "X: " + player.getLocation().getBlockX() + ", "
                                                                + "Y: " + player.getLocation().getBlockY() + ", "
                                                                + "Z: " + player.getLocation().getBlockZ() + "] "
                                                                + "Price: " + price + " " + Main.econ.currencyNamePlural());
                                            } else {
                                                player.sendMessage(plugin.dataStore.chatPrefix + ChatColor.RED + "You can't buy the same claim you are selling!");
                                            }
                                        } else {
                                            if (!sign.getLine(2).equalsIgnoreCase("server")) {
                                                claim.managers.remove(sign.getLine(2));
                                            }
                                            claim.managers.add(player.getUniqueId().toString());
                                            claim.setPermission(player.getUniqueId().toString(), ClaimPermission.Build);
                                            gp.dataStore.saveClaim(claim);
                                            event.getClickedBlock().breakNaturally();
                                            player.sendMessage(plugin.dataStore.chatPrefix + ChatColor.BLUE + "You have successfully purchased this subclaim for " + ChatColor.GREEN + price + Main.econ.currencyNamePlural());
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
                            } else {
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
