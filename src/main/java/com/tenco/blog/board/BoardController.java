package com.tenco.blog.board;

import com.tenco.blog._core.errors.exception.Exception403;
import com.tenco.blog._core.errors.exception.Exception404;
import com.tenco.blog.user.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class BoardController {

    private static final Logger log = LoggerFactory.getLogger(BoardController.class);

    // DI 처리
    private final BoardService boardService;

    // 게시글 수정하기 화면 요청

    /**
     * 게시글 수정 화면 요청
     */
    @GetMapping("/board/{id}/board-update")
    public String updateForm(@PathVariable(name = "id") Long boardId, HttpServletRequest request, HttpSession session) {
        log.info("게시글 수정 폼 요청 - boardId : {}", boardId);

        // 1. 인증 검사
        // 2. 게시물 조회 -> 위임
        Board board = boardService.findById(boardId);
        User user = (User)session.getAttribute("session");
        if(!board.isOwner(user.getId())) {
            throw new Exception403("본인 게시글만 수정 할 수 있습니다.");
        }
        User sessionUser = (User) session.getAttribute("session");
        boardService.checkBoardOwner(boardId, sessionUser.getId());
        request.setAttribute("board", boardService.findById(boardId));
        return "board/update-form";

        // 내부에서(스프링 컨테이너) 뷰 리졸브를 활용해서 머스테치 파일
    }

    // 게시글 수정 액션 요청 : 더티 체킹 활용
    // /board/5/update-form

    // 1. 인증검사 - 로그인여부
    // 2. 유효성 검사 (데이터 검증)
    // 3. 권한체크를 위해 게시글다시 조회
    // 4. 더티 체킹을 통한 수정 설정
    // 5. 수정 완료 후에 게시글 상세보기로 리다이렉트 처리


    @PostMapping("/board/{id}/update-form")
    public String update(@PathVariable(name = "id") Long boardId, BoardRequest.UpdateDTO reqDTO, HttpSession session) {

        log.info("게시글 수정 기능 요청 - boardId : {}, 새 제목 {} ", boardId, reqDTO.getTitle());

        // 1. 인증 검사
        // 2. 데이터 유효성(입력해야하는 값만 넣었는지) 검사
        // 3. 수정 요청 위임
        // 4. 리다이렉트 처리
        reqDTO.validate();

        User sessionUser = (User) session.getAttribute("sessionUser");
        boardService.updateById(boardId, reqDTO, sessionUser);

        return "redrect:/board/" + boardId;
    }


    // 게시글 삭제 액션 처리
    // /board/{{board.id}}/delete" method="post"

    // 1. 로그인 여부 (인증 검사)
    // 2. 로그인 x (로그인 페이지로 리다이렉트 처리)
    // 3. 로그인 o (게시물 존재 여부 다시 확인 - 관리자가 동시접근해서 삭제할수도 있음) - 이미 삭제된 게시물 입니다.
    // 4. 로그인 o, 게시물 o (-> 권한체크)
    // 5. 리스트화면으로 리다이렉트 처리
    @PostMapping("/board/{id}/delete")
    public String delete(@PathVariable(name = "id") long id, HttpSession session) {

        log.info("게시글 삭제 요청 - ID : {}", id);

        // 1. 인증검사
        // 2. 세션에서 로그인 한 사용자 정보 추출
        // 3. 서비스 위임
        // 4. 메인페이지로 리다이렉트 처리
        User sessionUser = (User) session.getAttribute("sessionUser");
        boardService.deleteById(id,sessionUser); // Ctrl + space 하면 나옴

        return "redirect:/";
    }


    // 게시글 작성 화면 요청

    /**
     * 주소 설계 : http://localhost:8080/board/save-form
     *
     * @param session
     * @return
     */
    @GetMapping("/board/save-form")
    public String saveForm(HttpSession session) {

        log.info("게시글 삭제 요청 ");
        // 권한 체크 -> 로그인된 사용자만 이동
        return "board/save-form";
    }

    // 게시글 저장 액션 처리
    // http://localhost:8080/board/save
    @PostMapping("/board/save")
    public String save(BoardRequest.SaveDTO reqDTO, HttpSession session) {

        log.info("게시글 작성 기능 요청 ");


        // 1. 인증검사
        // 2. 유효성 검사
        // 3. 서비스 계층 위임
        reqDTO.validate();
        User sessionUser = (User) session.getAttribute("sessionUser");
        boardService.save(reqDTO, sessionUser);

            return "redirect:/";
        }

    @GetMapping("/")
    public String index(Model model) {
        log.info("메인 페이지 요청");

        List<Board> boardList = boardService.findAll();
        model.addAttribute("boardList", boardList);
        return "index";
    }

    // 주소 설계 :
    @GetMapping("/board/{id}")
    public String detail(@PathVariable(name = "id") Long id, Model model) {
        log.info("게시글 상세 보기 요청 - ID : {}", id);
        model.addAttribute("board", boardService.findById(id));
        return "board/detail";
    }

}
