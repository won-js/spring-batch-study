package study.batch.week3;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Getter
@Setter
@ToString
public class Player implements Serializable {
    private long no;
    private String name;
    private int age;

}
