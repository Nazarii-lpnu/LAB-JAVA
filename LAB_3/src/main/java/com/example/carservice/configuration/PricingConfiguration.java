package com.example.carservice.configuration;

import com.example.carservice.pricing.PricingRuleEngine;
import com.example.carservice.pricing.DiscountPricingRule;
import com.example.carservice.pricing.MinimumPriceRule;
import com.example.carservice.pricing.PricingRule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Configuration
public class PricingConfiguration {
    @Bean
    public DiscountPricingRule discountPricingRule(
            @Value("${pricing.discount-percent:10}") BigDecimal percentage) {
        return new DiscountPricingRule("discount", percentage);
    }

    @Bean
    public MinimumPriceRule minimumPriceRule(
            @Value("${pricing.minimum-price:50.00}") BigDecimal minimumPrice) {
        return new MinimumPriceRule("minimum", minimumPrice);
    }

    @Bean
    public PricingRuleEngine pricingRuleEngine(List<PricingRule> rules) {
        return new PricingRuleEngine(rules);
    }
}
