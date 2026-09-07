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
            ResourceLocation fileId = entry.getKey();
            JsonElement jsonElement = entry.getValue();

            if (!jsonElement.isJsonObject()) continue;
            JsonObject obj = jsonElement.getAsJsonObject();
            PenchantmentDefinition.Partial partial = new PenchantmentDefinition.Partial();

            // Experience Cost aliases
            if (obj.has("experience_cost") && obj.get("experience_cost").isJsonPrimitive()) {
                partial.setExperienceCost(obj.get("experience_cost").getAsInt());
            } else if (obj.has("experienceCost") && obj.get("experienceCost").isJsonPrimitive()) {
                partial.setExperienceCost(obj.get("experienceCost").getAsInt());
            } else if (obj.has("xp_cost") && obj.get("xp_cost").isJsonPrimitive()) {
                partial.setExperienceCost(obj.get("xp_cost").getAsInt());
            } else if (obj.has("xpCost") && obj.get("xpCost").isJsonPrimitive()) {
                partial.setExperienceCost(obj.get("xpCost").getAsInt());
            } else if (obj.has("experience") && obj.get("experience").isJsonPrimitive()) {
                partial.setExperienceCost(obj.get("experience").getAsInt());
            } else if (obj.has("xp") && obj.get("xp").isJsonPrimitive()) {
                partial.setExperienceCost(obj.get("xp").getAsInt());
            }

            // Book Requirement aliases
            if (obj.has("book_requirement") && obj.get("book_requirement").isJsonPrimitive()) {
                partial.setBookRequirement(obj.get("book_requirement").getAsInt());
            } else if (obj.has("bookRequirement") && obj.get("bookRequirement").isJsonPrimitive()) {
                partial.setBookRequirement(obj.get("bookRequirement").getAsInt());
            } else if (obj.has("bookshelf_requirement") && obj.get("bookshelf_requirement").isJsonPrimitive()) {
                partial.setBookRequirement(obj.get("bookshelf_requirement").getAsInt());
            } else if (obj.has("bookshelfRequirement") && obj.get("bookshelfRequirement").isJsonPrimitive()) {
                partial.setBookRequirement(obj.get("bookshelfRequirement").getAsInt());
            } else if (obj.has("books") && obj.get("books").isJsonPrimitive()) {
                partial.setBookRequirement(obj.get("books").getAsInt());
            } else if (obj.has("bookshelves") && obj.get("bookshelves").isJsonPrimitive()) {
                partial.setBookRequirement(obj.get("bookshelves").getAsInt());
            }

            // Progress Cost Factor aliases
            JsonElement costElem = null;
            if (obj.has("progress_cost_factor")) costElem = obj.get("progress_cost_factor");
            else if (obj.has("progressCostFactor")) costElem = obj.get("progressCostFactor");
            else if (obj.has("cost_factor")) costElem = obj.get("cost_factor");
            else if (obj.has("costFactor")) costElem = obj.get("costFactor");
            else if (obj.has("progress_cost")) costElem = obj.get("progress_cost");
            else if (obj.has("progressCost")) costElem = obj.get("progressCost");
            else if (obj.has("scaling")) costElem = obj.get("scaling");
            else if (obj.has("cost")) costElem = obj.get("cost");

            if (costElem != null) {
                if (costElem.isJsonObject()) {
                    JsonObject costObj = costElem.getAsJsonObject();
                    int base = costObj.has("base") ? costObj.get("base").getAsInt() : 1;
                    int perLevel = costObj.has("per_level") ? costObj.get("per_level").getAsInt() :
                            (costObj.has("perLevel") ? costObj.get("perLevel").getAsInt() :
                            (costObj.has("perlevel") ? costObj.get("perlevel").getAsInt() : Math.max(base / 2, 1)));
                    partial.setProgressCostFactor(new PenchantmentDefinition.Cost(base, perLevel));
                } else if (costElem.isJsonPrimitive()) {
                    int base = costElem.getAsInt();
                    partial.setProgressCostFactor(new PenchantmentDefinition.Cost(base, Math.max(base / 2, 1)));
                }
            } else if (obj.has("base") && obj.get("base").isJsonPrimitive()) {
                int base = obj.get("base").getAsInt();
                int perLevel = obj.has("per_level") ? obj.get("per_level").getAsInt() :
                        (obj.has("perLevel") ? obj.get("perLevel").getAsInt() :
                        (obj.has("perlevel") ? obj.get("perlevel").getAsInt() : Math.max(base / 2, 1)));
                partial.setProgressCostFactor(new PenchantmentDefinition.Cost(base, perLevel));
            }

            if (!partial.isEmpty()) {
                ResourceLocation targetId = resolveEnchantmentId(fileId);
                map.put(targetId, partial);
                if (!targetId.equals(fileId)) {
                    map.put(fileId, partial);
                }
            }
        }

        PenchantmentDefinition.setDatapackDefinitions(map);
        Penchant.LOGGER.info("Loaded {} datapack penchantment definitions", map.size());

        PenchantNetworking.broadcastSyncDefinitions();
    }

    private static ResourceLocation resolveEnchantmentId(ResourceLocation fileId) {
        if (net.minecraft.core.registries.BuiltInRegistries.ENCHANTMENT.containsKey(fileId)) {
            return fileId;
        }
        if (fileId.getPath().contains("/")) {
            ResourceLocation parsed = ResourceLocation.tryParse(fileId.getPath());
            if (parsed != null && net.minecraft.core.registries.BuiltInRegistries.ENCHANTMENT.containsKey(parsed)) {
                return parsed;
            }
        }
        ResourceLocation mcId = new ResourceLocation("minecraft", fileId.getPath());
        if (net.minecraft.core.registries.BuiltInRegistries.ENCHANTMENT.containsKey(mcId)) {
            return mcId;
        }
        return fileId;
    }
}
