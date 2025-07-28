package org.xznetwork.ecopower;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.xznetwork.ecopower.config.Config;
import org.xznetwork.ecopower.config.PowerPlanDetector;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

public class EcoPowerCommand implements Command<ServerCommandSource> {
    private final EcoPower mod;
    private final PowerPlanDetector planDetector;

    public EcoPowerCommand(EcoPower mod) {
        this.mod = mod;
        this.planDetector = new PowerPlanDetector();
    }

    @Override
    public int run(CommandContext<ServerCommandSource> context) {
        String[] args = context.getInput().split(" ");
        if (args.length < 2) {
            context.getSource().sendError(Text.literal("Usage: /ecopower <reload|show|set>"));
            return 0;
        }

        String mainCommand = args[1];

        if ("reload".equals(mainCommand)) {
            return handleReload(context);
        } else if ("show".equals(mainCommand)) {
            if (args.length < 3) {
                return showAll(context);
            }

            String subCommand = args[2];
            switch (subCommand) {
                case "plans":
                    return showPlans(context);
                case "exclusion":
                    return showExclusion(context);
                case "activation":
                    return showActivation(context);
                default:
                    return showAll(context);
            }
        } else if ("set".equals(mainCommand)) {
            if (args.length < 4 || !"plan".equals(args[2])) {
                context.getSource().sendError(Text.literal("Usage: /ecopower set plan <plan_name|auto>"));
                return 0;
            }
            String planName = args[3];
            return handleSetPlan(context, planName);
        }

        context.getSource().sendError(Text.literal("Unknown command"));
        return 0;
    }

    private int handleReload(CommandContext<ServerCommandSource> context) {
        context.getSource().sendFeedback(() ->
                        Text.literal("Reloading EcoPower configuration..."),
                false
        );
        mod.reloadConfig();
        context.getSource().sendFeedback(() ->
                        Text.literal("Reloading EcoPower configuration successful!").formatted(Formatting.GREEN),
                false
        );
        return Command.SINGLE_SUCCESS;
    }

    private int handleSetPlan(CommandContext<ServerCommandSource> context, String planName) {
        if ("auto".equalsIgnoreCase(planName)) {
            mod.disableManualOverride();
            context.getSource().sendFeedback(() ->
                            Text.literal("Switched to automatic power plan mode")
                                    .formatted(Formatting.GREEN),
                    false);
            return Command.SINGLE_SUCCESS;
        }

        Config config = mod.getConfig();
        Map<String, Config.PlanEntry> plans = config.getPlan();
        Config.PlanEntry planEntry = plans.get(planName);

        if (planEntry == null) {
            context.getSource().sendError(Text.literal("Unknown power plan: " + planName)
                    .formatted(Formatting.RED));
            return 0;
        }

        mod.enableManualOverride();

        try {
            mod.getPowerPlanManager().setActivePlan(planEntry.getGuid());
            context.getSource().sendFeedback(() ->
                            Text.literal("Manually switched to power plan: ")
                                    .append(Text.literal(planName).formatted(Formatting.GOLD))
                                    .formatted(Formatting.GREEN),
                    false);

            // 广播切换消息
            //if (config.isBroadcastChanges()) {
            //    mod.broadcastMessage(context.getSource().getServer(), planEntry.getBroadcastMessage());
            //}

            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            context.getSource().sendError(Text.literal("Failed to switch power plan: " + e.getMessage())
                    .formatted(Formatting.RED));
            return 0;
        }
    }

    private int showAll(CommandContext<ServerCommandSource> context) {
        showPlans(context);
        showExclusion(context);
        showActivation(context);
        return Command.SINGLE_SUCCESS;
    }

    private int showPlans(CommandContext<ServerCommandSource> context) {
        try {
            String currentGuid = planDetector.getCurrentActivePlan();
            Config config = mod.getConfig();

            // 获取所有计划配置
            Map<String, Config.PlanEntry> plans = config.getPlan();

            // 创建标题文本
            MutableText title = Text.literal("===== Power Plans =====")
                    .formatted(Formatting.BOLD, Formatting.GOLD);

            context.getSource().sendFeedback(() -> title, false);

            if (plans.isEmpty()) {
                // 没有计划配置时的提示
                context.getSource().sendFeedback(() ->
                                Text.literal("No power plans configured")
                                        .formatted(Formatting.ITALIC, Formatting.RED),
                        false);
            } else {
                // 遍历并显示所有计划
                for (Map.Entry<String, Config.PlanEntry> entry : plans.entrySet()) {
                    String planName = entry.getKey();
                    Config.PlanEntry plan = entry.getValue();

                    // 检查是否为当前活动计划
                    boolean isActive = currentGuid != null && currentGuid.equals(plan.getGuid());

                    // 构建计划信息文本
                    MutableText planText = Text.literal("» " + planName + ":")
                            .formatted(Formatting.BOLD,
                                    isActive ? Formatting.GREEN : Formatting.WHITE);

                    // 添加详细信息
                    planText.append("\n  GUID: ").append(
                            Text.literal(plan.getGuid())
                                    .formatted(Formatting.GRAY));

                    planText.append("\n  Range: ").append(
                            Text.literal(Arrays.toString(plan.getRange()))
                                    .formatted(Formatting.YELLOW));

                    planText.append("\n  Message: ").append(
                            Text.literal(plan.getBroadcastMessage())
                                    .formatted(Formatting.AQUA));

                    // 添加当前活动状态标记
                    if (isActive) {
                        planText.append("\n  ").append(
                                Text.literal("ACTIVE")
                                        .formatted(Formatting.BOLD, Formatting.GREEN));
                    }

                    context.getSource().sendFeedback(() -> planText, false);
                }
            }

            context.getSource().sendFeedback(() ->
                            Text.literal("-----------------------")
                                    .formatted(Formatting.DARK_GRAY),
                    false);

            MutableText currentInfo = Text.literal("Current Active: ")
                    .formatted(Formatting.GRAY)
                    .append(Text.literal(currentGuid != null ? currentGuid : "None")
                            .formatted(Formatting.GOLD));

            context.getSource().sendFeedback(() -> currentInfo, false);

        } catch (IOException e) {
            context.getSource().sendError(
                    Text.literal("Failed to get power plan info: " + e.getMessage())
                            .formatted(Formatting.RED));
            return 0;
        }
        return Command.SINGLE_SUCCESS;
    }

    private int showExclusion(CommandContext<ServerCommandSource> context) {
        Config config = mod.getConfig();

        context.getSource().sendFeedback(() ->
                        Text.literal("=== Player Exclusion ==="),
                false
        );

        context.getSource().sendFeedback(() ->
                        Text.literal(" - Prefixes: " + config.getExclude().getPrefixes()),
                false
        );

        context.getSource().sendFeedback(() ->
                        Text.literal(" - Suffixes: " + config.getExclude().getSuffixes()),
                false
        );

        context.getSource().sendFeedback(() ->
                        Text.literal(" - Regexes: " + config.getExclude().getRegexes()),
                false
        );

        // Show current player exclusion status
        int excludedCount = 0;
        int totalCount = 0;
        for (ServerPlayerEntity player : context.getSource().getServer().getPlayerManager().getPlayerList()) {
            totalCount++;
            String name = player.getName().getString();
            if (mod.getPlayerExclusion().shouldExcludePlayer(name)) {
                excludedCount++;
            }
        }

        int finalTotalCount = totalCount;
        int finalExcludedCount = excludedCount;

        context.getSource().sendFeedback(() ->
                        Text.literal(String.format("Players: %d total, %d excluded", finalTotalCount, finalExcludedCount)),
                false
        );

        return Command.SINGLE_SUCCESS;
    }

    private int showActivation(CommandContext<ServerCommandSource> context) {
        Config config = mod.getConfig();

        context.getSource().sendFeedback(() ->
                        Text.literal("=== Activation Settings ==="),
                false
        );

        context.getSource().sendFeedback(() ->
                        Text.literal(" - Broadcast Changes: " + config.isBroadcastChanges()),
                false
        );

        return Command.SINGLE_SUCCESS;
    }

    public static void register(
            com.mojang.brigadier.CommandDispatcher<ServerCommandSource> dispatcher,
            EcoPower mod
    ) {
        SuggestionProvider<ServerCommandSource> planSuggester = (context, builder) -> {
            builder.suggest("auto");

            Config config = mod.getConfig();
            for (String planName : config.getPlan().keySet()) {
                builder.suggest(planName);
            }
            return builder.buildFuture();
        };

        dispatcher.register(
               CommandManager.literal("ecopower")
                        .requires(source -> source.hasPermissionLevel(4)) // OP only
                        .then(CommandManager.literal("reload")
                                .executes(new EcoPowerCommand(mod))
                        )
                        .then(CommandManager.literal("show")
                                .executes(new EcoPowerCommand(mod)) // /ecopower show (all)
                                .then(CommandManager.literal("plans")
                                        .executes(new EcoPowerCommand(mod)) // /ecopower show plans
                                )
                                .then(CommandManager.literal("exclusion")
                                            .executes(new EcoPowerCommand(mod)) // /ecopower show exclusion
                                )
                                .then(CommandManager.literal("activation")
                                        .executes(new EcoPowerCommand(mod)) // /ecopower show activation
                                )
                        )
                        .then(CommandManager.literal("set")
                                .then(CommandManager.literal("plan")
                                        .then(CommandManager.argument("planName", StringArgumentType.string())
                                                .suggests(planSuggester)
                                                .executes(context -> {
                                                    String planName = StringArgumentType.getString(context, "planName");
                                                    return new EcoPowerCommand(mod).handleSetPlan(context, planName);
                                                })
                                        )
                                )
                        )
        );
    }
}