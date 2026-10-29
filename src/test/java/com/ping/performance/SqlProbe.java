package com.ping.performance;

import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.*;
import javax.sql.DataSource;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/** Test-only JDBC observation: SQL executions and consumed rows, never parameter values. */
@TestConfiguration(proxyBeanMethods = false)
public class SqlProbe {
    static final AtomicBoolean CAPTURING = new AtomicBoolean();
    static final Queue<Query> QUERIES = new ConcurrentLinkedQueue<>();
    static final AtomicInteger CONNECTIONS = new AtomicInteger(), COMMITS = new AtomicInteger();
    record Query(String sql, AtomicLong rows) { }
    record Snapshot(List<Query> queries, int connections, int commits, long httpNanos) {
        long selects() { return queries.stream().filter(q -> q.sql().startsWith("select ")).count(); }
        long rows(String table) { return queries.stream().filter(q -> q.sql().contains(" from " + table + " ")).mapToLong(q -> q.rows().get()).sum(); }
        long statements(String table) { return queries.stream().filter(q -> q.sql().contains(" from " + table + " ")).count(); }
        void print(String label) {
            System.out.printf("BACKEND_METRIC %s phase=%s selects=%d rows_matches=%d rows_benefits=%d connections=%d commits=%d http_ms=%.3f%n",
                label, System.getProperty("backend.phase", "current"), selects(), rows("ping_matches"), rows("benefits"), connections, commits, httpNanos / 1_000_000.0);
            queries.forEach(q -> System.out.println("BACKEND_SQL " + label + " rows=" + q.rows().get() + " " + q.sql()));
        }
    }
    static void begin() {
        QUERIES.clear(); CONNECTIONS.set(0); COMMITS.set(0); CAPTURING.set(true);
    }
    static Snapshot end(long nanos) {
        CAPTURING.set(false);
        return new Snapshot(List.copyOf(QUERIES), CONNECTIONS.get(), COMMITS.get(), nanos);
    }
    static Object call(Object target, Method method, Object[] args) throws Throwable {
        try { return method.invoke(target, args); }
        catch (InvocationTargetException e) { throw e.getCause(); }
    }
    static ResultSet rows(ResultSet result, Query query) {
        return (ResultSet) Proxy.newProxyInstance(SqlProbe.class.getClassLoader(), new Class<?>[]{ResultSet.class}, (p,m,a) -> {
            Object value = call(result,m,a);
            if (m.getName().equals("next") && Boolean.TRUE.equals(value)) query.rows().incrementAndGet();
            return value;
        });
    }
    static Statement statement(Statement statement, String preparedSql) {
        var current = new AtomicReference<Query>();
        Class<?> type = statement instanceof PreparedStatement ? PreparedStatement.class : Statement.class;
        return (Statement) Proxy.newProxyInstance(SqlProbe.class.getClassLoader(), new Class<?>[]{type}, (p,m,a) -> {
            if (CAPTURING.get() && Set.of("execute", "executeQuery", "executeUpdate", "executeLargeUpdate").contains(m.getName())) {
                String sql = preparedSql != null ? preparedSql : a != null && a.length > 0 && a[0] instanceof String s ? s : "unknown";
                Query q = new Query(sql.replaceAll("\\s+", " ").strip().toLowerCase(Locale.ROOT), new AtomicLong());
                current.set(q); QUERIES.add(q);
            }
            Object value = call(statement,m,a);
            return value instanceof ResultSet r && current.get() != null ? rows(r,current.get()) : value;
        });
    }
    static Connection connection(Connection connection) {
        return (Connection) Proxy.newProxyInstance(SqlProbe.class.getClassLoader(), new Class<?>[]{Connection.class}, (p,m,a) -> {
            if (CAPTURING.get() && m.getName().equals("commit")) COMMITS.incrementAndGet();
            Object result = call(connection,m,a);
            if (m.getName().equals("prepareStatement")) return statement((Statement)result,(String)a[0]);
            if (m.getName().equals("createStatement")) return statement((Statement)result,null);
            return result;
        });
    }
    @Bean static BeanPostProcessor sqlObservation() {
        return new BeanPostProcessor() {
            @Override public Object postProcessAfterInitialization(Object bean, String name) {
                if (!(bean instanceof DataSource data)) return bean;
                return Proxy.newProxyInstance(SqlProbe.class.getClassLoader(),new Class<?>[]{DataSource.class},(p,m,a) -> {
                    Object result = call(data,m,a);
                    if (m.getName().equals("getConnection")) {
                        if (CAPTURING.get()) CONNECTIONS.incrementAndGet();
                        return connection((Connection)result);
                    }
                    return result;
                });
            }
        };
    }
}