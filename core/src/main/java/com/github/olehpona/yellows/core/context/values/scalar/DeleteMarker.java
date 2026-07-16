package com.github.olehpona.yellows.core.context.values.scalar;

import com.github.olehpona.yellows.core.context.ReadContextValue;

public class DeleteMarker extends ReadContextValue {
    public static final DeleteMarker INSTANCE = new DeleteMarker();
    private DeleteMarker() {}

    @Override
    public String asString() {
        return "Delete Marker";
    }
}
