/*
 * Copyright 2014 mango.jfaster.org
 *
 * The Mango Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.jfaster.mango.reflect;

import org.jfaster.mango.exception.UncheckedException;
import org.jfaster.mango.invoker.FunctionalGetterInvoker;
import org.jfaster.mango.invoker.FunctionalSetterInvoker;
import org.jfaster.mango.invoker.GetterInvoker;
import org.jfaster.mango.invoker.SetterInvoker;
import org.jfaster.mango.util.concurrent.cache.CacheLoader;
import org.jfaster.mango.util.concurrent.cache.DoubleCheckCache;
import org.jfaster.mango.util.concurrent.cache.LoadingCache;

import javax.annotation.Nullable;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.*;

/**
 * @author ash
 */
public class BeanInfoCache {

    @Nullable
    public static GetterInvoker getGetterInvoker(Class<?> clazz, String propertyName) {
        return cache.get(clazz).getGetterInvoker(propertyName);
    }

    @Nullable
    public static SetterInvoker getSetterInvoker(Class<?> clazz, String propertyName) {
        return cache.get(clazz).getSetterInvoker(propertyName);
    }

    public static List<PropertyDescriptor> getPropertyDescriptors(Class<?> clazz) {
        return cache.get(clazz).getPropertyDescriptors();
    }

    private final static LoadingCache<Class<?>, BeanInfo> cache = new DoubleCheckCache<Class<?>, BeanInfo>(
            new CacheLoader<Class<?>, BeanInfo>() {
                public BeanInfo load(Class<?> clazz) {
                    try {
                        return new BeanInfo(clazz);
                    } catch (Exception e) {
                        throw new UncheckedException(e.getMessage(), e);
                    }
                }
            });

    private static class BeanInfo {

        final List<PropertyDescriptor> propertyDescriptors;
        final Map<String, GetterInvoker> getterInvokerMap;
        final Map<String, SetterInvoker> setterInvokerMap;

        public BeanInfo(Class<?> clazz) throws Exception {
            Map<String, GetterInvoker> gim = new HashMap<String, GetterInvoker>();
            Map<String, SetterInvoker> sim = new HashMap<String, SetterInvoker>();
            List<PropertyDescriptor> pds = new ArrayList<PropertyDescriptor>();

            java.beans.BeanInfo beanInfo = Introspector.getBeanInfo(clazz);
            for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
                if (!Class.class.equals(pd.getPropertyType())) {
                    pds.add(pd);
                    String name = pd.getName();
                    Method readMethod = pd.getReadMethod();
                    if (readMethod != null) {
                        gim.put(name, FunctionalGetterInvoker.create(readMethod));
                    }
                    Method writeMethod = pd.getWriteMethod();
                    if (writeMethod != null) {
                        sim.put(name, FunctionalSetterInvoker.create(writeMethod));
                    }
                }
            }

            propertyDescriptors = Collections.unmodifiableList(pds);
            getterInvokerMap = Collections.unmodifiableMap(gim);
            setterInvokerMap = Collections.unmodifiableMap(sim);
        }

        public GetterInvoker getGetterInvoker(String propertyName) {
            return getterInvokerMap.get(propertyName);
        }

        public SetterInvoker getSetterInvoker(String propertyName) {
            return setterInvokerMap.get(propertyName);
        }

        public List<PropertyDescriptor> getPropertyDescriptors() {
            return propertyDescriptors;
        }

    }

}
