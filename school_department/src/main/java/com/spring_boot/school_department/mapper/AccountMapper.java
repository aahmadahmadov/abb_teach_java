package com.spring_boot.school_department.mapper;

import com.spring_boot.school_department.dto.response.AccountResponse;
import com.spring_boot.school_department.entity.Account;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    AccountResponse toResponse(Account account);

    List<AccountResponse> toResponseList(List<Account> accounts);
}
