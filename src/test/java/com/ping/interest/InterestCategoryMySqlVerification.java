package com.ping.interest;
import java.sql.*;
public class InterestCategoryMySqlVerification {
 public static void main(String[] args)throws Exception{
  String url=System.getenv("DB_URL"),password=System.getenv("DB_PASSWORD");
  if(url==null||!url.contains("exclusive"))throw new IllegalArgumentException("Use a NEW exclusive database, never the demo.");
  try(var c=DriverManager.getConnection(url,"root",password);var s=c.createStatement();var r=s.executeQuery("SELECT count(*) FROM information_schema.tables WHERE table_schema=database()")){
   r.next();if(!c.getCatalog().contains("exclusive")||r.getInt(1)!=0)throw new IllegalStateException("Database must be exclusive and empty; no changes applied.");
  }
  InterestCategoryMigrationTests.verify(new org.springframework.jdbc.datasource.DriverManagerDataSource(url,"root",password));
 }
}
