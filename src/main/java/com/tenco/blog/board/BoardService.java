package com.tenco.blog.board;


import com.tenco.blog._core.errors.exception.Exception403;
import com.tenco.blog._core.errors.exception.Exception404;
import com.tenco.blog.reply.Reply;
import com.tenco.blog.user.User;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Board 관련 비즈니스 로직을 처리하는 Service 계층
 */

@RequiredArgsConstructor
@Service // IoC 대상
@org.springframework.transaction.annotation.Transactional(readOnly = true)
// 모든 메서드를 읽기 전용 트랜잭션으로 실행(findAll, findByid 최적화)
// 성능 최적화 (변경 감지 비활성화), 데이터 수정 방지 ()
// 데이터베이스 락(lock) 최소화 하여 동시성 성능 개선
public class BoardService {

    private static final Logger log = LoggerFactory.getLogger(BoardService.class);
    private final BoardJpaRepository boardJpaRepository;

    /**
     * 게시글 저장
     */
    @Transactional // 메서드 레벨에서의 트랜잭션 선언
    public Board save(BoardRequest.SaveDTO saveDTO, User sessionUser) {
        // 1. 로그 기록 - 게시글 저장 요청 정보
        // 2. DTO를 Entity로 변환(작성자 정보 포함)
        // 3. 데이터베이스에 게시글 저장
        // 4. 저장 완료 로그 기록
        // 5. 저장된 Board 를 Controller 로 반환
        log.info("게시글 저장 서비스 처리 시작 - 제목 {} , 작성자 {}",
                saveDTO.getTitle(), sessionUser.getUsername());

        Board board = saveDTO.toEntity(sessionUser);
        boardJpaRepository.save(board);

        log.info("게시글 저장 완료 - ID {} , 제목 {}",
                board.getId(), board.getTitle());

        return board;
    }

    /**
     * 게시글 목록 조회
     */
    public List<Board> findAll() {
        // 1. 로그기록
        // 2. 데이터베이스 게시글 조회
        // 3. 로그 기록
        // 4. 조회된 게시글 목록 반환
        log.info("게시글 조회 서비스 처리 시작 - 제목 {} , 작성자 {}");
        List<Board> boardList = boardJpaRepository.findAllJoinUser();
        log.info("게시글 목록 조회 완료 - 총 {} 개 ", boardList.size());
        return boardList;
    }

    //
    public Board findByIdWithReplies(Long id, User sessionUser) {
        // 1. 게시글 조회
        Board board = boardJpaRepository.findByIdJoinUser(id).orElseThrow(
                () ->  new Exception404("게시글을 찾을 수 없습니다"));
        // 2. 게시글 작성자 정보 포함해 주어야 함
        // 3. 게시글 소유권 설정(수정/ 삭제버튼 표시용)
        if(sessionUser  != null) {
            boolean isBoardOwner = board.isOwner(sessionUser.getId());
            // 로직 : 메서드를 통해서 게시글 소유자를 확인하고
            // 그 결과 값을 Board 객체에 담아 둔다
            board.setBoardOwner(isBoardOwner);
        }

        // 댓글 정보 <-- 양방향 맵핑설정 Board <--- 댓글 가져옴
        List<Reply> replies = board.getReplies();

        // 댓글 소유권 설정 (삭제 버튼 표시용)
        if(sessionUser != null) {
            //for(int i = 0; i < replies.size(); i++)
            //for 문대신 사용 가능
            replies.forEach(reply -> {
                boolean isReplyOwner = reply.isOwner(sessionUser.getId());
                reply.setReplyOwner(isReplyOwner);

            });
        }
        return board;
    }


    /**
     * 게시글 상세 조회
     */
    public Board findById(Long id) {
        // 1. 로그 기록
        // 2. 데이터 베이스에서 해당하는 board id 로 조회 - WHERE
        // 3. 게시글이 없다면 404 에러 처리
        // 4. 조회 성공시 로그 기록
        // 5. 조회된 게시글 반환
        log.info("게시글 상세 조회 서비스 시작 - ID {}", id);
        Board board = boardJpaRepository.findByIdJoinUser(id).orElseThrow(() -> {
            log.warn("게시글 조회 실패 - ID {}", id);
            return new Exception404("게시글을 찾을 수 없습니다");
        });

        log.info("게시글 상세 조회 완료 - 제목 {}", board.getTitle());
        // log.info("게시글 목록 조회 완료 - 총 {} 개 ", boardList.size());
        return board;
    }

    /**
     *  게시글 수정(권한 체크 포함)
     */
    @Transactional
    public Board updateById(Long id, BoardRequest.UpdateDTO updateDTO,
                            User sessionUser) {
        // 1. 로그 기록
        // 2. 수정하려는 게시글 조회
        // 3. 권한 체크
        // 4. 권한이 없다면 403 예외 발생
        // 5. Board 엔티티에 상태값 변경 (더티 체킹)
        // 6. 로그 기록 - 수정 완료
        // 7. 수정된 게시글 반환

        log.info("게시글 수정 서비스 시작 - 게시글 ID {}", id);
        Board board = boardJpaRepository.findById(id).orElseThrow(() ->{
            log.warn("게시글 조회 실패 - ID {}", id);
            return new Exception404("해당 게시글이 존재하지 않습니다.");
        });
        
        if(!board.isOwner(sessionUser.getId())) {
            throw new Exception403("본인이 작성한 게시글만 수정 가능");
        }
        board.setTitle(updateDTO.getTitle()); // 필드값 상태 변경
        board.setContent(updateDTO.getContent()); 
        // TODO - board 엔티티에 update() 만들어 주기
        // 더티 체킹
        log.info("게시글 수정 완료 - 게시글 ID {}, 게시글 제목 {}", id, board.getTitle());
        return board;
        
    }

    /**
     * 게시글 삭제
     */
    @Transactional
    public void deleteById(Long id, User sessionUser){
        // 1. 로그 기록
        // 2. 삭제 하려는 게시글 조회
        // 3. 권한 체크
        // 4. 권한이 없다면 403 예외 발생
        // 5. 데이터베이스 삭제 처리
        // 6. 삭제 완료 로그 기록
        log.info("게시글 삭제 서비스 시작 - 게시글 ID {}", id);
        Board board = boardJpaRepository.findById(id).orElseThrow(() -> {
            throw new Exception404("삭제할 게시글이 없습니다");
        });
        if(! board.isOwner(sessionUser.getId())) {
            throw new Exception403("본인이 작성한 게시글만 삭제할 수 있습니다");
        }
        boardJpaRepository.deleteById(id);
        log.info("게시글 삭제 완료 - 게시글 ID {}", id);
    }

    // 게시글 권한 확인 서비스

    public void checkBoardOwner(Long boardId, Long userId) {
        Board board = findById(boardId);
        if(!board.isOwner(userId)) {
            throw new Exception403("본인 게시글만 수정할 수 있습니다.");
        }
    }

}


