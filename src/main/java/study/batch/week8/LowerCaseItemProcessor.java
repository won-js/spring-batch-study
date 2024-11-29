package study.batch.week8;

import org.springframework.batch.item.ItemProcessor;
import study.batch.common.Customer;

/**
 * 이름을 소문자로 변경하는 ItemProcessor
 */
public class LowerCaseItemProcessor implements ItemProcessor<Customer, Customer> {
    @Override
    public Customer process(Customer customer) {
        customer.nameToLowerCase();
        return customer;
    }
}
