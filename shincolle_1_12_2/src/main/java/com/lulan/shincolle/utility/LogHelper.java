package com.lulan.shincolle.utility;

import com.lulan.shincolle.handler.ConfigHandler;
import net.minecraftforge.fml.common.FMLLog;
import org.apache.logging.log4j.Level;

public class LogHelper {
    private LogHelper() {}

    private static void createLog(Level logLevel, Object object) {
        FMLLog.log("Shinkaiseikan Collection", logLevel, String.valueOf(object));
    }

    public static void warn(Object object) {
        LogHelper.createLog(Level.WARN, object);
    }

    public static void info(Object object) {
        LogHelper.createLog(Level.INFO, object);
    }

    public static void debug(Object object) {
        if (ConfigHandler.debugMode) {
            LogHelper.createLog(Level.INFO, object);
        }
    }

    public static void debugHighLevel(Object object) {
        if (ConfigHandler.debugMode) {
            LogHelper.createLog(Level.DEBUG, object);
        }
    }

    public static void all(Object object) {
        LogHelper.createLog(Level.ALL, object);
    }
}
