package io.github.thatrobin.functionmacrobackport.utils;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.nbt.*;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.function.CommandFunction;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class Macro extends CommandFunction implements CommandFunctionAccess {
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#");
    private final List<String> variables;
    private static final int CACHE_SIZE = 8;
    private final Object2ObjectLinkedOpenHashMap<List<String>, CommandFunction> cache = new Object2ObjectLinkedOpenHashMap(8, 0.25F);

    public Macro(Identifier id, Element[] elements, List<String> variables) {
        super(id, elements);
        this.variables = variables;
    }

    @Override
    public CommandFunction functionMacroBackport$withMacroReplaced(@Nullable NbtCompound arguments, CommandDispatcher<ServerCommandSource> dispatcher, ServerCommandSource source) throws MacroException {
        if (arguments == null) {
            throw new MacroException(Text.translatable("commands.function.error.missing_arguments", this.getId()));
        } else {
            List<String> list = new ArrayList(this.variables.size());
            Iterator var5 = this.variables.iterator();

            while(var5.hasNext()) {
                String string = (String)var5.next();
                if (!arguments.contains(string)) {
                    throw new MacroException(Text.translatable("commands.function.error.missing_argument", this.getId(), string));
                }

                list.add(toString(arguments.get(string)));
            }

            CommandFunction commandFunction = (CommandFunction)this.cache.getAndMoveToLast(list);
            if (commandFunction != null) {
                return commandFunction;
            } else {
                if (this.cache.size() >= 8) {
                    this.cache.removeFirst();
                }

                CommandFunction commandFunction2 = this.withMacroReplaced(list, dispatcher, source);
                if (commandFunction2 != null) {
                    this.cache.put(list, commandFunction2);
                }

                return commandFunction2;
            }
        }
    }

    private static String toString(NbtElement nbt) {
        if (nbt instanceof NbtFloat nbtFloat) {
            return DECIMAL_FORMAT.format((double)nbtFloat.floatValue());
        } else if (nbt instanceof NbtDouble nbtDouble) {
            return DECIMAL_FORMAT.format(nbtDouble.doubleValue());
        } else if (nbt instanceof NbtByte nbtByte) {
            return String.valueOf(nbtByte.byteValue());
        } else if (nbt instanceof NbtShort nbtShort) {
            return String.valueOf(nbtShort.shortValue());
        } else if (nbt instanceof NbtLong nbtLong) {
            return String.valueOf(nbtLong.longValue());
        } else {
            return nbt.asString();
        }
    }

    private CommandFunction withMacroReplaced(List<String> arguments, CommandDispatcher<ServerCommandSource> dispatcher, ServerCommandSource source) throws MacroException {
        Element[] elements = this.getElements();
        Element[] elements2 = new Element[elements.length];

        for(int i = 0; i < elements.length; ++i) {
            Element element = elements[i];
            if (!(element instanceof MacroElement macroElement)) {
                elements2[i] = element;
            } else {
                List<String> list = macroElement.getVariables();
                List<String> list2 = new ArrayList(list.size());
                Iterator var11 = list.iterator();

                while(var11.hasNext()) {
                    String string = (String)var11.next();
                    list2.add(arguments.get(this.variables.indexOf(string)));
                }

                String string2 = macroElement.getCommand(list2);

                try {
                    ParseResults<ServerCommandSource> parseResults = dispatcher.parse(string2, source);
                    if (parseResults.getReader().canRead()) {
                        throw CommandManager.getException(parseResults);
                    }

                    elements2[i] = new CommandElement(parseResults);
                } catch (CommandSyntaxException var13) {
                    throw new MacroException(Text.translatable("commands.function.error.parse", this.getId(), string2, var13.getMessage()));
                }
            }
        }

        Identifier identifier = this.getId();
        String var10004 = identifier.getNamespace();
        String var10005 = identifier.getPath();
        return new CommandFunction(new Identifier(var10004, var10005 + "/" + arguments.hashCode()), elements2);
    }

    static {
        DECIMAL_FORMAT.setMaximumFractionDigits(15);
        DECIMAL_FORMAT.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.US));
    }

}
