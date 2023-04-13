package com.example.bilingualb8.entity;

import com.example.bilingualb8.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import static jakarta.persistence.CascadeType.*;

@Entity
@Table(name = "user_infos")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "userInfo_id_gen")
    @SequenceGenerator(name = "userInfo_id-gen", sequenceName = "userInfo_id_seq", allocationSize = 1)
    private Long id;

    @Column(unique = true)
    private String email;
    private String password;

    @Enumerated(value = EnumType.STRING)
    private Role role;
    @OneToOne(cascade = ALL)
    private User user;
}
