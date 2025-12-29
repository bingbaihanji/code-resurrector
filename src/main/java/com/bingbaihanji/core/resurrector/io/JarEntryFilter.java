package com.bingbaihanji.core.resurrector.io;

import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JarEntryFilter {

    private JarFile file;

    public JarEntryFilter() {
    }

    public JarEntryFilter(JarFile jfile) {
        this.file = jfile;
    }

    public List<String> getAllEntriesFromJar() {
        List<String> mass = new ArrayList<>();
        Enumeration<JarEntry> entries = file.entries();
        while (entries.hasMoreElements()) {
            JarEntry e = entries.nextElement();
            if (!e.isDirectory()) {
                mass.add(e.getName());
            }
        }
        return mass;
    }

    public List<String> getEntriesWithoutInnerClasses() {
        List<String> mass = new ArrayList<>();
        Enumeration<JarEntry> entries = file.entries();
        Set<String> possibleInnerClasses = new HashSet<String>();
        Set<String> baseClasses = new HashSet<String>();

        while (entries.hasMoreElements()) {
            JarEntry e = entries.nextElement();
            if (!e.isDirectory()) {
                String entryName = e.getName();

                if (!entryName.trim().isEmpty()) {
                    entryName = entryName.trim();

                    if (!entryName.endsWith(".class")) {
                        mass.add(entryName);

                        // com/acme/Model$16.class
                    } else if (entryName.matches(".*[^(/|\\\\)]+\\$[^(/|\\\\)]+$")) {
                        possibleInnerClasses.add(entryName);

                    } else {
                        baseClasses.add(entryName);
                        mass.add(entryName);
                    }
                }
            }
        }

        // 保留Badly$Named但不包括内部类
        for (String inner : possibleInnerClasses) {

            // com/acme/Connection$Conn$1.class -> com/acme/Connection
            String innerWithoutTail = inner.replaceAll("\\$[^(/|\\\\)]+\\.class$", "");
            if (!baseClasses.contains(innerWithoutTail + ".class")) {
                mass.add(inner);
            }
        }
        return mass;
    }

    public JarFile getJfile() {
        return file;
    }

    public void setJfile(JarFile jfile) {
        this.file = jfile;
    }
}
