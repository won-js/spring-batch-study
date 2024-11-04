package study.batch.common;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Customer {
    private  int id;
    private String name;
    private int age;
    private Grade grade;

    public enum Grade {
        A, B, C, D
    }

    public String getGrade() {
        return grade.toString();
    }

    public void assignGroup() {
        if (50 <= age) {
            grade = Grade.A;
        } else if (40 <= age) {
            grade = Grade.B;
        } else if (30 <= age) {
            grade = Grade.C;
        } else {
            grade = Grade.D;
        }
    }
}
