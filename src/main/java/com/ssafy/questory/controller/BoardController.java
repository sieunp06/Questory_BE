package com.ssafy.questory.controller;

import com.ssafy.questory.domain.Member;
import com.ssafy.questory.domain.PostCategory;
import com.ssafy.questory.dto.request.posts.PostsCreateRequestDto;
import com.ssafy.questory.dto.request.posts.PostsDeleteRequestDto;
import com.ssafy.questory.dto.request.posts.PostsUpdateRequestDto;
import com.ssafy.questory.dto.response.post.PostDetailResponseDto;
import com.ssafy.questory.dto.response.post.PostsResponseDto;
import com.ssafy.questory.service.CommunityService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class BoardController {
    private final CommunityService communityService;

    @GetMapping("")
    @Operation(summary = "게시글 페이징 + 필터 조회", description = "게시글을 페이지별로 조회하며, 제목 또는 카테고리로 필터링할 수 있습니다.")
    public ResponseEntity<List<PostsResponseDto>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        List<PostsResponseDto> result = communityService.findAll(page, size, keyword);
        return ResponseEntity.ok(result);
    }

    @PostMapping("")
    @Operation(summary = "게시판 글 작성", description = "게시판에 글을 작성합니다.")
    public ResponseEntity<Map<String, String>> create(
            @AuthenticationPrincipal(expression = "member") Member member,
            @RequestBody PostsCreateRequestDto postsCreateRequestDto) {
        System.out.println("게시글 작성 요청 도착");
        System.out.println(member.getEmail());
        communityService.create(member, postsCreateRequestDto);
        return ResponseEntity.ok().body(Map.of(
                "message", "글이 등록되었습니다."
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "게시글 상세 조회", description = "게시글 ID로 상세 정보를 조회합니다.")
    public ResponseEntity<PostDetailResponseDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(communityService.findById(id));
    }

    @PatchMapping("")
    @Operation(summary = "게시글 수정", description = "게시글을 수정합니다.")
    public ResponseEntity<Map<String, String>> update(
            @AuthenticationPrincipal(expression = "member") Member member,
            @RequestBody PostsUpdateRequestDto postsUpdateRequestDto) {
        communityService.update(member, postsUpdateRequestDto);
        return ResponseEntity.ok().body(Map.of("message", "게시글이 수정되었습니다."));
    }

    @DeleteMapping("")
    @Operation(summary = "게시글 삭제", description = "게시글을 삭제합니다.")
    public ResponseEntity<Map<String, String>> delete(
            @AuthenticationPrincipal(expression = "member") Member member,
            @RequestBody PostsDeleteRequestDto postsDeleteRequestDto) {
        communityService.delete(member, postsDeleteRequestDto);
        return ResponseEntity.ok().body(Map.of("message", "게시글이 삭제되었습니다."));
    }

    @GetMapping("/me")
    @Operation(summary = "내가 작성한 게시글 조회", description = "로그인한 사용자가 작성한 게시글을 조회합니다.")
    public ResponseEntity<List<PostsResponseDto>> findMyPosts(
            @AuthenticationPrincipal(expression = "member") Member member,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        List<PostsResponseDto> result = communityService.findMyPosts(member, page, size, keyword);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/notice")
    @Operation(summary = "공지글 조회", description = "공지글을 조회합니다.")
    public ResponseEntity<List<PostsResponseDto>> getByCategory() {
        List<PostsResponseDto> posts = communityService.getNotice(PostCategory.NOTICE.name());
        return ResponseEntity.ok(posts);
    }
}
