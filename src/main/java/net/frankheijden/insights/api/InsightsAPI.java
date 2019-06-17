package net.frankheijden.insights.api;

import net.frankheijden.insights.Insights;
import net.frankheijden.insights.api.entities.ChunkLocation;
import net.frankheijden.insights.tasks.ScanTask;
import org.apache.commons.lang.RandomStringUtils;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;

import java.util.List;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class InsightsAPI {
    private Insights instance = null;

    /**
     * Initiates a new InsightsAPI instance.
     */
    public InsightsAPI() {}

    /**
     * Gets the instance of Insights.
     *
     * @return Insights Main class
     */
    public Insights getInstance() {
        if (instance == null) {
            instance = Insights.getInstance();
        }
        return instance;
    }

    /**
     * Scans chunks for all Materials and EntityTypes.
     *
     * @param world World in which we should scan
     * @param chunkLocations List of ChunkLocation to scan in
     * @return CompletableFuture which supplies the counts.
     */
    public CompletableFuture<TreeMap<String, Integer>> scan(World world, List<ChunkLocation> chunkLocations) {
        return scan(world, chunkLocations, null, null);
    }

    /**
     * Scans chunks for Materials and EntityTypes.
     *
     * @param world World in which we should scan
     * @param chunkLocations List of ChunkLocation to scan in
     * @param materials List of Material to scan for, null if none
     * @param entityTypes List of EntityType to scan for, null if none
     * @return CompletableFuture which supplies the counts.
     */
    public CompletableFuture<TreeMap<String, Integer>> scan(World world, List<ChunkLocation> chunkLocations, List<Material> materials, List<EntityType> entityTypes) {
        return CompletableFuture.supplyAsync(() -> {
            Object LOCK = new Object();

            String k = RandomStringUtils.randomAlphanumeric(16);
            while (getInstance().countsMap.containsKey(k)) {
                k = RandomStringUtils.randomAlphanumeric(16);
            }
            final String key = k;

            ScanTask scanTask = new ScanTask(getInstance(), world, chunkLocations, materials, entityTypes, (event) -> {
                getInstance().countsMap.put(key, event.getCounts());

                synchronized (LOCK) {
                    LOCK.notify();
                }
            });
            scanTask.start(System.currentTimeMillis());

            synchronized (LOCK) {
                try {
                    LOCK.wait(10000000);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }

            return getInstance().countsMap.get(key);
        });
    }

    /**
     * Toggles realtime checking for the UUID specified.
     * NOTE: To use realtime checking, the user still needs the permission 'insights.check.realtime'.
     *
     * @param uuid UUID of player
     */
    public void toggleCheck(UUID uuid) {
        getInstance().sqLite.toggleRealtimeCheck(uuid);
    }

    /**
     * Enables or disabled realtime checking for the UUID specified.
     * NOTE: To use realtime checking, the user still needs the permission 'insights.check.realtime'.
     *
     * @param uuid UUID of player
     * @param enabled boolean enabled
     */
    public void setToggleCheck(UUID uuid, boolean enabled) {
        getInstance().sqLite.setRealtimeCheck(uuid, enabled);
    }

    /**
     * Checks if the player specified is scanning for chunks.
     *
     * @param uuid UUID of player
     * @return boolean scanning
     */
    public boolean isScanningChunks(UUID uuid) {
        return getInstance().playerScanTasks.containsKey(uuid);
    }

    /**
     * Gets a percentage between 0 and 1 of the progress of scanning chunks,
     * returns null if the player is not scanning chunks.
     *
     * @param uuid UUID of player
     * @return double progress, or null if no ScanTask.
     */
    public Double getScanProgress(UUID uuid) {
        ScanTask task = getInstance().playerScanTasks.get(uuid);
        if (task != null) {
            double total = (double) task.getTotalChunks();
            double done = (double) task.getScanRunnable().getChunksDone();
            double progress = done/total;
            if (progress < 0) {
                progress = 0;
            } else if (progress > 1) {
                progress = 1;
            }
            return progress;
        }
        return null;
    }

    /**
     * Gets the time elapsed for the current scan of a player
     *
     * @param uuid UUID of player
     * @return String time elapsed, or null if no ScanTask.
     */
    public String getTimeElapsedOfScan(UUID uuid) {
        ScanTask task = getInstance().playerScanTasks.get(uuid);
        if (task != null) {
            return getInstance().utils.getDHMS(task.getStartTime());
        }
        return null;
    }
}