package dev.frankheijden.insights.api.util;

import dev.frankheijden.insights.api.InsightsPlugin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class MaterialTags implements Tag<Material> {

    private static final List<Material> MATERIALS = Arrays.asList(Material.values());
    public static final MaterialTags BUCKETS = new MaterialTags(m -> m.name().endsWith("_BUCKET"), "buckets");
    public static final MaterialTags NEEDS_GROUND = new MaterialTags(
            EnumSet.of(
                    Material.ACTIVATOR_RAIL,
                    Material.COMPARATOR,
                    Material.DETECTOR_RAIL,
                    Material.POWERED_RAIL,
                    Material.RAIL,
                    Material.REDSTONE_WIRE,
                    Material.REPEATER
            ),
            "needs_ground"
    );

    private final Set<Material> materials;
    private final String key;

    private MaterialTags(Predicate<Material> materialPredicate, String key) {
        this(MATERIALS.stream().filter(materialPredicate).collect(Collectors.toSet()), key);
    }

    private MaterialTags(Set<Material> materials, String key) {
        this.materials = materials;
        this.key = key;
    }

    @Override
    public boolean isTagged(Material material) {
        return materials.contains(material);
    }

    @Override
    public Set<Material> getValues() {
        return materials;
    }

    @Override
    public NamespacedKey getKey() {
        return new NamespacedKey(InsightsPlugin.getInstance(), key);
    }
}
