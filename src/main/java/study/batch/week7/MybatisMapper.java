package study.batch.week7;

import org.apache.ibatis.annotations.Mapper;
import study.batch.common.Customer;

import java.util.List;

@Mapper
public interface MybatisMapper {
    List<Customer> selectCustomers();
    int updateCustomer();
}
