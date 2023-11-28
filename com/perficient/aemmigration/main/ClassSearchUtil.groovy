package com.perficient.aemmigration.main

import java.util.stream.Collectors

class ClassSearchUtil {

    static Set<Class> findAllClassesUsingClassLoader(String packageName) {
        InputStream stream = ClassLoader.getSystemClassLoader()
                .getResourceAsStream(packageName.replaceAll("[.]", "/"));
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        return reader.lines()
                .filter(line -> line.endsWith(".class"))
                .map(line -> getClass(line, packageName))
                .collect(Collectors.toSet());
    }

    static private Class getClass(String className, String packageName) {
        try {
            return Class.forName(packageName + "."
                    + className.substring(0, className.lastIndexOf('.')));
        } catch (ClassNotFoundException e) {
            // handle the exception
        }
        return null;
    }

    static Class findAllClassesUsingReflectionsLibrary(String packageName) {
        Class template = null
        try{
            template = ObjectGraphBuilder.ReflectionClassNameResolver.forName("${packageName}")
        }catch(ClassNotFoundException cne){
            println "Could not find appropriate class: ${packageName}.  Please double-check and try again"
        }
        return template
    }
}