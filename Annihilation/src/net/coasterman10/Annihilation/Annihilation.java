package net.coasterman10.Annihilation;

import java.util.logging.Level;

import net.coasterman10.Annihilation.bar.BarUtil;
import net.coasterman10.Annihilation.chat.ChatListener;
import net.coasterman10.Annihilation.chat.ChatUtil;
import net.coasterman10.Annihilation.commands.AnnihilationCommand;
import net.coasterman10.Annihilation.kits.KitManager;
import net.coasterman10.Annihilation.listeners.PlayerListener;
import net.coasterman10.Annihilation.listeners.ResourceListener;
import net.coasterman10.Annihilation.listeners.SoulboundListener;
import net.coasterman10.Annihilation.listeners.WandListener;
import net.coasterman10.Annihilation.listeners.WorldListener;
import net.coasterman10.Annihilation.maps.MapManager;
import net.coasterman10.Annihilation.maps.VotingManager;
import net.coasterman10.Annihilation.stats.DatabaseHandler;
import net.coasterman10.Annihilation.stats.StatType;
import net.coasterman10.Annihilation.stats.StatsManager;
import net.coasterman10.Annihilation.teams.Team;
import net.coasterman10.Annihilation.teams.TeamManager;

import org.apache.commons.lang.WordUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class Annihilation extends JavaPlugin {
	private ConfigManager configManager;
	private VotingManager voting;
	private MapManager maps;
	private TeamManager teams;
	private PhaseTimer timer;
	private ResourceListener resources;
	private StatsManager stats;
	private DatabaseHandler db;
	private KitManager kits;
	private IngameScoreboardManager ingameScoreboard;
	public boolean useMysql = false;

	@Override
	public void onEnable() {
		configManager = new ConfigManager(this);
		configManager.loadConfigFiles("config.yml", "maps.yml", "shops.yml",
				"stats.yml");

		maps = new MapManager(this, configManager.getConfig("maps.yml"));
		teams = new TeamManager(this);

		Configuration shops = configManager.getConfig("shops.yml");
		new Shop(this, "Weapon", shops);
		new Shop(this, "Brewing", shops);

		stats = new StatsManager(this);
		resources = new ResourceListener(this);
		kits = new KitManager(this);
		ingameScoreboard = new IngameScoreboardManager(this);

		Configuration config = configManager.getConfig("config.yml");
		timer = new PhaseTimer(this, config);
		voting = new VotingManager(this);

		new AnnihilationCommand(this);
		new ChestLocker(this);
		new ChatListener(this);
		new PlayerListener(this);
		new WorldListener(this);
		new SoulboundListener(this);
		new WandListener(this);
		
		BarUtil.init(this);

		voting.setCurrentForPlayers(getServer().getOnlinePlayers());

		if (config.getString("stats").equalsIgnoreCase("sql"))
			useMysql = true;

		if (useMysql) {
			String host = config.getString("MySQL.host");
			Integer port = config.getInt("MySQL.port");
			String name = config.getString("MySQL.name");
			String user = config.getString("MySQL.user");
			String pass = config.getString("MySQL.pass");
			db = new DatabaseHandler(host, port, name, user, pass, this);

			db.query("CREATE TABLE IF NOT EXISTS `annihilation` ( `username` varchar(16) NOT NULL, `kills` int(16) NOT NULL, `deaths` int(16) NOT NULL, `wins` int(16) NOT NULL, `losses` int(16) NOT NULL, `nexus_damage` int(16) NOT NULL, UNIQUE KEY `username` (`username`) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;");
		} else
			db = new DatabaseHandler(this);
		
		reset();
	}

	public boolean startTimer() {
		if (timer.isRunning())
			return false;

		timer.start();
		voting.setCurrentForPlayers(getServer().getOnlinePlayers());

		return true;
	}

	public void startGame() {
		for (Team t : teams.getTeams()) {
			for (Player p : t.getPlayers()) {
				Location spawn = maps.getSpawnPoint(t.getName());
				p.teleport(spawn);
				kits.getKit(p).getKitClass().give(p, t.getName());
				ingameScoreboard.setCurrentForPlayers(p);
			}

			t.loadNexus(maps.getNexus(t.getName()), 25);
			ingameScoreboard.updateScore(t);
		}

		resources.loadDiamonds();
	}

	public void advancePhase() {
		ChatUtil.phaseMessage(timer.getPhase());
		if (timer.getPhase() == 3)
			resources.spawnDiamonds();
	}

	public void onSecond() {
		long time = timer.getTime();

		if (time == -5L) {
			if (maps.selectMap(voting.getWinner()))
				getServer().broadcastMessage(
						voting.getWinner() + " selected, loading...");
			else
				getServer().broadcastMessage(
						"Could not load " + voting.getWinner());
			voting.end();
			ingameScoreboard.setTitle(ChatColor.GOLD + "Map: "
					+ WordUtils.capitalize(voting.getWinner()));
		}

		if (time == 0L)
			startGame();
	}

	public int getPhase() {
		return timer.getPhase();
	}

	public TeamManager getTeamManager() {
		return teams;
	}

	public MapManager getMapManager() {
		return maps;
	}

	public ConfigManager getConfigManager() {
		return configManager;
	}

	public StatsManager getStatsManager() {
		return stats;
	}

	public DatabaseHandler getDatabaseHandler() {
		return db;
	}

	public KitManager getKitManager() {
		return kits;
	}

	public int getPhaseDelay() {
		return configManager.getConfig("config.yml").getInt("phase-period");
	}

	public void log(String m, Level l) {
		getLogger().log(l, m);
	}

	public VotingManager getVotingManager() {
		return voting;
	}

	public boolean isEmptyColumn(Location loc) {
		boolean hasBlock = false;
		Location test = loc.clone();
		for (int y = 0; y < loc.getWorld().getMaxHeight(); y++) {
			test.setY(y);
			if (test.getBlock().getType() != Material.AIR)
				hasBlock = true;
		}
		return !hasBlock;
	}

	public IngameScoreboardManager getIngameScoreboardmanager() {
		return ingameScoreboard;
	}

	public void endGame(Team winner) {
		ChatUtil.winMessage(winner);
		timer.stop();

		for (Player p : winner.getPlayers())
			stats.incrementStat(StatType.WINS, p);
		long restartDelay = configManager.getConfig("config.yml").getLong(
				"restart-delay");
		new RestartTimer(this, restartDelay).start(timer.getTime());
	}

	public void reset() {
		teams.reset();
		maps.reset();
		timer.reset();
		for (Player p : getServer().getOnlinePlayers()) {
			p.getInventory().clear();
			p.teleport(maps.getLobbySpawnPoint());
			BarUtil.setMessageAndPercent(p, ChatColor.DARK_AQUA + "Welcome to Annihilation!", 0.01F);
		}
	}
}
