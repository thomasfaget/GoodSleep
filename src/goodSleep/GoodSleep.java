package goodSleep;

import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;


/** This plugin allows to the players to skip the night if not all players are sleeping.
 * Only a percentage of persons sleeping is needed to skin the night.
 *
 * Created by thomas on 14/02/2017.
 */
public class GoodSleep extends JavaPlugin implements Listener {

    private double sleepPercentage = 0.5;
    private double nbPlayersSleeping = 0;

    /** Actions to do when the plugin is enabled :
     * - Save the default configuration.
     * - Init the configuration form the config file.
     */
    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfigFile();

        getServer().getPluginManager().registerEvents(this, this);
    }

    /** Actions to do when the plugin is disabled :
     * - Nothing
     */
    @Override
    public void onDisable() {
        // Do nothing
    }

    /** Read the config file and init the configuration
     */
    private void loadConfigFile() {
        FileConfiguration config = getConfig();
        try {
            double sp = config.getDouble("sleepPercentage");
            if (sp>=0 && sp<=1) {
                sleepPercentage = sp;
            }
        }
        catch (NumberFormatException e){
            getLogger().warning("[GoodSleep] Error during reading the sleepPercentage in the config file!");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // Command "/reload"
        if ( cmd.getName().equals("reload") ) {
            loadConfigFile();
            return true;
        }
        return false;
    }

    @EventHandler
    public void onPlayerBedEnter(PlayerBedEnterEvent e) {
        Player player = e.getPlayer();
        World world = player.getWorld();
        double nbPlayers = world.getPlayers().size();

        // Test if the player have the permission to sleep (using the plugin)
        if ( player.hasPermission("goodSleep.sleep") || e.isCancelled() ) {
            nbPlayersSleeping++;

            if ( nbPlayersSleeping/nbPlayers >= sleepPercentage ) {
                skipNight(world);
            }
            else {
                // Notify all the players :
                world.getPlayers().forEach( p -> p.sendMessage(p.getName() + " is Sleeping (" + nbPlayersSleeping + "/" + nbPlayers + ")") );
            }
            nbPlayersSleeping = 0; // Reset the number of people sleeping
        }
        else {
            player.sendMessage("You haven't the permission to use GoodSleep!");
        }

    }

    /** Skip the night and send a message to all the players.
     * Method used when their is enough player sleeping.
     * Also clear storms and thundering
     */
    private void skipNight(World world) {
        world.setTime(22796);
        world.getPlayers().forEach(p -> p.sendMessage("Good morning Minecraft"));

        if ( world.hasStorm() ) {
            world.setStorm(false);
        }
        if ( world.isThundering() ) {
            world.setThundering(false);
        }
    }

    @EventHandler
    public void onPlayerBedLeave(PlayerBedLeaveEvent e) {
        Player player = e.getPlayer();
        World world = player.getWorld();
        double nbPlayers = world.getPlayers().size();

        if ( player.hasPermission("goodSleep.sleep")  ) {
            nbPlayersSleeping--;

            // Notify all the players :
            world.getPlayers().forEach( p -> p.sendMessage(p.getName() + " leave his bed (" + nbPlayersSleeping + "/" + nbPlayers + ")") );
        }
        else {
            player.sendMessage("You haven't the permission to use GoodSleep!");
        }
    }

    @EventHandler
    /** Prevent players leaving the game while sleeping
     */
    public void onPlayerLeave(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        World world = player.getWorld();
        double nbPlayers = world.getPlayers().size();

        if ( player.hasPermission("goodSleep.sleep") && player.isSleeping() ) {
            nbPlayersSleeping--;

            // Notify all the players :
            world.getPlayers().forEach( p -> p.sendMessage(p.getName() + " leave the game! (" + nbPlayersSleeping + "/" + nbPlayers + ")") );
        }
        else {
            player.sendMessage("You haven't the permission to use GoodSleep!");
        }
    }


}
