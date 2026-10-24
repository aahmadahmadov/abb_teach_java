package com.spring_boot.jpa_practice_1.entity;

import com.spring_boot.jpa_practice_1.entity.converter.GenderConverter;
import com.spring_boot.jpa_practice_1.enums.GenderTypeEnum;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_email", columnNames = {"email"})
})
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "surname", nullable = false, length = 50)
    private String surname;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Convert(converter = GenderConverter.class)
    @Column(name = "gender_id", nullable = false)
    private GenderTypeEnum gender;

    // user silinende onun passportda silinsin deye cascade all
    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Passport passport;

    public void setPassport(Passport passport) {
        this.passport = passport;

        if (passport != null) {
            passport.setUser(this);
        }
    }

}
