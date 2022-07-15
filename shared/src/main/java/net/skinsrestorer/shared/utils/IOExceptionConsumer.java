 package net.skinsrestorer.shared.utils;

import java.io.IOException;

public interface IOExceptionConsumer<V> {
    void accept(V value) throws IOException;
}
