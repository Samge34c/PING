package com.ping.floorplan;

import java.sql.DriverManager;
public class FloorplanMySqlVerification {
    public static void main(String[] args) throws Exception {
        String url=System.getenv("DB_URL"),password=System.getenv("DB_PASSWORD");
        if(url==null||!url.contains("exclusive"))throw new IllegalArgumentException("Use a new exclusive MySQL database.");
        try(var c=DriverManager.getConnection(url,"root",password);var s=c.createStatement();var r=s.executeQuery("select count(*) from information_schema.tables where table_schema=database()")){
            r.next();if(!c.getMetaData().getDatabaseProductName().equals("MySQL")||!c.getCatalog().contains("exclusive")||r.getInt(1)!=0)throw new IllegalStateException("Use an empty exclusive MySQL database; no changes applied.");
        }
        FloorplanMigrationTests.verify(new org.springframework.jdbc.datasource.DriverManagerDataSource(url,"root",password));
    }
}
