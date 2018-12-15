package cn.yml.blog.repository;

import cn.yml.blog.domain.ArticleInfo;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Liuym
 * @date 2018/12/15 0015
 */
public interface ArticleInfoRepository extends JpaPartitionRepository<ArticleInfo, Long> {

    @Transactional(timeout = 20)
    @Modifying
    @Query(value = "update tbl_article_info c set c.traffic = c.traffic + :traffic where c.id = :id", nativeQuery = true)
    void updateTrafficById(@Param("id") Long id, @Param("traffic") Integer traffic);

    @Query(value = "select max(id) from tbl_article_info", nativeQuery = true)
    Long findMaxId();
}
