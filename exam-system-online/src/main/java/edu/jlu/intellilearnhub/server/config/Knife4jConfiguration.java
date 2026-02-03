package edu.jlu.intellilearnhub.server.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Knife4jConfiguration {


    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("🎓 智能考试系统API文档")
                        .description("📚 智能考试系统后端接口文档，提供完整的RESTful API服务\n\n" +
                                "✨ 主要功能模块：\n" +
                                "• 🧠 题目管理：支持选择题、判断题、简答题的增删改查\n" +
                                "• 📝 试卷管理：手动组卷和AI智能组卷\n" +
                                "• 🎨 轮播图管理：首页轮播图的图片上传和管理\n" +
                                "• 📊 考试记录：考试结果统计和分析\n" +
                                "• 🔔 公告管理：系统公告的发布和管理")
                        .version("v1.0.0"));
    }


    // 用户管理
    @Bean
    public GroupedOpenApi userAPI() {

        return GroupedOpenApi.builder().group("用户信息管理").
                pathsToMatch(
                        "/api/user/**"
                ).
                build();
    }

    // 试题信息管理
    @Bean
    public GroupedOpenApi questionsAPI() {

        return GroupedOpenApi.builder().group("试题信息管理").
                pathsToMatch(
                        "/api/categories/**",
                        "/api/questions/**"
                ).
                build();
    }



    // 试卷信息管理
    @Bean
    public GroupedOpenApi papersAPI() {

        return GroupedOpenApi.builder().group("考试信息管理").
                pathsToMatch(
                        "/api/papers/**",
                        "/api/exams/**",
                        "/api/exam-records/**"
                ).
                build();
    }

    // 视频信息管理
    @Bean
    public GroupedOpenApi videosAPI() {

        return GroupedOpenApi.builder().group("视频信息管理").
                pathsToMatch(
                        "/api/admin/videos/**",
                        "/api/videos/**",
                        "/api/video-categories/**"
                ).
                build();
    }


    // 系统信息管理
    @Bean
    public GroupedOpenApi systemAPI() {

        return GroupedOpenApi.builder().group("系统信息管理").
                pathsToMatch(
                        "/api/banners/**",
                        "/api/notices/**"
                ).
                build();
    }


    // 其他信息管理
    @Bean
    public GroupedOpenApi otherAPI() {

        return GroupedOpenApi.builder().group("其他内容管理").
                pathsToMatch(
                        "/api/stats/**",
                        "/files/**",
                        "/api/debug/**"
                ).
                build();
    }


}