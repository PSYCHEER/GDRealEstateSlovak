package me.EtienneDx.RealEstate.config;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.objectmapping.meta.NodeResolver;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.lang.reflect.AnnotatedElement;

public class RealEstateNodeResolver implements NodeResolver.Factory {

    @Override
    public @Nullable NodeResolver make(String name, AnnotatedElement element) {
        return node -> {
            if (element.isAnnotationPresent(Setting.class)) {
                final String key = element.getAnnotation(Setting.class).value();
                if (!key.isEmpty()) {
                    if (key.contains(".")) {
                        final String[] nodes = key.split("\\.");
                        node = node.node(nodes);
                    } else {
                        node = node.node(key);
                    }
                    return node;
                }
            }
            return null;
        };
    }

}
