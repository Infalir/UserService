package com.appname.orderservice.repository;

import com.appname.orderservice.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {
  boolean existsByName(String name);

  List<Item> findAllByIdIn(List<Long> ids);

}
