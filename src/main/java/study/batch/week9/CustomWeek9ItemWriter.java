package study.batch.week9;

import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;
import study.batch.common.Customer;

import java.util.Map;

@Slf4j
@Component
public class CustomWeek9ItemWriter implements ItemWriter<Customer> {
    private final CustomerRemoteApi customerRemoteApi;

    public CustomWeek9ItemWriter(CustomerRemoteApi customerRemoteApi) {
        this.customerRemoteApi = customerRemoteApi;
    }

    @Override
    public void write(Chunk<? extends Customer> chunk) {
        for (Customer customer: chunk) {
            Map<String, Integer> response = customerRemoteApi.getBonus(customer);
            Integer code = response.getOrDefault("code", 503);
            Integer bonus = response.getOrDefault("bonus", 0);

            if (code == 200) {
                if (bonus > 15000) {
                    System.out.println("보너스 많이 받은 사람: " + customer.getName());
                }
            } else {
                log.error("api server connection is not available");
            }
        }
    }
}
