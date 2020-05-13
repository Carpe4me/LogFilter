package com.legendmohe.tool.annotation;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by xinyu.he on 2016/1/14.
 */
public class UIStateSaver {

    private Class[] mSupportedSaveState = new Class[]{
            SaveState.class,
            CheckBoxSaveState.class,
            TextFieldSaveState.class,
            FieldSaveState.class
    };

    private ArrayList<Field> mFields = new ArrayList<>();
    private Map<String, Serializable> mSerializableMap = new HashMap<>();
    private Object mContext;

    private PersistenceHelper persistenceHelper;

    public UIStateSaver(Object target, PersistenceHelper helper) {
        persistenceHelper = helper;
        register(target);
    }

    public UIStateSaver(Object target, String filePath) {
        persistenceHelper = new DefaultPersistenceHelper(filePath);
        register(target);
    }

    private void register(Object context) {
        if (context == null) {
            throw new NullPointerException();
        }

        Class targetClass = context.getClass();
        Field[] fields = targetClass.getDeclaredFields();
        for (Field field : fields) {
            for (Class stateClass : mSupportedSaveState) {
                if (field.isAnnotationPresent(stateClass)) {
                    mFields.add(field);
                    break;
                }
            }
        }
        mContext = context;
    }

    public void save() {
        save(persistenceHelper);
    }

    public void save(PersistenceHelper helper) {
        for (Field field : mFields) {
            for (Class stateClass : mSupportedSaveState) {
                if (field.isAnnotationPresent(stateClass)) {
                    try {
                        field.setAccessible(true);
                        processSave(field, stateClass);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }
        if (helper != null) {
            helper.serialize(mSerializableMap);
        }
    }

    public void load(PersistenceHelper helper) {
        if (helper != null) {
            mSerializableMap.putAll(helper.deserialize());
        }
        if (mSerializableMap.size() == 0) {
            return;
        }
        for (Field field : mFields) {
            for (Class stateClass : mSupportedSaveState) {
                if (field.isAnnotationPresent(stateClass)) {
                    try {
                        field.setAccessible(true);
                        processLoad(field, stateClass);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }
    }

    public void load() {
        load(persistenceHelper);
    }

    protected void processSave(Field field, Class stateClass) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Object obj = field.get(mContext);
        String name = field.getName();

        if (stateClass == FieldSaveState.class) {
            putSerializable(name, obj);
            return;
        }

        Annotation annotation = field.getAnnotation(stateClass);
        Method annoGetter = annotation.annotationType().getMethod("getter");
        Method setMethod = obj.getClass().getMethod((String) annoGetter.invoke(annotation));
        Object value = setMethod.invoke(obj);
        putSerializable(name, value);
    }

    protected void processLoad(Field field, Class stateClass) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Object value = mSerializableMap.get(field.getName());
        if (value == null) {
            return;
        }
        if (stateClass == FieldSaveState.class) {
            if (isWrapperType(field.getType())) {
                field.set(mContext, value);
            } else if (field.getType() == Object.class || field.getType() == Object[].class) {
                field.set(mContext, value);
            } else if (field.getType() == int.class) {
                field.setInt(mContext, (Integer) value);
            } else if (field.getType() == long.class) {
                field.setLong(mContext, (Long) value);
            } else if (field.getType() == boolean.class) {
                field.setBoolean(mContext, (Boolean) value);
            } else if (field.getType() == short.class) {
                field.setShort(mContext, (Short) value);
            } else if (field.getType() == byte.class) {
                field.setByte(mContext, (Byte) value);
            } else if (field.getType() == char.class) {
                field.setChar(mContext, (Character) value);
            } else if (field.getType() == double.class) {
                field.setDouble(mContext, (Double) value);
            } else if (field.getType() == float.class) {
                field.setFloat(mContext, (Float) value);
            } else {
                field.set(mContext, value);
            }
            return;
        }

        Object obj = field.get(mContext);
        Annotation annotation = field.getAnnotation(stateClass);
        Method annoSetter = annotation.annotationType().getMethod("setter");
        String setterName = (String) annoSetter.invoke(annotation);
        Method setter = null;
        for (Method method : obj.getClass().getMethods()) {
            if (method.getParameterCount() == 1 && method.getName().equals(setterName)) {
                setter = method;
                break;
            }
        }
        if (setter != null) {
            setter.invoke(obj, value);
        }
    }

    // ----------------------------------------------------------------------

    private void putSerializable(String key, Object obj) {
        if (obj instanceof Serializable && mSerializableMap != null) {
            mSerializableMap.put(key, (Serializable) obj);
        }
    }

    // ----------------------------------------------------------------------

    private static final Set<Class<?>> WRAPPER_TYPES;

    static {
        WRAPPER_TYPES = new HashSet<>();
        WRAPPER_TYPES.add(Boolean.class);
        WRAPPER_TYPES.add(Character.class);
        WRAPPER_TYPES.add(Byte.class);
        WRAPPER_TYPES.add(Short.class);
        WRAPPER_TYPES.add(Integer.class);
        WRAPPER_TYPES.add(Long.class);
        WRAPPER_TYPES.add(Float.class);
        WRAPPER_TYPES.add(Double.class);
        WRAPPER_TYPES.add(Void.class);
    }

    private static boolean isWrapperType(Class<?> clazz) {
        return WRAPPER_TYPES.contains(clazz);
    }

    ///////////////////////////////////listener///////////////////////////////////

    public interface PersistenceHelper {
        Map<String, Serializable> deserialize();
        void serialize(Map<String, Serializable> data);
    }

    public static class DefaultPersistenceHelper implements PersistenceHelper {

        private String path;

        public DefaultPersistenceHelper(String path) {
            this.path = path;
        }

        @Override
        public Map<String, Serializable> deserialize() {
            return deserialize(path);
        }

        @Override
        public void serialize(Map<String, Serializable> data) {
            serialize(data, path);
        }

        private boolean serialize(Map<String, Serializable> data, String path) {
            FileOutputStream fos = null;
            ObjectOutputStream oos = null;
            try {
                fos = new FileOutputStream(path);
                oos = new ObjectOutputStream(fos);
                oos.writeObject(data);
                oos.close();
                return true;
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                try {
                    if (fos != null) {
                        fos.close();
                    }
                    if (oos != null) {
                        oos.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return false;
        }

        private Map<String, Serializable> deserialize(String path) {
            FileInputStream fos = null;
            ObjectInputStream oos = null;
            try {
                fos = new FileInputStream(path);
                oos = new ObjectInputStream(fos);
                HashMap<String, Serializable> result = (HashMap<String, Serializable>) oos.readObject();
                oos.close();
                return result;
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                try {
                    if (fos != null) {
                        fos.close();
                    }
                    if (oos != null) {
                        oos.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return Collections.emptyMap();
        }
    }
}
