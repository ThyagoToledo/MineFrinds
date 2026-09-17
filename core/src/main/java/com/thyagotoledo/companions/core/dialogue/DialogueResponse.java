package com.thyagotoledo.companions.core.dialogue;

import java.util.Objects;

public final class DialogueResponse {
    private final String locale;
    private final String speech;
    private final Intent intent;

    public DialogueResponse(String locale, String speech, Intent intent) {
        this.locale = locale != null ? locale : "en_us";
        this.speech = speech != null ? speech : "";
        this.intent = intent != null ? intent : new Intent(IntentType.UNKNOWN_OR_BLOCKED);
    }

    public String getLocale() {
        return locale;
    }

    public String getSpeech() {
        return speech;
    }

    public Intent getIntent() {
        return intent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DialogueResponse that = (DialogueResponse) o;
        return Objects.equals(locale, that.locale) && Objects.equals(speech, that.speech) && Objects.equals(intent, that.intent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(locale, speech, intent);
    }
}
