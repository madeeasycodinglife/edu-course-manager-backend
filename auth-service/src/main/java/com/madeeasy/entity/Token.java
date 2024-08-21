package com.madeeasy.entity;



import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Token implements Serializable {
    @Id
    private String id;
    @Column(length = 1000)
    private String token;
    private boolean isRevoked;
    private boolean isExpired;
    @Enumerated(EnumType.STRING)
    private TokenType tokenType;
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}

