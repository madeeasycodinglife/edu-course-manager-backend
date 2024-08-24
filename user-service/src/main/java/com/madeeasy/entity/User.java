package com.madeeasy.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "`user`")
public class User implements Serializable {

    @Id
    private String id;
    private String fullName;
    @Column(unique = true)
    private String email;
    private String password;
    private String phone;
    @ElementCollection(targetClass = Role.class)
    @Enumerated(EnumType.STRING)
    private List<Role> roles = new ArrayList<>();
}
