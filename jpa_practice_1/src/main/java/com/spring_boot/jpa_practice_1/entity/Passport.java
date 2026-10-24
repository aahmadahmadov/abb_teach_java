package com.spring_boot.jpa_practice_1.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "passports", uniqueConstraints = { })
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Passport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "serial_number", nullable = false, length = 20, unique = true)
    private String serialNumber;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "country", nullable = false, length = 50)
    private String country;

    // Hər userin 1 passport olur ona gore user_id unique-dir,
    // eger soft delet column olsaydi amma, bu zaman deleted_at ve user_id ikisi biryerde unique olardi
    // cascade vermirik cunku passport silinende user silinmemelidi
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true, foreignKey = @ForeignKey(name = "fk_passport_user"))
    @Setter(AccessLevel.PACKAGE)
    private User user;

}
