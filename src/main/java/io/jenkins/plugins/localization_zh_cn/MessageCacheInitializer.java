package io.jenkins.plugins.localization_zh_cn;

import hudson.init.InitMilestone;
import hudson.init.Initializer;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import jenkins.model.Jenkins;
import org.jvnet.localizer.ResourceBundleHolder;

public class MessageCacheInitializer {
    private static final Logger LOGGER = Logger.getLogger(MessageCacheInitializer.class.getName());

    @Initializer(after = InitMilestone.JOB_CONFIG_ADAPTED, fatal = false)
    public static void refreshMessages() {
        try {
            Field bundles = ResourceBundleHolder.class.getDeclaredField("bundles");
            bundles.setAccessible(true);
            for (String name : messageClasses()) {
                refreshMessages(name, bundles);
            }
        } catch (ReflectiveOperationException | IOException | URISyntaxException | RuntimeException e) {
            LOGGER.log(Level.WARNING, "Could not refresh cached Chinese messages", e);
        }
    }

    private static List<String> messageClasses() throws IOException, URISyntaxException {
        Path location = Path.of(MessageCacheInitializer.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        if (Files.isDirectory(location)) {
            try (Stream<Path> files = Files.walk(location)) {
                return files.filter(Files::isRegularFile)
                        .map(location::relativize)
                        .map(path -> path.toString().replace('\\', '/'))
                        .filter(name -> name.endsWith("/Messages_zh_CN.properties"))
                        .map(MessageCacheInitializer::messageClassName)
                        .toList();
            }
        }
        try (JarFile jar = new JarFile(location.toFile())) {
            return jar.stream()
                    .filter(entry -> !entry.isDirectory())
                    .map(entry -> entry.getName())
                    .filter(name -> name.endsWith("/Messages_zh_CN.properties"))
                    .map(MessageCacheInitializer::messageClassName)
                    .toList();
        }
    }

    private static String messageClassName(String resource) {
        return resource.substring(0, resource.length() - "Messages_zh_CN.properties".length())
                .replace('/', '.') + "Messages";
    }

    private static void refreshMessages(String name, Field bundles) {
        try {
            Class<?> messages = Jenkins.get().getPluginManager().uberClassLoader.loadClass(name);
            Field holder = messages.getDeclaredField("holder");
            if (!Modifier.isStatic(holder.getModifiers()) || holder.getType() != ResourceBundleHolder.class) {
                return;
            }
            holder.setAccessible(true);
            // Generated Messages classes retain old holders even after the localizer clears its registry.
            Object cache = bundles.get(holder.get(null));
            if (cache instanceof Map<?, ?>) {
                ((Map<?, ?>) cache).remove(Locale.SIMPLIFIED_CHINESE);
            }
        } catch (ClassNotFoundException | NoSuchFieldException e) {
            // Some translations belong to plugins that are not installed or to Jelly views.
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOGGER.log(Level.WARNING, "Could not refresh cached Chinese messages for " + name, e);
        }
    }
}
