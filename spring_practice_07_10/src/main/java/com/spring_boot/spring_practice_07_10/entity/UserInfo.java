package com.spring_boot.spring_practice_07_10.entity;

import lombok.*;

@AllArgsConstructor
@RequiredArgsConstructor
@Getter
@Setter
public class UserInfo {

    private Long id;
    private String name;
    private String email;

}