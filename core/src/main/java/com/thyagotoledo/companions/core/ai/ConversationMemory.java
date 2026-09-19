package com.thyagotoledo.companions.core.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConversationMemory {
    public static class Entry {
        private final String role;
        private final String text;

        public Entry(String role, String text) {
            this.role = role != null ? role : "user";
            this.text = text != null ? text : "";
        }

        public String getRole() {
            return role;
        }

        public String getText() {
            return text;
        }
    }

    private final int maxEntries;
    private final List<Entry> entries = new ArrayList<>();

    public ConversationMemory(int maxEntries) {
        this.maxEntries = Math.max(2, maxEntries);
    }

    public ConversationMemory() {
        this(6);
    }

    public synchronized void addEntry(String role, String text) {
        if (entries.size() >= maxEntries) {
            entries.remove(0);
        }
        entries.add(new Entry(role, text));
    }

    public synchronized List<Entry> getEntries() {
        return Collections.unmodifiableList(new ArrayList<>(entries));
    }

    public synchronized void clear() {
        entries.clear();
    }

    public synchronized int size() {
        return entries.size();
    }
}
