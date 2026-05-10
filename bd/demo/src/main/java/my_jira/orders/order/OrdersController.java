package my_jira.orders.order;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/client/applications")
@RequiredArgsConstructor
public class OrdersController {
    private final OrdersService ordersService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AnswerDto requestMap(
       
        @ModelAttribute OrdersDTO ordersDTO,
        @RequestParam(value = "documents", required = false) List<MultipartFile> documents,
        Authentication authentication
    ) {

         String email = authentication.getName();

        return ordersService.postOrder(ordersDTO, email, documents);
        
    }
}
