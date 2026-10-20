package com.ping.interest;
import java.util.*;import jakarta.validation.constraints.*;
public class TaxonomyForm {
 @NotBlank @Pattern(regexp="[a-z0-9][a-z0-9:-]{0,99}") private String nodeKey="";
 @NotBlank @Size(max=80) private String label="";@Size(max=100) private String parent="";private boolean active=true;@NotNull @PositiveOrZero private Long revision=0L;private List<Long> interestIds=new ArrayList<>();
 public String getNodeKey(){return nodeKey;}public void setNodeKey(String v){nodeKey=v;}public String getLabel(){return label;}public void setLabel(String v){label=v;}public String getParent(){return parent;}public void setParent(String v){parent=v;}public boolean isActive(){return active;}public void setActive(boolean v){active=v;}public Long getRevision(){return revision;}public void setRevision(Long v){revision=v;}public List<Long> getInterestIds(){return interestIds;}public void setInterestIds(List<Long> v){interestIds=v==null?new ArrayList<>():v;}
}
