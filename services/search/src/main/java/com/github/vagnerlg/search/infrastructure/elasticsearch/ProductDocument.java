package com.github.vagnerlg.search.infrastructure.elasticsearch;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.time.Instant;

@Document(indexName = "products")
record ProductDocument(
        @Id String id,
        @Field(type = FieldType.Text) String name,
        @Field(type = FieldType.Text) String description,
        @Field(type = FieldType.Double) BigDecimal price,
        @Field(type = FieldType.Keyword) String category,
        @Field(type = FieldType.Date, format = DateFormat.date_time) Instant createdAt,
        @Field(type = FieldType.Date, format = DateFormat.date_time) Instant updatedAt
) {}
