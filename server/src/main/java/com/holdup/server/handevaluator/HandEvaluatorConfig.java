package com.holdup.server.handevaluator;

import com.holdup.server.card.DefaultHandEvaluator;
import com.holdup.server.card.HandEvaluator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * HandEvaluator 빈 등록. WinnerResolver에서 주입받아 사용.
 */
@Configuration
public class HandEvaluatorConfig {

    @Bean
    public HandEvaluator handEvaluator() {
        return new DefaultHandEvaluator();
    }

    @Bean
    public WinnerResolver winnerResolver(HandEvaluator handEvaluator) {
        return new WinnerResolver(handEvaluator);
    }
}
