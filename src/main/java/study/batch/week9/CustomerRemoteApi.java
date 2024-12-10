package study.batch.week9;

import org.springframework.stereotype.Component;
import study.batch.common.Customer;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class CustomerRemoteApi {
    public Map<String, Integer> getBonus(Customer customer) {
        Integer bonus;
        if (Customer.Grade.A == customer.getGrade()) {
            bonus = 20000;
        } else {
            bonus = 10000;
        }

        return Map.of("code", 200, "bonus", bonus);
    }
}
