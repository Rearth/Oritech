package rearth.oritech.spaceage.client.mission_control;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import rearth.oritech.spaceage.OritechSpaceAge;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Local fleet organization; assignments are independent of missions and their changing locations.
 */
public final class FleetCategories {

    public static final String UNCATEGORIZED = "uncategorized";
    public static final String SUN = "sun";
    private final Path file;
    private final List<Category> categories = new ArrayList<>();
    private final Map<UUID, String> assignments = new HashMap<>();

    FleetCategories(Path file) {

        this.file = file;
        if (Files.exists(file)) {
            try {
                var saved = Saved.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(Files.readString(file))).getOrThrow();
                categories.addAll(saved.categories());
                assignments.putAll(saved.assignments());
            } catch (IOException | RuntimeException exception) {
                OritechSpaceAge.LOGGER.warn("Could not load fleet categories", exception);
            }
        }
        if (categories.stream().noneMatch(category -> category.id().equals(SUN)))
            categories.add(new Category(SUN, "", true));
        if (categories.stream().noneMatch(category -> category.id().equals(UNCATEGORIZED)))
            categories.addFirst(new Category(UNCATEGORIZED, "", false));
    }

    static boolean permanent(String id) {

        return id.equals(UNCATEGORIZED) || id.equals(SUN);
    }

    public List<Category> categories() {

        return List.copyOf(categories);
    }

    boolean collapsed(String id) {

        return categories.stream().anyMatch(category -> category.id().equals(id) && category.collapsed());
    }

    String create(String name) {

        var trimmed = name.strip();
        if (trimmed.isEmpty()) return null;
        var id = UUID.randomUUID().toString();
        categories.add(new Category(id, trimmed.substring(0, Math.min(48, trimmed.length())), false));
        save();
        return id;
    }

    void assign(UUID craft, String category) {

        if (!contains(category)) return;
        assignments.put(craft, category);
        save();
    }

    void toggle(String id) {

        categories.replaceAll(category -> category.id().equals(id)
                ? new Category(id, category.name(), !category.collapsed()) : category);
        save();
    }

    String category(UUID craft, boolean sunOrbit) {

        return category(craft, sunOrbit, "");
    }

    String category(UUID craft, boolean sunOrbit, String launchCategory) {

        if (sunOrbit) return SUN;
        var assigned = assignments.getOrDefault(craft, launchCategory);
        if (assigned != null && !assigned.equals(SUN) && contains(assigned)) return assigned;
        return UNCATEGORIZED;
    }

    void delete(String id, List<UUID> members) {

        if (permanent(id)) return;
        categories.removeIf(category -> category.id().equals(id));
        assignments.replaceAll((craft, category) -> category.equals(id) ? UNCATEGORIZED : category);
        members.forEach(craft -> assignments.put(craft, UNCATEGORIZED));
        save();
    }

    public Component label(String id) {

        if (id.equals(SUN)) return Component.translatable("screen.oritech_space_age.fleet.sun");
        return categories.stream().filter(category -> category.id().equals(id) && !id.equals(UNCATEGORIZED))
                .map(category -> Component.literal(category.name())).findFirst()
                .orElseGet(() -> Component.translatable("screen.oritech_space_age.fleet.uncategorized"));
    }

    private boolean contains(String id) {

        return categories.stream().anyMatch(category -> category.id().equals(id));
    }

    private void save() {

        try {
            var json = Saved.CODEC.encodeStart(JsonOps.INSTANCE, new Saved(categories, assignments)).getOrThrow();
            Files.createDirectories(file.getParent());
            Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(json));
        } catch (IOException exception) {
            OritechSpaceAge.LOGGER.warn("Could not save fleet categories", exception);
        }
    }

    public record Category(String id, String name, boolean collapsed) {

        private static final Codec<Category> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("id").forGetter(Category::id),
                Codec.STRING.fieldOf("name").forGetter(Category::name),
                Codec.BOOL.fieldOf("collapsed").forGetter(Category::collapsed)
        ).apply(instance, Category::new));
    }

    private record Saved(List<Category> categories, Map<UUID, String> assignments) {

        private static final Codec<Saved> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Category.CODEC.listOf().fieldOf("categories").forGetter(Saved::categories),
                Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.STRING).fieldOf("assignments").forGetter(Saved::assignments)
        ).apply(instance, Saved::new));
    }
}
