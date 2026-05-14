package my_jira.orders.order;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import my_jira.users.UsersEntity;



public interface OrdersRepo extends JpaRepository<OrdersEntity, Long>{

    @Query("select o.id from OrdersEntity o where o.clientUser = :user")
    List<Long> findAllIdsByClientUser(@Param("user") UsersEntity user);
    @Query("""
    select o
    from OrdersEntity o
    join fetch o.service
    where o.clientUser = :user
""")
List<OrdersEntity> findAllByClientUserWithService(@Param("user") UsersEntity user);
    @Query("""
            select o
            from OrdersEntity o
            join fetch o.clientUser
            where o.id = :id and o.clientUser = :user
            """)
    Optional< OrdersEntity> findByIdAndClientUser(@Param("id") Long id,
        @Param("user") UsersEntity user);


    

}
