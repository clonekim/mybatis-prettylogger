package com.koreanair.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.List;

@Configuration
@ConditionalOnBean(SqlSessionFactory.class)
@AutoConfigureAfter(MybatisAutoConfiguration.class)
public class MybatisPrettySQLConfig {


    @Autowired
    List<SqlSessionFactory> sqlSessionFactoryList;


    @Configuration
    @ConditionalOnProperty(prefix = "logging.level", value = "mybatis", havingValue = "debug")
    public class MybatisLogInterceptor {

        @PostConstruct
        public void addInterceptor() {
            for(SqlSessionFactory sqlSessionFactory: sqlSessionFactoryList) {
                MybatisPrettySQLInterceptor loggerInterceptor = new MybatisPrettySQLInterceptor();
                sqlSessionFactory.getConfiguration().addInterceptor(loggerInterceptor);
            }
        }
    }
}
