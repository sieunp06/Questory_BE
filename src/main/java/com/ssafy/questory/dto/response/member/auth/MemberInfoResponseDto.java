package com.ssafy.questory.dto.response.member.auth;

import com.ssafy.questory.domain.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberInfoResponseDto {
    private String email;
    private String nickname;
    private long exp;
    private String title;
    private LocalDate createdAt;
    private boolean isAdmin;
    private boolean mode;

    public static MemberInfoResponseDto from(Member member) {
        return MemberInfoResponseDto.builder()
                .email(member.getEmail())
                .nickname(member.getNickname())
                .exp(member.getExp())
                .title(member.getTitle())
                .createdAt(member.getCreatedAt())
                .isAdmin(member.isAdmin())
                .mode(member.isMode())
                .build();
    }
}
