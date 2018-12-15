package cn.yml.blog.repository;

import cn.yml.blog.domain.ArticleComment;

import java.util.List;

/**
 * @author Liuym
 * @date 2018/12/15 0015
 */
public interface ArticleCommentRepository extends JpaPartitionRepository<ArticleComment, Long> {

    List<ArticleComment> findByArticleId(Long articleId);
}
