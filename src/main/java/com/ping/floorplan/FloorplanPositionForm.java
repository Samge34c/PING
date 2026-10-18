package com.ping.floorplan;

import jakarta.validation.constraints.*;
public class FloorplanPositionForm {
    @NotBlank(message="Recarga el plano antes de guardar.") private String planId;
    @Min(value=0,message="Recarga las posiciones guardadas.") private long revision;
    @NotNull(message="Selecciona un punto de encuentro.") @Positive(message="Selecciona un punto existente.") private Long pointId;
    @DecimalMin(value="0",message="La posición horizontal debe estar entre0 y1.") @DecimalMax(value="1",message="La posición horizontal debe estar entre0 y1.") private Double x;
    @DecimalMin(value="0",message="La posición vertical debe estar entre0 y1.") @DecimalMax(value="1",message="La posición vertical debe estar entre0 y1.") private Double y;
    public String getPlanId(){return planId;} public void setPlanId(String value){planId=value;}
    public long getRevision(){return revision;} public void setRevision(long value){revision=value;}
    public Long getPointId(){return pointId;} public void setPointId(Long value){pointId=value;}
    public Double getX(){return x;} public void setX(Double value){x=value;}
    public Double getY(){return y;} public void setY(Double value){y=value;}
}
