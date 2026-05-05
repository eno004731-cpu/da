package my_jira.services;

public class ServiceResponseDto {

    private Long id;
    private String name;
    private String code;
    private String shortDescription;

    public ServiceResponseDto(){

    }

    public ServiceResponseDto(Long id, String code, String name ,String shortDescription){
        this.id=id;
        this.code=code;
        this.name=name;
        this.shortDescription=shortDescription;
    }

    public Long getId(){
        return id;
    }
    public String getCode(){
        return code;
    }
    public String getName(){
        return name;
    }
    public String getShortDescription(){
        return shortDescription;
    }
}