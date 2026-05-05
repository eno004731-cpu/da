package my_jira.orders.order;

import org.springframework.data.jpa.repository.JpaRepository;

import my_jira.users.UsersEntity;



public interface OrdersRepo extends JpaRepository<OrdersEntity, Long>{
    OrdersEntity findByClientUser(UsersEntity id);
}
