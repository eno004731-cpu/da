package my_jira.orders.order;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import my_jira.users.UsersEntity;



public interface OrdersRepo extends JpaRepository<OrdersEntity, Long>{
    OrdersEntity findByClientUser(UsersEntity id);
    @Query("select o.id from OrdersEntity o where o.clientUser = :user")
    List<Long> findAllIdsByClientUser(@Param("user") UsersEntity user);
}
