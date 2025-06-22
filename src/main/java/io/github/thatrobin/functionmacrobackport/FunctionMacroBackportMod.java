package io.github.thatrobin.functionmacrobackport;

import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FunctionMacroBackportMod implements ModInitializer {

	public static final String MODID = "functionmacrobackport";
	public static String VERSION = "";
	public static final Logger LOGGER = LogManager.getLogger(FunctionMacroBackportMod.class);

	@Override
	public void onInitialize() {
	}

	public static Identifier identifier(String path) {
		return new Identifier(FunctionMacroBackportMod.MODID, path);
	}

}
