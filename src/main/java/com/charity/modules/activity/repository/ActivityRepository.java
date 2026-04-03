package com.charity.modules.activity.repository;

import com.charity.modules.activity.doc.ActivityDoc;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityRepository extends ElasticsearchRepository<ActivityDoc, Long> {
    List<ActivityDoc> findByTitleOrSummaryOrContent(String title, String summary, String content);
}
