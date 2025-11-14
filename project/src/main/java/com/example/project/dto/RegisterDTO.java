package com.example.project.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterDTO {
    private long id;

    @NotBlank(message = "아이디는 필수 입력 항목입니다.")
    @Size(min = 3, max = 15, message = "아이디는 3자 이상 15자 이하로 입력해주세요.")
    private String username;

    @NotBlank(message = "이름은 필수 입력 항목입니다.")
    @Size(min = 2, max = 5, message = "이름은 2자 이상 5자 이하로 입력해주세요.")
    private String name;
    
    @NotBlank(message = "비밀번호는 필수 입력 항목입니다.")
    @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하로 입력해주세요.")
    private String password;

    @NotBlank(message = "이메일은 필수 입력 항목입니다.")
    @Email(message = "유효한 이메일 주소를 입력해주세요.")
    private String email;

    @NotBlank(message = "주소는 필수 입력 항목입니다.")
    @Size(min = 3, message = "주소는 3자 이상으로 입력해주세요.")
    private String address;

    @NotBlank(message = "전화번호는 필수 입력 항목입니다.")
    @Pattern(regexp = "^\\d{11}$", message = "전화번호 11자리를 입력해주세요.")   // 숫자 11자리만 허용
    private String phoneNumber;
}
