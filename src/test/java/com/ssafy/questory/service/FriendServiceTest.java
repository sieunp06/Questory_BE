package com.ssafy.questory.service;

import com.ssafy.questory.domain.FollowStatus;
import com.ssafy.questory.domain.Friend;
import com.ssafy.questory.domain.Member;
import com.ssafy.questory.dto.request.friend.FriendStatusRequestDto;
import com.ssafy.questory.dto.request.member.MemberEmailRequestDto;
import com.ssafy.questory.dto.request.member.MemberSearchRequestDto;
import com.ssafy.questory.dto.response.friend.FollowResponseDto;
import com.ssafy.questory.dto.response.member.MemberSearchResponseDto;
import com.ssafy.questory.dto.response.member.auth.MemberInfoResponseDto;
import com.ssafy.questory.repository.FriendRepository;
import com.ssafy.questory.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

class FriendServiceTest {

    private FriendRepository friendRepository;
    private MemberRepository memberRepository;
    private FriendService friendService;

    @BeforeEach
    void setUp() {
        friendRepository = mock(FriendRepository.class);
        memberRepository = mock(MemberRepository.class);
        friendService = new FriendService(friendRepository, memberRepository);
    }

    @Test
    @DisplayName("친구 목록 조회 테스트")
    void testGetFriendsInfo() {
        Member member = Member.builder().email("me@domain.com").build();

        Member friend1 = Member.builder()
                .email("a@domain.com")
                .password("pw1")
                .nickname("A")
                .build();

        List<Member> mockFriends = List.of(friend1);

        when(friendRepository.findFriendMembersByEmail("me@domain.com"))
                .thenReturn(mockFriends);

        List<MemberInfoResponseDto> friendsInfo = friendService.getFriendsInfo(member);

        assertThat(friendsInfo).hasSize(1);
        assertThat(friendsInfo.get(0).getEmail()).isEqualTo("a@domain.com");
        assertThat(friendsInfo.get(0).getNickname()).isEqualTo("A");
        assertThat(friendsInfo.get(0).getExp()).isEqualTo(0L);  // 기본값
        assertThat(friendsInfo.get(0).getTitle()).isEqualTo(""); // 기본값
        assertThat(friendsInfo.get(0).isAdmin()).isFalse();     // 기본값
    }



    @Test
    @DisplayName("친구 요청 테스트")
    void testRequestFriend() {
        Member member = Member.builder().email("me@domain.com").build();
        MemberEmailRequestDto dto = MemberEmailRequestDto.builder().email("other@domain.com").build();

        friendService.request(member, dto);

        ArgumentCaptor<Friend> captor = ArgumentCaptor.forClass(Friend.class);
        verify(friendRepository, times(1)).request(captor.capture());
        Friend sentFriend = captor.getValue();
        assertThat(sentFriend.getRequesterEmail()).isEqualTo("me@domain.com");
        assertThat(sentFriend.getTargetEmail()).isEqualTo("other@domain.com");
        assertThat(sentFriend.getStatus()).isEqualTo(FollowStatus.APPLIED);
    }

    @Test
    @DisplayName("친구 요청 수락 테스트")
    void testAcceptFriendRequest() {
        Member member = Member.builder().email("target@domain.com").build();
        FriendStatusRequestDto dto = FriendStatusRequestDto.builder()
                .requesterEmail("requester@domain.com")
                .status(FollowStatus.ACCEPTED)
                .build();

        friendService.update(member, dto);

        verify(friendRepository).deleteFollowRequest("requester@domain.com", "target@domain.com");
        verify(friendRepository).insertFriendRelation("requester@domain.com", "target@domain.com");
    }

    @Test
    @DisplayName("친구 요청 거절 테스트")
    void testRejectFriendRequest() {
        Member member = Member.builder().email("target@domain.com").build();
        FriendStatusRequestDto dto = FriendStatusRequestDto.builder()
                .requesterEmail("requester@domain.com")
                .status(FollowStatus.DENIED)
                .build();

        friendService.update(member, dto);

        verify(friendRepository, never()).deleteFollowRequest(anyString(), anyString());
        verify(friendRepository, never()).insertFriendRelation(anyString(), anyString());
        verify(friendRepository, atLeastOnce()).update(any(Friend.class));
    }

    @Test
    @DisplayName("보낸 친구 요청 목록 조회 테스트")
    void testGetSentFriendRequests() {
        Member member = Member.builder().email("user1@example.com").build();

        Friend friend1 = Friend.builder()
                .requesterEmail("user1@example.com")
                .targetEmail("target1@example.com")
                .targetNickname("Target One")
                .status(FollowStatus.APPLIED)
                .build();

        Friend friend2 = Friend.builder()
                .requesterEmail("user1@example.com")
                .targetEmail("target2@example.com")
                .targetNickname("Target Two")
                .status(FollowStatus.APPLIED)
                .build();

        when(friendRepository.findFollowRequestsByRequesterEmailWithPaging("user1@example.com", 0, 10))
                .thenReturn(List.of(friend1, friend2));
        when(friendRepository.countFollowRequestsByRequesterEmail("user1@example.com"))
                .thenReturn(2);

        Page<FollowResponseDto> result = friendService.getFollowRequests(member, 0, 10);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getTargetNickname()).isEqualTo("Target One");
        assertThat(result.getContent().get(1).getTargetNickname()).isEqualTo("Target Two");

        verify(friendRepository).findFollowRequestsByRequesterEmailWithPaging("user1@example.com", 0, 10);
        verify(friendRepository).countFollowRequestsByRequesterEmail("user1@example.com");
    }

    @Test
    @DisplayName("이메일 키워드로 유저 검색 - 페이징 포함")
    void searchMembersByEmail_withPaging() {
        // given
        String keyword = "kim";
        int page = 0;
        int size = 2;
        int offset = page * size;

        Member member1 = Member.builder()
                .email("kim1@example.com")
                .nickname("김하나")
                .profileUrl("https://cdn.questory.com/kim1.jpg")
                .build();

        Member member2 = Member.builder()
                .email("kim2@example.com")
                .nickname("김둘")
                .profileUrl("https://cdn.questory.com/kim2.jpg")
                .build();

        List<Member> memberList = List.of(member1, member2);

        Member requester = Member.builder()
                .email("test@example.com")
                .nickname("nick")
                .password("pw")
                .build();

        // ✅ requester.getEmail() 인자 추가
        given(memberRepository.searchByEmailWithPaging(keyword, requester.getEmail(), offset, size))
                .willReturn(memberList);

        given(memberRepository.countByEmail(keyword))
                .willReturn(10);

        MemberSearchRequestDto requestDto = MemberSearchRequestDto.builder()
                .email(keyword)
                .page(page)
                .size(size)
                .build();

        // when
        Page<MemberSearchResponseDto> result = friendService.search(requester, requestDto);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(10);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("kim1@example.com");
        assertThat(result.getContent().get(1).getNickname()).isEqualTo("김둘");
    }

}
