package com.example.capstone.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicInsert;

@DynamicInsert
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class PhotoActivityType {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)  // ID를 자동으로 생성
  private Long id;

  @Column(name = "name", nullable = false)
  private String name;
}
