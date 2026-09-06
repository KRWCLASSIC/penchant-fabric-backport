package archives.tater.penchant.command;

import archives.tater.penchant.PenchantmentDefinition;
import archives.tater.penchant.component.EnchantmentProgress;
import archives.tater.penchant.config.PenchantConfigManager;
import archives.tater.penchant.network.PenchantNetworking;
import archives.tater.penchant.util.PenchantmentHelper;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import static archives.tater.penchant.PenchantmentDefinition.Property.*;

public final class PenchantCommand {
    private static final DynamicCommandExceptionType ERROR_UNKNOWN_ENCHANTMENT = new DynamicCommandExceptionType(
            id -> Component.literal("Unknown enchantment: '" + id + "'").withStyle(ChatFormatting.RED)
    );

    private static final DynamicCommandExceptionType ERROR_DATAPACK_LOCKED = new DynamicCommandExceptionType(
            args -> Component.literal("Cannot modify " + ((Object[]) args)[0] + " for '" + ((Object[]) args)[1] + "': it is locked by a loaded datapack!")
                    .withStyle(ChatFormatting.RED)
    );

    private static final SimpleCommandExceptionType ERROR_NO_ITEM = new SimpleCommandExceptionType(
            Component.literal("You must be holding an item in your main hand!").withStyle(ChatFormatting.RED)
    );

    private PenchantCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("penchant")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("progress_cost_factor")
                        .then(Commands.argument("enchantment", ResourceLocationArgument.id())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(BuiltInRegistries.ENCHANTMENT.keySet(), builder))
                                .executes(ctx -> queryProgressCostFactor(ctx.getSource(), getEnchantment(ctx, "enchantment")))
                                .then(Commands.argument("base", IntegerArgumentType.integer(1))
                                        .executes(ctx -> setProgressCostFactor(
                                                ctx.getSource(),
                                                getEnchantment(ctx, "enchantment"),
                                                IntegerArgumentType.getInteger(ctx, "base"),
                                                Math.max(IntegerArgumentType.getInteger(ctx, "base") / 2, 1)
                                        ))
                                        .then(Commands.argument("per_level", IntegerArgumentType.integer(0))
                                                .executes(ctx -> setProgressCostFactor(
                                                        ctx.getSource(),
                                                        getEnchantment(ctx, "enchantment"),
                                                        IntegerArgumentType.getInteger(ctx, "base"),
                                                        IntegerArgumentType.getInteger(ctx, "per_level")
                                                ))
                                        )
                                )
                        )
                )
                .then(Commands.literal("experience_cost")
                        .then(Commands.argument("enchantment", ResourceLocationArgument.id())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(BuiltInRegistries.ENCHANTMENT.keySet(), builder))
                                .executes(ctx -> queryExperienceCost(ctx.getSource(), getEnchantment(ctx, "enchantment")))
                                .then(Commands.argument("cost", IntegerArgumentType.integer(0))
                                        .executes(ctx -> setExperienceCost(
                                                ctx.getSource(),
                                                getEnchantment(ctx, "enchantment"),
                                                IntegerArgumentType.getInteger(ctx, "cost")
                                        ))
                                )
                        )
                )
                .then(Commands.literal("book_requirement")
                        .then(Commands.argument("enchantment", ResourceLocationArgument.id())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(BuiltInRegistries.ENCHANTMENT.keySet(), builder))
                                .executes(ctx -> queryBookRequirement(ctx.getSource(), getEnchantment(ctx, "enchantment")))
                                .then(Commands.argument("books", IntegerArgumentType.integer(0))
                                        .executes(ctx -> setBookRequirement(
                                                ctx.getSource(),
                                                getEnchantment(ctx, "enchantment"),
                                                IntegerArgumentType.getInteger(ctx, "books")
                                        ))
                                )
                        )
                )
                .then(Commands.literal("reset")
                        .then(Commands.argument("enchantment", ResourceLocationArgument.id())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(BuiltInRegistries.ENCHANTMENT.keySet(), builder))
                                .executes(ctx -> resetEnchantment(ctx.getSource(), getEnchantment(ctx, "enchantment")))
                        )
                )
                .then(Commands.literal("debug")
                        .then(Commands.literal("set_progress")
                                .then(Commands.argument("enchantment", ResourceLocationArgument.id())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(BuiltInRegistries.ENCHANTMENT.keySet(), builder))
                                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 255))
                                                .executes(ctx -> setDebugProgress(
                                                        ctx.getSource(),
                                                        getEnchantment(ctx, "enchantment"),
                                                        IntegerArgumentType.getInteger(ctx, "level"),
                                                        0
                                                ))
                                                .then(Commands.argument("progress", IntegerArgumentType.integer(0))
                                                        .executes(ctx -> setDebugProgress(
                                                                ctx.getSource(),
                                                                getEnchantment(ctx, "enchantment"),
                                                                IntegerArgumentType.getInteger(ctx, "level"),
                                                                IntegerArgumentType.getInteger(ctx, "progress")
                                                        ))
                                                )
                                        )
                                )
                        )
                )
        );
    }

    private static Enchantment getEnchantment(CommandContext<CommandSourceStack> ctx, String name) throws CommandSyntaxException {
        ResourceLocation id = ResourceLocationArgument.getId(ctx, name);
        Enchantment enchantment = BuiltInRegistries.ENCHANTMENT.get(id);
        if (enchantment == null || !BuiltInRegistries.ENCHANTMENT.containsKey(id)) {
            throw ERROR_UNKNOWN_ENCHANTMENT.create(id);
        }
        return enchantment;
    }

    private static int queryProgressCostFactor(CommandSourceStack source, Enchantment enchantment) {
        ResourceLocation id = BuiltInRegistries.ENCHANTMENT.getKey(enchantment);
        PenchantmentDefinition def = PenchantmentDefinition.getDefinition(enchantment);
        String sourceOrigin = PenchantmentDefinition.getSource(enchantment, PROGRESS_COST_FACTOR);

        source.sendSuccess(() -> Component.literal("--- Penchant Progress Cost Factor: ")
                .withStyle(ChatFormatting.GOLD)
                .append(Component.literal(String.valueOf(id)).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" ---").withStyle(ChatFormatting.GOLD)), false);

        source.sendSuccess(() -> Component.literal("Source: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(sourceOrigin).withStyle(ChatFormatting.AQUA)), false);

        source.sendSuccess(() -> Component.literal("Base: ").withStyle(ChatFormatting.WHITE)
                .append(Component.literal(String.valueOf(def.progressCostFactor().base())).withStyle(ChatFormatting.GREEN))
                .append(Component.literal(" | Per Level: ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(String.valueOf(def.progressCostFactor().perLevel())).withStyle(ChatFormatting.GREEN)), false);

        int maxLvl = Math.max(enchantment.getMaxLevel(), 1);
        for (int i = 1; i <= maxLvl; i++) {
            final int targetLvl = i;
            int factor = def.getProgressCostFactor(targetLvl);
            source.sendSuccess(() -> Component.literal("  • Level " + targetLvl + ": ").withStyle(ChatFormatting.DARK_GRAY)
                    .append(Component.literal(String.valueOf(factor)).withStyle(ChatFormatting.WHITE)), false);
        }

        return 1;
    }

    private static int setProgressCostFactor(CommandSourceStack source, Enchantment enchantment, int base, int perLevel) throws CommandSyntaxException {
        ResourceLocation id = BuiltInRegistries.ENCHANTMENT.getKey(enchantment);
        if (PenchantmentDefinition.isDatapackForced(enchantment, PROGRESS_COST_FACTOR)) {
            throw ERROR_DATAPACK_LOCKED.create(new Object[]{"progress_cost_factor", id});
        }

        PenchantmentDefinition.Partial partial = PenchantmentDefinition.getOrCreateConfigPartial(id);
        partial.setProgressCostFactor(new PenchantmentDefinition.Cost(base, perLevel));
        PenchantConfigManager.save();
        PenchantmentDefinition.invalidateCache();
        PenchantNetworking.broadcastSyncDefinitions();

        source.sendSuccess(() -> Component.literal("[Penchant] Set progress_cost_factor for '")
                .withStyle(ChatFormatting.GREEN)
                .append(Component.literal(String.valueOf(id)).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("' to Base: " + base + ", Per Level: " + perLevel + " (saved to server config).")), true);

        return 1;
    }

    private static int queryExperienceCost(CommandSourceStack source, Enchantment enchantment) {
        ResourceLocation id = BuiltInRegistries.ENCHANTMENT.getKey(enchantment);
        PenchantmentDefinition def = PenchantmentDefinition.getDefinition(enchantment);
        String sourceOrigin = PenchantmentDefinition.getSource(enchantment, EXPERIENCE_COST);

        source.sendSuccess(() -> Component.literal("[Penchant] Experience Cost for '")
                .withStyle(ChatFormatting.GOLD)
                .append(Component.literal(String.valueOf(id)).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("': " + def.experienceCost() + " levels (Source: " + sourceOrigin + ")").withStyle(ChatFormatting.WHITE)), false);

        return 1;
    }

    private static int setExperienceCost(CommandSourceStack source, Enchantment enchantment, int cost) throws CommandSyntaxException {
        ResourceLocation id = BuiltInRegistries.ENCHANTMENT.getKey(enchantment);
        if (PenchantmentDefinition.isDatapackForced(enchantment, EXPERIENCE_COST)) {
            throw ERROR_DATAPACK_LOCKED.create(new Object[]{"experience_cost", id});
        }

        PenchantmentDefinition.Partial partial = PenchantmentDefinition.getOrCreateConfigPartial(id);
        partial.setExperienceCost(cost);
        PenchantConfigManager.save();
        PenchantmentDefinition.invalidateCache();
        PenchantNetworking.broadcastSyncDefinitions();

        source.sendSuccess(() -> Component.literal("[Penchant] Set experience_cost for '")
                .withStyle(ChatFormatting.GREEN)
                .append(Component.literal(String.valueOf(id)).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("' to " + cost + " levels (saved to server config).")), true);

        return 1;
    }

    private static int queryBookRequirement(CommandSourceStack source, Enchantment enchantment) {
        ResourceLocation id = BuiltInRegistries.ENCHANTMENT.getKey(enchantment);
        PenchantmentDefinition def = PenchantmentDefinition.getDefinition(enchantment);
        String sourceOrigin = PenchantmentDefinition.getSource(enchantment, BOOK_REQUIREMENT);

        source.sendSuccess(() -> Component.literal("[Penchant] Bookshelf Requirement for '")
                .withStyle(ChatFormatting.GOLD)
                .append(Component.literal(String.valueOf(id)).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("': " + def.bookRequirement() + " bookshelves (Source: " + sourceOrigin + ")").withStyle(ChatFormatting.WHITE)), false);

        return 1;
    }

    private static int setBookRequirement(CommandSourceStack source, Enchantment enchantment, int books) throws CommandSyntaxException {
        ResourceLocation id = BuiltInRegistries.ENCHANTMENT.getKey(enchantment);
        if (PenchantmentDefinition.isDatapackForced(enchantment, BOOK_REQUIREMENT)) {
            throw ERROR_DATAPACK_LOCKED.create(new Object[]{"book_requirement", id});
        }

        PenchantmentDefinition.Partial partial = PenchantmentDefinition.getOrCreateConfigPartial(id);
        partial.setBookRequirement(books);
        PenchantConfigManager.save();
        PenchantmentDefinition.invalidateCache();
        PenchantNetworking.broadcastSyncDefinitions();

        source.sendSuccess(() -> Component.literal("[Penchant] Set book_requirement for '")
                .withStyle(ChatFormatting.GREEN)
                .append(Component.literal(String.valueOf(id)).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("' to " + books + " bookshelves (saved to server config).")), true);

        return 1;
    }

    private static int resetEnchantment(CommandSourceStack source, Enchantment enchantment) {
        ResourceLocation id = BuiltInRegistries.ENCHANTMENT.getKey(enchantment);
        boolean removed = PenchantmentDefinition.removeConfigOverride(id);
        if (removed) {
            PenchantConfigManager.save();
            PenchantNetworking.broadcastSyncDefinitions();
            source.sendSuccess(() -> Component.literal("[Penchant] Reset server config overrides for '")
                    .withStyle(ChatFormatting.GREEN)
                    .append(Component.literal(String.valueOf(id)).withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal("'.")), true);
        } else {
            source.sendSuccess(() -> Component.literal("[Penchant] No server config overrides were active for '")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(String.valueOf(id)).withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal("'.")), false);
        }
        return 1;
    }

    private static int setDebugProgress(CommandSourceStack source, Enchantment enchantment, int level, int progress) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            throw ERROR_NO_ITEM.create();
        }

        ResourceLocation id = BuiltInRegistries.ENCHANTMENT.getKey(enchantment);
        ItemStack modified = PenchantmentHelper.enchant(stack, enchantment, level);
        if (modified != stack) {
            player.setItemInHand(InteractionHand.MAIN_HAND, modified);
            stack = modified;
        }

        EnchantmentProgress.Mutable mutable = EnchantmentProgress.getProgress(stack).toMutable();
        if (progress <= 0) {
            mutable.removeProgress(enchantment);
        } else {
            mutable.setProgress(enchantment, progress);
        }
        EnchantmentProgress.setProgress(stack, mutable.toImmutable());

        int maxProgress = EnchantmentProgress.getMaxProgress(enchantment, level, stack.getMaxDamage());
        source.sendSuccess(() -> Component.literal("[Penchant Debug] Set '")
                .withStyle(ChatFormatting.GREEN)
                .append(Component.literal(String.valueOf(id)).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("' on held item to Level " + level + " (Progress: " + progress + "/" + maxProgress + ").")), true);

        return 1;
    }
}
