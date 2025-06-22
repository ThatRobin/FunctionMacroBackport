package io.github.thatrobin.functionmacrobackport.mixin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import io.github.thatrobin.functionmacrobackport.utils.CommandFunctionManagerAccess;
import io.github.thatrobin.functionmacrobackport.utils.FunctionResult;
import io.github.thatrobin.functionmacrobackport.utils.MacroException;
import net.minecraft.command.DataCommandObject;
import net.minecraft.command.argument.CommandFunctionArgumentType;
import net.minecraft.command.argument.NbtCompoundArgumentType;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.DataCommand;
import net.minecraft.server.command.FunctionCommand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.function.CommandFunction;
import net.minecraft.text.Text;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

@Mixin(FunctionCommand.class)
public class FunctionCommandMixin {

    private static final DynamicCommandExceptionType ARGUMENT_NOT_COMPOUND_EXCEPTION = new DynamicCommandExceptionType(argument -> Text.translatable("commands.function.error.argument_not_compound", argument));

    @Inject(method = "register", at = @At("HEAD"), cancellable = true)
    private static void registerMacro(CommandDispatcher<ServerCommandSource> dispatcher, CallbackInfo ci) {
        LiteralArgumentBuilder<ServerCommandSource> literalArgumentBuilder = CommandManager.literal("with");
        for (DataCommand.ObjectType objectType : DataCommand.SOURCE_OBJECT_TYPES) {
            objectType.addArgumentsToBuilder(literalArgumentBuilder, builder -> ((ArgumentBuilder)builder.executes(context -> FunctionCommandMixin.execute((ServerCommandSource)context.getSource(), CommandFunctionArgumentType.getFunctions(context, "name"), objectType.getObject(context).getNbt()))).then(CommandManager.argument("path", NbtPathArgumentType.nbtPath()).executes(context -> FunctionCommandMixin.execute((ServerCommandSource)context.getSource(), CommandFunctionArgumentType.getFunctions(context, "name"), FunctionCommandMixin.getArgument(NbtPathArgumentType.getNbtPath(context, "path"), objectType.getObject(context))))));
        }
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)CommandManager.literal("function").requires(source -> source.hasPermissionLevel(2))).then(((RequiredArgumentBuilder)((RequiredArgumentBuilder)CommandManager.argument("name", CommandFunctionArgumentType.commandFunction()).suggests(FunctionCommand.SUGGESTION_PROVIDER).executes(context -> FunctionCommandMixin.execute((ServerCommandSource)context.getSource(), CommandFunctionArgumentType.getFunctions(context, "name"), null))).then(CommandManager.argument("arguments", NbtCompoundArgumentType.nbtCompound()).executes(context -> FunctionCommandMixin.execute((ServerCommandSource)context.getSource(), CommandFunctionArgumentType.getFunctions(context, "name"), NbtCompoundArgumentType.getNbtCompound(context, "arguments"))))).then(literalArgumentBuilder)));
        ci.cancel();
    }

    @Unique
    private static NbtCompound getArgument(NbtPathArgumentType.NbtPath path, DataCommandObject object) throws CommandSyntaxException {
        NbtElement nbtElement = DataCommand.getNbt(path, object);
        if (nbtElement instanceof NbtCompound) {
            NbtCompound nbtCompound = (NbtCompound)nbtElement;
            return nbtCompound;
        }
        throw ARGUMENT_NOT_COMPOUND_EXCEPTION.create(nbtElement.getNbtType().getCrashReportName());
    }

    private static int execute(ServerCommandSource source, Collection<CommandFunction> functions, @Nullable NbtCompound arguments) {
        int i = 0;
        boolean bl = false;
        boolean bl2 = false;
        for (CommandFunction commandFunction : functions) {
            try {
                FunctionResult functionResult = FunctionCommandMixin.execute(source, commandFunction, arguments);
                i += functionResult.value();
                bl |= functionResult.isReturn();
                bl2 = true;
            } catch (MacroException macroException) {
                source.sendError(Text.literal(macroException.getMessage()));
            }
        }
        if (bl2) {
            int j = i;
            if (functions.size() == 1) {
                if (bl) {
                    source.sendFeedback(() -> Text.translatable("commands.function.success.single.result", j, ((CommandFunction)functions.iterator().next()).getId()), true);
                } else {
                    source.sendFeedback(() -> Text.translatable("commands.function.success.single", j, ((CommandFunction)functions.iterator().next()).getId()), true);
                }
            } else if (bl) {
                source.sendFeedback(() -> Text.translatable("commands.function.success.multiple.result", functions.size()), true);
            } else {
                source.sendFeedback(() -> Text.translatable("commands.function.success.multiple", j, functions.size()), true);
            }
        }
        return i;
    }

    @Unique
    private static FunctionResult execute(ServerCommandSource source, CommandFunction function, @Nullable NbtCompound arguments) throws MacroException {
        MutableObject mutableObject = new MutableObject();
        int i = ((CommandFunctionManagerAccess)source.getServer().getCommandFunctionManager()).functionMacroBackport$execute(function, source.withSilent().withMaxLevel(2).withReturnValueConsumer(value -> mutableObject.setValue(new FunctionResult(value, true))), null, arguments);
        FunctionResult functionResult = (FunctionResult)mutableObject.getValue();
        if (functionResult != null) {
            return functionResult;
        }
        return new FunctionResult(i, false);
    }

}
