package org.example.context.values.scalar;

import org.example.context.ReadContextValue;

public class DeleteMarker extends ReadContextValue {
    public static final DeleteMarker INSTANCE = new DeleteMarker();
    private DeleteMarker() {}

    @Override
    public String asString() {
        return "Delete Marker";
    }
}
