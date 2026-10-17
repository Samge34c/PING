package com.ping.interest;
import java.util.*;
import jakarta.validation.constraints.*;
public class InterestCategoryForm {
 @NotBlank(message="Escribe el nombre de la categoría.") @Size(max=80) private String name="";
 @Min(0) @Max(9999) private int presentationOrder;
 private boolean active=true;
 @PositiveOrZero private Long version;
 @NotNull private List<@NotNull @Positive Long> interestIds=new ArrayList<>();
 public String getName(){return name;}public void setName(String value){name=value;}
 public int getPresentationOrder(){return presentationOrder;}public void setPresentationOrder(int value){presentationOrder=value;}
 public boolean isActive(){return active;}public void setActive(boolean value){active=value;}
 public Long getVersion(){return version;}public void setVersion(Long value){version=value;}
 public List<Long> getInterestIds(){return interestIds;}public void setInterestIds(List<Long> value){interestIds=value;}
}
