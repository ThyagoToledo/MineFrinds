package com.thyagotoledo.companions.core;

import com.thyagotoledo.companions.core.decision.ChoiceQuestion;
import com.thyagotoledo.companions.core.decision.DecisionQuestion;
import com.thyagotoledo.companions.core.decision.NoulQuestion;
import com.thyagotoledo.companions.core.decision.ScoreQuestion;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class DecisionQuestionTests {
    @Test
    void typedQuestionsAreImmutableAndBounded() {
        ChoiceQuestion choice = new ChoiceQuestion("intent", "choose", Arrays.asList("A", "B"));
        assertEquals(DecisionQuestion.Type.CHOICE, choice.getType());
        assertThrows(UnsupportedOperationException.class, () -> choice.getOptions().add("C"));

        ScoreQuestion score = new ScoreQuestion("danger", "rank", Arrays.asList("low", "high"));
        assertEquals(DecisionQuestion.Type.SCORE, score.getType());
        NoulQuestion noul = new NoulQuestion("threat", "check", "There is an immediate threat");
        assertEquals(DecisionQuestion.Type.NOUL, noul.getType());
    }
}
