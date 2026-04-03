package com.charity.modules.activity.doc;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.GeoPointField;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;

import java.time.LocalDateTime;

@Data
@Document(indexName = "activity")
public class ActivityDoc {
    @Id
    private Long id;

    @Field(type = FieldType.Long)
    private Long categoryId;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String title;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String summary;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String content;

    @Field(type = FieldType.Keyword)
    private String coverImage;

    @Field(type = FieldType.Integer)
    private Integer status;

    @Field(type = FieldType.Date, format = {}, pattern = "uuuu-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @Field(type = FieldType.Date, format = {}, pattern = "uuuu-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String locationName;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String address;

    @GeoPointField
    private GeoPoint location;

    @Field(type = FieldType.Integer)
    private Integer points;

    @Field(type = FieldType.Double)
    private Double volunteerDuration;

    @Field(type = FieldType.Integer)
    private Integer registeredCount;

    @Field(type = FieldType.Integer)
    private Integer viewCount;

    @Field(type = FieldType.Date, format = {}, pattern = "uuuu-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
