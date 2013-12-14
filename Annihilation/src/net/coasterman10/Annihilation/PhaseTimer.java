package net.coasterman10.Annihilation;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;

import net.coasterman10.Annihilation.bar.BarManager;

public class PhaseTimer {
    private long time;
    private long startTime;
    private long phaseTime;
    private int phase;
    private boolean isRunning;

    private final Annihilation plugin;
    private final BarManager bar;

    public PhaseTimer(Annihilation plugin, long start, long period) {
	this.plugin = plugin;
	bar = new BarManager(plugin);
	startTime = start;
	phaseTime = period;
	phase = 0;
    }

    public PhaseTimer(Annihilation plugin, ConfigurationSection config) {
	this.plugin = plugin;
	bar = new BarManager(plugin);
	startTime = config.getLong("start-delay", 120L);
	phaseTime = config.getLong("phase-period", 600L);
	phase = 0;
    }

    public void start() {
	if (!isRunning) {
	    BukkitScheduler scheduler = plugin.getServer().getScheduler();
	    scheduler.scheduleSyncRepeatingTask(plugin, new Runnable() {
		public void run() {
		    onSecond();
		}
	    }, 20L, 20L);
	    isRunning = true;
	}

	time = -startTime;

	for (Player p : Bukkit.getOnlinePlayers())
	    bar.setMessageAndPercent(p, ChatColor.GREEN + "Starting in "
		    + -time, 1F);
    }

    public long getTime() {
	return time;
    }

    public long getRemainingPhaseTime() {
	if (phase == 5) {
	    return phaseTime;
	}
	if (phase >= 1) {
	    return time % phaseTime;
	}
	return -time;
    }

    public int getPhase() {
	return phase;
    }

    public boolean isRunning() {
	return isRunning;
    }

    private void onSecond() {
	time++;

	if (getRemainingPhaseTime() == 0)
	    phase++;

	float percent;
	String text;

	if (phase == 0) {
	    percent = (float) -time / (float) startTime;
	    text = ChatColor.GREEN + "Starting in " + -time;
	} else {
	    if (phase == 5)
		percent = 1F;
	    else
		percent = (float) getRemainingPhaseTime() / (float) phaseTime;
	    text = "Phase " + phase;
	    switch (phase) {
	    case 1:
		text = ChatColor.BLUE + text;
		break;
	    case 2:
		text = ChatColor.GREEN + text;
		break;
	    case 3:
		text = ChatColor.YELLOW + text;
		break;
	    case 4:
		text = ChatColor.GOLD + text;
		break;
	    case 5:
		text = ChatColor.RED + text;
		break;
	    }
	    text += ChatColor.WHITE + " | " + timeString();
	}

	for (Player p : Bukkit.getOnlinePlayers())
	    bar.setMessageAndPercent(p, text, percent);

	plugin.onSecond();
    }

    private String timeString() {
	long hours = time / 3600L;
	long minutes = (time - hours * 3600L) / 60L;
	long seconds = time - hours * 3600L - minutes * 60L;
	return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
