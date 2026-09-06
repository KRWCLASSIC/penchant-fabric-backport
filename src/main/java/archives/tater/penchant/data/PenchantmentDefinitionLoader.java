package archives.tater.penchant.data;

import archives.tater.penchant.Penchant;
import archives.tater.penchant.PenchantmentDefinition;
import archives.tater.penchant.network.PenchantNetworking;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.HashMap;
import java.util.Map;

public class PenchantmentDefinitionLoader extends SimpleJsonResourceReloadListener implements IdentifiableResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final ResourceLocation ID = Penchant.id("penchantment_definitions");

    public PenchantmentDefinitionLoader() {
        super(GSON, "penchant/definition");
    }

    @Override
    public ResourceLocation getFabricId() {
        return ID;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> entries, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, PenchantmentDefinition.Partial> map = new HashMap<>();

        for (Map.Entry<ResourceLocation, JsonElement> entry : entries.entrySet()) {
            ResourceLocation id = entry.getKey();
            JsonElement jsonElement = entry.getValue();

            if (!jsonElement.isJsonObject()) continue;
            JsonObject obj = jsonElement.getAsJsonObject();
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

        PenchantmentDefinition.setDatapackDefinitions(map);
        Penchant.LOGGER.info("Loaded {} datapack penchantment definitions", map.size());

        PenchantNetworking.broadcastSyncDefinitions();
    }
}
