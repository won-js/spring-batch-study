package study.batch.week8;

import org.springframework.batch.item.ItemProcessor;
import study.batch.common.Customer;

/**
 * 나이에 20년을 더하는 ItemProcessor
 */
public class After20YearsItemProcessor implements ItemProcessor<Customer, Customer> {
    @Override
    public Customer process(Customer customer) {
        customer.after20Years();
        return customer;
    }
}
