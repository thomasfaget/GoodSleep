package goodSleep;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;


/** This plugin allows to the players to skip the night if not all players are sleeping.
 * Only a percentage of persons sleeping is needed to skin the night.
 *
 * Created by thomas on 14/02/2017.
 */
public class GoodSleep extends JavaPlugin implements Listener {

    private double sleepPercentage = 0.5;
    private int nbPlayersSleeping = 0;

    /** Actions to do when the plugin is enabled :
     * - Save the default configuration.
     * - Init values form the config file.
     */
    @Override
    public void onEnable() {
        saveDefaultConfig(); // Copy config.yml for jar to GoodSleep folder
        loadConfigFile(); // Load config.yml in the folder

        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Plugin loaded - The sleep percentage is : " + sleepPercentage);
    }


    /** Read the config file and init the configuration.
     *
     * @return if the configuration file was correctly read
     */
    private boolean loadConfigFile() {
        try {
            double sp = (double) getConfig().get("sleepPercentage");
            if (sp>=0 && sp<=1) {
                sleepPercentage = sp;
                return true;
            }
            else {
                getLogger().warning("Error : sleep percentage not between 0 and 1! Check config.yml! Default values will be used");
                return false;
            }
        }
        catch (Exception e){
            getLogger().warning("Error : Can't read config.yml! Check config.yml! Default values will be used");
            return true;
        }
    }


    /** Handle command thrown by players
     */
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // Command : "/reload"
        if ( cmd.getName().equals("reload") ) {
            reloadConfig();
            if ( loadConfigFile() ) {
                getLogger().info("Config reload. Sleep percentage : " + sleepPercentage);
                sender.sendMessage("Plugin reloaded !");
                return true;
            }
            else {
                getLogger().info("Config reload. Sleep percentage : " + sleepPercentage);
                sender.sendMessage("Error ! Plugin not reloaded !");
                return false;
            }
        }
        // Command : /setSleepPercentage <value between 0 and 1>
        else if ( cmd.getName().equals("setSleepPercentage") ) {
            if ( args.length == 0 || Double.parseDouble(args[0]) < 0.0 || Double.parseDouble(args[0]) > 1.0 ) {
                sender.sendMessage("Usage : /setSleepPercentage <value between 0 and 1>");
                return false;
            }
            else {
                sleepPercentage = Double.parseDouble(args[0]);
                getConfig().set("sleepPercentage", sleepPercentage);
                saveConfig();
                getLogger().info("Sleep percentage changed to " + sleepPercentage);
                sender.sendMessage("Sleep percentage changed to " + sleepPercentage);
                return true;
            }
        }
        // Command : /sleep
        else if ( cmd.getName().equals("sleep") ) {
            // Test the permission
            if ( sender.hasPermission("goodSleep.sleep") ) {
                World world = ((Player) sender).getWorld();
                List<Player> playerList = world.getPlayers();
                nbPlayersSleeping++;
                int nbPlayersNeeded = Math.max((int) Math.ceil(playerList.size() * sleepPercentage), 1);

                getLogger().info(sender.getName() + " sleep -> " + nbPlayersSleeping + "/" + nbPlayersNeeded);

                // Test to skip the night
                if (!skipNight(world)) {
                    // Notify all the players if not enough people are sleeping :
                    playerList.forEach(p -> p.sendMessage(
                            ChatColor.GOLD + sender.getName() +
                                    ChatColor.WHITE + " is sleeping (" +
                                    ChatColor.YELLOW + nbPlayersSleeping +
                                    ChatColor.WHITE + "/" +
                                    ChatColor.GREEN + nbPlayersNeeded +
                                    ChatColor.WHITE + ")"
                    ));

                }
                return true;
            }
            else {
                sender.sendMessage("You haven't the permission to use GoodSleep!");
                return false;
            }
        }
        else {
            return false;
        }
    }

    /** Prevent player entering in a bed.
     * Update the number of player sleeping
     * Skip the night if needed
     */
    @EventHandler
    public void onPlayerBedEnter(PlayerBedEnterEvent e) {
        Player player = e.getPlayer();
        World world = player.getWorld();
        List<Player> playerList = world.getPlayers();

        // Test if the player have the permission to sleep (using the plugin)
        if ( player.hasPermission("goodSleep.sleep") || e.isCancelled() ) {
            nbPlayersSleeping++;
            int nbPlayersNeeded = Math.max( (int)  Math.ceil(playerList.size()*sleepPercentage) , 1 );

            getLogger().info(player.getName() + " sleep -> " + nbPlayersSleeping + "/"  + nbPlayersNeeded);

            // Test to skip the night
            if (!skipNight(world)) {
                // Notify all the players if not enough people are sleeping :
                playerList.forEach(p -> p.sendMessage(
                        ChatColor.GOLD + player.getName() +
                                ChatColor.WHITE + " is sleeping (" +
                                ChatColor.YELLOW + nbPlayersSleeping +
                                ChatColor.WHITE + "/" +
                                ChatColor.GREEN + nbPlayersNeeded +
                                ChatColor.WHITE + ")"
                ));

            }
            else {
                nbPlayersSleeping = 0; // Reset the number of people sleeping
            }

        }
        else {
            player.sendMessage("You haven't the permission to use GoodSleep!");
        }

    }



    /** Prevent if the player leave the bed while sleep.
     * Update the number of player sleeping
     */
    @EventHandler
    public void onPlayerBedLeave(PlayerBedLeaveEvent e) {
        Player player = e.getPlayer();
        World world = player.getWorld();
        List<Player> playerList = world.getPlayers();

        // Test if the player has the permission,
        if ( player.hasPermission("goodSleep.sleep") ) {
            // Test if player leave bed while sleeping (not at the morning)
            if ( world.getTime()>13000 ) {
                nbPlayersSleeping--;
                int nbPlayersNeeded = (int) Math.ceil(playerList.size()*sleepPercentage);

                getLogger().info(player.getName() + " leaves bed -> " + nbPlayersSleeping + "/" + nbPlayersNeeded);

                // Notify all the players :
                playerList.forEach(p -> p.sendMessage(
                        ChatColor.GOLD + player.getName() +
                        ChatColor.WHITE + " leaves his bed (" +
                        ChatColor.YELLOW + nbPlayersSleeping +
                        ChatColor.WHITE + "/" +
                        ChatColor.GREEN + nbPlayersNeeded +
                        ChatColor.WHITE + ")"
                ));
            }
        }
        else {
            player.sendMessage("You haven't the permission to use GoodSleep!");
        }
    }


    /** Prevent players leaving the game while sleeping
     * Update the number of player sleeping
     * Skip the night if needed
     */
    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        World world = player.getWorld();
        List<Player> playerList = world.getPlayers();

        // Test permission
        if ( player.hasPermission("goodSleep.sleep") ) {
            // Test if player sleep (while leaving the game)
            if (player.isSleeping()) {
                nbPlayersSleeping--;
                int nbPlayersNeeded = (int) Math.ceil(playerList.size()*sleepPercentage);

                getLogger().info(player.getName() + " leaves game -> " + nbPlayersSleeping + "/" + nbPlayersNeeded);

                // Test to skip the night
                if (!skipNight(world)) {
                    // Notify all the players if not enough people are sleeping :
                    playerList.forEach(p -> player.sendMessage(
                            ChatColor.GOLD + p.getName() +
                                    ChatColor.WHITE + " leaves the game while sleeping (" +
                                    ChatColor.YELLOW + nbPlayersSleeping +
                                    ChatColor.WHITE + "/" +
                                    ChatColor.GREEN + nbPlayersNeeded +
                                    ChatColor.WHITE + ")"
                    ));
                }
                else {
                    nbPlayersSleeping = 0; // Reset the number of people sleeping
                }
            }
        }
        else {
            player.sendMessage("You haven't the permission to use GoodSleep!");
        }
    }


    /** Test if the enough are sleeping to skip the night
     * If yes : skip the night and send a message to all the players.
     * Clear rain and storm at the morning.
     *
     * @return if the night was skipped or not
     */
    private boolean skipNight(World world) {

        // Test if enough players are sleeping
        if ( (double) nbPlayersSleeping/(double) world.getPlayers().size() >= sleepPercentage ) {
            world.setTime(10); // Set time to morning
            world.getPlayers().forEach(p -> p.sendMessage(ChatColor.GREEN + "Good morning Minecraft"));

            if ( world.hasStorm() ) { // Clear rain/storm
                world.setStorm(false);
            }
            if ( world.isThundering() ) { // Clear thunder
                world.setThundering(false);
            }
            return true;
        }
        else {
            return false;
        }
    }


}
