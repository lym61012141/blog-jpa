package cn.yml.blog.service.impl;

import cn.yml.blog.dto.ArticleDto;
import cn.yml.blog.dto.ArticleWithPictureDto;
import cn.yml.blog.domain.*;
import cn.yml.blog.repository.*;
import cn.yml.blog.service.ArticleService;
import cn.yml.blog.util.BeanMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * 文章Service实现类
 * 说明：ArticleInfo里面封装了picture/content/category等信息
 *
 * @author:wmyskxz
 * @create:2018-06-19-上午 9:29
 */
@Service
public class ArticleServiceImpl implements ArticleService {

	private static final byte MAX_LASTEST_ARTICLE_COUNT = 5;

	@Autowired
	private BeanMapper beanMapper;

	@Autowired
	private ArticleInfoRepository articleInfoRepository;

	@Autowired
	private ArticlePictureRepository articlePictureRepository;

	@Autowired
	private ArticleCategoryRepository articleCategoryRepository;

	@Autowired
	private ArticleContentRepository articleContentRepository;

	@Autowired
	private CategoryInfoRepository categoryInfoRepository;


	/**
	 * 增加一篇文章信息
	 * 说明：需要对应的写入tbl_article_picture/tbl_article_content/tbl_article_category表
	 * 注意：使用的是Article封装类
	 *
	 * @param articleDto 文章封装类
	 */
	@Override
	public void addArticle(ArticleDto articleDto) {
		// 增加文章信息表 - title/summary
		ArticleInfo articleInfo = new ArticleInfo();
		articleInfo.setTitle(articleDto.getTitle());
		articleInfo.setSummary(articleDto.getSummary());
		articleInfo.setTraffic(0);
		articleInfoRepository.save(articleInfo);
		// 获取刚才插入文章信息的ID
		Long articleId = getArticleLastestId();
		// 增加文章题图信息 - pictureUrl/articleId
		ArticlePicture articlePicture = new ArticlePicture();
		articlePicture.setPictureUrl(articleDto.getPictureUrl());
		articlePicture.setArticleId(articleId);
		articlePictureRepository.save(articlePicture);
		// 增加文章内容信息表 - content/articleId
		ArticleContent articleContent = new ArticleContent();
		articleContent.setContent(articleDto.getContent());
		articleContent.setArticleId(articleId);
		articleContentRepository.save(articleContent);
		// 增加文章分类信息表 - articleId/categoryId
		ArticleCategory articleCategory = new ArticleCategory();
		articleCategory.setArticleId(articleId);
		articleCategory.setCategoryId(articleDto.getCategoryId());
		articleCategoryRepository.save(articleCategory);
		// 对应文章的数量要加1
		categoryInfoRepository.updateNumberById(articleCategory.getCategoryId(), 1);
	}

	/**
	 * 删除一篇文章
	 * 说明：需要对应删除tbl_article_picture/tbl_article_content/tbl_article_category/tbl_category_info表中的内容
	 *
	 * @param id
	 */
	@Override
	public void deleteArticleById(Long id) {
		ArticleDto articleDto = getOneById(id);
		// 删除文章信息中的数据
		articleInfoRepository.deleteById(articleDto.getId());
		// 删除文章题图信息数据
		articlePictureRepository.deleteById(articleDto.getArticlePictureId());
		// 删除文章内容信息表
		articleContentRepository.deleteById(articleDto.getArticleContentId());
		// 删除文章分类信息表
		articleCategoryRepository.deleteById(articleDto.getArticleCategoryId());
		// 扣减文章分类信息的数量
		categoryInfoRepository.updateNumberById(articleDto.getCategoryId(),-1);
	}

	/**
	 * 更改文章的分类信息
	 *
	 * @param articleId
	 * @param categoryId
	 */
	@Override
	public void updateArticleCategory(Long articleId, Long categoryId) {
		ArticleCategory articleCategory = articleCategoryRepository.getOne(articleId);
		// 减少被修改文章分类的分类下的文章数目,算的时候避免并发，采用SQL动态更新number
		categoryInfoRepository.updateNumberById(articleCategory.getCategoryId(),-1);
		//增加文章加入的分类下的文章数目
		categoryInfoRepository.updateNumberById(categoryId,1);
		// 把文章分类的分类设置为更新后的分类
		articleCategory.setCategoryId(categoryId);
		articleCategoryRepository.save(articleCategory);
	}

	/**
	 * 更新文章信息
	 * 说明：需要对应更改tbl_article_picture/tbl_article_content/tbl_article_category表中的内容
	 * 注意：我们使用的是封装好的Article文章信息类
	 *
	 * @param articleDto 自己封装的Article信息类
	 */
	@Override
	public void updateArticle(ArticleDto articleDto) {
		// 更新文章信息中的数据
		ArticleInfo articleInfo = new ArticleInfo();
		articleInfo.setId(articleDto.getId());
		articleInfo.setTitle(articleDto.getTitle());
		articleInfo.setSummary(articleDto.getSummary());
		articleInfo.setIsTop(articleDto.getTop());
		articleInfo.setTraffic(articleDto.getTraffic());
		articleInfoRepository.save(articleInfo);
		// 更新文章题图信息数据
		ArticlePicture articlePicture = articlePictureRepository.findByArticleId(articleDto.getId());
		articlePicture.setPictureUrl(articleDto.getPictureUrl());
		articlePictureRepository.save(articlePicture);
		// 更新文章内容信息数据
		ArticleContent articleContent = articleContentRepository.findByArticleId(articleDto.getId());
		articleContent.setContent(articleDto.getContent());
		articleContentRepository.save(articleContent);
		// 更新文章分类信息表
		ArticleCategory articleCategory = articleCategoryRepository.findByArticleId(articleDto.getId());
		articleCategory.setCategoryId(articleDto.getCategoryId());
		articleCategoryRepository.save(articleCategory);
	}

	/**
	 * 获取一篇文章内容
	 * 说明：需要对应从tbl_article_picture/tbl_article_content/tbl_article_category表中获取内容
	 * 并封装好
	 *
	 * @param id 文章ID
	 * @return 填充好数据的ArticleInfo
	 */
	@Override
	public ArticleDto getOneById(Long id) {
		ArticleDto articleDto = new ArticleDto();
		// 填充文章基础信息
		ArticleInfo articleInfo = articleInfoRepository.getOne(id);
		articleDto.setId(articleInfo.getId());
		articleDto.setTitle(articleInfo.getTitle());
		articleDto.setSummary(articleInfo.getSummary());
		articleDto.setTop(articleInfo.getIsTop());
		articleDto.setCreateBy(articleInfo.getCreateBy());
		// 文章访问量要加1
		articleDto.setTraffic(articleInfo.getTraffic() + 1);
		articleInfoRepository.updateTrafficById(id,1);
		// 填充文章内容信息
		ArticleContent articleContent = articleContentRepository.findByArticleId(id);
		articleDto.setContent(articleContent.getContent());
		articleDto.setArticleContentId(articleContent.getId());
		// 填充文章题图信息
		ArticlePicture articlePicture = articlePictureRepository.findByArticleId(id);
		articleDto.setPictureUrl(articlePicture.getPictureUrl());
		articleDto.setArticlePictureId(articlePicture.getId());
		// 填充文章分类信息
		ArticleCategory articleCategory = articleCategoryRepository.findByArticleId(id);
		articleDto.setArticleCategoryId(articleCategory.getId());
		// 填充文章分类基础信息
		CategoryInfo categoryInfo = categoryInfoRepository.getOne(articleCategory.getCategoryId());
		articleDto.setCategoryId(categoryInfo.getId());
		articleDto.setCategoryName(categoryInfo.getName());
		articleDto.setCategoryNumber(categoryInfo.getNumber());
		return articleDto;
	}

	/**
	 * 获取所有的文章内容
	 *
	 * @return 封装好的Article集合
	 */
	@Override
	public List<ArticleWithPictureDto> listAll() {
		// 1.先获取所有的数据
		List<ArticleWithPictureDto> articles = listAllArticleWithPicture();
		// 2.然后再对集合进行重排，置顶的文章在前
		LinkedList<ArticleWithPictureDto> list = new LinkedList<>();
		for (int i = 0; i < articles.size(); i++) {
			if (true == articles.get(i).getTop()) {
				list.addFirst(articles.get(i));
			} else {
				list.addLast(articles.get(i));
			}
		}
		articles = new ArrayList<>(list);

		return articles;
	}

	/**
	 * 通过分类id返回该分类下的所有文章
	 *
	 * @param id 分类ID
	 * @return 对应分类ID下的所有文章(带题图)
	 */
	@Override
	public List<ArticleWithPictureDto> listByCategoryId(Long id) {
		List<ArticleCategory> articleCategories = articleCategoryRepository.findByCategoryId(id);
		List<ArticleWithPictureDto> articles = new ArrayList<>();
		for (int i = 0; i < articleCategories.size(); i++) {
			Long articleId = articleCategories.get(i).getArticleId();
			ArticleWithPictureDto articleWithPictureDto = new ArticleWithPictureDto();
			// 填充文章基础信息
			ArticleInfo articleInfo = articleInfoRepository.getOne(articleId);
			articleWithPictureDto.setId(articleInfo.getId());
			articleWithPictureDto.setTitle(articleInfo.getTitle());
			articleWithPictureDto.setSummary(articleInfo.getSummary());
			articleWithPictureDto.setTop(articleInfo.getIsTop());
			articleWithPictureDto.setTraffic(articleInfo.getTraffic());
			// 填充文章图片信息
			ArticlePicture articlePicture = articlePictureRepository.findByArticleId(articleId);
			articleWithPictureDto.setArticlePictureId(articlePicture.getId());
			articleWithPictureDto.setPictureUrl(articlePicture.getPictureUrl());
			articles.add(articleWithPictureDto);
		}


		// 对集合进行重排，置顶的文章在前
		LinkedList<ArticleWithPictureDto> list = new LinkedList<>();
		for (int i = 0; i < articles.size(); i++) {
			if (true == articles.get(i).getTop()) {
				list.add(articles.get(i));
			} else {
				list.addLast(articles.get(i));
			}
		}
		articles = new ArrayList<>(list);

		return articles;
	}

	/**
	 * 获取最新的文章信息，默认为5篇
	 * 说明：listAll默认已经按照id排序了，所以我们只需要返回前五篇就可以了
	 * 注意：需要判断当前的文章是否大于5篇
	 *
	 * @return 返回五篇最新的文章数据
	 */
	@Override
	public List<ArticleWithPictureDto> listLastest() {
		// 1.先获取所有的数据
		List<ArticleWithPictureDto> articles = listAllArticleWithPicture();
		// 2.判断是否满足5个的条件
		if (articles.size() >= MAX_LASTEST_ARTICLE_COUNT) {
			// 3.大于5个则返回前五个数据
			List<ArticleWithPictureDto> tempArticles = new ArrayList<>();
			for (int i = 0; i < 5; i++) {
				tempArticles.add(articles.get(i));
			}
			return tempArticles;
		}
		// 4.不大于五个则直接返回
		return articles;
	}

	/**
	 * 返回最新插入一条数据的ID
	 *
	 * @return
	 */
	private Long getArticleLastestId() {
		return articleInfoRepository.findMaxId();
	}

	/**
	 * 通过文章ID获取对应的文章题图信息
	 *
	 * @param id 文章ID
	 * @return 文章ID对应的文章题图信息
	 */
	@Override
	public ArticlePicture getPictureByArticleId(Long id) {
		return articlePictureRepository.findByArticleId(id);
	}

	/**
	 * 获取所有的文章信息（带题图）
	 *
	 * @return
	 */
	private List<ArticleWithPictureDto> listAllArticleWithPicture() {
		// 无添加查询即返回所有
		Sort sort = new Sort(Sort.Direction.DESC,"is_top","id");
		List<ArticleInfo> articleInfos = articleInfoRepository.findAll();
		List<ArticleWithPictureDto> articles = new ArrayList<>();
		for (ArticleInfo articleInfo : articleInfos) {
			ArticleWithPictureDto articleWithPictureDto = new ArticleWithPictureDto();
			// 填充文章基础信息
			articleWithPictureDto.setId(articleInfo.getId());
			articleWithPictureDto.setTitle(articleInfo.getTitle());
			articleWithPictureDto.setSummary(articleInfo.getSummary());
			articleWithPictureDto.setTop(articleInfo.getIsTop());
			articleWithPictureDto.setTraffic(articleInfo.getTraffic());
			// 填充文章题图信息
			ArticlePicture articlePicture = articlePictureRepository.findByArticleId(articleInfo.getId());
			articleWithPictureDto.setArticlePictureId(articlePicture.getId());
			articleWithPictureDto.setPictureUrl(articlePicture.getPictureUrl());
			articles.add(articleWithPictureDto);
		}
		return articles;
	}

}
