package com.thyagotoledo.companions.core.dialogue;

import com.thyagotoledo.companions.core.ai.ConversationMemory;
import com.thyagotoledo.companions.core.ai.InferenceClient;
import com.thyagotoledo.companions.core.ai.OpenAiResponseParser;
import com.thyagotoledo.companions.core.locale.LocaleService;
import com.thyagotoledo.companions.core.model.CompanionProfile;
import com.thyagotoledo.companions.core.model.InventorySnapshot;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class HybridDialogueProvider {
    private final DeterministicDialogueProvider deterministicProvider;
    private final InferenceClient inferenceClient;
    private final ConversationMemory memory;
    private final LocaleService localeService;

    public HybridDialogueProvider(DeterministicDialogueProvider deterministicProvider,
                                  InferenceClient inferenceClient,
                                  ConversationMemory memory,
                                  LocaleService localeService) {
        this.localeService = localeService != null ? localeService : new LocaleService();
        this.deterministicProvider = deterministicProvider != null ? deterministicProvider : new DeterministicDialogueProvider(this.localeService);
        this.inferenceClient = inferenceClient;
        this.memory = memory != null ? memory : new ConversationMemory(6);
    }

    public CompletableFuture<DialogueResponse> processAsync(String input, String preferredLocale,
                                                           CompanionProfile profile, InventorySnapshot inventory) {
        String locale = localeService.normalizeLocale(preferredLocale);

        // 1. Avaliacao deterministica instantanea de comandos criticos
        DialogueResponse deterministicResp = deterministicProvider.process(input, locale, profile, inventory);
        IntentType detIntent = deterministicResp.getIntent().getType();

        // Se for comando canonico reconhecido, atende deterministamente em 0ms
        if (detIntent != IntentType.UNKNOWN_OR_BLOCKED && detIntent != IntentType.CASUAL_CHAT) {
            memory.addEntry("user", input);
            memory.addEntry("companion", deterministicResp.getSpeech());
            return CompletableFuture.completedFuture(deterministicResp);
        }

        // 2. Se a inferencia generativa nao estiver configurada ou disponivel, retorna deterministico
        if (inferenceClient == null || !inferenceClient.isAvailable() || inferenceClient.getPendingQueueSize() >= 8) {
            memory.addEntry("user", input);
            memory.addEntry("companion", deterministicResp.getSpeech());
            return CompletableFuture.completedFuture(deterministicResp);
        }

        // 3. Montar prompt do sistema com personalidade e memoria curta
        String systemPrompt = buildSystemPrompt(locale, profile);
        memory.addEntry("user", input);

        return inferenceClient.completeAsync(input, systemPrompt)
                .thenApply(rawJson -> {
                    DialogueResponse parsed = parseModelOutput(rawJson, locale, deterministicResp);
                    memory.addEntry("companion", parsed.getSpeech());
                    return parsed;
                })
                .exceptionally(ex -> {
                    // Fallback gracioso em caso de erro, timeout ou desconexao
                    return deterministicResp;
                });
    }

    public DialogueResponse processSync(String input, String preferredLocale,
                                        CompanionProfile profile, InventorySnapshot inventory,
                                        long timeoutMs) {
        try {
            return processAsync(input, preferredLocale, profile, inventory).get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            return deterministicProvider.process(input, preferredLocale, profile, inventory);
        }
    }

    public DeterministicDialogueProvider getDeterministicProvider() {
        return deterministicProvider;
    }

    public InferenceClient getInferenceClient() {
        return inferenceClient;
    }

    public ConversationMemory getMemory() {
        return memory;
    }

    private String buildSystemPrompt(String locale, CompanionProfile profile) {
        String personality = profile != null && profile.getPersonality() != null ? profile.getPersonality().name() : "BALANCED";
        return "Voce e um companheiro util em Minecraft. Personalidade: " + personality +
                ". Responda no idioma " + locale + ". Responda exclusivamente em formato JSON estruturado com os campos 'intent' e 'speech'. Sem markdown, sem emojis.";
    }

    private DialogueResponse parseModelOutput(String rawJson, String locale, DialogueResponse fallback) {
        if (rawJson == null || rawJson.trim().isEmpty()) {
            return fallback;
        }

        return OpenAiResponseParser.parse(rawJson, locale, fallback);
    }
}
