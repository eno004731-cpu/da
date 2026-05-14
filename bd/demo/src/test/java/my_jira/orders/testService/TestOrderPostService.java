package my_jira.orders.testService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import my_jira.common.exception.FileValidationException;
import my_jira.common.exception.ServiceNotFoundException;
import my_jira.common.exception.UserNotFoundException;
import my_jira.orders.documentss.DocumentsRepo;
import my_jira.orders.documentss.OrdersDocuments;
import my_jira.orders.order.OrdersEntity;
import my_jira.orders.order.OrdersRepo;
import my_jira.orders.order.postEnpoint.AnswerDto;
import my_jira.orders.order.postEnpoint.OrdersDTO;
import my_jira.orders.order.postEnpoint.OrdersService;
import my_jira.services.ServiceEntity;
import my_jira.services.ServiceRepository;
import my_jira.users.UsersEntity;
import my_jira.users.UsersRepo;

@ExtendWith(MockitoExtension.class)
public class TestOrderPostService {
    @Mock
    private UsersRepo usersRepo;
    @Mock
    private OrdersRepo ordersRepo;
    @Mock
    private ServiceRepository serviceRepository;
    @Mock
    private DocumentsRepo documentsRepo;
    @InjectMocks
    private OrdersService ordersService;

    
    private OrdersDTO getRequest(){
        OrdersDTO request = new OrdersDTO();
        request.setServiceCode("REGISTRATION");
        request.setClientName("Иван Иванов");
        request.setContact("test@mail.com");
        request.setCompanyName("ООО Ромашка");
        request.setDescription("Описание тестового заказа");
        return request;
    }
   
    private MockMultipartFile createDocument(){
        MockMultipartFile document = new MockMultipartFile(
        "documents",                 // имя параметра
        "test.pdf",                   // имя файла
        "application/pdf",            // contentType
        "test content".getBytes());    // содержимое файла
        return document;
    }
   
    private MultipartFile createBrokenDocument(){
        MockMultipartFile document2 = new MockMultipartFile(
        "documents",                 // имя параметра
        "test.webp",                   // имя файла
        "image/webp",            // contentType
        "test content".getBytes());     // содержимое файла
        return document2;
    }
    

    @TempDir
Path tempDir;

@BeforeEach
void setUp() {
    ReflectionTestUtils.setField(ordersService, "ordersUploadDir", tempDir.toString());
}

List<MultipartFile> documentsBroken = List.of(createBrokenDocument());
        List<MultipartFile> documents = List.of(createDocument());
    
    @Test
    public void testUserNotFound(){
        String email = "test@mail.com";
        
        OrdersDTO request = getRequest();
        when(usersRepo.findByEmail(email))
            .thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, ()->{
            ordersService.postOrder(request, email, documents);
        });
    }

    @Test
    public void testServiceNotFound(){
        OrdersDTO request = getRequest();
        UsersEntity user = new UsersEntity();
        String email = "test@mail.com";
        when(usersRepo.findByEmail("test@mail.com"))
        .thenReturn(Optional.of(user));
        when(serviceRepository.findByCode(request.getServiceCode()))
            .thenReturn(null);
        assertThrows(ServiceNotFoundException.class, ()-> {
            ordersService.postOrder(request, email, documents);
        });
    }
    @Test
    public void testValidateDocuments(){
        OrdersDTO request = getRequest();
        UsersEntity user = new UsersEntity();
        String email = "test@mail.com";
      
        ServiceEntity service = new ServiceEntity();
        service.setCode("REGISTRATION");
        service.setName("Регистрация ООО");
        
        assertThrows(FileValidationException.class, ()-> {
            ordersService.postOrder(request, email, documentsBroken);
        });

    }
    @Test
    public void createOrderWithoutDocuments(){
        // arrange
    String email = "test@mail.com";
    OrdersDTO request = getRequest();

    UsersEntity user = new UsersEntity();
    ServiceEntity service = new ServiceEntity();
    service.setCode("REGISTRATION");
    service.setName("Регистрация ООО");

    when(usersRepo.findByEmail(email)).thenReturn(Optional.of(user));
    when(serviceRepository.findByCode("REGISTRATION")).thenReturn(service);

    // save(order) в mock ничего сам не сохраняет,
    // поэтому просто возвращаем тот же объект
    when(ordersRepo.save(any(OrdersEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

    // act
    AnswerDto result = ordersService.postOrder(request, email, null);

    // assert dto
    assertEquals("NEW", result.getStatus());
    assertEquals("Иван Иванов", result.getClientName());
    assertEquals("test@mail.com", result.getContact());
    assertEquals("ООО Ромашка", result.getCompanyName());

    // verify repo calls
    verify(ordersRepo, times(1)).save(any(OrdersEntity.class));
    verify(documentsRepo, never()).save(any());

    // capture saved order
    ArgumentCaptor<OrdersEntity> orderCaptor = ArgumentCaptor.forClass(OrdersEntity.class);
    verify(ordersRepo).save(orderCaptor.capture());

    OrdersEntity savedOrder = orderCaptor.getValue();

    assertEquals("Иван Иванов", savedOrder.getClientNameSnapshot());
    assertEquals("test@mail.com", savedOrder.getContactSnapshot());
    assertEquals("ООО Ромашка", savedOrder.getCompanyNameSnapshot());
    assertEquals("Описание тестового заказа", savedOrder.getDescription());
    assertEquals("NEW", savedOrder.getStatus());
    assertEquals("LOW", savedOrder.getPriority());
    assertEquals(0, savedOrder.getRevisionCount());
    assertEquals(service, savedOrder.getService());
    assertEquals("Регистрация ООО", savedOrder.getTitle());
    }

    @Test
    public void testCreateOrderWithValidDoc(){
        String email = "test@mail.com";
    OrdersDTO request = getRequest();

    UsersEntity user = new UsersEntity();
    ServiceEntity service = new ServiceEntity();
    service.setCode("REGISTRATION");
    service.setName("Регистрация ООО");
    when(usersRepo.findByEmail(email)).thenReturn(Optional.of(user));
    when(serviceRepository.findByCode("REGISTRATION")).thenReturn(service);
    
    // save(order) в mock ничего сам не сохраняет,
    // поэтому просто возвращаем тот же объект
    when(ordersRepo.save(any(OrdersEntity.class)))
    .thenAnswer(invocation -> invocation.getArgument(0));
    ordersService.postOrder(request, email, documents);
    ArgumentCaptor<List<OrdersDocuments>> documentsCaptor = ArgumentCaptor.forClass(List.class);
    verify(documentsRepo, times(1)).saveAll(documentsCaptor.capture());

    verify(ordersRepo, times(1)).save(any(OrdersEntity.class));
    List<OrdersDocuments> documents = documentsCaptor.getValue();
        for(OrdersDocuments document:documents){
            assertNotNull(document);
            assertNotNull(document.getOrder());
            assertNotNull(document.getUploadedByUser());
            assertNotNull(document.getStorageKey());
            assertNotNull(document.getCreatedAt());
            assertNotNull(document.getMimeType());
            assertNotNull(document.getOriginalFileName());
            assertNotNull(document.getSizeBytes());
            


        }
    }  


}