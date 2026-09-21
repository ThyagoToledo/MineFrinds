package com.thyagotoledo.companions.core;

import com.thyagotoledo.companions.core.ai.InferenceSupervisor;
import com.thyagotoledo.companions.core.ai.InferenceTransport;
import com.thyagotoledo.companions.core.dialogue.DialogueResponse;
import com.thyagotoledo.companions.core.dialogue.IntentType;
import com.thyagotoledo.companions.core.ai.OpenAiResponseParser;
import com.thyagotoledo.companions.core.model.CompanionCommandRequest;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** Contratos leves do caminho de inferencia R2, sem servidor externo. */
public class InferenceSupervisorTests {

    @Test
    void parserAcceptsOpenAiEnvelopeAndKeepsLocale() {
        DialogueResponse fallback = new DialogueResponse("pt_br", "fallback", null);
        String body = "{\"choices\":[{\"message\":{\"content\":\"{\\\"intent\\\":\\\"FOLLOW_OWNER\\\",\\\"speech\\\":\\\"Vamos juntos.\\\"}\"}}]}";

        DialogueResponse parsed = OpenAiResponseParser.parse(body, "pt_br", fallback);

        assertEquals(IntentType.FOLLOW_OWNER, parsed.getIntent().getType());
        assertEquals("Vamos juntos.", parsed.getSpeech());
        assertEquals("pt_br", parsed.getLocale());
    }

    @Test
    void parserFallsBackOnInvalidOrOversizedSpeech() {
        DialogueResponse fallback = new DialogueResponse("en_us", "safe fallback", null);
        DialogueResponse invalid = OpenAiResponseParser.parse("not-json", "en_us", fallback);
        assertSame(fallback, invalid);

        StringBuilder huge = new StringBuilder("{\"intent\":\"CASUAL_CHAT\",\"speech\":\"");
        for (int i = 0; i < 400; i++) huge.append('x');
        huge.append("\"}");
        DialogueResponse bounded = OpenAiResponseParser.parse(huge.toString(), "en_us", fallback);
        assertTrue(bounded.getSpeech().length() <= 256);
    }

    @Test
    void disabledSupervisorDoesNotTouchTransport() {
        InferenceTransport transport = (prompt, systemPrompt, timeoutMs) -> {
            fail("transport should not be called when disabled");
            return CompletableFuture.completedFuture("{}");
        };
        InferenceSupervisor supervisor = new InferenceSupervisor(false, transport, 1000, 8, 1000);
        try {
            assertFalse(supervisor.isAvailable());
            assertTrue(supervisor.completeAsync("hello", "system").isCompletedExceptionally());
            assertEquals(1L, supervisor.getMetrics().getRequests());
            assertEquals(1L, supervisor.getMetrics().getRejected());
        } finally {
            supervisor.shutdown();
        }
    }

    @Test
    void supervisorResetsFailureCounterAfterSuccess() throws Exception {
        InferenceTransport transport = (prompt, systemPrompt, timeoutMs) ->
                CompletableFuture.completedFuture("{\"choices\":[{\"text\":\"ok\"}]}");
        InferenceSupervisor supervisor = new InferenceSupervisor(true, transport, 1000, 2, 5000);
        try {
            assertEquals("{\"choices\":[{\"text\":\"ok\"}]}", supervisor.completeAsync("hello", "system").get());
            assertEquals(0, supervisor.getPendingQueueSize());
            assertTrue(supervisor.isAvailable());
            assertEquals(1L, supervisor.getMetrics().getSuccesses());
        } finally {
            supervisor.shutdown();
        }
    }

    @Test
    void commandRequestRejectsUnknownAndNormalizesKnownInput() {
        CompanionCommandRequest known = CompanionCommandRequest.fromText(
                java.util.UUID.randomUUID(), java.util.UUID.randomUUID(),
                "/companion mode follow", "EN-us", 4L);
        assertTrue(known.isValid());
        assertEquals(IntentType.FOLLOW_OWNER, known.getIntent());
        assertEquals("en_us", known.getLocale());
        assertEquals(4L, known.getExpectedRevision());

        CompanionCommandRequest unknown = CompanionCommandRequest.fromText(
                java.util.UUID.randomUUID(), java.util.UUID.randomUUID(),
                "/kill @e", "pt_br", 4L);
        assertFalse(unknown.isValid());
    }

    @Test
    void shutdownCompletesQueuedRequestsAndCancelsTransport() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CompletableFuture<String> transportFuture = new CompletableFuture<>();
        InferenceTransport transport = (prompt, systemPrompt, timeoutMs) -> {
            started.countDown();
            return transportFuture;
        };
        InferenceSupervisor supervisor = new InferenceSupervisor(true, transport, 1000, 2, 5000);
        CompletableFuture<String> running = supervisor.completeAsync("one", "system");
        assertTrue(started.await(1, TimeUnit.SECONDS));
        CompletableFuture<String> queued = supervisor.completeAsync("two", "system");

        supervisor.shutdown();

        assertTrue(running.isCompletedExceptionally());
        assertTrue(queued.isCompletedExceptionally());
        assertTrue(transportFuture.isCancelled());
        assertFalse(supervisor.isAvailable());
    }

    @Test
    void timeoutCompletesRequestAndDoesNotLeaveItInFlight() throws Exception {
        InferenceTransport transport = (prompt, systemPrompt, timeoutMs) -> new CompletableFuture<>();
        InferenceSupervisor supervisor = new InferenceSupervisor(true, transport, 1000, 1, 5000);
        try {
            CompletableFuture<String> result = supervisor.completeAsync("slow", "system");
            assertThrows(Exception.class, result::get);
            assertEquals(0, supervisor.getPendingQueueSize());
        } finally {
            supervisor.shutdown();
        }
    }

    @Test
    void fullQueueRejectsWithoutCreatingAnUnboundedBacklog() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        InferenceTransport transport = (prompt, systemPrompt, timeoutMs) -> CompletableFuture.supplyAsync(() -> {
            try { release.await(2, TimeUnit.SECONDS); } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
            return "ok";
        });
        InferenceSupervisor supervisor = new InferenceSupervisor(true, transport, 1000, 1, 5000);
        try {
            CompletableFuture<String> first = supervisor.completeAsync("one", "system");
            CompletableFuture<String> second = supervisor.completeAsync("two", "system");
            CompletableFuture<String> third = supervisor.completeAsync("three", "system");
            assertTrue(third.isCompletedExceptionally());
            release.countDown();
            assertEquals("ok", first.get(2, TimeUnit.SECONDS));
            assertEquals("ok", second.get(2, TimeUnit.SECONDS));
        } finally {
            release.countDown();
            supervisor.shutdown();
        }
    }

    @Test
    void deadlineIncludesTimeWaitingInQueue() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CompletableFuture<String> blocked = new CompletableFuture<>();
        InferenceTransport transport = (prompt, systemPrompt, timeoutMs) -> {
            started.countDown();
            return blocked;
        };
        InferenceSupervisor supervisor = new InferenceSupervisor(true, transport, 1000, 2, 5000);
        try {
            CompletableFuture<String> first = supervisor.completeAsync("first", "system");
            assertTrue(started.await(1, TimeUnit.SECONDS));
            CompletableFuture<String> queued = supervisor.completeAsync("queued", "system");
            assertThrows(Exception.class, () -> queued.get(2, TimeUnit.SECONDS));
            assertTrue(first.isCompletedExceptionally());
            assertTrue(blocked.isCancelled());
        } finally {
            supervisor.shutdown();
        }
    }
}
