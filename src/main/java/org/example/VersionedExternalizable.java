package org.example;

import org.example.adapters.FieldAdapter;
import org.example.annotations.SerializeWith;
import org.example.annotations.Since;
import org.example.annotations.Until;
import org.example.annotations.Version;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.Field;

import static org.apache.commons.lang3.reflect.FieldUtils.getAllFields;

public abstract class VersionedExternalizable implements Externalizable {



    private static int getClassVersion(Class<?> clazz) {
        Version versionAnnotation = clazz.getAnnotation(Version.class);
        return (versionAnnotation != null) ? versionAnnotation.value() : 1;
    }


    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        int currentVersion = getClassVersion(this.getClass());
        out.writeInt(currentVersion);
        try{
            serialize(this, out, currentVersion);
        } catch (ReflectiveOperationException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        int serializedVersion = in.readInt();
        try {
            deserialize(this, in, serializedVersion, getClassVersion(this.getClass()));
        } catch (ReflectiveOperationException e) {
            throw new IOException(e);
        }
    }


    private static void serialize(Object obj, ObjectOutput out, int currentVersion) throws ReflectiveOperationException, IOException {
        for(Field field: getAllFields(obj.getClass())){
            field.setAccessible(true);

            Since since = field.getAnnotation(Since.class);
            if(since != null && currentVersion < since.value()){
                continue;
            }

            Until until = field.getAnnotation(Until.class);
            if(until != null && currentVersion >= until.value()){
                continue;
            }


            SerializeWith serializeWith = field.getAnnotation(SerializeWith.class);
            if(serializeWith == null){
                continue;
            }


            try{

                FieldAdapter<?> adapter = instantiateAdapter(serializeWith);

                writeField(field.get(obj), out, adapter);

            }catch (Exception e ){
                throw new IOException(String.format(
                        "Error serializing field '%s' of class '%s' with adapter '%s', in version = %d",
                        field.getName(),
                        obj.getClass().getName(),
                        serializeWith.value().getName(),
                        currentVersion
                ), e);
            }


        }
    }


    private static void deserialize(Object obj, ObjectInput in, int serializedVersion, int currentVersion) throws ReflectiveOperationException, IOException, ClassNotFoundException {
        for(Field field: getAllFields(obj.getClass())){
            field.setAccessible(true);

            Since since = field.getAnnotation(Since.class);
            if(since != null && serializedVersion < since.value()){
                continue;
            }


            SerializeWith serializeWith = field.getAnnotation(SerializeWith.class);
            if(serializeWith == null){
                continue;
            }


            try{

                FieldAdapter<?> adapter = instantiateAdapter(serializeWith);

                boolean assign = true;

                Until until = field.getAnnotation(Until.class);
                if(until != null && currentVersion >= until.value()){
                    assign = false;
                }

                if (assign) {
                    readField(field, obj, in, adapter);
                } else {
                    //Consume value and not assign to anything because it is being read in a version that no longer supports it
                    adapter.read(in);
                }


            }catch (Exception e ){
                throw new IOException(String.format(
                        "Error deserializing field '%s' of class '%s' with adapter '%s', in version = %d, serialized in version = %d",
                        field.getName(),
                        obj.getClass().getName(),
                        serializeWith.value().getName(),
                        currentVersion,
                        serializedVersion
                ), e);
            }


        }
    }


    @SuppressWarnings("unchecked")
    private static FieldAdapter<?> instantiateAdapter(SerializeWith serializeWith) throws ReflectiveOperationException {
        Class<?> adapterClass = serializeWith.value();
        Class<?> elementClass = serializeWith.elementType();

        if (elementClass != Void.class) {
            return (FieldAdapter<?>) adapterClass.getDeclaredConstructor(Class.class).newInstance(elementClass);
        } else {
            return (FieldAdapter<?>) adapterClass.getDeclaredConstructor().newInstance();
        }
    }



    private static <T> void writeField(Object value, ObjectOutput out, FieldAdapter<T> adapter){
        adapter.write(out, (T) value);
    }

    private static <T> void readField(Field field, Object obj, ObjectInput in, FieldAdapter<T> adapter) throws IOException, ClassNotFoundException, IllegalAccessException {
        T value = adapter.read(in);
        field.set(obj, value);
    }
}
