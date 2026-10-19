package com.ping.interest;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
@Service @Profile("!bootstrap")
public class InterestTaxonomy {
 private final JdbcTemplate jdbc;
 public InterestTaxonomy(JdbcTemplate j){jdbc=j;}
 public record Node(String key,String label,String parent){}
 public List<Node> nodes(){return jdbc.query("select node_key,label,parent_key from interest_nodes where active=true",(r,n)->new Node(r.getString(1),r.getString(2),r.getString(3)));}
 public Map<Long,List<String>> memberships(){var map=new LinkedHashMap<Long,List<String>>();jdbc.query("select m.interest_id,m.node_key from interest_node_members m join interest_nodes n on n.node_key=m.node_key where n.active=true",(org.springframework.jdbc.core.RowCallbackHandler)r->map.computeIfAbsent(r.getLong(1),id->new ArrayList<>()).add(r.getString(2)));return map;}
}
