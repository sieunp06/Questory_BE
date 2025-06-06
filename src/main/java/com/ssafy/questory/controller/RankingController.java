package com.ssafy.questory.controller;

import com.ssafy.questory.domain.Member;
import com.ssafy.questory.dto.response.ranking.MemberRankingResponseDto;
import com.ssafy.questory.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/ranking")
@RequiredArgsConstructor
public class RankingController {
    private final MemberService memberService;

    @GetMapping("")
    @Operation(summary = "전체 유저 랭킹 조회", description = "경험치를 기준으로 전체 유저의 랭킹 목록을 조회합니다.")
    public ResponseEntity<List<MemberRankingResponseDto>> getRankingList() {
        return ResponseEntity.ok(memberService.getTopRankingList());
    }

    @GetMapping("/me")
    @Operation(summary = "내 랭킹 조회", description = "현재 로그인한 유저의 랭킹 정보를 조회합니다.")
    public ResponseEntity<MemberRankingResponseDto> getMyRanking(
            @AuthenticationPrincipal(expression = "member") Member member) {
        return ResponseEntity.ok(memberService.getMyRanking(member.getEmail()));
    }
}
