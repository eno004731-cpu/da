package my_jira.orders.order;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/client/applications")
public class OrdersController {
    private final OrdersService ordersService;

    OrdersController(OrdersService ordersService) {
        this.ordersService = ordersService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AnswerDto requestMap(
        HttpSession session,
        @ModelAttribute OrdersDTO ordersDTO,
        @RequestParam(value = "documents", required = false) List<MultipartFile> documents
    ) {
        Long rawId = (Long) session.getAttribute("userId");
        if (rawId == null) {
            throw new RuntimeException("Пользователь не авторизован");
        }

        return ordersService.postOrder(ordersDTO, rawId, documents);
        
    }
}
