package com.thyagotoledo.companions.core;

import com.thyagotoledo.companions.core.ai.*;
import com.thyagotoledo.companions.core.dialogue.*;
import org.junit.jupiter.api.Test;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class DialogueRoutingTests {
    @Test void conversationIsNotAnEmbeddedStopCommand() {
        DeterministicDialogueProvider provider = new DeterministicDialogueProvider(null);
        assertEquals(IntentType.UNKNOWN_OR_BLOCKED, provider.process("qual o primeiro passo para sobreviver?", "pt_br", null, null).getIntent().getType());
        assertEquals(IntentType.UNKNOWN_OR_BLOCKED, provider.process("Can we visit a village?", "en_us", null, null).getIntent().getType());
        assertEquals(IntentType.STAY, provider.process("para", "pt_br", null, null).getIntent().getType());
    }

    @Test void localClosedIntentUnderstandsNaturalBilingualOrder() throws Exception {
        HybridDialogueProvider provider = new HybridDialogueProvider(null, null,
                new ConversationMemory(6), null);
        DialogueResponse response = provider.processAsync("pegue madeira da arvore", "pt_br", null, null).get();
        assertEquals(IntentType.CHOP_WOOD, response.getIntent().getType());
        assertTrue(response.getSpeech().contains("madeira"));
    }

    @Test void localClosedIntentAbstainsBeforeCasualFallback() throws Exception {
        HybridDialogueProvider provider = new HybridDialogueProvider(null, null,
                new ConversationMemory(6), null);
        DialogueResponse response = provider.processAsync("o que fazer para sobreviver?", "pt_br", null, null).get();
        assertEquals(IntentType.UNKNOWN_OR_BLOCKED, response.getIntent().getType());
    }
    @Test void modelReceivesMemoryAndRequestedLanguage() throws Exception {
        AtomicReference<String> prompt = new AtomicReference<>();
        InferenceTransport transport = (input, system, timeout) -> {
            prompt.set(system);
            return CompletableFuture.completedFuture("{\"intent\":\"CASUAL_CHAT\",\"speech\":\"Vamos preparar ferramentas.\"}");
        };
        InferenceSupervisor supervisor = new InferenceSupervisor(true, transport, 1000, 2, 1000);
        try {
            ConversationMemory memory = new ConversationMemory(6);
            memory.addEntry("user", "Meu objetivo e construir uma casa.");
            HybridDialogueProvider provider = new HybridDialogueProvider(null, supervisor, memory, null);
            assertEquals(IntentType.CASUAL_CHAT, provider.processAsync("o que fazer para sobreviver?", "pt_br", null, null).get().getIntent().getType());
            assertTrue(prompt.get().contains("Brazilian Portuguese"));
            assertTrue(prompt.get().contains("construir uma casa"));
            provider.processAsync("What should we do next?", "en_us", null, null).get();
            assertTrue(prompt.get().contains("Reply in English"));
        } finally { supervisor.shutdown(); }
    }
}
