package cc.attodao.mob_life.server;

import cc.attodao.mob_life.gameplay.awkwardness.MorphAwkwardness;
import cc.attodao.mob_life.morph.MorphDefinition;
import cc.attodao.mob_life.morph.MorphType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import java.util.Arrays;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;

public final class MobLifeCommand {

    private static final DynamicCommandExceptionType UNSUPPORTED_MORPH =
        new DynamicCommandExceptionType(id ->
            Component.translatable("commands.mob_life.morph.unsupported", id)
        );

    private MobLifeCommand() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register(
            (dispatcher, buildContext, selection) -> {
                dispatcher.register(
                    Commands.literal("moblife")
                        .requires(
                            source ->
                                source.getServer() == null ||
                                !source.getServer().isDedicatedServer() ||
                                Commands.hasPermission(
                                    Commands.LEVEL_GAMEMASTERS
                                ).test(source)
                        )
                        .then(
                            Commands.literal("morph").then(
                                Commands.argument(
                                    "entity",
                                    ResourceArgument.resource(
                                        buildContext,
                                        Registries.ENTITY_TYPE
                                    )
                                )
                                    .suggests((context, builder) ->
                                        SharedSuggestionProvider.suggestResource(
                                            Arrays.stream(MorphType.values())
                                                .map(MorphType::entityType)
                                                .map(
                                                    BuiltInRegistries.ENTITY_TYPE::getKey
                                                ),
                                            builder
                                        )
                                    )
                                    .executes(context ->
                                        changeMorph(
                                            context,
                                            ResourceArgument.getEntityType(
                                                context,
                                                "entity"
                                            ).value(),
                                            new CompoundTag()
                                        )
                                    )
                                    .then(
                                        Commands.argument(
                                            "nbt",
                                            CompoundTagArgument.compoundTag()
                                        ).executes(context ->
                                            changeMorph(
                                                context,
                                                ResourceArgument.getEntityType(
                                                    context,
                                                    "entity"
                                                ).value(),
                                                CompoundTagArgument.getCompoundTag(
                                                    context,
                                                    "nbt"
                                                )
                                            )
                                        )
                                    )
                            )
                        )
                        .then(
                            Commands.literal("awkwardness").then(
                                Commands.argument(
                                    "value",
                                    IntegerArgumentType.integer(
                                        (int) MorphAwkwardness.MINIMUM,
                                        (int) MorphAwkwardness.MAXIMUM
                                    )
                                ).executes(MobLifeCommand::setAwkwardness)
                            )
                        )
                );
            }
        );
    }

    private static int setAwkwardness(
        CommandContext<CommandSourceStack> context
    ) throws CommandSyntaxException {
        int value = IntegerArgumentType.getInteger(context, "value");
        ServerMorphManager.setAwkwardness(
            context.getSource().getPlayerOrException(),
            value
        );
        context
            .getSource()
            .sendSuccess(
                () ->
                    Component.translatable(
                        "commands.mob_life.awkwardness.success",
                        value
                    ),
                false
            );
        return value;
    }

    private static int changeMorph(
        CommandContext<CommandSourceStack> context,
        EntityType<?> entityType,
        CompoundTag nbt
    ) throws CommandSyntaxException {
        MorphType morph = MorphType.fromEntityType(entityType).orElseThrow(() ->
            UNSUPPORTED_MORPH.create(
                BuiltInRegistries.ENTITY_TYPE.getKey(entityType)
            )
        );
        ServerMorphManager.changeMorph(
            context.getSource().getServer(),
            new MorphDefinition(morph, nbt)
        );
        context
            .getSource()
            .sendSuccess(
                () ->
                    Component.translatable(
                        "commands.mob_life.morph.success",
                        Component.translatable(morph.translationKey())
                    ),
                true
            );
        return 1;
    }
}
