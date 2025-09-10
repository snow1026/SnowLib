package io.github.snow1026.snowlib.components;

import io.github.snow1026.snowlib.Snow;

public class Component<T extends Snow> {
    protected final T parent;

    public Component(T parent) {
        this.parent = parent;
    }

    public T getParent() {
        return parent;
    }
}
