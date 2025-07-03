package com.tenco.blog.board;

import com.tenco.blog.reply.Reply;
import com.tenco.blog.user.User;
import com.tenco.blog.util.MyDateUtil;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;


@NoArgsConstructor// JPA 에서 엔티티는 기본 생성자가 필요하다
@AllArgsConstructor
@Builder
@Data
// @Table : 실제 데이터베이스 테이블 명을 지정할 때 사용
@Table(name = "board_tb") // 데이터베이스에서 테이블 이름을 지정해줌(찾음)
// @Entity : JPA 가 이 클래스를 데이터베이스 테이블과 맵핑하는 객체(엔티티)로 인식
@Entity
public class Board {

    private static final Logger log = LoggerFactory.getLogger(Board.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String title;
    private String content;

    // V2에서 사용했던 방식
    // private String username;
    // V3에서 Board 엔티티는 User 엔티티와 연관관계가 성립이 된다.

    // 다대일
    // 여러개의 게시글에는 한명의 작성자를 가질 수 있다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") // 외래키 컬럼명 명시
    private User user;

    

    @CreationTimestamp // 하이버 네이트가 제공하는 어노테이션
    private Timestamp createdAt;

    // 테이블에 필드 만들지마!
    // (현재 로그인한 유저와 게시글 작성자 여부를 판단 함)
    @Transient
    private boolean isBoardOwner;

    public Board(String title, String content, String username) {
        this.title = title;
        this.content = content;
        // this.username = username;
    }

    // 게시글에 소유자를 직접 확인하는 기능을 만들자
    public boolean isOwner(Long checkUserId) {
        log.info("게시글 소유자 확인 요청 - 작성자 : {}", checkUserId);
        return this.user.getId().equals(checkUserId);
    }

    // 머스테치에서 포현할 시간을 포맷기능을 (행위) 스르로 만들자
    public String getTime(){

        return MyDateUtil.timestampFormat(createdAt);
    }

    /**
     * 게시글과 댓글을 양방향 맵핑으로 설계 해보겠다
     * 하나의 게시글(one)에는 여려 개의 댓글(Many)을 가질 수 있다.
     *
     * Board 와 Reply 테이블 간에 fk 는 Reply 이 가지고 있어야 한다.
     * mappedBy : 외래키 주인이 아닌 엔티티에 설정해야 한다.
     *
     * cascade = CascadeType.REMOVE
     * 영속성 전이
     * - 게시글 삭제 시 관련된 모든 댓글도 자동 삭제 처리 함
     * - 데이터 무결성 보장
     *
     */

    @OrderBy("id DESC") // 정렬 옵션 설정
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "board", cascade = CascadeType.REMOVE)
    List<Reply> replies = new ArrayList<>(); // 일기 전용
    // 여러 개의 row가 출력되므로 리스트로 보여줌. List 선언과 동시에 초기화 <- 객체가 있어야 배열가능.



}
