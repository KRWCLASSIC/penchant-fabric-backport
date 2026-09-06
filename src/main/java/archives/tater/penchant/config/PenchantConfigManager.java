package archives.tater.penchant.config;

import archives.tater.penchant.Penchant;
import archives.tater.penchant.PenchantmentDefinition;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class PenchantConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("penchant_definitions.json");

    private PenchantConfigManager() {}

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            PenchantmentDefinition.setConfigDefinitions(new HashMap<>());
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(CONFIG_PATH)) {
            JsonElement rootElement = JsonParser.parseReader(reader);
            if (!rootElement.isJsonObject()) {
                PenchantmentDefinition.setConfigDefinitions(new HashMap<>());
                return;
            }

            JsonObject root = rootElement.getAsJsonObject();
            Map<ResourceLocation, PenchantmentDefinition.Partial> map = new HashMap<>();

            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                ResourceLocation id = ResourceLocation.tryParse(entry.getKey());
                if (id == null || !entry.getValue().isJsonObject()) continue;

                JsonObject obj = entry.getValue().getAsJsonObject();
                PenchantmentDefinition.Partial partial = new PenchantmentDefinition.Partial();

                if (obj.has("experience_cost") && obj.get("experience_cost").isJsonPrimitive()) {
                    partial.setExperienceCost(obj.get("experience_cost").getAsInt());
                }

                if (obj.has("book_requirement") && obj.get("book_requirement").isJsonPrimitive()) {
                    partial.setBookRequirement(obj.get("book_requirement").getAsInt());
                }

                if (obj.has("progress_cost_factor")) {
                    JsonElement costElem = obj.get("progress_cost_factor");
                    if (costElem.isJsonObject()) {
                        JsonObject costObj = costElem.getAsJsonObject();
                        int base = costObj.has("base") ? costObj.get("base").getAsInt() : 1;
                        int perLevel = costObj.has("per_level") ? costObj.get("per_level").getAsInt() : Math.max(base / 2, 1);
                        partial.setProgressCostFactor(new PenchantmentDefinition.Cost(base, perLevel));
                    } else if (costElem.isJsonPrimitive()) {
                        int base = costElem.getAsInt();
                        partial.setProgressCostFactor(new PenchantmentDefinition.Cost(base, Math.max(base / 2, 1)));
                    }
                }

                if (!partial.isEmpty()) {
                    map.put(id, partial);
                }
            }

            PenchantmentDefinition.setConfigDefinitions(map);
            Penchant.LOGGER.info("Loaded {} server enchantment definition overrides from {}", map.size(), CONFIG_PATH.getFileName());
        } catch (Exception e) {
            Penchant.LOGGER.error("Failed to load {}", CONFIG_PATH, e);
        }
    }

    public static void save() {
        Map<ResourceLocation, PenchantmentDefinition.Partial> configMap = PenchantmentDefinition.getConfigDefinitions();
        JsonObject root = new JsonObject();

        for (Map.Entry<ResourceLocation, PenchantmentDefinition.Partial> entry : configMap.entrySet()) {
            PenchantmentDefinition.Partial partial = entry.getValue();
            if (partial.isEmpty()) continue;

            JsonObject obj = new JsonObject();
            if (partial.hasExperienceCost()) {
                obj.addProperty("experience_cost", partial.getExperienceCost());
            }
            if (partial.hasBookRequirement()) {
                obj.addProperty("book_requirement", partial.getBookRequirement());
            }
            if (partial.hasProgressCostFactor()) {
                JsonObject costObj = new JsonObject();
                costObj.addProperty("base", partial.getProgressCostFactor().base());
                costObj.addProperty("per_level", partial.getProgressCostFactor().perLevel());
                obj.add("progress_cost_factor", costObj);
            }

            root.add(entry.getKey().toString(), obj);
        }

        try {
            if (CONFIG_PATH.getParent() != null) {
                Files.createDirectories(CONFIG_PATH.getParent());
            }
            try (BufferedWriter writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException e) {
            Penchant.LOGGER.error("Failed to save {}", CONFIG_PATH, e);
        }
    }
}
