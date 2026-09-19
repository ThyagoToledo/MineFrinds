package com.thyagotoledo.companions.core.quest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Quest {
    private final String id;
    private final String title;
    private final String description;
    private final String chapter;
    private final List<String> dependencies;
    private final List<QuestTask> tasks;
    private final List<QuestReward> rewards;
    private final boolean hidden;
    private boolean completed;
    private boolean locked;

    public Quest(String id, String title, String description, String chapter,
                 List<String> dependencies, List<QuestTask> tasks, List<QuestReward> rewards,
                 boolean hidden) {
        this.id = id != null ? id : "quest_default";
        this.title = title != null ? title : id;
        this.description = description != null ? description : "";
        this.chapter = chapter != null ? chapter : "main";
        this.dependencies = dependencies != null ? new ArrayList<>(dependencies) : new ArrayList<>();
        this.tasks = tasks != null ? new ArrayList<>(tasks) : new ArrayList<>();
        this.rewards = rewards != null ? new ArrayList<>(rewards) : new ArrayList<>();
        this.hidden = hidden;
        this.completed = false;
        this.locked = false;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public boolean isHidden() {
        return hidden;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getChapter() {
        return chapter;
    }

    public List<String> getDependencies() {
        return Collections.unmodifiableList(dependencies);
    }

    public List<QuestTask> getTasks() {
        return Collections.unmodifiableList(tasks);
    }

    public List<QuestReward> getRewards() {
        return Collections.unmodifiableList(rewards);
    }
}
