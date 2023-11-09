package com.koreanair.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.springframework.core.Ordered;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;


@Intercepts({@Signature(type = StatementHandler.class, method = "query", args = {Statement.class, ResultHandler.class})})
public class MybatisPrettySQLInterceptor implements Interceptor, Ordered {


    final static Log log = LogFactory.getLog(MybatisPrettySQLInterceptor.class);


    @Override
    public Object intercept(Invocation invocation) throws Throwable {

        Object target = invocation.getTarget();

        try {
            return invocation.proceed();
        } finally {

            String sql = getSql(target);
            log.debug(String.format("Pretty => \n%s\n", sql));
        }
    }


    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {

    }


    Configuration getConfiguration(StatementHandler statementHandler) {

        try {
            final DefaultParameterHandler parameterHandler = (DefaultParameterHandler) statementHandler.getParameterHandler();
            Field configurationField = ReflectionUtils.findField(parameterHandler.getClass(), "configuration");
            ReflectionUtils.makeAccessible(configurationField);
            return (Configuration) configurationField.get(parameterHandler);
        } catch (Exception e) {
            log.warn("get configuration error", e);
        }

        return null;
    }

    private Configuration configuration;


    String getSql(Object target) {

        try {
            StatementHandler statementHandler = (StatementHandler) target;
            BoundSql boundSql = statementHandler.getBoundSql();


            if (configuration == null)
                this.configuration = getConfiguration(statementHandler);

            String sql = boundSql.getSql();
            List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
            Object parameterObject = boundSql.getParameterObject();

            if (sql == null || sql.length() == 0 || configuration == null) {
                return "";
            }

            TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();

            for (ParameterMapping mapping : parameterMappings) {
                if (mapping.getMode() != ParameterMode.OUT) {


                    Object value;
                    String propertyName = mapping.getProperty();

                    if (boundSql.hasAdditionalParameter(propertyName)) {
                        value = boundSql.getAdditionalParameter(propertyName);

                    } else if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
                        value = parameterObject;

                    } else if (parameterObject == null) {
                        value = null;

                    } else {
                        MetaObject metaObject = configuration.newMetaObject(parameterObject);
                        value = metaObject.getValue(propertyName);
                    }

                    if (value instanceof String) {
                        sql = sql.replaceFirst("\\?", Matcher.quoteReplacement("'" + value + "'"));
                    } else {
                        sql = sql.replaceFirst("\\?", value.toString());
                    }
                }
            }

            return sql.replaceAll("(?m)^\\s*\\r?\\n", "");

        } catch (Exception e) {
            log.warn(String.format("get error %s", target), e);
        }

        return "";
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
