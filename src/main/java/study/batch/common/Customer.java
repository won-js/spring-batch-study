package study.batch.common;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@ToString
@Entity(name = "CUSTOMER")
@Table(name="CUSTOMER")
public class Customer {
    @Id
    @Column(name="ID")
    private  int id;
    @Column(name="NAME")
    private String name;
    @Column(name = "AGE")
    private int age;
    @Enumerated(EnumType.STRING)
    @Column(name="GRADE")
    private Grade grade;

    public enum Grade {
        S, A, B, C, D
    }

    public String getGrade() {
        return grade.toString();
    }

    public void addOneAge() {
        age++;
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

    public void nameToLowerCase() {
        name = name.toLowerCase();
    }

    public void after20Years () {
        age += 20;
    }
}
