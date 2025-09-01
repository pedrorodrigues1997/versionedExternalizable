package org.example.adapters;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public interface FieldAdapter<T> {

    public void write(ObjectOutput out, T value);
    T read(ObjectInput in) throws IOException, ClassNotFoundException;
}
